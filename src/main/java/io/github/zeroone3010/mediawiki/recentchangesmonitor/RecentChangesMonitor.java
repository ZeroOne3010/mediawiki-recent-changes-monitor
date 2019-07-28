package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An application to monitor the Recent Changes list of a given MediaWiki instance and to report the edits
 * of any new users.
 */
public class RecentChangesMonitor {
  private static final String USER_SPACE_PREFIX = "User:";
  private final MediaWiki mediaWiki;

  /**
   * @param apiUrl The URL of the api.php service of the target MediaWiki instance.
   */
  public RecentChangesMonitor(final String apiUrl) {
    this.mediaWiki = new MediaWiki(apiUrl);
  }

  /**
   * Finds and returns edits by new users.
   *
   * @return A Map where the keys are user names and values are edits by those users.
   */
  public Map<String, List<RecentChange>> findEditsByNewUsers() {
    final List<RecentChange> recentChanges = mediaWiki.fetchRecentChanges();
    return findChangesByNewUsers(recentChanges);
  }

  /**
   * Finds any new users, then returns the changes they made.
   *
   * @param recentChanges A list of Recent Changes.
   * @return A Map where the key is the user name and the value is a list of their edits.
   */
  private Map<String, List<RecentChange>> findChangesByNewUsers(final List<RecentChange> recentChanges) {
    final Map<String, List<RecentChange>> changesByNewUsers = new HashMap<>();
    recentChanges.forEach(rc -> {
      if (rc.getType() == RecentChange.ChangeType.NEW && rc.getTitle().startsWith(USER_SPACE_PREFIX)) {
        changesByNewUsers.put(rc.getTitle().substring(USER_SPACE_PREFIX.length()), new ArrayList<>());
      }
    });

    recentChanges.forEach(rc -> {
          if (changesByNewUsers.keySet().contains(rc.getUser())) {
            changesByNewUsers.merge(rc.getUser(), new ArrayList<>(Collections.singleton(rc)), (a, b) -> {
              a.addAll(b);
              return a;
            });
          }
        }
    );
    return changesByNewUsers;
  }

  /**
   * Fetches the contents of the articles in the given Map of Recent Changes Lists.
   * Then creates a displayable String of the diffs between the old and new revisions of the articles.
   *
   * @param changesPerUser A Map where the key is the user name and the value is a list of their edits.
   * @return A human-readable String of the changes by the given users.
   */
  private String formatChangesPerUser(final Map<String, List<RecentChange>> changesPerUser) {
    final StringBuffer result = new StringBuffer();
    changesPerUser.forEach((user, edits) -> {
      if (!edits.isEmpty()) {
        result.append("\nEdits of ").append(user).append(":\n");
        edits.forEach(edit -> {
          result.append('\t').append(format(edit)).append("\n");
          final long oldRevisionId = edit.getOldRevisionId();
          final long currentRevisionId = edit.getRevisionId();

          if (oldRevisionId > 0L && currentRevisionId > 0L) {
            try {
              final List<Revision> revisions = mediaWiki.fetchRevisions(edit);
              final Revision oldRevision = revisions.get(0);
              final Revision newRevision = revisions.get(1);

              final Patch<String> patch = DiffUtils.diff(
                  getContentAsList(oldRevision),
                  getContentAsList(newRevision),
                  new HistogramDiff<>());
              patch.getDeltas().forEach(delta -> result.append("\t\t").append(format(delta)).append("\n"));
            } catch (final Exception e) {
              throw new RuntimeException(e);
            }
          }
        });
      }
    });
    return result.toString();
  }

  private static String format(final RecentChange edit) {
    final String lengthDiff = new DecimalFormat("+0;-0").format(edit.getNewLength() - edit.getOldLength());
    return String.format("%s %s%s: %s (%s) %s", edit.getTimestamp(), edit.getType(), formatLogInfo(edit),
        edit.getTitle(), lengthDiff, edit.getComment());
  }

  private static String formatLogInfo(final RecentChange edit) {
    if (edit.getLogAction() == null || edit.getLogType() == null) {
      return "";
    }
    return String.format(" (%s: %s)", edit.getLogType(), edit.getLogAction());
  }

  private static String format(final AbstractDelta<String> delta) {
    if (delta.getType() == DeltaType.CHANGE) {
      return String.format("[ChangeDelta, lines:\n\t\t\t%s\n\t\t\t%s", delta.getSource(), delta.getTarget());
    }
    return delta.toString();
  }

  private List<String> getContentAsList(final Revision revision) {
    return Arrays.asList(revision.getContent().split("\n"));
  }

  public static void main(final String... args) {
    if (args == null || args.length != 1) {
      throw new IllegalArgumentException("Give the URL of the api.php as the sole argument to this program.");
    }
    final RecentChangesMonitor patrol = new RecentChangesMonitor(args[0]);
    final Map<String, List<RecentChange>> changesByNewUsers = patrol.findEditsByNewUsers();
    final String formattedEdits = patrol.formatChangesPerUser(changesByNewUsers);
    System.out.println(formattedEdits);
  }
}

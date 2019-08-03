package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

/**
 * An application to monitor the Recent Changes list of a given MediaWiki instance and to report the edits
 * of any new and anonymous users.
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
   * Finds and returns edits by new and anonymous users.
   *
   * @return A Map where the keys are user names and values are edits by those users.
   */
  public Map<String, List<RecentChange>> findEditsByNewAndAnonymousUsers() {
    final List<RecentChange> recentChanges = mediaWiki.fetchRecentChanges();
    return findChangesByNewAndAnonymousUsers(recentChanges);
  }

  /**
   * Finds any new and anonymous users, then returns the changes they made.
   *
   * @param recentChanges A list of Recent Changes.
   * @return A Map where the key is the user name and the value is a list of their edits.
   */
  private Map<String, List<RecentChange>> findChangesByNewAndAnonymousUsers(final List<RecentChange> recentChanges) {
    final Map<String, List<RecentChange>> result = new HashMap<>();
    recentChanges.forEach(rc -> {
      if (rc.getType() == RecentChange.ChangeType.NEW && rc.getTitle().startsWith(USER_SPACE_PREFIX)) {
        result.put(rc.getTitle().substring(USER_SPACE_PREFIX.length()), new ArrayList<>());
      } else if (rc.getUserId() == 0L) {
        result.put(rc.getUser(), new ArrayList<>());
      }
    });

    recentChanges.forEach(rc -> {
      if (result.keySet().contains(rc.getUser())) {
        result.merge(rc.getUser(), new ArrayList<>(Collections.singleton(rc)), (a, b) -> {
          a.addAll(b);
          return a;
        });
      }
        }
    );
    return result;
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

  private static long findLatestStoredRcId(final String wikiName) {
    return findStoredWikiValue(wikiName, "_rcId");
  }

  private static long findLatestStoredLogId(final String wikiName) {
    return findStoredWikiValue(wikiName, "_logId");
  }

  private static long findStoredWikiValue(final String wikiName, final String suffix) {
    try {
      return Long.valueOf(Files.readAllLines(Paths.get(wikiName + suffix)).get(0));
    } catch (final Exception e) {
      System.err.println(e);
      return -1;
    }
  }

  private static void storeWikiValue(final String wikiName, final String suffix, final long value) {
    try {
      Files.write(Paths.get(wikiName + suffix), Collections.singleton(String.valueOf(value)),
          WRITE, CREATE, TRUNCATE_EXISTING);
    } catch (final Exception e) {
      System.err.println(e);
    }
  }

  private static OptionalLong findMaxValue(final Map<String, List<RecentChange>> changesPerUser,
                                           final Function<RecentChange, Long> longExtractor) {
    return findMaxValue(changesPerUser.values().stream().flatMap(Collection::stream), longExtractor);
  }

  private static OptionalLong findMaxValue(final Stream<RecentChange> recentChanges,
                                           final Function<RecentChange, Long> longExtractor) {
    return recentChanges
        .map(longExtractor)
        .filter(Objects::nonNull)
        .mapToLong(l -> l)
        .max();
  }

  private static String getWikiHostName(final String apiUrl) {
    try {
      return new URL(apiUrl).getHost();
    } catch (final MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(final String... args) {
    if (args == null || args.length != 1) {
      throw new IllegalArgumentException("Give the URL of the api.php as the sole argument to this program.");
    }
    final String apiUrl = args[0];
    final RecentChangesMonitor patrol = new RecentChangesMonitor(apiUrl);
    final Map<String, List<RecentChange>> changesByNewUsers = patrol.findEditsByNewAndAnonymousUsers();

    final String wikiHostName = getWikiHostName(apiUrl);
    final long lastSeenRecentChangeId = findLatestStoredRcId(wikiHostName);
    final long lastSeenLogId = findLatestStoredLogId(wikiHostName);

    final Map<String, List<RecentChange>> filteredChangesByNewUsers = new HashMap<>();
    changesByNewUsers.forEach((user, edits) -> {
      final long maxRcId = findMaxValue(edits.stream(), RecentChange::getRecentChangeId).orElse(-1L);
      final long maxLogId = findMaxValue(edits.stream(), RecentChange::getLogId).orElse(-1L);
      if (maxRcId > lastSeenRecentChangeId || maxLogId > lastSeenLogId) {
        filteredChangesByNewUsers.put(user, edits);
      }
    });
    final String formattedEdits = patrol.formatChangesPerUser(filteredChangesByNewUsers);

    System.out.println(formattedEdits);

    final long maxRecentChangeId = findMaxValue(changesByNewUsers, RecentChange::getRecentChangeId).orElse(-1L);
    final long maxLogId = findMaxValue(changesByNewUsers, RecentChange::getLogId).orElse(-1L);

    storeWikiValue(wikiHostName, "_rcId", maxRecentChangeId);
    storeWikiValue(wikiHostName, "_logId", maxLogId);
  }
}

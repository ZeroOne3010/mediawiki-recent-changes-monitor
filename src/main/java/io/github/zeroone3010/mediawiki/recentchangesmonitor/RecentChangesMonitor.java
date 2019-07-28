package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.jgit.HistogramDiff;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
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
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiUrl;

  /**
   * @param apiUrl The URL of the api.php service of the target MediaWiki instance.
   */
  public RecentChangesMonitor(final String apiUrl) {
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper.registerModule(new JavaTimeModule());
    this.apiUrl = apiUrl;
  }

  /**
   * The main method that does everything.
   */
  public String listEditsByNewUsers() {
    final List<RecentChange> recentChanges = fetchRecentChanges();
    final Map<String, List<RecentChange>> changesByNewUsers = findChangesByNewUsers(recentChanges);
    return formatChangesByUser(changesByNewUsers);
  }

  /**
   * @return List of Recent Changes.
   */
  private List<RecentChange> fetchRecentChanges() {
    final URL recentChangesUrl;
    try {
      recentChangesUrl = new URL(apiUrl + "?action=query" +
          "&list=recentchanges" +
          "&rclimit=100" +
          "&format=json" +
          "&rcprop=user|userid|comment|title|ids|sizes|flags|timestamp|loginfo");
      return objectMapper.readValue(recentChangesUrl, QueryResponse.class).getQuery().getRecentChanges();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
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
   * @param changesByNewUsers A Map where the key is the user name and the value is a list of their edits.
   * @return A human-readable String of the changes by the given users.
   */
  private String formatChangesByUser(final Map<String, List<RecentChange>> changesByNewUsers) {
    final StringBuffer result = new StringBuffer();
    changesByNewUsers.forEach((user, edits) -> {
      if (!edits.isEmpty()) {
        result.append("\nEdits of ").append(user).append(":\n");
        edits.forEach(edit -> {
          result.append('\t').append(format(edit)).append("\n");
          final long oldRevisionId = edit.getOldRevisionId();
          final long currentRevisionId = edit.getRevisionId();

          if (oldRevisionId > 0L && currentRevisionId > 0L) {
            try {
              final List<Revision> revisions = fetchRevisions(edit);

              assert revisions.size() == 2 : "Something is not right, there should be two revisions.";

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

  /**
   * Fetches the old and new contents of the article in the given Recent Change.
   *
   * @param edit A RecentChange of whose contents one is interested in.
   * @return A List of two items where the first item is the older version
   * and the second item is the newer version of the article.
   */
  private List<Revision> fetchRevisions(final RecentChange edit) {
    final URL revisionsUrl;
    try {
      revisionsUrl = new URL(apiUrl + "?action=query" +
          "&format=json" +
          "&titles=" + encode(edit.getTitle()) +
          "&rvstartid=" + edit.getOldRevisionId() +
          "&rvendid=" + edit.getRevisionId() +
          "&rvdir=newer" +
          "&prop=revisions" +
          "&rvprop=ids|timestamp|user|comment|content" +
          "&rvdir=newer");

      final Map<Long, Page> pages = objectMapper.readValue(revisionsUrl, QueryResponse.class).getQuery().getPages();
      final List<Revision> revisions = pages.get(edit.getPageId()).getRevisions();

      assert revisions.size() == 2 : "Something is not right, there should be two revisions.";

      return pages.get(edit.getPageId()).getRevisions();
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private List<String> getContentAsList(final Revision revision) {
    return Arrays.asList(revision.getContent().split("\n"));
  }

  private static String encode(final String string) {
    try {
      return URLEncoder.encode(string, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(final String... args) {
    if (args == null || args.length != 1) {
      throw new IllegalArgumentException("Give the URL of the api.php as the sole argument to this program.");
    }
    final RecentChangesMonitor patrol = new RecentChangesMonitor(args[0]);
    System.out.println(patrol.listEditsByNewUsers());
  }
}

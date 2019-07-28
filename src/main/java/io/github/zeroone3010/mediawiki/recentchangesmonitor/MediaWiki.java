package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

class MediaWiki {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final String apiUrl;

  public MediaWiki(final String apiUrl) {
    this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    this.objectMapper.registerModule(new JavaTimeModule());
    this.apiUrl = apiUrl;
  }

  /**
   * @return List of Recent Changes.
   */
  List<RecentChange> fetchRecentChanges() {
    try {
      final URL recentChangesUrl = new URL(apiUrl + "?action=query" +
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
   * Fetches the old and new contents of the article in the given Recent Change.
   *
   * @param edit A RecentChange of whose contents one is interested in.
   * @return A List of two items where the first item is the older version
   * and the second item is the newer version of the article.
   */
  List<Revision> fetchRevisions(final RecentChange edit) {
    try {
      final URL revisionsUrl = new URL(apiUrl + "?action=query" +
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

  private static String encode(final String string) {
    try {
      return URLEncoder.encode(string, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}

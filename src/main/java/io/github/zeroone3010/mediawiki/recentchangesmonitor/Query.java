package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

class Query {
  private final List<RecentChange> recentChanges;
  private final Map<Long, Page> pages;

  @JsonCreator
  public Query(@JsonProperty("recentchanges") List<RecentChange> recentChanges,
               @JsonProperty("pages") Map<Long, Page> pages) {
    this.pages = pages;
    this.recentChanges = recentChanges;
  }

  public List<RecentChange> getRecentChanges() {
    return recentChanges;
  }

  public Map<Long, Page> getPages() {
    return pages;
  }
}

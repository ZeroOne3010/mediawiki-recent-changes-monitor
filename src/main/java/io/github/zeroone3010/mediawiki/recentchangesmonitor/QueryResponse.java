package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.annotation.JsonProperty;

class QueryResponse {
  final Query query;

  public QueryResponse(@JsonProperty("query") Query query) {
    this.query = query;
  }

  public Query getQuery() {
    return query;
  }
}

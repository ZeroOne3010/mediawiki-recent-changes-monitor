package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

public class Page {
  private final long id;
  private final int namespace;
  private final String title;
  private final List<Revision> revisions;

  @JsonCreator
  public Page(@JsonProperty("pageid") long id,
              @JsonProperty("ns") int namespace,
              @JsonProperty("title") String title,
              @JsonProperty("revisions") List<Revision> revisions) {
    this.id = id;
    this.namespace = namespace;
    this.title = title;
    this.revisions = revisions;
  }

  public long getId() {
    return id;
  }

  public int getNamespace() {
    return namespace;
  }

  public String getTitle() {
    return title;
  }

  public List<Revision> getRevisions() {
    return revisions;
  }

  @Override
  public String toString() {
    return "Page{" +
        "id=" + id +
        ", namespace=" + namespace +
        ", title='" + title + '\'' +
        ", revisions=" + revisions +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Page page = (Page) o;
    return getId() == page.getId() &&
        getNamespace() == page.getNamespace() &&
        Objects.equals(getTitle(), page.getTitle()) &&
        Objects.equals(getRevisions(), page.getRevisions());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getNamespace(), getTitle(), getRevisions());
  }
}

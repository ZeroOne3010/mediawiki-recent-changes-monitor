package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class Revision {
  private final long id;
  private final long parentId;
  private final String user;
  private final Instant timestamp;
  private final String comment;
  private final String content;

  @JsonCreator
  public Revision(@JsonProperty("revid") long id,
                  @JsonProperty("parentid") long parentId,
                  @JsonProperty("user") String user,
                  @JsonProperty("timestramp") Instant timestamp,
                  @JsonProperty("comment") String comment,
                  @JsonProperty("*") String content) {
    this.id = id;
    this.parentId = parentId;
    this.user = user;
    this.timestamp = timestamp;
    this.comment = comment;
    this.content = content;
  }

  public long getId() {
    return id;
  }

  public long getParentId() {
    return parentId;
  }

  public String getUser() {
    return user;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getComment() {
    return comment;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "Revision{" +
        "id=" + id +
        ", parentId=" + parentId +
        ", user='" + user + '\'' +
        ", timestamp=" + timestamp +
        ", comment='" + comment + '\'' +
        ", content='" + content + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Revision revision = (Revision) o;
    return getId() == revision.getId() &&
        getParentId() == revision.getParentId() &&
        Objects.equals(getUser(), revision.getUser()) &&
        Objects.equals(getTimestamp(), revision.getTimestamp()) &&
        Objects.equals(getComment(), revision.getComment()) &&
        Objects.equals(getContent(), revision.getContent());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getId(), getParentId(), getUser(), getTimestamp(), getComment(), getContent());
  }
}

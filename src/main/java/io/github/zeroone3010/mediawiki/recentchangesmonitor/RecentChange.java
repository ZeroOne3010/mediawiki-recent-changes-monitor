package io.github.zeroone3010.mediawiki.recentchangesmonitor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Objects;

public class RecentChange {
  public enum ChangeType {
    @JsonProperty("edit")
    EDIT,

    @JsonProperty("external")
    EXTERNAL,

    @JsonProperty("new")
    NEW,

    @JsonProperty("log")
    LOG,

    @JsonProperty("categorize")
    CATEGORIZE;
  }

  private final Long recentChangeId;
  private final ChangeType type;
  private final int namespace;
  private final String title;
  private final long pageId;
  private final long revisionId;
  private final long oldRevisionId;
  private final String user;
  private final long userId;
  private final long oldLength;
  private final long newLength;
  private final Instant timestamp;
  private final String comment;
  private final Long logId;
  private final String logType;
  private final String logAction;
//  private final boolean minor;
//  private final boolean anon;

  @JsonCreator
  public RecentChange(@JsonProperty("rcid") long recentChangeId,
                      @JsonProperty("type") ChangeType type,
                      @JsonProperty("ns") int namespace,
                      @JsonProperty("title") String title,
                      @JsonProperty("pageid") long pageId,
                      @JsonProperty("revid") long revisionId,
                      @JsonProperty("old_revid") long oldRevisionId,
                      @JsonProperty("user") String user,
                      @JsonProperty("userid") long userId,
                      @JsonProperty("oldlen") long oldLength,
                      @JsonProperty("newlen") long newLength,
                      @JsonProperty("timestamp") Instant timestamp,
                      @JsonProperty("comment") String comment,
                      @JsonProperty("logid") Long logId,
                      @JsonProperty("logtype") String logType,
                      @JsonProperty("logaction") String logAction
  ) {
    this.recentChangeId = recentChangeId;
    this.type = type;
    this.namespace = namespace;
    this.title = title;
    this.pageId = pageId;
    this.revisionId = revisionId;
    this.oldRevisionId = oldRevisionId;
    this.user = user;
    this.userId = userId;
    this.oldLength = oldLength;
    this.newLength = newLength;
    this.timestamp = timestamp;
    this.comment = comment;
    this.logId = logId;
    this.logType = logType;
    this.logAction = logAction;
  }

  public Long getRecentChangeId() {
    return recentChangeId;
  }

  public ChangeType getType() {
    return type;
  }

  public int getNamespace() {
    return namespace;
  }

  public String getTitle() {
    return title;
  }

  public long getPageId() {
    return pageId;
  }

  public long getRevisionId() {
    return revisionId;
  }

  public long getOldRevisionId() {
    return oldRevisionId;
  }

  public String getUser() {
    return user;
  }

  public long getUserId() {
    return userId;
  }

  public long getOldLength() {
    return oldLength;
  }

  public long getNewLength() {
    return newLength;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getComment() {
    return comment;
  }

  public Long getLogId() {
    return logId;
  }

  public String getLogType() {
    return logType;
  }

  public String getLogAction() {
    return logAction;
  }

  @Override
  public String toString() {
    return "RecentChange{" +
        "recentChangeId=" + recentChangeId +
        ", type=" + type +
        ", namespace=" + namespace +
        ", title='" + title + '\'' +
        ", pageId=" + pageId +
        ", revisionId=" + revisionId +
        ", oldRevisionId=" + oldRevisionId +
        ", user='" + user + '\'' +
        ", userId=" + userId +
        ", oldLength=" + oldLength +
        ", newLength=" + newLength +
        ", timestamp=" + timestamp +
        ", comment='" + comment + '\'' +
        ", logId=" + logId +
        ", logType='" + logType + '\'' +
        ", logAction='" + logAction + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RecentChange that = (RecentChange) o;
    return getNamespace() == that.getNamespace() &&
        getPageId() == that.getPageId() &&
        getRevisionId() == that.getRevisionId() &&
        getOldRevisionId() == that.getOldRevisionId() &&
        getUserId() == that.getUserId() &&
        getOldLength() == that.getOldLength() &&
        getNewLength() == that.getNewLength() &&
        Objects.equals(getRecentChangeId(), that.getRecentChangeId()) &&
        getType() == that.getType() &&
        Objects.equals(getTitle(), that.getTitle()) &&
        Objects.equals(getUser(), that.getUser()) &&
        Objects.equals(getTimestamp(), that.getTimestamp()) &&
        Objects.equals(getComment(), that.getComment()) &&
        Objects.equals(getLogId(), that.getLogId()) &&
        Objects.equals(getLogType(), that.getLogType()) &&
        Objects.equals(getLogAction(), that.getLogAction());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getRecentChangeId(), getType(), getNamespace(), getTitle(), getPageId(), getRevisionId(),
        getOldRevisionId(), getUser(), getUserId(), getOldLength(), getNewLength(), getTimestamp(), getComment(),
        getLogId(), getLogType(), getLogAction());
  }
}

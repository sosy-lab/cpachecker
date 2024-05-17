// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

// TODO: find out how to serialize ImmutableList such that we can mark this class as @Immutable by
// making sequence an ImmutableList
@JsonPropertyOrder({"entry_type", "metadata", "sequence"})
public class ViolationSequenceEntry extends AbstractEntry {

  private static final String VIOLATION_SEQUENCE_ENTRY_IDENTIFIER = "violation_sequence";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("content")
  private final List<SegmentRecord> content;

  public ViolationSequenceEntry(
      @JsonProperty("metadata") MetadataRecord metadata,
      @JsonProperty("sequence") List<SegmentRecord> sequence) {
    super(VIOLATION_SEQUENCE_ENTRY_IDENTIFIER);
    this.metadata = metadata;
    content = sequence;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public List<SegmentRecord> getContent() {
    return content;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((content == null) ? 0 : content.hashCode());
    result = prime * result + ((metadata == null) ? 0 : metadata.hashCode());
    return result;
  }

  @Override
  @SuppressWarnings("EqualsGetClass")
  public boolean equals(@Nullable Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther == null || getClass() != pOther.getClass()) {
      return false;
    }
    ViolationSequenceEntry other = (ViolationSequenceEntry) pOther;
    return Objects.equals(metadata, other.metadata) && Objects.equals(content, other.content);
  }
}

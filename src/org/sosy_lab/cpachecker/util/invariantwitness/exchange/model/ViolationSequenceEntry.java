// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.WaypointRecord;

// TODO: find out how to serialize ImmutableList such that we can mark this class as @Immutable by
// making sequence an ImmutableList
@JsonPropertyOrder({"entry_type", "metadata", "sequence"})
public class ViolationSequenceEntry extends AbstractEntry {

  private static final String VIOLATION_SEQUENCE_ENTRY_IDENTIFIER = "violation_sequence";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("sequence")
  private final List<WaypointRecord> sequence;

  public ViolationSequenceEntry(
      @JsonProperty("metadata") MetadataRecord metadata,
      @JsonProperty("sequence") List<WaypointRecord> sequence) {
    super(VIOLATION_SEQUENCE_ENTRY_IDENTIFIER);
    this.metadata = metadata;
    this.sequence = sequence;
  }

  public MetadataRecord getMetadata() {
    return metadata;
  }

  public List<WaypointRecord> getSequence() {
    return sequence;
  }
}

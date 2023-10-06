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
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InvariantRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;

@JsonPropertyOrder({"entry_type", "metadata", "content"})
public class InvariantSetEntry extends AbstractEntry {

  private static final String INVARIANT_SET_ENTRY_IDENTIFIER = "invariant_set";

  @JsonProperty("metadata")
  private final MetadataRecord metadata;

  @JsonProperty("content")
  private final List<InvariantRecord> content;

  public InvariantSetEntry(
      @JsonProperty("metadata") MetadataRecord pMetadata,
      @JsonProperty("content") List<InvariantRecord> pContent) {
    super(INVARIANT_SET_ENTRY_IDENTIFIER);
    metadata = pMetadata;
    content = pContent;
  }

  public List<InvariantEntry> toInvariantEntries() {
    ImmutableList.Builder<InvariantEntry> builder = ImmutableList.builder();
    for (InvariantRecord record : content) {
      InformationRecord payload =
          new InformationRecord(record.getValue(), "assertion", record.getFormat());
      switch (InvariantRecord.InvariantRecordType.fromKeyword(record.getType())) {
        case LOOP_INVARIANT:
          builder.add(new LoopInvariantEntry(metadata, record.getLocation(), payload));
          break;
        case LOCATION_INVARIANT:
          builder.add(new LocationInvariantEntry(metadata, record.getLocation(), payload));
          break;
        default:
      }
    }
    return builder.build();
  }
}

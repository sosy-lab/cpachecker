// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.errorprone.annotations.Immutable;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.InformationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.LocationRecord;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.model.records.common.MetadataRecord;

@Immutable
@JsonPropertyOrder({"entry_type", "metadata", "location", "invariant"})
public class LoopInvariantEntry extends InvariantEntry {

  private static final String LOOP_INVARIANT_ENTRY_IDENTIFIER = "loop_invariant";

  public LoopInvariantEntry(
      @JsonProperty("metadata") MetadataRecord metadata,
      @JsonProperty("location") LocationRecord location,
      @JsonProperty("invariant") InformationRecord loopInvariant) {
    super(metadata, location, loopInvariant, LOOP_INVARIANT_ENTRY_IDENTIFIER);
  }
}

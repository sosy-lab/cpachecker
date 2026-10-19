// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;

public record DssLocationInstancePayload(int nodeId, int instance) {
  @JsonValue
  public String serialize() {
    return nodeId + "," + instance;
  }

  @JsonCreator(mode = Mode.DELEGATING)
  public static DssLocationInstancePayload deserialize(String pS) {
    List<String> parts = Splitter.on(',').splitToList(pS);
    Preconditions.checkArgument(
        parts.size() == 2 && !parts.getLast().isBlank(),
        "Both a nodeId and an instance must be provided");

    return new DssLocationInstancePayload(
        Integer.parseInt(parts.getFirst()), Integer.parseInt(parts.getLast()));
  }
}

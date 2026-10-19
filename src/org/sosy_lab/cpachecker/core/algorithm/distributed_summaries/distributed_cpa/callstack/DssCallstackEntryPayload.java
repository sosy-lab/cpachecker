// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonCreator.Mode;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;

public record DssCallstackEntryPayload(int nodeId, String function) {
  @JsonValue
  public String serialize() {
    return nodeId + "." + function;
  }

  @JsonCreator(mode = Mode.DELEGATING)
  public static DssCallstackEntryPayload deserialize(String serialized) {
    List<String> parts = Splitter.on(".").limit(2).splitToList(serialized);
    Preconditions.checkArgument(parts.size() == 2);

    return new DssCallstackEntryPayload(Integer.parseInt(parts.getFirst()), parts.getLast());
  }
}

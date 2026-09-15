// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;

public record DssSSAVariablePayload(int index, String serializedType) {
  public String serialize() {
    return index + " " + serializedType;
  }

  public static DssSSAVariablePayload deserialize(String pS) {
    List<String> parts = Splitter.on(" ").limit(2).splitToList(pS);
    Preconditions.checkArgument(
        parts.size() == 2 && !parts.getLast().isBlank(),
        "Serialized SSA Variable must have index and type");
    return new DssSSAVariablePayload(Integer.parseInt(parts.getFirst()), parts.getLast());
  }
}

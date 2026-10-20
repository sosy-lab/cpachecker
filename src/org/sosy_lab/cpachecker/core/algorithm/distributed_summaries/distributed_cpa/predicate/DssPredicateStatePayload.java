// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;

@JsonPropertyOrder({
  DssPredicateStatePayload.FORMULA,
  DssPredicateStatePayload.SSA_MAP,
  DssPredicateStatePayload.POINTER_TARGET_SET,
  DssPredicateStatePayload.READABLE_FORMULA
})
public record DssPredicateStatePayload(
    @JsonProperty(FORMULA) String formula,
    @JsonProperty(SSA_MAP) ImmutableMap<String, DssSSAVariablePayload> ssaMap,
    @JsonProperty(POINTER_TARGET_SET) String pts,
    @JsonProperty(READABLE_FORMULA) @Nullable String readableFormula)
    implements DssStatePayload {
  public static final String FORMULA = "formula";
  public static final String SSA_MAP = "ssaMap";
  public static final String POINTER_TARGET_SET = "pointerTargetSet";
  public static final String READABLE_FORMULA = "readableFormula";

  @JsonCreator
  DssPredicateStatePayload(
      @JsonProperty(FORMULA) String pFormula,
      @JsonProperty(SSA_MAP) Map<String, DssSSAVariablePayload> pSsaMap,
      @JsonProperty(POINTER_TARGET_SET) String pPts,
      @JsonProperty(READABLE_FORMULA) @Nullable String pReadableFormula) {
    this(pFormula, ImmutableMap.copyOf(pSsaMap), pPts, pReadableFormula);
  }

  @JsonIgnore
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.PREDICATE_STATE;
  }

}

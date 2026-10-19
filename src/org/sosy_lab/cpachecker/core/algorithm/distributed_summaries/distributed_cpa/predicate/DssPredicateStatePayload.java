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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatePayload;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;

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

  // ObjectMapper is expensive in initialization, but thread safe;
  // so we can reuse.
  private static final ObjectMapper objectMapper = new ObjectMapper();

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

  @Override
  public ImmutableMap<String, String> asContentMap() {
    ImmutableMap<String, String> serializedSSAMap =
        ssaMap.entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey, entry -> entry.getValue().serialize()));
    StringBuilder serializedSSAMapString = new StringBuilder();

    try {
      JSON.writeJSONString(serializedSSAMap, serializedSSAMapString);
    } catch (IOException e) {
      throw new AssertionError("Unable to serialize ssa map " + ssaMap, e);
    }
    return ContentBuilder.builder()
        .pushLevel(DssComponentKey.serializedNameFor(PredicateAbstractState.class))
        .put(SerializePredicateStateOperator.STATE_KEY, formula)
        .put(SerializePredicateStateOperator.SSA_KEY, serializedSSAMapString.toString())
        .put(SerializePredicateStateOperator.PTS_KEY, pts)
        .putIf(
            readableFormula != null,
            SerializePredicateStateOperator.READABLE_KEY,
            this::readableFormula)
        .build();
  }

  public static DssPredicateStatePayload fromContentMap(ImmutableMap<String, String> pContent) {
    String prefix = DssComponentKey.serializedNameFor(PredicateAbstractState.class) + ".";

    String serializedSSAMapString = pContent.get(prefix + SerializePredicateStateOperator.SSA_KEY);
    Preconditions.checkNotNull(serializedSSAMapString, "SSA Map must be provided");
    Map<String, String> ssaMapContents;
    try {
      ssaMapContents = objectMapper.readValue(serializedSSAMapString, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse SSA map JSON", e);
    }
    Preconditions.checkNotNull(ssaMapContents, "Parsed SSA map must not be null");
    ImmutableMap<String, DssSSAVariablePayload> ssaPayloadMap =
        ssaMapContents.entrySet().stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    Map.Entry::getKey,
                    entry -> DssSSAVariablePayload.deserialize(entry.getValue())));

    return new DssPredicateStatePayload(
        Preconditions.checkNotNull(
            pContent.get(prefix + SerializePredicateStateOperator.STATE_KEY),
            "State must be provided"),
        ssaPayloadMap,
        Preconditions.checkNotNull(
            pContent.get(prefix + SerializePredicateStateOperator.PTS_KEY),
            "Pointer target set (PTS) must be provided"),
        pContent.getOrDefault(prefix + SerializePredicateStateOperator.READABLE_KEY, null));
  }
}

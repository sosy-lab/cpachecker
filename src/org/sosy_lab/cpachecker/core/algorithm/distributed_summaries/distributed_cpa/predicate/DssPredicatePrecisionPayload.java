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
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentPrecisionPayload;

@JsonPropertyOrder({
  DssPredicatePrecisionPayload.LOCATION_INSTANCES,
  DssPredicatePrecisionPayload.LOCAL_PREDICATES,
  DssPredicatePrecisionPayload.FUNCTION_PREDICATES,
  DssPredicatePrecisionPayload.GLOBAL_PREDICATES
})
public record DssPredicatePrecisionPayload(
    @JsonProperty(LOCATION_INSTANCES)
        ImmutableMap<DssLocationInstancePayload, ImmutableSet<String>> locationInstances,
    @JsonProperty(LOCAL_PREDICATES) ImmutableMap<Integer, ImmutableSet<String>> localPredicates,
    @JsonProperty(FUNCTION_PREDICATES)
        ImmutableMap<String, ImmutableSet<String>> functionPredicates,
    @JsonProperty(GLOBAL_PREDICATES) ImmutableSet<String> globalPredicates)
    implements DssComponentPrecisionPayload {
  public static final String LOCATION_INSTANCES = "locationInstances";
  public static final String LOCAL_PREDICATES = "localPredicates";
  public static final String FUNCTION_PREDICATES = "functionPredicates";
  public static final String GLOBAL_PREDICATES = "globalPredicates";

  @JsonCreator
  DssPredicatePrecisionPayload(
      @JsonProperty(LOCATION_INSTANCES)
          Map<DssLocationInstancePayload, Set<String>> pLocationInstances,
      @JsonProperty(LOCAL_PREDICATES) Map<Integer, Set<String>> pLocalPredicates,
      @JsonProperty(FUNCTION_PREDICATES) Map<String, Set<String>> pFunctionPredicates,
      @JsonProperty(GLOBAL_PREDICATES) Set<String> pGlobalPredicates) {
    this(
        copySetMap(pLocationInstances),
        copySetMap(pLocalPredicates),
        copySetMap(pFunctionPredicates),
        ImmutableSet.copyOf(pGlobalPredicates));
  }

  private static <K> ImmutableMap<K, ImmutableSet<String>> copySetMap(
      Map<K, ? extends Set<String>> pSetMap) {
    return pSetMap.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey, entry -> ImmutableSet.copyOf(entry.getValue())));
  }

  @JsonIgnore
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.PREDICATE_PRECISION;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.collect.ImmutableList;
import java.util.List;

@JsonPropertyOrder({
  DssLocationInstancePredicatesPayload.LOCATION_INSTANCE,
  DssLocationInstancePredicatesPayload.PREDICATES
})
public record DssLocationInstancePredicatesPayload(
    @JsonProperty(LOCATION_INSTANCE) DssLocationInstancePayload locationInstance,
    @JsonProperty(PREDICATES) ImmutableList<String> predicates) {
  public static final String LOCATION_INSTANCE = "locationInstance";
  public static final String PREDICATES = "predicates";

  @JsonCreator
  DssLocationInstancePredicatesPayload(
      @JsonProperty(LOCATION_INSTANCE) DssLocationInstancePayload pLocationInstance,
      @JsonProperty(PREDICATES) List<String> pPredicates) {
    this(pLocationInstance, ImmutableList.copyOf(pPredicates));
  }
}

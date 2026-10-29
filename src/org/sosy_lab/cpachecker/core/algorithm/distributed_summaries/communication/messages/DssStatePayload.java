// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DssCallstackStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositeStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicateStatePayload;

/**
 * Jackson payload for one distributed-CPA abstract state.
 *
 * <p>When introducing a new state payload, create a dedicated payload record containing all
 * information required to reconstruct the state, implement {@link #getComponentKey()} with a stable
 * serialized component key, and register the payload in {@link DssStatePayloadModule}.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = DssMessagePayload.TYPE)
@JsonSubTypes({
  @JsonSubTypes.Type(
      value = DssLocationStatePayload.class,
      name = DssComponentKey.SerializedNames.LOCATION_STATE),
  @JsonSubTypes.Type(
      value = DssBlockStatePayload.class, name = DssComponentKey.SerializedNames.BLOCK_STATE),
  @JsonSubTypes.Type(
      value = DssCallstackStatePayload.class,
      name = DssComponentKey.SerializedNames.CALLSTACK_STATE),
  @JsonSubTypes.Type(
      value = DssFunctionPointerStatePayload.class,
      name = DssComponentKey.SerializedNames.FUNCTION_POINTER_STATE),
  @JsonSubTypes.Type(
      value = DssPredicateStatePayload.class,
      name = DssComponentKey.SerializedNames.PREDICATE_STATE),
  @JsonSubTypes.Type(
      value = DssCompositeStatePayload.class,
      name = DssComponentKey.SerializedNames.COMPOSITE_STATE)
})
public interface DssStatePayload {
  String STATES = "states";
  String STATE = "state";

  DssComponentKey getComponentKey();

  default Optional<DssStatePayload> findComponent(DssComponentKey key) {
    return getComponentKey() == key ? Optional.of(this) : Optional.empty();
  }
}

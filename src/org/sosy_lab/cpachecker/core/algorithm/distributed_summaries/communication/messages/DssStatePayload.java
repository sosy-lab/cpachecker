// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import java.util.Optional;

/**
 * Jackson payload for one distributed-CPA abstract state.
 *
 * <p>When introducing a new state payload, create a dedicated payload record containing all
 * information required to reconstruct the state, implement {@link #getComponentKey()} with a stable
 * serialized component key, and register the payload in {@link DssStatePayloadModule}.
 */
@JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = DssMessagePayload.TYPE)
public interface DssStatePayload {
  String STATES = "states";
  String STATE = "state";

  DssComponentKey getComponentKey();

  default Optional<DssStatePayload> findComponent(DssComponentKey key) {
    return getComponentKey() == key ? Optional.of(this) : Optional.empty();
  }
}

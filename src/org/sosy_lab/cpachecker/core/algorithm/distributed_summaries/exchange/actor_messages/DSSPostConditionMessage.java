// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

import java.time.Instant;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSSerializeUtil;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class DSSPostConditionMessage extends DSSMessage {
  private final boolean reachable;

  DSSPostConditionMessage(
      String pUniqueBlockId, int pTargetNodeNumber, DSSMessagePayload pPayload, Instant pInstant) {
    super(MessageType.BLOCK_POSTCONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    reachable = extractFlag(DSSMessagePayload.REACHABLE, true);
  }

  public SSAMap getSSAMap() {
    if (getPayload().containsKey(DSSMessagePayload.SSA)) {
      return DSSSerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(DSSMessagePayload.SSA)), SSAMap.class);
    }
    return SSAMap.emptySSAMap();
  }

  public String getAbstractionStrategy() {
    return (String) Objects.requireNonNull(getPayload().get(DSSMessagePayload.STRATEGY));
  }

  public PointerTargetSet getPointerTargetSet() {
    if (getPayload().containsKey(DSSMessagePayload.PTS)) {
      return DSSSerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(DSSMessagePayload.PTS)),
          PointerTargetSet.class);
    }
    return PointerTargetSet.emptyPointerTargetSet();
  }

  public boolean isReachable() {
    return reachable;
  }
}

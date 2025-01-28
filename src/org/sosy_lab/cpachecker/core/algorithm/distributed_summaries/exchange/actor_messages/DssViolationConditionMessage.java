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
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssSerializeUtil;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

public class DssViolationConditionMessage extends DssMessage {

  private final boolean first;
  private final String origin;

  /** Creates a new instance of this object. */
  DssViolationConditionMessage(
      String pUniqueBlockId, int pTargetNodeNumber, DssMessagePayload pPayload) {
    this(pUniqueBlockId, pTargetNodeNumber, pPayload, null);
  }

  /**
   * Creates a new instance of this object.
   *
   * @deprecated for debug mode only. use {@link #DssViolationConditionMessage(String, int,
   *     DssMessagePayload)} instead.
   */
  @Deprecated
  DssViolationConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DssMessagePayload pPayload,
      @Nullable Instant pInstant) {
    super(MessageType.VIOLATION_CONDITION, pUniqueBlockId, pTargetNodeNumber, pPayload, pInstant);
    first = extractFlag(DssMessagePayload.FIRST, false);
    origin = (String) getPayload().getOrDefault(DssMessagePayload.ORIGIN, "");
  }

  public SSAMap getSSAMap() {
    if (getPayload().containsKey(DssMessagePayload.SSA)) {
      return DssSerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(DssMessagePayload.SSA)), SSAMap.class);
    }
    return SSAMap.emptySSAMap();
  }

  public PointerTargetSet getPointerTargetSet() {
    if (getPayload().containsKey(DssMessagePayload.PTS)) {
      return DssSerializeUtil.deserialize(
          (String) Objects.requireNonNull(getPayload().get(DssMessagePayload.PTS)),
          PointerTargetSet.class);
    }
    return PointerTargetSet.emptyPointerTargetSet();
  }

  public String getAbstractionStrategy() {
    return (String) Objects.requireNonNull(getPayload().get(BlockSummaryMessagePayload.STRATEGY));
  }

  public String getVTypes() {
    return (String)
        Objects.requireNonNullElse(getPayload().get(BlockSummaryMessagePayload.VTYPES), "");
  }

  public String getOrigin() {
    return origin;
  }

  public boolean isFirst() {
    return first;
  }
}

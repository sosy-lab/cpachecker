// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.functionpointer.FunctionPointerState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BackwardsExecutionViolationConditionSynthesizer
    implements ViolationConditionSynthesizer {

  private final TransferRelation backwardTransfer;
  private final Class<? extends AbstractState> abstractState;

  public BackwardsExecutionViolationConditionSynthesizer(
      TransferRelation pBackwardTransfer, Class<? extends AbstractState> pAbstractState) {
    backwardTransfer = pBackwardTransfer;
    abstractState = pAbstractState;
  }

  @Override
  public ViolationCondition computeViolationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws InterruptedException, CPATransferException {
    AbstractState error =
        Objects.requireNonNull(
            pPreviousCondition == null
                ? FunctionPointerState.createEmptyState()
                : AbstractStates.extractStateByType(pPreviousCondition, abstractState));
    for (CFAEdge cfaEdge : Lists.reverse(pARGPath.getFullPath())) {
      Collection<? extends AbstractState> abstractSuccessorsForEdge =
          backwardTransfer.getAbstractSuccessorsForEdge(error, new Precision() {}, cfaEdge);
      if (abstractSuccessorsForEdge.isEmpty()) {
        return ViolationCondition.infeasibleCondition();
      }
      error = Iterables.getOnlyElement(abstractSuccessorsForEdge);
    }
    return ViolationCondition.feasibleCondition(error);
  }
}

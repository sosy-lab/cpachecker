// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.timedautomata;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings.TAFormulaEncoding;

public class TAUnrollingTransferRelation implements TransferRelation {
  private final TAFormulaEncoding encoding;
  private int maxStepCount = 0;

  public TAUnrollingTransferRelation(TAFormulaEncoding pEncoding, int pMaxStepCount) {
    encoding = pEncoding;
    maxStepCount = pMaxStepCount;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {
    var taState = (TAUnrollingState) pState;
    if (taState.didReachBound()) {
      return ImmutableSet.of();
    }

    var nextStepCount = taState.getStepCount() + 1;
    var successorFormulas =
        encoding.buildSuccessorFormulas(taState.getFormula(), nextStepCount - 1);
    var didReachBound = maxStepCount >= 0 && nextStepCount >= maxStepCount;
    return successorFormulas.stream()
        .map(formula -> new TAUnrollingState(formula, nextStepCount, didReachBound))
        .collect(Collectors.toList());
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge)
      throws CPATransferException, InterruptedException {
    var taState = (TAUnrollingState) pState;
    if (taState.didReachBound()) {
      return ImmutableSet.of();
    }

    var nextStepCount = taState.getStepCount() + 1;
    var successorFormulas =
        encoding.buildSuccessorFormulas(taState.getFormula(), nextStepCount, pCfaEdge);
    var didReachBound = maxStepCount >= 0 && nextStepCount >= maxStepCount;
    return successorFormulas.stream()
        .map(formula -> new TAUnrollingState(formula, nextStepCount, didReachBound))
        .collect(Collectors.toList());
  }

  public void setStepCountBound(int pMaxStepCount) {
    maxStepCount = pMaxStepCount;
  }
}

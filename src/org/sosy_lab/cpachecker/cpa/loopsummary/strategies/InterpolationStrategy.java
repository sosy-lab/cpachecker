// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary.strategies;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class InterpolationStrategy extends AbstractStrategy {

  public InterpolationStrategy(final LogManager pLogger) {
    super(pLogger);
  }

  @Override
  public Optional<Collection<? extends AbstractState>> summarizeLoopState(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    // TODO Auto-generated method stub
    return Optional.empty();
  }

  public Optional<HashMap<String, Collection<Integer>>> getPointsForVariables(
      final int amntOfPointsPerVariable, final CFANode pLoopStartNode) {
    HashMap<String, Collection<Integer>> variablesToDataPoints = new HashMap<>();
    // TODO Calculate the Data Points for each Loop Iteration
    // If the amount of Loop unrolling is less than amntOfPointsPerVariable
    // the empty optional should be returned
    return Optional.of(variablesToDataPoints);
  }
}

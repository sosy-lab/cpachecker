/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.chc;

import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Function;
import com.google.common.base.Optional;


public class CHCPrecisionAdjustment implements PrecisionAdjustment {

  private final LogManager logger;

  public CHCPrecisionAdjustment(LogManager logM) {
    logger = logM;
  }

  @Override
  public Optional<PrecisionAdjustmentResult> prec(AbstractState successor, Precision precision,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> projection,
      AbstractState fullState) throws CPAException {

    CHCState candidateState = (CHCState)successor;

    CHCState ancestor = findVariantAncestor(candidateState);

    if (ancestor != null) {
      AbstractState newState = generalize(candidateState, ancestor);
      return Optional.of(PrecisionAdjustmentResult
          .create(newState, precision, Action.CONTINUE));
    } else {
      return Optional.of(PrecisionAdjustmentResult.create(successor, precision, Action.CONTINUE));
    }

  }

  @Override
  public Optional<PrecisionAdjustmentResult> postAdjustmentStrengthen(
      AbstractState result,
      Precision precision,
      Iterable<AbstractState> otherStates,
      Iterable<Precision> otherPrecisions,
      UnmodifiableReachedSet states,
      Function<AbstractState, AbstractState> stateProjection,
      AbstractState resultFullState) throws CPAException, InterruptedException {
    return Optional.of(PrecisionAdjustmentResult.create(result, precision, Action.CONTINUE));
  }

  private CHCState findVariantAncestor(CHCState candidateState) {
    CHCState variantAncestor = candidateState.getAncestor();
    while (variantAncestor != null) {
      if (variantAncestor.getNodeId() == candidateState.getNodeId()) {
        logger.log(Level.FINEST, "\n * variant found: " + variantAncestor.toString());
        return variantAncestor;
      }
      variantAncestor = variantAncestor.getAncestor();
    }

    return null;

  }

  /**
   * Compute a generalization of reachedState w.r.t. one of its ancestors
   */
  private AbstractState generalize(CHCState reachedState, CHCState ancestor) {
    CHCState gState = new CHCState();

    gState.setConstraint(
        ConstraintManager.generalize(ancestor.getConstraint(), reachedState.getConstraint()));
    return gState;
  }

}
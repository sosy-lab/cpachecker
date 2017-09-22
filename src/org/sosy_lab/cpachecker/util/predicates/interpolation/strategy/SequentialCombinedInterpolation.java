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
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public class SequentialCombinedInterpolation<T> extends ITPStrategy<T> {

  private final SequentialInterpolation<T> forwardItp;
  private final SequentialReverseInterpolation<T> backwardItp;

  /**
   * This strategy returns a sequence of interpolants by computing
   * - each interpolant for i={0..n-1} for the partitions A=[0 .. i] and B=[i+1 .. n] ,
   * - each interpolant for i={0..n-1} for the partitions A=[0 .. i] and B=[i+1 .. n] ,
   * - combining them by some heuristic like conjunction or choose the best interpolant.
   *
   * If one of the sub-strategies fails, we directly return the other.
   */
  public SequentialCombinedInterpolation(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr,
      BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
    forwardItp = new SequentialInterpolation<>(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
    backwardItp = new SequentialReverseInterpolation<>(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  @Override
  public List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStateAndGroupId)
      throws InterruptedException, SolverException {
    List<BooleanFormula> forward = null;
    try {
      forward = forwardItp.getInterpolants(interpolator, formulasWithStateAndGroupId);
    } catch (SolverException e) {
      logger.log(
          Level.ALL,
          "Falling back to backward interpolant, because forward interpolant caused exception:",
          e);
      return backwardItp.getInterpolants(interpolator, formulasWithStateAndGroupId);
    }

    List<BooleanFormula> backward = null;
    try {
      backward = backwardItp.getInterpolants(interpolator, formulasWithStateAndGroupId);
    } catch (SolverException e) {
      if (forward == null) {
        throw e;
      } else {
        logger.log(
            Level.ALL,
            "Falling back to forward interpolant, because backward interpolant caused exception:",
            e);
        return forward;
      }
    }

    Preconditions.checkNotNull(forward);
    Preconditions.checkNotNull(backward);

    return combine(forward, backward);
  }

  /**
   * Combine two lists of interpolants, such that the resulting list is a valid sequence of path
   * interpolants. We are allowed to choose either one or the other list or conjunct them. We are
   * not allowed to choose elements only partially, because this might violate the sequence
   * properties.
   */
  private List<BooleanFormula> combine(
      List<BooleanFormula> forward, List<BooleanFormula> backward) {
    final List<BooleanFormula> interpolants = Lists.newArrayListWithExpectedSize(forward.size());

    // TODO add further heuristics like
    // - count clauses or operations and choose the sequence of minimal/nicest/optimal interpolants.
    // - switch between strategies at each call
    for (int i = 0; i < forward.size(); i++) {
      interpolants.add(bfmgr.and(forward.get(i), backward.get(i)));
    }
    return interpolants;
  }
}

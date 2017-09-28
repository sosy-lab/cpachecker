/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

public class SequentialInterpolation<T> extends ITPStrategy<T> {

  private static final String FALLBACK_BWD_MSG =
      "Falling back to backward interpolant, because forward interpolant caused exception:";
  private static final String FALLBACK_FWD_MSG =
      "Falling back to forward interpolant, because backward interpolant caused exception:";
  private static final String UNEXPECTED_DIRECTION_MSG =
      "unexpected direction for sequential interpolation";

  public static enum SeqInterpolationStrategy {
    FWD,
    FWD_FALLBACK,
    BWD,
    BWD_FALLBACK,
    CONJUNCTION,
    WEIGHTED,
    RANDOM
  }

  private final SeqInterpolationStrategy sequentialStrategy;

  /**
   * This strategy returns a sequence of interpolants by computing
   * each interpolant for i={0..n-1} for the partitions A=[0 .. i] and B=[i+1 .. n] .
   */
  public SequentialInterpolation(LogManager pLogger, ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr, BooleanFormulaManager pBfmgr,
      SeqInterpolationStrategy pSequentialStrategy) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
    sequentialStrategy = pSequentialStrategy;
  }

  @Override
  public List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStateAndGroupId)
      throws InterruptedException, SolverException {
    final List<T> formulas = Lists.transform(formulasWithStateAndGroupId, Triple::getThird);

    switch (sequentialStrategy) {
      case FWD_FALLBACK:
        try {
          return getFwdInterpolants(interpolator, formulas);
        } catch (SolverException e) {
          logger.log(Level.ALL, FALLBACK_BWD_MSG, e);
        }
        // $FALL-THROUGH$
      case BWD:
        return getBwdInterpolants(interpolator, formulas);

      case BWD_FALLBACK:
        try {
          return getBwdInterpolants(interpolator, formulas);
        } catch (SolverException e) {
          logger.log(Level.ALL, FALLBACK_FWD_MSG, e);
        }
        // $FALL-THROUGH$
      case FWD:
        return getFwdInterpolants(interpolator, formulas);

      case CONJUNCTION:
      case WEIGHTED:
      case RANDOM:
        List<BooleanFormula> forward = null;
        try {
          forward = getFwdInterpolants(interpolator, formulas);
        } catch (SolverException e) {
          logger.log(Level.ALL, FALLBACK_BWD_MSG, e);
          return getBwdInterpolants(interpolator, formulas);
        }

        try {
          List<BooleanFormula> backward = getBwdInterpolants(interpolator, formulas);
          return combine(forward, backward);
        } catch (SolverException e) {
          if (forward == null) {
            throw e;
          } else {
            logger.log(Level.ALL, FALLBACK_FWD_MSG, e);
            return forward;
          }
        }

      default:
        throw new AssertionError(UNEXPECTED_DIRECTION_MSG);
    }
  }

  /** Compute interpolants ITP(A,B) for i={0..n-1} for the partitions A=[0 .. i] and B=[i+1 .. n] . */
  private List<BooleanFormula> getFwdInterpolants(
      final InterpolationManager.Interpolator<T> interpolator, final List<T> formulas)
      throws InterruptedException, SolverException {
    final List<BooleanFormula> interpolants =
        Lists.newArrayListWithExpectedSize(formulas.size() - 1);
    for (int end_of_A = 0; end_of_A < formulas.size() - 1; end_of_A++) {
      // last iteration is left out because B would be empty
      final int start_of_A = 0;
      interpolants.add(
          getInterpolantFromSublist(interpolator.itpProver, formulas, start_of_A, end_of_A));
    }
    return interpolants;
  }

  /** Compute interpolants ITP(B,A) for i={0..n-1} for the partitions B=[0 .. i] and A=[i+1 .. n] ,
   * then negate each interpolant. */
  private List<BooleanFormula> getBwdInterpolants(
      final InterpolationManager.Interpolator<T> interpolator, final List<T> formulas)
      throws InterruptedException, SolverException {
    final List<BooleanFormula> interpolants =
        Lists.newArrayListWithExpectedSize(formulas.size() - 1);
    for (int start_of_A = 1; start_of_A < formulas.size(); start_of_A++) {
      // first iteration is left out because B would be empty
      final int end_of_A = formulas.size() - 1;
      interpolants.add(
          bfmgr.not(
              getInterpolantFromSublist(interpolator.itpProver, formulas, start_of_A, end_of_A)));
    }
    return interpolants;
  }

  /**
   * Combine two lists of interpolants, such that the resulting list is a valid sequence of path
   * interpolants. We are allowed to choose either one or the other list or conjunct them. We are
   * not allowed to choose elements only partially, because this might violate the sequence
   * properties.
   */
  // TODO add further heuristics like
  // - count clauses or operations and choose the sequence of minimal/nicest/optimal interpolants.
  // - switch between strategies at each call
  private List<BooleanFormula> combine(
      List<BooleanFormula> forward, List<BooleanFormula> backward) {

    Preconditions.checkNotNull(forward);
    Preconditions.checkNotNull(backward);

    switch (sequentialStrategy) {
      case CONJUNCTION:
        final List<BooleanFormula> interpolants =
            Lists.newArrayListWithExpectedSize(forward.size());
        for (int i = 0; i < forward.size(); i++) {
          interpolants.add(bfmgr.and(forward.get(i), backward.get(i)));
        }
        return interpolants;

      case WEIGHTED:
        long weightFwd = getWeight(forward);
        long weightBwd = getWeight(backward);
        return weightFwd <= weightBwd ? forward : backward;

      case RANDOM:
        return Math.random() <= 0.5 ? forward : backward;

      default:
        throw new AssertionError(UNEXPECTED_DIRECTION_MSG);
    }
  }

  /**
   * Just a simple heuristic for weighting formulas depending on their structure. We assume
   * something like less variables and less operations are good, equalities are bad, comparisons are
   * better (loop invariants!), and we ignore quantifiers.
   */
  private long getWeight(List<BooleanFormula> formulas) {
    long weight = 0;
    for (BooleanFormula formula : formulas) {
      Visitor fv = new Visitor();
      fmgr.visitRecursively(formula, fv);
      weight += fv.getWeight();
    }
    return weight;
  }

  private static final class Visitor extends DefaultFormulaVisitor<TraversalProcess> {

    private long weight = 0;

    private long getWeight() {
      return weight;
    }

    @Override
    protected TraversalProcess visitDefault(Formula pF) {
      return TraversalProcess.CONTINUE;
    }

    @Override
    public TraversalProcess visitFreeVariable(Formula f, String name) {
      weight += 5;
      return visitDefault(f);
    }

    @Override
    public TraversalProcess visitConstant(Formula f, Object value) {
      weight += 1;
      return visitDefault(f);
    }

    @Override
    public TraversalProcess visitFunction(
        Formula f, List<Formula> args, FunctionDeclaration<?> functionDeclaration) {
      if (functionDeclaration.getType().isBooleanType()) {
        weight += (functionDeclaration.getKind() == FunctionDeclarationKind.EQ ? 30 : 20);
      } else {
        weight += 50;
      }
      return visitDefault(f);
    }
  }
}

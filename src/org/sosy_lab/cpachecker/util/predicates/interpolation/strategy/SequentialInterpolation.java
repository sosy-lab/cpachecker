// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Random;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;
import org.sosy_lab.java_smt.api.visitors.TraversalProcess;

@Options(prefix = "cpa.predicate.refinement")
public class SequentialInterpolation extends ITPStrategy {

  private static final String FALLBACK_BWD_MSG =
      "Falling back to backward interpolant, because forward interpolant caused exception:";
  private static final String FALLBACK_FWD_MSG =
      "Falling back to forward interpolant, because backward interpolant caused exception:";
  private static final String UNEXPECTED_DIRECTION_MSG =
      "unexpected direction for sequential interpolation";

  public enum SeqInterpolationStrategy {
    FWD,
    FWD_FALLBACK,
    BWD,
    BWD_FALLBACK,
    CONJUNCTION,
    DISJUNCTION,
    WEIGHTED,
    RANDOM
  }

  @Option(
      secure = true,
      description =
          "In case we apply sequential interpolation, "
              + "forward and backward directions return valid interpolants. "
              + "We can either choose one of the directions, fallback to the other "
              + "if one does not succeed, or even combine the interpolants.")
  private SeqInterpolationStrategy sequentialStrategy = SeqInterpolationStrategy.FWD;

  private final Random rnd = new Random(0);

  /**
   * This strategy returns a sequence of interpolants by computing each interpolant for i={0..n-1}
   * for the partitions A=[0 .. i] and B=[i+1 .. n] .
   */
  public SequentialInterpolation(
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      FormulaManagerView pFmgr,
      Configuration pConfig)
      throws InvalidConfigurationException {
    super(pLogger, pShutdownNotifier, pFmgr);
    pConfig.inject(this);
  }

  @Override
  public <T> List<BooleanFormula> getInterpolants(
      final InterpolationManager.Interpolator<T> interpolator,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStateAndGroupId)
      throws InterruptedException, SolverException {
    final List<T> formulas = projectToThird(formulasWithStateAndGroupId);

    switch (sequentialStrategy) {
      case FWD_FALLBACK:
        try {
          return getFwdInterpolants(interpolator, formulas);
        } catch (SolverException e) {
          logger.logDebugException(e, FALLBACK_BWD_MSG);
        }
        // $FALL-THROUGH$
      case BWD:
        return getBwdInterpolants(interpolator, formulas);

      case BWD_FALLBACK:
        try {
          return getBwdInterpolants(interpolator, formulas);
        } catch (SolverException e) {
          logger.logDebugException(e, FALLBACK_FWD_MSG);
        }
        // $FALL-THROUGH$
      case FWD:
        return getFwdInterpolants(interpolator, formulas);

      case CONJUNCTION:
      case DISJUNCTION:
      case WEIGHTED:
      case RANDOM:
        List<BooleanFormula> forward = null;
        try {
          forward = getFwdInterpolants(interpolator, formulas);
        } catch (SolverException e) {
          logger.logDebugException(e, FALLBACK_BWD_MSG);
          return getBwdInterpolants(interpolator, formulas);
        }

        try {
          List<BooleanFormula> backward = getBwdInterpolants(interpolator, formulas);
          return combine(forward, backward);
        } catch (SolverException e) {
          if (forward == null) {
            throw e;
          } else {
            logger.logDebugException(e, FALLBACK_FWD_MSG);
            return forward;
          }
        }

      default:
        throw new AssertionError(UNEXPECTED_DIRECTION_MSG);
    }
  }

  /**
   * Compute interpolants ITP(A,B) for i={0..n-1} for the partitions A=[0 .. i] and B=[i+1 .. n] .
   */
  private <T> List<BooleanFormula> getFwdInterpolants(
      final InterpolationManager.Interpolator<T> interpolator, final List<T> formulas)
      throws InterruptedException, SolverException {
    final ImmutableList.Builder<BooleanFormula> interpolants =
        ImmutableList.builderWithExpectedSize(formulas.size() - 1);
    for (int end_of_A = 0; end_of_A < formulas.size() - 1; end_of_A++) {
      // last iteration is left out because B would be empty
      final int start_of_A = 0;
      interpolants.add(
          getInterpolantFromSublist(interpolator.itpProver, formulas, start_of_A, end_of_A));
    }
    return interpolants.build();
  }

  /**
   * Compute interpolants ITP(B,A) for i={0..n-1} for the partitions B=[0 .. i] and A=[i+1 .. n] ,
   * then negate each interpolant.
   */
  private <T> List<BooleanFormula> getBwdInterpolants(
      final InterpolationManager.Interpolator<T> interpolator, final List<T> formulas)
      throws InterruptedException, SolverException {
    final ImmutableList.Builder<BooleanFormula> interpolants =
        ImmutableList.builderWithExpectedSize(formulas.size() - 1);
    for (int start_of_A = 1; start_of_A < formulas.size(); start_of_A++) {
      // first iteration is left out because B would be empty
      final int end_of_A = formulas.size() - 1;
      interpolants.add(
          bfmgr.not(
              getInterpolantFromSublist(interpolator.itpProver, formulas, start_of_A, end_of_A)));
    }
    return interpolants.build();
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
        {
          final ImmutableList.Builder<BooleanFormula> interpolants =
              ImmutableList.builderWithExpectedSize(forward.size());
          for (int i = 0; i < forward.size(); i++) {
            interpolants.add(bfmgr.and(forward.get(i), backward.get(i)));
          }
          return interpolants.build();
        }
      case DISJUNCTION:
        {
          final ImmutableList.Builder<BooleanFormula> interpolants =
              ImmutableList.builderWithExpectedSize(forward.size());
          for (int i = 0; i < forward.size(); i++) {
            interpolants.add(bfmgr.or(forward.get(i), backward.get(i)));
          }
          return interpolants.build();
        }
      case WEIGHTED:
        long weightFwd = getWeight(forward);
        long weightBwd = getWeight(backward);
        return weightFwd <= weightBwd ? forward : backward;

      case RANDOM:
        return rnd.nextBoolean() ? forward : backward;

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

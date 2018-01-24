/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.FluentIterable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StandardLiftings.UnsatCallback;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.DefaultBooleanFormulaVisitor;

public class AbstractionBasedLifting implements Lifting {

  private final AbstractionStrategy abstractionStrategy;

  private final LiftingAbstractionFailureStrategy lafStrategy;

  public AbstractionBasedLifting(
      AbstractionStrategy pAbstractionStrategy, LiftingAbstractionFailureStrategy pLAFStrategy) {
    abstractionStrategy = Objects.requireNonNull(pAbstractionStrategy);
    lafStrategy = Objects.requireNonNull(pLAFStrategy);
  }

  @Override
  public boolean canLift() {
    return true;
  }

  @Override
  public SingleLocationFormulaInvariant lift(
      FormulaManagerView pFMGR,
      PredicateAbstractionManager pPam,
      ProverEnvironmentWithFallback pProver,
      CounterexampleToInductivity pCti,
      AssertPredecessor pAssertPredecessor)
      throws CPATransferException, InterruptedException, SolverException {
    CFANode location = pCti.getLocation();
    BooleanFormula concreteCTIFormula = pCti.getFormula(pFMGR);

    abstractionStrategy.refinePrecision(pPam, location, pFMGR, pCti.getVariables(pFMGR));

    BooleanFormula abstractCTIFormula =
        abstractionStrategy.performAbstraction(pPam, pCti.getLocation(), concreteCTIFormula);
    SingleLocationFormulaInvariant abstractCTI =
        SingleLocationFormulaInvariant.makeLocationInvariant(
            location, abstractCTIFormula, pFMGR);

    // First, check if abstract lifting succeeds
    SuccessCheckingLiftingUnsatCallback abstractLiftingUnsatCallback = new SuccessCheckingLiftingUnsatCallback();
    SingleLocationFormulaInvariant unsatLiftedAbstractCTI =
        StandardLiftings.unsatBasedLifting(
            pFMGR,
            pProver,
            abstractCTI,
            cti -> splitLiterals(pFMGR, location, abstractCTIFormula),
            pAssertPredecessor,
            abstractLiftingUnsatCallback);
    if (abstractLiftingUnsatCallback.isSuccessful()) {
      return unsatLiftedAbstractCTI;
    }

    return lafStrategy.handleLAF(
        pFMGR, pPam, pProver, pCti, abstractCTI, pAssertPredecessor, abstractionStrategy);
  }

  private Iterable<CandidateInvariant> splitLiterals(
      FormulaManagerView pFMGR, CFANode pLocation, BooleanFormula pAbstractCTIFormula) {
    Set<BooleanFormula> operands =
        pFMGR.getBooleanFormulaManager().toConjunctionArgs(pAbstractCTIFormula, true);
    if (operands.isEmpty()) {
      return Collections.singleton(
          SingleLocationFormulaInvariant.makeLocationInvariant(
              pLocation, pAbstractCTIFormula, pFMGR));
    }
    return FluentIterable.from(operands)
        .transform(
            operand ->
                SingleLocationFormulaInvariant.makeLocationInvariant(pLocation, operand, pFMGR));
  }

  private static class SuccessCheckingLiftingUnsatCallback implements UnsatCallback {

    private boolean successful = false;

    @Override
    public void unsat(
        SingleLocationFormulaInvariant pLiftedCTI, List<Object> pCtiLiteralAssertionIds)
        throws SolverException, InterruptedException {
      successful = true;
    }

    public boolean isSuccessful() {
      return successful;
    }
  }

  private static class InterpolatingLiftingUnsatCallback
      extends SuccessCheckingLiftingUnsatCallback {

    private final ProverEnvironmentWithFallback prover;

    private @Nullable BooleanFormula interpolant = null;

    InterpolatingLiftingUnsatCallback(ProverEnvironmentWithFallback pProver) {
      prover = Objects.requireNonNull(pProver);
    }

    @Override
    public void unsat(
        SingleLocationFormulaInvariant pLiftedCTI, List<Object> pCtiLiteralAssertionIds)
        throws SolverException, InterruptedException {
      super.unsat(pLiftedCTI, pCtiLiteralAssertionIds);
      // Lifting is indeed successful, but we can do even better using interpolation
      if (prover.supportsInterpolation()) {
        interpolant = prover.getInterpolant(pCtiLiteralAssertionIds);
      }
    }

    public @Nullable BooleanFormula getInterpolant() {
      if (!isSuccessful()) {
        throw new IllegalStateException("Lifting not yet performed or unsuccessful.");
      }
      return interpolant;
    }
  }

  static interface LiftingAbstractionFailureStrategy {

    SingleLocationFormulaInvariant handleLAF(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        CounterexampleToInductivity pConcreteCti,
        SingleLocationFormulaInvariant pAbstractCTI,
        AssertPredecessor pAssertPredecessor,
        AbstractionStrategy pAbstractionStrategy)
        throws CPATransferException, InterruptedException, SolverException;
  }

  public static enum EagerRefinementLAFStrategy implements LiftingAbstractionFailureStrategy {

    INSTANCE;

    @Override
    public SingleLocationFormulaInvariant handleLAF(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        CounterexampleToInductivity pConcreteCti,
        SingleLocationFormulaInvariant pAbstractCTI,
        AssertPredecessor pAssertPredecessor,
        AbstractionStrategy pAbstractionStrategy)
        throws CPATransferException, InterruptedException, SolverException {
      CFANode location = pConcreteCti.getLocation();

      // If abstract lifting fails, check if concrete lifting succeeds (it should)
      InterpolatingLiftingUnsatCallback concreteLiftingUnsatCallback =
          new InterpolatingLiftingUnsatCallback(pProver);
      SingleLocationFormulaInvariant unsatLiftedConcreteCTI =
          StandardLiftings.unsatBasedLifting(
              pFMGR,
              pProver,
              pConcreteCti,
              cti -> pConcreteCti.splitLiterals(pFMGR, true),
              pAssertPredecessor,
              concreteLiftingUnsatCallback);
      if (concreteLiftingUnsatCallback.isSuccessful()) {
        // Abstract lifting failed, but concrete lifting succeeded
        BooleanFormula interpolant = concreteLiftingUnsatCallback.getInterpolant();
        if (interpolant != null) {
          interpolant = pFMGR.uninstantiate(interpolant);
        } else {
          interpolant = pConcreteCti.getFormula(pFMGR);
        }
        pAbstractionStrategy.refinePrecision(pPam, location, pFMGR.extractAtoms(interpolant, true));
        SingleLocationFormulaInvariant interpolantInvariant =
            SingleLocationFormulaInvariant.makeLocationInvariant(location, interpolant, pFMGR);

        SingleLocationFormulaInvariant trueAtLocation =
            SingleLocationFormulaInvariant.makeBooleanInvariant(location, true);
        if (trueAtLocation.equals(pAbstractCTI)) {
          return interpolantInvariant;
        }

        return CandidateInvariantConjunction.ofSingleLocation(pAbstractCTI, interpolantInvariant);
      }

      return unsatLiftedConcreteCTI;
    }
  }

  private static Iterable<BooleanFormula> getConjunctionOperands(FormulaManagerView pFMGR, BooleanFormula pFormula) {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    return bfmgr.visit(
        pFormula,
        new DefaultBooleanFormulaVisitor<Iterable<BooleanFormula>>() {

          @Override
          protected Iterable<BooleanFormula> visitDefault() {
            return pFMGR.splitNumeralEqualityIfPossible(pFormula);
          }

          @Override
          public Iterable<BooleanFormula> visitAnd(List<BooleanFormula> pArg0) {
            return FluentIterable.from(pArg0)
                .transformAndConcat(operand -> getConjunctionOperands(pFMGR, operand));
          }

          @Override
          public Iterable<BooleanFormula> visitNot(BooleanFormula pArg0) {
            FluentIterable<BooleanFormula> disjunctionOperands =
                FluentIterable.from(getDisjunctionOperands(pFMGR, pArg0));
            if (disjunctionOperands.skip(1).isEmpty()) {
              return disjunctionOperands;
            }
            return disjunctionOperands.transformAndConcat(
                innerOp -> getConjunctionOperands(pFMGR, bfmgr.not(innerOp)));
          }
        });
  }

  private static Iterable<BooleanFormula> getDisjunctionOperands(
      FormulaManagerView pFMGR, BooleanFormula pFormula) {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    return bfmgr.visit(
        pFormula,
        new DefaultBooleanFormulaVisitor<Iterable<BooleanFormula>>() {

          @Override
          protected Iterable<BooleanFormula> visitDefault() {
            return Collections.singleton(pFormula);
          }

          @Override
          public Iterable<BooleanFormula> visitOr(List<BooleanFormula> pArg0) {
            return FluentIterable.from(pArg0)
                .transformAndConcat(operand -> getDisjunctionOperands(pFMGR, operand));
          }

          @Override
          public Iterable<BooleanFormula> visitNot(BooleanFormula pArg0) {
            FluentIterable<BooleanFormula> conjunctionOperands =
                FluentIterable.from(getConjunctionOperands(pFMGR, pArg0));
            if (conjunctionOperands.skip(1).isEmpty()) {
              return conjunctionOperands;
            }
            return conjunctionOperands.transformAndConcat(
                innerOp -> getDisjunctionOperands(pFMGR, bfmgr.not(innerOp)));
          }
        });
  }
}

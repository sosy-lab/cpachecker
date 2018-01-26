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

import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StandardLiftings.UnsatCallback;
import org.sosy_lab.cpachecker.core.algorithm.bmc.SymbolicCandiateInvariant.BlockedCounterexampleToInductivity;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

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
  public SymbolicCandiateInvariant lift(
      FormulaManagerView pFMGR,
      PredicateAbstractionManager pPam,
      ProverEnvironmentWithFallback pProver,
      BlockedCounterexampleToInductivity pBlockedConcreteCti,
      AssertCandidate pAssertPredecessor)
      throws CPATransferException, InterruptedException, SolverException {
    CounterexampleToInductivity cti = pBlockedConcreteCti.getCti();
    BooleanFormula concreteCTIFormula = cti.getFormula(pFMGR);

    abstractionStrategy.refinePrecision(pPam, cti.getLocation(), pFMGR, cti.getVariables(pFMGR));

    BooleanFormula abstractCtiFormula =
        abstractionStrategy.performAbstraction(pPam, cti.getLocation(), concreteCTIFormula);
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BooleanFormula blockedAbstractCtiFormula = bfmgr.not(abstractCtiFormula);
    SymbolicCandiateInvariant blockedAbstractCti =
        SymbolicCandiateInvariant.makeSymbolicInvariant(
            pBlockedConcreteCti.getApplicableLocations(),
            pBlockedConcreteCti.getStateFilter(),
            blockedAbstractCtiFormula,
            pFMGR);

    // First, check if abstract lifting succeeds
    SuccessCheckingLiftingUnsatCallback abstractLiftingUnsatCallback = new SuccessCheckingLiftingUnsatCallback();
    SymbolicCandiateInvariant unsatLiftedAbstractBlockingClause =
        StandardLiftings.unsatBasedLifting(
            pFMGR,
            pProver,
            blockedAbstractCti,
            blockedAbstractCti.negate(pFMGR).splitLiterals(pFMGR, true),
            pAssertPredecessor,
            abstractLiftingUnsatCallback);
    if (abstractLiftingUnsatCallback.isSuccessful()) {
      return unsatLiftedAbstractBlockingClause;
    }

    return lafStrategy.handleLAF(
        pFMGR,
        pPam,
        pProver,
        pBlockedConcreteCti,
        blockedAbstractCti,
        pAssertPredecessor,
        abstractionStrategy);
  }

  private static class SuccessCheckingLiftingUnsatCallback implements UnsatCallback {

    private boolean successful = false;

    @Override
    public void unsat(SymbolicCandiateInvariant pLiftedCTI, List<Object> pCtiLiteralAssertionIds)
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
    public void unsat(SymbolicCandiateInvariant pLiftedCTI, List<Object> pCtiLiteralAssertionIds)
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

    SymbolicCandiateInvariant handleLAF(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        BlockedCounterexampleToInductivity pBlockedConcreteCti,
        SymbolicCandiateInvariant pBlockedAbstractCti,
        AssertCandidate pAssertPredecessor,
        AbstractionStrategy pAbstractionStrategy)
        throws CPATransferException, InterruptedException, SolverException;
  }

  public static enum EagerRefinementLAFStrategy implements LiftingAbstractionFailureStrategy {

    INSTANCE;

    @Override
    public SymbolicCandiateInvariant handleLAF(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        BlockedCounterexampleToInductivity pBlockedConcreteCti,
        SymbolicCandiateInvariant pBlockedAbstractCti,
        AssertCandidate pAssertPredecessor,
        AbstractionStrategy pAbstractionStrategy)
        throws CPATransferException, InterruptedException, SolverException {

      // If abstract lifting fails, check if concrete lifting succeeds (it should)
      InterpolatingLiftingUnsatCallback concreteLiftingUnsatCallback =
          new InterpolatingLiftingUnsatCallback(pProver);
      SymbolicCandiateInvariant unsatLiftedConcreteCTI =
          StandardLiftings.unsatBasedLifting(
              pFMGR,
              pProver,
              pBlockedConcreteCti,
              pBlockedConcreteCti.getCti().splitLiterals(pFMGR, true),
              pAssertPredecessor,
              concreteLiftingUnsatCallback);
      if (concreteLiftingUnsatCallback.isSuccessful()) {
        // Abstract lifting failed, but concrete lifting succeeded
        BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
        BooleanFormula interpolant = concreteLiftingUnsatCallback.getInterpolant();
        if (interpolant != null) {
          interpolant = pFMGR.uninstantiate(interpolant);
        } else {
          return unsatLiftedConcreteCTI;
        }
        refinePrecision(
            pAbstractionStrategy,
            pPam,
            pFMGR,
            pBlockedConcreteCti.getCti().getLocation(),
            interpolant);
        return SymbolicCandiateInvariant.makeSymbolicInvariant(
            pBlockedConcreteCti.getApplicableLocations(),
            pBlockedConcreteCti.getStateFilter(),
            bfmgr.not(
                bfmgr.and(bfmgr.not(pBlockedAbstractCti.getPlainFormula(pFMGR)), interpolant)),
            pFMGR);
      }

      return pBlockedConcreteCti;
    }
  }

  private static void refinePrecision(
      AbstractionStrategy pAbstractionStrategy,
      PredicateAbstractionManager pPam,
      FormulaManagerView pFMGR,
      CFANode pLocation,
      BooleanFormula pInterpolant) {
    pAbstractionStrategy.refinePrecision(
        pPam,
        pLocation,
        SymbolicCandiateInvariant.getConjunctionOperands(pFMGR, pInterpolant, true));
  }
}

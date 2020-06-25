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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.FluentIterable;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.StandardLiftings.UnsatCallback;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant.BlockedCounterexampleToInductivity;
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
      AssertCandidate pAssertPredecessor,
      Iterable<Object> pAssertionIds)
      throws CPATransferException, InterruptedException, SolverException {
    CounterexampleToInductivity cti = pBlockedConcreteCti.getCti();
    BooleanFormula concreteCTIFormula = cti.getFormula(pFMGR);

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
    SuccessCheckingLiftingUnsatCallback abstractLiftingUnsatCallback =
        new SuccessCheckingLiftingUnsatCallback();
    SymbolicCandiateInvariant unsatLiftedAbstractBlockingClause =
        StandardLiftings.unsatBasedLifting(
            pFMGR,
            pProver,
            blockedAbstractCti,
            blockedAbstractCti.negate(pFMGR).splitLiterals(pFMGR, false),
            pAssertPredecessor,
            pAssertionIds,
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
        pAssertionIds,
        abstractionStrategy);
  }

  private static class SuccessCheckingLiftingUnsatCallback implements UnsatCallback {

    private boolean successful = false;

    @Override
    public void unsat(
        SymbolicCandiateInvariant pLiftedCTI,
        Iterable<Object> pCtiLiteralAssertionIds,
        Iterable<Object> pOtherAssertionIds)
        throws SolverException, InterruptedException {
      successful = true;
    }

    public boolean isSuccessful() {
      return successful;
    }
  }

  private static class InterpolatingLiftingUnsatCallback
      extends SuccessCheckingLiftingUnsatCallback {

    private final FormulaManagerView fmgr;

    private final ProverEnvironmentWithFallback prover;

    private @Nullable BooleanFormula interpolant = null;

    InterpolatingLiftingUnsatCallback(
        FormulaManagerView pFmgr, ProverEnvironmentWithFallback pProver) {
      fmgr = Objects.requireNonNull(pFmgr);
      prover = Objects.requireNonNull(pProver);
    }

    @Override
    public void unsat(
        SymbolicCandiateInvariant pLiftedCTI,
        Iterable<Object> pCtiLiteralAssertionIds,
        Iterable<Object> pOtherAssertionIds)
        throws SolverException, InterruptedException {
      super.unsat(pLiftedCTI, pCtiLiteralAssertionIds, pOtherAssertionIds);
      // Lifting is indeed successful, but we can do even better using interpolation
      if (prover.supportsInterpolation()) {
        try {
          interpolant =
              fmgr.getBooleanFormulaManager().not(prover.getInterpolant(pOtherAssertionIds));
        } catch (SolverException solverException) {
          // TODO log that interpolation was switched off
        }
      }
    }

    public @Nullable BooleanFormula getInterpolant() {
      checkState(isSuccessful(), "Lifting not yet performed or unsuccessful.");
      return interpolant;
    }
  }

  public interface LiftingAbstractionFailureStrategy {

    SymbolicCandiateInvariant handleLAF(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        BlockedCounterexampleToInductivity pBlockedConcreteCti,
        SymbolicCandiateInvariant pBlockedAbstractCti,
        AssertCandidate pAssertPredecessor,
        Iterable<Object> pAssertionIds,
        AbstractionStrategy pAbstractionStrategy)
        throws CPATransferException, InterruptedException, SolverException;
  }

  public enum RefinementLAFStrategies implements LiftingAbstractionFailureStrategy {
    IGNORE {

      @Override
      public SymbolicCandiateInvariant handleLAF(
          FormulaManagerView pFMGR,
          PredicateAbstractionManager pPam,
          ProverEnvironmentWithFallback pProver,
          BlockedCounterexampleToInductivity pBlockedConcreteCti,
          SymbolicCandiateInvariant pBlockedAbstractCti,
          AssertCandidate pAssertPredecessor,
          Iterable<Object> pAssertionIds,
          AbstractionStrategy pAbstractionStrategy)
          throws CPATransferException, InterruptedException, SolverException {
        return pBlockedAbstractCti;
      }
    },

    EAGER {

      @Override
      public SymbolicCandiateInvariant handleLAF(
          FormulaManagerView pFMGR,
          PredicateAbstractionManager pPam,
          ProverEnvironmentWithFallback pProver,
          BlockedCounterexampleToInductivity pBlockedConcreteCti,
          SymbolicCandiateInvariant pBlockedAbstractCti,
          AssertCandidate pAssertPredecessor,
          Iterable<Object> pAssertionIds,
          AbstractionStrategy pAbstractionStrategy)
          throws CPATransferException, InterruptedException, SolverException {

        // If abstract lifting fails, check if concrete lifting succeeds (it should)
        InterpolatingLiftingUnsatCallback concreteLiftingUnsatCallback =
            new InterpolatingLiftingUnsatCallback(pFMGR, pProver);
        Iterable<CandidateInvariant> ctiLiterals =
            pBlockedConcreteCti.getCti().splitLiterals(pFMGR, false);
        SymbolicCandiateInvariant unsatLiftedConcreteCTI =
            StandardLiftings.unsatBasedLifting(
                pFMGR,
                pProver,
                pBlockedConcreteCti,
                ctiLiterals,
                pAssertPredecessor,
                pAssertionIds,
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

          SymbolicCandiateInvariant refinedBlockingClause =
              SymbolicCandiateInvariant.makeSymbolicInvariant(
                  pBlockedConcreteCti.getApplicableLocations(),
                  pBlockedConcreteCti.getStateFilter(),
                  bfmgr.not(
                      bfmgr.and(
                          bfmgr.not(pBlockedAbstractCti.getPlainFormula(pFMGR)), interpolant)),
                  pFMGR);
          return refinedBlockingClause;
        }
        return pBlockedConcreteCti;
      }
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
        FluentIterable.from(
                SymbolicCandiateInvariant.getConjunctionOperands(pFMGR, pInterpolant, true))
            .filter(f -> !pFMGR.getBooleanFormulaManager().isTrue(f)));
  }
}

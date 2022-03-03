// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant.BlockedCounterexampleToInductivity;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

public enum StandardLiftings implements Lifting {
  NO_LIFTING {

    @Override
    public boolean canLift() {
      return false;
    }

    @Override
    public SymbolicCandiateInvariant lift(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        BlockedCounterexampleToInductivity pBlockedConcreteCti,
        AssertCandidate pAssertPredecessor,
        Iterable<Object> pAssertionIds) {
      return pBlockedConcreteCti;
    }
  },

  UNSAT_BASED_LIFTING {

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
      return unsatBasedLifting(
          pFMGR,
          pProver,
          pBlockedConcreteCti,
          pBlockedConcreteCti.getCti().splitLiterals(pFMGR, false),
          pAssertPredecessor,
          pAssertionIds,
          DoNothingUnsatCallback.INSTANCE);
    }
  };

  public static <T extends SymbolicCandiateInvariant> SymbolicCandiateInvariant unsatBasedLifting(
      FormulaManagerView pFMGR,
      ProverEnvironmentWithFallback pProver,
      T pBlockedCti,
      Iterable<? extends CandidateInvariant> pCtiLiterals,
      AssertCandidate pAssertPredecessor,
      Iterable<Object> pAssertionIds,
      UnsatCallback pUnsatCallback)
      throws CPATransferException, InterruptedException, SolverException {

    // Note that at this point, the formula on the stack *may* already be unsatisfiable
    // in cases where the candidate invariant can in fact be refuted by a counterexample
    // of length k+1.

    Iterator<? extends CandidateInvariant> literalIterator = pCtiLiterals.iterator();
    boolean isUnsat = false;
    boolean checked = false;
    List<BooleanFormula> assertedLiterals = new ArrayList<>();
    List<Object> ctiLiteralAssertionIds = new ArrayList<>();
    while (literalIterator.hasNext() && !isUnsat) {
      CandidateInvariant literal = literalIterator.next();
      BooleanFormula literalFormula = pAssertPredecessor.assertCandidate(literal);
      for (BooleanFormula component : pFMGR.splitNumeralEqualityIfPossible(literalFormula)) {
        assertedLiterals.add(component);
        checked = false;
        ctiLiteralAssertionIds.add(pProver.push(component));
      }
      if (!pProver.supportsUnsatCoreGeneration()) {
        isUnsat = pProver.isUnsat();
        checked = true;
      }
    }
    if (!checked) {
      isUnsat = pProver.isUnsat();
      checked = true;
    }
    int pushes = ctiLiteralAssertionIds.size();
    final SymbolicCandiateInvariant liftedBlockedCti;
    if (isUnsat) {
      if (assertedLiterals.size() > 1 && pProver.supportsUnsatCoreGeneration()) {
        assertedLiterals.retainAll(pProver.getUnsatCore());
      }
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      liftedBlockedCti =
          SymbolicCandiateInvariant.makeSymbolicInvariant(
              pBlockedCti.getApplicableLocations(),
              pBlockedCti.getStateFilter(),
              pFMGR.makeNot(pFMGR.uninstantiate(bfmgr.and(assertedLiterals))),
              pFMGR);
      pUnsatCallback.unsat(liftedBlockedCti, ctiLiteralAssertionIds, pAssertionIds);
    } else {
      liftedBlockedCti = pBlockedCti;
    }
    // Pop all asserted literals
    IntStream.range(0, pushes)
        .forEach(
            i -> {
              pProver.pop();
            });
    return liftedBlockedCti;
  }

  interface UnsatCallback {

    void unsat(
        SymbolicCandiateInvariant pLiftedCTI,
        Iterable<Object> pCtiLiteralAssertionIds,
        Iterable<Object> pOtherAssertionIds)
        throws SolverException, InterruptedException;
  }

  enum DoNothingUnsatCallback implements UnsatCallback {
    INSTANCE;

    @Override
    public void unsat(
        SymbolicCandiateInvariant pLiftedCTI,
        Iterable<Object> pCtiLiteralAssertionIds,
        Iterable<Object> pOtherAssertionIds) {
      // Do nothing
    }
  }
}

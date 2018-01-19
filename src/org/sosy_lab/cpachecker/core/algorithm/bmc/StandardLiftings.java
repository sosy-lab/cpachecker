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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

enum StandardLiftings implements Lifting {

  NO_LIFTING {

    @Override
    public boolean canLift() {
      return false;
    }

    @Override
    public SingleLocationFormulaInvariant lift(
        FormulaManagerView pFMGR,
        PredicateAbstractionManager pPam,
        ProverEnvironmentWithFallback pProver,
        CounterexampleToInductivity pCti,
        AssertPredecessor pAssertPredecessor) {
      return pCti;
    }
  },

  UNSAT_BASED_LIFTING {

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
      return unsatBasedLifting(
          pFMGR,
          pProver,
          pCti,
          cti -> cti.splitLiterals(pFMGR, true),
          pAssertPredecessor,
          DoNothingUnsatCallback.INSTANCE);
    }

  };

  public static <T extends SingleLocationFormulaInvariant>
      SingleLocationFormulaInvariant unsatBasedLifting(
          FormulaManagerView pFMGR,
          ProverEnvironmentWithFallback pProver,
          T pCti,
          Function<? super T, Iterable<? extends CandidateInvariant>> pSplitLiterals,
          AssertPredecessor pAssertPredecessor,
          UnsatCallback pUnsatCallback)
          throws CPATransferException, InterruptedException, SolverException {
    Iterator<? extends CandidateInvariant> literalIterator = pSplitLiterals.apply(pCti).iterator();
    boolean isUnsat = false;
    List<BooleanFormula> assertedLiterals = new ArrayList<>();
    List<Object> ctiLiteralAssertionIds = new ArrayList<>();
    while (literalIterator.hasNext() && !isUnsat) {
      CandidateInvariant literal = literalIterator.next();
      BooleanFormula literalFormula = pAssertPredecessor.assertPredecessor(literal);
      assertedLiterals.add(literalFormula);
      ctiLiteralAssertionIds.add(pProver.push(literalFormula));
      isUnsat = pProver.isUnsat();
    }
    int pushes = assertedLiterals.size();
    final SingleLocationFormulaInvariant liftedCTI;
    if (isUnsat) {
      if (assertedLiterals.size() > 1 && pProver.supportsUnsatCoreGeneration()) {
        assertedLiterals.retainAll(pProver.getUnsatCore());
      }
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      liftedCTI =
          SingleLocationFormulaInvariant.makeLocationInvariant(
              pCti.getLocation(), pFMGR.uninstantiate(bfmgr.and(assertedLiterals)), pFMGR);
      pUnsatCallback.unsat(liftedCTI, ctiLiteralAssertionIds);
    } else {
      liftedCTI = pCti;
    }
    // Pop all asserted literals
    IntStream.range(0, pushes)
        .forEach(
            i -> {
              pProver.pop();
            });
    return liftedCTI;
  }

  static interface UnsatCallback {

    void unsat(SingleLocationFormulaInvariant pLiftedCTI, List<Object> pCtiLiteralAssertionIds)
        throws SolverException, InterruptedException;
  }

  static enum DoNothingUnsatCallback implements UnsatCallback {

    INSTANCE;

    @Override
    public void unsat(
        SingleLocationFormulaInvariant pLiftedCTI, List<Object> pCtiLiteralAssertionIds) {
      // Do nothing
    }
  }
}
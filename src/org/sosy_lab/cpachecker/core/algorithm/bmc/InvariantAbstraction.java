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

import java.util.Objects;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public interface InvariantAbstraction<S, T, D extends SuccessorViolation> {

  D getSuccessorViolation(
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      S pCandidateInvariant,
      Iterable<AbstractState> pAssertionStates)
      throws CPATransferException, InterruptedException;

  T performAbstraction(
      ProverEnvironmentWithFallback pProver,
      D pSuccessorViolation,
      Object pSuccessorViolationAssertionId);

  public static class NoAbstraction<S extends CandidateInvariant>
      implements InvariantAbstraction<S, S, SimpleSuccessorViolation<S>> {

    private static NoAbstraction<CandidateInvariant> INSTANCE = new NoAbstraction<>();

    private NoAbstraction() {}

    @Override
    public SimpleSuccessorViolation<S> getSuccessorViolation(
        FormulaManagerView pFMGR,
        PathFormulaManager pPFMGR,
        S pCandidateInvariant,
        Iterable<AbstractState> pAssertionStates)
        throws CPATransferException, InterruptedException {
      BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
      BooleanFormula successorAssertion =
          pCandidateInvariant.getAssertion(pAssertionStates, pFMGR, pPFMGR);
      return new SimpleSuccessorViolation<>(pCandidateInvariant, bfmgr.not(successorAssertion));
    }

    @Override
    public S performAbstraction(
        ProverEnvironmentWithFallback pProver,
        SimpleSuccessorViolation<S> pSuccessorViolation,
        Object pSuccessorViolationAssertionId) {
      return pSuccessorViolation.candidateInvariant;
    }

  }

  public static class SimpleSuccessorViolation<S> implements SuccessorViolation {

    private final S candidateInvariant;

    private final BooleanFormula violationAssertion;

    private SimpleSuccessorViolation(
        S pCandidateInvariant, BooleanFormula pViolationAssertion) {
      candidateInvariant = Objects.requireNonNull(pCandidateInvariant);
      violationAssertion = Objects.requireNonNull(pViolationAssertion);
    }

    @Override
    public BooleanFormula getViolationAssertion() {
      return violationAssertion;
    }
  }

  @SuppressWarnings("unchecked")
  public static <S extends CandidateInvariant>
      InvariantAbstraction<S, S, SimpleSuccessorViolation<S>> noAbstraction() {
    return (NoAbstraction<S>) NoAbstraction.INSTANCE;
  }
}

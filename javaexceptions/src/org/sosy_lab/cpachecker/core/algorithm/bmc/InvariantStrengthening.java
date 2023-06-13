// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.collect.Multimap;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.SolverException;

public interface InvariantStrengthening<
    S extends CandidateInvariant, T extends CandidateInvariant> {

  T strengthenInvariant(
      ProverEnvironmentWithFallback pProver,
      FormulaManagerView pFmgr,
      PredicateAbstractionManager pPam,
      S pInvariant,
      AssertCandidate pAssertPredecessor,
      AssertCandidate pAssertSuccessorViolation,
      AssertCandidate pAssertCti,
      Multimap<BooleanFormula, BooleanFormula> pStateViolationAssertions,
      Optional<BooleanFormula> pAssertedInvariants,
      NextCti pNextCti)
      throws SolverException, InterruptedException, CPATransferException;

  interface NextCti {

    Optional<CounterexampleToInductivity> getNextCti() throws SolverException;
  }
}

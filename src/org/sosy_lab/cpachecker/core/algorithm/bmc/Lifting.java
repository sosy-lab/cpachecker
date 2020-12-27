// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.SymbolicCandiateInvariant.BlockedCounterexampleToInductivity;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.SolverException;

public interface Lifting {

  boolean canLift();

  SymbolicCandiateInvariant lift(
      FormulaManagerView pFMGR,
      PredicateAbstractionManager pPam,
      ProverEnvironmentWithFallback pProver,
      BlockedCounterexampleToInductivity pBlockedConcreteCti,
      AssertCandidate pAssertPredecessor,
      Iterable<Object> pAssertionIds)
      throws CPATransferException, InterruptedException, SolverException;
}

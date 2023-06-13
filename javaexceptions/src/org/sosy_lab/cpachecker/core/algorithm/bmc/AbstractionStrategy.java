// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.SolverException;

public interface AbstractionStrategy {

  BooleanFormula performAbstraction(
      PredicateAbstractionManager pPam, CFANode pLocation, BooleanFormula pFormula)
      throws InterruptedException, SolverException;

  void refinePrecision(
      PredicateAbstractionManager pPam, CFANode pLocation, Iterable<BooleanFormula> pPredicates);

  void refinePrecision(PredicateAbstractionManager pPam, Iterable<BooleanFormula> pPredicates);

  void refinePrecision(
      PredicateAbstractionManager pPam,
      CFANode pLocation,
      FormulaManagerView pFMGR,
      Set<Formula> pVariables);

  enum NoAbstraction implements AbstractionStrategy {
    INSTANCE {

      @Override
      public BooleanFormula performAbstraction(
          PredicateAbstractionManager pPam, CFANode pLocation, BooleanFormula pFormula) {
        return pFormula;
      }

      @Override
      public void refinePrecision(
          PredicateAbstractionManager pPam,
          CFANode pLocation,
          Iterable<BooleanFormula> pPredicates) {
        // Do nothing
      }

      @Override
      public void refinePrecision(
          PredicateAbstractionManager pPam, Iterable<BooleanFormula> pPredicates) {
        // Do nothing
      }

      @Override
      public void refinePrecision(
          PredicateAbstractionManager pPam,
          CFANode pLocation,
          FormulaManagerView pFMGR,
          Set<Formula> pVariables) {
        // Do nothing
      }
    }
  }
}

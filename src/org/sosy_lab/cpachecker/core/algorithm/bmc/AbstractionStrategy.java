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

  public static enum NoAbstraction implements AbstractionStrategy {

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
    };
  }
}

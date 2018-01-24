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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.SolverException;

public class PredicateAbstractionStrategy implements AbstractionStrategy {

  private final Map<PredicateAbstractionManager, Multimap<CFANode, AbstractionPredicate>>
      precision = new HashMap<>();

  private final SetMultimap<FormulaType<Formula>, Formula> seenVariables = HashMultimap.create();

  private Multimap<CFANode, AbstractionPredicate> getPrecision(PredicateAbstractionManager pPam) {
    Multimap<CFANode, AbstractionPredicate> pamPrecision = precision.get(pPam);
    if (pamPrecision == null) {
      pamPrecision = HashMultimap.create();
      precision.put(pPam, pamPrecision);
    }
    return pamPrecision;
  }

  @Override
  public BooleanFormula performAbstraction(
      PredicateAbstractionManager pPam, CFANode pLocation, BooleanFormula pFormula)
      throws InterruptedException, SolverException {
    return pPam.computeAbstraction(pFormula, getPrecision(pPam).get(pLocation));
  }

  @Override
  public void refinePrecision(
      PredicateAbstractionManager pPam, CFANode pLocation, Iterable<BooleanFormula> pPredicates) {
    Multimap<CFANode, AbstractionPredicate> pamPrecision = getPrecision(pPam);
    for (BooleanFormula pPredicate : pPredicates) {
      pamPrecision.putAll(pLocation, pPam.getPredicatesForAtomsOf(pPredicate));
    }
  }

  @Override
  public void refinePrecision(
      PredicateAbstractionManager pPam,
      CFANode pLocation,
      FormulaManagerView pFMGR,
      Set<Formula> pVariables) {
    if (pVariables.isEmpty()) {
      return;
    }
    Multimap<CFANode, AbstractionPredicate> pamPrecision = getPrecision(pPam);
    for (Formula variable : pVariables) {
      FormulaType<Formula> variableType = pFMGR.getFormulaType(variable);
      Set<Formula> seenVariablesOfSameType = seenVariables.get(variableType);
      if (!seenVariablesOfSameType.contains(variable)) {
        for (Formula previouslySeenVariable : seenVariablesOfSameType) {
          BooleanFormula leq = pFMGR.makeLessOrEqual(variable, previouslySeenVariable, true);
          pamPrecision.put(pLocation, pPam.getPredicateFor(leq));
          BooleanFormula geq = pFMGR.makeGreaterOrEqual(variable, previouslySeenVariable, true);
          pamPrecision.put(pLocation, pPam.getPredicateFor(geq));
        }
        seenVariables.put(variableType, variable);
      }
    }
  }
}

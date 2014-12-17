/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.wp.segkro.rules;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Implementation of the "SUB" inference rule:
 *    Substitution of equalities.
 */
public class SubstitutionRule extends AbstractRule {

  public SubstitutionRule(FormulaManager pFm, Solver pSolver) {
    super(pFm, pSolver);
  }

  private boolean equalFormulas(Formula f1, Formula f2) {
    throw new UnsupportedOperationException("Implement me");
  }

  private Map<Formula, Formula> getEqualitiesFrom(BooleanFormula f) {
    throw new UnsupportedOperationException("Implement me");
  }

  @Override
  public Set<BooleanFormula> apply(List<BooleanFormula> pConjunctiveInputPredicates) {

    Set<BooleanFormula> result = Sets.newHashSet();
    Map<Formula, Formula> varEqualExpressionMap = Maps.newHashMap();

    // Create an index of the equalities
    for (BooleanFormula conjunctive: pConjunctiveInputPredicates) {
      varEqualExpressionMap.putAll(getEqualitiesFrom(conjunctive));
    }

    // Perform the substitution
    for (BooleanFormula conjunctive: pConjunctiveInputPredicates) {
      BooleanFormula substResult = fm.getUnsafeFormulaManager().substitute(conjunctive, varEqualExpressionMap);
      result.add(substResult);
    }

    return result;
  }

  @Override
  public Set<BooleanFormula> apply(BooleanFormula pInput) {
    return apply(Lists.newArrayList(pInput));
  }
}

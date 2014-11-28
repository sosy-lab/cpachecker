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
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Implementation of the "EQ" inference rule:
 *    Equality of arithmetic expressions; simplification.
 */
public class EquivalenceRule extends AbstractRule {

  public EquivalenceRule(FormulaManager pFm, Solver pSolver) {
    super(pFm, pSolver);
  }

  @Override
  public Set<BooleanFormula> apply(List<BooleanFormula> pConjunctiveInputPredicates) {

    Set<BooleanFormula> result = Sets.newHashSet();
    HashMultimap<NumeralFormula, NumeralFormula> isLessEqualThan = HashMultimap.create();

    // Create an index
    for (BooleanFormula conjunctive: pConjunctiveInputPredicates) {
      isLessEqualThan.putAll(extractIsLessEqualThans(conjunctive));
    }

    // Infer the equalities (SAT check could be used)
    for (NumeralFormula lessOrEqual: isLessEqualThan.keySet()) {
      Set<NumeralFormula> greaterOrEqual = isLessEqualThan.get(lessOrEqual);
      if (greaterOrEqual.size() > 1) {

      }
    }

    return result;
  }

  private Multimap<? extends NumeralFormula, ? extends NumeralFormula> extractIsLessEqualThans(
      BooleanFormula pConjunctive) {
    throw new UnsupportedOperationException("Implement me");
  }
}

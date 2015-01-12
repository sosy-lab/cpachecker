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
package org.sosy_lab.cpachecker.util.precondition.segkro.interfaces;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public interface Rule {

  /**
   * @return  Return a human-readable name for the rule.
   */
  public String getRuleName();

  /**
   * Apply the rule for an arbitrary boolean formula that also might contain disjunctions.
   *
   * @param pInput  An arbitrary boolean formula
   * @return  Set of boolean formulas (predicates) that can be concluded based on the rule.
   */
  public Set<BooleanFormula> apply(BooleanFormula pInput) throws SolverException, InterruptedException;

  /**
   * Apply the rule and return a set of boolean formulas that can be concluded from the input.
   *
   * @param pConjunctiveInputPredicates
   *        A set of boolean formulas (predicates) that do not contain disjunctions.
   * @return
   *        Set of boolean formulas (predicates) that can be concluded based on the rule.
   */
  public Set<BooleanFormula> apply(Collection<BooleanFormula> pConjunctiveInputPredicates) throws SolverException, InterruptedException;

  public Set<BooleanFormula> applyWithInputRelatingPremises(List<BooleanFormula> pConjunctiveInputPredicates) throws SolverException, InterruptedException;

  public ImmutableList<Premise> getPremises();

  public Set<BooleanFormula> apply(Collection<BooleanFormula> pConjunctiveInputPredicates,
      Multimap<String, Formula> pMatchingBindings) throws SolverException, InterruptedException;
}

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
package org.sosy_lab.cpachecker.util.precondition.segkro.rules;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.sosy_lab.solver.SolverException;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Premise;
import org.sosy_lab.cpachecker.util.precondition.segkro.interfaces.Rule;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public abstract class AbstractRule implements Rule {

  protected final Solver solver;
  protected final SmtAstMatcher matcher;
  protected final List<Premise> premises;

  public AbstractRule(Solver pSolver, SmtAstMatcher pMatcher) {
    this.solver = pSolver;
    this.matcher = pMatcher;
    this.premises = Lists.newArrayList();
  }

  @Override
  public String getRuleName() {
    return getClass().getSimpleName();
  }

  @Override
  public Set<BooleanFormula> apply(BooleanFormula pInput) throws SolverException, InterruptedException {
    throw new UnsupportedOperationException("Implement me in the child class if needed!");
  }

  @Override
  public Set<BooleanFormula> apply(Collection<BooleanFormula> pConjunctiveInputPredicates) {
    throw new UnsupportedOperationException("Implement me in the child class if needed!");
  }

  @Override
  public Set<BooleanFormula> applyWithInputRelatingPremises(List<BooleanFormula> pConjunctiveInputPredicates) throws SolverException, InterruptedException {
    throw new UnsupportedOperationException("Implement me in the child class if needed!");
  }

  @Override
  public ImmutableList<Premise> getPremises() {
    return ImmutableList.copyOf(premises);
  }

  @Override
  public String toString() {
    return getRuleName();
  }

}

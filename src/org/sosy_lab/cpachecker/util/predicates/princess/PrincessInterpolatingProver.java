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
package org.sosy_lab.cpachecker.util.predicates.princess;

import ap.parser.IExpression;
import ap.parser.IFormula;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.sosy_lab.cpachecker.core.counterexample.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static com.google.common.collect.FluentIterable.from;

public class PrincessInterpolatingProver implements InterpolatingProverEnvironment<Integer> {

  protected final PrincessFormulaManager mgr;
  private PrincessEnvironment env;

  private final List<Integer> assertedFormulas; // Collection of termNames
  private final Map<Integer, IFormula> annotatedTerms; // Collection of termNames
  private static int counter = 0; // for different indices // TODO static?

  public PrincessInterpolatingProver(PrincessFormulaManager pMgr) {
    mgr = pMgr;
    env = mgr.createEnvironment();
    assertedFormulas = new ArrayList<>();
    annotatedTerms = new HashMap<>();
  }

  @Override
  public Integer push(BooleanFormula f) {
    Preconditions.checkNotNull(env);
    IFormula t = (IFormula)mgr.getTerm(f);
    Integer termIndex = counter++;

    env.push(1);
    env.assertTermInPartition(t, termIndex);

    assertedFormulas.add(termIndex);
    annotatedTerms.put(termIndex, t);
    assert assertedFormulas.size() == annotatedTerms.size();
    return termIndex;
  }

  @Override
  public void pop() {
    Preconditions.checkNotNull(env);
    Integer removed = assertedFormulas.remove(assertedFormulas.size()-1); // remove last term
    annotatedTerms.remove(removed);
    assert assertedFormulas.size() == annotatedTerms.size();
    env.pop(1);
  }

  @Override
  public boolean isUnsat() throws InterruptedException {
    return !env.checkSat();
  }

  @Override
  public BooleanFormula getInterpolant(List<Integer> pTermNamesOfA) {
    Preconditions.checkNotNull(env);

    Set<Integer> indizesOfA = new HashSet<>(pTermNamesOfA);

    // calc difference: termNamesOfB := assertedFormulas - termNamesOfA
    Set<Integer> indizesOfB = from(assertedFormulas)
                                 .filter(not(in(indizesOfA)))
                                 .toSet();

    // get interpolant of groups
    List<IFormula> itp = env.getInterpolants(Lists.newArrayList(indizesOfA, indizesOfB));
    assert itp.size() == 1; // 2 groups -> 1 interpolant

    return mgr.encapsulateBooleanFormula(itp.get(0));
  }

  @Override
  public void close() {
    Preconditions.checkNotNull(env);
    assert assertedFormulas.size() == annotatedTerms.size();
    if (!assertedFormulas.isEmpty()) {
      env.pop(assertedFormulas.size());
      assertedFormulas.clear();
      annotatedTerms.clear();
    }
    env = null;
  }

  @Override
  public Model getModel() {
    Preconditions.checkNotNull(env);
    assert assertedFormulas.size() == annotatedTerms.size();

    final List<IExpression> values = new ArrayList<>();
    for (IFormula val : annotatedTerms.values()) {
      values.add(val);
    }

    return PrincessModel.createModel(mgr, values);
  }
}
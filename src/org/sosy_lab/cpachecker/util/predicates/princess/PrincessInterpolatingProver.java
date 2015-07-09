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

import static com.google.common.base.Predicates.*;
import static com.google.common.collect.FluentIterable.from;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;

import ap.parser.IExpression;
import ap.parser.IFormula;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

class PrincessInterpolatingProver extends PrincessAbstractProver implements InterpolatingProverEnvironment<Integer> {

  private final List<Integer> assertedFormulas = new ArrayList<>(); // Collection of termNames
  private final Map<Integer, IFormula> annotatedTerms = new HashMap<>(); // Collection of termNames
  private static final UniqueIdGenerator counter = new UniqueIdGenerator(); // for different indices

  PrincessInterpolatingProver(PrincessFormulaManager pMgr) {
    super(pMgr, true);
  }

  @Override
  public Integer push(BooleanFormula f) {
    IFormula t = (IFormula)mgr.extractInfo(f);
    int termIndex = counter.getFreshId();

    stack.push(1);
    stack.assertTermInPartition(t, termIndex);

    assertedFormulas.add(termIndex);
    annotatedTerms.put(termIndex, t);
    assert assertedFormulas.size() == annotatedTerms.size();
    return termIndex;
  }

  @Override
  public void pop() {
    Integer removed = assertedFormulas.remove(assertedFormulas.size()-1); // remove last term
    annotatedTerms.remove(removed);
    assert assertedFormulas.size() == annotatedTerms.size();
    stack.pop(1);
  }

  @Override
  public BooleanFormula getInterpolant(List<Integer> pTermNamesOfA) {

    Set<Integer> indizesOfA = new HashSet<>(pTermNamesOfA);

    // calc difference: termNamesOfB := assertedFormulas - termNamesOfA
    Set<Integer> indizesOfB = from(assertedFormulas)
                                 .filter(not(in(indizesOfA)))
                                 .toSet();

    // get interpolant of groups
    List<IFormula> itp = stack.getInterpolants(ImmutableList.of(indizesOfA, indizesOfB));
    assert itp.size() == 1; // 2 groups -> 1 interpolant

    return mgr.encapsulateBooleanFormula(itp.get(0));
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(final List<Set<Integer>> pTermNamesOfA) {

    // get interpolant of groups
    final List<IFormula> itps = stack.getInterpolants(pTermNamesOfA);

    final List<BooleanFormula> result = new ArrayList<>();
    for (final IFormula itp : itps) {
      result.add(mgr.encapsulateBooleanFormula(itp));
    }
    return result;
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(List<Set<Integer>> partitionedFormulas, int[] startOfSubTree) {
    throw new UnsupportedOperationException("directly receiving of tree interpolants is not supported." +
        "Use another solver or another strategy for interpolants.");
  }

  @Override
  public Model getModel() {
    assert assertedFormulas.size() == annotatedTerms.size();
    final List<IExpression> values = Lists.<IExpression>newArrayList(annotatedTerms.values());
    return PrincessModel.createModel(stack, values);
  }
}
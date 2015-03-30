/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.constraints;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.constraints.util.DependencyGraph;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicExpression;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.SymbolicValueFactory;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;

import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for {@link org.sosy_lab.cpachecker.cpa.constraints.util.DependencyGraph}.
 */
public class ConstraintsStateTest {

  private final SymbolicValueFactory factory = SymbolicValueFactory.getInstance();

  private final Type intType = CNumericTypes.INT;

  private final SymbolicExpression const1 = factory.asConstant(new NumericValue(5), intType);

  private final SymbolicExpression id1 = factory.asConstant(factory.newIdentifier(), intType);
  private final SymbolicExpression id2 = factory.asConstant(factory.newIdentifier(), intType);
  private final SymbolicExpression id3 = factory.asConstant(factory.newIdentifier(), intType);
  private final SymbolicExpression alias1 = factory.asConstant(factory.newIdentifier(), intType);
  private final SymbolicExpression alias2 = factory.asConstant(factory.newIdentifier(), intType);
  private final SymbolicExpression alias3 = factory.asConstant(factory.newIdentifier(), intType);

  private final SymbolicExpression exp1 = factory.add(id1, id2, intType, intType);
  private final SymbolicExpression exp2 = factory.lessThanOrEqual(id2, id3, intType, intType);

  private final SymbolicExpression aliasExp1 = factory.add(alias1, alias2, intType, intType);
  private final SymbolicExpression aliasExp2 = factory.lessThanOrEqual(alias2, alias3, intType,
      intType);


  @Test
  public void testIsLessOrEqual_emptyGraph() {
    DependencyGraph allGraph = getAllGraph();
    DependencyGraph noneGraph = new DependencyGraph(Collections.<SymbolicExpression>emptySet());

    Assert.assertTrue(allGraph.isLessOrEqual(noneGraph));
    Assert.assertFalse(noneGraph.isLessOrEqual(allGraph));
  }

  @Test
  public void testIsLessOrEqual_reflexive() {
    DependencyGraph allGraph = getAllGraph();

    Assert.assertTrue(allGraph.isLessOrEqual(allGraph));
  }

  @Test
  public void testIsLessOrEqual_supersetGraph() {
    DependencyGraph graph = getAllGraph();

    SymbolicExpression additionalExpression = factory.lessThanOrEqual(alias3, alias2, intType,
        intType);

    DependencyGraph stricterGraph = new DependencyGraph(
        ImmutableSet.of(alias1, alias3, aliasExp1, aliasExp2, additionalExpression));

    Assert.assertTrue(stricterGraph.isLessOrEqual(graph));
    Assert.assertFalse(graph.isLessOrEqual(stricterGraph));
  }

  @Test
  public void testIsLessOrEqual_sameGraphWithOtherIdentifiers() {
    DependencyGraph allGraph = getAllGraph();
    DependencyGraph aliasGraph = getAllAliasGraph();

    Assert.assertTrue(allGraph.isLessOrEqual(aliasGraph));
    Assert.assertTrue(aliasGraph.isLessOrEqual(allGraph));
  }

  private DependencyGraph getAllGraph() {
    Set<SymbolicExpression> allLinked = ImmutableSet.of(id1, exp1, exp2, id3);

    return new DependencyGraph(allLinked);
  }

  private DependencyGraph getAllAliasGraph() {
    Set<SymbolicExpression> allLinked = ImmutableSet.of(alias1, aliasExp1, aliasExp2, alias3);

    return new DependencyGraph(allLinked);
  }
}

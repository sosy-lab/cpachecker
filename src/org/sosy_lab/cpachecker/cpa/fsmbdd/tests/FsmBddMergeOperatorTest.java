/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.fsmbdd.tests;

import junit.framework.Assert;

import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.FsmBddMergeOperator;
import org.sosy_lab.cpachecker.cpa.fsmbdd.FsmBddState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.FsmBddStatistics;
import org.sosy_lab.cpachecker.exceptions.CPAException;


public class FsmBddMergeOperatorTest extends FsmBddTest {

  @Test
  public void testMergeOnlyOnEqualCond() throws CPAException, InvalidConfigurationException {
    FsmBddMergeOperator mergeOp = new FsmBddMergeOperator(null, new FsmBddStatistics(bddfactory));

    mergeOp.mergeOnlyOnEqualConditions = false;

    FsmBddState s1 = new FsmBddState(bddfactory, l1);
    FsmBddState s2 = new FsmBddState(bddfactory, l1);
    FsmBddState s3 = new FsmBddState(bddfactory, l1);
    FsmBddState s4 = new FsmBddState(bddfactory, l1);

    s1.declareGlobal("v1", domainInterval.getIntervalMaximum());
    s1.declareGlobal("v2", domainInterval.getIntervalMaximum());
    s1.declareGlobal("v3", domainInterval.getIntervalMaximum());

    CBinaryExpression comp1 = new CBinaryExpression(null, null, v2, int9, BinaryOperator.EQUALS);
    CBinaryExpression comp2 = new CBinaryExpression(null, null, v3, int7, BinaryOperator.EQUALS);

    // State s1: (v1=7, v2=9)
    s1.assingConstantToVariable("v1", domainInterval, int7);
    s1.addToConditionBlock(comp1, true);

    // State s2: (v1=7, v2=9 && v3=7)
    s2.assingConstantToVariable("v1", domainInterval, int7);
    s2.addToConditionBlock(comp1, true);
    s2.addToConditionBlock(comp2, true);

    // State s3: (v1=7, v2=9 || v3=7)
    s3.assingConstantToVariable("v1", domainInterval, int7);
    s3.addToConditionBlock(comp1, true);
    s3.addToConditionBlock(comp2, false);

    // State s4: (v1=7, TRUE)
    s4.assingConstantToVariable("v1", domainInterval, int7);
    s4.resetConditionBlock();

    AbstractState joined = mergeOp.merge(s1, s2, null);
    Assert.assertNotSame(joined, s2);
    Assert.assertNotSame(joined, s1);
  }

}

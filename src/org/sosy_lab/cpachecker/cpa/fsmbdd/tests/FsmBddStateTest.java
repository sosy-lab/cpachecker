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
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFileLocation;
import org.sosy_lab.cpachecker.cpa.fsmbdd.FsmBddState;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.UnrecognizedSyntaxException;
import org.sosy_lab.cpachecker.cpa.fsmbdd.exceptions.VariableDeclarationException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


public class FsmBddStateTest extends FsmBddTesting {

  @Test
  public void testCloneState_NoConditions() throws CPATransferException {
    FsmBddState state1 = new FsmBddState(bddfactory, l1);
    state1.declareGlobal("v1", domainInterval.getIntervalMaximum());
    state1.assingConstantToVariable("v1", domainInterval, int7);

    FsmBddState clone1 = state1.cloneState(l1);

    Assert.assertEquals(state1.getConditionBlockIsTrue(), clone1.getConditionBlockIsTrue());
    Assert.assertEquals(state1.getStateBdd(), clone1.getStateBdd());
  }

  @Test
  public void testCloneState_Conditions() throws CPATransferException {
    CFileLocation loc = new CFileLocation(2, "test.c", 12, 200, 1);
    CBinaryExpression bin = new CBinaryExpression(loc, null, v2, int8, BinaryOperator.EQUALS);
    FsmBddState state1 = new FsmBddState(bddfactory, l1);
    state1.declareGlobal("v1", domainInterval.getIntervalMaximum());
    state1.assingConstantToVariable("v1", domainInterval, int7);
    state1.conjunctToConditionBlock(bin);

    FsmBddState clone1 = state1.cloneState(l1);

    Assert.assertEquals(state1.getConditionBlockIsTrue(), clone1.getConditionBlockIsTrue());
    Assert.assertEquals(state1.getStateBdd(), clone1.getStateBdd());
  }

  @Test
  public void testGetConditionBlockIsTrue() throws CPATransferException {
    CFileLocation loc = new CFileLocation(2, "test.c", 12, 200, 1);
    CBinaryExpression bin = new CBinaryExpression(loc, null, v2, int8, BinaryOperator.EQUALS);

    FsmBddState state1 = new FsmBddState(bddfactory, l1);
    state1.declareGlobal("v1", domainInterval.getIntervalMaximum());
    state1.assingConstantToVariable("v1", domainInterval, int7);

    Assert.assertTrue(state1.getConditionBlockIsTrue());

    state1.conjunctToConditionBlock(bin);
    Assert.assertFalse(state1.getConditionBlockIsTrue());
  }

  @Test
  public void testDisjunctConditionBlocks() throws VariableDeclarationException, CPATransferException {
    CFileLocation loc = new CFileLocation(2, "test.c", 12, 200, 1);
    CBinaryExpression bin1 = new CBinaryExpression(loc, null, v2, int8, BinaryOperator.EQUALS);

    FsmBddState state1 = new FsmBddState(bddfactory, l1);
    state1.declareGlobal("v1", domainInterval.getIntervalMaximum());
    state1.assingConstantToVariable("v1", domainInterval, int7);
    state1.conjunctToConditionBlock(bin1);

    FsmBddState state2 = state1.cloneState(l1);
    state2.resetConditionBlock();

    Assert.assertFalse(state1.getConditionBlockIsTrue());
    Assert.assertTrue(state2.getConditionBlockIsTrue());
    state1.disjunctConditionBlocks(state2.getConditionBlock());
    Assert.assertTrue(state1.getConditionBlockIsTrue());
  }

  @Test
  public void testResetConditionBlock() throws VariableDeclarationException, CPATransferException {
    CFileLocation loc = new CFileLocation(2, "test.c", 12, 200, 1);
    CBinaryExpression bin1 = new CBinaryExpression(loc, null, v2, int8, BinaryOperator.EQUALS);

    FsmBddState state1 = new FsmBddState(bddfactory, l1);
    state1.declareGlobal("v1", domainInterval.getIntervalMaximum());
    state1.assingConstantToVariable("v1", domainInterval, int7);
    state1.conjunctToConditionBlock(bin1);

    Assert.assertFalse(state1.getConditionBlockIsTrue());
    state1.resetConditionBlock();
    Assert.assertTrue(state1.getConditionBlockIsTrue());
  }

  @Test
  public void testCondBlockEqualToBlockOf() throws UnrecognizedSyntaxException {
    CFileLocation loc = new CFileLocation(2, "test.c", 12, 200, 1);
    CBinaryExpression bin1 = new CBinaryExpression(loc, null, v2, int8, BinaryOperator.EQUALS);

    FsmBddState state1 = new FsmBddState(bddfactory, l1);
    FsmBddState state2 = state1.cloneState(l1);

    Assert.assertTrue(state1.condBlockEqualToBlockOf(state2));
    state1.conjunctToConditionBlock(bin1);
    Assert.assertFalse(state1.condBlockEqualToBlockOf(state2));
  }

}

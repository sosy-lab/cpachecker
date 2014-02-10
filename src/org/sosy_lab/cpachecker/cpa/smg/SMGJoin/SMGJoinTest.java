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
package org.sosy_lab.cpachecker.cpa.smg.SMGJoin;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class SMGJoinTest {
  static private final CFunctionType functionType = AnonymousTypes.createSimpleFunctionType(AnonymousTypes.dummyInt);
  static private final CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(null, functionType, "foo", ImmutableList.<CParameterDeclaration>of());

  private CLangSMG smg1;
  private CLangSMG smg2;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    smg1 = new CLangSMG(MachineModel.LINUX64);
    smg2 = new CLangSMG(MachineModel.LINUX64);
  }

  // Testing condition: adds an identical global variable to both SMGs
  private void addGlobalWithoutValueToBoth(String pVarName) {
    SMGRegion global1 = new SMGRegion(8, pVarName);
    SMGRegion global2 = new SMGRegion(8, pVarName);

    smg1.addGlobalObject(global1);
    smg2.addGlobalObject(global2);
  }

  // Testing condition: adds an identical local variable to both SMGs
  private void addLocalWithoutValueToBoth(String pVarName) {
    SMGRegion local1 = new SMGRegion(8, pVarName);
    SMGRegion local2 = new SMGRegion(8, pVarName);

    smg1.addStackObject(local1);
    smg2.addStackObject(local2);
  }

  // Testing condition: adds an identical global variable to both SMGs, with value
  private void addGlobalWithValueToBoth(String pVarName) {
    SMGRegion global1 = new SMGRegion(8, pVarName);
    SMGRegion global2 = new SMGRegion(8, pVarName);
    Integer value1 = SMGValueFactory.getNewValue();
    Integer value2 = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(4, 0, global1, value1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(4, 0, global2, value2);

    smg1.addGlobalObject(global1);
    smg2.addGlobalObject(global2);
    smg1.addValue(value1);
    smg2.addValue(value2);
    smg1.addHasValueEdge(hv1);
    smg2.addHasValueEdge(hv2);
  }

  // Testing condition: adds an identical local value to both SMGs, with value
  private void addLocalWithValueToBoth(String pVarName) {
    SMGRegion local1 = new SMGRegion(8, pVarName);
    SMGRegion local2 = new SMGRegion(8, pVarName);
    Integer value1 = SMGValueFactory.getNewValue();
    Integer value2 = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(4, 0, local1, value1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(4, 0, local2, value2);

    smg1.addStackObject(local1);
    smg2.addStackObject(local2);
    smg1.addValue(value1);
    smg2.addValue(value2);
    smg1.addHasValueEdge(hv1);
    smg2.addHasValueEdge(hv2);
  }

  private void assertObjectCounts(CLangSMG pSMG, int pGlobals, int pHeap, int pFrames) {
    Assert.assertEquals(pSMG.getGlobalObjects().size(), pGlobals);
    Assert.assertEquals(pSMG.getHeapObjects().size(), pHeap);
    Assert.assertEquals(pSMG.getStackFrames().size(), pFrames);
  }

  @Test
  public void simpleGlobalVarJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    addGlobalWithoutValueToBoth(varName);
    SMGJoin join = new SMGJoin(smg1, smg2);
    Assert.assertTrue(join.isDefined());
    Assert.assertEquals(join.getStatus(), SMGJoinStatus.EQUAL);

    CLangSMG resultSMG = join.getJointSMG();
    Assert.assertTrue(resultSMG.getGlobalObjects().containsKey(varName));
    assertObjectCounts(resultSMG, 1, 1, 0);
  }

  @Test
  public void simpleLocalVarJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    smg1.addStackFrame(functionDeclaration);
    smg2.addStackFrame(functionDeclaration);
    addLocalWithoutValueToBoth(varName);

    SMGJoin join = new SMGJoin(smg1, smg2);
    Assert.assertTrue(join.isDefined());
    Assert.assertEquals(join.getStatus(), SMGJoinStatus.EQUAL);

    CLangSMG resultSMG = join.getJointSMG();
    Assert.assertTrue(resultSMG.getStackFrames().getFirst().containsVariable(varName));
    assertObjectCounts(resultSMG, 0, 1, 1);
  }

  @Test
  public void globalVarWithValueJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    addGlobalWithValueToBoth(varName);
    SMGJoin join = new SMGJoin(smg1, smg2);
    Assert.assertTrue(join.isDefined());
    Assert.assertEquals(join.getStatus(), SMGJoinStatus.EQUAL);

    CLangSMG resultSMG = join.getJointSMG();
    Assert.assertTrue(resultSMG.getGlobalObjects().containsKey(varName));
    assertObjectCounts(resultSMG, 1, 1, 0);

    SMGObject global = resultSMG.getGlobalObjects().get(varName);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(global).filterAtOffset(0);
    Set<SMGEdgeHasValue> edges = resultSMG.getHVEdges(filter);
    SMGEdgeHasValue edge = Iterables.getOnlyElement(edges);
    Assert.assertTrue(resultSMG.getValues().contains(Integer.valueOf(edge.getValue())));
  }

  @Test
  public void localVarWithValueJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    smg1.addStackFrame(functionDeclaration);
    smg2.addStackFrame(functionDeclaration);
    addLocalWithValueToBoth(varName);
    SMGJoin join = new SMGJoin(smg1, smg2);
    Assert.assertTrue(join.isDefined());
    Assert.assertEquals(join.getStatus(), SMGJoinStatus.EQUAL);

    CLangSMG resultSMG = join.getJointSMG();
    Assert.assertTrue(resultSMG.getStackFrames().getFirst().containsVariable(varName));
    assertObjectCounts(resultSMG, 0, 1, 1);

    SMGObject global = resultSMG.getStackFrames().getFirst().getVariable(varName);
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(global).filterAtOffset(0);
    Set<SMGEdgeHasValue> edges = resultSMG.getHVEdges(filter);
    SMGEdgeHasValue edge = Iterables.getOnlyElement(edges);
    Assert.assertTrue(resultSMG.getValues().contains(Integer.valueOf(edge.getValue())));
  }

  private void joinUpdateUnit(SMGJoinStatus firstOperand, SMGJoinStatus forLe, SMGJoinStatus forRe) {
    Assert.assertEquals(firstOperand, SMGJoinStatus.updateStatus(firstOperand, SMGJoinStatus.EQUAL));
    Assert.assertEquals(forLe, SMGJoinStatus.updateStatus(firstOperand, SMGJoinStatus.LEFT_ENTAIL));
    Assert.assertEquals(forRe, SMGJoinStatus.updateStatus(firstOperand, SMGJoinStatus.RIGHT_ENTAIL));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinStatus.updateStatus(firstOperand, SMGJoinStatus.INCOMPARABLE));
  }

  @Test
  public void joinUpdateTest() {
    joinUpdateUnit(SMGJoinStatus.EQUAL, SMGJoinStatus.LEFT_ENTAIL,
        SMGJoinStatus.RIGHT_ENTAIL);
    joinUpdateUnit(SMGJoinStatus.LEFT_ENTAIL, SMGJoinStatus.LEFT_ENTAIL,
        SMGJoinStatus.INCOMPARABLE);
    joinUpdateUnit(SMGJoinStatus.RIGHT_ENTAIL, SMGJoinStatus.INCOMPARABLE,
        SMGJoinStatus.RIGHT_ENTAIL);
    joinUpdateUnit(SMGJoinStatus.INCOMPARABLE, SMGJoinStatus.INCOMPARABLE,
        SMGJoinStatus.INCOMPARABLE);
  }
}

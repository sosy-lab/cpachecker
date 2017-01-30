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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.util.Pair;

import java.util.Set;

public class SMGJoinTest {
  static private final CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  static private final CFunctionDeclaration functionDeclaration = new CFunctionDeclaration(FileLocation.DUMMY, functionType, "foo", ImmutableList.<CParameterDeclaration>of());
  static private final CFunctionDeclaration functionDeclaration2 = new CFunctionDeclaration(FileLocation.DUMMY, functionType, "bar", ImmutableList.<CParameterDeclaration>of());
  static private final CFunctionDeclaration functionDeclaration3 = new CFunctionDeclaration(FileLocation.DUMMY, functionType, "main", ImmutableList.<CParameterDeclaration>of());

  SMGState dummyState = new SMGState(LogManager.createTestLogManager(), MachineModel.LINUX32, false, false,
      null, 4, false, false);

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
    SMGRegion global1 = new SMGRegion(64, pVarName);
    SMGRegion global2 = new SMGRegion(64, pVarName);

    smg1.addGlobalObject(global1);
    smg2.addGlobalObject(global2);
  }

  // Testing condition: adds an identical global variable to both SMGs
  private Pair<SMGRegion, SMGRegion> addGlobalWithoutValueToBoth(String pVarName, int size) {
    SMGRegion global1 = new SMGRegion(size, pVarName);
    SMGRegion global2 = new SMGRegion(size, pVarName);

    smg1.addGlobalObject(global1);
    smg2.addGlobalObject(global2);

    return Pair.of(global1, global2);
  }

  // Testing condition: adds an identical local variable to both SMGs
  private Pair<SMGRegion, SMGRegion> addLocalWithoutValueToBoth(String pVarName, int size) {
    SMGRegion local1 = new SMGRegion(size, pVarName);
    SMGRegion local2 = new SMGRegion(size, pVarName);

    smg1.addStackObject(local1);
    smg2.addStackObject(local2);

    return Pair.of(local1, local2);
  }

  // Testing condition: adds an identical local variable to both SMGs
  private Pair<SMGRegion, SMGRegion> addHeapWithoutValueToBoth(String pVarName, int size) {
    SMGRegion local1 = new SMGRegion(size, pVarName);
    SMGRegion local2 = new SMGRegion(size, pVarName);

    smg1.addHeapObject(local1);
    smg2.addHeapObject(local2);

    return Pair.of(local1, local2);
  }

  //Testing condition: adds an identical value to both SMGs
  private void addValueToBoth(Pair<? extends SMGObject, ? extends SMGObject> var, int pOffset,
      int pValue, int pSizeInBits) {

    if(!smg1.getValues().contains(pValue)) {
      smg1.addValue(pValue);
    }

    if(!smg2.getValues().contains(pValue)) {
      smg2.addValue(pValue);
    }

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(pSizeInBits, pOffset, var.getFirst(), pValue);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(pSizeInBits, pOffset, var.getSecond(), pValue);

    smg1.addHasValueEdge(hv1);
    smg2.addHasValueEdge(hv2);
  }

  //Testing condition: adds a pointer to both SMGs
  private void addPointerToBoth(Pair<? extends SMGObject, ? extends SMGObject> target, int pOffset,
      int pValue) {

    if(!smg1.getValues().contains(pValue)) {
      smg1.addValue(pValue);
    }

    if(!smg2.getValues().contains(pValue)) {
      smg2.addValue(pValue);
    }

    SMGEdgePointsTo pt1 = new SMGEdgePointsTo(pValue, target.getFirst(), pOffset);
    SMGEdgePointsTo pt2 = new SMGEdgePointsTo(pValue, target.getSecond(), pOffset);

    smg1.addPointsToEdge(pt1);
    smg2.addPointsToEdge(pt2);
  }

  //Testing condition: adds a pointer to both SMGs
  private void addPointerValueToBoth(Pair<? extends SMGObject, ? extends SMGObject> var,
      int pOffset, int pValue, int pSize,
      Pair<? extends SMGObject, ? extends SMGObject> target, int pTargetOffset) {

    addValueToBoth(var, pOffset, pValue, pSize);
    addPointerToBoth(target, pTargetOffset, pValue);
  }

  // Testing condition: adds an identical local variable to both SMGs
  private void addLocalWithoutValueToBoth(String pVarName) {
    SMGRegion local1 = new SMGRegion(64, pVarName);
    SMGRegion local2 = new SMGRegion(64, pVarName);

    smg1.addStackObject(local1);
    smg2.addStackObject(local2);
  }

  // Testing condition: adds an identical global variable to both SMGs, with value
  private void addGlobalWithValueToBoth(String pVarName) {
    SMGRegion global1 = new SMGRegion(64, pVarName);
    SMGRegion global2 = new SMGRegion(64, pVarName);
    Integer value1 = SMGValueFactory.getNewValue();
    Integer value2 = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(32, 0, global1, value1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(32, 0, global2, value2);

    smg1.addGlobalObject(global1);
    smg2.addGlobalObject(global2);
    smg1.addValue(value1);
    smg2.addValue(value2);
    smg1.addHasValueEdge(hv1);
    smg2.addHasValueEdge(hv2);
  }

  // Testing condition: adds an identical local value to both SMGs, with value
  private void addLocalWithValueToBoth(String pVarName) {
    SMGRegion local1 = new SMGRegion(64, pVarName);
    SMGRegion local2 = new SMGRegion(64, pVarName);
    Integer value1 = SMGValueFactory.getNewValue();
    Integer value2 = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(32, 0, local1, value1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(32, 0, local2, value2);

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
    SMGJoin join = new SMGJoin(smg1, smg2, null, null);
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

    SMGJoin join = new SMGJoin(smg1, smg2, null, null);
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
    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
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
    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
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

  @Test
  public void complexJoinTestNoAbstraction() throws SMGInconsistentException {

    smg1.addStackFrame(functionDeclaration3);
    smg2.addStackFrame(functionDeclaration3);
    Pair<SMGRegion, SMGRegion> global = addGlobalWithoutValueToBoth("global", 64);
    Pair<SMGRegion, SMGRegion> l1 = addHeapWithoutValueToBoth("l1", 96);
    Pair<SMGRegion, SMGRegion> l2 = addHeapWithoutValueToBoth("l2", 96);
    Pair<SMGRegion, SMGRegion> l3 = addHeapWithoutValueToBoth("l3", 96);
    Pair<SMGRegion, SMGRegion> l4 = addHeapWithoutValueToBoth("l4", 96);
    addPointerValueToBoth(global, 0, 100, 32, l1, 0);
    addPointerValueToBoth(l1, 0, 102, 32, l2, 0);
    addPointerValueToBoth(l2, 0, 103, 32, l3, 0);
    addPointerValueToBoth(l3, 0, 104, 32, l4, 0);
    addPointerValueToBoth(l4, 0, 109, 32, global, 0);
    addPointerValueToBoth(global, 32, 105, 32, l4, 0);
    addPointerValueToBoth(l4, 32, 106, 32, l3, 0);
    addPointerValueToBoth(l3, 32, 107, 32, l2, 0);
    addPointerValueToBoth(l2, 32, 108, 32, l1, 0);
    addPointerValueToBoth(l1, 32, 110, 32, global, 0);
    addValueToBoth(l1, 64, 5, 8);
    addValueToBoth(l2, 64, 5, 8);
    addValueToBoth(l3, 64, 5, 8);
    addValueToBoth(l4, 64, 5, 8);
    Pair<SMGRegion, SMGRegion> a1 = addLocalWithoutValueToBoth("a", 32);
    addValueToBoth(a1, 0, 5, 32);
    Pair<SMGRegion, SMGRegion> b1 = addLocalWithoutValueToBoth("b", 32);
    addValueToBoth(b1, 0, 100, 32);
    smg1.addStackFrame(functionDeclaration2);
    smg2.addStackFrame(functionDeclaration2);
    Pair<SMGRegion, SMGRegion> b2 = addLocalWithoutValueToBoth("b", 32);
    addValueToBoth(b2, 0, 100, 32);
    Pair<SMGRegion, SMGRegion> c2 = addLocalWithoutValueToBoth("c", 32);
    addValueToBoth(c2, 0, 104, 32);
    smg1.addStackFrame(functionDeclaration);
    smg2.addStackFrame(functionDeclaration);
    Pair<SMGRegion, SMGRegion> a3 = addLocalWithoutValueToBoth("a", 32);
    addValueToBoth(a3, 0, 5, 32);
    Pair<SMGRegion, SMGRegion> c3 = addLocalWithoutValueToBoth("c", 32);
    addValueToBoth(c3, 0, 104, 32);

    SMGJoin join = new SMGJoin(smg1, smg2, null, null);
    Assert.assertTrue(join.isDefined());
    Assert.assertEquals(join.getStatus(), SMGJoinStatus.EQUAL);
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

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.util.Pair;

public class SMGJoinTest {
  private static final CFunctionType functionType =
      CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  private static final CFunctionDeclaration functionDeclaration =
      new CFunctionDeclaration(
          FileLocation.DUMMY, functionType, "foo", ImmutableList.of(), ImmutableSet.of());
  private static final CFunctionDeclaration functionDeclaration2 =
      new CFunctionDeclaration(
          FileLocation.DUMMY, functionType, "bar", ImmutableList.of(), ImmutableSet.of());
  private static final CFunctionDeclaration functionDeclaration3 =
      new CFunctionDeclaration(
          FileLocation.DUMMY, functionType, "main", ImmutableList.of(), ImmutableSet.of());

  private SMGState dummyState;

  private CLangSMG smg1;
  private CLangSMG smg2;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws InvalidConfigurationException {
    dummyState =
        new SMGState(
            LogManager.createTestLogManager(),
            MachineModel.LINUX32,
            new SMGOptions(Configuration.defaultConfiguration()));
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

  // Testing condition: adds an identical value to both SMGs
  private void addValueToBoth(
      Pair<? extends SMGObject, ? extends SMGObject> var,
      long pOffset,
      int pValue,
      int pSizeInBits) {
    SMGValue value = SMGKnownExpValue.valueOf(pValue);
    smg1.addValue(value);
    smg2.addValue(value);
    smg1.addHasValueEdge(new SMGEdgeHasValue(pSizeInBits, pOffset, var.getFirst(), value));
    smg2.addHasValueEdge(new SMGEdgeHasValue(pSizeInBits, pOffset, var.getSecond(), value));
  }

  // Testing condition: adds a pointer to both SMGs
  private void addPointerToBoth(
      Pair<? extends SMGObject, ? extends SMGObject> target, long pOffset, int pValue) {
    SMGValue value = SMGKnownExpValue.valueOf(pValue);
    smg1.addValue(value);
    smg2.addValue(value);
    smg1.addPointsToEdge(new SMGEdgePointsTo(value, target.getFirst(), pOffset));
    smg2.addPointsToEdge(new SMGEdgePointsTo(value, target.getSecond(), pOffset));
  }

  // Testing condition: adds a pointer to both SMGs
  private void addPointerValueToBoth(
      Pair<? extends SMGObject, ? extends SMGObject> var,
      long pOffset,
      int pValue,
      int pSize,
      Pair<? extends SMGObject, ? extends SMGObject> target,
      long pTargetOffset) {

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
    SMGValue value1 = SMGKnownSymValue.of();
    SMGValue value2 = SMGKnownSymValue.of();
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
    SMGValue value1 = SMGKnownSymValue.of();
    SMGValue value2 = SMGKnownSymValue.of();
    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(32, 0, local1, value1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(32, 0, local2, value2);

    smg1.addStackObject(local1);
    smg2.addStackObject(local2);
    smg1.addValue(value1);
    smg2.addValue(value2);
    smg1.addHasValueEdge(hv1);
    smg2.addHasValueEdge(hv2);
  }

  private void assertObjectCounts(UnmodifiableCLangSMG pSMG, int pGlobals, int pHeap, int pFrames) {
    assertThat(pSMG.getGlobalObjects()).hasSize(pGlobals);
    assertThat(pSMG.getHeapObjects()).hasSize(pHeap);
    assertThat(pSMG.getStackFrames()).hasSize(pFrames);
  }

  @Test
  public void simpleGlobalVarJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    addGlobalWithoutValueToBoth(varName);
    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);

    UnmodifiableCLangSMG resultSMG = join.getJointSMG();
    assertThat(resultSMG.getGlobalObjects()).containsKey(varName);
    assertObjectCounts(resultSMG, 1, 1, 0);
  }

  @Test
  public void simpleLocalVarJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    smg1.addStackFrame(functionDeclaration);
    smg2.addStackFrame(functionDeclaration);
    addLocalWithoutValueToBoth(varName);

    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);

    UnmodifiableCLangSMG resultSMG = join.getJointSMG();
    assertThat(Iterables.get(resultSMG.getStackFrames(), 0).containsVariable(varName)).isTrue();
    assertObjectCounts(resultSMG, 0, 1, 1);
  }

  @Test
  public void globalVarWithValueJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    addGlobalWithValueToBoth(varName);
    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);

    UnmodifiableCLangSMG resultSMG = join.getJointSMG();
    assertThat(resultSMG.getGlobalObjects()).containsKey(varName);
    assertObjectCounts(resultSMG, 1, 1, 0);

    SMGObject global = resultSMG.getGlobalObjects().get(varName);
    SMGEdgeHasValueFilter filter =
        SMGEdgeHasValueFilter.objectFilter(global).filterAtOffset(0).filterWithoutSize();
    SMGEdgeHasValue edge = Iterables.getOnlyElement(resultSMG.getHVEdges(filter));
    assertThat(resultSMG.getValues()).contains(edge.getValue());
  }

  @Test
  public void localVarWithValueJoinTest() throws SMGInconsistentException {
    String varName = "variableName";
    smg1.addStackFrame(functionDeclaration);
    smg2.addStackFrame(functionDeclaration);
    addLocalWithValueToBoth(varName);
    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);

    UnmodifiableCLangSMG resultSMG = join.getJointSMG();
    assertThat(Iterables.get(resultSMG.getStackFrames(), 0).containsVariable(varName)).isTrue();
    assertObjectCounts(resultSMG, 0, 1, 1);

    SMGObject global = Iterables.get(resultSMG.getStackFrames(), 0).getVariable(varName);
    SMGEdgeHasValueFilter filter =
        SMGEdgeHasValueFilter.objectFilter(global).filterAtOffset(0).filterWithoutSize();
    SMGEdgeHasValue edge = Iterables.getOnlyElement(resultSMG.getHVEdges(filter));
    assertThat(resultSMG.getValues()).contains(edge.getValue());
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

    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
  }

  private void joinUpdateUnit(
      SMGJoinStatus firstOperand, SMGJoinStatus forLe, SMGJoinStatus forRe) {
    assertThat(firstOperand.updateWith(SMGJoinStatus.EQUAL)).isEqualTo(firstOperand);
    assertThat(firstOperand.updateWith(SMGJoinStatus.LEFT_ENTAIL)).isEqualTo(forLe);
    assertThat(firstOperand.updateWith(SMGJoinStatus.RIGHT_ENTAIL)).isEqualTo(forRe);
    assertThat(firstOperand.updateWith(SMGJoinStatus.INCOMPARABLE))
        .isEqualTo(SMGJoinStatus.INCOMPARABLE);
  }

  @Test
  public void joinUpdateTest() {
    joinUpdateUnit(SMGJoinStatus.EQUAL, SMGJoinStatus.LEFT_ENTAIL, SMGJoinStatus.RIGHT_ENTAIL);
    joinUpdateUnit(
        SMGJoinStatus.LEFT_ENTAIL, SMGJoinStatus.LEFT_ENTAIL, SMGJoinStatus.INCOMPARABLE);
    joinUpdateUnit(
        SMGJoinStatus.RIGHT_ENTAIL, SMGJoinStatus.INCOMPARABLE, SMGJoinStatus.RIGHT_ENTAIL);
    joinUpdateUnit(
        SMGJoinStatus.INCOMPARABLE, SMGJoinStatus.INCOMPARABLE, SMGJoinStatus.INCOMPARABLE);
  }

  // tests, whether the SMGJoinFields has an appropriate effect on the join status
  @Test
  public void nullifiedBlocksJoinTest() throws SMGInconsistentException {
    final int mockType4bSize = 32;
    smg1.addStackFrame(functionDeclaration3);
    smg2.addStackFrame(functionDeclaration3);

    Pair<SMGRegion, SMGRegion> objs = addHeapWithoutValueToBoth("Object", 64);

    // more general
    smg1.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 0, objs.getFirst(), SMGZeroValue.INSTANCE));

    smg2.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 0, objs.getSecond(), SMGZeroValue.INSTANCE));
    smg2.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 32, objs.getSecond(), SMGZeroValue.INSTANCE));

    Pair<SMGRegion, SMGRegion> global = addGlobalWithoutValueToBoth("global", 128);
    addPointerValueToBoth(global, 0, 100, 32, objs, 0);

    SMGJoin join = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.RIGHT_ENTAIL);

    // this will lead to incomparable, undefined, you can not join 0(ptr valuu) with 666(nonptr val)
    // one might expect SMGJoinValues.joinValuesNonPointers to be used,
    // but 0 is considered a pointer value ( SMG.isPointer() )
    // this join fails due to SMGJoinValues not due to SMGJoinFields!
    SMGValue un = SMGKnownSymValue.valueOf(666);
    smg1.addValue(un);
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 32, objs.getFirst(), un));

    SMGJoin join2 = new SMGJoin(smg1, smg2, dummyState, dummyState);
    assertThat(join2.isDefined()).isFalse();
    assertThat(join2.getStatus()).isEqualTo(SMGJoinStatus.INCOMPARABLE);
  }
}

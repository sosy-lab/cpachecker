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
package org.sosy_lab.cpachecker.cpa.smg;

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
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGExpressionEvaluator.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedList;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;


public class SMGStateTest {
  static private final  LogManager logger = LogManager.createTestLogManager();
  private SMGState consistent_state;
  private SMGState inconsistent_state;

  static private final CType mockType16b = AnonymousTypes.createTypeWithLength(16);
  static private final CType mockType8b = AnonymousTypes.createTypeWithLength(8);

  private final CFunctionType functionType = CFunctionType.functionTypeWithReturnType(CNumericTypes.UNSIGNED_LONG_INT);
  private final CFunctionDeclaration functionDeclaration3 = new CFunctionDeclaration(FileLocation.DUMMY, functionType, "main", ImmutableList.<CParameterDeclaration>of());
  private CSimpleType unspecifiedType = new CSimpleType(false, false, CBasicType.UNSPECIFIED, false, false, true, false, false, false, false);
  private CType pointerType = new CPointerType(false, false, unspecifiedType);

  @Test
  public void abstractionTest() throws SMGInconsistentException {

    CLangSMG smg1 = new CLangSMG(MachineModel.LINUX32);

    smg1.addStackFrame(functionDeclaration3);

    SMGValueFactory.prepareForTest();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();

    SMGRegion l1 = new SMGRegion(96, "l1");
    SMGRegion l2 = new SMGRegion(96, "l2");
    SMGRegion l3 = new SMGRegion(96, "l3");
    SMGRegion l4 = new SMGRegion(96, "l4");
    SMGRegion l5 = new SMGRegion(96, "l5");

    SMGEdgeHasValue l1fn = new SMGEdgeHasValue(pointerType, 0, l1, 7);
    SMGEdgeHasValue l2fn = new SMGEdgeHasValue(pointerType, 0, l2, 8);
    SMGEdgeHasValue l3fn = new SMGEdgeHasValue(pointerType, 0, l3, 9);
    SMGEdgeHasValue l4fn = new SMGEdgeHasValue(pointerType, 0, l4, 10);
    SMGEdgeHasValue l5fn = new SMGEdgeHasValue(pointerType, 0, l5, 5);

    SMGEdgeHasValue l1fp = new SMGEdgeHasValue(pointerType, 4, l1, 5);
    SMGEdgeHasValue l2fp = new SMGEdgeHasValue(pointerType, 4, l2, 6);
    SMGEdgeHasValue l3fp = new SMGEdgeHasValue(pointerType, 4, l3, 7);
    SMGEdgeHasValue l4fp = new SMGEdgeHasValue(pointerType, 4, l4, 8);
    SMGEdgeHasValue l5fp = new SMGEdgeHasValue(pointerType, 4, l5, 9);

    SMGEdgePointsTo l1t = new SMGEdgePointsTo(6, l1, 0);
    SMGEdgePointsTo l2t = new SMGEdgePointsTo(7, l2, 0);
    SMGEdgePointsTo l3t = new SMGEdgePointsTo(8, l3, 0);
    SMGEdgePointsTo l4t = new SMGEdgePointsTo(9, l4, 0);
    SMGEdgePointsTo l5t = new SMGEdgePointsTo(10, l5, 0);

    smg1.addHeapObject(l1);
    smg1.addHeapObject(l2);
    smg1.addHeapObject(l3);
    smg1.addHeapObject(l4);
    smg1.addHeapObject(l5);

    smg1.addValue(5);
    smg1.addValue(6);
    smg1.addValue(7);
    smg1.addValue(8);
    smg1.addValue(9);
    smg1.addValue(10);

    smg1.addHasValueEdge(l1fn);
    smg1.addHasValueEdge(l2fn);
    smg1.addHasValueEdge(l3fn);
    smg1.addHasValueEdge(l4fn);
    smg1.addHasValueEdge(l5fn);

    smg1.addHasValueEdge(l1fp);
    smg1.addHasValueEdge(l2fp);
    smg1.addHasValueEdge(l3fp);
    smg1.addHasValueEdge(l4fp);
    smg1.addHasValueEdge(l5fp);

    smg1.addPointsToEdge(l1t);
    smg1.addPointsToEdge(l2t);
    smg1.addPointsToEdge(l3t);
    smg1.addPointsToEdge(l4t);
    smg1.addPointsToEdge(l5t);

    smg1.setValidity(l1, true);
    smg1.setValidity(l2, true);
    smg1.setValidity(l3, true);
    smg1.setValidity(l4, true);
    smg1.setValidity(l5, true);

    Map<SMGKnownSymValue, SMGKnownExpValue> empty = new java.util.HashMap<>();
    SMGState smg1State = new SMGState(logger, true,
        true, SMGRuntimeCheck.NONE, smg1,
        new AtomicInteger(1), 0, empty, 32, false, false);

    SMGObject head = smg1State.addGlobalVariable(64, "head");
    smg1State.addPointsToEdge(head, 0, 5);

    smg1State.writeValue(head, 0, pointerType, SMGKnownSymValue.valueOf(6));
    smg1State.writeValue(head, 4, pointerType, SMGKnownSymValue.valueOf(10));

    smg1State.performConsistencyCheck(SMGRuntimeCheck.NONE);

    smg1State.executeHeapAbstraction();

    smg1State.performConsistencyCheck(SMGRuntimeCheck.NONE);
  }

  @Test
  public void materialiseTest() throws SMGInconsistentException {

    SMGValueFactory.prepareForTest();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();
    SMGValueFactory.getNewValue();

    CLangSMG heap = new CLangSMG(MachineModel.LINUX32);

   SMGObject dls = new SMGDoublyLinkedList(96, 0, 0, 4, 0, 0);
   SMGEdgeHasValue dlsN = new SMGEdgeHasValue(pointerType, 0, dls, 5);
   SMGEdgeHasValue dlsP = new SMGEdgeHasValue(pointerType, 4, dls, 5);
   heap.addHeapObject(dls);
   heap.setValidity(dls, true);
   heap.addValue(5);
   heap.addValue(6);
   heap.addValue(7);
   heap.addHasValueEdge(dlsP);
   heap.addHasValueEdge(dlsN);
   heap.addPointsToEdge(new SMGEdgePointsTo(6, dls, 0, SMGTargetSpecifier.FIRST));
   heap.addPointsToEdge(new SMGEdgePointsTo(7, dls, 0, SMGTargetSpecifier.LAST));

   SMGRegion l1 = new SMGRegion(96, "l1", 1);
   SMGRegion l2 = new SMGRegion(96, "l2", 1);
   SMGRegion l3 = new SMGRegion(96, "l3", 1);
   SMGRegion l4 = new SMGRegion(96, "l4", 1);
   SMGRegion l5 = new SMGRegion(96, "l5", 1);

   SMGEdgeHasValue l1fn = new SMGEdgeHasValue(pointerType, 0, l1, 13);
   SMGEdgeHasValue l2fn = new SMGEdgeHasValue(pointerType, 0, l2, 8);
   SMGEdgeHasValue l3fn = new SMGEdgeHasValue(pointerType, 0, l3, 9);
   SMGEdgeHasValue l4fn = new SMGEdgeHasValue(pointerType, 0, l4, 10);
   SMGEdgeHasValue l5fn = new SMGEdgeHasValue(pointerType, 0, l5, 11);
   SMGEdgeHasValue dlsSub = new SMGEdgeHasValue(pointerType, 8, dls, 12);

   SMGEdgeHasValue l1fp = new SMGEdgeHasValue(pointerType, 4, l1, 11);
   SMGEdgeHasValue l2fp = new SMGEdgeHasValue(pointerType, 4, l2, 12);
   SMGEdgeHasValue l3fp = new SMGEdgeHasValue(pointerType, 4, l3, 13);
   SMGEdgeHasValue l4fp = new SMGEdgeHasValue(pointerType, 4, l4, 8);
   SMGEdgeHasValue l5fp = new SMGEdgeHasValue(pointerType, 4, l5, 9);

   SMGEdgePointsTo l1t = new SMGEdgePointsTo(12, l1, 0);
   SMGEdgePointsTo l2t = new SMGEdgePointsTo(13, l2, 0);
   SMGEdgePointsTo l3t = new SMGEdgePointsTo(8, l3, 0);
   SMGEdgePointsTo l4t = new SMGEdgePointsTo(9, l4, 0);
   SMGEdgePointsTo l5t = new SMGEdgePointsTo(10, l5, 0);

   heap.addHeapObject(l1);
   heap.addHeapObject(l2);
   heap.addHeapObject(l3);
   heap.addHeapObject(l4);
   heap.addHeapObject(l5);

   heap.addValue(11);
   heap.addValue(12);
   heap.addValue(13);
   heap.addValue(8);
   heap.addValue(9);
   heap.addValue(10);

   heap.addHasValueEdge(l1fn);
   heap.addHasValueEdge(l2fn);
   heap.addHasValueEdge(l3fn);
   heap.addHasValueEdge(l4fn);
   heap.addHasValueEdge(l5fn);
   heap.addHasValueEdge(dlsSub);

   heap.addHasValueEdge(l1fp);
   heap.addHasValueEdge(l2fp);
   heap.addHasValueEdge(l3fp);
   heap.addHasValueEdge(l4fp);
   heap.addHasValueEdge(l5fp);

   heap.addPointsToEdge(l1t);
   heap.addPointsToEdge(l2t);
   heap.addPointsToEdge(l3t);
   heap.addPointsToEdge(l4t);
   heap.addPointsToEdge(l5t);

   heap.setValidity(l1, true);
   heap.setValidity(l2, true);
   heap.setValidity(l3, true);
   heap.setValidity(l4, true);
   heap.setValidity(l5, true);

    Map<SMGKnownSymValue, SMGKnownExpValue> empty = new java.util.HashMap<>();
    SMGState smg1State = new SMGState(logger, true,
        true, SMGRuntimeCheck.NONE, heap,
        new AtomicInteger(1), 0, empty, 32, false, false);

    smg1State.addStackFrame(functionDeclaration3);
    SMGObject head = smg1State.addGlobalVariable(64, "head");
    smg1State.addPointsToEdge(head, 0, 5);

    smg1State.writeValue(head, 0, pointerType, SMGKnownSymValue.valueOf(6));
    smg1State.writeValue(head, 4, pointerType, SMGKnownSymValue.valueOf(10));

    smg1State.performConsistencyCheck(SMGRuntimeCheck.NONE);

    SMGAddressValueAndStateList add = smg1State.getPointerFromValue(6);

    add.getValueAndStateList().get(1).getSmgState().performConsistencyCheck(SMGRuntimeCheck.NONE);

    add.getValueAndStateList().get(0).getSmgState().performConsistencyCheck(SMGRuntimeCheck.NONE);

    SMGState newState = add.getValueAndStateList().get(1).getSmgState();

    SMGAddressValueAndStateList add2 = newState.getPointerFromValue(7);

    add2.getValueAndStateList().get(1).getSmgState().performConsistencyCheck(SMGRuntimeCheck.NONE);

    add2.getValueAndStateList().get(0).getSmgState().performConsistencyCheck(SMGRuntimeCheck.NONE);

  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws SMGInconsistentException {
    consistent_state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0, false, false);
    inconsistent_state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0, false, false);
    SMGAddressValue pt = inconsistent_state.addNewHeapAllocation(8, "label");

    consistent_state.addGlobalObject((SMGRegion)pt.getObject());
    inconsistent_state.addGlobalObject((SMGRegion)pt.getObject());
  }

  /*
   * Test that consistency violation is reported on:
   *   - inconsistent state
   *   - requested check level is lower than threshold
   */
  @Test(expected=SMGInconsistentException.class)
  public void ConfigurableConsistencyInconsistentReported1Test() throws SMGInconsistentException {
    SMGState inconsistent_state = new SMGState(this.inconsistent_state, SMGRuntimeCheck.FULL);
    inconsistent_state.performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /*
   * Test that consistency violation is reported on:
   *   - inconsistent state
   *   - requested check level is equal to threshold
   */
  @Test(expected=SMGInconsistentException.class)
  public void ConfigurableConsistencyInconsistentReported2Test() throws SMGInconsistentException {
    SMGState inconsistent_state = new SMGState(this.inconsistent_state, SMGRuntimeCheck.FULL);
    inconsistent_state.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /*
   * Test that no consistency violation is reported on:
   *   - inconsistent state
   *   - requested check level is higher than threshold
   */
  @Test
  public void ConfigurableConsistencyInconsistentNotReportedTest() throws SMGInconsistentException {
    SMGState inconsistent_state = new SMGState(this.inconsistent_state, SMGRuntimeCheck.NONE);
    inconsistent_state.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /*
   * Test that no consistency violation is reported on:
   *   - consistent state
   *   - requested check level is lower than threshold
   */
  @Test
  public void ConfigurableConsistencyConsistent1Test() throws SMGInconsistentException {
    SMGState consistent_state = new SMGState(this.consistent_state, SMGRuntimeCheck.FULL);
    consistent_state.performConsistencyCheck(SMGRuntimeCheck.HALF);
  }
  /*
   * Test that no consistency violation is reported on:
   *   - consistent state
   *   - requested check level is higher than threshold
   */
  @Test
  public void ConfigurableConsistencyConsistent2Test() throws SMGInconsistentException {
    SMGState consistent_state = new SMGState(this.consistent_state, SMGRuntimeCheck.NONE);
    consistent_state.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  @Test
  public void PredecessorsTest() {
    SMGState original = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0, false, false);
    SMGState second = new SMGState(original);
    Assert.assertNotEquals(original.getId(), second.getId());

    SMGState copy = new SMGState(original);
    Assert.assertNotEquals(copy.getId(), original.getId());
    Assert.assertNotEquals(copy.getId(), second.getId());

    Assert.assertSame(second.getPredecessorId(), original.getId());
    Assert.assertSame(copy.getPredecessorId(), original.getId());
  }

  @Test
  public void WriteReinterpretationTest() throws SMGInconsistentException {
    // Empty state
    SMGState state = new SMGState(logger, MachineModel.LINUX64,true, true, SMGRuntimeCheck.NONE,
        0, false, false);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Add an 16b object and write a 16b value into it
    SMGAddressValue pt = state.addNewHeapAllocation(16, "OBJECT");
    SMGKnownSymValue new_value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
    SMGEdgeHasValue hv = state.writeValue(pt.getObject(), 0, mockType16b, new_value).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Check the object values and assert it has only the written 16b value
    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(pt.getObject());

    Set<SMGEdgeHasValue> values_for_obj = state.getHVEdges(filter);
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv));

    // Write a same 16b value into it and assert that the state did not change
    state.writeValue(pt.getObject(), 0, mockType16b, new_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getHVEdges(filter);
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv));

    // Write a *different* 16b value into it and assert that the state *did* change
    SMGKnownSymValue newer_value = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
    SMGEdgeHasValue new_hv = state.writeValue(pt.getObject(), 0, mockType16b, newer_value).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getHVEdges(filter);
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(new_hv));
    Assert.assertFalse(values_for_obj.contains(hv));

    // Write a 8b value at index 0 and see that the old value got overwritten
    SMGEdgeHasValue hv8at0 = state.writeValue(pt.getObject(), 0, mockType8b, new_value).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getHVEdges(filter);
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at0));

    // Write a 8b value at index 8 and see that the old value did *not* get overwritten
    SMGEdgeHasValue hv8at8 = state.writeValue(pt.getObject(), 8, mockType8b, new_value).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getHVEdges(filter);
    Assert.assertEquals(2, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at0));
    Assert.assertTrue(values_for_obj.contains(hv8at8));

    // Write a 8b value at index 4 and see that the old value got overwritten
    SMGEdgeHasValue hv8at4 = state.writeValue(pt.getObject(), 4, mockType8b, new_value).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getHVEdges(filter);
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at4));
    Assert.assertFalse(values_for_obj.contains(hv8at0));
    Assert.assertFalse(values_for_obj.contains(hv8at8));
  }

  @Test
  public void WriteReinterpretationNullifiedTest() throws SMGInconsistentException {
    // Empty state
    SMGState state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.FORCED, 0, false, false);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Add an 16b object and write a 16b zero value into it
    SMGAddressValue pt = state.addNewHeapAllocation(16, "OBJECT");
    SMGEdgeHasValue hv = state.writeValue(pt.getObject(), 0, mockType16b, SMGKnownSymValue.ZERO).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Check the object values and assert it has only the written 16b value
    Set<SMGEdgeHasValue> values_for_obj = state.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pt.getObject()));
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv));

    // Write a 8b value at index 4
    // We should see three Has-Value edges: 4b zero, 8b just written, 4b zero
    SMGEdgeHasValue hv8at4 = state.writeValue(pt.getObject(), 4, mockType8b, SMGUnknownValue.getInstance()).getNewEdge();
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pt.getObject()));
    Assert.assertEquals(3, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at4));
    Assert.assertTrue(values_for_obj.contains(new SMGEdgeHasValue(4, 0, pt.getObject(), 0)));
    Assert.assertTrue(values_for_obj.contains(new SMGEdgeHasValue(4, 12, pt.getObject(), 0)));


    SMGEdgeHasValueFilter nullFilter = SMGEdgeHasValueFilter.objectFilter(pt.getObject()).filterHavingValue(0);
    Set<SMGEdgeHasValue> nulls_for_value = state.getHVEdges(nullFilter);
    Assert.assertEquals(2, nulls_for_value.size());

    Assert.assertEquals(1, state.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pt.getObject()).filterHavingValue(0).filterAtOffset(0)).size());
    Assert.assertEquals(1, state.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pt.getObject()).filterHavingValue(0).filterAtOffset(12)).size());

  }

  @Test
  public void getPointerFromValueTest() throws SMGInconsistentException {
 // Empty state
    SMGState state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE,
        0, false, false);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    SMGAddressValue pt = state.addNewHeapAllocation(16, "OBJECT");

    Integer pointer = pt.getValue().intValue();

    SMGAddressValue pt_obtained = Iterables.getOnlyElement(state.getPointerFromValue(pointer).asAddressValueAndStateList()).getObject();
    Assert.assertEquals(pt_obtained.getObject(), pt.getObject());
  }

  @Test(expected=SMGInconsistentException.class)
  public void getPointerFromValueNonPointerTest() throws SMGInconsistentException {
    SMGState state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE,
        0, false, false);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    SMGAddressValue pt = state.addNewHeapAllocation(16, "OBJECT");
    SMGKnownSymValue nonpointer = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
    state.writeValue(pt.getObject(), 0, mockType16b, nonpointer);

    state.getPointerFromValue(nonpointer.getAsInt());
  }
}

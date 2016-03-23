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

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGTransferRelation.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.collect.Iterables;


public class SMGStateTest {
  static private final  LogManager logger = TestLogManager.getInstance();
  private SMGState consistent_state;
  private SMGState inconsistent_state;

  static private final CType mockType16b = AnonymousTypes.createTypeWithLength(16);
  static private final CType mockType8b = AnonymousTypes.createTypeWithLength(8);

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws SMGInconsistentException {
    consistent_state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0);
    inconsistent_state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0);
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
    SMGState original = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0);
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
    SMGState state = new SMGState(logger, MachineModel.LINUX64,true, true, SMGRuntimeCheck.NONE, 0);
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
    SMGState state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.FORCED, 0);
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
    SMGState state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    SMGAddressValue pt = state.addNewHeapAllocation(16, "OBJECT");

    Integer pointer = pt.getValue().intValue();

    SMGAddressValue pt_obtained = Iterables.getOnlyElement(state.getPointerFromValue(pointer).asAddressValueAndStateList()).getObject();
    Assert.assertEquals(pt_obtained.getObject(), pt.getObject());
  }

  @Test(expected=SMGInconsistentException.class)
  public void getPointerFromValueNonPointerTest() throws SMGInconsistentException {
    SMGState state = new SMGState(logger, MachineModel.LINUX64, true, true, SMGRuntimeCheck.NONE, 0);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    SMGAddressValue pt = state.addNewHeapAllocation(16, "OBJECT");
    SMGKnownSymValue nonpointer = SMGKnownSymValue.valueOf(SMGValueFactory.getNewValue());
    state.writeValue(pt.getObject(), 0, mockType16b, nonpointer);

    state.getPointerFromValue(nonpointer.getAsInt());
  }
}

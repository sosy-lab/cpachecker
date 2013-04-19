/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;


public class SMGStateTest {
  private LogManager logger = mock(LogManager.class);
  private SMGState consistent_state;
  private SMGState inconsistent_state;

  private CType mockType16b = mock(CType.class);
  private CType mockType8b = mock(CType.class);

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws SMGInconsistentException {
    consistent_state = new SMGState(logger, MachineModel.LINUX64);
    inconsistent_state = new SMGState(logger, MachineModel.LINUX64);
    SMGEdgePointsTo pt = inconsistent_state.addNewHeapAllocation(8, "label");

    consistent_state.addGlobalObject(pt.getObject());
    inconsistent_state.addGlobalObject(pt.getObject());

    when(mockType16b.accept((CTypeVisitor<Integer, IllegalArgumentException>)(anyObject()))).thenReturn(Integer.valueOf(16));
    when(mockType8b.accept((CTypeVisitor<Integer, IllegalArgumentException>)(anyObject()))).thenReturn(Integer.valueOf(8));
  }

  /*
   * Test that consistency violation is reported on:
   *   - inconsistent state
   *   - requested check level is lower than threshold
   */
  @Test(expected=SMGInconsistentException.class)
  public void ConfigurableConsistencyInconsistentReported1Test() throws SMGInconsistentException {
    inconsistent_state.setRuntimeCheck(SMGRuntimeCheck.FULL);
    inconsistent_state.performConsistencyCheck(SMGRuntimeCheck.HALF);
  }

  /*
   * Test that consistency violation is reported on:
   *   - inconsistent state
   *   - requested check level is equal to threshold
   */
  @Test(expected=SMGInconsistentException.class)
  public void ConfigurableConsistencyInconsistentReported2Test() throws SMGInconsistentException {
    inconsistent_state.setRuntimeCheck(SMGRuntimeCheck.FULL);
    inconsistent_state.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /*
   * Test that no consistency violation is reported on:
   *   - inconsistent state
   *   - requested check level is higher than threshold
   */
  @Test
  public void ConfigurableConsistencyInconsistentNotReportedTest() throws SMGInconsistentException {
    inconsistent_state.setRuntimeCheck(SMGRuntimeCheck.NONE);
    inconsistent_state.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  /*
   * Test that no consistency violation is reported on:
   *   - consistent state
   *   - requested check level is lower than threshold
   */
  @Test
  public void ConfigurableConsistencyConsistent1Test() throws SMGInconsistentException {
    consistent_state.setRuntimeCheck(SMGRuntimeCheck.FULL);
    consistent_state.performConsistencyCheck(SMGRuntimeCheck.HALF);
  }
  /*
   * Test that no consistency violation is reported on:
   *   - consistent state
   *   - requested check level is higher than threshold
   */
  @Test
  public void ConfigurableConsistencyConsistent2Test() throws SMGInconsistentException {
    consistent_state.setRuntimeCheck(SMGRuntimeCheck.NONE);
    consistent_state.performConsistencyCheck(SMGRuntimeCheck.FULL);
  }

  @Test
  public void PredecessorsTest() throws SMGInconsistentException {
    SMGState original = new SMGState(logger, MachineModel.LINUX64);
    SMGState second = new SMGState(logger, MachineModel.LINUX64);
    Assert.assertNull(original.getPredecessor());
    Assert.assertNull(second.getPredecessor());
    Assert.assertNotEquals(original.getId(), second.getId());

    SMGState copy = new SMGState(original);
    Assert.assertNull(copy.getPredecessor());
    Assert.assertNotEquals(copy.getId(), original.getId());
    Assert.assertNotEquals(copy.getId(), second.getId());
    Assert.assertNotEquals(original.getId(), second.getId());

    second.setPredecessor(original);
    Assert.assertSame(second.getPredecessor(), original);
    Assert.assertNotEquals(copy.getId(), original.getId());
    Assert.assertNotEquals(copy.getId(), second.getId());
    Assert.assertNotEquals(original.getId(), second.getId());
  }

  @Test
  public void WriteReinterpretationTest() throws SMGInconsistentException {
    // Empty state
    SMGState state = new SMGState(logger, MachineModel.LINUX64);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Add an 16b object and write a 16b value into it
    SMGEdgePointsTo pt = state.addNewHeapAllocation(16, "OBJECT");
    Integer new_value = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv = state.writeValue(pt.getObject(), 0, mockType16b, new_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Check the object values and assert it has only the written 16b value
    Set<SMGEdgeHasValue> values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv));

    // Write a same 16b value into it and assert that the state did not change
    state.writeValue(pt.getObject(), 0, mockType16b, new_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv));

    // Write a *different* 16b value into it and assert that the state *did* change
    Integer newer_value = SMGValueFactory.getNewValue();
    SMGEdgeHasValue new_hv = state.writeValue(pt.getObject(), 0, mockType16b, newer_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(new_hv));
    Assert.assertFalse(values_for_obj.contains(hv));

    // Write a 8b value at index 0 and see that the old value got overwritten
    SMGEdgeHasValue hv8at0 = state.writeValue(pt.getObject(), 0, mockType8b, new_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at0));

    // Write a 8b value at index 8 and see that the old value did *not* get overwritten
    SMGEdgeHasValue hv8at8 = state.writeValue(pt.getObject(), 8, mockType8b, new_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(2, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at0));
    Assert.assertTrue(values_for_obj.contains(hv8at8));

    // Write a 8b value at index 4 and see that the old value got overwritten
    SMGEdgeHasValue hv8at4 = state.writeValue(pt.getObject(), 4, mockType8b, new_value);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at4));
    Assert.assertFalse(values_for_obj.contains(hv8at0));
    Assert.assertFalse(values_for_obj.contains(hv8at8));
  }

  //TODO: This does not pass yet
  /*
  @Test
  public void WriteReinterpretationNullifiedTest() throws SMGInconsistentException {
    // Empty state
    SMGState state = new SMGState(logger, MachineModel.LINUX64);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Add an 16b object and write a 16b zero value into it
    SMGEdgePointsTo pt = state.addNewHeapAllocation(16, "OBJECT");
    SMGEdgeHasValue hv = state.writeValue(pt.getObject(), 0, mockType16b, null);
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);

    // Check the object values and assert it has only the written 16b value
    Set<SMGEdgeHasValue> values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(1, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv));

    // Write a 8b value at index 4
    // We should see three Has-Value edges: 4b zero, 8b just written, 4b zero
    SMGEdgeHasValue hv8at4 = state.writeValue(pt.getObject(), 4, mockType8b, SMGValueFactory.getNewValue());
    state.performConsistencyCheck(SMGRuntimeCheck.FORCED);
    values_for_obj = state.getValuesForObject(pt.getObject());
    Assert.assertEquals(3, values_for_obj.size());
    Assert.assertTrue(values_for_obj.contains(hv8at4));
    // TODO: Checks for presence of two zero edges
  }
  */
}
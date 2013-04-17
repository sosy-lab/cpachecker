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

import static org.mockito.Mockito.mock;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;


public class SMGStateTest {
  private LogManager logger = mock(LogManager.class);
  private SMGState consistent_state;
  private SMGState inconsistent_state;

  @Before
  public void setUp() {
    consistent_state = new SMGState(logger, MachineModel.LINUX64);
    inconsistent_state = new SMGState(logger, MachineModel.LINUX64);
    SMGObject obj = new SMGObject(8, "label");

    consistent_state.addGlobalObject(obj);
    inconsistent_state.addGlobalObject(obj);

    inconsistent_state.addHeapObject(obj);
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
}
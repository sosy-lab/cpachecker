/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.array;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGArraySymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoin;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;

public class SMGAbstractArrayProxyTest {

  // private SMGState dummyState;
  static private final CType mockType4b = TypeUtils.createTypeWithLength(32);

  private CLangSMG smg1;
  private CLangSMG smg2;

  //@SuppressWarnings("unchecked")
  @Before
  public void setUp() throws InvalidConfigurationException {
    //dummyState = new SMGState(LogManager.createTestLogManager(), MachineModel.LINUX64,
    //    new SMGOptions(Configuration.defaultConfiguration()));
    smg1 = new CLangSMG(MachineModel.LINUX64);
    smg2 = new CLangSMG(MachineModel.LINUX64);
  }

  @Test
  public void simpleTest() throws SMGInconsistentException {
    SMGArraySymbolicValue symLen = new SMGArraySymbolicValue(8, 16, "new array");

    SMGRegion global1 = new SMGRegion(32, "global", 0);
    SMGRegion global2 = new SMGRegion(32, "global", 0);

    SMGAbstractArrayProxy arr1 = SMGAbstractArrayProxy.createArray(32, symLen, 0, "array");
    SMGAbstractArrayProxy arr2 = SMGAbstractArrayProxy.createArray(32, symLen, 0, "array");

    SMGValue ptrVal1 = SMGKnownSymValue.valueOf(51);
    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockType4b, 0, global1, ptrVal1);
    SMGEdgePointsTo pt1 = new SMGEdgePointsTo(ptrVal1, arr1, 0);

    SMGValue ptrVal2 = SMGKnownSymValue.valueOf(52);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockType4b, 0, global2, ptrVal2);
    SMGEdgePointsTo pt2 = new SMGEdgePointsTo(ptrVal2, arr2, 0);

    smg1.addGlobalObject(global1);
    smg1.addHeapObject(arr1);
    smg1.addValue(ptrVal1);
    smg1.addHasValueEdge(hv1);
    smg1.addPointsToEdge(pt1);

    smg2.addGlobalObject(global2);
    smg2.addHeapObject(arr2);
    smg2.addValue(ptrVal2);
    smg2.addHasValueEdge(hv2);
    smg2.addPointsToEdge(pt2);

    SMGJoin join = new SMGJoin(smg1, smg2, null, null);
    Assert.assertTrue(join.isDefined());
    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
  }
}

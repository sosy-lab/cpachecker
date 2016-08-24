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

import com.google.common.collect.Iterables;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Set;


public class SMGAbstractionManagerTest {
  private CLangSMG smg;
  @Before
  public void setUp() {
    smg = new CLangSMG(MachineModel.LINUX64);

    SMGRegion globalVar = new SMGRegion(8, "pointer");

    SMGRegion next = null;
    for (int i = 0; i < 20; i++) {
      SMGRegion node = new SMGRegion(16, "node " + i);
      SMGEdgeHasValue hv;
      smg.addHeapObject(node);
      if (next != null) {
        int address = SMGValueFactory.getNewValue();
        SMGEdgePointsTo pt = new SMGEdgePointsTo(address, next, 0);
        hv = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 8, node, address);
        smg.addValue(address);
        smg.addPointsToEdge(pt);
      } else {
        hv = new SMGEdgeHasValue(16, 0, node, 0);
      }
      smg.addHasValueEdge(hv);
      next = node;
    }

    int address = SMGValueFactory.getNewValue();
    SMGEdgeHasValue hv = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 8, globalVar, address);
    SMGEdgePointsTo pt = new SMGEdgePointsTo(address, next, 0);
    smg.addGlobalObject(globalVar);
    smg.addValue(address);
    smg.addPointsToEdge(pt);
    smg.addHasValueEdge(hv);
  }

  @Test
  public void testExecute() throws SMGInconsistentException {
    SMGState dummyState = new SMGState(LogManager.createTestLogManager(), MachineModel.LINUX32, false, false, null, 4, false, false);
    SMGAbstractionManager manager = new SMGAbstractionManager(LogManager.createTestLogManager(), smg, dummyState);
    manager.execute();

    SMGRegion globalVar = smg.getObjectForVisibleVariable("pointer");
    Set<SMGEdgeHasValue> hvs = smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalVar));
    Assert.assertEquals(1, hvs.size());
    SMGEdgeHasValue hv = Iterables.getOnlyElement(hvs);
    SMGEdgePointsTo pt = smg.getPointer(hv.getValue());
    SMGObject segment = pt.getObject();
    Assert.assertTrue(segment.isAbstract());
  }
}

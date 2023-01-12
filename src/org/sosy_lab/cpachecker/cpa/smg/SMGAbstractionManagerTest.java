// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

public class SMGAbstractionManagerTest {
  private CLangSMG smg;

  @Before
  public void setUp() {
    smg = new CLangSMG(MachineModel.LINUX64);

    SMGRegion globalVar = new SMGRegion(64, "pointer");

    SMGRegion next = null;
    for (int i = 0; i < 20; i++) {
      SMGRegion node = new SMGRegion(128, "node " + i);
      SMGEdgeHasValue hv;
      smg.addHeapObject(node);
      if (next != null) {
        SMGValue address = SMGKnownSymValue.of();
        SMGEdgePointsTo pt = new SMGEdgePointsTo(address, next, 0);
        hv =
            new SMGEdgeHasValue(
                smg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
                64,
                node,
                address);
        smg.addValue(address);
        smg.addPointsToEdge(pt);
      } else {
        hv = new SMGEdgeHasValue(128, 0, node, SMGZeroValue.INSTANCE);
      }
      smg.addHasValueEdge(hv);
      next = node;
    }

    SMGValue address = SMGKnownSymValue.of();
    SMGEdgeHasValue hv =
        new SMGEdgeHasValue(
            smg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
            64,
            globalVar,
            address);
    SMGEdgePointsTo pt = new SMGEdgePointsTo(address, next, 0);
    smg.addGlobalObject(globalVar);
    smg.addValue(address);
    smg.addPointsToEdge(pt);
    smg.addHasValueEdge(hv);
  }

  @Test
  public void testExecute() throws SMGInconsistentException, InvalidConfigurationException {
    SMGState dummyState =
        new SMGState(
            LogManager.createTestLogManager(),
            MachineModel.LINUX32,
            new SMGOptions(Configuration.defaultConfiguration()));
    SMGAbstractionManager manager =
        new SMGAbstractionManager(LogManager.createTestLogManager(), smg, dummyState);
    manager.execute();

    SMGRegion globalVar = smg.getObjectForVisibleVariable("pointer");
    SMGHasValueEdges hvs = smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalVar));
    assertThat(hvs).hasSize(1);
    SMGEdgeHasValue hv = Iterables.getOnlyElement(hvs);
    SMGEdgePointsTo pt = smg.getPointer(hv.getValue());
    SMGObject segment = pt.getObject();
    assertThat(segment.isAbstract()).isTrue();
  }
}

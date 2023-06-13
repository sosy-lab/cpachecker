// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

public final class TestHelpers {
  public static SMGValue createList(
      CLangSMG pSmg, int pLength, int pSize, int pOffset, String pPrefix) {
    SMGValue value = null;
    for (int i = 0; i < pLength; i++) {
      SMGObject node = new SMGRegion(pSize, pPrefix + "list_node" + i);
      SMGEdgeHasValue hv;
      if (value == null) {
        hv =
            new SMGEdgeHasValue(
                pSmg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
                pOffset,
                node,
                SMGZeroValue.INSTANCE);
      } else {
        hv =
            new SMGEdgeHasValue(
                pSmg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
                pOffset,
                node,
                value);
      }
      value = SMGKnownSymValue.of();
      SMGEdgePointsTo pt = new SMGEdgePointsTo(value, node, 0);
      pSmg.addHeapObject(node);
      pSmg.addValue(value);
      pSmg.addHasValueEdge(hv);
      pSmg.addPointsToEdge(pt);
    }
    return value;
  }

  public static SMGEdgeHasValue createGlobalList(
      CLangSMG pSmg, int pLength, int pSize, int pOffset, String pVariable) {
    SMGValue value = TestHelpers.createList(pSmg, pLength, pSize, pOffset, pVariable);
    SMGRegion globalVar = new SMGRegion(64, pVariable);
    SMGEdgeHasValue hv =
        new SMGEdgeHasValue(
            pSmg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
            0,
            globalVar,
            value);
    pSmg.addGlobalObject(globalVar);
    pSmg.addHasValueEdge(hv);

    return hv;
  }

  private TestHelpers() {}
}

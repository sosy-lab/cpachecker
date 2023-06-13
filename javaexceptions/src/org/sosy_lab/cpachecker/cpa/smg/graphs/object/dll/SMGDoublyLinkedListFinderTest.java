// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGCPA;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMGTest;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGDoublyLinkedListFinderTest {

  private CSimpleType intType = CNumericTypes.SIGNED_INT;
  private final MachineModel MM = MachineModel.LINUX32;
  private CType pointerType = new CPointerType(false, false, intType);
  private final BigInteger ptrSize = MM.getSizeofInBits(pointerType);

  private CLangSMG smg1;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    smg1 = new CLangSMG(MM);
  }

  @Test
  public void simpleFindAbstractionCandidateTest() throws SMGInconsistentException {

    smg1.addStackFrame(CLangSMGTest.DUMMY_FUNCTION);

    for (int i = 0; i < 15; i++) {
      SMGCPA.getNewValue();
    }

    SMGRegion l1 = new SMGRegion(96, "l1");
    SMGRegion l2 = new SMGRegion(96, "l2");
    SMGRegion l3 = new SMGRegion(96, "l3");
    SMGRegion l4 = new SMGRegion(96, "l4");
    SMGRegion l5 = new SMGRegion(96, "l5");
    SMGRegion head = new SMGRegion(64, "head");

    SMGValue value5 = SMGKnownExpValue.valueOf(5);
    SMGValue value6 = SMGKnownExpValue.valueOf(6);
    SMGValue value7 = SMGKnownExpValue.valueOf(7);
    SMGValue value8 = SMGKnownExpValue.valueOf(8);
    SMGValue value9 = SMGKnownExpValue.valueOf(9);
    SMGValue value10 = SMGKnownExpValue.valueOf(10);

    SMGEdgeHasValue headfn = new SMGEdgeHasValue(ptrSize, 0, head, value6);
    SMGEdgeHasValue l1fn = new SMGEdgeHasValue(ptrSize, 0, l1, value7);
    SMGEdgeHasValue l2fn = new SMGEdgeHasValue(ptrSize, 0, l2, value8);
    SMGEdgeHasValue l3fn = new SMGEdgeHasValue(ptrSize, 0, l3, value9);
    SMGEdgeHasValue l4fn = new SMGEdgeHasValue(ptrSize, 0, l4, value10);
    SMGEdgeHasValue l5fn = new SMGEdgeHasValue(ptrSize, 0, l5, value5);

    SMGEdgeHasValue l1fp = new SMGEdgeHasValue(ptrSize, 32, l1, value5);
    SMGEdgeHasValue l2fp = new SMGEdgeHasValue(ptrSize, 32, l2, value6);
    SMGEdgeHasValue l3fp = new SMGEdgeHasValue(ptrSize, 32, l3, value7);
    SMGEdgeHasValue l4fp = new SMGEdgeHasValue(ptrSize, 32, l4, value8);
    SMGEdgeHasValue l5fp = new SMGEdgeHasValue(ptrSize, 32, l5, value9);
    SMGEdgeHasValue headfp = new SMGEdgeHasValue(ptrSize, 32, head, value10);

    SMGEdgePointsTo lht = new SMGEdgePointsTo(value5, head, 0);
    SMGEdgePointsTo l1t = new SMGEdgePointsTo(value6, l1, 0);
    SMGEdgePointsTo l2t = new SMGEdgePointsTo(value7, l2, 0);
    SMGEdgePointsTo l3t = new SMGEdgePointsTo(value8, l3, 0);
    SMGEdgePointsTo l4t = new SMGEdgePointsTo(value9, l4, 0);
    SMGEdgePointsTo l5t = new SMGEdgePointsTo(value10, l5, 0);

    smg1.addGlobalObject(head);
    smg1.addHeapObject(l1);
    smg1.addHeapObject(l2);
    smg1.addHeapObject(l3);
    smg1.addHeapObject(l4);
    smg1.addHeapObject(l5);

    smg1.addValue(value5);
    smg1.addValue(value6);
    smg1.addValue(value7);
    smg1.addValue(value8);
    smg1.addValue(value9);
    smg1.addValue(value10);

    smg1.addHasValueEdge(headfn);
    smg1.addHasValueEdge(l1fn);
    smg1.addHasValueEdge(l2fn);
    smg1.addHasValueEdge(l3fn);
    smg1.addHasValueEdge(l4fn);
    smg1.addHasValueEdge(l5fn);

    smg1.addHasValueEdge(headfp);
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
    smg1.addPointsToEdge(lht);

    smg1.setValidity(l1, true);
    smg1.setValidity(l2, true);
    smg1.setValidity(l3, true);
    smg1.setValidity(l4, true);
    smg1.setValidity(l5, true);
    smg1.setValidity(head, true);

    SMGDoublyLinkedListFinder f = new SMGDoublyLinkedListFinder();

    Set<SMGAbstractionCandidate> s = f.traverse(smg1, null, ImmutableSet.of());

    assertThat(s).isNotEmpty();
  }
}

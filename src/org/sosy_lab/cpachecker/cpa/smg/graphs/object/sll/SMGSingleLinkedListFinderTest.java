// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGSingleLinkedListFinderTest {
  @Test
  public void simpleListTest() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 5, 128, 64, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder();
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg, null, ImmutableSet.of());
    assertThat(!candidates.isEmpty()).isTrue();
    SMGAbstractionCandidate candidate = getBestCandidate(candidates);
    assertThat(candidate).isInstanceOf(SMGSingleLinkedListCandidateSequence.class);
    SMGSingleLinkedListCandidateSequence sllCandidate =
        (SMGSingleLinkedListCandidateSequence) candidate;
    assertThat(sllCandidate.getLength()).isEqualTo(5);
    assertThat(sllCandidate.getCandidate().getShape().getNfo()).isEqualTo(64);
    SMGRegion expectedStart = (SMGRegion) smg.getPointer(root.getValue()).getObject();
    assertThat(sllCandidate.getCandidate().getStartObject()).isSameInstanceAs(expectedStart);
  }

  private SMGAbstractionCandidate getBestCandidate(Collection<SMGAbstractionCandidate> candidates) {

    SMGAbstractionCandidate bestCandidate = candidates.iterator().next();

    for (SMGAbstractionCandidate candidate : candidates) {
      if (candidate.getScore() > bestCandidate.getScore()) {
        bestCandidate = candidate;
      }
    }

    return bestCandidate;
  }

  @Test
  public void nullifiedPointerInferenceTest() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    TestHelpers.createGlobalList(smg, 2, 128, 64, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(2, 2, 2);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg, null, ImmutableSet.of());
    assertThat(candidates).hasSize(1);
  }

  @Test
  public void listWithInboundPointersTest() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    SMGValue tail = TestHelpers.createList(smg, 4, 128, 64, "tail");

    SMGEdgeHasValue head = TestHelpers.createGlobalList(smg, 3, 128, 64, "head");

    SMGObject inside = new SMGRegion(128, "pointed_at");
    SMGEdgeHasValue tailConnection =
        new SMGEdgeHasValue(
            smg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID), 64, inside, tail);

    SMGValue addressOfInside = SMGKnownSymValue.of();
    SMGEdgePointsTo insidePT = new SMGEdgePointsTo(addressOfInside, inside, 0);
    SMGRegion inboundPointer = new SMGRegion(64, "inbound_pointer");
    SMGEdgeHasValue inboundPointerConnection =
        new SMGEdgeHasValue(
            smg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
            0,
            inboundPointer,
            addressOfInside);

    SMGObject lastFromHead = smg.getPointer(head.getValue()).getObject();
    SMGEdgeHasValue connection = null;
    do {
      SMGEdgeHasValueFilter filter =
          SMGEdgeHasValueFilter.objectFilter(lastFromHead)
              .filterAtOffset(64)
              .filterBySize(smg.getSizeofPtrInBits());
      Iterable<SMGEdgeHasValue> connections = smg.getHVEdges(filter);
      connection = null;
      if (connections.iterator().hasNext()) {
        connection = Iterables.getOnlyElement(connections);
        lastFromHead = smg.getPointer(connection.getValue()).getObject();
      }
    } while (connection != null);

    for (SMGEdgeHasValue hv : smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(lastFromHead))) {
      smg.removeHasValueEdge(hv);
    }

    SMGEdgeHasValue headConnection =
        new SMGEdgeHasValue(
            smg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
            64,
            lastFromHead,
            addressOfInside);

    SMGRegion tailPointer = new SMGRegion(64, "tail_pointer");
    SMGEdgeHasValue tailPointerConnection =
        new SMGEdgeHasValue(
            smg.getMachineModel().getSizeofInBits(CPointerType.POINTER_TO_VOID),
            0,
            tailPointer,
            tail);

    smg.addGlobalObject(tailPointer);
    smg.addHasValueEdge(tailPointerConnection);

    smg.addHeapObject(inside);
    smg.addValue(addressOfInside);
    smg.addPointsToEdge(insidePT);

    smg.addGlobalObject(inboundPointer);
    smg.addHasValueEdge(inboundPointerConnection);

    smg.addHasValueEdge(tailConnection);
    smg.addHasValueEdge(headConnection);

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder();
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg, null, ImmutableSet.of());
    assertThat(!candidates.isEmpty()).isTrue();

    for (SMGAbstractionCandidate candidate : candidates) {
      assertThat(candidate.getLength() < 5).isTrue();
    }
  }
}

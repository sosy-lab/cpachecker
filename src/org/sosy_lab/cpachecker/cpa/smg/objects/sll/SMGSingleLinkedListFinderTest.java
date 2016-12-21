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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import com.google.common.collect.Iterables;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Collection;
import java.util.Set;


public class SMGSingleLinkedListFinderTest {
  @Test
  public void simpleListTest() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 5, 128, 64, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder();
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg, null);
    Assert.assertTrue(!candidates.isEmpty());
    SMGAbstractionCandidate candidate = getBestCandidate(candidates);
    Assert.assertTrue(candidate instanceof SMGSingleLinkedListCandidateSequence);
    SMGSingleLinkedListCandidateSequence sllCandidate = (SMGSingleLinkedListCandidateSequence)candidate;
    Assert.assertEquals(5, sllCandidate.getLength());
    Assert.assertEquals(64, sllCandidate.getCandidate().getNfo());
    SMGRegion expectedStart = (SMGRegion) smg.getPointer(root.getValue()).getObject();
    Assert.assertSame(expectedStart, sllCandidate.getCandidate().getStartObject());
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

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(2,2,2);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg, null);
    Assert.assertEquals(1, candidates.size());
  }

  @Test
  public void listWithInboundPointersTest() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    Integer tail = TestHelpers.createList(smg, 4, 128, 64, "tail");

    SMGEdgeHasValue head = TestHelpers.createGlobalList(smg, 3, 128, 64, "head");

    SMGObject inside = new SMGRegion(128, "pointed_at");
    SMGEdgeHasValue tailConnection = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 64, inside, tail);

    Integer addressOfInside = SMGValueFactory.getNewValue();
    SMGEdgePointsTo insidePT = new SMGEdgePointsTo(addressOfInside, inside, 0);
    SMGRegion inboundPointer = new SMGRegion(64, "inbound_pointer");
    SMGEdgeHasValue inboundPointerConnection = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 0, inboundPointer, addressOfInside);

    SMGObject lastFromHead = smg.getPointer(head.getValue()).getObject();
    SMGEdgeHasValue connection = null;
    do {
      SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(lastFromHead).filterAtOffset(64);
      Set<SMGEdgeHasValue> connections = smg.getHVEdges(filter);
      connection = null;
      if (connections.size() > 0) {
        connection = Iterables.getOnlyElement(connections);
        lastFromHead = smg.getPointer(connection.getValue()).getObject();
      }
    } while (connection != null);

    for (SMGEdgeHasValue hv : smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(lastFromHead))) {
      smg.removeHasValueEdge(hv);
    }

    SMGEdgeHasValue headConnection = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 64, lastFromHead, addressOfInside);

    SMGRegion tailPointer = new SMGRegion(64, "tail_pointer");
    SMGEdgeHasValue tailPointerConnection = new SMGEdgeHasValue(CPointerType.POINTER_TO_VOID, 0, tailPointer, tail);

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
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg, null);
    Assert.assertTrue(!candidates.isEmpty());

    for (SMGAbstractionCandidate candidate : candidates) {
      Assert.assertTrue(((SMGSingleLinkedListCandidateSequence)candidate).getLength() < 5 );
    }
  }
}

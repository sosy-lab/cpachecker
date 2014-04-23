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
package org.sosy_lab.cpachecker.cpa.smgfork.objects.sll;

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smgfork.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smgfork.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGRegion;

import com.google.common.collect.Iterables;


public class SMGSingleLinkedListFinderTest {
  @Test
  public void simpleListTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 5, 16, 8, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(1);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(1, candidates.size());
    SMGAbstractionCandidate candidate = Iterables.getOnlyElement(candidates);
    Assert.assertTrue(candidate instanceof SMGSingleLinkedListCandidate);
    SMGSingleLinkedListCandidate sllCandidate = (SMGSingleLinkedListCandidate)candidate;
    Assert.assertEquals(5, sllCandidate.getLength());
    Assert.assertEquals(8, sllCandidate.getOffset());
    SMGRegion expectedStart = (SMGRegion) smg.getPointer(root.getValue()).getObject();
    Assert.assertSame(expectedStart, sllCandidate.getStart());
  }

  @Test
  public void nullifiedPointerInferenceTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    TestHelpers.createGlobalList(smg, 2, 16, 8, "pointer");

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(1);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(1, candidates.size());
  }

  @Test
  public void listWithInboundPointersTest() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    Integer tail = TestHelpers.createList(smg, 4, 16, 8, "tail");

    SMGEdgeHasValue head = TestHelpers.createGlobalList(smg, 3, 16, 8, "head");

    SMGObject inside = new SMGRegion(16, "pointed_at");
    SMGEdgeHasValue tailConnection = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, 8, inside, tail);

    Integer addressOfInside = SMGValueFactory.getNewValue();
    SMGEdgePointsTo insidePT = new SMGEdgePointsTo(addressOfInside, inside, 0);
    SMGRegion inboundPointer = new SMGRegion(8, "inbound_pointer");
    SMGEdgeHasValue inboundPointerConnection = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, 0, inboundPointer, addressOfInside);

    SMGObject lastFromHead = smg.getPointer(head.getValue()).getObject();
    SMGEdgeHasValue connection = null;
    do {
      SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(lastFromHead).filterAtOffset(8);
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

    SMGEdgeHasValue headConnection = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, 8, lastFromHead, addressOfInside);

    SMGRegion tailPointer = new SMGRegion(8, "tail_pointer");
    SMGEdgeHasValue tailPointerConnection = new SMGEdgeHasValue(AnonymousTypes.dummyPointer, 0, tailPointer, tail);

    smg.addGlobalObject(tailPointer);
    smg.addHasValueEdge(tailPointerConnection);

    smg.addHeapObject(inside);
    smg.addValue(addressOfInside);
    smg.addPointsToEdge(insidePT);

    smg.addGlobalObject(inboundPointer);
    smg.addHasValueEdge(inboundPointerConnection);

    smg.addHasValueEdge(tailConnection);
    smg.addHasValueEdge(headConnection);

    SMGSingleLinkedListFinder finder = new SMGSingleLinkedListFinder(1);
    Set<SMGAbstractionCandidate> candidates = finder.traverse(smg);
    Assert.assertEquals(2, candidates.size());

    boolean sawHead = false;
    boolean sawTail = false;
    for (SMGAbstractionCandidate candidate : candidates) {
      SMGSingleLinkedListCandidate sllCandidate = (SMGSingleLinkedListCandidate)candidate;
      if (sllCandidate.getLength() == 3) {
        Assert.assertSame(smg.getPointer(head.getValue()).getObject(), sllCandidate.getStart());
        Assert.assertFalse(sawHead);
        sawHead = true;
      } else if (sllCandidate.getLength() == 4) {
        Assert.assertSame(smg.getPointer(tail).getObject(), sllCandidate.getStart());
        Assert.assertFalse(sawTail);
      } else {
        Assert.fail("We should not see any candidates with length other than 3 or 4");
      }
    }
  }
}

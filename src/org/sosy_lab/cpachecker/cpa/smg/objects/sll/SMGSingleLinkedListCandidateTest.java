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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGSingleLinkedListCandidateTest {

  @Test
  public void basicTest() {
    SMGObject object = new SMGRegion(8, "object");
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(object, 4, 2);

    Assert.assertSame(object, candidate.getStart());
    Assert.assertEquals(4, candidate.getOffset());
    Assert.assertEquals(2, candidate.getLength());

    candidate.addLength(4);
    Assert.assertEquals(4, candidate.getOffset());
    Assert.assertEquals(6, candidate.getLength());
  }

  @Test
  public void isCompatibleWithTest() {
    SMGObject object8_1 = new SMGRegion(8, "object 1");
    SMGObject object8_2 = new SMGRegion(8, "object 2");
    SMGObject object16 = new SMGRegion(16, "object 3");

    SMGSingleLinkedListCandidate candidate8_1 = new SMGSingleLinkedListCandidate(object8_1, 4, 2);
    SMGSingleLinkedListCandidate candidate8_2 = new SMGSingleLinkedListCandidate(object8_2, 4, 8);
    SMGSingleLinkedListCandidate candidate16 = new SMGSingleLinkedListCandidate(object16, 4, 2);

    Assert.assertTrue(candidate8_1.isCompatibleWith(candidate8_2));
    Assert.assertTrue(candidate8_2.isCompatibleWith(candidate8_1));
    Assert.assertFalse(candidate16.isCompatibleWith(candidate8_1));
    Assert.assertFalse(candidate8_1.isCompatibleWith(candidate16));

    candidate8_2 = new SMGSingleLinkedListCandidate(object8_2, 6, 2);
    Assert.assertFalse(candidate8_1.isCompatibleWith(candidate8_2));
  }

  @Test
  public void executeOnSimpleList() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    int NODE_SIZE = 8;
    int SEGMENT_LENGTH = 4;
    int OFFSET = 0;

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, SEGMENT_LENGTH + 1, NODE_SIZE, OFFSET, "pointer");
    Integer value = root.getValue();

    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(startObject, OFFSET, SEGMENT_LENGTH);

    CLangSMG abstractedSmg = candidate.execute(smg);
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(3, heap.size());
    SMGObject pointedObject = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(pointedObject instanceof SMGSingleLinkedList);
    Assert.assertTrue(pointedObject.isAbstract());
    SMGSingleLinkedList segment = (SMGSingleLinkedList)pointedObject;
    Assert.assertEquals(NODE_SIZE, segment.getSize());
    Assert.assertEquals(SEGMENT_LENGTH, segment.getLength());
    Assert.assertEquals(OFFSET, segment.getOffset());

    SMGEdgeHasValue onlyOutboundEdge = abstractedSmg.getUniqueHV(SMGEdgeHasValueFilter.objectFilter(segment), true);
    Assert.assertEquals(OFFSET, onlyOutboundEdge.getOffset());
    Assert.assertSame(AnonymousTypes.dummyPointer, onlyOutboundEdge.getType());

    SMGObject stopper = abstractedSmg.getPointer(onlyOutboundEdge.getValue()).getObject();
    Assert.assertTrue(stopper instanceof SMGRegion);
    SMGRegion stopperRegion = (SMGRegion)stopper;
    Assert.assertEquals(NODE_SIZE, stopperRegion.getSize());
    onlyOutboundEdge = abstractedSmg.getUniqueHV(SMGEdgeHasValueFilter.objectFilter(stopper), true);
    Assert.assertEquals(0, onlyOutboundEdge.getValue());
    Assert.assertEquals(0, onlyOutboundEdge.getOffset());
    Assert.assertEquals(NODE_SIZE, onlyOutboundEdge.getSizeInBytes(abstractedSmg.getMachineModel()));
  }

  @Test
  public void executeOnNullTerminatedList() {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 2, 16, 8, "pointer");

    Integer value = root.getValue();
    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(startObject, 8, 2);
    CLangSMG abstractedSmg = candidate.execute(smg);
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(2, heap.size());

    SMGObject sll = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(sll.isAbstract());
    Assert.assertTrue(sll instanceof SMGSingleLinkedList);
    SMGSingleLinkedList realSll = (SMGSingleLinkedList)sll;
    Assert.assertEquals(2, realSll.getLength());
    SMGEdgeHasValue outbound = abstractedSmg.getUniqueHV(SMGEdgeHasValueFilter.objectFilter(realSll), true);
    Assert.assertEquals(8, outbound.getOffset());
    Assert.assertEquals(8, outbound.getSizeInBytes(abstractedSmg.getMachineModel()));
    Assert.assertEquals(0, outbound.getValue());
  }
}

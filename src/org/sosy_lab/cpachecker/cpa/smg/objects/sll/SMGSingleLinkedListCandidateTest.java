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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import java.util.Set;


public class SMGSingleLinkedListCandidateTest {

  @Test
  public void basicTest() {
    SMGObject object = new SMGRegion(8, "object");
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(object, 4, 0, CPointerType.POINTER_TO_VOID, MachineModel.LINUX32);
    SMGSingleLinkedListCandidateSequence candidateSeq = new SMGSingleLinkedListCandidateSequence(candidate, 2, SMGJoinStatus.INCOMPARABLE, false);

    Assert.assertSame(object, candidate.getStartObject());
    Assert.assertEquals(4, candidate.getNfo());
    Assert.assertEquals(0, candidate.getHfo());
    Assert.assertEquals(2, candidateSeq.getLength());
  }

  @Test
  public void executeOnSimpleList() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    int NODE_SIZE = 8;
    int SEGMENT_LENGTH = 4;
    int OFFSET = 0;

    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, SEGMENT_LENGTH, NODE_SIZE, OFFSET, "pointer");
    Integer value = root.getValue();

    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(startObject, OFFSET, 0, CPointerType.POINTER_TO_VOID, MachineModel.LINUX32);
    SMGSingleLinkedListCandidateSequence candidateSeq = new SMGSingleLinkedListCandidateSequence(candidate, SEGMENT_LENGTH, SMGJoinStatus.INCOMPARABLE, false);

    CLangSMG abstractedSmg = candidateSeq.execute(smg, new SMGState(LogManager.createTestLogManager(), MachineModel.LINUX64, false, false, null, 4, false, false));
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(2, heap.size());
    SMGObject pointedObject = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(pointedObject instanceof SMGSingleLinkedList);
    Assert.assertTrue(pointedObject.isAbstract());
    SMGSingleLinkedList segment = (SMGSingleLinkedList)pointedObject;
    Assert.assertEquals(NODE_SIZE, segment.getSize());
    Assert.assertEquals(SEGMENT_LENGTH, segment.getMinimumLength());
    Assert.assertEquals(OFFSET, segment.getNfo());
    Set<SMGEdgeHasValue> outboundEdges = abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(segment));
    Assert.assertEquals(1, outboundEdges.size());
    SMGEdgeHasValue onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(OFFSET, onlyOutboundEdge.getOffset());
    Assert.assertSame(CPointerType.POINTER_TO_VOID, onlyOutboundEdge.getType());

    Assert.assertEquals(1, outboundEdges.size());
    onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(0, onlyOutboundEdge.getValue());
    Assert.assertEquals(0, onlyOutboundEdge.getOffset());
    Assert.assertEquals(NODE_SIZE, onlyOutboundEdge.getSizeInBytes(abstractedSmg.getMachineModel()));
  }

  @Test
  public void executeOnNullTerminatedList() throws SMGInconsistentException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 2, 16, 8, "pointer");

    Integer value = root.getValue();
    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate = new SMGSingleLinkedListCandidate(startObject, 8, 0, CPointerType.POINTER_TO_VOID, MachineModel.LINUX32);
    SMGSingleLinkedListCandidateSequence candidateSeq = new SMGSingleLinkedListCandidateSequence(candidate, 2, SMGJoinStatus.INCOMPARABLE, false);
    CLangSMG abstractedSmg = candidateSeq.execute(smg, new SMGState(LogManager.createTestLogManager(), MachineModel.LINUX64, false, false, null, 4, false, false));
    Set<SMGObject> heap = abstractedSmg.getHeapObjects();
    Assert.assertEquals(2, heap.size());

    SMGObject sll = abstractedSmg.getPointer(value).getObject();
    Assert.assertTrue(sll.isAbstract());
    Assert.assertTrue(sll instanceof SMGSingleLinkedList);
    SMGSingleLinkedList realSll = (SMGSingleLinkedList)sll;
    Assert.assertEquals(2, realSll.getMinimumLength());
    Set<SMGEdgeHasValue> outboundEdges = abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(realSll));
    Assert.assertEquals(1, outboundEdges.size());
    SMGEdgeHasValue outbound = Iterables.getOnlyElement(outboundEdges);
    Assert.assertEquals(8, outbound.getOffset());
    Assert.assertEquals(8, outbound.getSizeInBytes(abstractedSmg.getMachineModel()));
    Assert.assertEquals(0, outbound.getValue());
  }
}

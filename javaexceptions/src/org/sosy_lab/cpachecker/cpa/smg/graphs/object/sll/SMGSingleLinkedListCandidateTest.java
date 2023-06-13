// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class SMGSingleLinkedListCandidateTest {

  @Test
  public void basicTest() {
    SMGObject object = new SMGRegion(64, "object");
    SMGSingleLinkedListCandidate candidate =
        new SMGSingleLinkedListCandidate(
            object,
            32,
            0,
            MachineModel.LINUX32.getSizeofInBits(CPointerType.POINTER_TO_VOID).longValueExact(),
            MachineModel.LINUX32);
    SMGSingleLinkedListCandidateSequence candidateSeq =
        new SMGSingleLinkedListCandidateSequence(candidate, 2, SMGJoinStatus.INCOMPARABLE, false);

    assertThat(candidate.getStartObject()).isSameInstanceAs(object);
    assertThat(candidate.getShape().getNfo()).isEqualTo(32);
    assertThat(candidate.getShape().getHfo()).isEqualTo(0);
    assertThat(candidateSeq.getLength()).isEqualTo(2);
  }

  @Test
  public void executeOnSimpleList() throws SMGInconsistentException, InvalidConfigurationException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    int NODE_SIZE = 64;
    int SEGMENT_LENGTH = 4;
    int OFFSET = 0;

    SMGEdgeHasValue root =
        TestHelpers.createGlobalList(smg, SEGMENT_LENGTH, NODE_SIZE, OFFSET, "pointer");
    SMGValue value = root.getValue();

    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate =
        new SMGSingleLinkedListCandidate(
            startObject,
            OFFSET,
            0,
            MachineModel.LINUX32.getSizeofInBits(CPointerType.POINTER_TO_VOID).longValueExact(),
            MachineModel.LINUX32);
    SMGSingleLinkedListCandidateSequence candidateSeq =
        new SMGSingleLinkedListCandidateSequence(
            candidate, SEGMENT_LENGTH, SMGJoinStatus.INCOMPARABLE, false);

    CLangSMG abstractedSmg =
        candidateSeq.execute(
            smg,
            new SMGState(
                LogManager.createTestLogManager(),
                MachineModel.LINUX64,
                new SMGOptions(Configuration.defaultConfiguration())));
    PersistentSet<SMGObject> heap = abstractedSmg.getHeapObjects();
    assertThat(heap).hasSize(2);
    SMGHasValueEdges globalHves =
        abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(root.getObject()));
    root = Iterables.getOnlyElement(globalHves);
    value = root.getValue();
    SMGObject pointedObject = abstractedSmg.getPointer(value).getObject();
    assertThat(pointedObject).isInstanceOf(SMGSingleLinkedList.class);
    assertThat(pointedObject.isAbstract()).isTrue();
    SMGSingleLinkedList segment = (SMGSingleLinkedList) pointedObject;
    assertThat(segment.getSize()).isEqualTo(NODE_SIZE);
    assertThat(segment.getMinimumLength()).isEqualTo(SEGMENT_LENGTH);
    assertThat(segment.getNfo()).isEqualTo(OFFSET);
    SMGHasValueEdges outboundEdges =
        abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(segment));
    assertThat(outboundEdges).hasSize(1);
    SMGEdgeHasValue onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    assertThat(onlyOutboundEdge.getOffset()).isEqualTo(OFFSET);

    assertThat(outboundEdges).hasSize(1);
    onlyOutboundEdge = Iterables.getOnlyElement(outboundEdges);
    assertThat(onlyOutboundEdge.getValue()).isEqualTo(SMGZeroValue.INSTANCE);
    assertThat(onlyOutboundEdge.getOffset()).isEqualTo(0);
    assertThat(onlyOutboundEdge.getSizeInBits()).isEqualTo(NODE_SIZE);
  }

  @Test
  public void executeOnNullTerminatedList()
      throws SMGInconsistentException, InvalidConfigurationException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);
    SMGEdgeHasValue root = TestHelpers.createGlobalList(smg, 2, 128, 64, "pointer");

    SMGValue value = root.getValue();
    SMGObject startObject = smg.getPointer(value).getObject();
    SMGSingleLinkedListCandidate candidate =
        new SMGSingleLinkedListCandidate(
            startObject,
            64,
            0,
            MachineModel.LINUX32.getSizeofInBits(CPointerType.POINTER_TO_VOID).longValueExact(),
            MachineModel.LINUX32);
    SMGSingleLinkedListCandidateSequence candidateSeq =
        new SMGSingleLinkedListCandidateSequence(candidate, 2, SMGJoinStatus.INCOMPARABLE, false);
    CLangSMG abstractedSmg =
        candidateSeq.execute(
            smg,
            new SMGState(
                LogManager.createTestLogManager(),
                MachineModel.LINUX64,
                new SMGOptions(Configuration.defaultConfiguration())));
    PersistentSet<SMGObject> heap = abstractedSmg.getHeapObjects();
    assertThat(heap).hasSize(2);
    SMGHasValueEdges globalHves =
        abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(root.getObject()));
    root = Iterables.getOnlyElement(globalHves);
    value = root.getValue();
    SMGObject sll = abstractedSmg.getPointer(value).getObject();
    assertThat(sll.isAbstract()).isTrue();
    assertThat(sll).isInstanceOf(SMGSingleLinkedList.class);
    SMGSingleLinkedList realSll = (SMGSingleLinkedList) sll;
    assertThat(realSll.getMinimumLength()).isEqualTo(2);
    SMGHasValueEdges outboundEdges =
        abstractedSmg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(realSll));
    assertThat(outboundEdges).hasSize(1);
    SMGEdgeHasValue outbound = Iterables.getOnlyElement(outboundEdges);
    assertThat(outbound.getOffset()).isEqualTo(64);
    assertThat(outbound.getSizeInBits()).isEqualTo(64);
    assertThat(outbound.getValue()).isEqualTo(SMGZeroValue.INSTANCE);
  }
}

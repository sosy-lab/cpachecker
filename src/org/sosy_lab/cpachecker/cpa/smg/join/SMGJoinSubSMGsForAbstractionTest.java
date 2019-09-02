/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.HashBiMap;
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
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableCLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.TestHelpers;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;

public class SMGJoinSubSMGsForAbstractionTest {

  private static final LogManager logger = LogManager.createTestLogManager();

  @Test
  public void testJoinFirstTwoElementsOfSimpleList()
      throws SMGInconsistentException, InvalidConfigurationException {
    CLangSMG smg = new CLangSMG(MachineModel.LINUX64);

    SMGState smgState =
        new SMGState(
            logger,
            new SMGOptions(Configuration.defaultConfiguration()),
            smg,
            0,
            HashBiMap.create());

    int NODE_SIZE = 64;
    int SEGMENT_LENGTH = 4;
    int OFFSET = 0;

    SMGEdgeHasValue root =
        TestHelpers.createGlobalList(smg, SEGMENT_LENGTH, NODE_SIZE, OFFSET, "pointer");
    SMGObject firstObject = smg.getPointer(root.getValue()).getObject();
    assertThat(firstObject.getKind()).isSameInstanceAs(SMGObjectKind.REG);

    SMGSingleLinkedListCandidate candidate =
        new SMGSingleLinkedListCandidate(
            firstObject, OFFSET, 0, CPointerType.POINTER_TO_VOID, MachineModel.LINUX32);

    long nfo = candidate.getShape().getNfo();
    SMGEdgeHasValue nextEdge =
        Iterables.getOnlyElement(
            smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(firstObject).filterAtOffset(nfo)));
    SMGObject secondObject = smg.getObjectPointedBy(nextEdge.getValue());
    assertThat(secondObject.getKind()).isSameInstanceAs(SMGObjectKind.REG);

    SMGJoinSubSMGsForAbstraction join =
        new SMGJoinSubSMGsForAbstraction(smg, firstObject, secondObject, candidate, smgState);

    assertThat(join.isDefined()).isTrue();
    assertThat(join.getStatus()).isSameInstanceAs(SMGJoinStatus.EQUAL);

    assertThat(join.getNonSharedObjectsFromSMG1()).contains(firstObject);
    assertThat(join.getNonSharedObjectsFromSMG1()).doesNotContain(secondObject);
    assertThat(join.getNonSharedObjectsFromSMG2()).contains(secondObject);
    assertThat(join.getNonSharedObjectsFromSMG2()).doesNotContain(firstObject);

    SMGObject joinResult = join.getNewAbstractObject();
    assertThat(joinResult.isAbstract()).isTrue();
    assertThat(joinResult.getKind()).isSameInstanceAs(SMGObjectKind.SLL);

    SMGSingleLinkedList resultSll = (SMGSingleLinkedList) joinResult;
    assertThat(resultSll.getMinimumLength()).isEqualTo(2);

    UnmodifiableCLangSMG resultSMG = join.getResultSMG();
    PersistentSet<SMGObject> resultHeapObjects = resultSMG.getHeapObjects();
    assertThat(resultHeapObjects.contains(joinResult)).isTrue();
    assertThat(resultHeapObjects.contains(firstObject)).isTrue();
    assertThat(resultHeapObjects.contains(secondObject)).isTrue();

    assertThat(SMGUtils.getPointerToThisObject(resultSll, resultSMG)).isEmpty();
  }
}

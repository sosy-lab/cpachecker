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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import java.util.Set;
import org.junit.Assert;
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
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedListCandidate;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.TestHelpers;

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
    Integer value = root.getValue();
    SMGObject firstObject = smg.getPointer(value).getObject();
    Assert.assertTrue(firstObject.getKind() == SMGObjectKind.REG);

    SMGSingleLinkedListCandidate candidate =
        new SMGSingleLinkedListCandidate(
            firstObject, OFFSET, 0, CPointerType.POINTER_TO_VOID, MachineModel.LINUX32);

    long nfo = candidate.getShape().getNfo();
    SMGEdgeHasValue nextEdge =
        Iterables.getOnlyElement(
            smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(firstObject).filterAtOffset(nfo)));
    SMGObject secondObject = smg.getObjectPointedBy(nextEdge.getValue());
    Assert.assertTrue(secondObject.getKind() == SMGObjectKind.REG);

    SMGJoinSubSMGsForAbstraction join =
        new SMGJoinSubSMGsForAbstraction(smg, firstObject, secondObject, candidate, smgState);

    Assert.assertTrue(join.isDefined());
    Assert.assertTrue(join.getStatus() == SMGJoinStatus.EQUAL);

    Assert.assertTrue(join.getNonSharedObjectsFromSMG1().contains(firstObject));
    Assert.assertFalse(join.getNonSharedObjectsFromSMG1().contains(secondObject));
    Assert.assertTrue(join.getNonSharedObjectsFromSMG2().contains(secondObject));
    Assert.assertFalse(join.getNonSharedObjectsFromSMG2().contains(firstObject));

    SMGObject joinResult = join.getNewAbstractObject();
    Assert.assertTrue(joinResult.isAbstract());
    Assert.assertTrue(joinResult.getKind() == SMGObjectKind.SLL);

    SMGSingleLinkedList resultSll = (SMGSingleLinkedList) joinResult;
    Assert.assertTrue(resultSll.getMinimumLength() == 2);

    CLangSMG resultSMG = join.getResultSMG();
    Set<SMGObject> resultHeapObjects = resultSMG.getHeapObjects();
    Assert.assertTrue(resultHeapObjects.contains(joinResult));
    Assert.assertTrue(resultHeapObjects.contains(firstObject));
    Assert.assertTrue(resultHeapObjects.contains(secondObject));

    Assert.assertTrue(SMGUtils.getPointerToThisObject(resultSll, resultSMG).isEmpty());
  }
}

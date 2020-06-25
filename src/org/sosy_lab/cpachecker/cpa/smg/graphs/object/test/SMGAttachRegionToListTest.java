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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.HashBiMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

@RunWith(Parameterized.class)
public class SMGAttachRegionToListTest {

  private static final String GLOBAL_LIST_POINTER_LABEL = "pointer";
  private static final MachineModel MACHINE_MODEL_FOR_TESTING = MachineModel.LINUX64;
  private static final int LEVEL_ZERO = 0;

  @Parameters
  public static Collection<Object[]> data() {
    return SMGListAbstractionTestInputs.getAttachRegionToListTestInputs();
  }

  @Parameter(value = 0)
  public SMGValue[] values;

  @Parameter(value = 1)
  public SMGValue valueToAttach;

  @Parameter(value = 2)
  public SMGListCircularity circularity;

  @Parameter(value = 3)
  public SMGListLinkage linkage;

  private CLangSMG smg;
  private SMGState state;
  private SMGRegion globalListPointer;
  private SMGValue addressOfList;
  private SMGValue addressOfRegion;
  private SMGObjectKind listKind;
  private int nodeSize;
  private int hfo;
  private int nfo;
  private int pfo;

  @Before
  public void setUp() throws InvalidConfigurationException {

    smg = new CLangSMG(MACHINE_MODEL_FOR_TESTING);
    state =
        new SMGState(
            LogManager.createTestLogManager(),
            new SMGOptions(Configuration.defaultConfiguration()),
            smg,
            0,
            HashBiMap.create());

    final int intSize = 8 * MACHINE_MODEL_FOR_TESTING.getSizeofInt();
    final int ptrSize = 8 * MACHINE_MODEL_FOR_TESTING.getSizeofPtr();

    hfo = 0;
    nfo = 0;
    pfo = (linkage == SMGListLinkage.DOUBLY_LINKED) ? ptrSize : -1;
    final int dfo = (linkage == SMGListLinkage.DOUBLY_LINKED) ? 2 * ptrSize : ptrSize;
    final int dataSize = intSize;
    nodeSize = dfo + dataSize;
    listKind = (linkage == SMGListLinkage.DOUBLY_LINKED) ? SMGObjectKind.DLL : SMGObjectKind.SLL;

    smg = new CLangSMG(MACHINE_MODEL_FOR_TESTING);

    // create one region
    addressOfRegion =
        SMGListAbstractionTestHelpers.addLinkedRegionsWithValuesToHeap(
            smg,
            new SMGValue[] {valueToAttach},
            nodeSize,
            hfo,
            nfo,
            pfo,
            dfo,
            dataSize,
            circularity,
            linkage)[0];

    // create one list
    addressOfList =
        SMGListAbstractionTestHelpers.addLinkedListsWithValuesToHeap(
            smg,
            new SMGValue[][] {values},
            nodeSize,
            hfo,
            nfo,
            pfo,
            dfo,
            dataSize,
            circularity,
            linkage)[0];
  }

  @Test
  public void testAbstractionOfListWithPrependedRegion() throws SMGInconsistentException {

    SMGValue firstAddress = addressOfRegion;
    SMGValue secondAddress = addressOfList;
    firstAddress =
        SMGListAbstractionTestHelpers.linkObjectsOnHeap(
            smg, new SMGValue[] {firstAddress, secondAddress}, hfo, nfo, pfo, circularity, linkage)[
            0];

    globalListPointer =
        SMGListAbstractionTestHelpers.addGlobalListPointerToSMG(
            smg, firstAddress, GLOBAL_LIST_POINTER_LABEL);

    SMGListAbstractionTestHelpers.executeHeapAbstractionWithConsistencyChecks(state, smg);

    Set<SMGEdgeHasValue> hves =
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalListPointer));
    assertThat(hves).hasSize(1);

    SMGEdgePointsTo pt = smg.getPointer(Iterables.getOnlyElement(hves).getValue());
    SMGObject segment = pt.getObject();

    SMGListAbstractionTestHelpers.assertAbstractListSegmentAsExpected(
        segment, nodeSize, LEVEL_ZERO, listKind, values.length + 1);
  }

  @Test
  public void testAbstractonOfListWithAppendedRegion() throws SMGInconsistentException {

    SMGValue firstAddress = addressOfList;
    SMGValue secondAddress = addressOfRegion;
    firstAddress =
        SMGListAbstractionTestHelpers.linkObjectsOnHeap(
            smg, new SMGValue[] {firstAddress, secondAddress}, hfo, nfo, pfo, circularity, linkage)[
            0];

    globalListPointer =
        SMGListAbstractionTestHelpers.addGlobalListPointerToSMG(
            smg, firstAddress, GLOBAL_LIST_POINTER_LABEL);

    SMGListAbstractionTestHelpers.executeHeapAbstractionWithConsistencyChecks(state, smg);

    Set<SMGEdgeHasValue> hves =
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalListPointer));
    assertThat(hves).hasSize(1);

    SMGEdgePointsTo pt = smg.getPointer(Iterables.getOnlyElement(hves).getValue());
    SMGObject segment = pt.getObject();

    SMGListAbstractionTestHelpers.assertAbstractListSegmentAsExpected(
        segment, nodeSize, LEVEL_ZERO, listKind, values.length + 1);
  }
}

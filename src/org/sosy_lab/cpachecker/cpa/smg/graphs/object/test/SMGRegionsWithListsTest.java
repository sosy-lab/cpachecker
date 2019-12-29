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
import com.google.common.truth.Truth;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
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
public class SMGRegionsWithListsTest {

  private static final String GLOBAL_LIST_POINTER_LABEL = "pointer";
  private static final MachineModel MACHINE_MODEL_FOR_TESTING = MachineModel.LINUX64;
  private static final int LEVEL_ZERO = 0;
  private static final int LEVEL_ONE = 1;

  @Parameters
  public static Collection<Object[]> data() {
    return SMGListAbstractionTestInputs.getListsWithSublistsAsTestInputs();
  }

  @Parameter(value = 0)
  public SMGValue[][] sublists;

  @Parameter(value = 1)
  public SMGListCircularity circularity;

  @Parameter(value = 2)
  public SMGListLinkage linkage;

  private CLangSMG smg;
  private SMGState state;
  private SMGRegion globalListPointer;
  private int nodeSize;
  private int dfo;
  private SMGObjectKind listKind;

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

    final int hfo = 0;
    final int nfo = 0;
    final int pfo = (linkage == SMGListLinkage.DOUBLY_LINKED) ? ptrSize : -1;
    dfo = (linkage == SMGListLinkage.DOUBLY_LINKED) ? 2 * ptrSize : ptrSize;
    final int dataSize = intSize;
    nodeSize = dfo + dataSize;
    listKind = (linkage == SMGListLinkage.DOUBLY_LINKED) ? SMGObjectKind.DLL : SMGObjectKind.SLL;

    SMGValue[] addresses =
        SMGListAbstractionTestHelpers.addLinkedRegionsWithSublistsWithValuesToHeap(
            smg, sublists, nodeSize, hfo, nfo, pfo, dfo, dataSize, circularity, linkage);

    globalListPointer =
        SMGListAbstractionTestHelpers.addGlobalListPointerToSMG(
            smg, addresses[0], GLOBAL_LIST_POINTER_LABEL);

    SMGObject segment = smg.getObjectPointedBy(addresses[0]);
    assertThat(segment.isAbstract()).isFalse();
    Truth.assertThat(segment.getKind()).isSameInstanceAs(SMGObjectKind.REG);
    Truth.assertThat(segment.getLevel()).isEqualTo(LEVEL_ZERO);
    Truth.assertThat(segment.getSize()).isEqualTo(nodeSize);
  }

  @Test
  public void testAbstractionOfLinkedRegionsWithSublists() throws SMGInconsistentException {

    SMGListAbstractionTestHelpers.executeHeapAbstractionWithConsistencyChecks(state, smg);

    Set<SMGEdgeHasValue> hvs =
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalListPointer));
    assertThat(hvs).hasSize(1);

    SMGEdgePointsTo pt = smg.getPointer(Iterables.getOnlyElement(hvs).getValue());
    SMGObject abstractionResult = pt.getObject();

    SMGListAbstractionTestHelpers.assertAbstractListSegmentAsExpected(
        abstractionResult, nodeSize, LEVEL_ZERO, listKind, sublists.length);

    Set<SMGEdgeHasValue> dataFieldSet =
        smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(abstractionResult).filterAtOffset(dfo));
    assertThat(dataFieldSet).hasSize(1);
    SMGEdgeHasValue dataField = Iterables.getOnlyElement(dataFieldSet);
    SMGValue dataValue = dataField.getValue();

    // assert that the abstract list points to an abstract sublist
    assertThat(smg.isPointer(dataValue)).isTrue();
    SMGObject subobject = smg.getObjectPointedBy(dataValue);
    Truth.assertThat(subobject).isNotNull();
    int minSublistLength =
        Stream.of(sublists)
            .map(e -> e.length)
            .min(Comparator.comparing(Integer::valueOf))
            .orElseThrow();

    SMGListAbstractionTestHelpers.assertAbstractListSegmentAsExpected(
        subobject, nodeSize, LEVEL_ONE, listKind, minSublistLength);

    SMGListAbstractionTestHelpers.assertStoredDataOfAbstractSublist(smg, sublists, subobject, dfo);
  }
}

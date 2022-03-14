// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.test;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import java.util.Collection;
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
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentBiMap;

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

    final int intSize = 8 * MACHINE_MODEL_FOR_TESTING.getSizeofInt();
    final int ptrSize = MACHINE_MODEL_FOR_TESTING.getSizeofPtrInBits();

    hfo = 0;
    nfo = 0;
    pfo = (linkage == SMGListLinkage.DOUBLY_LINKED) ? ptrSize : -1;
    final int dfo = (linkage == SMGListLinkage.DOUBLY_LINKED) ? 2 * ptrSize : ptrSize;
    final int dataSize = intSize;
    nodeSize = dfo + dataSize;
    listKind = (linkage == SMGListLinkage.DOUBLY_LINKED) ? SMGObjectKind.DLL : SMGObjectKind.SLL;

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

    state =
        new SMGState(
            LogManager.createTestLogManager(),
            new SMGOptions(Configuration.defaultConfiguration()),
            smg,
            0,
            PersistentBiMap.of());
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

    SMGHasValueEdges hves = smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalListPointer));
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

    SMGHasValueEdges hves = smg.getHVEdges(SMGEdgeHasValueFilter.objectFilter(globalListPointer));
    assertThat(hves).hasSize(1);

    SMGEdgePointsTo pt = smg.getPointer(Iterables.getOnlyElement(hves).getValue());
    SMGObject segment = pt.getObject();

    SMGListAbstractionTestHelpers.assertAbstractListSegmentAsExpected(
        segment, nodeSize, LEVEL_ZERO, listKind, values.length + 1);
  }
}

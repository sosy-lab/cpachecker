// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cpa.smg2.SMGCPATest0;
import org.sosy_lab.cpachecker.cpa.smg2.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGSolverException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;
import org.sosy_lab.cpachecker.util.smg.test.SMGTest0;

// TODO write more test cases which test not only base cases
public class SMGProveNequalityTest extends SMGTest0 {

  private SMG smg;
  private SMGOptions options;
  private final SMGValue value1 = SMGValue.of();
  private final SMGValue value2 = SMGValue.of();
  private final SMGValue value3 = SMGValue.of();
  private final SMGValue value4 = SMGValue.of();

  @Before
  public void setUp() throws InvalidConfigurationException {
    options = new SMGOptions(Configuration.defaultConfiguration());
    smg = new SMG(mockType4bSize);
    smg =
        smg.copyAndAddValueWithNestingLevelZero(value1)
            .copyAndAddValueWithNestingLevelZero(value2)
            .copyAndAddValueWithNestingLevelZero(value3)
            .copyAndAddValueWithNestingLevelZero(value4);
  }

  @Test
  public void equalValuesAreNotInequal() throws SMGSolverException, InvalidConfigurationException {
    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);

    assertThat(nequality.proveInequality(value1, value1)).isFalse();
    assertThat(nequality.proveInequality(value2, value2)).isFalse();
    assertThat(nequality.proveInequality(value3, value3)).isFalse();
  }

  @Test
  public void pointerValuesThatShareTargetValuesAreNotInEqual()
      throws SMGSolverException, InvalidConfigurationException {
    SMGDoublyLinkedListSegment dlls1 = createDLLS(64, 0, 32, 0, 0, 0);
    SMGDoublyLinkedListSegment dlls2 = createDLLS(64, 0, 32, 0, 0, 0);
    SMGPointsToEdge pt1 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls1);
    SMGPointsToEdge pt2 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls2);

    smg = smg.copyAndAddPTEdge(pt1, value1);
    smg = smg.copyAndAddPTEdge(pt2, value2);

    SMGHasValueEdge hasValueEdge1 = createHasValueEdge(32, 0, value3);
    SMGHasValueEdge hasValueEdge2 = createHasValueEdge(32, 0, value3);

    smg = smg.copyAndAddHVEdge(hasValueEdge1, dlls1);
    smg = smg.copyAndAddHVEdge(hasValueEdge2, dlls2);

    SMGPointsToEdge pt3 =
        createPTEdge(64, SMGTargetSpecifier.IS_FIRST_POINTER, SMGObject.nullInstance());
    smg = smg.copyAndAddPTEdge(pt3, value3);

    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);
    assertThat(nequality.proveInequality(value1, value2)).isFalse();
  }

  @Test
  public void pointerValuesThatHaveSharedObjectsAreNotInEqual()
      throws SMGSolverException, InvalidConfigurationException {
    SMGDoublyLinkedListSegment dlls1 = createDLLS(64, 0, 32, 0, 0, 0);
    SMGDoublyLinkedListSegment dlls2 = createDLLS(64, 0, 32, 0, 0, 0);
    SMGPointsToEdge pt1 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls1);
    SMGPointsToEdge pt2 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls2);

    smg = smg.copyAndAddPTEdge(pt1, value1);
    smg = smg.copyAndAddPTEdge(pt2, value2);

    SMGHasValueEdge hasValueEdge1 = createHasValueEdge(32, 0, value3);
    SMGHasValueEdge hasValueEdge2 = createHasValueEdge(32, 0, value4);

    smg = smg.copyAndAddHVEdge(hasValueEdge1, dlls1);
    smg = smg.copyAndAddHVEdge(hasValueEdge2, dlls2);

    SMGDoublyLinkedListSegment dlls3 = createDLLS(64, 0, 32, 0, 0, 0);

    SMGPointsToEdge pt4 = createPTEdge(0, SMGTargetSpecifier.IS_LAST_POINTER, dlls3);
    SMGPointsToEdge pt5 = createPTEdge(32, SMGTargetSpecifier.IS_FIRST_POINTER, dlls3);
    smg = smg.copyAndAddPTEdge(pt4, value3);
    smg = smg.copyAndAddPTEdge(pt5, value4);
    final SMGValue value5 = SMGValue.of();
    final SMGValue value6 = SMGValue.of();

    SMGHasValueEdge hasValueEdge3 = createHasValueEdge(32, 32, value5);
    SMGHasValueEdge hasValueEdge4 = createHasValueEdge(32, 0, value6);

    smg =
        smg.copyAndAddValueWithNestingLevelZero(value5)
            .copyAndAddValueWithNestingLevelZero(value6)
            .copyAndAddHVEdge(hasValueEdge4, dlls3)
            .copyAndAddHVEdge(hasValueEdge3, dlls3);
    SMGPointsToEdge pt6 =
        createPTEdge(0, SMGTargetSpecifier.IS_LAST_POINTER, SMGObject.nullInstance());
    SMGPointsToEdge pt7 =
        createPTEdge(32, SMGTargetSpecifier.IS_FIRST_POINTER, SMGObject.nullInstance());

    smg = smg.copyAndAddPTEdge(pt6, value5);
    smg = smg.copyAndAddPTEdge(pt7, value6);

    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);
    assertThat(nequality.proveInequality(value1, value2)).isFalse();
  }

  @Test
  public void pointsToEdgeNotOutOfBoundsTest()
      throws InvalidConfigurationException, SMGSolverException {
    SMGObject reg1 = createRegion(32);
    SMGObject reg2 = createRegion(64);
    SMGPointsToEdge pt1 = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2 = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg2);

    // State is irrelevant for this check (as we only use concrete offsets)
    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt1, pt2, false)).isFalse();
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt2, pt1, false)).isFalse();

    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt1, pt2, true)).isFalse();
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt2, pt1, true)).isFalse();

    SMGPointsToEdge pt22 = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg2);

    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt1, pt22, true)).isFalse();
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt22, pt1, true)).isFalse();

    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt1, pt22, false)).isFalse();
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt22, pt1, false)).isFalse();

    SMGPointsToEdge pt12 = createPTEdge(31, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt23 = createPTEdge(63, SMGTargetSpecifier.IS_REGION, reg2);

    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt12, pt23, true)).isFalse();
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt23, pt12, true)).isFalse();

    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt12, pt23, false)).isFalse();
    assertThat(nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(pt23, pt12, false)).isFalse();
  }

  @Test
  public void pointsToEdgeOutOfBoundsWithoutMemoryLayoutCheckTest()
      throws InvalidConfigurationException, SMGSolverException {
    SMGObject reg1 = createRegion(32);
    SMGObject reg2 = createRegion(64);
    SMGPointsToEdge pt1Zero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2Zero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1Max = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2Max = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1Middle = createPTEdge(16, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2Middle = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1NegativeExceedsOne = createPTEdge(-1, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2NegativeExceedsOne = createPTEdge(-1, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1NegativeExceeds = createPTEdge(-16, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2NegativeExceeds = createPTEdge(-32, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1Exceeds = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2Exceeds = createPTEdge(64, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1ExceedsMore = createPTEdge(48, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2ExceedsMore = createPTEdge(128, SMGTargetSpecifier.IS_REGION, reg2);

    List<SMGPointsToEdge> allExceedingEdges =
        ImmutableList.of(
            pt1Exceeds,
            pt2Exceeds,
            pt1NegativeExceedsOne,
            pt2NegativeExceedsOne,
            pt1NegativeExceeds,
            pt2NegativeExceeds,
            pt1ExceedsMore,
            pt2ExceedsMore);
    List<SMGPointsToEdge> allNotExceedingEdges =
        ImmutableList.of(pt1Zero, pt2Zero, pt1Max, pt2Max, pt1Middle, pt2Middle);

    // State is irrelevant for this check (as we only use concrete offsets)
    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);

    for (SMGPointsToEdge exceedingEdge : allExceedingEdges) {
      for (SMGPointsToEdge notExceedingEdge : allNotExceedingEdges) {
        assertThat(
                nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(
                    exceedingEdge, notExceedingEdge, true))
            .isTrue();
        assertThat(
                nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(
                    notExceedingEdge, exceedingEdge, true))
            .isFalse();
      }
    }

    for (SMGPointsToEdge exceedingEdge : allExceedingEdges) {
      for (SMGPointsToEdge otherExceedingEdge : allExceedingEdges) {
        assertThat(
                nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(
                    exceedingEdge, otherExceedingEdge, true))
            .isTrue();
        assertThat(
                nequality.checkIfEdgePointsOutOfBoundsAndMayOverlap(
                    otherExceedingEdge, exceedingEdge, true))
            .isTrue();
      }
    }
  }
}

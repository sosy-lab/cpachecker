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
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1, pt2)).isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2, pt1)).isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1, pt2)).isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2, pt1)).isFalse();

    SMGPointsToEdge pt22 = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg2);

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1, pt22)).isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt22, pt1)).isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1, pt22)).isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt22, pt1)).isFalse();

    SMGPointsToEdge pt12 = createPTEdge(31, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt23 = createPTEdge(63, SMGTargetSpecifier.IS_REGION, reg2);

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt12, pt23)).isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt23, pt12)).isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt12, pt23)).isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt23, pt12)).isFalse();
  }

  @Test
  public void pointsToEdgeOutOfBoundsWithoutMemoryLayoutCheckTest()
      throws InvalidConfigurationException, SMGSolverException {
    SMGObject reg1 = createRegion(32);
    SMGObject reg2 = createRegion(64);
    SMGPointsToEdge pt1Zero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2Zero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1Max = createPTEdge(31, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2Max = createPTEdge(63, SMGTargetSpecifier.IS_REGION, reg2);

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
        assertThat(nequality.checkPointsToEdgeOutOfBounds(exceedingEdge)).isTrue();
        assertThat(nequality.checkPointsToEdgeOutOfBounds(notExceedingEdge)).isFalse();
      }
    }

    for (SMGPointsToEdge exceedingEdge : allExceedingEdges) {
      for (SMGPointsToEdge otherExceedingEdge : allExceedingEdges) {
        assertThat(nequality.checkPointsToEdgeOutOfBounds(exceedingEdge)).isTrue();
        assertThat(nequality.checkPointsToEdgeOutOfBounds(otherExceedingEdge)).isTrue();
      }
    }
  }

  @Test
  public void pointsToEdgeOutOfBoundsWithMemoryLayoutCheckTest()
      throws InvalidConfigurationException, SMGSolverException {
    SMGObject reg1 = createRegion(32);
    SMGObject reg2 = createRegion(64);
    SMGObject reg3 = createRegion(128);

    SMGPointsToEdge pt1OffsetZero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2OffsetZero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt3OffsetZero = createPTEdge(0, SMGTargetSpecifier.IS_REGION, reg3);
    SMGPointsToEdge pt3Offset32 = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg3);
    SMGPointsToEdge pt3Offset64 = createPTEdge(64, SMGTargetSpecifier.IS_REGION, reg3);

    SMGPointsToEdge pt1OffsetMax = createPTEdge(31, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2OffsetMax = createPTEdge(63, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt2Offset32 = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1NegativeExceededBy1 = createPTEdge(-1, SMGTargetSpecifier.IS_REGION, reg1);

    SMGPointsToEdge pt1NegativeExceededBy16 = createPTEdge(-16, SMGTargetSpecifier.IS_REGION, reg1);

    SMGPointsToEdge pt1ExceededBy1 = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg1);

    SMGPointsToEdge pt1ExceededBy16 = createPTEdge(48, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2ExceededBy64 = createPTEdge(128, SMGTargetSpecifier.IS_REGION, reg2);

    SMGPointsToEdge pt1ExceededBy32 = createPTEdge(64, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt2ExceededBy128 = createPTEdge(192, SMGTargetSpecifier.IS_REGION, reg2);

    // State is irrelevant for this check (as we only use concrete offsets)
    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1OffsetZero, pt3OffsetZero))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1OffsetMax, pt3OffsetZero))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1OffsetMax, pt3Offset32))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1OffsetMax, pt3Offset64))
        .isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt3OffsetZero))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt3Offset32))
        .isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy16, pt3OffsetZero))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy16, pt3Offset32))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy16, pt2OffsetZero))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy16, pt2Offset32))
        .isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt2OffsetZero))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt2Offset32))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt2OffsetMax))
        .isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt3OffsetZero))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt3Offset32))
        .isTrue();
    // The memory has 64 bits space before the pointer, so this can not match!
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt3Offset64))
        .isFalse();

    // None can succeed for exceeding negatively by 1, as the rest of the memory can not fit
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy1, pt3OffsetZero))
        .isFalse();

    // region 2 is too large for this to match
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy16, pt2OffsetZero))
        .isFalse();
    // region 3 is too large for this to match
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy16, pt3OffsetZero))
        .isFalse();

    // region 2 is too large for this to match
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy16, pt2Offset32))
        .isFalse();
    // region 3 is too large for this to match
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy16, pt3Offset32))
        .isFalse();

    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy16, pt2OffsetMax))
        .isTrue();
    // region 3 is too large for this to match
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(pt1NegativeExceededBy16, pt3Offset64))
        .isFalse();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2ExceededBy64, pt3Offset32))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2ExceededBy64, pt3Offset64))
        .isTrue();

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2ExceededBy128, pt3Offset32))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2ExceededBy128, pt3OffsetZero))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt2ExceededBy128, pt3Offset64))
        .isTrue();
  }

  /*
   * Test for both pointers exceeding (both positive or both negative),
   * as then we might need to take the other memory structure into account.
   * E.g. 2 memory structures 32 bit, both pointer have offset 32 bit, they can't be equal,
   * as both are at the end of their memory. If one is offset 64 bit, it may be equal to the 32
   * bit one, as the memory of the 32 bit fits in between the end of the 64 bit
   * offset memory and the pointers locations.
   */
  @Test
  public void twoPointsToEdgesOutOfBoundsWithEqualSignumWithMemoryLayoutCheckTest()
      throws InvalidConfigurationException, SMGSolverException {
    SMGObject reg1 = createRegion(32);
    SMGObject reg2 = createRegion(64);

    SMGPointsToEdge pt1ExceededBy32 = createPTEdge(64, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1ExceededBy63 = createPTEdge(95, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1ExceededBy64 = createPTEdge(96, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1ExceededBy65 = createPTEdge(97, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1ExceededBy95 = createPTEdge(127, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1ExceededBy96 = createPTEdge(128, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1ExceededBy1 = createPTEdge(32, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1NegativeExceededBy16 = createPTEdge(-16, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1NegativeExceededBy1 = createPTEdge(-1, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1NegativeExceededBy32 = createPTEdge(-32, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1NegativeExceededBy64 = createPTEdge(-64, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1NegativeExceededBy65 = createPTEdge(-65, SMGTargetSpecifier.IS_REGION, reg1);
    SMGPointsToEdge pt1NegativeExceededBy96 = createPTEdge(-96, SMGTargetSpecifier.IS_REGION, reg1);

    SMGPointsToEdge pt2ExceededBy32 = createPTEdge(96, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2ExceededBy33 = createPTEdge(97, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2ExceededBy64 = createPTEdge(128, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2ExceededBy128 = createPTEdge(192, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2NegativeExceededBy1 = createPTEdge(-1, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2NegativeExceededBy32 = createPTEdge(-32, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2NegativeExceededBy33 = createPTEdge(-33, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2NegativeExceededBy48 = createPTEdge(-48, SMGTargetSpecifier.IS_REGION, reg2);
    SMGPointsToEdge pt2ExceededBy1 = createPTEdge(64, SMGTargetSpecifier.IS_REGION, reg2);

    // State is irrelevant for this check (as we only use concrete offsets)
    SMGProveNequality nequality = new SMGProveNequality(SMGCPATest0.stateFromSMG(smg), options);

    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt2ExceededBy1))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt2ExceededBy1))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy63, pt2ExceededBy1))
        .isFalse();
    // region 2 fits in between region 1 and its pointer + offsets (exactly) here, with buffer below
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy64, pt2ExceededBy1))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy65, pt2ExceededBy1))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy95, pt2ExceededBy1))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy96, pt2ExceededBy1))
        .isTrue();

    // With the layout: reg2 follow by reg1 directly, the offsets can match exactly here
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt2ExceededBy32))
        .isTrue();
    // Now the offset of region1s pointer exceeds the reach of region2s pointer
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy32, pt2ExceededBy32))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy63, pt2ExceededBy32))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy64, pt2ExceededBy32))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy65, pt2ExceededBy32))
        .isFalse();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy95, pt2ExceededBy32))
        .isFalse();
    // region 2 fits in between region 1 and its pointer + offsets
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy96, pt2ExceededBy32))
        .isTrue();

    // Reg1 fits in between reg2 and its pointer
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt2ExceededBy33))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt2ExceededBy64))
        .isTrue();
    assertThat(nequality.checkPointsToEdgesOutOfBoundsEquality(pt1ExceededBy1, pt2ExceededBy128))
        .isTrue();

    // reg 2 fits not in between reg1 and its pointer and vice versa
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt1NegativeExceededBy64, pt2NegativeExceededBy1))
        .isFalse();
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt1NegativeExceededBy32, pt2NegativeExceededBy1))
        .isFalse();
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt1NegativeExceededBy1, pt2NegativeExceededBy1))
        .isFalse();
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt2NegativeExceededBy32, pt1NegativeExceededBy1))
        .isFalse();

    // reg 1 fits in between reg2 and its pointer and vice versa
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt1NegativeExceededBy65, pt2NegativeExceededBy1))
        .isTrue();
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt1NegativeExceededBy96, pt2NegativeExceededBy32))
        .isTrue();

    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt2NegativeExceededBy33, pt1NegativeExceededBy1))
        .isTrue();
    assertThat(
            nequality.checkPointsToEdgesOutOfBoundsEquality(
                pt2NegativeExceededBy48, pt1NegativeExceededBy16))
        .isTrue();
  }
}

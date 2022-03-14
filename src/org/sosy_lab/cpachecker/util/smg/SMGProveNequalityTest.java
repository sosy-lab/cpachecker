// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
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
  private final SMGValue value1 = createValue("value1");
  private final SMGValue value2 = createValue("value2");
  private final SMGValue value3 = createValue("value3");
  private final SMGValue value4 = createValue("value4");

  @Before
  public void setUp() {
    smg = new SMG(mockType8bSize);
    smg =
        smg.copyAndAddValue(value1)
            .copyAndAddValue(value2)
            .copyAndAddValue(value3)
            .copyAndAddValue(value4);
  }

  @Test
  public void equalValuesAreNotInequal() {
    SMGProveNequality nequality = new SMGProveNequality(smg);

    assertThat(nequality.proveInequality(value1, value1)).isFalse();
    assertThat(nequality.proveInequality(value2, value2)).isFalse();
    assertThat(nequality.proveInequality(value3, value3)).isFalse();
  }

  @Test
  public void pointerValuesThatShareTargetValuesAreNotInEqual() {
    SMGDoublyLinkedListSegment dlls1 = createDLLS(64, 0, 32, 32, "dlls1");
    SMGDoublyLinkedListSegment dlls2 = createDLLS(64, 0, 32, 32, "dlls2");
    SMGPointsToEdge pt1 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls1);
    SMGPointsToEdge pt2 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls2);

    smg = smg.copyAndAddPTEdge(pt1, value1);
    smg = smg.copyAndAddPTEdge(pt2, value2);

    SMGHasValueEdge hasValueEdge1 = createHasValueEdge(64, 32, value3);
    SMGHasValueEdge hasValueEdge2 = createHasValueEdge(64, 32, value3);

    smg = smg.copyAndAddHVEdge(hasValueEdge1, dlls1);
    smg = smg.copyAndAddHVEdge(hasValueEdge2, dlls2);

    SMGPointsToEdge pt3 =
        createPTEdge(64, SMGTargetSpecifier.IS_FIRST_POINTER, SMGObject.nullInstance());
    smg = smg.copyAndAddPTEdge(pt3, value3);

    SMGProveNequality nequality = new SMGProveNequality(smg);
    assertThat(nequality.proveInequality(value1, value2)).isFalse();
  }

  @Test
  public void pointerValuesThatHaveSharedObjectsAreNotInEqual() {
    SMGDoublyLinkedListSegment dlls1 = createDLLS(64, 0, 32, 32, "dlls1");
    SMGDoublyLinkedListSegment dlls2 = createDLLS(64, 0, 32, 32, "dlls2");
    SMGPointsToEdge pt1 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls1);
    SMGPointsToEdge pt2 = createPTEdge(0, SMGTargetSpecifier.IS_FIRST_POINTER, dlls2);

    smg = smg.copyAndAddPTEdge(pt1, value1);
    smg = smg.copyAndAddPTEdge(pt2, value2);

    SMGHasValueEdge hasValueEdge1 = createHasValueEdge(64, 32, value3);
    SMGHasValueEdge hasValueEdge2 = createHasValueEdge(64, 32, value4);

    smg = smg.copyAndAddHVEdge(hasValueEdge1, dlls1);
    smg = smg.copyAndAddHVEdge(hasValueEdge2, dlls2);

    SMGDoublyLinkedListSegment dlls3 = createDLLS(64, 0, 32, 0, "dlls3");

    SMGPointsToEdge pt4 = createPTEdge(0, SMGTargetSpecifier.IS_LAST_POINTER, dlls3);
    SMGPointsToEdge pt5 = createPTEdge(32, SMGTargetSpecifier.IS_FIRST_POINTER, dlls3);
    smg = smg.copyAndAddPTEdge(pt4, value3);
    smg = smg.copyAndAddPTEdge(pt5, value4);
    final SMGValue value5 = createValue("value5");
    final SMGValue value6 = createValue("value6");

    SMGHasValueEdge hasValueEdge3 = createHasValueEdge(64, 32, value5);
    SMGHasValueEdge hasValueEdge4 = createHasValueEdge(64, 0, value6);

    smg =
        smg.copyAndAddValue(value5)
            .copyAndAddValue(value6)
            .copyAndAddHVEdge(hasValueEdge4, dlls3)
            .copyAndAddHVEdge(hasValueEdge3, dlls3);
    SMGPointsToEdge pt6 =
        createPTEdge(0, SMGTargetSpecifier.IS_LAST_POINTER, SMGObject.nullInstance());
    SMGPointsToEdge pt7 =
        createPTEdge(32, SMGTargetSpecifier.IS_FIRST_POINTER, SMGObject.nullInstance());

    smg = smg.copyAndAddPTEdge(pt6, value5);
    smg = smg.copyAndAddPTEdge(pt7, value6);

    SMGProveNequality nequality = new SMGProveNequality(smg);
    assertThat(nequality.proveInequality(value1, value2)).isFalse();
  }
}

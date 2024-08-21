// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.DummyAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;

public class SMGSingleLinkedListTest {

  @Test
  public void basicsTest() {
    SMGSingleLinkedList sll = new SMGSingleLinkedList(128, 0, 2, 4, 0);

    assertThat(sll.isAbstract()).isTrue();
    assertThat(sll.getMinimumLength()).isEqualTo(4);
    assertThat(sll.getSize()).isEqualTo(128);
    assertThat(sll.getNfo()).isEqualTo(2);
  }

  @Test
  public void matchGenericShapeTest() {
    SMGRegion prototype = new SMGRegion(128, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(128, 0, 0, 4, 0);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(128, 0, 0, 7, 0);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(128, 0, 8, 4, 0);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    assertThat(sll1.matchGenericShape(dummy)).isFalse();
    assertThat(sll1.matchGenericShape(sll2)).isTrue();
    assertThat(sll2.matchGenericShape(sll3)).isTrue();
    assertThat(sll1.matchGenericShape(sll3)).isTrue();
  }

  @Test
  public void matchSpecificShapeTest() {
    SMGRegion prototype = new SMGRegion(128, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(128, 0, 0, 4, 0);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(128, 0, 0, 7, 0);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(128, 0, 8, 4, 0);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    assertThat(sll1.matchSpecificShape(dummy)).isFalse();
    assertThat(sll1.matchSpecificShape(sll2)).isTrue();
    assertThat(sll2.matchSpecificShape(sll3)).isFalse();
    assertThat(sll1.matchSpecificShape(sll3)).isFalse();
  }
}

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

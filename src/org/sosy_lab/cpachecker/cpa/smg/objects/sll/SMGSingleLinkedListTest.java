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
package org.sosy_lab.cpachecker.cpa.smg.objects.sll;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.objects.DummyAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGSingleLinkedListTest {

  @Test
  public void basicsTest() {
    SMGSingleLinkedList sll = new SMGSingleLinkedList(16, 0, 2, 4, 0);

    Assert.assertTrue(sll.isAbstract());
    Assert.assertEquals(4, sll.getMinimumLength());
    Assert.assertEquals(16, sll.getSize());
    Assert.assertEquals(2, sll.getNfo());
  }

  @Test
  public void matchGenericShapeTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(16, 0, 0, 4, 0);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(16, 0, 0, 7, 0);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(16, 0, 8, 4, 0);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    Assert.assertFalse(sll1.matchGenericShape(dummy));
    Assert.assertTrue(sll1.matchGenericShape(sll2));
    Assert.assertTrue(sll2.matchGenericShape(sll3));
    Assert.assertTrue(sll1.matchGenericShape(sll3));
  }

  @Test
  public void matchSpecificShapeTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(16, 0, 0, 4, 0);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(16, 0, 0, 7, 0);
    SMGSingleLinkedList sll3 = new SMGSingleLinkedList(16, 0, 8, 4, 0);

    DummyAbstraction dummy = new DummyAbstraction(prototype);

    Assert.assertFalse(sll1.matchSpecificShape(dummy));
    Assert.assertTrue(sll1.matchSpecificShape(sll2));
    Assert.assertFalse(sll2.matchSpecificShape(sll3));
    Assert.assertFalse(sll1.matchSpecificShape(sll3));
  }
}

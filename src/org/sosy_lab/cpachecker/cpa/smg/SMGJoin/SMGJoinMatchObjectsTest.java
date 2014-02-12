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
package org.sosy_lab.cpachecker.cpa.smg.SMGJoin;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.SMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.objects.DummyAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedList;


public class SMGJoinMatchObjectsTest {

  static private final CType mockType2b = AnonymousTypes.createTypeWithLength(2);

  private SMG smg1;
  private SMG smg2;

  final private SMGObject srcObj1 = new SMGRegion(8, "Source object 1");
  final private SMGObject destObj1 = new SMGRegion(8, "Destination object 1");
  final private SMGObject srcObj2 = new SMGRegion(8, "Source object 2");
  final private SMGObject destObj2 = new SMGRegion(8, "Destination object 2");

  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  @Before
  public void setUp() {
    smg1 = new SMG(MachineModel.LINUX64);
    smg2 = new SMG(MachineModel.LINUX64);

    mapping1 = new SMGNodeMapping();
    mapping2 = new SMGNodeMapping();
  }

  @Test
  public void nullObjectTest() {
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, smg1.getNullObject(), smg2.getNullObject());
    Assert.assertFalse(mo.isDefined());

    smg1.addObject(srcObj1);
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, srcObj1, smg2.getNullObject());
    Assert.assertFalse(mo.isDefined());
  }

  @Test(expected=IllegalArgumentException.class)
  public void nonMemberObjectsTestObj1() {
    smg2.addObject(srcObj2);
    @SuppressWarnings("unused")
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, srcObj1, srcObj2);
  }

  @Test(expected=IllegalArgumentException.class)
  public void nonMemberObjectsTestObj2() {
    smg1.addObject(srcObj1);
    @SuppressWarnings("unused")
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, srcObj1, srcObj2);
  }

  @Test
  public void inconsistentMappingTest() {
    mapping1.map(srcObj1, destObj1);
    smg1.addObject(srcObj1);

    smg2.addObject(srcObj2);
    mapping2.map(srcObj2, destObj1);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void inconsistentMappingViceVersaTest() {
    mapping2.map(srcObj2, destObj2);
    smg2.addObject(srcObj2);

    smg1.addObject(srcObj1);
    mapping1.map(srcObj1, destObj2);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void inconsistentObjectsTest() {
    SMGObject diffSizeObject = new SMGRegion(16, "Object with different size");
    smg1.addObject(srcObj1);
    smg2.addObject(diffSizeObject);
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL,  smg1, smg2, mapping1, mapping2, srcObj1, diffSizeObject);
    Assert.assertFalse(mo.isDefined());

    smg2.addObject(srcObj2, false);
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void nonMatchingMappingTest() {
    smg1.addObject(srcObj1);
    smg1.addObject(destObj1);
    mapping1.map(srcObj1, destObj1);

    smg2.addObject(srcObj2);
    smg2.addObject(destObj2);
    mapping2.map(srcObj2, destObj2);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void fieldInconsistencyTest() {
    smg1.addObject(srcObj1);
    smg2.addObject(srcObj2);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockType2b, 0, srcObj1, SMGValueFactory.getNewValue());
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockType2b, 2, srcObj2, SMGValueFactory.getNewValue());
    SMGEdgeHasValue hvMatching1 = new SMGEdgeHasValue(mockType2b, 4, srcObj1, SMGValueFactory.getNewValue());
    SMGEdgeHasValue hvMatching2 = new SMGEdgeHasValue(mockType2b, 4, srcObj2, SMGValueFactory.getNewValue());

    smg1.addHasValueEdge(hv1);
    smg1.addValue(hv1.getValue());

    smg2.addHasValueEdge(hv2);
    smg2.addValue(hv2.getValue());

    smg1.addHasValueEdge(hvMatching1);
    smg1.addValue(hvMatching1.getValue());

    smg2.addHasValueEdge(hvMatching2);
    smg2.addValue(hvMatching2.getValue());

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertTrue(mo.isDefined());

    mapping1.map(hvMatching1.getValue(), SMGValueFactory.getNewValue());
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertTrue(mo.isDefined());

    mapping2.map(hvMatching2.getValue(), mapping1.get(hvMatching1.getValue()));
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertTrue(mo.isDefined());

    mapping2.map(hvMatching2.getValue(), SMGValueFactory.getNewValue());
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void sameAbstractionMatchTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 8, 7);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 0, 7);

    smg1.addObject(sll1);
    smg2.addObject(sll2);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void differentAbstractionMatch() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(prototype, 8, 3);
    DummyAbstraction dummy = new DummyAbstraction(prototype);

    smg1.addObject(sll);
    smg2.addObject(dummy);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll, dummy);
    Assert.assertFalse(mo.isDefined());
  }

  @Test
  public void twoAbstractionsTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(prototype, 8, 2);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(prototype, 8, 4);
    smg1.addObject(sll1);
    smg2.addObject(sll2);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    Assert.assertTrue(mo.isDefined());
    Assert.assertEquals(SMGJoinStatus.LEFT_ENTAIL, mo.getStatus());

    sll1 = new SMGSingleLinkedList(prototype, 8, 4);
    smg1.addObject(sll1);
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    Assert.assertTrue(mo.isDefined());
    Assert.assertEquals(SMGJoinStatus.EQUAL, mo.getStatus());

    sll1 = new SMGSingleLinkedList(prototype, 8, 8);
    smg1.addObject(sll1);
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    Assert.assertTrue(mo.isDefined());
    Assert.assertEquals(SMGJoinStatus.RIGHT_ENTAIL, mo.getStatus());
  }

  @Test
  public void oneAbstractionTest() {
    SMGRegion prototype = new SMGRegion(16, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(prototype, 8, 8);

    smg1.addObject(sll);
    smg2.addObject(sll);
    smg1.addObject(prototype);
    smg2.addObject(prototype);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll, prototype);
    Assert.assertTrue(mo.isDefined());
    Assert.assertEquals(SMGJoinStatus.LEFT_ENTAIL, mo.getStatus());

    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, prototype, sll);
    Assert.assertTrue(mo.isDefined());
    Assert.assertEquals(SMGJoinStatus.RIGHT_ENTAIL, mo.getStatus());
  }

  @Test
  public void noAbstractionTest() {
    SMGRegion object = new SMGRegion(16, "prototype");
    smg1.addObject(object);
    smg2.addObject(object);
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, object, object);
    Assert.assertTrue(mo.isDefined());
    Assert.assertEquals(SMGJoinStatus.EQUAL, mo.getStatus());
  }
}

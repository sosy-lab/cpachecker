/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.cpalien2.SMGJoin;

import java.math.BigInteger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.cpalien2.SMG;
import org.sosy_lab.cpachecker.cpa.cpalien2.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.cpalien2.SMGObject;
import org.sosy_lab.cpachecker.cpa.cpalien2.SMGValueFactory;


public class SMGJoinMatchObjectsTest {

  final private CSimpleType dummyInt = new CSimpleType(false, false, CBasicType.INT, true, false, false, true, false, false, false);
  final private CSimpleType dummyChar = new CSimpleType(false, false, CBasicType.CHAR, false, false, true, false, false, false, false);
  final private CIntegerLiteralExpression arrayLen2 = new CIntegerLiteralExpression(null, dummyInt, BigInteger.valueOf(2));
  private final CType mockType2b = new CArrayType(false, false, dummyChar, arrayLen2);

  private SMG smg1;
  private SMG smg2;

  final private SMGObject srcObj1 = new SMGObject(8, "Source object 1");
  final private SMGObject destObj1 = new SMGObject(8, "Destination object 1");
  final private SMGObject srcObj2 = new SMGObject(8, "Source object 2");
  final private SMGObject destObj2 = new SMGObject(8, "Destination object 2");

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
    SMGObject diffSizeObject = new SMGObject(16, "Object with different size");
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
}

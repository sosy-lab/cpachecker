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
package org.sosy_lab.cpachecker.cpa.cpalien;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;


public class SMGEdgeHasValueTest {

  CType mockType = mock(CType.class);
  CType mockType12b = mock(CType.class);

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    when(mockType.accept((CTypeVisitor<Integer, IllegalArgumentException>)(anyObject()))).thenReturn(Integer.valueOf(4));
    when(mockType12b.accept((CTypeVisitor<Integer, IllegalArgumentException>)(anyObject()))).thenReturn(Integer.valueOf(12));
  }

  @Test
  public void testSMGEdgeHasValue() {
    SMGObject obj = new SMGObject(8, "object");
    Integer val = Integer.valueOf(666);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(mockType, 4, obj, val);

    Assert.assertEquals(obj, hv.getObject());
    Assert.assertEquals(4, hv.getOffset());
    Assert.assertEquals(mockType, hv.getType());
    Assert.assertEquals(4, hv.getSizeInBytes(MachineModel.LINUX64));
  }

  @Test
  public void testIsConsistentWith() {
    SMGObject obj1 = new SMGObject(8, "object");
    SMGObject obj2 = new SMGObject(8, "different object");
    Integer val1 = Integer.valueOf(666);
    Integer val2 = Integer.valueOf(777);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockType, 0, obj1, val1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockType, 4, obj1, val2);
    SMGEdgeHasValue hv3 = new SMGEdgeHasValue(mockType, 4, obj1, val1);
    SMGEdgeHasValue hv4 = new SMGEdgeHasValue(mockType, 4, obj2, val1);

    Assert.assertTrue(hv1.isConsistentWith(hv1));
    Assert.assertTrue(hv1.isConsistentWith(hv2));
    Assert.assertTrue(hv1.isConsistentWith(hv3));
    Assert.assertFalse(hv2.isConsistentWith(hv3));
    Assert.assertTrue(hv2.isConsistentWith(hv4));
  }

  @Test
  public void testOverlapsWith() {
    SMGObject object = new SMGObject(12, "object");
    Integer value = Integer.valueOf(666);

    SMGEdgeHasValue at0 = new SMGEdgeHasValue(mockType, 0, object, value);
    SMGEdgeHasValue at2 = new SMGEdgeHasValue(mockType, 2, object, value);
    SMGEdgeHasValue at4 = new SMGEdgeHasValue(mockType, 4, object, value);
    SMGEdgeHasValue at6 = new SMGEdgeHasValue(mockType, 6, object, value);

    Assert.assertTrue(at0.overlapsWith(at2, MachineModel.LINUX64));
    Assert.assertTrue(at2.overlapsWith(at0, MachineModel.LINUX64));
    Assert.assertTrue(at2.overlapsWith(at4, MachineModel.LINUX64));
    Assert.assertTrue(at4.overlapsWith(at2, MachineModel.LINUX64));
    Assert.assertTrue(at4.overlapsWith(at6, MachineModel.LINUX64));
    Assert.assertTrue(at6.overlapsWith(at4, MachineModel.LINUX64));

    Assert.assertTrue(at0.overlapsWith(at0, MachineModel.LINUX64));

    Assert.assertFalse(at0.overlapsWith(at4, MachineModel.LINUX64));
    Assert.assertFalse(at0.overlapsWith(at6, MachineModel.LINUX64));
    Assert.assertFalse(at2.overlapsWith(at6, MachineModel.LINUX64));
    Assert.assertFalse(at4.overlapsWith(at0, MachineModel.LINUX64));
    Assert.assertFalse(at6.overlapsWith(at0, MachineModel.LINUX64));
    Assert.assertFalse(at6.overlapsWith(at2, MachineModel.LINUX64));

    SMGEdgeHasValue whole = new SMGEdgeHasValue(mockType12b, 0, object, value);
    Assert.assertTrue(whole.overlapsWith(at4, MachineModel.LINUX64));
    Assert.assertTrue(at4.overlapsWith(whole, MachineModel.LINUX64));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIllegalOverlapsWith() {
    SMGObject object1 = new SMGObject(12, "object1");
    SMGObject object2 = new SMGObject(12, "object2");
    Integer value = Integer.valueOf(666);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockType, 0, object1, value);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockType, 2, object2, value);

    hv1.overlapsWith(hv2, MachineModel.LINUX64);
  }
}
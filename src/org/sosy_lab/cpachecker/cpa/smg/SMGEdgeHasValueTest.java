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
package org.sosy_lab.cpachecker.cpa.smg;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

import com.google.common.base.Predicate;


public class SMGEdgeHasValueTest {

  CType mockType = AnonymousTypes.createTypeWithLength(4);
  CType mockType12b = AnonymousTypes.createTypeWithLength(12);

  @Test
  public void testSMGEdgeHasValue() {
    SMGObject obj = new SMGRegion(8, "object");
    Integer val = Integer.valueOf(666);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(mockType, 4, obj, val);

    Assert.assertEquals(obj, hv.getObject());
    Assert.assertEquals(4, hv.getOffset());
    Assert.assertEquals(mockType, hv.getType());
    Assert.assertEquals(4, hv.getSizeInBytes(MachineModel.LINUX64));
  }

  @Test
  public void testIsConsistentWith() {
    SMGObject obj1 = new SMGRegion(8, "object");
    SMGObject obj2 = new SMGRegion(8, "different object");
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
    SMGObject object = new SMGRegion(12, "object");
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

  @Test
  public void testIsCompatibleField() {
    SMGObject object1 = new SMGRegion(12, "object-1");
    SMGObject object2 = new SMGRegion(12, "object-2");
    Integer value = Integer.valueOf(666);

    SMGEdgeHasValue obj1_at0 = new SMGEdgeHasValue(mockType, 0, object1, value);
    SMGEdgeHasValue obj1_at2 = new SMGEdgeHasValue(mockType, 2, object1, value);
    SMGEdgeHasValue obj1_at4 = new SMGEdgeHasValue(mockType, 4, object1, value);
    SMGEdgeHasValue obj1_12at0 = new SMGEdgeHasValue(mockType12b, 0, object1, value);

    SMGEdgeHasValue obj2_at0 = new SMGEdgeHasValue(mockType, 0, object2, value);
    SMGEdgeHasValue obj2_at2 = new SMGEdgeHasValue(mockType, 2, object2, value);
    SMGEdgeHasValue obj2_at4 = new SMGEdgeHasValue(mockType, 4, object2, value);
    SMGEdgeHasValue obj2_12at0 = new SMGEdgeHasValue(mockType12b, 0, object2, value);

    Assert.assertTrue(obj1_at0.isCompatibleField(obj1_at0));
    Assert.assertFalse(obj1_at0.isCompatibleField(obj1_at2));
    Assert.assertFalse(obj1_at0.isCompatibleField(obj1_at4));
    Assert.assertFalse(obj1_at0.isCompatibleField(obj1_12at0));
    Assert.assertTrue(obj1_at0.isCompatibleField(obj2_at0));
    Assert.assertFalse(obj1_at0.isCompatibleField(obj2_at2));
    Assert.assertFalse(obj1_at0.isCompatibleField(obj2_at4));
    Assert.assertFalse(obj1_at0.isCompatibleField(obj2_12at0));
  }

  @Test
  public void testIsCompatibleFieldOnSameObject() {
    SMGObject object1 = new SMGRegion(12, "object-1");
    SMGObject object2 = new SMGRegion(12, "object-2");
    Integer value = Integer.valueOf(666);

    SMGEdgeHasValue obj1_at0 = new SMGEdgeHasValue(mockType, 0, object1, value);
    SMGEdgeHasValue obj1_at2 = new SMGEdgeHasValue(mockType, 2, object1, value);
    SMGEdgeHasValue obj1_at4 = new SMGEdgeHasValue(mockType, 4, object1, value);
    SMGEdgeHasValue obj1_12at0 = new SMGEdgeHasValue(mockType12b, 0, object1, value);

    SMGEdgeHasValue obj2_at0 = new SMGEdgeHasValue(mockType, 0, object2, value);
    SMGEdgeHasValue obj2_at2 = new SMGEdgeHasValue(mockType, 2, object2, value);
    SMGEdgeHasValue obj2_at4 = new SMGEdgeHasValue(mockType, 4, object2, value);
    SMGEdgeHasValue obj2_12at0 = new SMGEdgeHasValue(mockType12b, 0, object2, value);

    Assert.assertTrue(obj1_at0.isCompatibleFieldOnSameObject(obj1_at0, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj1_at2, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj1_at4, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj1_12at0, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_at0, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_at2, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_at4, MachineModel.LINUX64));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_12at0, MachineModel.LINUX64));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIllegalOverlapsWith() {
    SMGObject object1 = new SMGRegion(12, "object1");
    SMGObject object2 = new SMGRegion(12, "object2");
    Integer value = Integer.valueOf(666);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockType, 0, object1, value);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockType, 2, object2, value);

    hv1.overlapsWith(hv2, MachineModel.LINUX64);
  }

  @Test
  public void testFilterAsPredicate() {
    SMGObject object1 = new SMGRegion(8, "object1");

    Integer value1 = Integer.valueOf(1);
    Integer value2 = Integer.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockType, 0, object1, value2);

    Predicate<SMGEdgeHasValue> predicate = SMGEdgeHasValueFilter.objectFilter(object1).filterHavingValue(value1).asPredicate();

    Assert.assertTrue(predicate.apply(hv11at0));
    Assert.assertFalse(predicate.apply(hv12at0));
  }

  @Test
  public void testFilterOnObject() {
    SMGObject object1 = new SMGRegion(8, "object1");
    SMGObject object2 = new SMGRegion(8, "Object2");

    Integer value1 = Integer.valueOf(1);
    Integer value2 = Integer.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockType, 0, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(mockType, 0, object2, value1);
    SMGEdgeHasValue hv22at0 = new SMGEdgeHasValue(mockType, 0, object2, value2);
    Set<SMGEdgeHasValue> allEdges = new HashSet<>();
    allEdges.add(hv11at0);
    allEdges.add(hv12at0);
    allEdges.add(hv21at0);
    allEdges.add(hv22at0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertTrue(filter.holdsFor(hv12at0));
    Assert.assertTrue(filter.holdsFor(hv21at0));
    Assert.assertTrue(filter.holdsFor(hv22at0));

    filter.filterByObject(object1);

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertTrue(filter.holdsFor(hv12at0));
    Assert.assertFalse(filter.holdsFor(hv21at0));
    Assert.assertFalse(filter.holdsFor(hv22at0));

    Set<SMGEdgeHasValue> filteredSet = filter.filterSet(allEdges);

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv11at0));
    Assert.assertTrue(filteredSet.contains(hv12at0));
  }

  @Test
  public void testFilterAtOffset() {
    SMGObject object1 = new SMGRegion(8, "object1");
    SMGObject object2 = new SMGRegion(8, "Object2");

    Integer value1 = Integer.valueOf(1);
    Integer value2 = Integer.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockType, 4, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(mockType, 0, object2, value1);
    SMGEdgeHasValue hv22at0 = new SMGEdgeHasValue(mockType, 4, object2, value2);
    Set<SMGEdgeHasValue> allEdges = new HashSet<>();
    allEdges.add(hv11at0);
    allEdges.add(hv12at0);
    allEdges.add(hv21at0);
    allEdges.add(hv22at0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    filter.filterAtOffset(0);

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertFalse(filter.holdsFor(hv12at0));
    Assert.assertTrue(filter.holdsFor(hv21at0));
    Assert.assertFalse(filter.holdsFor(hv22at0));

    Set<SMGEdgeHasValue> filteredSet = filter.filterSet(allEdges);

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv11at0));
    Assert.assertTrue(filteredSet.contains(hv21at0));
  }

  @Test
  public void testFilterOnValue() {
    SMGObject object1 = new SMGRegion(8, "object1");
    SMGObject object2 = new SMGRegion(8, "Object2");

    Integer value1 = Integer.valueOf(1);
    Integer value2 = Integer.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockType, 4, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(mockType, 0, object2, value1);
    SMGEdgeHasValue hv22at0 = new SMGEdgeHasValue(mockType, 4, object2, value2);
    Set<SMGEdgeHasValue> allEdges = new HashSet<>();
    allEdges.add(hv11at0);
    allEdges.add(hv12at0);
    allEdges.add(hv21at0);
    allEdges.add(hv22at0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    filter.filterHavingValue(value1);

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertFalse(filter.holdsFor(hv12at0));
    Assert.assertTrue(filter.holdsFor(hv21at0));
    Assert.assertFalse(filter.holdsFor(hv22at0));

    Set<SMGEdgeHasValue> filteredSet = filter.filterSet(allEdges);

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv11at0));
    Assert.assertTrue(filteredSet.contains(hv21at0));

    filter.filterNotHavingValue(value1);

    Assert.assertFalse(filter.holdsFor(hv11at0));
    Assert.assertTrue(filter.holdsFor(hv12at0));
    Assert.assertFalse(filter.holdsFor(hv21at0));
    Assert.assertTrue(filter.holdsFor(hv22at0));

    filteredSet = filter.filterSet(allEdges);

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv22at0));
    Assert.assertTrue(filteredSet.contains(hv12at0));
  }
}
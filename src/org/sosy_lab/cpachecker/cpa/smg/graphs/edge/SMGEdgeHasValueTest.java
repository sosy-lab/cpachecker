/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdgeSet;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgeHasValueTest {

  CType mockType = TypeUtils.createTypeWithLength(32);
  CType mockType12b = TypeUtils.createTypeWithLength(96);

  @Test
  public void testSMGEdgeHasValue() {
    SMGObject obj = new SMGRegion(64, "object");
    SMGValue val = SMGKnownExpValue.valueOf(666);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, obj, val);

    Assert.assertEquals(obj, hv.getObject());
    Assert.assertEquals(32, hv.getOffset());
    Assert.assertEquals(mockType, hv.getType());
    Assert.assertEquals(32, hv.getSizeInBits());
  }

  @Test
  public void testIsConsistentWith() {
    SMGObject obj1 = new SMGRegion(64, "object");
    SMGObject obj2 = new SMGRegion(64, "different object");
    SMGValue val1 = SMGKnownExpValue.valueOf(666);
    SMGValue val2 = SMGKnownExpValue.valueOf(777);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, obj1, val1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, obj1, val2);
    SMGEdgeHasValue hv3 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, obj1, val1);
    SMGEdgeHasValue hv4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, obj2, val1);

    Assert.assertTrue(hv1.isConsistentWith(hv1));
    Assert.assertTrue(hv1.isConsistentWith(hv2));
    Assert.assertTrue(hv1.isConsistentWith(hv3));
    Assert.assertFalse(hv2.isConsistentWith(hv3));
    Assert.assertTrue(hv2.isConsistentWith(hv4));
  }

  @Test
  public void testOverlapsWith() {
    SMGObject object = new SMGRegion(96, "object");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object, value);
    SMGEdgeHasValue at2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 16, object, value);
    SMGEdgeHasValue at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object, value);
    SMGEdgeHasValue at6 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 48, object, value);

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

    SMGEdgeHasValue whole = new SMGEdgeHasValue(MachineModel.LINUX64, mockType12b, 0, object, value);
    Assert.assertTrue(whole.overlapsWith(at4, MachineModel.LINUX64));
    Assert.assertTrue(at4.overlapsWith(whole, MachineModel.LINUX64));
  }

  @Test
  public void testIsCompatibleField() {
    SMGObject object1 = new SMGRegion(96, "object-1");
    SMGObject object2 = new SMGRegion(96, "object-2");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue obj1_at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value);
    SMGEdgeHasValue obj1_at2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 16, object1, value);
    SMGEdgeHasValue obj1_at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object1, value);
    SMGEdgeHasValue obj1_12at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType12b, 0, object1, value);

    SMGEdgeHasValue obj2_at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object2, value);
    SMGEdgeHasValue obj2_at2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 16, object2, value);
    SMGEdgeHasValue obj2_at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object2, value);
    SMGEdgeHasValue obj2_12at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType12b, 0, object2, value);

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
    SMGObject object1 = new SMGRegion(96, "object-1");
    SMGObject object2 = new SMGRegion(96, "object-2");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue obj1_at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value);
    SMGEdgeHasValue obj1_at2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 16, object1, value);
    SMGEdgeHasValue obj1_at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object1, value);
    SMGEdgeHasValue obj1_12at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType12b, 0, object1, value);

    SMGEdgeHasValue obj2_at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object2, value);
    SMGEdgeHasValue obj2_at2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 16, object2, value);
    SMGEdgeHasValue obj2_at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object2, value);
    SMGEdgeHasValue obj2_12at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType12b, 0, object2, value);

    Assert.assertTrue(obj1_at0.isCompatibleFieldOnSameObject(obj1_at0));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj1_at2));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj1_at4));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj1_12at0));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_at0));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_at2));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_at4));
    Assert.assertFalse(obj1_at0.isCompatibleFieldOnSameObject(obj2_12at0));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIllegalOverlapsWith() {
    SMGObject object1 = new SMGRegion(96, "object1");
    SMGObject object2 = new SMGRegion(96, "object2");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 16, object2, value);

    hv1.overlapsWith(hv2, MachineModel.LINUX64);
  }

  @Test
  public void testFilterAsPredicate() {
    SMGObject object1 = new SMGRegion(64, "object1");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value2);

    Predicate<SMGEdgeHasValue> predicate =
        SMGEdgeHasValueFilter.objectFilter(object1).filterHavingValue(value1)::holdsFor;

    Assert.assertTrue(predicate.apply(hv11at0));
    Assert.assertFalse(predicate.apply(hv12at0));
  }

  @Test
  public void testFilterOnObject() {
    SMGObject object1 = new SMGRegion(64, "object1");
    SMGObject object2 = new SMGRegion(64, "Object2");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object2, value1);
    SMGEdgeHasValue hv22at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object2, value2);
    SMGHasValueEdges allEdges = new SMGHasValueEdgeSet();
    allEdges = allEdges.addEdgeAndCopy(hv11at0);
    allEdges = allEdges.addEdgeAndCopy(hv12at4);
    allEdges = allEdges.addEdgeAndCopy(hv21at0);
    allEdges = allEdges.addEdgeAndCopy(hv22at4);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertTrue(filter.holdsFor(hv12at4));
    Assert.assertTrue(filter.holdsFor(hv21at0));
    Assert.assertTrue(filter.holdsFor(hv22at4));

    filter.filterByObject(object1);

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertTrue(filter.holdsFor(hv12at4));
    Assert.assertFalse(filter.holdsFor(hv21at0));
    Assert.assertFalse(filter.holdsFor(hv22at4));

    Set<SMGEdgeHasValue> filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv11at0));
    Assert.assertTrue(filteredSet.contains(hv12at4));
  }

  @Test
  public void testFilterAtOffset() {
    SMGObject object1 = new SMGRegion(64, "object1");
    SMGObject object2 = new SMGRegion(64, "Object2");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object2, value1);
    SMGEdgeHasValue hv22at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object2, value2);
    SMGHasValueEdges allEdges = new SMGHasValueEdgeSet();
    allEdges = allEdges.addEdgeAndCopy(hv11at0);
    allEdges = allEdges.addEdgeAndCopy(hv12at4);
    allEdges = allEdges.addEdgeAndCopy(hv21at0);
    allEdges = allEdges.addEdgeAndCopy(hv22at4);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    filter.filterAtOffset(0);

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertFalse(filter.holdsFor(hv12at4));
    Assert.assertTrue(filter.holdsFor(hv21at0));
    Assert.assertFalse(filter.holdsFor(hv22at4));

    Set<SMGEdgeHasValue> filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv11at0));
    Assert.assertTrue(filteredSet.contains(hv21at0));
  }

  @Test
  public void testFilterOnValue() {
    SMGObject object1 = new SMGRegion(64, "object1");
    SMGObject object2 = new SMGRegion(64, "Object2");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object1, value1);
    SMGEdgeHasValue hv12at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 0, object2, value1);
    SMGEdgeHasValue hv22at4 = new SMGEdgeHasValue(MachineModel.LINUX64, mockType, 32, object2, value2);
    SMGHasValueEdges allEdges = new SMGHasValueEdgeSet();
    allEdges = allEdges.addEdgeAndCopy(hv11at0);
    allEdges = allEdges.addEdgeAndCopy(hv12at4);
    allEdges = allEdges.addEdgeAndCopy(hv21at0);
    allEdges = allEdges.addEdgeAndCopy(hv22at4);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    filter.filterHavingValue(value1);

    Assert.assertTrue(filter.holdsFor(hv11at0));
    Assert.assertFalse(filter.holdsFor(hv12at4));
    Assert.assertTrue(filter.holdsFor(hv21at0));
    Assert.assertFalse(filter.holdsFor(hv22at4));

    Set<SMGEdgeHasValue> filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv11at0));
    Assert.assertTrue(filteredSet.contains(hv21at0));

    filter.filterNotHavingValue(value1);

    Assert.assertFalse(filter.holdsFor(hv11at0));
    Assert.assertTrue(filter.holdsFor(hv12at4));
    Assert.assertFalse(filter.holdsFor(hv21at0));
    Assert.assertTrue(filter.holdsFor(hv22at4));

    filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    Assert.assertEquals(2, filteredSet.size());
    Assert.assertTrue(filteredSet.contains(hv22at4));
    Assert.assertTrue(filteredSet.contains(hv12at4));
  }
}
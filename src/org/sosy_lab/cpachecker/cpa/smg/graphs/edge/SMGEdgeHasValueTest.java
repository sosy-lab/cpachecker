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

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGEdgeHasValueTest {

  private static final int mockTypeSize = 32;
  private static final int mockTypeSize12b = 96;

  @Test
  public void testSMGEdgeHasValue() {
    SMGObject obj = new SMGRegion(64, "object");
    SMGValue val = SMGKnownExpValue.valueOf(666);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(mockTypeSize, 32, obj, val);

    assertThat(hv.getObject()).isEqualTo(obj);
    assertThat(hv.getOffset()).isEqualTo(32);
    assertThat(hv.getSizeInBits()).isEqualTo(mockTypeSize);
    assertThat(hv.getSizeInBits()).isEqualTo(32);
  }

  @Test
  public void testIsConsistentWith() {
    SMGObject obj1 = new SMGRegion(64, "object");
    SMGObject obj2 = new SMGRegion(64, "different object");
    SMGValue val1 = SMGKnownExpValue.valueOf(666);
    SMGValue val2 = SMGKnownExpValue.valueOf(777);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockTypeSize, 0, obj1, val1);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockTypeSize, 32, obj1, val2);
    SMGEdgeHasValue hv3 = new SMGEdgeHasValue(mockTypeSize, 32, obj1, val1);
    SMGEdgeHasValue hv4 = new SMGEdgeHasValue(mockTypeSize, 32, obj2, val1);

    assertThat(hv1.isConsistentWith(hv1)).isTrue();
    assertThat(hv1.isConsistentWith(hv2)).isTrue();
    assertThat(hv1.isConsistentWith(hv3)).isTrue();
    assertThat(hv2.isConsistentWith(hv3)).isFalse();
    assertThat(hv2.isConsistentWith(hv4)).isTrue();
  }

  @Test
  public void testOverlapsWith() {
    SMGObject object = new SMGRegion(96, "object");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue at0 = new SMGEdgeHasValue(mockTypeSize, 0, object, value);
    SMGEdgeHasValue at2 = new SMGEdgeHasValue(mockTypeSize, 16, object, value);
    SMGEdgeHasValue at4 = new SMGEdgeHasValue(mockTypeSize, 32, object, value);
    SMGEdgeHasValue at6 = new SMGEdgeHasValue(mockTypeSize, 48, object, value);

    assertThat(at0.overlapsWith(at2)).isTrue();
    assertThat(at2.overlapsWith(at0)).isTrue();
    assertThat(at2.overlapsWith(at4)).isTrue();
    assertThat(at4.overlapsWith(at2)).isTrue();
    assertThat(at4.overlapsWith(at6)).isTrue();
    assertThat(at6.overlapsWith(at4)).isTrue();

    assertThat(at0.overlapsWith(at0)).isTrue();

    assertThat(at0.overlapsWith(at4)).isFalse();
    assertThat(at0.overlapsWith(at6)).isFalse();
    assertThat(at2.overlapsWith(at6)).isFalse();
    assertThat(at4.overlapsWith(at0)).isFalse();
    assertThat(at6.overlapsWith(at0)).isFalse();
    assertThat(at6.overlapsWith(at2)).isFalse();

    SMGEdgeHasValue whole = new SMGEdgeHasValue(mockTypeSize12b, 0, object, value);
    assertThat(whole.overlapsWith(at4)).isTrue();
    assertThat(at4.overlapsWith(whole)).isTrue();
  }

  @Test
  public void testIsCompatibleFieldOnSameObject() {
    SMGObject object1 = new SMGRegion(96, "object-1");
    SMGObject object2 = new SMGRegion(96, "object-2");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue obj1_at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value);
    SMGEdgeHasValue obj1_at2 = new SMGEdgeHasValue(mockTypeSize, 16, object1, value);
    SMGEdgeHasValue obj1_at4 = new SMGEdgeHasValue(mockTypeSize, 32, object1, value);
    SMGEdgeHasValue obj1_12at0 = new SMGEdgeHasValue(mockTypeSize12b, 0, object1, value);

    SMGEdgeHasValue obj2_at0 = new SMGEdgeHasValue(mockTypeSize, 0, object2, value);
    SMGEdgeHasValue obj2_at2 = new SMGEdgeHasValue(mockTypeSize, 16, object2, value);
    SMGEdgeHasValue obj2_at4 = new SMGEdgeHasValue(mockTypeSize, 32, object2, value);
    SMGEdgeHasValue obj2_12at0 = new SMGEdgeHasValue(mockTypeSize12b, 0, object2, value);

    assertThat(obj1_at0.equals(obj1_at0)).isTrue();
    assertThat(obj1_at0.equals(obj1_at2)).isFalse();
    assertThat(obj1_at0.equals(obj1_at4)).isFalse();
    assertThat(obj1_at0.equals(obj1_12at0)).isFalse();
    assertThat(obj1_at0.equals(obj2_at0)).isFalse();
    assertThat(obj1_at0.equals(obj2_at2)).isFalse();
    assertThat(obj1_at0.equals(obj2_at4)).isFalse();
    assertThat(obj1_at0.equals(obj2_12at0)).isFalse();
  }

  @Test(expected=IllegalArgumentException.class)
  public void testIllegalOverlapsWith() {
    SMGObject object1 = new SMGRegion(96, "object1");
    SMGObject object2 = new SMGRegion(96, "object2");
    SMGValue value = SMGKnownExpValue.valueOf(666);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value);
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(mockTypeSize, 16, object2, value);

    hv1.overlapsWith(hv2);
  }

  @Test
  public void testFilterAsPredicate() {
    SMGObject object1 = new SMGRegion(64, "object1");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value2);

    Predicate<SMGEdgeHasValue> predicate =
        SMGEdgeHasValueFilter.objectFilter(object1).filterHavingValue(value1)::holdsFor;

    assertThat(predicate.apply(hv11at0)).isTrue();
    assertThat(predicate.apply(hv12at0)).isFalse();
  }

  @Test
  public void testFilterOnObject() {
    SMGObject object1 = new SMGRegion(64, "object1");
    SMGObject object2 = new SMGRegion(64, "Object2");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(mockTypeSize, 0, object2, value1);
    SMGEdgeHasValue hv22at0 = new SMGEdgeHasValue(mockTypeSize, 0, object2, value2);
    Set<SMGEdgeHasValue> allEdges = new HashSet<>();
    allEdges.add(hv11at0);
    allEdges.add(hv12at0);
    allEdges.add(hv21at0);
    allEdges.add(hv22at0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    assertThat(filter.holdsFor(hv11at0)).isTrue();
    assertThat(filter.holdsFor(hv12at0)).isTrue();
    assertThat(filter.holdsFor(hv21at0)).isTrue();
    assertThat(filter.holdsFor(hv22at0)).isTrue();

    filter.filterByObject(object1);

    assertThat(filter.holdsFor(hv11at0)).isTrue();
    assertThat(filter.holdsFor(hv12at0)).isTrue();
    assertThat(filter.holdsFor(hv21at0)).isFalse();
    assertThat(filter.holdsFor(hv22at0)).isFalse();

    Set<SMGEdgeHasValue> filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    assertThat(filteredSet).hasSize(2);
    assertThat(filteredSet).contains(hv11at0);
    assertThat(filteredSet).contains(hv12at0);
  }

  @Test
  public void testFilterAtOffset() {
    SMGObject object1 = new SMGRegion(64, "object1");
    SMGObject object2 = new SMGRegion(64, "Object2");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockTypeSize, 32, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(mockTypeSize, 0, object2, value1);
    SMGEdgeHasValue hv22at0 = new SMGEdgeHasValue(mockTypeSize, 32, object2, value2);
    Set<SMGEdgeHasValue> allEdges = new HashSet<>();
    allEdges.add(hv11at0);
    allEdges.add(hv12at0);
    allEdges.add(hv21at0);
    allEdges.add(hv22at0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    filter.filterAtOffset(0);

    assertThat(filter.holdsFor(hv11at0)).isTrue();
    assertThat(filter.holdsFor(hv12at0)).isFalse();
    assertThat(filter.holdsFor(hv21at0)).isTrue();
    assertThat(filter.holdsFor(hv22at0)).isFalse();

    Set<SMGEdgeHasValue> filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    assertThat(filteredSet).hasSize(2);
    assertThat(filteredSet).contains(hv11at0);
    assertThat(filteredSet).contains(hv21at0);
  }

  @Test
  public void testFilterOnValue() {
    SMGObject object1 = new SMGRegion(64, "object1");
    SMGObject object2 = new SMGRegion(64, "Object2");

    SMGValue value1 = SMGKnownExpValue.valueOf(1);
    SMGValue value2 = SMGKnownExpValue.valueOf(2);

    SMGEdgeHasValue hv11at0 = new SMGEdgeHasValue(mockTypeSize, 0, object1, value1);
    SMGEdgeHasValue hv12at0 = new SMGEdgeHasValue(mockTypeSize, 32, object1, value2);
    SMGEdgeHasValue hv21at0 = new SMGEdgeHasValue(mockTypeSize, 0, object2, value1);
    SMGEdgeHasValue hv22at0 = new SMGEdgeHasValue(mockTypeSize, 32, object2, value2);
    Set<SMGEdgeHasValue> allEdges = new HashSet<>();
    allEdges.add(hv11at0);
    allEdges.add(hv12at0);
    allEdges.add(hv21at0);
    allEdges.add(hv22at0);

    SMGEdgeHasValueFilter filter = new SMGEdgeHasValueFilter();

    filter.filterHavingValue(value1);

    assertThat(filter.holdsFor(hv11at0)).isTrue();
    assertThat(filter.holdsFor(hv12at0)).isFalse();
    assertThat(filter.holdsFor(hv21at0)).isTrue();
    assertThat(filter.holdsFor(hv22at0)).isFalse();

    Set<SMGEdgeHasValue> filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    assertThat(filteredSet).hasSize(2);
    assertThat(filteredSet).contains(hv11at0);
    assertThat(filteredSet).contains(hv21at0);

    filter.filterNotHavingValue(value1);

    assertThat(filter.holdsFor(hv11at0)).isFalse();
    assertThat(filter.holdsFor(hv12at0)).isTrue();
    assertThat(filter.holdsFor(hv21at0)).isFalse();
    assertThat(filter.holdsFor(hv22at0)).isTrue();

    filteredSet = ImmutableSet.copyOf(filter.filter(allEdges));

    assertThat(filteredSet).hasSize(2);
    assertThat(filteredSet).contains(hv22at0);
    assertThat(filteredSet).contains(hv12at0);
  }
}
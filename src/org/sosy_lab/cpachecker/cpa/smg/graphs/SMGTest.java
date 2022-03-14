// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs;

import static com.google.common.truth.Truth.assertThat;

import java.util.TreeMap;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

public class SMGTest {
  private LogManager logger = LogManager.createTestLogManager();

  private SMG smg;
  private static final int mockTypeSize = 32;

  SMGObject obj1 = new SMGRegion(64, "object-1");
  SMGObject obj2 = new SMGRegion(64, "object-2");

  SMGValue val1 = SMGKnownExpValue.valueOf(1);
  SMGValue val2 = SMGKnownExpValue.valueOf(2);

  SMGEdgePointsTo pt1to1 = new SMGEdgePointsTo(val1, obj1, 0);
  SMGEdgeHasValue hv2has2at0 = new SMGEdgeHasValue(mockTypeSize, 0, obj2, val2);
  SMGEdgeHasValue hv2has1at4 = new SMGEdgeHasValue(mockTypeSize, 32, obj2, val1);

  // obj1 = xxxxxxxx
  // obj2 = yyyyzzzz
  // val1 -> obj1
  // yyyy has value 2
  // zzzz has value 1

  private static SMG getNewSMG64() {
    return new SMG(MachineModel.LINUX64);
  }

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    smg = getNewSMG64();

    smg.addObject(obj1);
    smg.addObject(obj2);

    smg.addValue(val1);
    smg.addValue(val2);

    smg.addPointsToEdge(pt1to1);

    smg.addHasValueEdge(hv2has2at0);
    smg.addHasValueEdge(hv2has1at4);
  }

  @Test
  public void getNullBytesForObjectTest() {
    SMG smg1 = getNewSMG64();
    smg1.addObject(obj1);
    SMGEdgeHasValue hv = new SMGEdgeHasValue(mockTypeSize, 32, obj1, SMGZeroValue.INSTANCE);
    smg1.addHasValueEdge(hv);

    TreeMap<Long, Long> nullEdges = smg1.getNullEdgesMapOffsetToSizeForObject(obj1);
    assertThat(nullEdges).containsExactly(32L, 32L);
  }

  @Test
  public void SMGConstructorTest() {
    SMG smg1 = getNewSMG64();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();
    SMGObject nullObject = SMGNullObject.INSTANCE;
    SMGValue nullAddress = SMGZeroValue.INSTANCE;

    assertThat(nullObject).isNotNull();
    assertThat(SMGNullObject.INSTANCE).isSameInstanceAs(nullObject);
    assertThat(smg1.getObjects()).hasSize(1);
    assertThat(smg1.getObjects()).contains(nullObject);

    assertThat(smg1.getValues()).hasSize(1);
    assertThat(smg1.getValues()).contains(nullAddress);

    assertThat(smg1.getPTEdges().size()).isEqualTo(1);
    SMGObject target_object = smg1.getObjectPointedBy(nullAddress);
    assertThat(target_object).isEqualTo(nullObject);

    assertThat(smg1.getHVEdges()).hasSize(0);

    // copy constructor
    SMG smg_copy = new SMG(smg1);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg_copy)).isTrue();

    SMGObject third_object = new SMGRegion(128, "object-3");
    SMGValue third_value = SMGKnownExpValue.valueOf(3);
    smg_copy.addObject(third_object);
    smg_copy.addValue(third_value);
    smg_copy.addHasValueEdge(new SMGEdgeHasValue(mockTypeSize, 0, third_object, third_value));
    smg_copy.addPointsToEdge(new SMGEdgePointsTo(third_value, third_object, 0));

    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg_copy)).isTrue();
    assertThat(smg1.getObjects()).hasSize(1);
    assertThat(smg_copy.getObjects()).hasSize(2);
    assertThat(smg_copy.getObjects()).contains(third_object);

    assertThat(smg1.getValues()).hasSize(1);
    assertThat(smg_copy.getValues()).hasSize(2);
    assertThat(smg_copy.getValues()).contains(third_value);

    assertThat(smg1.getPTEdges().size()).isEqualTo(1);
    assertThat(smg_copy.getPTEdges().size()).isEqualTo(2);
    SMGObject target_object_for_third = smg_copy.getObjectPointedBy(third_value);
    assertThat(target_object_for_third).isEqualTo(third_object);

    assertThat(smg1.getHVEdges()).hasSize(0);
    assertThat(smg_copy.getHVEdges()).hasSize(1);
  }

  @Test
  public void addRemoveHasValueEdgeTest() {
    SMG smg1 = getNewSMG64();
    SMGObject object = new SMGRegion(32, "object");

    SMGEdgeHasValue hv = new SMGEdgeHasValue(mockTypeSize, 0, object, SMGZeroValue.INSTANCE);

    smg1.addHasValueEdge(hv);
    assertThat(smg1.getHVEdges()).contains(hv);

    smg1.removeHasValueEdge(hv);
    assertThat(smg1.getHVEdges()).doesNotContain(hv);
  }

  @Test
  public void removeObjectTest() {
    SMG smg1 = getNewSMG64();
    SMGValue newValue = SMGKnownSymValue.of();

    SMGObject object = new SMGRegion(64, "object");
    SMGEdgeHasValue hv0 = new SMGEdgeHasValue(mockTypeSize, 0, object, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue hv4 = new SMGEdgeHasValue(mockTypeSize, 32, object, SMGZeroValue.INSTANCE);
    SMGEdgePointsTo pt = new SMGEdgePointsTo(newValue, object, 0);

    smg1.addValue(newValue);
    smg1.addObject(object);
    smg1.addPointsToEdge(pt);
    smg1.addHasValueEdge(hv0);
    smg1.addHasValueEdge(hv4);

    assertThat(smg1.getObjects()).contains(object);
    smg1.removeObject(object);
    assertThat(smg1.getObjects()).doesNotContain(object);
    assertThat(smg1.getHVEdges().contains(hv0)).isTrue();
    assertThat(smg1.getHVEdges().contains(hv4)).isTrue();
    assertThat(smg1.getPTEdges()).contains(pt);
  }

  @Test
  public void removeObjectAndEdgesTest() {
    SMG smg1 = getNewSMG64();
    SMGValue newValue = SMGKnownSymValue.of();

    SMGObject object = new SMGRegion(64, "object");
    SMGEdgeHasValue hv0 = new SMGEdgeHasValue(mockTypeSize, 0, object, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue hv4 = new SMGEdgeHasValue(mockTypeSize, 32, object, SMGZeroValue.INSTANCE);
    SMGEdgePointsTo pt = new SMGEdgePointsTo(newValue, object, 0);

    smg1.addValue(newValue);
    smg1.addObject(object);
    smg1.addPointsToEdge(pt);
    smg1.addHasValueEdge(hv0);
    smg1.addHasValueEdge(hv4);

    assertThat(smg1.getObjects()).contains(object);
    smg1.markObjectDeletedAndRemoveEdges(object);
    assertThat(smg1.isObjectValid(object)).isFalse();
    assertThat(smg1.getHVEdges()).doesNotContain(hv0);
    assertThat(smg1.getHVEdges()).doesNotContain(hv4);
    assertThat(smg1.getPTEdges()).contains(pt);
  }

  @Test
  public void validityTest() {
    assertThat(smg.isObjectValid(SMGNullObject.INSTANCE)).isFalse();
    assertThat(smg.isObjectValid(obj1)).isTrue();
    assertThat(smg.isObjectValid(obj2)).isTrue();

    UnmodifiableSMG smg_copy = smg.copyOf();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg_copy)).isTrue();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg)).isTrue();

    smg.setValidity(obj1, false);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg_copy)).isTrue();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg)).isTrue();
    assertThat(smg.isObjectValid(SMGNullObject.INSTANCE)).isFalse();
    assertThat(smg.isObjectValid(obj1)).isFalse();
    assertThat(smg.isObjectValid(obj2)).isTrue();
    assertThat(smg_copy.isObjectValid(SMGNullObject.INSTANCE)).isFalse();
    assertThat(smg_copy.isObjectValid(obj1)).isTrue();
    assertThat(smg_copy.isObjectValid(obj2)).isTrue();

    smg.setValidity(obj2, false);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg_copy)).isTrue();
    assertThat(smg_copy.isObjectValid(SMGNullObject.INSTANCE)).isFalse();
    assertThat(smg_copy.isObjectValid(obj1)).isTrue();
    assertThat(smg_copy.isObjectValid(obj2)).isTrue();
  }

  @Test
  public void consistencyViolationValidNullTest() {
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg)).isTrue();
    smg.setValidity(SMGNullObject.INSTANCE, true);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg)).isFalse();
  }

  @Test
  public void consistencyViolationInvalidRegionHasValueTest() {
    smg.setValidity(obj1, false);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg)).isTrue();
    smg.setValidity(obj2, false);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg)).isFalse();
  }

  @Test
  public void consistencyViolationFieldConsistency() {
    SMG smg1 = getNewSMG64();
    SMG smg2 = getNewSMG64();

    SMGObject object_2b = new SMGRegion(16, "object_2b");
    SMGObject object_4b = new SMGRegion(32, "object_4b");
    SMGValue random_value = SMGKnownExpValue.valueOf(6);

    smg1.addObject(object_2b);
    smg2.addObject(object_4b);
    smg1.addValue(random_value);
    smg2.addValue(random_value);

    // Read 4 bytes (sizeof(mockType)) on offset 0 of 2b object -> out of bounds
    SMGEdgeHasValue invalidHV1 = new SMGEdgeHasValue(mockTypeSize, 0, object_2b, random_value);

    // Read 4 bytes (sizeof(mockType)) on offset 8 of 4b object -> out of bounds
    SMGEdgeHasValue invalidHV2 = new SMGEdgeHasValue(mockTypeSize, 64, object_4b, random_value);

    smg1.addHasValueEdge(invalidHV1);
    smg2.addHasValueEdge(invalidHV2);

    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isFalse();
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg2)).isFalse();
  }

  @Test
  public void consistencyViolationHVConsistency() {
    SMG smg1 = getNewSMG64();

    SMGObject object_8b = new SMGRegion(64, "object_8b");
    SMGObject object_16b = new SMGRegion(80, "object_10b");

    SMGValue first_value = SMGKnownExpValue.valueOf(6);
    SMGValue second_value = SMGKnownExpValue.valueOf(8);

    // 1, 3, 4 are consistent (different offsets or object)
    // 2 is inconsistent with 1 (same object and offset, different value)
    SMGEdgeHasValue hv_edge1 = new SMGEdgeHasValue(mockTypeSize, 0, object_8b, first_value);
    SMGEdgeHasValue hv_edge2 = new SMGEdgeHasValue(mockTypeSize, 0, object_8b, second_value);
    SMGEdgeHasValue hv_edge3 = new SMGEdgeHasValue(mockTypeSize, 32, object_8b, second_value);
    SMGEdgeHasValue hv_edge4 = new SMGEdgeHasValue(mockTypeSize, 0, object_16b, second_value);

    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addValue(first_value);
    smg1.addObject(object_8b);
    smg1.addHasValueEdge(hv_edge1);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addValue(second_value);
    smg1.addHasValueEdge(hv_edge3);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addHasValueEdge(hv_edge4);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isFalse();
    smg1.addObject(object_16b);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    boolean thrown = false;
    try {
      smg1.addHasValueEdge(hv_edge2);
    } catch (AssertionError pAssertionError) {
      thrown = true;
    }
    assertThat(thrown).isTrue();
  }

  @Test
  public void consistencyViolationPTConsistency() {
    SMG smg1 = getNewSMG64();

    SMGObject object_8b = new SMGRegion(64, "object_8b");
    SMGObject object_16b = new SMGRegion(80, "object_10b");

    SMGValue first_value = SMGKnownExpValue.valueOf(6);
    SMGValue second_value = SMGKnownExpValue.valueOf(8);
    SMGValue third_value = SMGKnownExpValue.valueOf(10);

    SMGEdgePointsTo edge1 = new SMGEdgePointsTo(first_value, object_8b, 0);
    SMGEdgePointsTo edge2 = new SMGEdgePointsTo(third_value, object_8b, 32);
    SMGEdgePointsTo edge3 = new SMGEdgePointsTo(second_value, object_16b, 0);
    SMGEdgePointsTo edge4 = new SMGEdgePointsTo(first_value, object_16b, 0);

    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addValue(first_value);
    smg1.addPointsToEdge(edge1);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isFalse();

    smg1.addObject(object_8b);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addValue(third_value);
    smg1.addPointsToEdge(edge2);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addValue(second_value);
    smg1.addObject(object_16b);
    smg1.addPointsToEdge(edge3);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isTrue();

    smg1.addPointsToEdge(edge4);
    assertThat(SMGConsistencyVerifier.verifySMG(logger, smg1)).isFalse();
  }

  @Test // (expected=IllegalArgumentException.class)
  public void isObjectValidBadCallTest() {
    assertThat(smg.isObjectValid(new SMGRegion(192, "wee"))).isFalse();
  }

  @Test(expected = IllegalArgumentException.class)
  public void setValidityBadCallTest() {
    smg.setValidity(new SMGRegion(192, "wee"), true);
  }

  @Test
  public void getObjectsTest() {
    assertThat(smg.getObjects()).containsAtLeast(obj1, obj2, SMGNullObject.INSTANCE);
  }

  @Test
  public void getNullObjectTest() {
    SMGObject nullObject = SMGNullObject.INSTANCE;
    assertThat(smg.isObjectValid(nullObject)).isFalse();
    assertThat(nullObject.getSize()).isEqualTo(0);
  }

  @Test
  public void getValuesTest() {
    assertThat(smg.getValues()).containsAtLeast(val1, val2, SMGZeroValue.INSTANCE);
  }

  @Test
  public void getHVEdgesTest() {
    assertThat(smg.getHVEdges()).containsAtLeast(hv2has2at0, hv2has1at4);
  }

  @Test
  public void getPTEdgesTest() {
    assertThat(smg.getPTEdges()).contains(pt1to1);
  }

  @Test
  public void getObjectPointedByTest() {
    assertThat(smg.getObjectPointedBy(val1)).isEqualTo(obj1);
    assertThat(smg.getObjectPointedBy(val2)).isNull();
  }

  @Test
  public void neqBasicTest() {
    NeqRelation nr = new NeqRelation();
    SMGValue one = SMGKnownExpValue.valueOf(1);
    SMGValue two = SMGKnownExpValue.valueOf(2);
    SMGValue three = SMGKnownExpValue.valueOf(3);

    assertThat(nr.neq_exists(one, two)).isFalse();
    assertThat(nr.neq_exists(one, three)).isFalse();
    assertThat(nr.neq_exists(two, three)).isFalse();
    assertThat(nr.neq_exists(two, one)).isFalse();
    assertThat(nr.neq_exists(three, one)).isFalse();
    assertThat(nr.neq_exists(three, two)).isFalse();

    nr = nr.addRelationAndCopy(one, three);

    assertThat(nr.neq_exists(one, two)).isFalse();
    assertThat(nr.neq_exists(one, three)).isTrue();
    assertThat(nr.neq_exists(two, three)).isFalse();
    assertThat(nr.neq_exists(two, one)).isFalse();
    assertThat(nr.neq_exists(three, one)).isTrue();
    assertThat(nr.neq_exists(three, two)).isFalse();

    nr = nr.addRelationAndCopy(one, three);

    assertThat(nr.neq_exists(one, two)).isFalse();
    assertThat(nr.neq_exists(one, three)).isTrue();
    assertThat(nr.neq_exists(two, three)).isFalse();
    assertThat(nr.neq_exists(two, one)).isFalse();
    assertThat(nr.neq_exists(three, one)).isTrue();
    assertThat(nr.neq_exists(three, two)).isFalse();

    nr = nr.addRelationAndCopy(two, three);

    assertThat(nr.neq_exists(one, two)).isFalse();
    assertThat(nr.neq_exists(one, three)).isTrue();
    assertThat(nr.neq_exists(two, three)).isTrue();
    assertThat(nr.neq_exists(two, one)).isFalse();
    assertThat(nr.neq_exists(three, one)).isTrue();
    assertThat(nr.neq_exists(three, two)).isTrue();

    nr = nr.removeRelationAndCopy(one, three);

    assertThat(nr.neq_exists(one, two)).isFalse();
    assertThat(nr.neq_exists(one, three)).isFalse();
    assertThat(nr.neq_exists(two, three)).isTrue();
    assertThat(nr.neq_exists(two, one)).isFalse();
    assertThat(nr.neq_exists(three, one)).isFalse();
    assertThat(nr.neq_exists(three, two)).isTrue();
  }

  @Test
  public void neqRemoveValueTest() {
    NeqRelation nr = new NeqRelation();
    SMGValue one = SMGKnownExpValue.valueOf(1);
    SMGValue two = SMGKnownExpValue.valueOf(2);
    SMGValue three = SMGKnownExpValue.valueOf(3);

    nr = nr.addRelationAndCopy(one, two);
    nr = nr.addRelationAndCopy(one, three);
    nr = nr.removeValueAndCopy(one);
    assertThat(nr.neq_exists(one, two)).isFalse();
    assertThat(nr.neq_exists(one, three)).isFalse();
    assertThat(nr.neq_exists(two, three)).isFalse();
  }

  @Test
  public void neqMergeValuesTest() {
    NeqRelation nr = new NeqRelation();
    SMGValue one = SMGKnownExpValue.valueOf(1);
    SMGValue two = SMGKnownExpValue.valueOf(2);
    SMGValue three = SMGKnownExpValue.valueOf(3);

    nr = nr.addRelationAndCopy(one, three);
    nr = nr.replaceValueAndCopy(two, three);

    assertThat(nr.neq_exists(one, two)).isTrue();
    assertThat(nr.neq_exists(one, three)).isFalse();
    assertThat(nr.neq_exists(two, three)).isFalse();
  }

  @Test
  public void neqMergeValuesTest2() {
    NeqRelation nr = new NeqRelation();
    SMGValue zero = SMGZeroValue.INSTANCE;
    SMGValue one = SMGKnownExpValue.valueOf(1);
    SMGValue two = SMGKnownExpValue.valueOf(2);
    SMGValue three = SMGKnownExpValue.valueOf(3);

    nr = nr.addRelationAndCopy(zero, three);
    nr = nr.addRelationAndCopy(one, three);
    nr = nr.replaceValueAndCopy(two, three);

    assertThat(nr.neq_exists(zero, two)).isTrue();
    assertThat(nr.neq_exists(one, two)).isTrue();
    assertThat(nr.neq_exists(zero, three)).isFalse();
    assertThat(nr.neq_exists(one, three)).isFalse();
    assertThat(nr.neq_exists(two, three)).isFalse();
  }
}

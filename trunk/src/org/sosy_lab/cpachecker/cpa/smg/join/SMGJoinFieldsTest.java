// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGHasValueEdges;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter.SMGEdgeHasValueFilterByObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.util.Pair;

public class SMGJoinFieldsTest {

  private static final int mockType4bSize = 32;
  private static final int mockType8bSize = 64;

  private SMG smg1;
  private SMG smg2;

  private final SMGValue value1 = SMGKnownSymValue.of();
  private final SMGValue value2 = SMGKnownSymValue.of();

  @Before
  public void setUp() {
    smg1 = new SMG(MachineModel.LINUX64);
    smg2 = new SMG(MachineModel.LINUX64);
  }

  @Test
  public void joinNulliefiedAndUndefinedFieldsTest() {
    SMGRegion obj1 = new SMGRegion(128, "obj1");
    SMGRegion obj2 = new SMGRegion(128, "obj2");

    SMGRegion oth1 = new SMGRegion(128, "oth1");
    SMGRegion oth2 = new SMGRegion(128, "oth2");
    SMGValue value100 = SMGKnownExpValue.valueOf(100);

    SMGEdgeHasValue obj1hv1at0 = new SMGEdgeHasValue(40, 0, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue obj1hv2at7 = new SMGEdgeHasValue(16, 56, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue obj1hv3at9 = new SMGEdgeHasValue(56, 72, obj1, value1);

    SMGEdgeHasValue obj2hv1at0 = new SMGEdgeHasValue(32, 0, obj2, value100);
    SMGEdgePointsTo obj2pt1to0 = new SMGEdgePointsTo(value100, oth2, 0);
    SMGEdgeHasValue obj2hv2at4 = new SMGEdgeHasValue(24, 32, obj2, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue obj2hv3at8 = new SMGEdgeHasValue(64, 64, obj2, value2);

    SMGEdgeHasValue oth1hv1at0 = new SMGEdgeHasValue(40, 0, oth1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue oth1hv2at7 = new SMGEdgeHasValue(16, 56, oth1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue oth1hv3at9 = new SMGEdgeHasValue(56, 72, oth1, value1);

    SMGEdgeHasValue oth2hv1at0 = new SMGEdgeHasValue(32, 0, oth2, value100);
    SMGEdgeHasValue oth2hv2at4 = new SMGEdgeHasValue(24, 32, oth2, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue oth2hv3at8 = new SMGEdgeHasValue(64, 64, oth2, value2);

    smg1.addObject(obj1);
    smg1.addObject(oth1);
    smg2.addObject(obj2);
    smg2.addObject(oth2);

    smg1.addValue(value1);
    smg2.addValue(value2);
    smg2.addValue(value100);

    smg1.addHasValueEdge(obj1hv1at0);
    smg1.addHasValueEdge(obj1hv2at7);
    smg1.addHasValueEdge(obj1hv3at9);
    smg1.addHasValueEdge(oth1hv1at0);
    smg1.addHasValueEdge(oth1hv2at7);
    smg1.addHasValueEdge(oth1hv3at9);

    smg2.addHasValueEdge(obj2hv1at0);
    smg2.addPointsToEdge(obj2pt1to0);
    smg2.addHasValueEdge(obj2hv2at4);
    smg2.addHasValueEdge(obj2hv3at8);
    smg2.addHasValueEdge(oth2hv1at0);
    smg2.addHasValueEdge(oth2hv2at4);
    smg2.addHasValueEdge(oth2hv3at8);

    SMGJoinFields join = new SMGJoinFields(smg1, smg2, obj1, obj2);

    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.INCOMPARABLE);

    Map<Long, Pair<SMGValue, Integer>> fieldMap1 = new HashMap<>();
    Map<Long, Pair<SMGValue, Integer>> fieldMap2 = new HashMap<>();

    fieldMap1.put(0L, Pair.of(SMGZeroValue.INSTANCE, 40));
    fieldMap1.put(72L, Pair.of(value1, 56));

    fieldMap2.put(0L, Pair.of(value100, 32));
    fieldMap2.put(32L, Pair.of(SMGZeroValue.INSTANCE, 8));
    fieldMap2.put(64L, Pair.of(value2, 64));

    checkFields(join.getSMG1(), fieldMap1, obj1);
    checkFields(join.getSMG2(), fieldMap2, obj2);
  }

  private void checkFields(
      UnmodifiableSMG pSmg, Map<Long, Pair<SMGValue, Integer>> pFieldMap, SMGObject pObj) {

    SMGEdgeHasValueFilterByObject filterOnSMG = SMGEdgeHasValueFilter.objectFilter(pObj);
    SMGHasValueEdges edges = pSmg.getHVEdges(filterOnSMG);

    assertThat(edges).hasSize(pFieldMap.size());

    for (SMGEdgeHasValue edge : edges) {

      long offset = edge.getOffset();

      assertThat(pFieldMap).containsKey(offset);

      SMGValue value = edge.getValue();
      long length = edge.getSizeInBits();
      Pair<SMGValue, Integer> expectedValueAndLength = pFieldMap.get(offset);

      SMGValue eValue = expectedValueAndLength.getFirst();
      int eLength = expectedValueAndLength.getSecond();

      if (!eValue.equals(SMGKnownExpValue.valueOf(-1))) {
        assertThat(value).isEqualTo(eValue);
      }
      assertThat(length).isEqualTo(eLength);
    }
  }

  @Test
  public void getHVSetOfMissingNullValuesTest() {
    SMGRegion obj1 = new SMGRegion(64, "1");
    SMGRegion obj2 = new SMGRegion(64, "2");

    smg1.addObject(obj1);
    smg2.addObject(obj2);
    smg2.addValue(value2);

    SMGEdgeHasValue nullifyObj1 = new SMGEdgeHasValue(64, 0, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue nonPointer = new SMGEdgeHasValue(mockType4bSize, 16, obj2, value2);

    smg1.addHasValueEdge(nullifyObj1);
    smg2.addHasValueEdge(nonPointer);

    Set<SMGEdgeHasValue> hvSet1 = SMGJoinFields.getHVSetOfMissingNullValues(smg1, smg2, obj1, obj2);
    assertThat(hvSet1).hasSize(1);

    // adding a "back" edge should not change anything
    smg2.addPointsToEdge(new SMGEdgePointsTo(value2, obj2, 0));

    Set<SMGEdgeHasValue> hvSet2 = SMGJoinFields.getHVSetOfMissingNullValues(smg1, smg2, obj1, obj2);
    assertThat(hvSet2).hasSize(1);

    for (SMGEdgeHasValue hv : Sets.union(hvSet1, hvSet2)) { // just two edges in there
      assertThat(hv.getValue()).isEqualTo(SMGZeroValue.INSTANCE);
      assertThat(hv.getObject()).isSameInstanceAs(obj1);
      assertThat(hv.getSizeInBits()).isEqualTo(32);
      assertThat(hv.getOffset()).isEqualTo(16);
    }
  }

  @Test
  public void getHVSetOfCommonNullValuesTest() {
    SMGRegion obj1 = new SMGRegion(176, "1");

    SMGEdgeHasValue smg1at4 = new SMGEdgeHasValue(mockType4bSize, 32, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue smg2at8 = new SMGEdgeHasValue(mockType4bSize, 64, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue smg1at14 =
        new SMGEdgeHasValue(mockType4bSize, 112, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue smg2at12 = new SMGEdgeHasValue(mockType4bSize, 96, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue smg1at18 =
        new SMGEdgeHasValue(mockType4bSize, 144, obj1, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue smg2at18 =
        new SMGEdgeHasValue(mockType4bSize, 144, obj1, SMGZeroValue.INSTANCE);

    smg1.addHasValueEdge(smg1at18);
    smg1.addHasValueEdge(smg1at14);
    smg1.addHasValueEdge(smg1at4);
    smg2.addHasValueEdge(smg2at18);
    smg2.addHasValueEdge(smg2at12);
    smg2.addHasValueEdge(smg2at8);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.getHVSetOfCommonNullValues(smg1, smg2, obj1, obj1);
    assertThat(hvSet).hasSize(2);
    for (SMGEdgeHasValue hv : hvSet) {
      assertThat(hv.getValue()).isEqualTo(SMGZeroValue.INSTANCE);
      assertThat(hv.getObject()).isSameInstanceAs(obj1);
      assertThat(hv.getOffset()).isAnyOf(112L, 144L);
      if (hv.getOffset() == 112) {
        assertThat(hv.getSizeInBits()).isEqualTo(16);
      } else {
        assertThat(hv.getSizeInBits()).isEqualTo(32);
      }
    }
  }

  @Test
  public void getCompatibleHVEdgeSetTest() {
    SMGRegion obj = new SMGRegion(256, "Object");
    SMGRegion differentObject = new SMGRegion(128, "Different object");

    smg1.addObject(obj);
    smg2.addObject(obj);
    smg1.addObject(differentObject);

    smg2.addValue(value1);

    SMGEdgeHasValue hv0for4at0in1 =
        new SMGEdgeHasValue(mockType4bSize, 0, obj, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue hv0for4at0in2 =
        new SMGEdgeHasValue(mockType4bSize, 0, obj, SMGZeroValue.INSTANCE);

    SMGEdgeHasValue hv0for4at5in1 =
        new SMGEdgeHasValue(mockType4bSize, 40, obj, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue hv0for4at7in2 =
        new SMGEdgeHasValue(mockType4bSize, 56, obj, SMGZeroValue.INSTANCE);

    SMGEdgeHasValue hv0for4at12in1 =
        new SMGEdgeHasValue(mockType4bSize, 96, obj, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue hv0for4at16in2 =
        new SMGEdgeHasValue(mockType4bSize, 128, obj, SMGZeroValue.INSTANCE);

    SMGEdgeHasValue hv0for4at20in1 =
        new SMGEdgeHasValue(mockType4bSize, 160, obj, SMGZeroValue.INSTANCE);
    SMGEdgeHasValue hv666for4at20in2 = new SMGEdgeHasValue(mockType4bSize, 160, obj, value1);

    SMGEdgeHasValue hv666for4at28in2 = new SMGEdgeHasValue(mockType4bSize, 224, obj, value1);

    SMGEdgeHasValue diffObjectNullValue =
        new SMGEdgeHasValue(mockType4bSize, 0, differentObject, SMGZeroValue.INSTANCE);

    smg1.addHasValueEdge(hv0for4at0in1);
    smg1.addHasValueEdge(hv0for4at5in1);
    smg1.addHasValueEdge(hv0for4at12in1);
    smg1.addHasValueEdge(hv0for4at20in1);
    smg1.addHasValueEdge(diffObjectNullValue);

    smg2.addHasValueEdge(hv0for4at0in2);
    smg2.addHasValueEdge(hv0for4at7in2);
    smg2.addHasValueEdge(hv0for4at16in2);
    smg2.addHasValueEdge(hv666for4at20in2);
    smg2.addPointsToEdge(new SMGEdgePointsTo(value1, obj, 160));
    smg2.addHasValueEdge(hv666for4at28in2);

    SMGJoinFields.setCompatibleHVEdgesToSMG(smg1, smg2, obj, obj);
    assertThat(smg1.getHVEdges()).hasSize(4);

    SMGJoinFields.setCompatibleHVEdgesToSMG(smg2, smg1, obj, obj);
    assertThat(smg1.getHVEdges()).hasSize(4);
  }

  @Test
  public void mergeNonNullHVEdgesTest() {
    Set<SMGValue> values = new HashSet<>();
    values.add(value1);
    values.add(value2);

    SMGRegion object = new SMGRegion(128, "Object");
    SMGEdgeHasValue smg1_4bFrom0ToV1 = new SMGEdgeHasValue(mockType4bSize, 0, object, value1);
    SMGEdgeHasValue smg1_4bFrom4ToV2 = new SMGEdgeHasValue(mockType4bSize, 32, object, value2);
    SMGEdgeHasValue smg1_4bFrom8ToNull =
        new SMGEdgeHasValue(mockType4bSize, 64, object, SMGZeroValue.INSTANCE);

    smg1.addObject(object);
    smg1.addValue(value1);
    smg1.addValue(value2);
    smg1.addHasValueEdge(smg1_4bFrom8ToNull);
    smg1.addHasValueEdge(smg1_4bFrom4ToV2);
    smg1.addHasValueEdge(smg1_4bFrom0ToV1);

    smg2.addObject(object);

    SMGHasValueEdges hvSet = SMGJoinFields.mergeNonNullHasValueEdges(smg1, smg2, object, object);
    assertThat(hvSet).hasSize(2);

    boolean seenZero = false;
    boolean seenTwo = false;

    SMGEdgeHasValueFilterByObject filter = SMGEdgeHasValueFilter.objectFilter(object);
    for (SMGEdgeHasValue edge : filter.filter(hvSet)) {
      if (edge.getOffset() == 0) {
        seenZero = true;
      } else if (edge.getOffset() == 32) {
        seenTwo = true;
      }
      assertThat(edge.getOffset()).isAnyOf(0L, 32L);
      assertThat(edge.getSizeInBits()).isEqualTo(mockType4bSize);
      assertThat(values).doesNotContain(edge.getValue());
      values.add(edge.getValue());
    }
    assertThat(seenZero).isTrue();
    assertThat(seenTwo).isTrue();

    smg2.addValue(value1);
    smg2.addHasValueEdge(smg1_4bFrom0ToV1);
    hvSet = SMGJoinFields.mergeNonNullHasValueEdges(smg1, smg2, object, object);
    assertThat(hvSet).hasSize(1);
  }

  @Test
  public void mergeNonNullAplliedTest() {
    SMGRegion obj1 = new SMGRegion(64, "Object 1");
    SMGRegion obj2 = new SMGRegion(64, "Object 2");
    smg1.addObject(obj1);
    smg2.addObject(obj2);

    SMGValue value3 = SMGKnownSymValue.of();
    smg1.addValue(value3);
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, value3));

    SMGJoinFields jf = new SMGJoinFields(smg1.copyOf(), smg2.copyOf(), obj1, obj2);
    UnmodifiableSMG resultSMG = jf.getSMG2();

    SMGHasValueEdges edges = resultSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2));
    assertThat(edges.isEmpty()).isFalse();

    jf = new SMGJoinFields(smg2.copyOf(), smg1.copyOf(), obj2, obj1);
    resultSMG = jf.getSMG1();

    edges = resultSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2));
    assertThat(edges.isEmpty()).isFalse();
  }

  @Test
  public void joinFieldsRelaxStatusTest() {
    SMGRegion object = new SMGRegion(64, "Object");
    smg1.addObject(object);

    SMG smg0_0B_4B = smg1.copyOf();
    SMG smg0_2B_6B = smg1.copyOf();
    SMG smg0_4B_8B = smg1.copyOf();
    SMG smg0_0B_8B = smg1.copyOf();

    smg0_0B_4B.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 0, object, SMGZeroValue.INSTANCE));
    smg0_2B_6B.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 16, object, SMGZeroValue.INSTANCE));
    smg0_4B_8B.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 32, object, SMGZeroValue.INSTANCE));
    smg0_0B_8B.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 0, object, SMGZeroValue.INSTANCE));
    smg0_0B_8B.addHasValueEdge(
        new SMGEdgeHasValue(mockType4bSize, 32, object, SMGZeroValue.INSTANCE));

    // these test invalid inputs, so they are disabled now
    /*
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_4B, smg0_4B_8B, object); // invalid use
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_4B, smg0_2B_6B, object); // invalid use
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_4B, smg0_0B_8B, object); // invalid use, should throw

    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_4B_8B, smg0_0B_4B, object); // invalid use
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_4B_8B, smg0_2B_6B, object); // invalid use
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_4B_8B, smg0_0B_8B, object); // invalid use

    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_2B_6B, smg0_0B_4B, object); // invalid use
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_2B_6B, smg0_4B_8B, object); // invalid use
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_2B_6B, smg0_0B_8B, object); // invalid use
    */

    checkStatusAfterRelax(SMGJoinStatus.EQUAL, smg0_0B_8B, smg0_0B_8B, object); // OK
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_8B, smg0_0B_4B, object); // OK
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_8B, smg0_4B_8B, object); // OK
    checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_8B, smg0_2B_6B, object); // OK

    checkStatusAfterJoinFields(SMGJoinStatus.EQUAL, smg0_0B_8B, smg0_0B_8B, object);
    checkStatusAfterJoinFields(SMGJoinStatus.LEFT_ENTAIL, smg0_0B_8B, smg0_0B_4B, object);
    checkStatusAfterJoinFields(SMGJoinStatus.LEFT_ENTAIL, smg0_0B_8B, smg0_2B_6B, object);
    checkStatusAfterJoinFields(SMGJoinStatus.LEFT_ENTAIL, smg0_0B_8B, smg0_4B_8B, object);
    checkStatusAfterJoinFields(SMGJoinStatus.RIGHT_ENTAIL, smg0_0B_4B, smg0_0B_8B, object);
    checkStatusAfterJoinFields(SMGJoinStatus.RIGHT_ENTAIL, smg0_2B_6B, smg0_0B_8B, object);
    checkStatusAfterJoinFields(SMGJoinStatus.RIGHT_ENTAIL, smg0_4B_8B, smg0_0B_8B, object);
  }

  private void checkStatusAfterRelax(SMGJoinStatus expected, SMG a, SMG b, SMGRegion object) {
    SMGJoinFields js = new SMGJoinFields(a, a, object, object); // dummy instantiation
    js.joinFieldsRelaxStatus(a, b, SMGJoinStatus.INCOMPARABLE, object);
    assertThat(js.getStatus()).isEqualTo(expected);
  }

  private void checkStatusAfterJoinFields(SMGJoinStatus expected, SMG a, SMG b, SMGRegion object) {
    SMGJoinFields js = new SMGJoinFields(a, b, object, object); // join fields
    assertThat(js.getStatus()).isEqualTo(expected);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void differentSizeCheckTest() {
    SMGRegion obj1 = new SMGRegion(64, "Object 1");
    SMGRegion obj2 = new SMGRegion(96, "Object 2");
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX64);
    smg3.addObject(obj1);
    smg4.addObject(obj2);

    new SMGJoinFields(smg3, smg4, obj1, obj2);
  }

  @Test
  public void consistencyCheckTest() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    SMGValue value3 = SMGKnownSymValue.of();
    SMGValue value4 = SMGKnownSymValue.of();

    smg3.addObject(obj1);
    smg4.addObject(obj2);
    smg3.addValue(value3);
    smg4.addValue(value4);

    SMGEdgeHasValue hvAt0in1 = new SMGEdgeHasValue(mockType4bSize, 0, obj1, value3);
    SMGEdgeHasValue hvAt0in2 = new SMGEdgeHasValue(mockType4bSize, 0, obj2, value4);
    smg3.addHasValueEdge(hvAt0in1);
    smg4.addHasValueEdge(hvAt0in2);
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 32, obj1, SMGZeroValue.INSTANCE));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 32, obj2, SMGZeroValue.INSTANCE));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType8bSize, 64, obj1, SMGZeroValue.INSTANCE));

    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8bSize, 64, obj2, SMGZeroValue.INSTANCE));

    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 128, obj1, value3));
    smg3.addPointsToEdge(new SMGEdgePointsTo(value3, obj1, 0));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 128, obj2, value4));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected = SMGInconsistentException.class)
  public void consistencyCheckNegativeTest1() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    UnmodifiableSMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    SMGValue value3 = SMGKnownSymValue.of();

    smg3.addValue(value3);
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, value3));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected = SMGInconsistentException.class)
  public void consistencyCheckNegativeTest2() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, SMGZeroValue.INSTANCE));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8bSize, 0, obj2, SMGZeroValue.INSTANCE));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected = SMGInconsistentException.class)
  public void consistencyCheckNegativeTest3() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, SMGZeroValue.INSTANCE));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 64, obj1, SMGZeroValue.INSTANCE));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8bSize, 0, obj2, SMGZeroValue.INSTANCE));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected = SMGInconsistentException.class)
  public void consistencyCheckNegativeTest4() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");
    SMGValue value = SMGKnownSymValue.of();
    smg3.addValue(value);
    smg4.addValue(value);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, value));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 32, obj1, SMGZeroValue.INSTANCE));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8bSize, 0, obj2, value));

    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 64, obj2, SMGZeroValue.INSTANCE));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 96, obj2, SMGZeroValue.INSTANCE));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType8bSize, 64, obj1, SMGZeroValue.INSTANCE));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected = SMGInconsistentException.class)
  public void consistencyCheckNegativeTest5() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");

    SMGValue value4 = SMGKnownSymValue.of();
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, SMGZeroValue.INSTANCE));
    smg4.addValue(value4);
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj2, value4));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test
  public void consistencyCheckPositiveTest1() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    SMGRegion obj3 = new SMGRegion(256, "Object 3");

    SMGValue value3 = SMGKnownSymValue.of();
    SMGValue value4 = SMGKnownSymValue.of();

    smg3.addValue(value3);
    smg4.addValue(value4);
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj1, value3));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj2, value4));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4bSize, 0, obj3, value4));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void nonMemberObjectTest1() {
    UnmodifiableSMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    smg4.addObject(obj2);

    new SMGJoinFields(smg3, smg4, obj1, obj2);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void nonMemberObjectTest2() {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    UnmodifiableSMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    smg3.addObject(obj1);

    new SMGJoinFields(smg3, smg4, obj1, obj2);
  }
}

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
package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.util.Pair;

public class SMGJoinFieldsTest {
  static private final CType mockType4b = AnonymousTypes.createTypeWithLength(32);
  static private final CType mockType8b = AnonymousTypes.createTypeWithLength(64);

  private SMG smg1;
  private SMG smg2;

  final private Integer value1 = SMGValueFactory.getNewValue();
  final private Integer value2 = SMGValueFactory.getNewValue();

  @Before
  public void setUp() {
    smg1 = new SMG(MachineModel.LINUX64);
    smg2 = new SMG(MachineModel.LINUX64);
  }

  @Test
  public void joinNulliefiedAndUndefinedFieldsTest() {
    SMGRegion obj1 = new SMGRegion(128, "1");
    SMGRegion obj2 = new SMGRegion(128, "1");

    SMGRegion oth1 = new SMGRegion(128, "1");
    SMGRegion oth2 = new SMGRegion(128, "1");

    SMGEdgeHasValue obj1hv1at0 = new SMGEdgeHasValue(40, 0, obj1, 0);
    SMGEdgeHasValue obj1hv2at7 = new SMGEdgeHasValue(16, 56, obj1, 0);
    SMGEdgeHasValue obj1hv3at9 = new SMGEdgeHasValue(56, 72, obj1, value1);

    SMGEdgeHasValue obj2hv1at0 = new SMGEdgeHasValue(32, 0, obj2, 100);
    SMGEdgePointsTo obj2pt1to0 = new SMGEdgePointsTo(100, oth2, 0);
    SMGEdgeHasValue obj2hv2at4 = new SMGEdgeHasValue(24, 32, obj2, 0);
    SMGEdgeHasValue obj2hv3at8 = new SMGEdgeHasValue(64, 64, obj2, value2);

    SMGEdgeHasValue oth1hv1at0 = new SMGEdgeHasValue(40, 0, oth1, 0);
    SMGEdgeHasValue oth1hv2at7 = new SMGEdgeHasValue(16, 56, oth1, 0);
    SMGEdgeHasValue oth1hv3at9 = new SMGEdgeHasValue(56, 72, oth1, value1);

    SMGEdgeHasValue oth2hv1at0 = new SMGEdgeHasValue(32, 0, oth2, 100);
    SMGEdgeHasValue oth2hv2at4 = new SMGEdgeHasValue(24, 32, oth2, 0);
    SMGEdgeHasValue oth2hv3at8 = new SMGEdgeHasValue(64, 64, oth2, value2);

    smg1.addObject(obj1);
    smg1.addObject(oth1);
    smg2.addObject(obj2);
    smg2.addObject(oth2);

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

    Map<Long, Pair<Integer, Integer>> fieldMap1 = new HashMap<>();
    Map<Long, Pair<Integer, Integer>> fieldMap2 = new HashMap<>();

    fieldMap1.put(0L, Pair.of(0, 32));
    fieldMap1.put(32L, Pair.of(0, 8));
    fieldMap1.put(64L, Pair.of(-1, 64));
    fieldMap1.put(72L, Pair.of(value1, 56));

    fieldMap2.put(0L, Pair.of(100, 32));
    fieldMap2.put(32L, Pair.of(0, 8));
    fieldMap2.put(64L, Pair.of(value2, 64));
    fieldMap2.put(72L, Pair.of(-1, 56));

    checkFields(smg1, fieldMap1, obj1);
    checkFields(smg2, fieldMap2, obj2);


  }

  private void checkFields(SMG pSmg, Map<Long, Pair<Integer, Integer>> pFieldMap, SMGObject pObj) {

    SMGEdgeHasValueFilter filterOnSMG = SMGEdgeHasValueFilter.objectFilter(pObj);
    Set<SMGEdgeHasValue> edges = pSmg.getHVEdges(filterOnSMG);

    assertThat(edges).hasSize(pFieldMap.keySet().size());

    for (SMGEdgeHasValue edge : edges) {

      long offset = edge.getOffset();

      assertThat(pFieldMap).containsKey(offset);

      int value = edge.getValue();
      int length = edge.getSizeInBits(MachineModel.LINUX64);
      Pair<Integer, Integer> expectedValueAndLength = pFieldMap.get(offset);

      int eValue = expectedValueAndLength.getFirst();
      int eLength = expectedValueAndLength.getSecond();

      if (eValue != -1) {
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

    SMGEdgeHasValue nullifyObj1 = new SMGEdgeHasValue(64, 0, obj1, SMG.NULL_ADDRESS);
    SMGEdgeHasValue nonPointer = new SMGEdgeHasValue(mockType4b, 16, obj2, value2);

    smg1.addHasValueEdge(nullifyObj1);
    smg2.addHasValueEdge(nonPointer);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.getHVSetOfMissingNullValues(smg1, smg2, obj1, obj2);
    assertThat(hvSet).hasSize(0);

    smg2.addPointsToEdge(new SMGEdgePointsTo(value2, obj2, 0));

    hvSet = SMGJoinFields.getHVSetOfMissingNullValues(smg1, smg2, obj1, obj2);
    assertThat(hvSet).hasSize(1);

    SMGEdgeHasValue newHv = Iterables.getOnlyElement(hvSet);
    assertThat(newHv.getValue()).isEqualTo(SMG.NULL_ADDRESS);
    assertThat(newHv.getObject()).isSameAs(obj1);
    assertThat(newHv.getSizeInBits(MachineModel.LINUX64)).isEqualTo(32);
    assertThat(newHv.getOffset()).isEqualTo(16);
    Assert.assertTrue(newHv.isCompatibleField(nonPointer));
  }

  @Test
  public void getHVSetOfCommonNullValuesTest() {
    SMGRegion obj1 = new SMGRegion(176, "1");

    SMGEdgeHasValue smg1at4 = new SMGEdgeHasValue(mockType4b, 32, obj1, SMG.NULL_ADDRESS);
    SMGEdgeHasValue smg2at8 = new SMGEdgeHasValue(mockType4b, 64, obj1, SMG.NULL_ADDRESS);
    SMGEdgeHasValue smg1at14 = new SMGEdgeHasValue(mockType4b, 112, obj1, SMG.NULL_ADDRESS);
    SMGEdgeHasValue smg2at12 = new SMGEdgeHasValue(mockType4b, 96, obj1, SMG.NULL_ADDRESS);
    SMGEdgeHasValue smg1at18 = new SMGEdgeHasValue(mockType4b, 144, obj1, SMG.NULL_ADDRESS);
    SMGEdgeHasValue smg2at18 = new SMGEdgeHasValue(mockType4b, 144, obj1, SMG.NULL_ADDRESS);

    smg1.addHasValueEdge(smg1at18);
    smg1.addHasValueEdge(smg1at14);
    smg1.addHasValueEdge(smg1at4);
    smg2.addHasValueEdge(smg2at18);
    smg2.addHasValueEdge(smg2at12);
    smg2.addHasValueEdge(smg2at8);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.getHVSetOfCommonNullValues(smg1, smg2, obj1, obj1);
    assertThat(hvSet).hasSize(2);
    for (SMGEdgeHasValue hv : hvSet) {
      assertThat(hv.getValue()).isEqualTo(SMG.NULL_ADDRESS);
      assertThat(hv.getObject()).isSameAs(obj1);
      assertThat(hv.getOffset()).isAnyOf(112L, 144L);
      if (hv.getOffset() == 112) {
        assertThat(hv.getSizeInBits(MachineModel.LINUX64)).isEqualTo(16);
      } else {
        assertThat(hv.getSizeInBits(MachineModel.LINUX64)).isEqualTo(32);
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

    SMGEdgeHasValue hv0for4at0in1 = new SMGEdgeHasValue(mockType4b, 0, obj, SMG.NULL_ADDRESS);
    SMGEdgeHasValue hv0for4at0in2 = new SMGEdgeHasValue(mockType4b, 0, obj, SMG.NULL_ADDRESS);

    SMGEdgeHasValue hv0for4at5in1 = new SMGEdgeHasValue(mockType4b, 40, obj, SMG.NULL_ADDRESS);
    SMGEdgeHasValue hv0for4at7in2 = new SMGEdgeHasValue(mockType4b, 56, obj, SMG.NULL_ADDRESS);

    SMGEdgeHasValue hv0for4at12in1 = new SMGEdgeHasValue(mockType4b, 96, obj, SMG.NULL_ADDRESS);
    SMGEdgeHasValue hv0for4at16in2 = new SMGEdgeHasValue(mockType4b, 128, obj, SMG.NULL_ADDRESS);

    SMGEdgeHasValue hv0for4at20in1 = new SMGEdgeHasValue(mockType4b, 160, obj, SMG.NULL_ADDRESS);
    SMGEdgeHasValue hv666for4at20in2 = new SMGEdgeHasValue(mockType4b, 160, obj, value1);

    SMGEdgeHasValue hv666for4at28in2 = new SMGEdgeHasValue(mockType4b, 224, obj, value1);

    SMGEdgeHasValue diffObjectNullValue = new SMGEdgeHasValue(mockType4b, 0, differentObject, SMG.NULL_ADDRESS);

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
    Set<Integer> values = new HashSet<>();
    values.add(value1);
    values.add(value2);

    SMGRegion object = new SMGRegion(128, "Object");
    SMGEdgeHasValue smg1_4bFrom0ToV1 = new SMGEdgeHasValue(mockType4b, 0, object, value1);
    SMGEdgeHasValue smg1_4bFrom2ToV2 = new SMGEdgeHasValue(mockType4b, 16, object, value2);
    SMGEdgeHasValue smg1_4bFrom4ToNull = new SMGEdgeHasValue(mockType4b, 32, object, SMG.NULL_ADDRESS);

    smg1.addObject(object);
    smg1.addValue(value1);
    smg1.addValue(value2);
    smg1.addHasValueEdge(smg1_4bFrom4ToNull);
    smg1.addHasValueEdge(smg1_4bFrom2ToV2);
    smg1.addHasValueEdge(smg1_4bFrom0ToV1);

    smg2.addObject(object);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.mergeNonNullHasValueEdges(smg1, smg2, object, object);
    assertThat(hvSet).hasSize(2);

    boolean seenZero = false;
    boolean seenTwo = false;

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(object);
    for (SMGEdgeHasValue edge : filter.filter(hvSet)) {
      if (edge.getOffset() == 0) {
        seenZero = true;
      } else if (edge.getOffset() == 16) {
        seenTwo = true;
      }
      assertThat(edge.getOffset()).isAnyOf(0L, 16L);
      assertThat(edge.getType()).isEqualTo(mockType4b);
      assertThat(values).doesNotContain(Integer.valueOf(edge.getValue()));
      values.add(Integer.valueOf(edge.getValue()));
    }
    Assert.assertTrue(seenZero);
    Assert.assertTrue(seenTwo);

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

    Integer value3 = SMGValueFactory.getNewValue();
    smg1.addValue(value3);
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, value3));

    SMGJoinFields jf = new SMGJoinFields(new SMG(smg1), new SMG(smg2), obj1, obj2);
    SMG resultSMG = jf.getSMG2();

    Set<SMGEdgeHasValue> edges = resultSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2));
    assertThat(edges.size()).isGreaterThan(0);

    jf = new SMGJoinFields(new SMG(smg2), new SMG(smg1), obj2, obj1);
    resultSMG = jf.getSMG1();

    edges = resultSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2));
    assertThat(edges.size()).isGreaterThan(0);
  }

  @Test
  public void joinFieldsRelaxStatusTest() {
    SMGRegion object = new SMGRegion(64, "Object");
    smg1.addObject(object);

    SMG smg04 = new SMG(smg1);
    SMG smg48 = new SMG(smg1);
    SMG smg26 = new SMG(smg1);
    SMG smg08 = new SMG(smg1);

    smg04.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, object, SMG.NULL_ADDRESS));
    smg48.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 32, object, SMG.NULL_ADDRESS));
    smg26.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 16, object, SMG.NULL_ADDRESS));
    smg08.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, object, SMG.NULL_ADDRESS));
    smg08.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 32, object, SMG.NULL_ADDRESS));

    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg04, smg48,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg04, smg26,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.EQUAL,
        SMGJoinFields.joinFieldsRelaxStatus(smg04, smg08,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));

    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg48, smg04,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg48, smg26,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.EQUAL,
        SMGJoinFields.joinFieldsRelaxStatus(smg48, smg08,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));

    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg26, smg04,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg26, smg48,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.EQUAL,
        SMGJoinFields.joinFieldsRelaxStatus(smg26, smg08,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));

    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg08, smg04,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg08, smg48,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
    Assert.assertEquals(SMGJoinStatus.INCOMPARABLE,
        SMGJoinFields.joinFieldsRelaxStatus(smg08, smg26,
            SMGJoinStatus.EQUAL, SMGJoinStatus.INCOMPARABLE, object));
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
    Integer value3 = SMGValueFactory.getNewValue();
    Integer value4 = SMGValueFactory.getNewValue();

    smg3.addObject(obj1);
    smg4.addObject(obj2);
    smg3.addValue(value3);
    smg4.addValue(value4);

    SMGEdgeHasValue hvAt0in1 = new SMGEdgeHasValue(mockType4b, 0, obj1, value3);
    SMGEdgeHasValue hvAt0in2 = new SMGEdgeHasValue(mockType4b, 0, obj2, value4);
    smg3.addHasValueEdge(hvAt0in1);
    smg4.addHasValueEdge(hvAt0in2);
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 32, obj1, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 32, obj2, SMG.NULL_ADDRESS));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 64, obj1, SMG.NULL_ADDRESS));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 96, obj1, SMG.NULL_ADDRESS));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 64, obj1, SMG.NULL_ADDRESS));

    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 64, obj2, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 96, obj2, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 64, obj2, SMG.NULL_ADDRESS));

    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 128, obj1, value3));
    smg3.addPointsToEdge(new SMGEdgePointsTo(value3, obj1, 0));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 128, obj2, SMG.NULL_ADDRESS));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest1() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    Integer value3 = SMGValueFactory.getNewValue();

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, value3));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest2() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 0, obj2, SMG.NULL_ADDRESS));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest3() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, SMG.NULL_ADDRESS));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 32, obj1, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 0, obj2, SMG.NULL_ADDRESS));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest4() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, SMG.NULL_ADDRESS));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 32, obj1, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 0, obj2, SMG.NULL_ADDRESS));

    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 64, obj2, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 96, obj2, SMG.NULL_ADDRESS));
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 64, obj1, SMG.NULL_ADDRESS));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest5() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");

    Integer value4 = SMGValueFactory.getNewValue();
    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, SMG.NULL_ADDRESS));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj2, value4));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @Test
  public void consistencyCheckPositiveTest1() throws SMGInconsistentException {
    SMG smg3 = new SMG(MachineModel.LINUX64);
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    SMGRegion obj3 = new SMGRegion(256, "Object 3");

    Integer value3 = SMGValueFactory.getNewValue();
    Integer value4 = SMGValueFactory.getNewValue();

    smg3.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, value3));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj2, value4));
    smg4.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj3, value4));
    SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void nonMemberObjectTest1() {
    SMG smg3 = new SMG(MachineModel.LINUX64);
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
    SMG smg4 = new SMG(MachineModel.LINUX32);

    SMGRegion obj1 = new SMGRegion(256, "Object 1");
    SMGRegion obj2 = new SMGRegion(256, "Object 2");
    smg3.addObject(obj1);

    new SMGJoinFields(smg3, smg4, obj1, obj2);
  }
}

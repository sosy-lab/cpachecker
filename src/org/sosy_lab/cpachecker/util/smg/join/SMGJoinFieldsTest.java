// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.cpa.smg.util.PersistentSet;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

// TODO enable this tests once the write value reinterpretation was implemented
public class SMGJoinFieldsTest extends SMGJoinTest0 {

  private SMG smg1;
  private SMG smg2;

  private final SMGValue value1 = createValue();
  private final SMGValue value2 = createValue();

  @Before
  public void setUp() {
    smg1 = new SMG(mockType16bSize);
    smg2 = new SMG(mockType16bSize);
  }

  @Test
  @Ignore
  public void joinNulliefiedAndUndefinedFieldsTest() {
    SMGObject obj1 = createRegion(mockType16bSize);
    SMGObject obj2 = createRegion(mockType16bSize);

    SMGObject oth1 = createRegion(mockType16bSize);
    SMGObject oth2 = createRegion(mockType16bSize);
    SMGValue otherValue = createValue();

    SMGHasValueEdge obj1hv1at0 = createHasValueEdgeToZero(BigInteger.valueOf(40));
    SMGHasValueEdge obj1hv2at7 = createHasValueEdgeToZero(mockType2bSize, 56);
    SMGHasValueEdge obj1hv3at9 = createHasValueEdge(56, 72, value1);

    SMGHasValueEdge obj2hv1at0 = createHasValueEdge(mockType4bSize, otherValue);
    SMGPointsToEdge obj2pt1to0 = createPTRegionEdge(0, obj2);
    SMGHasValueEdge obj2hv2at4 = createHasValueEdgeToZero(24, 32);
    SMGHasValueEdge obj2hv3at8 = createHasValueEdge(64, 64, value2);

    SMGHasValueEdge oth1hv1at0 = createHasValueEdgeToZero(40);
    SMGHasValueEdge oth1hv2at7 = createHasValueEdgeToZero(16, 56);
    SMGHasValueEdge oth1hv3at9 = createHasValueEdge(56, 72, value1);

    SMGHasValueEdge oth2hv1at0 = createHasValueEdge(32, 0, otherValue);
    SMGHasValueEdge oth2hv2at4 = createHasValueEdgeToZero(24, 32);
    SMGHasValueEdge oth2hv3at8 = createHasValueEdge(64, 64, value2);

    smg1 = smg1.copyAndAddObject(obj1);
    smg1 = smg1.copyAndAddObject(oth1);
    smg2 = smg2.copyAndAddObject(obj2);
    smg2 = smg2.copyAndAddObject(oth2);

    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value2);
    smg2 = smg2.copyAndAddValue(otherValue);

    smg1 = smg1.copyAndAddHVEdge(obj1hv1at0, obj1);
    smg1 = smg1.copyAndAddHVEdge(obj1hv2at7, obj1);
    smg1 = smg1.copyAndAddHVEdge(obj1hv3at9, obj1);
    smg1 = smg1.copyAndAddHVEdge(oth1hv1at0, oth1);
    smg1 = smg1.copyAndAddHVEdge(oth1hv2at7, oth1);
    smg1 = smg1.copyAndAddHVEdge(oth1hv3at9, oth1);

    smg2 = smg2.copyAndAddHVEdge(obj2hv1at0, obj2);
    smg2 = smg2.copyAndAddHVEdge(obj2hv2at4, obj2);
    smg2 = smg2.copyAndAddHVEdge(obj2hv3at8, obj2);
    smg2 = smg2.copyAndAddHVEdge(oth2hv1at0, oth2);
    smg2 = smg2.copyAndAddHVEdge(oth2hv2at4, oth2);
    smg2 = smg2.copyAndAddHVEdge(oth2hv3at8, oth2);

    smg2 = smg2.copyAndAddPTEdge(obj2pt1to0, otherValue);

    SMGJoinFields join = new SMGJoinFields(smg1, smg2);
    join.joinFields(obj1, obj2);

    assertThat(join.getStatus()).isEqualTo(SMGJoinStatus.INCOMPARABLE);

    Map<BigInteger, Pair<SMGValue, BigInteger>> fieldMap1 = new HashMap<>();
    Map<BigInteger, Pair<SMGValue, BigInteger>> fieldMap2 = new HashMap<>();

    fieldMap1.put(BigInteger.valueOf(0L), Pair.of(SMGValue.zeroValue(), BigInteger.valueOf(40)));
    fieldMap1.put(BigInteger.valueOf(72L), Pair.of(value1, BigInteger.valueOf(56)));

    fieldMap2.put(BigInteger.valueOf(0L), Pair.of(otherValue, BigInteger.valueOf(32)));
    fieldMap2.put(BigInteger.valueOf(32L), Pair.of(SMGValue.zeroValue(), BigInteger.valueOf(8)));
    fieldMap2.put(BigInteger.valueOf(64L), Pair.of(value2, BigInteger.valueOf(64)));

    checkFields(join.getSmg1(), fieldMap1, obj1);
    checkFields(join.getSmg2(), fieldMap2, obj2);
  }

  private void checkFields(
      SMG pSmg, Map<BigInteger, Pair<SMGValue, BigInteger>> pFieldMap, SMGObject pObj) {

    Set<SMGHasValueEdge> edges = pSmg.getEdges(pObj);

    assertThat(edges).hasSize(pFieldMap.size());

    for (SMGHasValueEdge edge : edges) {

      BigInteger offset = edge.getOffset();

      assertThat(pFieldMap).containsKey(offset);

      SMGValue value = edge.hasValue();
      BigInteger length = edge.getSizeInBits();
      Pair<SMGValue, BigInteger> expectedValueAndLength = pFieldMap.get(offset);

      SMGValue eValue = expectedValueAndLength.getFirst();
      BigInteger eLength = expectedValueAndLength.getSecond();

      // Why -1? if (!eValue.equals(SMGKnownExpValue.valueOf(-1))) {
      assertThat(value).isEqualTo(eValue);
      // }
      assertThat(length).isEqualTo(eLength);
    }
  }

  @Test
  @Ignore
  public void processHVEdgeSetTest() {
    SMGObject obj = createRegion(mockType32bSize);
    SMGObject differentObject = createRegion(mockType16bSize);

    smg1 = smg1.copyAndAddObject(obj);
    smg2 = smg2.copyAndAddObject(obj);
    smg1 = smg1.copyAndAddObject(differentObject);

    smg2 = smg2.copyAndAddValue(value1);

    SMGHasValueEdge hv0for4at0in1 = createHasValueEdgeToZero(mockType4bSize);
    SMGHasValueEdge hv0for4at0in2 = createHasValueEdgeToZero(mockType4bSize);

    SMGHasValueEdge hv0for4at5in1 = createHasValueEdgeToZero(mockType4bSize, 40);
    SMGHasValueEdge hv0for4at7in2 = createHasValueEdgeToZero(mockType4bSize, 56);

    SMGHasValueEdge hv0for4at12in1 = createHasValueEdgeToZero(mockType4bSize, 96);
    SMGHasValueEdge hv0for4at16in2 = createHasValueEdgeToZero(mockType4bSize, 128);

    SMGHasValueEdge hv0for4at20in1 = createHasValueEdgeToZero(mockType4bSize, 160);
    SMGHasValueEdge hv666for4at20in2 = createHasValueEdge(mockType4bSize, 160, value1);

    SMGHasValueEdge hv666for4at28in2 = createHasValueEdge(mockType4bSize, 224, value1);

    SMGHasValueEdge diffObjectNullValue = createHasValueEdgeToZero(mockType4bSize, 0);

    smg1 = smg1.copyAndAddHVEdge(hv0for4at0in1, obj);
    smg1 = smg1.copyAndAddHVEdge(hv0for4at5in1, obj);
    smg1 = smg1.copyAndAddHVEdge(hv0for4at12in1, obj);
    smg1 = smg1.copyAndAddHVEdge(hv0for4at20in1, obj);
    smg1 = smg1.copyAndAddHVEdge(diffObjectNullValue, differentObject);

    smg2 = smg2.copyAndAddHVEdge(hv0for4at0in2, obj);
    smg2 = smg2.copyAndAddHVEdge(hv0for4at7in2, obj);
    smg2 = smg2.copyAndAddHVEdge(hv0for4at16in2, obj);
    smg2 = smg2.copyAndAddHVEdge(hv666for4at20in2, obj);
    smg2 = smg2.copyAndAddPTEdge(createPTRegionEdge(160, obj), value1);
    smg2 = smg2.copyAndAddHVEdge(hv666for4at28in2, obj);
    SMGJoinFields jFields = new SMGJoinFields(smg1, smg2);
    PersistentSet<SMGHasValueEdge> edgesFluentIterable =
        jFields.processHasValueEdgeSet(obj, obj, smg1, smg2);
    assertThat(edgesFluentIterable).hasSize(4);

    edgesFluentIterable = jFields.processHasValueEdgeSet(obj, obj, smg2, smg1);
    assertThat(edgesFluentIterable).hasSize(4);
  }

  @Ignore
  @Test
  public void mergeNonNullHVEdgesTest() {
    Set<SMGValue> values = new HashSet<>();
    values.add(value1);
    values.add(value2);

    SMGObject object = createRegion(mockType16bSize);
    SMGHasValueEdge smg1_4bFrom0ToV1 = createHasValueEdge(mockType4bSize, value1);
    SMGHasValueEdge smg1_4bFrom4ToV2 = createHasValueEdge(mockType4bSize, 32, value2);
    SMGHasValueEdge smg1_4bFrom8ToNull = createHasValueEdgeToZero(mockType4bSize, 64);

    smg1 = smg1.copyAndAddObject(object);
    smg1 = smg1.copyAndAddValue(value1);
    smg1 = smg1.copyAndAddValue(value2);
    smg1 = smg1.copyAndAddHVEdge(smg1_4bFrom8ToNull, object);
    smg1 = smg1.copyAndAddHVEdge(smg1_4bFrom4ToV2, object);
    smg1 = smg1.copyAndAddHVEdge(smg1_4bFrom0ToV1, object);

    smg2 = smg2.copyAndAddObject(object);

    SMGJoinFields jFields = new SMGJoinFields(smg1, smg2);

    Set<SMGHasValueEdge> hvSet = jFields.mergeNonNullValues(smg1, smg2, object, object).toSet();
    assertThat(hvSet).hasSize(2);

    boolean seenZero = false;
    boolean seenTwo = false;

    for (SMGHasValueEdge edge : hvSet) {
      if (edge.getOffset().equals(BigInteger.ZERO)) {
        seenZero = true;
      } else if (edge.getOffset().equals(BigInteger.valueOf(32))) {
        seenTwo = true;
      }
      assertThat(edge.getOffset()).isAnyOf(BigInteger.ZERO, BigInteger.valueOf(32));
      assertThat(edge.getSizeInBits()).isEqualTo(mockType4bSize);
      assertThat(values).doesNotContain(edge.hasValue());
      values.add(edge.hasValue());
    }
    assertThat(seenZero).isTrue();
    assertThat(seenTwo).isTrue();

    smg2 = smg2.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddHVEdge(smg1_4bFrom0ToV1, object);

    hvSet = jFields.mergeNonNullValues(smg1, smg2, object, object).toSet();
    assertThat(hvSet).hasSize(1);
  }

  @Ignore
  @Test
  public void mergeNonNullAplliedTest() {
    SMGObject obj1 = createRegion(mockType8bSize);
    SMGObject obj2 = createRegion(mockType8bSize);
    smg1 = smg1.copyAndAddObject(obj1);
    smg2 = smg2.copyAndAddObject(obj2);

    SMGValue value3 = createValue();
    smg1 = smg1.copyAndAddValue(value3);
    smg1 = smg1.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, value3), obj1);

    SMGJoinFields jf = new SMGJoinFields(smg1, smg2);
    jf.joinFields(obj1, obj2);
    SMG resultSMG = jf.getSmg2();

    Set<SMGHasValueEdge> edges = resultSMG.getEdges(obj2);
    assertThat(edges.isEmpty()).isFalse();

    jf = new SMGJoinFields(smg2, smg1);
    jf.joinFields(obj2, obj1);
    resultSMG = jf.getSmg1();

    edges = resultSMG.getEdges(obj2);
    assertThat(edges.isEmpty()).isFalse();
  }

  @Ignore
  @Test
  public void joinFieldsRelaxStatusTest() {
    SMGObject object = createRegion(mockType8bSize);

    smg1 = smg1.copyAndAddObject(object);

    SMG smg0_0B_4B =
        smg1.copyAndAddHVEdge(
            new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, mockType4bSize), object);
    SMG smg0_2B_6B =
        smg1.copyAndAddHVEdge(
            new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.valueOf(16), mockType4bSize),
            object);
    SMG smg0_4B_8B =
        smg1.copyAndAddHVEdge(
            new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.valueOf(32), mockType4bSize),
            object);
    SMG smg0_0B_8B =
        smg1.copyAndAddHVEdge(
                new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.ZERO, mockType4bSize), object)
            .copyAndAddHVEdge(
                new SMGHasValueEdge(SMGValue.zeroValue(), BigInteger.valueOf(32), mockType4bSize),
                object);

    // these test invalid inputs, so they are disabled now
    /*
     * checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_4B, smg0_4B_8B, object); // invalid
     * use checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_4B, smg0_2B_6B, object); //
     * invalid use checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_0B_4B, smg0_0B_8B,
     * object); // invalid use, should throw
     *
     * checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_4B_8B, smg0_0B_4B, object); // invalid
     * use checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_4B_8B, smg0_2B_6B, object); //
     * invalid use checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_4B_8B, smg0_0B_8B,
     * object); // invalid use
     *
     * checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_2B_6B, smg0_0B_4B, object); // invalid
     * use checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_2B_6B, smg0_4B_8B, object); //
     * invalid use checkStatusAfterRelax(SMGJoinStatus.INCOMPARABLE, smg0_2B_6B, smg0_0B_8B,
     * object); // invalid use
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

  private void checkStatusAfterRelax(SMGJoinStatus expected, SMG a, SMG b, SMGObject object) {
    SMGJoinFields js = new SMGJoinFields(a, a); // dummy instantiation
    js.updateStatus(a, b, SMGJoinStatus.INCOMPARABLE, object);
    assertThat(js.getStatus()).isEqualTo(expected);
  }

  private void checkStatusAfterJoinFields(SMGJoinStatus expected, SMG a, SMG b, SMGObject object) {
    SMGJoinFields js = new SMGJoinFields(a, b); // join fields
    js.joinFields(object, object);
    assertThat(js.getStatus()).isEqualTo(expected);
  }

  /*
   * TODO port result consistency implementation
   *
   * @SuppressWarnings("unused")
   *
   * @Test(expected = IllegalArgumentException.class) public void differentSizeCheckTest() {
   *
   * SMGObject obj1 = createRegion(mockType8bSize); SMGObject obj2 = createRegion(96); SMG smg3 =
   * new SMG(); SMG smg4 = new SMG(); smg3 = smg3.copyAndAddObject(obj1); smg4 =
   * smg4.copyAndAddObject(obj2);
   *
   * new SMGJoinFields(smg3, smg4, obj1, obj2); }
   *
   * @Test public void consistencyCheckTest() throws SMGInconsistentException { SMG smg3 = new
   * SMG(); SMG smg4 = new SMG();
   *
   * SMGObject obj1 = createRegion(mockType32bSize); SMGObject obj2 = createRegion(mockType32bSize);
   * SMGValue value3 = createValue(); SMGValue value4 = createValue();
   *
   * smg3 = smg3.copyAndAddObject(obj1); smg4 = smg4.copyAndAddObject(obj2); smg3 =
   * smg3.copyAndAddValue(value3); smg4 = smg4.copyAndAddValue(value4);
   *
   * SMGHasValueEdge hvAt0in1 = createHasValueEdge(mockType4bSize, value3); SMGHasValueEdge hvAt0in2
   * = createHasValueEdge(mockType4bSize, value4); smg3 = smg3.copyAndAddHVEdge(hvAt0in1, obj1);
   * smg4 = smg4.copyAndAddHVEdge(hvAt0in2, obj2); SMGJoinFields.checkResultConsistency(smg3, smg4,
   * obj1, obj2);
   *
   * smg3 = smg3.copyAndAddHVEdge( createHasValueEdgeToZero(mockType4bSize, 32), obj1); smg4 =
   * smg4.copyAndAddHVEdge( createHasValueEdgeToZero(mockType4bSize, 32), obj2);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
   *
   * smg3 = smg3.copyAndAddHVEdge( createHasValueEdgeToZero(mockType8bSize, 64), obj1);
   *
   * smg4 = smg4.copyAndAddHVEdge( createHasValueEdgeToZero(mockType8bSize, 64), obj2);
   *
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2);
   *
   * smg3 = smg3.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, 128, value3), obj1); smg3 =
   * smg3.copyAndAddPTEdge(createPTRegionEdge(0, obj1), value3); smg4 =
   * smg4.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, 128, value4), obj2);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   *
   * @Test(expected = SMGInconsistentException.class) public void consistencyCheckNegativeTest1()
   * throws SMGInconsistentException { SMG smg3 = new SMG(); SMG smg4 = new SMG();
   *
   * SMGObject obj1 = createRegion(mockType32bSize); SMGObject obj2 = createRegion(mockType32bSize);
   * SMGValue value3 = createValue();
   *
   * smg3 = smg3.copyAndAddValue(value3); smg3 =
   * smg3.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, value3), obj1);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   *
   * @Test(expected = SMGInconsistentException.class) public void consistencyCheckNegativeTest2()
   * throws SMGInconsistentException { SMG smg3 = new SMG(); SMG smg4 = new SMG();
   *
   * SMGObject obj1 = createRegion(mockType32bSize); SMGObject obj2 = createRegion(mockType32bSize);
   * smg3.copyAndAddHVEdge(createHasValueEdgeToZero(mockType4bSize), obj1);
   * smg4.copyAndAddHVEdge(createHasValueEdgeToZero(mockType8bSize), obj2);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   *
   * @Test(expected = SMGInconsistentException.class) public void consistencyCheckNegativeTest3()
   * throws SMGInconsistentException { SMG smg3 = new SMG(); SMG smg4 = new SMG();
   *
   * SMGObject obj1 = createRegion(mockType32bSize); SMGObject obj2 = createRegion(mockType32bSize);
   *
   * smg3 = smg3.copyAndAddHVEdge( createHasValueEdgeToZero(mockType4bSize), obj1); smg3 =
   * smg3.copyAndAddHVEdge( createHasValueEdgeToZero(mockType4bSize, 64), obj1); smg4 =
   * smg4.copyAndAddHVEdge( createHasValueEdgeToZero(mockType8bSize), obj2);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   *
   * @Test(expected=SMGInconsistentException.class) public void consistencyCheckNegativeTest4()
   * throws SMGInconsistentException { SMG smg3 = = new SMG(); SMG smg4 = new SMG(); SMGObject obj1
   * = createRegion(mockType4bSize); SMGObject obj2 = createRegion(mockType4bSize); SMGValue value =
   * createValue(); smg3 = smg3.copyAndAddValue(value); smg4 = smg4.copyAndAddValue(value);
   *
   * smg3 = smg3.copyAndAddHVEdge( createHasValueEdge(mockType4bSize,value), obj1); smg3 =
   * smg3.copyAndAddHVEdge(createHasValueEdgeToZero(mockType4bSize, 32), obj1); smg4 =
   * smg4.copyAndAddHVEdge(createHasValueEdge(mockType8bSize, value), obj2);
   *
   * smg4 = smg4.copyAndAddHVEdge(createHasValueEdgeToZero(mockType4bSize, 64), obj2); smg4 =
   * smg4.copyAndAddHVEdge(createHasValueEdgeToZero(mockType4bSize, 96), obj2); smg3 =
   * smg3.copyAndAddHVEdge(createHasValueEdgeToZero(mockType8bSize, 64), obj1);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   *
   * @Test(expected = SMGInconsistentException.class) public void consistencyCheckNegativeTest5()
   * throws SMGInconsistentException { SMG smg3 = new SMG(); SMG smg4 = new SMG();
   *
   * SMGObject obj1 = createRegion(mockType32bSize); SMGObject obj2 = createRegion(mockType32bSize);
   *
   * SMGValue value4 = createValue(); smg3 = smg3.copyAndAddHVEdge(
   * createHasValueEdgeToZero(mockType4bSize), obj1); smg4 = smg4.copyAndAddValue(value4); smg4 =
   * smg4.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, value4), obj2);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   *
   * @Test public void consistencyCheckPositiveTest1() throws SMGInconsistentException { SMG smg3 =
   * new SMG(); SMG smg4 = new SMG();
   *
   * SMGObject obj1 = createRegion(mockType32bSize); SMGObject obj2 = createRegion(mockType32bSize);
   * SMGObject obj3 = createRegion(mockType32bSize);
   *
   * SMGValue value3 = createValue(); SMGValue value4 = createValue();
   *
   * smg3 = smg3.copyAndAddValue(value3); smg4 = smg4.copyAndAddValue(value4); smg3 =
   * smg3.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, value3), obj1); smg4 =
   * smg4.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, value4), obj2); smg4 =
   * smg4.copyAndAddHVEdge(createHasValueEdge(mockType4bSize, value4), obj3);
   * SMGJoinFields.checkResultConsistency(smg3, smg4, obj1, obj2); }
   */
  @Test
  public void nonMemberObjectTest1() {
    SMG smg3 = new SMG(mockType32bSize);
    SMG smg4 = new SMG(mockType32bSize);

    SMGObject obj1 = createRegion(mockType32bSize);
    SMGObject obj2 = createRegion(mockType32bSize);
    smg4 = smg4.copyAndAddObject(obj2);

    SMGJoinFields jFields = new SMGJoinFields(smg3, smg4);
    assertThrows(IllegalArgumentException.class, () -> jFields.joinFields(obj1, obj2));
  }

  @Test
  public void nonMemberObjectTest2() {
    SMG smg3 = new SMG(mockType32bSize);
    SMG smg4 = new SMG(mockType32bSize);

    SMGObject obj1 = createRegion(mockType32bSize);
    SMGObject obj2 = createRegion(mockType32bSize);
    smg3 = smg3.copyAndAddObject(obj1);

    SMGJoinFields jFields = new SMGJoinFields(smg3, smg4);
    assertThrows(IllegalArgumentException.class, () -> jFields.joinFields(obj1, obj2));
  }
}

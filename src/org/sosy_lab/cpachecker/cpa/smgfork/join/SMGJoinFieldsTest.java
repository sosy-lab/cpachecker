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
package org.sosy_lab.cpachecker.cpa.smgfork.join;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smgfork.AnonymousTypes;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smgfork.graphs.ReadableSMG;
import org.sosy_lab.cpachecker.cpa.smgfork.graphs.SMGFactory;
import org.sosy_lab.cpachecker.cpa.smgfork.graphs.WritableSMG;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGRegion;

import com.google.common.collect.Iterables;


public class SMGJoinFieldsTest {
  static private final CType mockType4b = AnonymousTypes.createTypeWithLength(4);
  static private final CType mockType8b = AnonymousTypes.createTypeWithLength(8);

  private WritableSMG smg1;
  private WritableSMG smg2;

  final private Integer value1 = SMGValueFactory.getNewValue();
  final private Integer value2 = SMGValueFactory.getNewValue();

  @Before
  public void setUp() {
    smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
  }

  @Test
  public void getHVSetWithoutNullValuesOnObjectTest() {
    SMGRegion obj1 = new SMGRegion(8, "1");
    SMGRegion obj2 = new SMGRegion(8, "1");

    SMGEdgeHasValue obj1hv1at0 = new SMGEdgeHasValue(mockType4b, 0, obj1, value1);
    SMGEdgeHasValue obj1hv0at4 = new SMGEdgeHasValue(mockType4b, 4, obj1, smg1.getNullValue());
    SMGEdgeHasValue obj2hv2at0 = new SMGEdgeHasValue(mockType4b, 0, obj2, value2);
    SMGEdgeHasValue obj2hv0at4 = new SMGEdgeHasValue(mockType4b, 4, obj2, smg1.getNullValue());

    smg1.addObject(obj1);
    smg1.addObject(obj2);
    smg1.addValue(value1);
    smg1.addValue(value2);
    smg1.addHasValueEdge(obj1hv0at4);
    smg1.addHasValueEdge(obj1hv1at0);
    smg1.addHasValueEdge(obj2hv0at4);
    smg1.addHasValueEdge(obj2hv2at0);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.getHVSetWithoutNullValuesOnObject(smg1, obj1);
    Assert.assertTrue(hvSet.contains(obj1hv1at0));
    Assert.assertTrue(hvSet.contains(obj2hv2at0));
    Assert.assertTrue(hvSet.contains(obj2hv0at4));
    Assert.assertEquals(3, hvSet.size());
  }

  @Test
  public void getHVSetOfMissingNullValuesTest() {
    SMGRegion obj1 = new SMGRegion(8, "1");
    SMGRegion obj2 = new SMGRegion(8, "2");

    smg1.addObject(obj1);
    smg2.addObject(obj2);
    smg2.addValue(value2);

    SMGEdgeHasValue nullifyObj1 = new SMGEdgeHasValue(8, 0, obj1, smg1.getNullValue());
    SMGEdgeHasValue nonPointer = new SMGEdgeHasValue(mockType4b, 2, obj2, value2);

    smg1.addHasValueEdge(nullifyObj1);
    smg2.addHasValueEdge(nonPointer);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.getHVSetOfMissingNullValues(smg1, smg2, obj1, obj2);
    Assert.assertEquals(0, hvSet.size());

    smg2.addPointsToEdge(new SMGEdgePointsTo(value2, obj2, 0));

    hvSet = SMGJoinFields.getHVSetOfMissingNullValues(smg1, smg2, obj1, obj2);
    Assert.assertEquals(1, hvSet.size());

    SMGEdgeHasValue newHv = Iterables.getOnlyElement(hvSet);
    Assert.assertEquals(smg1.getNullValue(), newHv.getValue());
    Assert.assertSame(obj1, newHv.getObject());
    Assert.assertEquals(4, newHv.getSizeInBytes(MachineModel.LINUX64));
    Assert.assertEquals(2, newHv.getOffset());
    Assert.assertTrue(newHv.isCompatibleField(nonPointer, MachineModel.LINUX64));
  }

  @Test
  public void getHVSetOfCommonNullValuesTest() {
    SMGRegion obj1 = new SMGRegion(22, "1");

    SMGEdgeHasValue smg1at4 = new SMGEdgeHasValue(mockType4b, 4, obj1, smg1.getNullValue());
    SMGEdgeHasValue smg2at8 = new SMGEdgeHasValue(mockType4b, 8, obj1, smg2.getNullValue());
    SMGEdgeHasValue smg1at14 = new SMGEdgeHasValue(mockType4b, 14, obj1, smg1.getNullValue());
    SMGEdgeHasValue smg2at12 = new SMGEdgeHasValue(mockType4b, 12, obj1, smg2.getNullValue());
    SMGEdgeHasValue smg1at18 = new SMGEdgeHasValue(mockType4b, 18, obj1, smg1.getNullValue());
    SMGEdgeHasValue smg2at18 = new SMGEdgeHasValue(mockType4b, 18, obj1, smg2.getNullValue());

    smg1.addHasValueEdge(smg1at18);
    smg1.addHasValueEdge(smg1at14);
    smg1.addHasValueEdge(smg1at4);
    smg2.addHasValueEdge(smg2at18);
    smg2.addHasValueEdge(smg2at12);
    smg2.addHasValueEdge(smg2at8);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.getHVSetOfCommonNullValues(smg1, smg2, obj1, obj1);
    Assert.assertEquals(2, hvSet.size());
    for (SMGEdgeHasValue hv : hvSet) {
      Assert.assertEquals(hv.getValue(), smg1.getNullValue());
      Assert.assertSame(hv.getObject(), obj1);
      Assert.assertTrue(hv.getOffset() == 14 || hv.getOffset() == 18);
      if (hv.getOffset() == 14) {
        Assert.assertTrue(hv.getSizeInBytes(MachineModel.LINUX64) == 2);
      }
      else {
        Assert.assertTrue(hv.getSizeInBytes(MachineModel.LINUX64) == 4);
      }
    }
  }

  @Test
  public void getCompatibleHVEdgeSetTest() {
    SMGRegion obj = new SMGRegion(32, "Object");
    SMGRegion differentObject = new SMGRegion(16, "Different object");

    smg1.addObject(obj);
    smg2.addObject(obj);
    smg1.addObject(differentObject);

    smg2.addValue(value1);

    SMGEdgeHasValue hv0for4at0in1 = new SMGEdgeHasValue(mockType4b, 0, obj, smg1.getNullValue());
    SMGEdgeHasValue hv0for4at0in2 = new SMGEdgeHasValue(mockType4b, 0, obj, smg2.getNullValue());

    SMGEdgeHasValue hv0for4at5in1 = new SMGEdgeHasValue(mockType4b, 5, obj, smg1.getNullValue());
    SMGEdgeHasValue hv0for4at7in2 = new SMGEdgeHasValue(mockType4b, 7, obj, smg2.getNullValue());

    SMGEdgeHasValue hv0for4at12in1 = new SMGEdgeHasValue(mockType4b, 12, obj, smg1.getNullValue());
    SMGEdgeHasValue hv0for4at16in2 = new SMGEdgeHasValue(mockType4b, 16, obj, smg2.getNullValue());

    SMGEdgeHasValue hv0for4at20in1 = new SMGEdgeHasValue(mockType4b, 20, obj, smg1.getNullValue());
    SMGEdgeHasValue hv666for4at20in2 = new SMGEdgeHasValue(mockType4b, 20, obj, value1);

    SMGEdgeHasValue hv666for4at28in2 = new SMGEdgeHasValue(mockType4b, 28, obj, value1);

    SMGEdgeHasValue diffObjectNullValue = new SMGEdgeHasValue(mockType4b, 0, differentObject, smg1.getNullValue());

    smg1.addHasValueEdge(hv0for4at0in1);
    smg1.addHasValueEdge(hv0for4at5in1);
    smg1.addHasValueEdge(hv0for4at12in1);
    smg1.addHasValueEdge(hv0for4at20in1);
    smg1.addHasValueEdge(diffObjectNullValue);

    smg2.addHasValueEdge(hv0for4at0in2);
    smg2.addHasValueEdge(hv0for4at7in2);
    smg2.addHasValueEdge(hv0for4at16in2);
    smg2.addHasValueEdge(hv666for4at20in2);
    smg2.addPointsToEdge(new SMGEdgePointsTo(value1, obj, 20));
    smg2.addHasValueEdge(hv666for4at28in2);

    Set<SMGEdgeHasValue> compSet1 = SMGJoinFields.getCompatibleHVEdgeSet(smg1, smg2, obj, obj);
    Assert.assertEquals(4, compSet1.size());

    Set<SMGEdgeHasValue> compSet2 = SMGJoinFields.getCompatibleHVEdgeSet(smg2, smg1, obj, obj);
    Assert.assertEquals(4, compSet2.size());
  }

  @Test
  public void mergeNonNullHVEdgesTest() {
    Set<Integer> values = new HashSet<>();
    values.add(value1);
    values.add(value2);

    SMGRegion object = new SMGRegion(16, "Object");
    SMGEdgeHasValue smg1_4bFrom0ToV1 = new SMGEdgeHasValue(mockType4b, 0, object, value1);
    SMGEdgeHasValue smg1_4bFrom2ToV2 = new SMGEdgeHasValue(mockType4b, 2, object, value2);
    SMGEdgeHasValue smg1_4bFrom4ToNull = new SMGEdgeHasValue(mockType4b, 4, object, smg1.getNullValue());

    smg1.addObject(object);
    smg1.addValue(value1);
    smg1.addValue(value2);
    smg1.addHasValueEdge(smg1_4bFrom4ToNull);
    smg1.addHasValueEdge(smg1_4bFrom2ToV2);
    smg1.addHasValueEdge(smg1_4bFrom0ToV1);

    smg2.addObject(object);

    Set<SMGEdgeHasValue> hvSet = SMGJoinFields.mergeNonNullHasValueEdges(smg1, smg2, object, object);
    Assert.assertEquals(2, hvSet.size());

    boolean seenZero = false;
    boolean seenTwo = false;

    SMGEdgeHasValueFilter filter = SMGEdgeHasValueFilter.objectFilter(object);
    for (SMGEdgeHasValue edge : filter.filterSet(hvSet)) {
      if (edge.getOffset() == 0) {
        seenZero = true;
      }
      else if (edge.getOffset() == 2) {
        seenTwo = true;
      }
      Assert.assertTrue(edge.getOffset() == 0 || edge.getOffset() == 2);
      Assert.assertTrue(mockType4b.equals(edge.getType()));
      Assert.assertFalse(values.contains(Integer.valueOf(edge.getValue())));
      values.add(Integer.valueOf(edge.getValue()));
    }
    Assert.assertTrue(seenZero);
    Assert.assertTrue(seenTwo);

    smg2.addValue(value1);
    smg2.addHasValueEdge(smg1_4bFrom0ToV1);
    hvSet = SMGJoinFields.mergeNonNullHasValueEdges(smg1, smg2, object, object);
    Assert.assertEquals(1, hvSet.size());
  }

  @Test
  public void mergeNonNullAplliedTest() {
    SMGRegion obj1 = new SMGRegion(8, "Object 1");
    SMGRegion obj2 = new SMGRegion(8, "Object 2");
    smg1.addObject(obj1);
    smg2.addObject(obj2);

    Integer value1 = SMGValueFactory.getNewValue();
    smg1.addValue(value1);
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, value1));

    SMGJoinFields jf = new SMGJoinFields(SMGFactory.createWritableCopy(smg1), SMGFactory.createWritableCopy(smg2), obj1, obj2);
    ReadableSMG resultSMG = jf.getSMG2();

    Set<SMGEdgeHasValue> edges = resultSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2));
    Assert.assertTrue(edges.size() > 0);

    jf = new SMGJoinFields(SMGFactory.createWritableCopy(smg2), SMGFactory.createWritableCopy(smg1), obj2, obj1);
    resultSMG = jf.getSMG1();

    edges = resultSMG.getHVEdges(SMGEdgeHasValueFilter.objectFilter(obj2));
    Assert.assertTrue(edges.size() > 0);
  }

  @Test
  public void joinFieldsRelaxStatusTest() {
    SMGRegion object = new SMGRegion(8, "Object");
    smg1.addObject(object);

    WritableSMG smg04 = SMGFactory.createWritableCopy(smg1);
    WritableSMG smg48 = SMGFactory.createWritableCopy(smg1);
    WritableSMG smg26 = SMGFactory.createWritableCopy(smg1);
    WritableSMG smg08 = SMGFactory.createWritableCopy(smg1);

    smg04.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, object, smg04.getNullValue()));
    smg48.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 4, object, smg48.getNullValue()));
    smg26.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 2, object, smg26.getNullValue()));
    smg08.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, object, smg08.getNullValue()));
    smg08.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 4, object, smg08.getNullValue()));

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

  @Test(expected=IllegalArgumentException.class)
  public void differentSizeCheckTest(){
    SMGRegion obj1 = new SMGRegion(8, "Object 1");
    SMGRegion obj2 = new SMGRegion(12, "Object 2");
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    smg1.addObject(obj1);
    smg2.addObject(obj2);

    SMGJoinFields jf = new SMGJoinFields(smg1, smg2, obj1, obj2);
    jf.getStatus(); // Avoid dead store warning
  }

  @Test
  public void consistencyCheckTest() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");
    Integer value1 = SMGValueFactory.getNewValue();
    Integer value2 = SMGValueFactory.getNewValue();

    smg1.addObject(obj1);
    smg2.addObject(obj2);
    smg1.addValue(value1);
    smg2.addValue(value2);

    SMGEdgeHasValue hvAt0in1 = new SMGEdgeHasValue(mockType4b, 0, obj1, value1);
    SMGEdgeHasValue hvAt0in2 = new SMGEdgeHasValue(mockType4b, 0, obj2, value2);
    smg1.addHasValueEdge(hvAt0in1);
    smg2.addHasValueEdge(hvAt0in2);
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 4, obj1, smg1.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 4, obj2, smg2.getNullValue()));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 8, obj1, smg1.getNullValue()));
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 12, obj1, smg1.getNullValue()));
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 8, obj1, smg1.getNullValue()));

    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 8, obj2, smg2.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 12, obj2, smg2.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 8, obj2, smg2.getNullValue()));

    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 16, obj1, value1));
    smg1.addPointsToEdge(new SMGEdgePointsTo(value1, obj1, 0));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 16, obj2, smg2.getNullValue()));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest1() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    ReadableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");
    Integer value1 = SMGValueFactory.getNewValue();

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, value1));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest2() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, smg1.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 0, obj2, smg2.getNullValue()));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest3() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, smg1.getNullValue()));
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 4, obj1, smg1.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 0, obj2, smg2.getNullValue()));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest4() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, smg1.getNullValue()));
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 4, obj1, smg1.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 0, obj2, smg2.getNullValue()));

    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 8, obj2, smg2.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 12, obj2, smg2.getNullValue()));
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType8b, 8, obj1, smg1.getNullValue()));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test(expected=SMGInconsistentException.class)
  public void consistencyCheckNegativeTest5() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");

    Integer value2 = SMGValueFactory.getNewValue();
    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, smg1.getNullValue()));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj2, value2));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test
  public void consistencyCheckPositiveTest1() throws SMGInconsistentException {
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");
    SMGRegion obj3 = new SMGRegion(32, "Object 3");

    Integer value1 = SMGValueFactory.getNewValue();
    Integer value2 = SMGValueFactory.getNewValue();

    smg1.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj1, value1));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj2, value2));
    smg2.addHasValueEdge(new SMGEdgeHasValue(mockType4b, 0, obj3, value2));
    SMGJoinFields.checkResultConsistency(smg1, smg2, obj1, obj2);
  }

  @Test(expected=IllegalArgumentException.class)
  public void nonMemberObjectTest1(){
    ReadableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    WritableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");
    smg2.addObject(obj2);

    SMGJoinFields jf = new SMGJoinFields(smg1, smg2, obj1, obj2);
    jf.getStatus(); // Avoid dead store warning
  }

  @Test(expected=IllegalArgumentException.class)
  public void nonMemberObjectTest2(){
    WritableSMG smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    ReadableSMG smg2 = SMGFactory.createWritableSMG(MachineModel.LINUX64);

    SMGRegion obj1 = new SMGRegion(32, "Object 1");
    SMGRegion obj2 = new SMGRegion(32, "Object 2");
    smg1.addObject(obj1);

    SMGJoinFields jf = new SMGJoinFields(smg1, smg2, obj1, obj2);
    jf.getStatus(); // Avoid dead store warning
  }
}

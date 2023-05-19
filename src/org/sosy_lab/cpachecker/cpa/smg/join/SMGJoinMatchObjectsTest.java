// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.DummyAbstraction;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;

public class SMGJoinMatchObjectsTest {

  private SMG smg1;
  private SMG smg2;

  private final SMGObject srcObj1 = new SMGRegion(64, "Source object 1");
  private final SMGObject destObj1 = new SMGRegion(64, "Destination object 1");
  private final SMGObject srcObj2 = new SMGRegion(64, "Source object 2");
  private final SMGObject destObj2 = new SMGRegion(64, "Destination object 2");

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
    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            null,
            null,
            SMGNullObject.INSTANCE,
            SMGNullObject.INSTANCE);
    assertThat(mo.isDefined()).isFalse();

    smg1.addObject(srcObj1);
    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, null, null, srcObj1, SMGNullObject.INSTANCE);
    assertThat(mo.isDefined()).isFalse();
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void nonMemberObjectsTestObj1() {
    smg2.addObject(srcObj2);

    new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, srcObj1, srcObj2);
  }

  @SuppressWarnings("unused")
  @Test(expected = IllegalArgumentException.class)
  public void nonMemberObjectsTestObj2() {
    smg1.addObject(srcObj1);

    new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, srcObj1, srcObj2);
  }

  @Test
  public void inconsistentMappingTest() {
    mapping1.map(srcObj1, destObj1);
    smg1.addObject(srcObj1);

    smg2.addObject(srcObj2);
    mapping2.map(srcObj2, destObj1);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void inconsistentMappingViceVersaTest() {
    mapping2.map(srcObj2, destObj2);
    smg2.addObject(srcObj2);

    smg1.addObject(srcObj1);
    mapping1.map(srcObj1, destObj2);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void inconsistentObjectsTest() {
    SMGObject diffSizeObject = new SMGRegion(128, "Object with different size");
    smg1.addObject(srcObj1);
    smg2.addObject(diffSizeObject);
    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, diffSizeObject);
    assertThat(mo.isDefined()).isFalse();

    smg2.addObject(srcObj2, false, false);
    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void nonMatchingMappingTest() {
    smg1.addObject(srcObj1);
    smg1.addObject(destObj1);
    mapping1.map(srcObj1, destObj1);

    smg2.addObject(srcObj2);
    smg2.addObject(destObj2);
    mapping2.map(srcObj2, destObj2);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void fieldInconsistencyTest() {
    smg1.addObject(srcObj1);
    smg2.addObject(srcObj2);

    SMGEdgeHasValue hv1 = new SMGEdgeHasValue(2, 0, srcObj1, SMGKnownSymValue.of());
    SMGEdgeHasValue hv2 = new SMGEdgeHasValue(2, 2, srcObj2, SMGKnownSymValue.of());
    SMGEdgeHasValue hvMatching1 = new SMGEdgeHasValue(2, 4, srcObj1, SMGKnownSymValue.of());
    SMGEdgeHasValue hvMatching2 = new SMGEdgeHasValue(2, 4, srcObj2, SMGKnownSymValue.of());

    smg1.addValue(hv1.getValue());
    smg1.addHasValueEdge(hv1);

    smg2.addValue(hv2.getValue());
    smg2.addHasValueEdge(hv2);

    smg1.addValue(hvMatching1.getValue());
    smg1.addHasValueEdge(hvMatching1);

    smg2.addValue(hvMatching2.getValue());
    smg2.addHasValueEdge(hvMatching2);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isTrue();

    mapping1.map(hvMatching1.getValue(), SMGKnownSymValue.of());
    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isTrue();

    mapping2.map(hvMatching2.getValue(), mapping1.get(hvMatching1.getValue()));
    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isTrue();

    mapping2.map(hvMatching2.getValue(), SMGKnownSymValue.of());
    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, srcObj1, srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void sameAbstractionMatchTest() {
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(128, 0, 8, 7, 0);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(128, 0, 0, 7, 0);

    smg1.addObject(sll1);
    smg2.addObject(sll2);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void differentAbstractionMatch() {
    SMGRegion prototype = new SMGRegion(128, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(128, 0, 8, 3, 0);
    DummyAbstraction dummy = new DummyAbstraction(prototype);

    smg1.addObject(sll);
    smg2.addObject(dummy);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll, dummy);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void twoAbstractionsTest() {
    SMGSingleLinkedList sll1 = new SMGSingleLinkedList(128, 0, 8, 2, 0);
    SMGSingleLinkedList sll2 = new SMGSingleLinkedList(128, 0, 8, 4, 0);
    smg1.addObject(sll1);
    smg2.addObject(sll2);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.LEFT_ENTAIL);

    sll1 = new SMGSingleLinkedList(128, 0, 8, 4, 0);
    smg1.addObject(sll1);
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);

    sll1 = new SMGSingleLinkedList(128, 0, 8, 8, 0);
    smg1.addObject(sll1);
    mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll1, sll2);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.RIGHT_ENTAIL);
  }

  @Test
  public void oneAbstractionTest() {
    SMGRegion prototype = new SMGRegion(128, "prototype");
    SMGSingleLinkedList sll = new SMGSingleLinkedList(128, 0, 8, 8, 0);

    smg1.addObject(sll);
    smg2.addObject(sll);
    smg1.addObject(prototype);
    smg2.addObject(prototype);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll, prototype);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.INCOMPARABLE);

    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, prototype, sll);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.INCOMPARABLE);

    sll = new SMGSingleLinkedList(128, 0, 8, 0, 0);

    smg1.addObject(sll);
    smg2.addObject(sll);

    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, sll, prototype);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.LEFT_ENTAIL);

    mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, prototype, sll);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.RIGHT_ENTAIL);
  }

  @Test
  public void noAbstractionTest() {
    SMGRegion object = new SMGRegion(128, "prototype");
    smg1.addObject(object);
    smg2.addObject(object);
    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, object, object);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
  }
}

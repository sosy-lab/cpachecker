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

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

public class SMGMatchObjectsTest extends SMGJoinTest0 {

  private SMG smg1;
  private SMG smg2;

  private final SMGObject srcObj1 = createRegion(mockType4bSize);
  private final SMGObject destObj1 = createRegion(mockType4bSize);
  private final SMGObject srcObj2 = createRegion(mockType4bSize);
  private final SMGObject destObj2 = createRegion(mockType4bSize);

  private NodeMapping mapping1;
  private NodeMapping mapping2;

  @Before
  public void setUp() {
    smg1 = new SMG(mockType8bSize);
    smg2 = new SMG(mockType8bSize);

    mapping1 = new NodeMapping();
    mapping2 = new NodeMapping();
  }

  @Test
  public void nullObjectTest() {
    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            SMGObject.nullInstance(),
            SMGObject.nullInstance());
    assertThat(mo.isDefined()).isFalse();

    smg1 = smg1.copyAndAddObject(srcObj1);
    mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            SMGObject.nullInstance());
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void nonMemberObjectsTestObj1() {
    smg2 = smg2.copyAndAddObject(srcObj2);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SMGMatchObjects(
                SMGJoinStatus.EQUAL,
                smg1,
                smg2,
                new SMG(smg1.getSizeOfPointer()),
                mapping1,
                mapping2,
                srcObj1,
                srcObj2));
  }

  @Test
  public void nonMemberObjectsTestObj2() {
    smg1 = smg1.copyAndAddObject(srcObj1);
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new SMGMatchObjects(
                SMGJoinStatus.EQUAL,
                smg1,
                smg2,
                new SMG(smg1.getSizeOfPointer()),
                mapping1,
                mapping2,
                srcObj1,
                srcObj2));
  }

  @Test
  public void inconsistentMappingTest() {
    mapping1.addMapping(srcObj1, destObj1);
    smg1 = smg1.copyAndAddObject(srcObj1);

    smg2 = smg2.copyAndAddObject(srcObj2);
    mapping2.addMapping(srcObj2, destObj1);

    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void inconsistentMappingViceVersaTest() {
    mapping2.addMapping(srcObj2, destObj2);
    smg2 = smg2.copyAndAddObject(srcObj2);

    smg1 = smg1.copyAndAddObject(srcObj1);
    mapping1.addMapping(srcObj1, destObj2);

    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void inconsistentObjectsTest() {
    SMGObject diffSizeObject = createRegion(128);
    smg1 = smg1.copyAndAddObject(srcObj1);
    smg2 = smg2.copyAndAddObject(diffSizeObject);
    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            diffSizeObject);
    assertThat(mo.isDefined()).isFalse();

    smg2 = smg2.copyAndAddObject(srcObj2);
    smg2 = smg2.copyAndInvalidateObject(srcObj2);
    mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void nonMatchingMappingTest() {
    smg1 = smg1.copyAndAddObject(srcObj1);
    smg1 = smg1.copyAndAddObject(destObj1);
    mapping1.addMapping(srcObj1, destObj1);

    smg2 = smg2.copyAndAddObject(srcObj2);
    smg2 = smg2.copyAndAddObject(destObj2);
    mapping2.addMapping(srcObj2, destObj2);

    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void fieldInconsistencyTest() {
    smg1 = smg1.copyAndAddObject(srcObj1);
    smg2 = smg2.copyAndAddObject(srcObj2);

    SMGHasValueEdge hv1 = createHasValueEdge(2, 0, createValue());
    SMGHasValueEdge hv2 = createHasValueEdge(2, 2, createValue());
    SMGHasValueEdge hvMatching1 = createHasValueEdge(2, 4, createValue());
    SMGHasValueEdge hvMatching2 = createHasValueEdge(2, 4, createValue());

    smg1 = smg1.copyAndAddValue(hv1.hasValue());
    smg1 = smg1.copyAndAddHVEdge(hv1, srcObj1);

    smg2 = smg2.copyAndAddValue(hv2.hasValue());
    smg2 = smg2.copyAndAddHVEdge(hv2, srcObj2);

    smg1 = smg1.copyAndAddValue(hvMatching1.hasValue());
    smg1 = smg1.copyAndAddHVEdge(hvMatching1, srcObj1);

    smg2 = smg2.copyAndAddValue(hvMatching2.hasValue());
    smg2 = smg2.copyAndAddHVEdge(hvMatching2, srcObj2);

    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isTrue();

    mapping1.addMapping(hvMatching1.hasValue(), createValue());
    mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isTrue();

    mapping2.addMapping(hvMatching2.hasValue(), mapping1.getMappedValue(hvMatching1.hasValue()));
    mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isTrue();

    mapping2.addMapping(hvMatching2.hasValue(), createValue());
    mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            srcObj1,
            srcObj2);
    assertThat(mo.isDefined()).isFalse();
  }

  @Test
  public void noAbstractionTest() {
    SMGObject object = createRegion(128);
    smg1 = smg1.copyAndAddObject(object);
    smg2 = smg2.copyAndAddObject(object);
    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            new SMG(smg1.getSizeOfPointer()),
            mapping1,
            mapping2,
            object,
            object);
    assertThat(mo.isDefined()).isTrue();
    assertThat(mo.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
  }
}

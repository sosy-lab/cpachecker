// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGJoinTargetObjectsTest extends SMGJoinTest0 {
  private SMG smg1;
  private SMG smg2;
  private SMG destSMG;

  private NodeMapping mapping1;
  private NodeMapping mapping2;

  private final SMGObject obj1 = createRegion(64);
  private final SMGValue value1 = createValue();
  private final SMGPointsToEdge pt1 = createPTRegionEdge(0, obj1);

  private final SMGObject obj2 = createRegion(64);
  private final SMGValue value2 = createValue();
  private final SMGPointsToEdge pt2 = createPTRegionEdge(0, obj2);

  private final SMGObject destObj = createRegion(64);

  @Before
  public void setUp() {
    smg1 = new SMG(mockType8bSize);
    smg2 = new SMG(mockType8bSize);
    destSMG = new SMG(mockType8bSize);

    mapping1 = new NodeMapping();
    mapping2 = new NodeMapping();
  }

  @Test
  public void matchingObjectsWithoutMappingTest() {
    smg1 = smg1.copyAndAddObject(obj1);
    smg1 = smg1.copyAndAddValue(value1);
    smg1 = smg1.copyAndAddPTEdge(pt1, value1);

    smg2 = smg2.copyAndAddObject(obj2);
    smg2 = smg2.copyAndAddValue(value2);
    smg2 = smg2.copyAndAddPTEdge(pt2, value2);

    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2, 0);
    assertThat(jto.mapping2.getMappedObject(obj2))
        .isSameInstanceAs(jto.mapping1.getMappedObject(obj1));
    // TODO investigate why they should not be the same, regions are immutable
    // Assert.assertNotSame(jto.mapping1.get(obj1), obj1);
    assertThat(jto.mapping1.getMappedObject(obj1)).isNotNull();
    assertThat(obj1.getOffset()).isEqualTo(jto.mapping1.getMappedObject(obj1).getOffset());
    assertThat(obj1.getSize()).isEqualTo(jto.mapping1.getMappedObject(obj1).getSize());
  }

  @Test
  public void nonMatchingObjectsTest() {
    smg1 = smg1.copyAndAddObject(obj1);
    smg1 = smg1.copyAndAddValue(value1);
    smg1 = smg1.copyAndAddPTEdge(pt1, value1);

    SMGMatchObjects mo =
        new SMGMatchObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSMG,
            mapping1,
            mapping2,
            obj1,
            SMGObject.nullInstance());
    assertThat(mo.isDefined()).isFalse();
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSMG,
            mapping1,
            mapping2,
            value1,
            SMGValue.zeroValue(),
            0);
    assertThat(jto.isDefined()).isFalse();
    assertThat(jto.isRecoverableFailur()).isTrue();
  }

  @Test
  public void joinTargetObjectsDifferentOffsets() {
    SMGPointsToEdge pt1null = createPTRegionEdge(2, SMGObject.nullInstance());
    SMGPointsToEdge pt2null = createPTRegionEdge(1, SMGObject.nullInstance());

    smg1 = smg1.copyAndAddObject(obj1);
    smg1 = smg1.copyAndAddValue(value1);
    smg1 = smg1.copyAndAddPTEdge(pt1null, value1);

    smg2 = smg2.copyAndAddObject(obj2);
    smg2 = smg2.copyAndAddValue(value2);
    smg2 = smg2.copyAndAddPTEdge(pt2null, value2);

    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2, 0);

    assertThat(jto.isDefined()).isFalse();
    assertThat(jto.isRecoverableFailur()).isTrue();
  }

  @Test
  public void joinTargetObjectsAlreadyJoinedNull() {
    SMGPointsToEdge pt1null = createPTRegionEdge(0, SMGObject.nullInstance());
    SMGPointsToEdge pt2null = createPTRegionEdge(0, SMGObject.nullInstance());

    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value2);

    smg1 = smg1.copyAndAddPTEdge(pt1null, value1);
    smg2 = smg2.copyAndAddPTEdge(pt2null, value2);

    SMGJoinTargetObjects mta =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2, 0);
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2, 0);
    assertThat(jto.isDefined()).isTrue();
    assertThat(jto.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jto.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jto.getInputSMG2()).isSameInstanceAs(smg2);
    assertThat(jto.getDestinationSMG()).isEqualTo(mta.getDestinationSMG());
    assertThat(jto.mapping1).isEqualTo(mta.mapping1);
    assertThat(jto.mapping2).isEqualTo(mta.mapping2);
    assertThat(jto.getValue()).isEqualTo(mta.getValue());
  }

  @Test
  public void joinTargetObjectsAlreadyJoinedNonNull() {
    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value2);

    smg1 = smg1.copyAndAddObject(obj1);
    smg2 = smg2.copyAndAddObject(obj2);
    destSMG = destSMG.copyAndAddObject(destObj);

    smg1 = smg1.copyAndAddPTEdge(pt1, value1);
    smg2 = smg2.copyAndAddPTEdge(pt2, value2);

    mapping1.addMapping(obj1, destObj);
    mapping2.addMapping(obj2, destObj);

    // See TODO below
    // SMGMapTargetAddress mta = new SMGMapTargetAddress(new SMG(smg1), new SMG(smg2), new
    // SMG(destSMG),
    //                                                  new SMGNodeMapping(mapping1), new
    // SMGNodeMapping(mapping2),
    //                                                  value1, value2);
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2, 0);
    assertThat(jto.isDefined()).isTrue();
    assertThat(jto.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jto.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jto.getInputSMG2()).isSameInstanceAs(smg2);
    // TODO: Not equal, but isomorphic (newly created values differ in mta and jto)
    //       But we currently do not have isomorphism
    // Assert.assertEquals(mta.getSMG(), jto.getDestinationSMG());

    assertThat(jto.mapping1.hasMapping(value1)).isTrue();
    assertThat(jto.getValue()).isEqualTo(jto.mapping1.getMappedValue(value1));

    assertThat(jto.mapping2.hasMapping(value2)).isTrue();
    assertThat(jto.getValue()).isEqualTo(jto.mapping2.getMappedValue(value2));
  }
}

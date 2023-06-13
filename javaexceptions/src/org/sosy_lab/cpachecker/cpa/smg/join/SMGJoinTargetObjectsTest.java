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
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

public class SMGJoinTargetObjectsTest {
  private SMG smg1;
  private SMG smg2;
  private SMG destSMG;

  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  private final SMGObject obj1 = new SMGRegion(64, "ze label");
  private final SMGSymbolicValue value1 = SMGKnownSymValue.of();
  private final SMGEdgePointsTo pt1 = new SMGEdgePointsTo(value1, obj1, 0);

  private final SMGObject obj2 = new SMGRegion(64, "ze label");
  private final SMGSymbolicValue value2 = SMGKnownSymValue.of();
  private final SMGEdgePointsTo pt2 = new SMGEdgePointsTo(value2, obj2, 0);

  private final SMGObject destObj = new SMGRegion(64, "destination");

  @Before
  public void setUp() {
    smg1 = new SMG(MachineModel.LINUX64);
    smg2 = new SMG(MachineModel.LINUX64);
    destSMG = new SMG(MachineModel.LINUX64);

    mapping1 = new SMGNodeMapping();
    mapping2 = new SMGNodeMapping();
  }

  @Test
  public void matchingObjectsWithoutMappingTest() throws SMGInconsistentException {
    smg1.addObject(obj1);
    smg1.addValue(value1);
    smg1.addPointsToEdge(pt1);

    smg2.addObject(obj2);
    smg2.addValue(value2);
    smg2.addPointsToEdge(pt2);

    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSMG,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            0,
            0,
            false,
            null,
            null);
    assertThat(jto.mapping2.get(obj2)).isSameInstanceAs(jto.mapping1.get(obj1));
    // TODO investigate why they should not be the same, regions are immutable
    // Assert.assertNotSame(jto.mapping1.get(obj1), obj1);
    assertThat(obj1.getLabel()).isEqualTo(jto.mapping1.get(obj1).getLabel());
    assertThat(obj1.getSize()).isEqualTo(jto.mapping1.get(obj1).getSize());
  }

  @Test
  public void nonMatchingObjectsTest() throws SMGInconsistentException {
    smg1.addObject(obj1);
    smg1.addValue(value1);
    smg1.addPointsToEdge(pt1);

    SMGJoinMatchObjects mo =
        new SMGJoinMatchObjects(
            SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, obj1, SMGNullObject.INSTANCE);
    assertThat(mo.isDefined()).isFalse();
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSMG,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            SMGZeroValue.INSTANCE,
            0,
            0,
            0,
            false,
            null,
            null);
    assertThat(jto.isDefined()).isFalse();
    assertThat(jto.isRecoverable()).isTrue();
  }

  @Test
  public void joinTargetObjectsDifferentOffsets() throws SMGInconsistentException {
    SMGEdgePointsTo pt1null = new SMGEdgePointsTo(value1, SMGNullObject.INSTANCE, 2);
    SMGEdgePointsTo pt2null = new SMGEdgePointsTo(value2, SMGNullObject.INSTANCE, 1);

    smg1.addObject(obj1);
    smg1.addValue(value1);
    smg1.addPointsToEdge(pt1null);

    smg2.addObject(obj2);
    smg2.addValue(value2);
    smg2.addPointsToEdge(pt2null);

    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            null,
            null,
            null,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            0,
            0,
            false,
            null,
            null);

    assertThat(jto.isDefined()).isFalse();
    assertThat(jto.isRecoverable()).isTrue();
  }

  @Test
  public void joinTargetObjectsAlreadyJoinedNull() throws SMGInconsistentException {
    SMGEdgePointsTo pt1null = new SMGEdgePointsTo(value1, SMGNullObject.INSTANCE, 0);
    SMGEdgePointsTo pt2null = new SMGEdgePointsTo(value2, SMGNullObject.INSTANCE, 0);

    smg1.addValue(value1);
    smg2.addValue(value2);

    smg1.addPointsToEdge(pt1null);
    smg2.addPointsToEdge(pt2null);

    SMGJoinMapTargetAddress mta =
        new SMGJoinMapTargetAddress(
            smg1.copyOf(),
            smg2.copyOf(),
            destSMG.copyOf(),
            new SMGNodeMapping(mapping1),
            new SMGNodeMapping(mapping2),
            value1,
            value2);
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSMG,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            0,
            0,
            false,
            null,
            null);
    assertThat(jto.isDefined()).isTrue();
    assertThat(jto.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jto.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jto.getInputSMG2()).isSameInstanceAs(smg2);
    assertThat(jto.getDestinationSMG()).isEqualTo(mta.getSMG());
    assertThat(jto.mapping1).isEqualTo(mta.mapping1);
    assertThat(jto.mapping2).isEqualTo(mta.mapping2);
    assertThat(jto.getValue()).isEqualTo(mta.getValue());
  }

  @Test
  public void joinTargetObjectsAlreadyJoinedNonNull() throws SMGInconsistentException {
    smg1.addValue(value1);
    smg2.addValue(value2);

    smg1.addObject(obj1);
    smg2.addObject(obj2);
    destSMG.addObject(destObj);

    smg1.addPointsToEdge(pt1);
    smg2.addPointsToEdge(pt2);

    mapping1.map(obj1, destObj);
    mapping2.map(obj2, destObj);

    // See TODO below
    // SMGMapTargetAddress mta = new SMGMapTargetAddress(new SMG(smg1), new SMG(smg2), new
    // SMG(destSMG),
    //                                                  new SMGNodeMapping(mapping1), new
    // SMGNodeMapping(mapping2),
    //                                                  value1, value2);
    SMGJoinTargetObjects jto =
        new SMGJoinTargetObjects(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSMG,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            0,
            0,
            false,
            null,
            null);
    assertThat(jto.isDefined()).isTrue();
    assertThat(jto.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jto.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jto.getInputSMG2()).isSameInstanceAs(smg2);
    // TODO: Not equal, but isomorphic (newly created values differ in mta and jto)
    //       But we currently do not have isomorphism
    // Assert.assertEquals(mta.getSMG(), jto.getDestinationSMG());

    assertThat(jto.mapping1.containsKey(value1)).isTrue();
    assertThat(jto.getValue()).isEqualTo(jto.mapping1.get(value1));

    assertThat(jto.mapping2.containsKey(value2)).isTrue();
    assertThat(jto.getValue()).isEqualTo(jto.mapping2.get(value2));
  }
}

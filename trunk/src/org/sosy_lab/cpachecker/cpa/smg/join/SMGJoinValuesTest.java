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
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGOptions;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

public class SMGJoinValuesTest {
  private SMG smg1;
  private SMG smg2;
  private SMG smgDest;

  private SMGState dummyState;

  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  private final SMGValue value1 = SMGKnownSymValue.of();
  private final SMGValue value2 = SMGKnownSymValue.of();
  private final SMGValue value3 = SMGKnownSymValue.of();

  @Before
  public void setUp() throws InvalidConfigurationException {
    dummyState =
        new SMGState(
            LogManager.createTestLogManager(),
            MachineModel.LINUX32,
            new SMGOptions(Configuration.defaultConfiguration()));
    smg1 = new SMG(MachineModel.LINUX64);
    smg2 = new SMG(MachineModel.LINUX64);
    smgDest = new SMG(MachineModel.LINUX64);

    mapping1 = new SMGNodeMapping();
    mapping2 = new SMGNodeMapping();
  }

  //  Test disabled until Join is not called correctly from isLessOrEqual (see SMGJoinValues)
  //  @Test
  //  public void joinValuesIdenticalTest() throws SMGInconsistentException {
  //    smg1.addValue(value1);
  //    smg2.addValue(value1);
  //
  //    SMGJoinValues jv = new SMGJoinValues(SMGJoinStatus.EQUAL, smg1, smg2, smgDest, null, null,
  // value1, value1);
  //    Assert.assertTrue(jv.isDefined());
  //    Assert.assertEquals(SMGJoinStatus.EQUAL, jv.getStatus());
  //    Assert.assertSame(smg1, jv.getInputSMG1());
  //    Assert.assertSame(smg2, jv.getInputSMG2());
  //    Assert.assertSame(smgDest, jv.getDestinationSMG());
  //    Assert.assertSame(null, jv.getMapping1());
  //    Assert.assertSame(null, jv.getMapping2());
  //    Assert.assertEquals(value1, jv.getValue());
  //  }

  @Test
  public void joinValuesAlreadyJoinedTest() throws SMGInconsistentException {
    smg1.addValue(value1);
    smg2.addValue(value2);
    smgDest.addValue(value3);

    mapping1.map(value1, value3);
    mapping2.map(value2, value3);

    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            smgDest,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            false,
            0,
            0,
            0,
            dummyState,
            dummyState);
    assertThat(jv.isDefined()).isTrue();
    assertThat(jv.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jv.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jv.getInputSMG2()).isSameInstanceAs(smg2);
    assertThat(jv.getDestinationSMG()).isSameInstanceAs(smgDest);
    assertThat(jv.mapping1).isSameInstanceAs(mapping1);
    assertThat(jv.mapping2).isSameInstanceAs(mapping2);
    assertThat(jv.getValue()).isEqualTo(value3);
  }

  @Test
  public void joinValuesNonPointers() throws SMGInconsistentException {
    smg1.addValue(value1);
    smg2.addValue(value2);
    smgDest.addValue(value3);

    mapping1.map(value1, value3);
    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            smgDest,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            false,
            0,
            0,
            0,
            dummyState,
            dummyState);
    assertThat(jv.isDefined()).isFalse();

    mapping1 = new SMGNodeMapping();
    mapping2.map(value2, value3);
    jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            smgDest,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            false,
            0,
            0,
            0,
            dummyState,
            dummyState);
    assertThat(jv.isDefined()).isFalse();

    mapping2 = new SMGNodeMapping();

    jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            smgDest,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            false,
            0,
            0,
            0,
            dummyState,
            dummyState);
    assertThat(jv.isDefined()).isTrue();
    assertThat(jv.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jv.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jv.getInputSMG2()).isSameInstanceAs(smg2);
    assertThat(jv.getDestinationSMG()).isSameInstanceAs(smgDest);
    assertThat(jv.mapping1).isSameInstanceAs(mapping1);
    assertThat(jv.mapping2).isSameInstanceAs(mapping2);
    assertThat(jv.getValue()).isNotEqualTo(value1);
    assertThat(jv.getValue()).isNotEqualTo(value2);
    assertThat(jv.getValue()).isNotEqualTo(value3);
    assertThat(mapping1.get(value1)).isEqualTo(jv.getValue());
    assertThat(mapping2.get(value2)).isEqualTo(jv.getValue());
  }

  @Test
  public void joinValuesSinglePointer() throws SMGInconsistentException {
    smg1.addValue(value1);
    smg2.addValue(value2);
    smgDest.addValue(value3);

    SMGRegion obj1 = new SMGRegion(64, "Object");
    SMGEdgePointsTo pt = new SMGEdgePointsTo(value1, obj1, 0);
    smg1.addPointsToEdge(pt);
    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            smgDest,
            mapping1,
            mapping2,
            SMGLevelMapping.createDefaultLevelMap(),
            value1,
            value2,
            0,
            false,
            0,
            0,
            0,
            dummyState,
            dummyState);
    assertThat(jv.isDefined()).isFalse();
  }
}

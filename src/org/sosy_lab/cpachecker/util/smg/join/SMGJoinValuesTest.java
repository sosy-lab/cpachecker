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

// TODO this class misses a test case for two pointers
public class SMGJoinValuesTest extends SMGJoinTest0 {
  private SMG smg1;
  private SMG smg2;
  private SMG smgDest;

  private NodeMapping mapping1;
  private NodeMapping mapping2;

  private final SMGValue value1 = createValue();
  private final SMGValue value2 = createValue();
  private final SMGValue value3 = createValue();

  @Before
  public void setUp() {
    smg1 = new SMG(mockType8bSize);
    smg2 = new SMG(mockType8bSize);
    smgDest = new SMG(mockType8bSize);

    mapping1 = new NodeMapping();
    mapping2 = new NodeMapping();
  }

  @Test
  public void joinValuesIdenticalTest() {
    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value1);

    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL, smg1, smg2, smgDest, mapping1, mapping2, value1, value1, 0);
    assertThat(jv.isDefined()).isTrue();
    assertThat(SMGJoinStatus.EQUAL).isEqualTo(jv.getStatus());
    assertThat(jv.getMapping1()).isEqualTo(new NodeMapping());
    assertThat(jv.getMapping2()).isEqualTo(new NodeMapping());
    assertThat(value1).isEqualTo(jv.getValue());
  }

  @Test
  public void joinValuesAlreadyJoinedTest() {
    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value2);
    smgDest = smgDest.copyAndAddValue(value3);

    mapping1.addMapping(value1, value3);
    mapping2.addMapping(value2, value3);

    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL, smg1, smg2, smgDest, mapping1, mapping2, value1, value2, 0);
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
  public void joinValuesNonPointers() {
    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value2);
    smgDest = smgDest.copyAndAddValue(value3);

    mapping1.addMapping(value1, value3);
    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL, smg1, smg2, smgDest, mapping1, mapping2, value1, value2, 0);
    assertThat(jv.isDefined()).isFalse();

    mapping1 = new NodeMapping();
    mapping2.addMapping(value2, value3);
    jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL, smg1, smg2, smgDest, mapping1, mapping2, value1, value2, 0);
    assertThat(jv.isDefined()).isFalse();

    mapping2 = new NodeMapping();

    jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL, smg1, smg2, smgDest, mapping1, mapping2, value1, value2, 0);
    assertThat(jv.isDefined()).isTrue();
    assertThat(jv.getStatus()).isEqualTo(SMGJoinStatus.EQUAL);
    assertThat(jv.getInputSMG1()).isSameInstanceAs(smg1);
    assertThat(jv.getInputSMG2()).isSameInstanceAs(smg2);
    assertThat(jv.mapping1).isSameInstanceAs(mapping1);
    assertThat(jv.mapping2).isSameInstanceAs(mapping2);
    assertThat(jv.getValue()).isNotEqualTo(value1);
    assertThat(jv.getValue()).isNotEqualTo(value2);
    assertThat(jv.getValue()).isNotEqualTo(value3);
    assertThat(mapping1.getMappedValue(value1)).isEqualTo(jv.getValue());
    assertThat(mapping2.getMappedValue(value2)).isEqualTo(jv.getValue());
  }

  @Test
  public void joinValuesSinglePointer() {
    smg1 = smg1.copyAndAddValue(value1);
    smg2 = smg2.copyAndAddValue(value2);
    smgDest = smgDest.copyAndAddValue(value3);

    SMGObject obj1 = createRegion(64);
    SMGPointsToEdge pt = createPTRegionEdge(0, obj1);
    smg1 = smg1.copyAndAddPTEdge(pt, value1);
    SMGJoinValues jv =
        new SMGJoinValues(
            SMGJoinStatus.EQUAL, smg1, smg2, smgDest, mapping1, mapping2, value1, value2, 0);
    assertThat(jv.isDefined()).isFalse();
  }
}

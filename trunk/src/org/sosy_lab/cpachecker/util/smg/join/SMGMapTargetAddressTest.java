// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import static com.google.common.truth.Truth.assertThat;

import java.math.BigInteger;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

public class SMGMapTargetAddressTest extends SMGJoinTest0 {

  private SMG smg1;
  private SMG destSMG;
  private NodeMapping mapping1;
  private NodeMapping mapping2;

  final SMGObject obj1 = createRegion(64);
  final SMGValue value1 = createValue();
  final SMGPointsToEdge edge1 = createPTRegionEdge(0, obj1);

  final SMGValue value2 = createValue();

  final SMGObject destObj = createRegion(64);
  final SMGValue destValue = createValue();

  @Before
  public void setUp() {
    smg1 = new SMG(mockType8bSize);
    destSMG = new SMG(mockType8bSize);
    mapping1 = new NodeMapping();
    mapping2 = new NodeMapping();
  }

  @Test
  public void mapTargetAddressExistingNull() {
    SMG origDestSMG = destSMG;
    NodeMapping origMapping1 = cloneMapping(mapping1);

    SMGMapTargetAddress mta =
        new SMGMapTargetAddress(
            SMGJoinStatus.EQUAL,
            smg1,
            smg1,
            destSMG,
            mapping1,
            mapping1,
            SMGValue.zeroValue(),
            SMGValue.zeroValue());
    assertThat(mta.getDestinationSMG()).isEqualTo(origDestSMG);
    assertThat(mta.mapping1).isEqualTo(origMapping1);
    assertThat(mta.getValue()).isSameInstanceAs(SMGValue.zeroValue());
  }

  @Test
  public void mapTargetAddressExisting() {
    SMGPointsToEdge destEdge = createPTRegionEdge(0, destObj);
    smg1 = smg1.copyAndAddValue(value1);
    smg1 = smg1.copyAndAddObject(obj1);
    smg1 = smg1.copyAndAddPTEdge(edge1, value1);

    destSMG = destSMG.copyAndAddValue(destValue);
    destSMG = destSMG.copyAndAddObject(destObj);
    destSMG = destSMG.copyAndAddPTEdge(destEdge, destValue);

    mapping1.addMapping(obj1, destObj);

    NodeMapping origMapping1 = cloneMapping(mapping1);

    SMGMapTargetAddress mta =
        new SMGMapTargetAddress(
            SMGJoinStatus.EQUAL, smg1, smg1, destSMG, mapping1, mapping1, value1, value1);
    assertThat(mta.getDestinationSMG()).isEqualTo(destSMG);
    assertThat(mta.mapping1).isEqualTo(origMapping1);
    assertThat(mta.getValue()).isSameInstanceAs(destValue);
  }

  @Test
  public void mapTargetAddressNew() {
    smg1 = smg1.copyAndAddValue(value1);
    smg1 = smg1.copyAndAddObject(obj1);
    smg1 = smg1.copyAndAddPTEdge(edge1, value1);

    destSMG = destSMG.copyAndAddObject(destObj);

    mapping1.addMapping(obj1, destObj);

    NodeMapping origMapping1 = cloneMapping(mapping1);
    NodeMapping origMapping2 = cloneMapping(mapping2);

    SMGMapTargetAddress mta =
        new SMGMapTargetAddress(
            SMGJoinStatus.EQUAL, smg1, smg1, destSMG, mapping1, mapping2, value1, value2);
    assertThat(mta.getDestinationSMG()).isNotEqualTo(destSMG);
    assertThat(mta.mapping1).isNotEqualTo(origMapping1);
    assertThat(mta.mapping2).isNotEqualTo(origMapping2);

    assertThat(destSMG.getValues().contains(mta.getValue())).isFalse();

    SMG newDestSmg = mta.getDestinationSMG();
    SMGPointsToEdge newEdge = newDestSmg.getPTEdge(mta.getValue()).orElseThrow();

    assertThat(mta.mapping1.getMappedValue(value1)).isSameInstanceAs(mta.getValue());
    assertThat(mta.mapping2.getMappedValue(value2)).isSameInstanceAs(mta.getValue());

    assertThat(newEdge.pointsTo()).isSameInstanceAs(destObj);
    assertThat(newEdge.getOffset()).isEqualTo(BigInteger.ZERO);
  }
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;

public class SMGJoinMapTargetAddressTest {

  private SMG smg1;
  private SMG destSMG;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  final SMGRegion obj1 = new SMGRegion(64, "ze label");
  final SMGValue value1 = SMGKnownSymValue.of();
  final SMGEdgePointsTo edge1 = new SMGEdgePointsTo(value1, obj1, 0);

  final SMGValue value2 = SMGKnownSymValue.of();

  final SMGObject destObj = new SMGRegion(64, "destination");
  final SMGValue destValue = SMGKnownSymValue.of();

  @Before
  public void setUp() {
    smg1 = new SMG(MachineModel.LINUX64);
    destSMG = new SMG(MachineModel.LINUX64);
    mapping1 = new SMGNodeMapping();
    mapping2 = new SMGNodeMapping();
  }

  @Test
  public void mapTargetAddressExistingNull() {
    UnmodifiableSMG origDestSMG = destSMG.copyOf();
    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(smg1, smg1, destSMG, mapping1, mapping1, SMGZeroValue.INSTANCE, SMGZeroValue.INSTANCE);
    assertThat(mta.getSMG()).isEqualTo(origDestSMG);
    assertThat(mta.mapping1).isEqualTo(origMapping1);
    assertThat(mta.getValue()).isSameInstanceAs(SMGZeroValue.INSTANCE);
  }

  @Test
  public void mapTargetAddressExisting() {
    SMGEdgePointsTo destEdge = new SMGEdgePointsTo(destValue, destObj, 0);

    smg1.addValue(value1);
    smg1.addObject(obj1);
    smg1.addPointsToEdge(edge1);

    destSMG.addValue(destValue);
    destSMG.addObject(destObj);
    destSMG.addPointsToEdge(destEdge);

    mapping1.map(obj1, destObj);

    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);
    UnmodifiableSMG origDestSMG = destSMG.copyOf();

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(smg1, smg1, destSMG, mapping1, mapping1, value1, value1);
    assertThat(mta.getSMG()).isEqualTo(origDestSMG);
    assertThat(mta.mapping1).isEqualTo(origMapping1);
    assertThat(mta.getValue()).isSameInstanceAs(destValue);
  }

  @Test
  public void mapTargetAddressNew() {
    smg1.addValue(value1);
    smg1.addObject(obj1);
    smg1.addPointsToEdge(edge1);

    destSMG.addObject(destObj);

    mapping1.map(obj1, destObj);

    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);
    SMGNodeMapping origMapping2 = new SMGNodeMapping(mapping2);
    UnmodifiableSMG origDestSMG = destSMG.copyOf();

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(smg1, smg1, destSMG, mapping1, mapping2, value1, value2);
    assertThat(mta.getSMG()).isNotEqualTo(origDestSMG);
    assertThat(mta.mapping1).isNotEqualTo(origMapping1);
    assertThat(mta.mapping2).isNotEqualTo(origMapping2);

    assertThat(origDestSMG.getValues().contains(mta.getValue())).isFalse();

    SMGEdgePointsTo newEdge = destSMG.getPointer(mta.getValue());
    assertThat(newEdge.getObject()).isSameInstanceAs(destObj);
    assertThat(newEdge.getOffset()).isEqualTo(0);

    assertThat(mta.mapping1.get(value1)).isSameInstanceAs(mta.getValue());
    assertThat(mta.mapping2.get(value2)).isSameInstanceAs(mta.getValue());
  }
}

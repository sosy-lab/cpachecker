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
package org.sosy_lab.cpachecker.cpa.smg.join;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.ReadableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.WritableSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGJoinMapTargetAddressTest {

  private WritableSMG smg1;
  private WritableSMG destSMG;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  final SMGRegion obj1 = new SMGRegion(8, "ze label");
  final Integer value1 = SMGValueFactory.getNewValue();
  final SMGEdgePointsTo edge1 = new SMGEdgePointsTo(value1, obj1, 0);

  final Integer value2 = SMGValueFactory.getNewValue();

  final SMGObject destObj = new SMGRegion(8, "destination");
  final Integer destValue = SMGValueFactory.getNewValue();

  @Before
  public void setUp() {
    smg1 = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    destSMG = SMGFactory.createWritableSMG(MachineModel.LINUX64);
    mapping1 = new SMGNodeMapping();
    mapping2 = new SMGNodeMapping();
  }

  @Test
  @Ignore
  public void mapTargetAddressExistingNull() throws SMGInconsistentException {
    ReadableSMG origDestSMG = SMGFactory.createWritableCopy(destSMG);
    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(smg1, null, destSMG, mapping1, null, smg1.getNullValue(), null);
    Assert.assertEquals(origDestSMG, mta.getSMG());
    Assert.assertEquals(origMapping1, mta.getMapping1());
    Assert.assertNull(mta.getMapping2());
    Assert.assertSame(destSMG.getNullValue(), mta.getValue());
  }

  @Test
  @Ignore
  public void mapTargetAddressExisting() throws SMGInconsistentException {
    SMGEdgePointsTo destEdge = new SMGEdgePointsTo(destValue, destObj, 0);

    smg1.addValue(value1);
    smg1.addHeapObject(obj1);
    smg1.addPointsToEdge(edge1);

    destSMG.addValue(destValue);
    destSMG.addHeapObject(destObj);
    destSMG.addPointsToEdge(destEdge);

    mapping1.map(obj1, destObj);

    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);
    ReadableSMG origDestSMG = SMGFactory.createWritableCopy(destSMG);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(smg1, null, destSMG, mapping1, null, value1, null);
    Assert.assertEquals(origDestSMG, mta.getSMG());
    Assert.assertEquals(origMapping1, mta.getMapping1());
    Assert.assertNull(mta.getMapping2());
    Assert.assertSame(destValue, mta.getValue());
  }

  @Test
  public void mapTargetAddressNew() throws SMGInconsistentException {
    smg1.addValue(value1);
    smg1.addHeapObject(obj1);
    smg1.addPointsToEdge(edge1);

    destSMG.addHeapObject(destObj);

    mapping1.map(obj1, destObj);

    SMGNodeMapping origMapping1 = new SMGNodeMapping(mapping1);
    SMGNodeMapping origMapping2 = new SMGNodeMapping(mapping2);
    ReadableSMG origDestSMG = SMGFactory.createWritableCopy(destSMG);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(smg1, null, destSMG, mapping1, mapping2, value1, value2);
    Assert.assertNotEquals(origDestSMG, mta.getSMG());
    Assert.assertNotEquals(origMapping1, mta.getMapping1());
    Assert.assertNotEquals(origMapping2, mta.getMapping2());

    Assert.assertFalse(origDestSMG.getValues().contains(mta.getValue()));

    SMGEdgePointsTo newEdge = destSMG.getPointer(mta.getValue());
    Assert.assertSame(destObj, newEdge.getObject());
    Assert.assertEquals(0, newEdge.getOffset());

    Assert.assertSame(mta.getValue(), mta.getMapping1().get(value1));
    Assert.assertSame(mta.getValue(), mta.getMapping2().get(value2));
  }
}

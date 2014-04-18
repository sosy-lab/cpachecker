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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smgfork.SMG;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGRegion;


public class SMGJoinTargetObjectsTest {
  private SMG smg1;
  private SMG smg2;
  private SMG destSMG;

  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  final private SMGObject obj1 = new SMGRegion(8, "ze label");
  final private Integer value1 = SMGValueFactory.getNewValue();
  final private SMGEdgePointsTo pt1 = new SMGEdgePointsTo(value1, obj1, 0);

  final private SMGObject obj2 = new SMGRegion(8, "ze label");
  final private Integer value2 = SMGValueFactory.getNewValue();
  final private SMGEdgePointsTo pt2 = new SMGEdgePointsTo(value2, obj2, 0);

  final private SMGObject destObj = new SMGRegion(8, "destination");

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

    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2);
    Assert.assertSame(jto.getMapping1().get(obj1), jto.getMapping2().get(obj2));
    Assert.assertNotSame(jto.getMapping1().get(obj1), obj1);
    Assert.assertTrue(((SMGRegion)jto.getMapping1().get(obj1)).propertiesEqual((SMGRegion)obj1));
  }

  @Test(expected=UnsupportedOperationException.class)
  public void matchingObjectsWithMappingTest() throws SMGInconsistentException {
    smg1.addObject(obj1);
    smg1.addValue(value1);
    smg1.addPointsToEdge(pt1);

    smg2.addObject(obj2);
    smg2.addValue(value2);
    smg2.addPointsToEdge(pt2);

    destSMG.addObject(destObj);
    mapping1.map(obj1, destObj);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, obj1, obj2);
    Assert.assertTrue(mo.isDefined());

    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(mo.getStatus(), smg1, smg2, destSMG, mapping1, mapping2, value1, value2);
    jto.getStatus(); // Avoid dead store warning
  }

  @Test
  public void nonMatchingObjectsTest() throws SMGInconsistentException {
    smg1.addObject(obj1);
    smg1.addValue(value1);
    smg1.addPointsToEdge(pt1);

    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(SMGJoinStatus.EQUAL, smg1, smg2, mapping1, mapping2, obj1, smg2.getNullObject());
    Assert.assertFalse(mo.isDefined());
    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, smg2.getNullValue());
    Assert.assertFalse(jto.isDefined());
    Assert.assertTrue(jto.isRecoverable());
  }

  @Test
  public void joinTargetObjectsDifferentOffsets() throws SMGInconsistentException {
    SMGEdgePointsTo pt1null = new SMGEdgePointsTo(value1, smg1.getNullObject(), 2);
    SMGEdgePointsTo pt2null = new SMGEdgePointsTo(value2, smg2.getNullObject(), 1);

    smg1.addObject(obj1);
    smg1.addValue(value1);
    smg1.addPointsToEdge(pt1null);

    smg2.addObject(obj2);
    smg2.addValue(value2);
    smg2.addPointsToEdge(pt2null);

    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(SMGJoinStatus.EQUAL, smg1, smg2, null, null, null, value1, value2);

    Assert.assertFalse(jto.isDefined());
    Assert.assertTrue(jto.isRecoverable());
  }

  @Test
  public void joinTargetObjectsAlreadyJoinedNull() throws SMGInconsistentException {
    SMGEdgePointsTo pt1null = new SMGEdgePointsTo(value1, smg1.getNullObject(), 0);
    SMGEdgePointsTo pt2null = new SMGEdgePointsTo(value2, smg2.getNullObject(), 0);

    smg1.addValue(value1);
    smg2.addValue(value2);

    smg1.addPointsToEdge(pt1null);
    smg2.addPointsToEdge(pt2null);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(new SMG(smg1), new SMG(smg2), new SMG(destSMG),
                                                      new SMGNodeMapping(mapping1), new SMGNodeMapping(mapping2),
                                                      value1, value2);
    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2);
    Assert.assertTrue(jto.isDefined());
    Assert.assertEquals(SMGJoinStatus.EQUAL, jto.getStatus());
    Assert.assertSame(smg1, jto.getInputSMG1());
    Assert.assertSame(smg2, jto.getInputSMG2());
    Assert.assertEquals(mta.getSMG(), jto.getDestinationSMG());
    Assert.assertEquals(mta.getMapping1(), jto.getMapping1());
    Assert.assertEquals(mta.getMapping2(), jto.getMapping2());
    Assert.assertEquals(mta.getValue(), jto.getValue());
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
    // SMGMapTargetAddress mta = new SMGMapTargetAddress(new SMG(smg1), new SMG(smg2), new SMG(destSMG),
    //                                                  new SMGNodeMapping(mapping1), new SMGNodeMapping(mapping2),
    //                                                  value1, value2);
    SMGJoinTargetObjects jto = new SMGJoinTargetObjects(SMGJoinStatus.EQUAL, smg1, smg2, destSMG, mapping1, mapping2, value1, value2);
    Assert.assertTrue(jto.isDefined());
    Assert.assertEquals(SMGJoinStatus.EQUAL, jto.getStatus());
    Assert.assertSame(smg1, jto.getInputSMG1());
    Assert.assertSame(smg2, jto.getInputSMG2());
    // TODO: Not equal, but isomorphic (newly created values differ in mta and jto)
    //       But we currently do not have isomorphism
    //Assert.assertEquals(mta.getSMG(), jto.getDestinationSMG());

    Assert.assertTrue(jto.getMapping1().containsKey(value1));
    Assert.assertEquals(jto.getMapping1().get(value1), jto.getValue());

    Assert.assertTrue(jto.getMapping2().containsKey(value2));
    Assert.assertEquals(jto.getMapping2().get(value2), jto.getValue());
  }
}

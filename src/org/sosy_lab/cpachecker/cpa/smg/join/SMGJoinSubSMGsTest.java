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
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;


public class SMGJoinSubSMGsTest {

  SMGJoinSubSMGs jssDefined;

  @Before
  public void setUp() throws SMGInconsistentException {
    SMG smg1 = new SMG(MachineModel.LINUX64);
    SMG smg2 = new SMG(MachineModel.LINUX64);
    SMG destSmg = new SMG(MachineModel.LINUX64);

    SMGObject obj1 = new SMGRegion(64, "Test object 1");
    SMGObject obj2 = new SMGRegion(64, "Test object 2");

    smg1.addObject(obj1);
    smg2.addObject(obj2);

    SMGNodeMapping mapping1 = new SMGNodeMapping();
    SMGNodeMapping mapping2 = new SMGNodeMapping();

    SMGLevelMapping levelMapping = SMGLevelMapping.createDefaultLevelMap();
    jssDefined = new SMGJoinSubSMGs(SMGJoinStatus.EQUAL, smg1, smg2, destSmg, mapping1, mapping2, levelMapping, obj1, obj2, null, 0, false, null, null);
  }

  @Test
  public void testIsDefined() {
    Assert.assertTrue(jssDefined.isDefined());
  }

  @Test
  public void testGetStatusOnDefined() {
    Assert.assertNotNull(jssDefined.getStatus());
  }

  @Test
  public void testGetSMG1() {
    Assert.assertNotNull(jssDefined.getSMG1());
  }

  @Test
  public void testGetSMG2() {
    Assert.assertNotNull(jssDefined.getSMG2());
  }

  @Test
  public void testGetDestSMG() {
    Assert.assertNotNull(jssDefined.getDestSMG());
  }

  @Test
  public void testGetMapping1() {
    Assert.assertNotNull(jssDefined.getMapping1());
  }

  @Test
  public void testGetMapping2() {
    Assert.assertNotNull(jssDefined.getMapping2());
  }
}

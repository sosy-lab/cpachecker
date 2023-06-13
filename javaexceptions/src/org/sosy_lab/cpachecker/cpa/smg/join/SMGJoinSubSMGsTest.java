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
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;

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
    jssDefined =
        new SMGJoinSubSMGs(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSmg,
            mapping1,
            mapping2,
            levelMapping,
            obj1,
            obj2,
            null,
            0,
            false,
            null,
            null);
  }

  @Test
  public void testIsDefined() {
    assertThat(jssDefined.isDefined()).isTrue();
  }

  @Test
  public void testGetStatusOnDefined() {
    assertThat(jssDefined.getStatus()).isNotNull();
  }

  @Test
  public void testGetSMG1() {
    assertThat(jssDefined.getSMG1()).isNotNull();
  }

  @Test
  public void testGetSMG2() {
    assertThat(jssDefined.getSMG2()).isNotNull();
  }

  @Test
  public void testGetDestSMG() {
    assertThat(jssDefined.getDestSMG()).isNotNull();
  }
}

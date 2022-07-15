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

public class SMGJoinSubSMGsTest extends SMGJoinTest0 {

  SMGJoinSubSMGs jssDefined;

  @Before
  public void setUp() {

    SMGObject obj1 = createRegion(64);
    SMGObject obj2 = createRegion(64);

    SMG smg1 = new SMG(mockType4bSize).copyAndAddObject(obj1);
    SMG smg2 = new SMG(mockType4bSize).copyAndAddObject(obj2);
    SMG destSmg = new SMG(mockType4bSize);

    NodeMapping mapping1 = new NodeMapping();
    NodeMapping mapping2 = new NodeMapping();

    jssDefined =
        new SMGJoinSubSMGs(
            SMGJoinStatus.EQUAL,
            smg1,
            smg2,
            destSmg,
            mapping1,
            mapping2,
            obj1,
            obj2,
            createRegion(mockType4bSize),
            0);
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
    assertThat(jssDefined.getInputSMG1()).isNotNull();
  }

  @Test
  public void testGetSMG2() {
    assertThat(jssDefined.getInputSMG2()).isNotNull();
  }

  @Test
  public void testGetDestSMG() {
    assertThat(jssDefined.getDestinationSMG()).isNotNull();
  }
}

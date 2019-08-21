/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;

public class SMGRegionTest {

  @Before
  public void setUp() {}

  @Test
  public void testIsAbstract() {
    SMGRegion region = new SMGRegion(64, "region");
    assertThat(region.isAbstract()).isFalse();
  }

  @Test
  public void testJoin() {
    SMGRegion region = new SMGRegion(64, "region");
    SMGRegion region_same = new SMGRegion(64, "region");
    SMGObject objectJoint = region.join(region_same, region_same.getLevel());
    assertThat(objectJoint).isInstanceOf(SMGRegion.class);
    SMGRegion regionJoint = (SMGRegion)objectJoint;

    assertThat(regionJoint.getSize()).isEqualTo(64);
    assertThat(regionJoint.getLabel()).isEqualTo("region");
  }

  @Test(expected=UnsupportedOperationException.class)
  public void testJoinDiffSize() {
    SMGRegion region = new SMGRegion(64, "region");
    SMGRegion regionDiff = new SMGRegion(80, "region");
    region.join(regionDiff, regionDiff.getLevel());
  }

  @Test
  public void testPropertiesEqual() {
    SMGRegion one = new SMGRegion(64, "region");
    SMGRegion two = new SMGRegion(64, "region");
    SMGRegion three = new SMGRegion(80, "region");
    SMGRegion four = new SMGRegion(64, "REGION");

    assertThat(two.getSize()).isEqualTo(one.getSize());
    assertThat(three.getSize()).isNotEqualTo(one.getSize());
    assertThat(four.getSize()).isEqualTo(one.getSize());
    assertThat(two.getLabel()).isEqualTo(one.getLabel());
    assertThat(three.getLabel()).isEqualTo(one.getLabel());
    assertThat(four.getLabel()).isNotEqualTo(one.getLabel());
  }

}

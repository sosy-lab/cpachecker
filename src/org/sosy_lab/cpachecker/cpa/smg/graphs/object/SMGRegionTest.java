// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
    SMGRegion regionJoint = (SMGRegion) objectJoint;

    assertThat(regionJoint.getSize()).isEqualTo(64);
    assertThat(regionJoint.getLabel()).isEqualTo("region");
  }

  @Test(expected = UnsupportedOperationException.class)
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

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.mpa;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.core.defaults.NamedProperty;
import org.sosy_lab.cpachecker.core.interfaces.Property;

import com.google.common.collect.ImmutableSet;


public class PartitioningDefaultOperatorTest {

  private PartitioningDefaultOperator op;

  private Property p1;
  private Property p2;
  private Property p3;
  private Property p4;
  private Property p5;

  @Before
  public void setUp() throws Exception {
    op = new PartitioningDefaultOperator();

    p1 = NamedProperty.create("p1");
    p2 = NamedProperty.create("p2");
    p3 = NamedProperty.create("p3");
    p4 = NamedProperty.create("p4");
    p5 = NamedProperty.create("p5");
  }

  @Test
  public void testBisectPartitons() {

    ImmutableSet<ImmutableSet<Property>> toPartition = ImmutableSet.of(
        ImmutableSet.of(p1,p2,p3),
        ImmutableSet.of(p4,p5));

    ImmutableSet<ImmutableSet<Property>> result = op.bisectPartitons(toPartition);

    Assert.assertNotEquals(toPartition, result);
    Assert.assertEquals(4, result.size());
    Assert.assertTrue(result.contains(ImmutableSet.of(p4)));
    Assert.assertTrue(result.contains(ImmutableSet.of(p5)));
  }

  @Test
  public void testPartitoning_Case1() {

    final ImmutableSet<ImmutableSet<Property>> lastChecked = ImmutableSet.of(
        ImmutableSet.of(p1,p2,p3),
        ImmutableSet.of(p4,p5));

    final ImmutableSet<Property> all = ImmutableSet.of(p1,p2,p3,p4,p5);

    ImmutableSet<ImmutableSet<Property>> result = op.partition(lastChecked, all);

    Assert.assertNotEquals(lastChecked, result);
    Assert.assertEquals(4, result.size());
  }

  @Test
  public void testPartitoning_Case2() {

    final ImmutableSet<ImmutableSet<Property>> lastChecked = ImmutableSet.of(
        ImmutableSet.of(p1,p2,p3,p4,p5));

    final ImmutableSet<Property> all = ImmutableSet.of(p1,p2,p3,p4,p5);

    ImmutableSet<ImmutableSet<Property>> result = op.partition(lastChecked, all);

    Assert.assertNotEquals(lastChecked, result);
    Assert.assertEquals(2, result.size());
    Assert.assertTrue(result.contains(ImmutableSet.of(p1,p2,p3)));
  }

}

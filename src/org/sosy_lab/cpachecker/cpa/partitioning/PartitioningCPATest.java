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
package org.sosy_lab.cpachecker.cpa.partitioning;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAchecker.InitialStatesFor;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class PartitioningCPATest {

  private PartitioningCPA cpa;
  private AbstractDomain domain;
  private CFANode DUMMY_CFA_NODE = new CFANode("DUMMY_CFA_NODE");

  @Before
  public void setUp() {
    cpa = new PartitioningCPA();
    domain = cpa.getAbstractDomain();
  }

  @Test
  public void testIsLessOrEqual_EqualPartition() throws CPAException, InterruptedException {
    AbstractState p1 = cpa.getInitialState(
        DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 = cpa.getInitialState(
        DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    assertThat(p1).isEqualTo(p2);

    assertThat(domain.isLessOrEqual(p1, p2)).isTrue();
  }

  @Test
  public void testMerge_EqualPartition() throws CPAException, InterruptedException {
    AbstractState p1 = cpa.getInitialState(
        DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 = cpa.getInitialState(
        DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState mergeResult = cpa.getMergeOperator().merge(p1, p2, SingletonPrecision.getInstance());

    assertThat(mergeResult).isEqualTo(p2); // MERGE-SEP
  }

  @Test
  public void testIsLessOrEqual_DifferentPartitions() throws CPAException, InterruptedException {
    AbstractState p1 = cpa.getInitialState(
        DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 = cpa.getInitialState(
        DUMMY_CFA_NODE,
        StateSpacePartition.getPartitionWithKey(InitialStatesFor.EXIT));

    assertThat(p1).isNotEqualTo(p2);

    assertThat(domain.isLessOrEqual(p1, p2)).isFalse();
  }

}

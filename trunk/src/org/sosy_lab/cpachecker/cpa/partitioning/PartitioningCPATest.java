// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
  private CFANode DUMMY_CFA_NODE = CFANode.newDummyCFANode();

  @Before
  public void setUp() {
    cpa = new PartitioningCPA();
    domain = cpa.getAbstractDomain();
  }

  @Test
  public void testIsLessOrEqual_EqualPartition() throws CPAException, InterruptedException {
    AbstractState p1 =
        cpa.getInitialState(
            DUMMY_CFA_NODE, StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 =
        cpa.getInitialState(
            DUMMY_CFA_NODE, StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    assertThat(p1).isEqualTo(p2);

    assertThat(domain.isLessOrEqual(p1, p2)).isTrue();
  }

  @Test
  public void testMerge_EqualPartition() throws CPAException, InterruptedException {
    AbstractState p1 =
        cpa.getInitialState(
            DUMMY_CFA_NODE, StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 =
        cpa.getInitialState(
            DUMMY_CFA_NODE, StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState mergeResult =
        cpa.getMergeOperator().merge(p1, p2, SingletonPrecision.getInstance());

    assertThat(mergeResult).isEqualTo(p2); // MERGE-SEP
  }

  @Test
  public void testIsLessOrEqual_DifferentPartitions() throws CPAException, InterruptedException {
    AbstractState p1 =
        cpa.getInitialState(
            DUMMY_CFA_NODE, StateSpacePartition.getPartitionWithKey(InitialStatesFor.ENTRY));

    AbstractState p2 =
        cpa.getInitialState(
            DUMMY_CFA_NODE, StateSpacePartition.getPartitionWithKey(InitialStatesFor.EXIT));

    assertThat(p1).isNotEqualTo(p2);

    assertThat(domain.isLessOrEqual(p1, p2)).isFalse();
  }
}

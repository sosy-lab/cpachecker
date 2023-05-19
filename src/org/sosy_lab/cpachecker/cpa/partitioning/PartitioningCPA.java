// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.partitioning;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.IdentityTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * CPA for partitioning the state space of an analysis; one set of reached states can be used to
 * analyze disjoint partitions of the state space.
 */
public class PartitioningCPA extends AbstractCPA {

  protected PartitioningCPA() {
    super("JOIN", "JOIN", IdentityTransferRelation.INSTANCE);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PartitioningCPA.class);
  }

  /** The abstract state of the PartitioningCPA */
  public static class PartitionState implements AbstractState {
    private final StateSpacePartition partition;

    public PartitionState(StateSpacePartition pPartition) {
      partition = pPartition;
    }

    public StateSpacePartition getStateSpacePartition() {
      return partition;
    }

    @Override
    public String toString() {
      if (partition == null) {
        return "PARTITION [NULL]";
      } else {
        return "PARTITION " + partition;
      }
    }

    @Override
    public int hashCode() {
      return partition.hashCode();
    }

    @Override
    public boolean equals(Object pObj) {
      return pObj instanceof PartitionState && partition.equals(((PartitionState) pObj).partition);
    }
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new PartitionState(pPartition);
  }
}

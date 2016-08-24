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

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractCPA;
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.IdentityTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;

/**
 * CPA for partitioning the state space of an analysis;
 * one set of reached states can be used to analyze
 * disjoint partitions of the state space.
 */
public class PartitioningCPA extends AbstractCPA {

  protected PartitioningCPA() {
    super("JOIN", "JOIN", IdentityTransferRelation.INSTANCE);
  }

  public static CPAFactory factory() {
    return AutomaticCPAFactory.forType(PartitioningCPA.class);
  }

  /**
   * The abstract state of the PartitioningCPA
   */
  public static class PartitionState implements AbstractState {
    private final StateSpacePartition partition;

    public PartitionState(StateSpacePartition pPartition) {
      this.partition = pPartition;
    }

    public StateSpacePartition getStateSpacePartition() {
      return partition;
    }

    @Override
    public String toString() {
      if (partition == null) {
        return "PARTITION [NULL]";
      } else {
        return "PARTITION " + partition.toString();
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

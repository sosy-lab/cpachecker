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
import org.sosy_lab.cpachecker.core.defaults.AutomaticCPAFactory;
import org.sosy_lab.cpachecker.core.defaults.FlatLatticeDomain;
import org.sosy_lab.cpachecker.core.defaults.IdentityTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.MergeJoinOperator;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.defaults.StaticPrecisionAdjustment;
import org.sosy_lab.cpachecker.core.defaults.StopJoinOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

import com.google.common.base.Objects;

/**
 * CPA for partitioning the state space of an analysis;
 * one set of reached states can be used to analyze
 * disjoint partitions of the state space.
 */
public class PartitioningCPA implements ConfigurableProgramAnalysis {

  /**
   * The abstract domain of the PartitioningCPA is a flat lattice.
   * The elements of the lattice are the partitions.
   */
  private final AbstractDomain abstractDomain = new FlatLatticeDomain();
  private final StopOperator stopOperator = new StopJoinOperator(abstractDomain);
  private final MergeOperator mergeOperator = new MergeJoinOperator(abstractDomain);

  /**
   * The transfer relation keeps the partition of the predecessor state.
   * (the successor state is the predecessor state).
   *
   * The partition is determined by the initial state.
   */
  private final TransferRelation transferRelation = IdentityTransferRelation.INSTANCE;

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
      final int prime = 31;
      int result = 1;
      result = prime * result + Objects.hashCode(partition);
      return result;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof PartitionState)) {
        return false;
      }
      return Objects.equal(this.partition, ((PartitionState) pObj).partition);
    }
  }

  @Override
  public AbstractDomain getAbstractDomain() {
    return abstractDomain;
  }

  @Override
  public TransferRelation getTransferRelation() {
    return transferRelation;
  }

  @Override
  public MergeOperator getMergeOperator() {
    return mergeOperator;
  }

  @Override
  public StopOperator getStopOperator() {
    return stopOperator;
  }

  @Override
  public PrecisionAdjustment getPrecisionAdjustment() {
    return StaticPrecisionAdjustment.getInstance();
  }

  @Override
  public AbstractState getInitialState(CFANode pNode, StateSpacePartition pPartition) {
    return new PartitionState(pPartition);
  }

  @Override
  public Precision getInitialPrecision(CFANode pNode, StateSpacePartition pPartition) {
    return SingletonPrecision.getInstance();
  }

}

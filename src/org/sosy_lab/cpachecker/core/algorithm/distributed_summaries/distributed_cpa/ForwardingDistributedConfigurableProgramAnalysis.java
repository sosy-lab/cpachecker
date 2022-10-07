// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.interfaces.StopOperator;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

public interface ForwardingDistributedConfigurableProgramAnalysis
    extends DistributedConfigurableProgramAnalysis {

  @Override
  default TransferRelation getTransferRelation() {
    return getCPA().getTransferRelation();
  }

  @Override
  default AbstractDomain getAbstractDomain() {
    return getCPA().getAbstractDomain();
  }

  @Override
  default MergeOperator getMergeOperator() {
    return getCPA().getMergeOperator();
  }

  @Override
  default StopOperator getStopOperator() {
    return getCPA().getStopOperator();
  }

  @Override
  default AbstractState getInitialState(
      CFANode node, StateSpacePartition partition) throws InterruptedException {
    return getCPA().getInitialState(node, partition);
  }

}

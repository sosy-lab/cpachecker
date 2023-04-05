// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;

public class SerializeARGStateOperator implements SerializeOperator {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public SerializeARGStateOperator(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    return wrapped.getSerializeOperator().serialize(((ARGState) pState).getWrappedState());
  }
}

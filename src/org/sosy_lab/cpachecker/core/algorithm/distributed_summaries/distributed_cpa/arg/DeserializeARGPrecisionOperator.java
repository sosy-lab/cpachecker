// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class DeserializeARGPrecisionOperator implements DeserializePrecisionOperator {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public DeserializeARGPrecisionOperator(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public Precision deserializePrecision(BlockSummaryMessage pMessage) {
    return wrapped.getDeserializePrecisionOperator().deserializePrecision(pMessage);
  }
}

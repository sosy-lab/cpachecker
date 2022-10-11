// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

public class IdentitySerializeOperator implements SerializeOperator {

  private final Class<? extends ConfigurableProgramAnalysis> key;

  public IdentitySerializeOperator(final Class<? extends ConfigurableProgramAnalysis> pKey) {
    key = pKey;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    return BlockSummaryMessagePayload.builder().addEntry(key.getName(), pState).buildPayload();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.arg;

import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;

public class SerializeARGStateOperator implements SerializeOperator {

  private final DistributedConfigurableProgramAnalysis wrapped;

  public static final String COUNTEREXAMPLE_PATH = "counterexample_path";

  public SerializeARGStateOperator(DistributedConfigurableProgramAnalysis pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    ImmutableSet<CFANode> path = buildCounterExamplePath((ARGState) pState);
    BlockSummaryMessagePayload payload =
        wrapped.getSerializeOperator().serialize(((ARGState) pState).getWrappedState());
    if (!path.isEmpty()) {
      payload =
          BlockSummaryMessagePayload.builder()
              .addAllEntries(payload)
              .addEntry(COUNTEREXAMPLE_PATH, path)
              .buildPayload();
    }
    return payload;
  }

  private ImmutableSet<CFANode> buildCounterExamplePath(ARGState pState) {
    ImmutableSet.Builder<CFANode> builder = ImmutableSet.builder();
    Optional<CounterexampleInfo> info = pState.getCounterexampleInformation();
    if (info.isPresent()) {
      PathIterator iterator = info.get().getTargetPath().fullPathIterator();
      do {
        builder.add(iterator.getLocation());
      } while (iterator.advanceIfPossible());
    }
    return builder.build();
  }
}

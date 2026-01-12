// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants.SerializeDataflowAnalysisStateOperator.STRATEGY;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsPrecision;

public class SerializeInvariantsPrecisionOperator implements SerializePrecisionOperator {
  @Override
  public ImmutableMap<String, String> serializePrecision(Precision pPrecision) {
    return ContentBuilder.builder()
        .pushLevel(InvariantsPrecision.class.getName())
        .put(STRATEGY, ((InvariantsPrecision) pPrecision).getAbstractionStrategy().toString())
        .build();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySerializeUtil;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;

public class DeserializeVariableTrackingPrecision implements DeserializePrecisionOperator {

  private final ValueAnalysisCPA valueAnalysisCPA;

  public DeserializeVariableTrackingPrecision(ValueAnalysisCPA pValueAnalysisCPA) {
    valueAnalysisCPA = pValueAnalysisCPA;
  }

  @Override
  public Precision deserializePrecision(BlockSummaryMessage pMessage) {
    Optional<Object> precision = pMessage.getPrecision(VariableTrackingPrecision.class);
    if (precision.isEmpty()) {
      // location node is ignored anyway
      return valueAnalysisCPA.getInitialPrecision(
          CFANode.newDummyCFANode(), StateSpacePartition.getDefaultPartition());
    }
    return BlockSummarySerializeUtil.deserialize(
        (String) precision.orElseThrow(), VariableTrackingPrecision.class);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.io.IOException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySerializeUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;

public class SerializeValueAnalysisStateOperator implements SerializeOperator {

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    try {
      if (pState instanceof ValueAnalysisState valueState) {
        return BlockSummaryMessagePayload.builder()
            .addEntry(
                ValueAnalysisCPA.class.getName(), BlockSummarySerializeUtil.serialize(valueState))
            .addEntry("readable", pState.toString())
            .buildPayload();
      }
    } catch (IOException e) {
      throw new AssertionError("Value state must be serializable " + e);
    }
    throw new AssertionError("Cannot serialize a non value-state: " + pState.getClass());
  }
}

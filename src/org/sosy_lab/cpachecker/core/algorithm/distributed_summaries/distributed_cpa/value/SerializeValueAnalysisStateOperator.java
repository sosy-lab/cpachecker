// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.util.Map.Entry;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SerializeValueAnalysisStateOperator implements SerializeOperator {

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    ValueAnalysisState valueState = (ValueAnalysisState) pState;
    StringBuilder stringBuilder = new StringBuilder();
    for (Entry<MemoryLocation, ValueAndType> entry : valueState.getConstants()) {
      stringBuilder
          .append(entry.getKey().getIdentifier())
          .append(":")
          .append(entry.getValue().getType())
          .append("=")
          .append(entry.getValue().getValue().asNumericValue().number().intValue())
          .append(" && ");
    }
    String serializedValueString = stringBuilder.toString();
    if (serializedValueString.isEmpty()) {
      serializedValueString = "No constants";
    } else {
      serializedValueString =
          serializedValueString.substring(0, serializedValueString.length() - 4);
    }

    BlockSummaryMessagePayload.Builder payload =
        BlockSummaryMessagePayload.builder()
            .addEntry(ValueAnalysisCPA.class.getName(), serializedValueString);

    // Memory Location mitschicken

    // try {
    //   if (pState instanceof ValueAnalysisState valueAnalysisState) {
    //     return BlockSummaryMessagePayload.builder()
    //         .addEntry(
    //             ValueAnalysisCPA.class.getName(),
    // BlockSummarySerializeUtil.serialize(valueState))
    //         .addEntry("readable", pState.toString())
    //         .buildPayload();
    //   }
    // } catch (IOException e) {
    //   throw new AssertionError("Value state must be serializable " + e);
    // }
    // throw new AssertionError("Cannot serialize a non value-state: " + pState.getClass());
    return payload.buildPayload();
  }
}

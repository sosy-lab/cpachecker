// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.io.IOException;
import java.util.Map.Entry;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySerializeUtil;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class SerializeValueAnalysisStateOperator implements SerializeOperator {

  BlockNode currentBlockNode;

  public SerializeValueAnalysisStateOperator(BlockNode pCurrentBlockNode) {
    currentBlockNode = pCurrentBlockNode;
  }

  @Override
  public BlockSummaryMessagePayload serialize(AbstractState pState) {
    ValueAnalysisState valueState = (ValueAnalysisState) pState;
    String serializedValueString = "";
    for (Entry<MemoryLocation, ValueAndType> entry : valueState.getConstants()) {
      serializedValueString += 
          entry.getKey().getIdentifier()  + ":" + entry.getValue().getType() + "=" + entry.getValue().getValue().asNumericValue().number().intValue() + " && ";
    }
    if (serializedValueString == "") {
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
    //             ValueAnalysisCPA.class.getName(), BlockSummarySerializeUtil.serialize(valueState))
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

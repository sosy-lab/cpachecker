// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import java.io.IOException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummarySerializeUtil;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class SerializeVariableTrackingPrecision implements SerializePrecisionOperator {

  @Override
  public BlockSummaryMessagePayload serializePrecision(Precision pPrecision) {
    try {
      if (pPrecision instanceof VariableTrackingPrecision vtp) {
        return BlockSummaryMessagePayload.builder()
            .addEntry(
                VariableTrackingPrecision.class.getName(), BlockSummarySerializeUtil.serialize(vtp))
            .buildPayload();
      }
    } catch (IOException e) {
      throw new AssertionError("Serialization of ValueState is required", e);
    }
    throw new AssertionError("ValueAnalysis precision does not support " + pPrecision);
  }
}

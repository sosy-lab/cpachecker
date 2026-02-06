// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.FullConstraintsPrecision;

public class SerializeConstraintsPrecisionOperator implements SerializePrecisionOperator {
  public static final String DSS_MESSAGE_CONSTRAINTS_KEY = "constraints";

  public SerializeConstraintsPrecisionOperator() {}

  @Override
  public ImmutableMap<String, String> serializePrecision(Precision pPrecision) {
    ContentBuilder contentBuilder =
        ContentBuilder.builder().pushLevel(FullConstraintsPrecision.class.getName());
    contentBuilder.put(DSS_MESSAGE_CONSTRAINTS_KEY, "");
    return contentBuilder.build();
  }
}

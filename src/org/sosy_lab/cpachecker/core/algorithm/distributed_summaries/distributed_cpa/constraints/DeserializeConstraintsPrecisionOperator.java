// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.constraints;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.FullConstraintsPrecision;

public class DeserializeConstraintsPrecisionOperator implements DeserializePrecisionOperator {
  @Override
  public Precision deserializePrecision(DssMessage pMessage) {
    ContentReader contentReader = pMessage.getPrecisionContent(FullConstraintsPrecision.class);
    assert contentReader
        .get(SerializeConstraintsPrecisionOperator.DSS_MESSAGE_CONSTRAINTS_KEY)
        .isEmpty();
    return FullConstraintsPrecision.getInstance();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.collect.ImmutableMap;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.ScopedRefinablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class SerializeValuePrecisionOperator implements SerializePrecisionOperator {
  static final String RAW_PRECISION_KEY = "raw-precision";
  static final String PRECISION_TYPE_KEY = "precision-type";

  @Override
  public ImmutableMap<String, String> serializePrecision(Precision pPrecision) {
    Class<?> precisionType = pPrecision.getClass();
    String serializedRawPrecision = "";
    if (pPrecision instanceof ScopedRefinablePrecision scopedRefPrecision) {
      serializedRawPrecision =
          scopedRefPrecision.getRawPrecision().stream()
              .map(e -> e.getExtendedQualifiedName())
              .collect(Collectors.joining(","));

    } else if (!(pPrecision instanceof ConfigurablePrecision)) {
      throw new AssertionError(
          "Cannot serialize a precision that is not of type "
              + ScopedRefinablePrecision.class
              + "or"
              + ConfigurablePrecision.class
              + " (got: "
              + pPrecision.getClass()
              + ")");
    }

    return ContentBuilder.builder()
        .pushLevel(VariableTrackingPrecision.class.getName())
        .put(PRECISION_TYPE_KEY, precisionType.getName())
        .put(RAW_PRECISION_KEY, serializedRawPrecision)
        .build();
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.SerializeValuePrecisionOperator.PRECISION_TYPE_KEY;
import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value.SerializeValuePrecisionOperator.RAW_PRECISION_KEY;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.ScopedRefinablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisCPA;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public class DeserializeValuePrecisionOperator implements DeserializePrecisionOperator {
  private final Configuration config;
  private final Optional<VariableClassification> vC;

  public DeserializeValuePrecisionOperator(
      Configuration pConfig, Optional<VariableClassification> pVC) {
    config = pConfig;
    vC = pVC;
  }

  @Override
  public Precision deserializePrecision(DssMessage pMessage) {

    ContentReader precisionContent = pMessage.getPrecisionContent(VariableTrackingPrecision.class);
    String precisionType = precisionContent.get(PRECISION_TYPE_KEY);
    String serializedPrecision = precisionContent.get(RAW_PRECISION_KEY);
    Preconditions.checkNotNull(precisionType, "Value precision must be provided");

    VariableTrackingPrecision variableTrackingPrecision;
    try {
      variableTrackingPrecision =
          ConfigurablePrecision.createStaticPrecision(config, vC, ValueAnalysisCPA.class);
    } catch (InvalidConfigurationException e) {
      throw new AssertionError("Could not deserialize value precision");
    }

    if (precisionType.equals(ConfigurablePrecision.class.getName()))
      return variableTrackingPrecision;

    if (precisionType.equals(ScopedRefinablePrecision.class.getName())) {
      List<MemoryLocation> rawPrecision =
          Splitter.on(",")
              .splitToStream(serializedPrecision)
              .map(e -> MemoryLocation.fromQualifiedName(e))
              .toList();
      return new ScopedRefinablePrecision(variableTrackingPrecision, rawPrecision);
    }
    throw new AssertionError("Unsupported precision type");
  }
}

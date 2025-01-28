// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class SerializeCompositePrecisionOperator implements SerializePrecisionOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;

  public SerializeCompositePrecisionOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered) {
    registered = pRegistered;
  }

  @Override
  public DssMessagePayload serializePrecision(Precision pPrecision) {
    DssMessagePayload.Builder payload = DssMessagePayload.builder();
    try {
      CompositePrecision wrapped = (CompositePrecision) pPrecision;
      for (Precision wrappedPrecision : wrapped.getWrappedPrecisions()) {
        for (DistributedConfigurableProgramAnalysis value : registered.values()) {
          if (wrappedPrecision
              .getClass()
              .isAssignableFrom(
                  value
                      .getInitialPrecision(
                          CFANode.newDummyCFANode(), StateSpacePartition.getDefaultPartition())
                      .getClass())) {
            payload =
                payload.addAllEntries(
                    value.getSerializePrecisionOperator().serializePrecision(wrappedPrecision));
          }
        }
      }
      return payload.buildPayload();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }
}

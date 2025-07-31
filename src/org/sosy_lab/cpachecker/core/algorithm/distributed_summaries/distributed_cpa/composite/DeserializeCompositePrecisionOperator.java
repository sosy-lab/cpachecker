// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class DeserializeCompositePrecisionOperator implements DeserializePrecisionOperator {

  private final Map<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      registered;
  private final CompositeCPA compositeCPA;
  private final BlockNode node;

  public DeserializeCompositePrecisionOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      BlockNode pNode,
      CompositeCPA pCompositeCPA) {
    registered = pRegistered;
    node = pNode;
    compositeCPA = pCompositeCPA;
  }

  @Override
  public Precision deserializePrecision(DssMessage pMessage) {
    List<Precision> precisions = new ArrayList<>();
    for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
      if (registered.containsKey(wrappedCPA.getClass())) {
        DistributedConfigurableProgramAnalysis entry = registered.get(wrappedCPA.getClass());
        precisions.add(entry.getDeserializePrecisionOperator().deserializePrecision(pMessage));
      } else {
        try {
          precisions.add(
              wrappedCPA.getInitialPrecision(
                  DeserializeOperator.startLocationFromMessageType(pMessage, node),
                  StateSpacePartition.getDefaultPartition()));
        } catch (InterruptedException e) {
          throw new AssertionError(
              "Deserialization of precision interrupted for CPA: "
                  + wrappedCPA.getClass().getSimpleName(),
              e);
        }
      }
    }
    return new CompositePrecision(precisions);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.graph.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializeOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class DeserializeCompositePrecisionOperator implements DeserializePrecisionOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;
  private final BlockNode node;

  public DeserializeCompositePrecisionOperator(
      List<ConfigurableProgramAnalysis> pWrapped, BlockNode pNode) {
    wrapped = pWrapped;
    node = pNode;
  }

  @Override
  public Precision deserializePrecision(DssMessage pMessage) {
    ImmutableList.Builder<Precision> precisions = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : wrapped) {
      if (cpa instanceof DistributedConfigurableProgramAnalysis dcpa) {
        precisions.add(dcpa.getDeserializePrecisionOperator().deserializePrecision(pMessage));
      } else {
        try {
          precisions.add(
              cpa.getInitialPrecision(
                  DeserializeOperator.startLocationFromMessageType(pMessage, node),
                  StateSpacePartition.getDefaultPartition()));
        } catch (InterruptedException e) {
          throw new AssertionError(
              "Deserialization of precision interrupted for CPA: " + cpa.getClass().getSimpleName(),
              e);
        }
      }
    }
    return new CompositePrecision(precisions.build());
  }
}

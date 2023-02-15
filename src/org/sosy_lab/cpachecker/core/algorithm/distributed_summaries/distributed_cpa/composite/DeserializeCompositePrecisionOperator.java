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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.decomposition.BlockNode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
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
  private final BlockNode block;

  public DeserializeCompositePrecisionOperator(
      Map<Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pRegistered,
      CompositeCPA pCompositeCPA,
      BlockNode pBlock) {
    registered = pRegistered;
    compositeCPA = pCompositeCPA;
    block = pBlock;
  }

  @Override
  public Precision deserializePrecision(BlockSummaryMessage pMessage) {
    try {
      CFANode location = block.getNodeWithNumber(pMessage.getTargetNodeNumber());
      List<Precision> precisions = new ArrayList<>();
      for (ConfigurableProgramAnalysis wrappedCPA : compositeCPA.getWrappedCPAs()) {
        if (registered.containsKey(wrappedCPA.getClass())) {
          DistributedConfigurableProgramAnalysis entry = registered.get(wrappedCPA.getClass());
          precisions.add(entry.getDeserializePrecisionOperator().deserializePrecision(pMessage));
        } else {
          precisions.add(
              wrappedCPA.getInitialPrecision(location, StateSpacePartition.getDefaultPartition()));
        }
      }
      return new CompositePrecision(precisions);
    } catch (InterruptedException pE) {
      throw new AssertionError(pE);
    }
  }
}

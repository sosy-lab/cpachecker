// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import static org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA.zip;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DistributedCompositeCPA.CpaAndPrecision;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class SerializeCompositePrecisionOperator implements SerializePrecisionOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;

  public SerializeCompositePrecisionOperator(List<ConfigurableProgramAnalysis> pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public ImmutableMap<String, String> serializePrecision(Precision pPrecision) {
    ImmutableMap.Builder<String, String> content = ImmutableMap.builder();
    try {
      CompositePrecision compositePrecision = (CompositePrecision) pPrecision;
      for (CpaAndPrecision cpaAndPrecision : zip(wrapped, compositePrecision)) {
        if (cpaAndPrecision.cpa() instanceof DistributedConfigurableProgramAnalysis dcpa) {
          Preconditions.checkState(
              dcpa.getInitialPrecision(
                      CFANode.newDummyCFANode(), StateSpacePartition.getDefaultPartition())
                  .getClass()
                  .isAssignableFrom(cpaAndPrecision.precision().getClass()));
          content.putAll(
              dcpa.getSerializePrecisionOperator().serializePrecision(cpaAndPrecision.precision()));
        }
      }
      return content.buildOrThrow();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }
}

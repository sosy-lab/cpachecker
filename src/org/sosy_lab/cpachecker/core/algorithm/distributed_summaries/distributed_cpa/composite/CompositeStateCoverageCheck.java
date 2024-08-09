// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableMap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.fixpoint.CoverageCheck;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CompositeStateCoverageCheck implements CoverageCheck {

  private final ImmutableMap<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;

  public CompositeStateCoverageCheck(
      ImmutableMap<
              Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pAnalyses) {
    analyses = pAnalyses;
  }

  @Override
  public boolean isCovered(AbstractState state1, AbstractState state2)
      throws CPAException, InterruptedException {
    CompositeState compositeState1 = (CompositeState) state1;
    CompositeState compositeState2 = (CompositeState) state2;
    for (DistributedConfigurableProgramAnalysis dcpa : analyses.values()) {
      boolean covered =
          dcpa.getCoverageCheck()
              .isCovered(
                  AbstractStates.extractStateByType(compositeState1, dcpa.getAbstractStateClass()),
                  AbstractStates.extractStateByType(compositeState2, dcpa.getAbstractStateClass()));
      if (covered) {
        return true;
      }
    }
    return false;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.verification_condition.VerificationConditionOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.java_smt.api.SolverException;

public class CompositeVerificationConditionOperator implements VerificationConditionOperator {

  private final CompositeCPA compositeCPA;
  private final ImmutableMap<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;

  public CompositeVerificationConditionOperator(
      CompositeCPA pCompositeCPA,
      ImmutableMap<
              Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pAnalyses) {
    compositeCPA = pCompositeCPA;
    analyses = pAnalyses;
  }

  @Override
  public Optional<AbstractState> computeVerificationCondition(
      ARGPath pARGPath, Optional<ARGState> pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    ImmutableList.Builder<AbstractState> states = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : compositeCPA.getWrappedCPAs()) {
      if (!analyses.containsKey(cpa.getClass())) {
        states.add(
            cpa.getInitialState(
                Objects.requireNonNull(AbstractStates.extractLocation(pARGPath.getFirstState())),
                StateSpacePartition.getDefaultPartition()));
      } else {
        Optional<AbstractState> abstractState =
            Objects.requireNonNull(analyses.get(cpa.getClass()))
                .getVerificationConditionOperator()
                .computeVerificationCondition(pARGPath, pPreviousCondition);
        if (abstractState.isEmpty()) {
          return Optional.empty();
        }
        states.add(abstractState.orElseThrow());
      }
    }
    return Optional.of(new CompositeState(states.build()));
  }
}

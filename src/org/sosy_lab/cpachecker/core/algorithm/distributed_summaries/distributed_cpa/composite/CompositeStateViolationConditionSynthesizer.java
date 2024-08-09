// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationCondition;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition.ViolationConditionSynthesizer;
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

public class CompositeStateViolationConditionSynthesizer implements ViolationConditionSynthesizer {

  private final CompositeCPA compositeCPA;
  private final ImmutableMap<
          Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
      analyses;

  public CompositeStateViolationConditionSynthesizer(
      CompositeCPA pCompositeCPA,
      ImmutableMap<
              Class<? extends ConfigurableProgramAnalysis>, DistributedConfigurableProgramAnalysis>
          pAnalyses) {
    compositeCPA = pCompositeCPA;
    analyses = pAnalyses;
  }

  @Override
  public ViolationCondition computeViolationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws InterruptedException, CPATransferException, SolverException {
    ImmutableList.Builder<AbstractState> states = ImmutableList.builder();
    for (ConfigurableProgramAnalysis cpa : compositeCPA.getWrappedCPAs()) {
      if (!analyses.containsKey(cpa.getClass())) {
        states.add(
            cpa.getInitialState(
                Objects.requireNonNull(AbstractStates.extractLocation(pARGPath.getFirstState())),
                StateSpacePartition.getDefaultPartition()));
      } else {
        ViolationCondition violationCondition =
            Objects.requireNonNull(analyses.get(cpa.getClass()))
                .computeViolationCondition(pARGPath, pPreviousCondition);
        if (violationCondition.isFeasible()) {
          states.add(violationCondition.getViolation());
        } else {
          return violationCondition;
        }
      }
    }
    return ViolationCondition.feasibleCondition(new CompositeState(states.build()));
  }
}

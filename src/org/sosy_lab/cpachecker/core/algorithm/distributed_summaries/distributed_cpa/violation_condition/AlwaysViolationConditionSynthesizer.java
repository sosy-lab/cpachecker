// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.violation_condition;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class AlwaysViolationConditionSynthesizer implements ViolationConditionSynthesizer {

  private final ConfigurableProgramAnalysis cpa;
  private final CFANode startNode;

  public AlwaysViolationConditionSynthesizer(ConfigurableProgramAnalysis pCpa, CFANode pStartNode) {
    cpa = pCpa;
    startNode = pStartNode;
  }

  @Override
  public ViolationCondition computeViolationCondition(ARGPath pARGPath, ARGState pPreviousCondition)
      throws InterruptedException, CPATransferException {
    return ViolationCondition.feasibleCondition(
        cpa.getInitialState(startNode, StateSpacePartition.getDefaultPartition()));
  }
}

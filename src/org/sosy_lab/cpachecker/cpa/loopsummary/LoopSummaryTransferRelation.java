// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class LoopSummaryTransferRelation extends AbstractLoopSummaryTransferRelation<CPAException> {

  // private final AlgorithmFactory algorithmFactory;

  // private final LoopSummaryCPAStatistics stats;

  public LoopSummaryTransferRelation(
      LoopSummaryCPA loopSummaryCpa,
      ShutdownNotifier pShutdownNotifier,
      AlgorithmFactory pFactory) {
    super(loopSummaryCpa, pShutdownNotifier);
    // algorithmFactory = pFactory;
    // stats = loopSummaryCpa.getStatistics();
  }
}

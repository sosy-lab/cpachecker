// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.java_smt.api.SolverException;

public interface BlockAnalyzer {

  Collection<BlockSummaryMessage> analyze(Collection<BlockSummaryMessage> messages)
      throws CPAException, InterruptedException, SolverException;

  Collection<BlockSummaryMessage> performInitialAnalysis()
      throws InterruptedException, CPAException;
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.MessageProcessing;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.ActorMessage;
import org.sosy_lab.java_smt.api.SolverException;

public interface ProceedOperator {

  MessageProcessing proceedForward(ActorMessage pMessage)
      throws InterruptedException, SolverException;

  MessageProcessing proceedBackward(ActorMessage pMessage)
      throws InterruptedException, SolverException;

  MessageProcessing proceed(ActorMessage pMessage) throws InterruptedException, SolverException;

  void synchronizeKnowledge(DistributedConfigurableProgramAnalysis pAnalysis);

  void update(ActorMessage pLatestOwnPreconditionMessage);
}

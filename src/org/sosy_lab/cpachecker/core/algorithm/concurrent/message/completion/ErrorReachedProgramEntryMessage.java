// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler.MessageProcessingVisitor;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;

public class ErrorReachedProgramEntryMessage implements Message {
  private final ErrorOrigin origin;
  
  /*
   * ErrorReachedProgramEntryMessage independently publishes the AlgorithmStatus of the 
   * corresponding BackwardAnalysis. After sending the ErrorReachedProgramEntryMessage, this task 
   * might get aborted before emitting the TaskCompletionMessage which would otherwise propagate 
   * its status.
   */
  private final AlgorithmStatus status;
  
  public ErrorReachedProgramEntryMessage(final ErrorOrigin pOrigin, final AlgorithmStatus pStatus) {
    origin = pOrigin;
    status = pStatus;
  }
  
  @Override
  public void accept(MessageProcessingVisitor visitor) {
    visitor.visit(this);
  }
  
  public ErrorOrigin getOrigin() {
    return origin;
  }
  
  public AlgorithmStatus getStatus() {
    return status;
  }
}

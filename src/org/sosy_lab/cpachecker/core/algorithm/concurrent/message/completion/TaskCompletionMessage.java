// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.completion;


import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler.MessageProcessingVisitor;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.Message;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;

public class TaskCompletionMessage implements Message {  
  private final Task task;
  
  public TaskCompletionMessage(final Task pTask) {
    task = pTask;
  }
  
  @Override
  public void accept(MessageProcessingVisitor visitor) {
    visitor.visit(this);
  }
  
  public Task getTask() {
    return task;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message;

import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler.MessageProcessingVisitor;

/**
 * Tasks communicate with the Scheduler using message objects of this class.
 * Such messages (a) provide the scheduler with statistics about a completed task, (b) request the 
 * execution of a new task. Often, these two events occur simultaneously, because new tasks   
 */
public interface Message { 
  void accept(final MessageProcessingVisitor visitor);
}

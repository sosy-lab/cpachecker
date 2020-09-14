// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * The Control Flow Distance is represented through a List/Set of Events The Event class makes
 * easier the representation of the Differences between two Executions
 */
public class Event {

  private final int line;
  private final CFAEdge execution;
  private final CFANode node;
  private final List<CFAEdge> path;

  public Event(CFAEdge pExecution, List<CFAEdge> pPath) {
    line = pExecution.getLineNumber();
    execution = pExecution;
    node = pExecution.getPredecessor();
    path = pPath;
  }

  /** Calculates how far is this Event from the Target */
  public int getDistanceFromTheEnd() {
    for (int i = 0; i < path.size(); i++) {
      if (path.get(i) == execution) {
        return path.size() - i;
      }
    }
    return 0;
  }

  int getLine() {
    return line;
  }

  String getStatement() {
    return execution.getCode();
  }

  CFANode getNode() {
    return node;
  }

  @Override
  public String toString() {
    return "Line " + line + ": " + execution.getCode();
  }
}

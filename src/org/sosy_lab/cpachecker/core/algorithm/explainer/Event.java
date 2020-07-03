/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2020  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.algorithm.explainer;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * The Control Flow Distance is represented through a List/Set of Events
 * The Event class makes easier the representation of the Differences between two Executions
 */
public class Event {

  private final int line;
  private final CFAEdge execution;
  private final CFANode node;
  private final List<CFAEdge> path;

  public Event(CFAEdge pExecution, List<CFAEdge> pPath) {
    this.line = pExecution.getLineNumber();
    this.execution = pExecution;
    this.node = pExecution.getPredecessor();
    this.path = pPath;
  }

  /**
   * Calculates how far is this Event from the Target
   */
  public int getDistanceFromTheEnd() {
    for (int i = 0; i < path.size(); i++) {
      if (path.get(i) == execution) {
        return (path.size() - i);
      }
    }
    return 0;
  }

  int getLine() {
    return this.line;
  }

  String getStatement() {
    return this.execution.getCode();
  }

  CFANode getNode() {
    return this.node;
  }


  @Override
  public String toString() {
    return "Line " + line + ": " + execution.getCode();
  }


}

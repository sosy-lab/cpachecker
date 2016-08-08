/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.postprocessing.sequencer.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.AFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;

import com.google.common.base.Optional;
import com.google.common.collect.SetMultimap;

public abstract class AThread {

  private FunctionEntryNode threadFunction;
  private int threadNumber;
  private String threadName;
  private Map<CFANode, ContextSwitch> contextSwitchPoints = new HashMap<CFANode, ContextSwitch>();
  protected SetMultimap<String, ? extends AStatementEdge> usedFunctions;
  private Optional<? extends AFunctionCall> threadCreationStatement;
  private Optional<? extends AThread> creator;

  public AThread(FunctionEntryNode threadFunction, String threadName,
      int threadNumber, @Nullable AFunctionCall threadCreationStatement,
      SetMultimap<String, ? extends AStatementEdge> usedFunctions, @Nullable AThread creator) {
    assert threadNumber >= 0;

    this.threadFunction = threadFunction;
    this.threadName = threadName;
    this.threadNumber = threadNumber;
    this.usedFunctions = usedFunctions;
    this.creator = creator == null ? Optional.<AThread>absent() : Optional.<AThread>of(creator);
    this.threadCreationStatement = threadCreationStatement == null ? Optional.<AFunctionCall>absent() : Optional.<AFunctionCall>of(threadCreationStatement);

  }

  public FunctionEntryNode getThreadFunction() {
    return threadFunction;
  }

  public void setThreadFunction(FunctionEntryNode threadFunction) {
    this.threadFunction = threadFunction;
  }

  public String getThreadName() {
    return threadName;
  }

  public int getThreadNumber() {
    return threadNumber;
  }

  public Optional<? extends AFunctionCall> getThreadCreationStatement() {
    return threadCreationStatement;
  }

  public Optional<? extends AThread> getCreator() {
    return creator;
  }

  public void addContextSwitch(CFAEdge switchEdge) {
    if (contextSwitchPoints.containsKey(switchEdge.getSuccessor())) {
      ContextSwitch contextSwitchLocation = contextSwitchPoints.get(switchEdge.getSuccessor());
      assert equals(contextSwitchLocation.getThread());
      contextSwitchLocation.addContextStatementCause(switchEdge);

    } else {
      ContextSwitch cs = new ContextSwitch(contextSwitchPoints.size() + 1, this, switchEdge);
      contextSwitchPoints.put(switchEdge.getSuccessor(), cs);
    }

  }

  public abstract void addUsedFunction(String usedFunction, AStatementEdge functionCallStatement);

  public SetMultimap<String, ? extends AStatementEdge> getUsedFunctions() {
    return usedFunctions;
  }

  public List<ContextSwitch> getContextSwitchPoints() {
    List<ContextSwitch> sorted = new ArrayList<>(contextSwitchPoints.values());
    Collections.sort(sorted);

    // context switch points musn't appear twice
    assert sorted.size() == new HashSet<>(sorted).size();

    return sorted;
  }


  @Override
  public String toString() {
    return threadName + "[" + threadFunction.getFunctionName() + "]";
  }

}

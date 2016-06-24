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

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class CThread extends AThread {


  public CThread(CFunctionEntryNode threadFunction, String threadName, int threadNumber, @Nullable CFunctionCall pthread_createStatement, CThread creator) {
    super(threadFunction, threadName, threadNumber, pthread_createStatement, HashMultimap.<String, CStatementEdge> create(), creator);
  }

  @Override
  public CFunctionEntryNode getThreadFunction() {
    return (CFunctionEntryNode) super.getThreadFunction();
  }

  @Override
  public void setThreadFunction(FunctionEntryNode threadFunction) {
    assert threadFunction instanceof CFunctionEntryNode;
    super.setThreadFunction(threadFunction);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<CFunctionCall> getThreadCreationStatement() {
    return (Optional<CFunctionCall>) super.getThreadCreationStatement();
  }

  @SuppressWarnings("unchecked")
  @Override
  public void addUsedFunction(String usedFunction, AStatementEdge functionCallStatement) {
    assert functionCallStatement instanceof CStatementEdge;
    ((SetMultimap<String, CStatementEdge>) usedFunctions).put(usedFunction, (CStatementEdge) functionCallStatement);
  }

  @Override
  @SuppressWarnings("unchecked")
  public SetMultimap<String, CStatementEdge> getUsedFunctions() {
    return (SetMultimap<String, CStatementEdge>) super.getUsedFunctions();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Optional<CThread> getCreator() {
    return (Optional<CThread>) super.getCreator();
  }

}
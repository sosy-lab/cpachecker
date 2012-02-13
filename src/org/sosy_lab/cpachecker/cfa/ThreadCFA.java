/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * Wrapper around CFA with information on thread number, variables, etc.
 */
public class ThreadCFA extends CFA {

  public final int tid;
  public final ImmutableList<CFANode> globalDeclNodes;
  /** The first node of the CFA */
  public final CFAFunctionDefinitionNode initalNode;
  /** The first node after the global declaration */
  public final CFANode threadStart;
  public final String threadName;
  /** local variables */
  private ImmutableSet<String> localVars;

  public ThreadCFA(CFA cfa, int tid, List<CFANode> globalDecl, CFAFunctionDefinitionNode initalNode, CFANode threadStart, String name){
    super(cfa.getFunctions(), cfa.getCFANodes(), cfa.getGlobalDeclarations());
    this.tid = tid;
    this.globalDeclNodes = ImmutableList.copyOf(globalDecl);
    this.initalNode = initalNode;
    this.threadStart = threadStart;
    this.threadName = name;
  }

  public int getTid() {
    return tid;
  }

  public ImmutableList<CFANode> getGlobalDeclNodes() {
    return globalDeclNodes;
  }

  public CFANode getThreadStart() {
    return threadStart;
  }

  public CFAFunctionDefinitionNode getInitalNode() {
    return initalNode;
  }

  public Multimap<CFANode, String> getLhsVariables() {
    // TODO Auto-generated method stub
    return null;
  }

  public Multimap<CFANode, String> getRhsVariables() {
    // TODO Auto-generated method stub
    return null;
  }


  public ImmutableSet<String> getLocalVars() {
    return localVars;
  }

  public void setLocalVars(ImmutableSet<String> pLocalVars) {
    localVars = pLocalVars;
  }

  public void setLocalVars(final Set<String> localVars) {
    this.localVars = ImmutableSet.copyOf(localVars);
  }



}

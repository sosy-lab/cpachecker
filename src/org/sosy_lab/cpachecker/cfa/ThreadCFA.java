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

import java.util.Collection;
import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableSet;

/**
 * Wrapper around CFA with information on thread number, variables, etc.
 */
public class ThreadCFA extends CFA {

  public final int tid;
  /** All nodes **/
  public final ImmutableSet<CFANode> allNodes;
  /** Nodes before global declarations, up to but excluding {@link #executionStart} node. */
  public final ImmutableSet<CFANode> globalDeclNodes;
  /** All nodes minus the global declaration nodes. */
  public final ImmutableSet<CFANode> execNodes;
  /** nodes where other threads cannot interfere */
  public final ImmutableSet<CFANode> atomic;
  /** The first node of the CFA. */
  public final CFAFunctionDefinitionNode initalNode;
  /** The first node after the global declaration, i.e. the predecessor of "Function start dummy edge". */
  public final CFANode executionStart;
  public final String threadName;
  /** local variables */
  public final ImmutableSet<String> localVars;


  public ThreadCFA(CFA cfa, int tid, Collection<CFANode> globalDecl, Collection<CFANode> atomic, Collection<String> localVars, CFAFunctionDefinitionNode initalNode, CFANode threadStart, String name){
    super(cfa.getFunctions(), cfa.getCFANodes(), cfa.getGlobalDeclarations());
    this.tid = tid;
    this.globalDeclNodes = ImmutableSet.copyOf(globalDecl);
    this.initalNode = initalNode;
    this.executionStart = threadStart;

    List<CFANode> bldr = new Vector<CFANode>(cfa.getCFANodes().values());
    bldr.add(this.executionStart);
    bldr.addAll(this.globalDeclNodes);
    this.allNodes = ImmutableSet.copyOf(bldr);
    bldr.removeAll(this.globalDeclNodes);
    this.execNodes = ImmutableSet.copyOf(bldr);

    this.threadName = name;
    this.localVars = ImmutableSet.copyOf(localVars);
    this.atomic  = ImmutableSet.copyOf(atomic);
  }

  public ImmutableSet<CFANode> getAllNodes() {
    return allNodes;
  }

  public ImmutableSet<CFANode> getGlobalDeclNodes() {
    return globalDeclNodes;
  }

  public ImmutableSet<CFANode> getExecNodes() {
    return execNodes;
  }

  public int getTid() {
    return tid;
  }

  public CFANode getExecutionStart() {
    return executionStart;
  }

  public CFAFunctionDefinitionNode getInitalNode() {
    return initalNode;
  }

  public ImmutableSet<String> getLocalVars() {
    return localVars;
  }

  public String getThreadName() {
    return threadName;
  }

  public ImmutableSet<CFANode> getAtomic() {
    return atomic;
  }

}

/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public class LoopScopeImpl implements LoopScope, UnnamedScope, NoLocalDeclarationsScope {
  @Nonnull private final Scope parentScope;
  private CFANode loopStartNode;
  private CFANode loopExitNode;

  LoopScopeImpl(@Nonnull final Scope pParentScope) {
    parentScope = pParentScope;
  }

  public LoopScopeImpl(
      @Nonnull final Scope pParentScope,
      @Nonnull final CFANode pLoopStartNode,
      @Nonnull final CFANode pLoopExitNode) {
    parentScope = pParentScope;
    loopStartNode = pLoopStartNode;
    loopExitNode = pLoopExitNode;
  }

  @Nonnull
  @Override
  public CFANode getLoopStartNode() {
    return loopStartNode;
  }

  void setLoopStartNode(@Nonnull final CFANode pLoopStartNode) {
    loopStartNode = pLoopStartNode;
  }

  @Nonnull
  @Override
  public CFANode getLoopExitNode() {
    return loopExitNode;
  }

  void setLoopExitNode(@Nonnull final CFANode pLoopExitNode) {
    loopExitNode = pLoopExitNode;
  }

  @Override
  public Scope getParentScope() {
    return parentScope;
  }

}

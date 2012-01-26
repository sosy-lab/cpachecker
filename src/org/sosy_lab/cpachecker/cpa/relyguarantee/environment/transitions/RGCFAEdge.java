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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class RGCFAEdge implements CFAEdge {

  private final RGEnvTransition rgEnvTransition;
  private final CFANode predecessor;
  private final CFANode successor;

  public RGCFAEdge(RGEnvTransition rgEnvTransition, CFANode predecessor, CFANode successor){
    this.rgEnvTransition  = rgEnvTransition;
    this.predecessor      = predecessor;
    this.successor        = successor;
  }

  public RGEnvTransition getRgEnvTransition() {
    return rgEnvTransition;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.RelyGuaranteeCFAEdge;
  }

  @Override
  public CFANode getPredecessor() {
    return predecessor;
  }

  @Override
  public CFANode getSuccessor() {
    return successor;
  }

  @Override
  public String getRawStatement() {
    return null;
  }

  @Override
  public IASTNode getRawAST() {
    return null;
  }

  @Override
  public int getLineNumber() {
    return -1;
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

}

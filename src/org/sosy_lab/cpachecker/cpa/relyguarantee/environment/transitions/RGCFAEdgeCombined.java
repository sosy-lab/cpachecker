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

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

public class RGCFAEdgeCombined implements CFAEdge {

  private final List<RGEnvTransition> ets;
  private final CFANode predecessor;
  private final CFANode successor;

  public RGCFAEdgeCombined(List<RGEnvTransition> rgEnvTransitions,
      CFANode predecessor,
      CFANode successor){
    this.ets         = rgEnvTransitions;
    this.predecessor      = predecessor;
    this.successor        = successor;
  }



  public List<RGEnvTransition> getEnvTransitions() {
    return ets;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.RelyGuaranteeCombinedCFAEdge;
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
    StringBuilder bldr = new StringBuilder();

    for (int i=0; i<ets.size(); i++){
      if (i>0){
        bldr.append(" | ");
      }
      bldr.append(ets.get(i));
    }
    return bldr.toString();
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

  @Override
  public String toString() {
    return getRawStatement();
  }

  @Override
  public boolean isLocalRead() {
    return false;
  }

  @Override
  public boolean isGlobalRead() {
    return false;
  }

  @Override
  public boolean isLocalWrite() {
    return false;
  }

  @Override
  public boolean isGlobalWrite() {
    return false;
  }

  @Override
  public boolean equals(Object o){
    if (o instanceof RGCFAEdgeCombined){
      RGCFAEdgeCombined oe = (RGCFAEdgeCombined) o;
      return ets.equals(oe.ets);
    }

    return false;
  }

}

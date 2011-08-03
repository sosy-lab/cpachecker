/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdgeTemplate.ARTElementWrapper;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RelyGuaranteeCFAEdgeTemplate.PathFormulaWrapper;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

public class RelyGuaranteeCFAEdge implements CFAEdge{

  private final CFAEdge localEdge;
  private final PathFormulaWrapper pathFormulaWrapper;
  private final ARTElementWrapper sourceARTElementWrapper;
  private final int sourceTid;
  private CFANode predecessor;
  private CFANode successor;

  protected RelyGuaranteeCFAEdge(CFAEdge pEdge, PathFormulaWrapper pathFormulaWrapper, int sourceTid, ARTElementWrapper sourceARTElementWrapper, RelyGuaranteeEnvironmentalTransition sourceEnvTransition){
    this.localEdge = pEdge;
    this.pathFormulaWrapper = pathFormulaWrapper;
    this.sourceARTElementWrapper = sourceARTElementWrapper;
    this.sourceTid = sourceTid;
  }

  public ARTElement getSourceARTElement() {
    return this.sourceARTElementWrapper.artElement;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.RelyGuaranteeCFAEdge;
  }

  @Override
  public int getLineNumber() {
    return -1;
  }

  @Override
  public CFANode getPredecessor() {
    return this.predecessor;
  }

  public void setPredecessor(CFANode node){
    this.predecessor = node;
  }

  @Override
  public IASTNode getRawAST() {
    return null;
  }

  @Override
  public String getRawStatement() {
    return localEdge.getRawStatement();
  }

  @Override
  public CFANode getSuccessor() {
    return this.successor;
  }

  public void setSuccessor(CFANode node){
    this.successor = node;
  }


  public PathFormula getPathFormula() {
    return this.pathFormulaWrapper.pathFormula;
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

  @Override
  public String toString() {
    return "RelyGuaranteeEnvEdge from "+this.sourceTid+": "+localEdge.getRawStatement()+", "+this.pathFormulaWrapper.pathFormula;
  }

  public CFAEdge getLocalEdge() {
    return this.localEdge;
  }

  public int getSourceTid(){
    return this.sourceTid;
  }

}

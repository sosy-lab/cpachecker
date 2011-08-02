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
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

public class RelyGuaranteeCFAEdge implements CFAEdge{

  private CFAEdge localEdge;
  private PathFormula pathFormula;
  private CFANode predecessor;
  private CFANode successor;
  private final int sourceTid;
  private ARTElement sourceARTElement;

  // environmental transition form which this edge was generated from
  private final RelyGuaranteeEnvironmentalTransition sourceEnvTransition;
  //
  private final Status status;

  //private final int id;

  private static int lastGlobalId = 0;


  /**
   *
   * @param edge local edge that created the environmental transition
   * @param abstractionFormula abstraction formula of the predecessor of the local edge
   * @param pathFormula path formula of the predecessor of the local edge
   */
  public RelyGuaranteeCFAEdge(CFAEdge pEdge, PathFormula pPathFormula, int sourceTid, ARTElement sourceARTElement, RelyGuaranteeEnvironmentalTransition sourceEnvTransition){
    this.localEdge = pEdge;
    this.pathFormula = pPathFormula;
    this.sourceTid = sourceTid;
    this.sourceARTElement = sourceARTElement;
    lastGlobalId++;
    //this.id = lastGlobalId;
    this.sourceEnvTransition = sourceEnvTransition;
    this.status = new Status(false);
  }

  private RelyGuaranteeCFAEdge(CFAEdge pEdge, PathFormula pPathFormula, int sourceTid, ARTElement sourceARTElement, RelyGuaranteeEnvironmentalTransition sourceEnvTransition, Status status){
    this.localEdge = pEdge;
    this.pathFormula = pPathFormula;
    this.sourceTid = sourceTid;
    this.sourceARTElement = sourceARTElement;
    lastGlobalId++;
    //this.id = lastGlobalId;
    this.sourceEnvTransition = sourceEnvTransition;
    this.status = status;
  }

  /*public RelyGuaranteeCFAEdge(RelyGuaranteeCFAEdge pOther) {
    this.localEdge = pOther.localEdge;
    this.pathFormula = pOther.pathFormula;
    this.sourceTid = pOther.sourceTid;
    this.sourceARTElement = pOther.sourceARTElement;
    lastGlobalId++;
    this.id = lastGlobalId;
    this.sourceEnvTransition = pOther.sourceEnvTransition;
    this.status = pOther.status;
  }*/


  public RelyGuaranteeCFAEdge makeCopy(){
    RelyGuaranteeCFAEdge copy = new RelyGuaranteeCFAEdge(this.localEdge, this.pathFormula, this.sourceTid, this.sourceARTElement, this.sourceEnvTransition, this.status);
    return copy;
  }



  public ARTElement getSourceARTElement() {
    return sourceARTElement;
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
    return this.pathFormula;
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

  @Override
  public String toString() {
    return "RelyGuaranteeEnvEdge from "+this.sourceTid+": "+localEdge.getRawStatement()+", "+this.pathFormula;
  }

  public CFAEdge getLocalEdge() {
    return this.localEdge;
  }

  public int getSourceTid(){
    return this.sourceTid;
  }

  public void addCovered(RelyGuaranteeCFAEdge covered){

  }

  public void setPathFormula(PathFormula pPathFormula) {
    pathFormula = pPathFormula;
  }

  public void setSourceARTElement(ARTElement pSourceARTElement) {
    sourceARTElement = pSourceARTElement;
  }

  public RelyGuaranteeEnvironmentalTransition getSourceEnvTransition() {
    return sourceEnvTransition;
  }


  class Status{
    private boolean isKilled;

    Status(boolean isKilled){
      this.isKilled = isKilled;
    }

    public boolean isKilled(){
      return isKilled;
    }

    public void setKilled(boolean isKilled){
      this.isKilled = isKilled;
    }
  }


}

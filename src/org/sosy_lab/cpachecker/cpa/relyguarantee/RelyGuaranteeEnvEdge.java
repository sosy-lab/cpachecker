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
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

public class RelyGuaranteeEnvEdge implements CFAEdge{

  private CFAEdge localEdge;
  private AbstractionFormula abstractionFormula;
  private PathFormula pathFormula;
  private CFANode predecessor;
  private CFANode successor;


  /**
   * @param edge local edge that created the environmental transition
   * @param abstractionFormula abstraction formula of the predecessor of the local edge
   * @param pathFormula path formula of the predecessor of the local edge
   */
  public RelyGuaranteeEnvEdge(CFAEdge pEdge, AbstractionFormula pAbstractionFormula, PathFormula pPathFormula){
    this.localEdge = pEdge;
    this.abstractionFormula = pAbstractionFormula;
    this.pathFormula = pPathFormula;
  }

  public RelyGuaranteeEnvEdge(RelyGuaranteeEnvEdge pOther) {
    this.localEdge = pOther.localEdge;
    this.abstractionFormula = pOther.abstractionFormula;
    this.pathFormula = pOther.pathFormula;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.EnvironmentalEdge;
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

  public AbstractionFormula getAbstractionFormula(){
    return this.abstractionFormula;
  }

  public PathFormula getPathFormula() {
    return this.pathFormula;
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

  public String toString() {
    return "RelyGuaranteeEnvEdge: "+localEdge.getRawStatement()+", "+this.abstractionFormula.toString()+", "+this.pathFormula.toString();
  }

  public CFAEdge getLocalEdge() {
    return this.localEdge;
  }

}

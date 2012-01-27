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
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGCFAEdgeTemplate;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

public class RGCFAEdge2 implements CFAEdge{

  private final RGCFAEdgeTemplate template;

  private final CFANode predecessor;
  private final CFANode successor;

  public RGCFAEdge2(RGCFAEdgeTemplate pTemplate, CFANode predecessor, CFANode successor) {
    assert predecessor != null;
    assert successor != null;
    this.template = pTemplate;
    this.predecessor = predecessor;
    this.successor = successor;
  }

  public ARTElement getSourceARTElement() {
    return this.template.getSourceARTElement();
  }

  public ARTElement getLastARTAbstractionElement() {
    return this.template.getLastARTAbstractionElement();
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.RelyGuaranteeCFAEdge;
  }

  public RGEnvCandidate getSourceEnvTransition(){
    return template.getSourceEnvTransition();
  }

  @Override
  public int getLineNumber() {
    return -1;
  }

  @Override
  public CFANode getPredecessor() {
    return this.predecessor;
  }


  @Override
  public IASTNode getRawAST() {
    return null;
  }

  @Override
  public String getRawStatement() {
    return this.template.getRawStatement();
  }

  @Override
  public CFANode getSuccessor() {
    return this.successor;
  }

 /* public PathFormula getPathFormula() {
    return this.template.getPathFormula();
  }

  public PathFormula getAbstractionFormula() {
    return this.template.getAbstractionFormula();
  }*/

  public PathFormula getFilter() {
    return this.template.getFilter();
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

  public CFAEdge getLocalEdge(){
    return this.template.getLocalEdge();
  }

  @Override
  public String toString() {
    return "RG edge -- op:"+this.getRawStatement()+", filter:"+this.getFilter()+",  T:"+this.getSourceTid()+", sART:"+this.getSourceARTElement().getElementId();
  }

  public int getSourceTid(){
    return this.template.getSourceTid();
  }

  public RGCFAEdgeTemplate getTemplate() {
    return template;
  }

  public CFAEdge getOperation() {
    return this.template.getOperation();
  }


  @Override
  public int hashCode() {
    return this.predecessor.hashCode() + 23 * (this.successor.hashCode() +  13 * this.template.hashCode());
  }


}

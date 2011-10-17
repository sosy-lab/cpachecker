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

  private final RelyGuaranteeCFAEdgeTemplate template;

  private final CFANode predecessor;
  private final CFANode successor;

  public RelyGuaranteeCFAEdge(RelyGuaranteeCFAEdgeTemplate pTemplate, CFANode predecessor, CFANode successor) {
    assert predecessor != null;
    assert successor != null;
    this.template = pTemplate;
    this.predecessor = predecessor;
    this.successor = successor;
  }

  public ARTElement getSourceARTElement() {
    return this.template.getSourceARTElement();
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

  public PathFormula getPathFormula() {
    return this.template.getPathFormula();
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

  @Override
  public String toString() {
    return "RG edge from "+this.getSourceTid()+": "+this.getRawStatement()+", "+this.getPathFormula()+" by element id:"+this.getSourceARTElement();
  }

  public CFAEdge getLocalEdge() {
    return this.template.getLocalEdge();
  }

  public int getSourceTid(){
    return this.template.getSourceTid();
  }

  public RelyGuaranteeCFAEdgeTemplate getTemplate() {
    return template;
  }

  public Integer getUniquePrimeThis() {
    return this.template.getUniquePrimeThis();
  }

  public Integer getUniquePrimeOther() {
    return this.template.getUniquePrimeOther();
  }

  @Override
  public int hashCode() {
    return this.predecessor.hashCode() + 23 * (this.successor.hashCode() +  13 * this.template.hashCode());
  }

}

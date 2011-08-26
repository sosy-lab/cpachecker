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

import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

/**
 * CFA edge combined of multiple env. edges.
 */
public class RelyGuaranteeCombinedCFAEdge implements CFAEdge {

  private CFANode predecessor;
  private CFANode successor;

  private final List<RelyGuaranteeCFAEdge> envEdges;
  private final int edgeNo;


  public RelyGuaranteeCombinedCFAEdge(List<RelyGuaranteeCFAEdgeTemplate> templates){
    assert !templates.isEmpty();
    this.edgeNo = templates.size();
    this.envEdges = new Vector<RelyGuaranteeCFAEdge>(edgeNo);
    for (RelyGuaranteeCFAEdgeTemplate template : templates){
      this.envEdges.add(template.instantiate());
    }


  }

  /*public RelyGuaranteeCombinedCFAEdge(List<RelyGuaranteeCFAEdge> envEdges){
    assert !envEdges.isEmpty();
    this.envEdges = envEdges;
    this.edgeNo = envEdges.size();
  }*/



  public List<RelyGuaranteeCFAEdge> getEnvEdges() {
    return envEdges;
  }

  public int getEdgeNo() {
    return edgeNo;
  }




  public void setPredecessor(CFANode pPredecessor) {
    predecessor = pPredecessor;
  }

  public void setSuccessor(CFANode pSuccessor) {
    successor = pSuccessor;
  }

  @Override
  public CFAEdgeType getEdgeType() {
    return CFAEdgeType.RelyGuaranteeCombinedCFAEdge;
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
  public CFANode getSuccessor() {
    return this.successor;
  }

  @Override
  public IASTNode getRawAST() {
    return null;
  }

  @Override
  public String getRawStatement() {
    String result = envEdges.get(0).getRawStatement();
    for (int i=1; i<edgeNo; i++){
      result = result + " || " + envEdges.get(i).getRawStatement();
    }
    return result;
  }

  @Override
  public boolean isJumpEdge() {
    return false;
  }

  @Override
  public String toString() {
    return "RG combined cfa edge " + getRawStatement();
  }

}

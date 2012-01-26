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

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvCandidate;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;




/**
 * Represents one type of environmental edge, which consists of:
 * path formula, local edge and source thread id. The class contains
 * general information, i.e. which edges are more general.
 * It can be instantiated into RelyGuaranteeCFAEdges attached to different CFA nodes.
 */
public class RGCFAEdgeTemplate{

  /** Type indicators for edges. */
  public static int RelyGuaranteeCFAEdgeTemplate = 0;
  public static int RelyGuaranteeAbstractCFAEdgeTemplate = 1;

  /** Describes the precondition for appling the env. operation */
  private final PathFormula filter;

  /** Environmental transition form which this edge was generated from */
  private final RGEnvCandidate sourceEnvTransition;

  /** Unkilled env. edge that is more general than this one. */
  private RGCFAEdgeTemplate coveredBy;

  /** Unkilled env. edges that are less general that this one. */
  private final Set<RGCFAEdgeTemplate> covers;

  /** Last abstracton point before the element that generated the env. transition */
  private final ARTElement lastAbstraction;


  public RGCFAEdgeTemplate(PathFormula filter, ARTElement lastARTAbstractionElement, RGEnvCandidate sourceEnvTransition){
    this.filter = filter;
    this.lastAbstraction = lastARTAbstractionElement;
    this.sourceEnvTransition = sourceEnvTransition;
    this.coveredBy = null;
    this.covers = new HashSet<RGCFAEdgeTemplate>();
  }

  /**
   * Instantiate.
   * @param successor
   * @param predecessor
   * @return
   */
  public RGCFAEdge2 instantiate(CFANode successor, CFANode predecessor){
    RGCFAEdge2 edge = new RGCFAEdge2(this, successor, predecessor);
    return edge;
  }

  public int getType() {
    return RelyGuaranteeCFAEdgeTemplate;
  }


  public ARTElement getSourceARTElement() {
    return this.sourceEnvTransition.getElement();
  }

  public ARTElement getTargetARTElement() {
    return this.sourceEnvTransition.getSuccessor();
  }

  /*public void setSourceARTElement(ARTElement newElem) {
    this.sourceEnvTransition.setSourceARTElement(newElem);
  }*/

  public PathFormula getAbstractionFormula() {
    return this.sourceEnvTransition.getRgElement().getPathFormula();
  }

  public IASTNode getRawAST() {
    return null;
  }

  public ARTElement getLastARTAbstractionElement() {
    return lastAbstraction;
  }

  public String getRawStatement() {
    return this.sourceEnvTransition.getOperation().getRawStatement();
  }

  public PathFormula getPathFormula() {
    return this.sourceEnvTransition.getRgElement().getPathFormula();
  }

  public PathFormula getFilter() {
    return filter;
  }

  @Override
  public String toString() {
    return "RG template -- op:"+this.getRawStatement()+", filter:"+this.getFilter()+",  T:"+this.getSourceTid()+", sART:"+this.getSourceARTElement().getElementId();
  }

  public CFAEdge getLocalEdge() {
    return this.sourceEnvTransition.getOperation();
  }

  public int getSourceTid(){
    return this.sourceEnvTransition.getTid();
  }

  public RGEnvCandidate getSourceEnvTransition() {
    return sourceEnvTransition;
  }

  public RGCFAEdgeTemplate getCoveredBy() {
    return coveredBy;
  }
  public Set<RGCFAEdgeTemplate> getCovers() {
    return covers;
  }

  public CFAEdge getOperation() {
    return this.sourceEnvTransition.getOperation();
  }


}

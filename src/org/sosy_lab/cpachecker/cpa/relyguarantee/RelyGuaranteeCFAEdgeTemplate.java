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
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Represents one type of environmental edge, which consists of:
 * path formula, local edge and source thread id. The class contains
 * general information, i.e. which edges are more general.
 * It can be instantiated into RelyGuaranteeCFAEdges attached to different CFA nodes.
 */
public class RelyGuaranteeCFAEdgeTemplate{

  private final CFAEdge localEdge;
  private final PathFormula pathFormula;
  private ARTElement  sourceARTElement;
  // the last abstraction point in the ART
  private final ARTElement  lastARTAbstractionElement;
  private final int sourceTid;

  // environmental transition form which this edge was generated from
  private final RelyGuaranteeEnvironmentalTransition sourceEnvTransition;
  // unkilled env. edge that is more general than this one
  private RelyGuaranteeCFAEdgeTemplate coveredBy;
  // unkilled env. edges that are less general that this one
  private final Set<RelyGuaranteeCFAEdgeTemplate> covers;
  // part of the env. transition that should always be unique
  private final Integer uniquePrimeThis;
  private final Integer uniquePrimeOther;



  public RelyGuaranteeCFAEdgeTemplate(CFAEdge pEdge, PathFormula pPathFormula, int sourceTid, ARTElement sourceARTElement, ARTElement lastARTAbstractionElement, RelyGuaranteeEnvironmentalTransition sourceEnvTransition, Integer uniquePrimeThis, Integer uniquePrimeOther){
    this.localEdge = pEdge;
    this.pathFormula= pPathFormula;
    this.sourceARTElement = sourceARTElement;
    this.lastARTAbstractionElement = lastARTAbstractionElement;
    this.sourceTid = sourceTid;
    this.sourceEnvTransition = sourceEnvTransition;
    this.coveredBy = null;
    this.covers = new HashSet<RelyGuaranteeCFAEdgeTemplate>();
    this.uniquePrimeThis  = uniquePrimeThis;
    this.uniquePrimeOther = uniquePrimeOther;
  }

  // instantiate
  public RelyGuaranteeCFAEdge instantiate(CFANode successor, CFANode predecessor){
    RelyGuaranteeCFAEdge edge = new RelyGuaranteeCFAEdge(this, successor, predecessor);
    return edge;
  }


  public ARTElement getSourceARTElement() {
    return this.sourceARTElement;
  }

  public void setSourceARTElement(ARTElement newElem) {
    this.sourceARTElement =  newElem;
  }

  public ARTElement getLastARTAbstractionElement() {
    return lastARTAbstractionElement;
  }


  public IASTNode getRawAST() {
    return null;
  }

  public String getRawStatement() {
    return localEdge.getRawStatement();
  }

  public PathFormula getPathFormula() {
    return this.pathFormula;
  }


  @Override
  public String toString() {
    return "RG edge template  T:"+this.sourceTid+", source ART:"+this.sourceARTElement.getElementId()+", abstr. ART:"+this.lastARTAbstractionElement.getElementId()+", CFA edge:"+localEdge.getRawStatement()+", path formula:"+this.pathFormula;
  }

  public CFAEdge getLocalEdge() {
    return this.localEdge;
  }

  public int getSourceTid(){
    return this.sourceTid;
  }

  public RelyGuaranteeEnvironmentalTransition getSourceEnvTransition() {
    return sourceEnvTransition;
  }

  public RelyGuaranteeCFAEdgeTemplate getCoveredBy() {
    return coveredBy;
  }
  public Set<RelyGuaranteeCFAEdgeTemplate> getCovers() {
    return covers;
  }

  public Integer getUniquePrimeThis() {
    return uniquePrimeThis;
  }

  public Integer getUniquePrimeOther() {
    return uniquePrimeOther;
  }

  @Override
  public int hashCode(){
    return uniquePrimeThis + 17 * uniquePrimeOther;
  }

  /**
   * Remember that environmental edge 'other' is more general than this one.
   * @param other
   */
  public void coveredBy(RelyGuaranteeCFAEdgeTemplate other) {

    assert other != null;
    assert other != this;
    assert !other.covers.contains(this);
    assert other.localEdge.equals(this.localEdge);
    // TODO change it - only null should be allowed
    if (coveredBy == null){
      coveredBy  = other;
      other.covers.add(this);
    } else {
      coveredBy.covers.remove(this);
      coveredBy  = other;
      other.covers.add(this);
    }

  }

  /**
   * This edge is not valid anymore.
   */
  public void killValidEdge(){
    assert coveredBy == null;
    for ( RelyGuaranteeCFAEdgeTemplate child : covers){
      assert child.coveredBy == this;
      child.coveredBy = null;
    }
    covers.clear();
  }

  /**
   * Called on a covered edge, which source element has been removed. The edge that covers this object will directly cover the elements
   * are covered by the object.
   */
  public void killCoveredEdge() {
    if (coveredBy == null){
      System.out.println("DEBUG: "+this);
    }
    assert coveredBy != null;
    assert coveredBy.covers.contains(this);

    for ( RelyGuaranteeCFAEdgeTemplate child : covers){
      assert child.coveredBy == this;
      assert child != coveredBy;
      child.coveredBy = coveredBy;
      coveredBy.covers.add(child);
    }
    coveredBy.covers.remove(this);
    covers.clear();
    coveredBy = null;
  }

}

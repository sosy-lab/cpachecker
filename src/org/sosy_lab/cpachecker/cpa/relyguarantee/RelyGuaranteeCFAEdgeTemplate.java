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

  /** The operation. */
  protected final CFAEdge operation;

  /**  Abstraction formula. */
  protected final PathFormula abstractionFormula;

  /** Path formula before the operation. */
  protected final PathFormula pathFormula;

  /** Path formula after the operation. */
  protected final PathFormula opPathFormula;

  /** Variable assigned by the operation */
  protected final String opVar;

  /** ART element that generated this edge. */
  protected ARTElement  sourceARTElement;

  /** The last abstraction point before the source element */
  protected final ARTElement  lastARTAbstractionElement;

  /** Source thread id. */
  protected final int sourceTid;

  /** Environmental transition form which this edge was generated from */
  protected final RelyGuaranteeEnvironmentalTransition sourceEnvTransition;

  /** Unkilled env. edge that is more general than this one. */
  private RelyGuaranteeCFAEdgeTemplate coveredBy;

  /** Unkilled env. edges that are less general that this one. */
  private final Set<RelyGuaranteeCFAEdgeTemplate> covers;

  public RelyGuaranteeCFAEdgeTemplate(CFAEdge operation, PathFormula abstractionFormula, PathFormula pathFormula, PathFormula opPathFormula, String opVar, ARTElement sourceARTElement, ARTElement lastARTAbstractionElement, int sourceTid, RelyGuaranteeEnvironmentalTransition sourceEnvTransition){
    this.operation = operation;
    this.abstractionFormula = abstractionFormula;
    this.pathFormula = pathFormula;
    this.opPathFormula = opPathFormula;
    this.opVar = opVar;
    this.sourceARTElement = sourceARTElement;
    this.lastARTAbstractionElement = lastARTAbstractionElement;
    this.sourceTid = sourceTid;
    this.sourceEnvTransition = sourceEnvTransition;
    this.coveredBy = null;
    this.covers = new HashSet<RelyGuaranteeCFAEdgeTemplate>();
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

  /*public ARTElement getLastARTAbstractionElement() {
    return nextARTAbstractionElement;
  }*/

  public PathFormula getAbstractionFormula() {
    return abstractionFormula;
  }

  public ARTElement getLastARTAbstractionElement() {
    return lastARTAbstractionElement;
  }

  public IASTNode getRawAST() {
    return null;
  }

  public String getRawStatement() {
    return operation.getRawStatement();
  }

  public PathFormula getPathFormula() {
    return this.pathFormula;
  }


  @Override
  public String toString() {
    return "RG edge template  T:"+this.sourceTid+", source ART:"+this.sourceARTElement.getElementId()+", last abstr. ART:"+this.lastARTAbstractionElement.getElementId()+", operation:"+opPathFormula+", opVar:"+opVar+", path formula:"+this.pathFormula+", abstraction formula:"+this.abstractionFormula;
  }

  public CFAEdge getLocalEdge() {
    return this.operation;
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


  public CFAEdge getOperation() {
    return operation;
  }

  public PathFormula getOpPathFormula() {
    return opPathFormula;
  }

  public String getOpVar() {
    return opVar;
  }

  /**
   * Remember that environmental edge 'other' is more general than this one.
   * @param other
   */
  public void coveredBy(RelyGuaranteeCFAEdgeTemplate other) {

    assert other != null;
    assert other != this;
    assert !other.covers.contains(this);
    assert other.operation.equals(this.operation);
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

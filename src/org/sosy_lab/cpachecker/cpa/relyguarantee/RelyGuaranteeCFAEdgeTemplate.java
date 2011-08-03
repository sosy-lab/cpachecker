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
  private final PathFormulaWrapper pathFormulaWrapper;
  private final ARTElementWrapper sourceARTElementWrapper;
  private final int sourceTid;

  // environmental transition form which this edge was generated from
  private final RelyGuaranteeEnvironmentalTransition sourceEnvTransition;
  // unkilled env. edge that is more general than this one
  private RelyGuaranteeCFAEdgeTemplate coveredBy;
  // unkilled env. edges that are less general that this one
  private final List<RelyGuaranteeCFAEdgeTemplate> covers;


  public RelyGuaranteeCFAEdgeTemplate(CFAEdge pEdge, PathFormula pPathFormula, int sourceTid, ARTElement sourceARTElement, RelyGuaranteeEnvironmentalTransition sourceEnvTransition){
    this.localEdge = pEdge;
    this.pathFormulaWrapper = new PathFormulaWrapper(pPathFormula);
    this.sourceARTElementWrapper = new ARTElementWrapper(sourceARTElement);
    this.sourceTid = sourceTid;
    this.sourceEnvTransition = sourceEnvTransition;
    this.coveredBy = null;
    this.covers = new Vector<RelyGuaranteeCFAEdgeTemplate>();
  }
  // instantiate
  public RelyGuaranteeCFAEdge instantiate(){
    RelyGuaranteeCFAEdge edge = new RelyGuaranteeCFAEdge(this.localEdge, this.pathFormulaWrapper, this.sourceTid, this.sourceARTElementWrapper, this.sourceEnvTransition);
    return edge;
  }


  public ARTElement getSourceARTElement() {
    return this.sourceARTElementWrapper.artElement;
  }



  public IASTNode getRawAST() {
    return null;
  }

  public String getRawStatement() {
    return localEdge.getRawStatement();
  }

  public PathFormula getPathFormula() {
    return this.pathFormulaWrapper.pathFormula;
  }


  @Override
  public String toString() {
    return "RelyGuaranteeEnvEdgeTemplate from "+this.sourceTid+": "+localEdge.getRawStatement()+", by element id:"+this.pathFormulaWrapper.pathFormula+this.sourceARTElementWrapper.artElement.getElementId();
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


  public PathFormulaWrapper getPathFormulaWrapper() {
    return pathFormulaWrapper;
  }

  public ARTElementWrapper getSourceARTElementWrapper() {
    return sourceARTElementWrapper;
  }

  public RelyGuaranteeCFAEdgeTemplate getCoveredBy() {
    return coveredBy;
  }
  public List<RelyGuaranteeCFAEdgeTemplate> getCovers() {
    return covers;
  }
  public void setCoveredBy(RelyGuaranteeCFAEdgeTemplate pCoveredBy) {
    coveredBy = pCoveredBy;
  }
  /**
   * Remember that environmental edge 'other' is more general than this one.
   * @param other
   */
  public void coveredBy(RelyGuaranteeCFAEdgeTemplate other) {
    coveredBy  = other;
    other.covers.add(this);
  }


  /**
   * Called on a covered edge, which source element has been removed. The edge that covers this object will directly cover the elements
   * are covered by the object.
   */
  public void recoverChildren() {
    assert coveredBy != null;
    assert coveredBy.covers.contains(this);
    for ( RelyGuaranteeCFAEdgeTemplate child : covers){
      child.coveredBy(coveredBy);
    }
    coveredBy.covers.remove(this);
    covers.clear();
    assert !coveredBy.covers.contains(this);
    coveredBy = null;
  }



  /**
   * Wrapper around a path formula.
   */
  class PathFormulaWrapper{

    PathFormula pathFormula;

    public PathFormulaWrapper(PathFormula pf){
      this.pathFormula = pf;
    }

    public void setPathFormula(PathFormula pf){
      this.pathFormula = pf;
    }

    public PathFormula getPathFormula() {
      return pathFormula;
    }

  }

  /**
   * Wrapper around an ART element.
   */
  class ARTElementWrapper{

    ARTElement artElement;

    public ARTElementWrapper(ARTElement artElement){
      assert artElement != null;
      this.artElement = artElement;
    }

    public void setARTElement(ARTElement artElement){
      this.artElement = artElement;
    }

    public ARTElement getArtElement() {
      return artElement;
    }

    public void setArtElement(ARTElement pArtElement) {
      artElement = pArtElement;
    }
  }

}

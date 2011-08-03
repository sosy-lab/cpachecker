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
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

public class RelyGuaranteeCFAEdge implements CFAEdge{

  private CFAEdge localEdge;
  private final PathFormulaWrapper pathFormulaWrapper;
  private final ARTElementWrapper sourceARTElementWrapper;
  private CFANode predecessor;
  private CFANode successor;
  private final int sourceTid;

  // environmental transition form which this edge was generated from
  private final RelyGuaranteeEnvironmentalTransition sourceEnvTransition;

  // unkilled env. edge that is more general than this one
  private RelyGuaranteeCFAEdge coveredBy;
  // unkilled env. edges that are less general that this one
  private final Set<RelyGuaranteeCFAEdge> covers;




  /**
   *
   * @param edge local edge that created the environmental transition
   * @param abstractionFormula abstraction formula of the predecessor of the local edge
   * @param pathFormula path formula of the predecessor of the local edge
   */
  public RelyGuaranteeCFAEdge(CFAEdge pEdge, PathFormula pPathFormula, int sourceTid, ARTElement sourceARTElement, RelyGuaranteeEnvironmentalTransition sourceEnvTransition){
    this.localEdge = pEdge;
    this.pathFormulaWrapper = new PathFormulaWrapper(pPathFormula);
    this.sourceARTElementWrapper = new ARTElementWrapper(sourceARTElement);
    //this.pathFormula = pPathFormula;
    this.sourceTid = sourceTid;
    //this.sourceARTElement = sourceARTElement;
    this.sourceEnvTransition = sourceEnvTransition;
    this.coveredBy = null;
    this.covers = new HashSet<RelyGuaranteeCFAEdge>();
  }

  private RelyGuaranteeCFAEdge(CFAEdge pEdge, PathFormulaWrapper pathFormulaWrapper, int sourceTid, ARTElementWrapper sourceARTElementWrapper, RelyGuaranteeEnvironmentalTransition sourceEnvTransition){
    this.localEdge = pEdge;
    this.pathFormulaWrapper = pathFormulaWrapper;
    this.sourceARTElementWrapper = sourceARTElementWrapper;
    //this.pathFormula = pPathFormula;

    this.sourceTid = sourceTid;
    //this.sourceARTElement = sourceARTElement;
    this.sourceEnvTransition = sourceEnvTransition;
    this.coveredBy = null;
    this.covers = null;
  }


  public RelyGuaranteeCFAEdge makeCopy(){
    RelyGuaranteeCFAEdge copy = new RelyGuaranteeCFAEdge(this.localEdge, this.pathFormulaWrapper, this.sourceTid, this.sourceARTElementWrapper, this.sourceEnvTransition);
    return copy;
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

  public void addCovered(RelyGuaranteeCFAEdge covered){

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

  /**
   * Remember that environmental edge 'other' is more general than this one.
   * @param other
   */
  public void coveredBy(RelyGuaranteeCFAEdge other) {
    coveredBy  = other;
    other.covers.add(this);
  }

  /**
   * Promotes
   */
  public void recoverChildren() {


  }




  /**
   * Wrapper around a path formula.
   */
  class PathFormulaWrapper{

    private PathFormula pathFormula;

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

    private ARTElement artElement;

    public ARTElementWrapper(ARTElement artElement){
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

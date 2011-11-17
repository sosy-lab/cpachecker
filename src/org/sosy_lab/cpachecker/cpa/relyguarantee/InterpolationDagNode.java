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

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Represents a abstraction point.
 */
public class InterpolationDagNode {

  /** ART element, where the path formula was abstracted */
  protected final ARTElement artElement;

  /** Path formula */
  protected PathFormula pathFormula;

  /** Information on applied env. edges */
  protected final RelyGuaranteeApplicationInfo appInfo;

  /** Node directly affecting this node */
  protected final List<InterpolationDagNode> parents;

  /** Nodes directly affected by this node */
  protected final List<InterpolationDagNode> children;

  /** Thread id. */
  protected final int tid;

  protected final InterpolationDagNodeKey key;

  /**
   * Makes a deep copy of the node, except for children and parents.
   * @param node
   */
  public InterpolationDagNode(InterpolationDagNode node){
    this.artElement   = node.artElement;
    this.pathFormula  = node.pathFormula;
    this.appInfo      = node.appInfo;
    this.children     = new Vector<InterpolationDagNode>();
    this.parents      = new Vector<InterpolationDagNode>();
    this.tid          = node.tid;
    this.key          = node.key;
  }

  public InterpolationDagNode(ARTElement artElement, PathFormula pathFormula, RelyGuaranteeApplicationInfo appInfo, int tid){
    assert artElement   != null;
    assert pathFormula  != null;

    this.artElement   = artElement;
    this.pathFormula  = pathFormula;
    this.appInfo      = appInfo;
    this.children     = new Vector<InterpolationDagNode>();
    this.parents      = new Vector<InterpolationDagNode>();
    this.tid          = tid;
    this.key          = new InterpolationDagNodeKey(tid, artElement.getElementId());
  }

  /*public InterpolationDagNode(PathFormula pf, int traceNo, ARTElement artElement, List<InterpolationDagNode> children, List<InterpolationDagNode> parents, int tid) {
    super(pf, traceNo, artElement, null);
    this.children = children;
    this.parents  = parents;
    this.tid      = tid;
    this.envPrimes  = new HashMap<RelyGuaranteeCFAEdge, Integer>();
    this.key        = new InterpolationDagNodeKey(tid, artElement.getElementId());
  }

  public InterpolationDagNode(PathFormula pf, int traceNo, ARTElement artElement, List<InterpolationDagNode> children, List<InterpolationDagNode> parents, int tid, Map<RelyGuaranteeCFAEdge, Integer> envPrimes) {
    super(pf, traceNo, artElement, null);
    this.children = children;
    this.parents  = parents;
    this.tid      = tid;
    this.envPrimes  = envPrimes;
    this.key        = new InterpolationDagNodeKey(tid, artElement.getElementId());
  }*/

  public List<InterpolationDagNode> getChildren() {
    return children;
  }

  public List<InterpolationDagNode> getParents() {
    return parents;
  }

  public String toString() {
    RelyGuaranteeAbstractElement rgElement = AbstractElements.extractElementByType(artElement, RelyGuaranteeAbstractElement.class);
    return "ItpDagNode "+key.toString();
  }

  public int getTid() {
    return tid;
  }

  public InterpolationDagNodeKey getKey(){
    return key;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public void setPathFormula(PathFormula pPathFormula) {
    pathFormula = pPathFormula;
  }

  public RelyGuaranteeApplicationInfo getAppInfo() {
    return appInfo;
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  @Override
  public int hashCode(){
    return key.hashCode();
  }


}

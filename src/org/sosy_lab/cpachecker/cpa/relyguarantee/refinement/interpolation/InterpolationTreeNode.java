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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement.interpolation;

import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

import com.google.common.collect.ImmutableMap;

public class InterpolationTreeNode  {

  /** ART element, where the path formula was abstracted */
  protected final ARTElement artElement;

  /** Path formula */
  protected PathFormula pathFormula;

  /** Information on applied env. edges */
  protected final ImmutableMap<Integer, RGEnvTransition> envMap;

  /** Node directly affecting this node */
  protected  InterpolationTreeNode parent;

  public void setParent(InterpolationTreeNode pParent) {
    parent = pParent;
  }

  /** Nodes directly affected by this node */
  protected final Vector<InterpolationTreeNode> children;

  /** Thread id. */
  protected final int tid;

  /** Identifies the branch */
  protected final int uniqueId;

  /** Does it represent an ART abstraction element */
  protected boolean isARTAbstraction;

  /** Does it represent an env. abstraction */
  protected  boolean isEnvAbstraction;

  protected final InterpolationTreeNodeKey key;



  public InterpolationTreeNode(InterpolationDagNode node, int uniqueId) {
    this.artElement   = node.artElement;
    this.pathFormula  = node.pathFormula;
    this.envMap       = node.envAppMap;
    this.children     = new Vector<InterpolationTreeNode>();
    this.parent       = null;
    this.tid          = node.tid;
    this.uniqueId     = uniqueId;
    this.key          = new InterpolationTreeNodeKey(node.key, uniqueId);
    this.isARTAbstraction = true;
    this.isEnvAbstraction = false;
  }

  public InterpolationTreeNode(InterpolationDagNode node, int uniqueId, boolean isARTAbstraction, boolean isEnvAbstraction) {
    this.artElement   = node.artElement;
    this.pathFormula  = node.pathFormula;
    this.envMap       = node.envAppMap;
    this.children     = new Vector<InterpolationTreeNode>();
    this.parent       = null;
    this.tid          = node.tid;
    this.uniqueId     = uniqueId;
    this.key          = new InterpolationTreeNodeKey(node.key, uniqueId);
    this.isARTAbstraction = isARTAbstraction;
    this.isEnvAbstraction = isEnvAbstraction;
  }

  public InterpolationTreeNode(ARTElement artElement, PathFormula pathFormula, ImmutableMap<Integer, RGEnvTransition> envMap, int tid, int uniqueId, boolean isARTAbstraction, boolean isEnvAbstraction){
    assert artElement   != null;
    assert pathFormula  != null;

    this.artElement   = artElement;
    this.pathFormula  = pathFormula;
    this.envMap       = envMap;
    this.children     = new Vector<InterpolationTreeNode>();
    this.parent       = null;
    this.tid          = tid;
    this.key          = new InterpolationTreeNodeKey(tid, artElement.getElementId(), uniqueId);
    this.uniqueId     = uniqueId;
    this.isARTAbstraction = isARTAbstraction;
    this.isEnvAbstraction = isEnvAbstraction;
  }

  public int getUniqueId() {
    return uniqueId;
  }

  public InterpolationTreeNodeKey getKey(){
    return this.key;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public void setPathFormula(PathFormula pPathFormula) {
    pathFormula = pPathFormula;
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  public ImmutableMap<Integer, RGEnvTransition> getEnvMap() {
    return envMap;
  }

  public InterpolationTreeNode getParent() {
    return parent;
  }

  public Vector<InterpolationTreeNode> getChildren() {
    return children;
  }

  public int getTid() {
    return tid;
  }

  @Override
  public String toString(){
    CFANode loc = this.artElement.retrieveLocationElement().getLocationNode();
    return "itpTreeNode ("+tid+","+artElement.getElementId()+","+loc+", "+uniqueId+")";
  }

  public boolean isARTAbstraction() {
    return isARTAbstraction;
  }

  public void setARTAbstraction(boolean pIsARTAbstraction) {
    isARTAbstraction = pIsARTAbstraction;
  }

  public boolean isEnvAbstraction() {
    return isEnvAbstraction;
  }

  public void setEnvAbstraction(boolean pIsEnvAbstraction) {
    isEnvAbstraction = pIsEnvAbstraction;
  }

  @Override
  public int hashCode(){
    return this.key.hashCode();
  }


  /**
   * Returns true iff the argument is an ancestor of this node.
   * @param pNode
   * @return
   */
  public boolean hasAncestor(InterpolationTreeNode pNode) {
    assert pNode != null;
    InterpolationTreeNode ancestor = parent;

    while(ancestor != null && !ancestor.equals(pNode)){
      ancestor = ancestor.parent;
    }

    return ancestor != null;
  }

  /**
   * Finds the child that has the same branch id as the node;
   * returns null if it doesn't exist.
   * @return
   */
  public InterpolationTreeNode getLocalChild() {
    InterpolationTreeNode lChild = null;

    for (InterpolationTreeNode child : children){
      if (child.uniqueId == uniqueId){
        lChild = child;
        break;
      }
    }

    return lChild;
  }





}

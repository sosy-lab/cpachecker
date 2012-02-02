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
package org.sosy_lab.cpachecker.cpa.relyguarantee.refinement;

import java.util.Vector;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.RGApplicationInfo;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

public class InterpolationTreeNode  {

  /** ART element, where the path formula was abstracted */
  protected final ARTElement artElement;

  /** Path formula */
  protected PathFormula pathFormula;

  /** Information on applied env. edges */
  protected final RGApplicationInfo appInfo;

  /** Node directly affecting this node */
  protected  InterpolationTreeNode parent;

  public void setParent(InterpolationTreeNode pParent) {
    parent = pParent;
  }

  /** Nodes directly affected by this node */
  protected final Vector<InterpolationTreeNode> children;

  /** Thread id. */
  protected final Integer tid;

  /** Identifies the branch */
  protected final Integer uniqueId;

  /** Does it represent an ART abstraction element */
  protected boolean isARTAbstraction;

  /** Does it represent an env. abstraction */
  protected  boolean isEnvAbstraction;

  protected final InterpolationTreeNodeKey key;



  public InterpolationTreeNode(InterpolationDagNode node, int uniqueId) {
    this.artElement   = node.artElement;
    this.pathFormula  = node.pathFormula;
    this.appInfo      = node.appInfo;
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
    this.appInfo      = node.appInfo;
    this.children     = new Vector<InterpolationTreeNode>();
    this.parent       = null;
    this.tid          = node.tid;
    this.uniqueId     = uniqueId;
    this.key          = new InterpolationTreeNodeKey(node.key, uniqueId);
    this.isARTAbstraction = isARTAbstraction;
    this.isEnvAbstraction = isEnvAbstraction;
  }

  public InterpolationTreeNode(ARTElement artElement, PathFormula pathFormula, RGApplicationInfo appInfo, int tid, Integer uniqueId, boolean isARTAbstraction, boolean isEnvAbstraction){
    assert artElement   != null;
    assert pathFormula  != null;

    this.artElement   = artElement;
    this.pathFormula  = pathFormula;
    this.appInfo      = appInfo;
    this.children     = new Vector<InterpolationTreeNode>();
    this.parent       = null;
    this.tid          = tid;
    this.key          = new InterpolationTreeNodeKey(tid, artElement.getElementId(), uniqueId);
    this.uniqueId     = uniqueId;
    this.isARTAbstraction = isARTAbstraction;
    this.isEnvAbstraction = isEnvAbstraction;
  }

  public Integer getUniqueId() {
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

  public RGApplicationInfo getAppInfo() {
    return appInfo;
  }

  public InterpolationTreeNode getParent() {
    return parent;
  }

  public Vector<InterpolationTreeNode> getChildren() {
    return children;
  }

  public Integer getTid() {
    return tid;
  }

  public String toString(){
    return "itpTreeNode ("+tid+","+artElement.getElementId()+","+uniqueId+")";
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





}

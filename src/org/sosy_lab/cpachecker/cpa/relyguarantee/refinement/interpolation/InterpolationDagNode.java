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

import java.util.List;
import java.util.Vector;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions.RGEnvTransition;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

import com.google.common.collect.ImmutableMap;

/**
 * Represents an abstraction point.
 */
public class InterpolationDagNode{

  /** ART element, where the path formula was abstracted */
  protected final ARTElement artElement;

  /** Path formula */
  protected PathFormula pathFormula;

  /** Information on applied env. edges */
  protected final ImmutableMap<Integer, RGEnvTransition> envAppMap;

  /** Node directly affecting this node */
  protected final List<InterpolationDagNode> parents;

  /** Nodes directly affected by this node */
  protected final List<InterpolationDagNode> children;

  /** Thread id. */
  protected final int tid;

  /** Does it represent an env. abstraction */
  protected  boolean isEnvAbstraction;

  /** Unique id */
  protected Integer uniqueId;


  protected InterpolationDagNodeKey key;


  /**
   * Makes a deep copy of the node, except for children and parents.
   * @param node
   */
  public InterpolationDagNode(InterpolationDagNode node){
    this.artElement   = node.artElement;
    this.pathFormula  = node.pathFormula;
    this.envAppMap    = node.envAppMap;
    this.children     = new Vector<InterpolationDagNode>();
    this.parents      = new Vector<InterpolationDagNode>();
    this.tid          = node.tid;
    this.key          = node.key;
    this.isEnvAbstraction = node.isEnvAbstraction;
  }

  public InterpolationDagNode(ARTElement artElement, PathFormula pathFormula, ImmutableMap<Integer, RGEnvTransition> envAppMap, int tid){
    assert artElement   != null;
    assert pathFormula  != null;

    this.artElement   = artElement;
    this.pathFormula  = pathFormula;
    this.envAppMap    = envAppMap;
    this.children     = new Vector<InterpolationDagNode>();
    this.parents      = new Vector<InterpolationDagNode>();
    this.tid          = tid;
    this.key          = new InterpolationDagNodeKey(tid, artElement.getElementId());
    this.isEnvAbstraction = false;
  }

  public InterpolationDagNode(ARTElement artElement, PathFormula pathFormula, ImmutableMap<Integer, RGEnvTransition> envAppMap, int tid, boolean isEnvAbstraction, Integer uniqueId){
    assert artElement   != null;
    assert pathFormula  != null;

    this.artElement   = artElement;
    this.pathFormula  = pathFormula;
    this.envAppMap    = envAppMap;
    this.children     = new Vector<InterpolationDagNode>();
    this.parents      = new Vector<InterpolationDagNode>();
    this.tid          = tid;
    this.key          = new InterpolationDagNodeKey(tid, artElement.getElementId());
    this.isEnvAbstraction = isEnvAbstraction;
    this.uniqueId     = uniqueId;
  }


  public List<InterpolationDagNode> getChildren() {
    return children;
  }

  public List<InterpolationDagNode> getParents() {
    return parents;
  }

  @Override
  public String toString() {
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



  public ImmutableMap<Integer, RGEnvTransition> getEnvAppMap() {
    return envAppMap;
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  @Override
  public int hashCode(){
    return key.hashCode();
  }


  @Override
  public boolean equals(Object other){
    if (!(other instanceof InterpolationDagNode)){
      return false;
    };

    InterpolationDagNode oNode = (InterpolationDagNode) other;

    return key.equals(oNode.key);
  }




}

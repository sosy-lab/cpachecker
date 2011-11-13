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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Represents a formula block.
 */
public class InterpolationDagNode extends InterpolationBlock{

  protected final List<InterpolationDagNode> children;
  protected final List<InterpolationDagNode> parents;
  protected final int tid;
  protected final Map<RelyGuaranteeCFAEdge, Integer> envPrimes;
  protected final InterpolationDagNodeKey key;

  /**
   * Deep copy of the node, except for children and parents.
   * @param node
   */
  public InterpolationDagNode(InterpolationDagNode node){
    super(node.pathFormula, node.traceNo, node.artElement, null);
    this.children   = new Vector<InterpolationDagNode>();
    this.parents    = new Vector<InterpolationDagNode>();
    this.tid        = node.tid;
    this.envPrimes  = new HashMap<RelyGuaranteeCFAEdge, Integer>(node.envPrimes);
    this.key        = node.key;
  }

  public InterpolationDagNode(PathFormula pf, int traceNo, ARTElement artElement, int tid){
    super(pf, traceNo, artElement, null);
    this.children   = new Vector<InterpolationDagNode>();
    this.parents    = new Vector<InterpolationDagNode>();
    this.tid        = tid;
    this.envPrimes  = new HashMap<RelyGuaranteeCFAEdge, Integer>();
    this.key        = new InterpolationDagNodeKey(tid, artElement.getElementId());
  }


  public InterpolationDagNode(PathFormula pf, int traceNo, ARTElement artElement, List<InterpolationDagNode> children, List<InterpolationDagNode> parents, int tid) {
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
  }

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

  public Map<RelyGuaranteeCFAEdge, Integer> getEnvPrimes() {
    return envPrimes;
  }

  public InterpolationDagNodeKey getKey(){
    return key;
  }

  @Override
  public int hashCode(){
    return key.hashCode();
  }


}

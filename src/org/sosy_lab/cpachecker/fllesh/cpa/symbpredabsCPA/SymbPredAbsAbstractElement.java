/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.fllesh.cpa.symbpredabsCPA.util.symbpredabstraction.interfaces.AbstractFormula;

import com.google.common.base.Preconditions;

/**
 * AbstractElement for Symbolic Predicate Abstraction CPA
 *
 * @author Erkan
 */
public class SymbPredAbsAbstractElement implements AbstractElement, Partitionable {

  /** If the element is on an abstraction location */
  private final boolean isAbstractionNode;
  /** This is a pointer to the last abstraction node, when the computed abstract element
   * is an abstraction node, this node is set to the new abstraction node, otherwise it is
   * the same node with the last element's abstraction node  */
  private final CFANode abstractionLocation;
  /** The path formula for the path from the last abstraction node to this node.
   * it is set to true on a new abstraction location and updated with a new
   * non-abstraction location */
  private final PathFormula pathFormula;
  /** If this node is not an abstraction node, then this is invalid;
   * otherwise this is the {@link PathFormula} of the last element before the
   * abstraction is computed. This formula is used by the refinement procedure
   * to build the formula to the error location */
  private final PathFormula initAbstractionFormula;
  /** The abstraction which is updated only on abstraction locations */
  private AbstractFormula abstraction;

  /** The unique id for the abstraction */
  private final int abstractionId;
  
  private static int nextAbstractionId = 0;
  
  private final int sizeSinceAbstraction;

  /**
   * The abstract element this element was merged into.
   * Used for fast coverage checks.
   */
  private SymbPredAbsAbstractElement mergedInto = null;
  
  public static int INSTANCES = 0;
  
  public SymbPredAbsAbstractElement() {
    this.isAbstractionNode = false;
    this.abstractionLocation = null;
    this.pathFormula = null;
    this.initAbstractionFormula = null;
    this.abstraction = null;
    this.abstractionId = nextAbstractionId++;
    this.sizeSinceAbstraction = 0;
    
    INSTANCES++;
  }

  /**
   * Constructor for abstraction element.
   * @param abstLoc The CFANode where the abstraction took place.
   * @param pf  The new path formula.
   * @param initFormula The path formula before the abstraction.
   * @param a The abstraction.
   */
  public SymbPredAbsAbstractElement(CFANode abstLoc,
      PathFormula pf, PathFormula initFormula, AbstractFormula a){
    // set 'isAbstractionLocation' to true
    this.isAbstractionNode = true;
    this.abstractionLocation = abstLoc;
    this.pathFormula = pf;
    this.initAbstractionFormula = initFormula;
    this.abstraction = a;
    this.abstractionId = nextAbstractionId++;
    this.sizeSinceAbstraction = 0;
    
    INSTANCES++;
  }
  
  /**
   * Constructor for non-abstraction location.
   * @param abstLoc
   * @param pf
   * @param pfParentsList
   * @param initFormula
   * @param a
   * @param sizeSinceAbstraction
   */
  public SymbPredAbsAbstractElement(CFANode abstLoc,
      PathFormula pf, PathFormula initFormula, AbstractFormula a, int abstractionId,
      int sizeSinceAbstraction){
    this.isAbstractionNode = false;
    this.abstractionLocation = abstLoc;
    this.pathFormula = pf;
    this.initAbstractionFormula = initFormula;
    this.abstraction = a;
    this.abstractionId = abstractionId;
    this.sizeSinceAbstraction = sizeSinceAbstraction;
    
    INSTANCES++;
  }
  
  public AbstractFormula getAbstraction() {
    return abstraction;
  }

  public int getAbstractionId() {
    return abstractionId;
  }

  public CFANode getAbstractionLocation() {
    return abstractionLocation;
  }

  public PathFormula getInitAbstractionFormula() {
    return initAbstractionFormula;
  }

  SymbPredAbsAbstractElement getMergedInto() {
    return mergedInto;
  }
  
  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public int getSizeSinceAbstraction() {
    return sizeSinceAbstraction;
  }

  public boolean isAbstractionNode(){
    return isAbstractionNode;
  }

  public void setAbstraction(AbstractFormula pAbstraction) {
    abstraction = pAbstraction;
  }

  void setMergedInto(SymbPredAbsAbstractElement pMergedInto) {
    Preconditions.checkNotNull(pMergedInto);
    mergedInto = pMergedInto;
  }

  @Override
  public String toString() {
    return "Abstraction location: " + isAbstractionNode
        + " Abstraction id: " + abstractionId + " Symbolic Formula: " + pathFormula.hashCode();
  }
  
  @Override
  public Object getPartitionKey() {
    if (isAbstractionNode) {
      // all abstraction nodes are in one block (for coverage checks)
      return null;
    } else {
      return abstractionId;
    }
  }
}

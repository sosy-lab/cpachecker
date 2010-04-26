/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.AbstractFormula;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;

/**
 * AbstractElement for Symbolic Predicate Abstraction CPA
 *
 * @author Erkan
 */
public class SymbPredAbsAbstractElement implements AbstractElement {

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
  /** If this node is not and abstraction node, then this is invalid; 
   * otherwise this is the {@link PathFormula} of the last element before the 
   * abstraction is computed. This formula is used by the refinement procedure 
   * to build the formula to the error location */
  private final PathFormula initAbstractionFormula;
  /** The abstraction which is updated only on abstraction locations */
  private AbstractFormula abstraction;
  /** List of abstraction locations with the order of their computation
   * up to that point. We use this list in {@link SymbPredAbsMergeOperator#merge(AbstractElement, AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision)} and
   * for partial order operator*/
  private final ImmutableList<CFANode> abstractionPathList;
  /** List of {@link CFANode} that we constructed the {@link PathFormula}. This is
   * updated if {@link SymbPredAbsMergeOperator#merge(AbstractElement, AbstractElement, org.sosy_lab.cpachecker.core.interfaces.Precision)}
   * is called and the {@link PathFormula} is updated. This list is also used by 
   * {@link SymbPredAbsAbstractElement#equals(Object)} to make a fast, syntactic check on 
   * equality of formula*/
  private final ImmutableSet<CFANode> pfParents;
  
  private final int sizeSinceAbstraction;
  
  public int getSizeSinceAbstraction() {
    return sizeSinceAbstraction;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public boolean isAbstractionNode(){
    return isAbstractionNode;
  }

  public AbstractFormula getAbstraction() {
    return abstraction;
  }

  public void setAbstraction(AbstractFormula pAbstraction) {
    abstraction = pAbstraction;
  }
  
  public ImmutableList<CFANode> getAbstractionPathList() {
    return abstractionPathList;
  }

  public CFANode getAbstractionLocation() {
    return abstractionLocation;
  }

  public SymbPredAbsAbstractElement() {
    this.isAbstractionNode = false;
    this.abstractionLocation = null;
    this.pathFormula = null;
    this.pfParents = null;
    this.initAbstractionFormula = null;
    this.abstraction = null;
    this.abstractionPathList = null;
    this.sizeSinceAbstraction = 0;
  }
  
  /**
   * Constructor for non-abstraction location.
   * @param abstLoc
   * @param pf
   * @param pfParentsList
   * @param initFormula
   * @param a
   * @param pl
   * @param sizeSinceAbstraction
   */
  public SymbPredAbsAbstractElement(CFANode abstLoc,
      PathFormula pf, ImmutableSet<CFANode> pfParentsList, PathFormula initFormula, AbstractFormula a, 
      ImmutableList<CFANode> pl, int sizeSinceAbstraction){
    this.isAbstractionNode = false;
    this.abstractionLocation = abstLoc;
    this.pathFormula = pf;
    this.pfParents = pfParentsList;
    this.initAbstractionFormula = initFormula;
    this.abstraction = a;
    this.abstractionPathList = pl;
    this.sizeSinceAbstraction = sizeSinceAbstraction;
  }
  
  /**
   * Constructor for abstraction element.
   * @param abstLoc The CFANode where the abstraction took place.
   * @param pf  The new path formula.
   * @param initFormula The path formula before the abstraction.
   * @param a The abstraction.
   * @param oldAbstractionPathList The old abstraction path.
   */
  public SymbPredAbsAbstractElement(CFANode abstLoc,
      PathFormula pf, PathFormula initFormula, AbstractFormula a, 
      ImmutableList<CFANode> oldAbstractionPathList){
    // set 'isAbstractionLocation' to true
    this.isAbstractionNode = true;
    this.abstractionLocation = abstLoc;
    this.pathFormula = pf;
    // 'pfParents' is not instantiated for abstraction locations
    this.pfParents = null;
    this.initAbstractionFormula = initFormula;
    this.abstraction = a;
    
    // add the new abstraction location to the abstractionPath
    ImmutableList.Builder<CFANode> newAbstractionPath = ImmutableList.builder();
    newAbstractionPath.addAll(oldAbstractionPathList);
    newAbstractionPath.add(abstLoc);
    this.abstractionPathList = newAbstractionPath.build();
    
    this.sizeSinceAbstraction = 0;
  }

  /*@Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    
    } else if (o == null || !(o instanceof SymbPredAbsAbstractElement)) {
      return false;
    
    } else{
      SymbPredAbsAbstractElement other = (SymbPredAbsAbstractElement)o;

      if ((this.isAbstractionNode != other.isAbstractionNode)
          || !this.getAbstractionPathList().equals(other.getAbstractionPathList())) {
        return false;
      }
      
      if (isAbstractionNode()) {
        // if this is an abstraction location

        // if the abstraction formulas are same, we return true
        // TODO note: if this is called before an abstraction is computed
        // it might be buggy because initAbstractionFormula is used to
        // compute abstraction and we don't check if they are equal
        // ** initAbstractionFormula cannot be different though, we have the same
        // AbstractionPathList
        return this.getAbstraction().equals(other.getAbstraction());
        
      } else{
        // if this is not an abstraction location

        // we check if this element and the other element has the same 
        // PathFormulas. We can do this by comparing pfParents because
        // since two elements have the same abstraction path list PathFormulas
        // of two elements are same if they are constructed by same edges

        return this.getAbstraction().equals(other.getAbstraction())
            && this.getPfParents().equals(other.getPfParents());
      }
    }
  }*/

  @Override
  public String toString() {
    return "Abstraction location: " + isAbstractionNode
        + " Abstraction path: " + abstractionPathList;
  }

  /*@Override
  public int hashCode() {
    return abstractionPathList.hashCode()
      + 17 * abstraction.hashCode()
      + 23 * pathFormula.hashCode();
  }*/

  public PathFormula getInitAbstractionFormula() {
    return initAbstractionFormula;
  }
  
  public ImmutableSet<CFANode> getPfParents() {
    assert !isAbstractionNode : "abstraction nodes have no pathformula parents";
    return pfParents;
  }

  @Override
  public boolean isError() {
    return false;
  }
}

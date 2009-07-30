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
package cpa.symbpredabsCPA;

import java.util.List;

import symbpredabstraction.AbstractionPathList;
import symbpredabstraction.PathFormula;
import cfa.objectmodel.CFAErrorNode;
import cfa.objectmodel.CFANode;
import cpa.common.interfaces.AbstractDomain;
import cpa.common.interfaces.AbstractElement;
import cpa.symbpredabs.AbstractFormula;
import cpa.symbpredabs.PredicateMap;
import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.mathsat.BDDAbstractFormula;

/**
 * AbstractElement for Symbolic Predicate Abstraction CPA
 *
 * @author Erkan
 */
public class SymbPredAbsAbstractElement
implements AbstractElement {

  private SymbPredAbsAbstractDomain domain;

  /** Unique state id */
  private int elementId;
  /** If the element is on an abstraction location */
  private boolean isAbstractionNode = false;
  /** This is a pointer to the last abstraction node, when the computed abstract element
   * is an abstraction node, this node is set to the new abstraction node, otherwise it is
   * the same node with the last element's abstraction node  */
  private CFANode abstractionLocation;
  /** The path formula for the path from the last abstraction node to this node. 
   * it is set to true on a new abstraction location and updated with a new 
   * non-abstraction location */
  private PathFormula pathFormula;
  /** If this node is not and abstraction node, then this is invalid; 
   * otherwise this is the {@link PathFormula} of the last element before the 
   * abstraction is computed. This formula is used by the refinement procedure 
   * to build the formula to the error location */
  private PathFormula initAbstractionFormula;
  /** The abstraction which is updated only on abstraction locations */
  private AbstractFormula abstraction;
  /** List of abstraction locations with the order of their computation
   * up to that point. We use this list in {@link SymbPredAbsMergeOperator#merge(AbstractElement, AbstractElement, cpa.common.interfaces.Precision)} and
   * for partial order operator*/
  private AbstractionPathList abstractionPathList;
  /** Parent of this element in the ART */
  private SymbPredAbsAbstractElement artParent;
  /** Current predicates used to compute abstraction on this location */
  private PredicateMap predicates;
  /** List of {@link CFANode} ids that we constructed the {@link PathFormula}. This is
   * updated if {@link SymbPredAbsMergeOperator#merge(AbstractElement, AbstractElement, cpa.common.interfaces.Precision)}
   * is called and the {@link PathFormula} is updated. This list is also used by 
   * {@link SymbPredAbsAbstractElement#equals(Object)} to make a fast, syntactic check on 
   * equality of formula*/
  private List<Integer> pfParents;

  private SSAMap maxIndex;

  private static int nextAvailableId = 1;

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public boolean isAbstractionNode(){
    return isAbstractionNode;
  }

  public AbstractFormula getAbstraction() {
    return abstraction;
  }

  public void setAbstraction(AbstractFormula abs) {
    abstraction = abs;
  }
  public void setPathFormula(PathFormula pf){
    pathFormula = pf;
  }

  public AbstractionPathList getAbstractionPathList() {
    return abstractionPathList;
  }

  /**
   * @param otherElement
   * @return true if otherElement is descendant of this on ART
   */
  public boolean isDescendant(SymbPredAbsAbstractElement otherElement) {
    SymbPredAbsAbstractElement a = this;
    while (a != null) {
      if (a.equals(otherElement)) return true;
      a = a.getArtParent();
    }
    return false;
  }

  public CFANode getAbstractionLocation(){
    return abstractionLocation;
  }

  public void setAbstractionLocation(CFANode absLoc){
    abstractionLocation = absLoc;
  }

  public SymbPredAbsAbstractElement(){

  }

  public SymbPredAbsAbstractElement(AbstractDomain d, boolean isAbstractionElement, CFANode abstLoc,
      PathFormula pf, List<Integer> pfParentsList, PathFormula initFormula, AbstractFormula a, 
      AbstractionPathList pl, SymbPredAbsAbstractElement artParent, PredicateMap pmap){
    this.elementId = nextAvailableId++;
    this.domain = (SymbPredAbsAbstractDomain)d;
    this.isAbstractionNode = isAbstractionElement;
    this.abstractionLocation = abstLoc;
    this.pathFormula = pf;
    this.pfParents = pfParentsList;
    this.initAbstractionFormula = initFormula;
    this.abstraction = a;
    this.abstractionPathList = pl;
    this.artParent = artParent;
    this.predicates = pmap;
    this.maxIndex = new SSAMap();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }

    else if(elementId == ((SymbPredAbsAbstractElement)o).elementId){
      return true;
    }

    else{
      SymbPredAbsAbstractElement thisElement = this;
      SymbPredAbsAbstractElement otherElement = (SymbPredAbsAbstractElement)o;

      // if this is not an abstraction location
      if(!thisElement.isAbstractionNode()){
        // we check if this element and the other element has the same
        // elements on the AbstractionPathList
        if(thisElement.getAbstractionPathList().equals(otherElement.getAbstractionPathList())){
          // we check if this element and the other element has the same 
          // PathFormulas. We can do this by comparing pfParents because
          // since two elements have the same abstraction path list PathFormulas
          // of two elements are same if they are constructed by same edges
          List<Integer> thisList = thisElement.getPfParents();
          List<Integer> otherList = otherElement.getPfParents();

          if(thisList.size() != otherList.size()){
            return false;
          }

          for(int par: thisList){
            if(!otherList.contains(par)){
              return false;
            }
          }
          return true;
        }
        return false;
      }
      // if this is an abstraction location
      else{
        assert(thisElement.getAbstraction() != null);
        assert(otherElement.getAbstraction() != null);
        // we check if this element and the other element has the same
        // elements on the AbstractionPathList
        if(!thisElement.getAbstractionPathList().equals(otherElement.getAbstractionPathList())){
          return false;
        }
        else{
          // if they have the same abstraction path and if the abstraction formulas
          // are same, we return true
          // TODO note: if this is called before an abstraction is computed
          // it might be buggy because initAbstractionFormula is used to
          // compute abstraction and we don't check if they are equal
          // ** initAbstractionFormula cannot be different though, we have the same
          // AbstractionPathList
          return thisElement.getAbstraction().equals(otherElement.getAbstraction());
        }
      }
    }
  }

  @Override
  public String toString() {
    BDDAbstractFormula abst = (BDDAbstractFormula)getAbstraction();
    SymbolicFormula  symbReprAbst = null;
    if(abst != null){
      symbReprAbst = domain.getCPA().getAbstractFormulaManager().toConcrete(domain.getCPA().getSymbolicFormulaManager(), abst);
    }
    return
    " Abstraction LOCATION: " + getAbstractionLocation() + ((getAbstractionLocation() instanceof CFAErrorNode) ? " {ERROR NODE}" : "") +
//  " PF: "+ getPathFormula().getSymbolicFormula() +
//  " Abstraction: " + symbReprAbst  +
//  " Init Formula--> " + (getInitAbstractionFormula() != null ? getInitAbstractionFormula().getSymbolicFormula() : "null")  +
//  " Parents --> " + abstractionPathList + 
    " ART Parent --> " + (getArtParent() != null ? getArtParent().getAbstractionLocation().toString() : "NULL")+ 
    "";
    //+ ">(" + Integer.toString(getId()) + ")"
  }

  @Override
  public int hashCode() {
    return elementId;
  }

  public PredicateMap getPredicates() {
    return predicates;
  }

  public void setPredicates(PredicateMap predicates) {
    this.predicates = predicates;
  }

  public void setParents(AbstractionPathList parents2) {
    abstractionPathList = parents2;
  }

  public PathFormula getInitAbstractionFormula() {
    return initAbstractionFormula;
  }

  public void setInitAbstractionFormula(PathFormula initFormula) {
    this.initAbstractionFormula = initFormula;
  }

  public SymbPredAbsAbstractElement getArtParent() {
    return this.artParent;
  }

  public void setArtParent(SymbPredAbsAbstractElement artParent) {
    this.artParent = artParent;
  }

  public void updateMaxIndex(SSAMap ssa) {
    assert(maxIndex != null);
    for (String var : ssa.allVariables()) {
      int i = ssa.getIndex(var);
      int i2 = maxIndex.getIndex(var);
      maxIndex.setIndex(var, Math.max(i, i2));
    }
  }

  public SSAMap getMaxIndex() {
    return maxIndex;
  }

  public void setMaxIndex(SSAMap maxIndex) {
    this.maxIndex = maxIndex;
  }

  public List<Integer> getPfParents() {
    return pfParents;
  }

  public void setPfParents(List<Integer> pfParents) {
    this.pfParents = pfParents;
  }
}

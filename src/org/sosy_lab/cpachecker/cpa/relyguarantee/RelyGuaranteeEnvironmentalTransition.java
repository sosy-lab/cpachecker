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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.util.AbstractElements;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

/**
 * Stores information about environmental transition
 */
public class RelyGuaranteeEnvironmentalTransition {

  private final Formula formula;
  private final PathFormula pathFormula;
  private final CFAEdge edge;
  private final int sourceThread;
  private final ARTElement sourceARTElement;

 /* public RelyGuaranteeEnvironmentalTransition (Formula formula, PathFormula pathFormula, CFAEdge edge, int sourceThread) {
    this.formula = formula;
    this.pathFormula = pathFormula;
    this.edge = edge;
    this.sourceThread = sourceThread;
  }*/

  // TODO some type-check would be good
  public RelyGuaranteeEnvironmentalTransition(ARTElement aElement, CFAEdge edge, int tid) {
    CompositeElement cElement = (CompositeElement)  aElement.getWrappedElement();
    CFANode node = cElement.retrieveLocationElement().getLocationNode();
    RelyGuaranteeAbstractElement predElement = AbstractElements.extractElementByType(cElement, RelyGuaranteeAbstractElement.class);

    this.formula = predElement.getAbstractionFormula().asFormula();
    this.pathFormula = predElement.getPathFormula();
    this.edge = edge;
    this.sourceThread = tid;
    this.sourceARTElement = aElement;
  }

  public Formula getFormula() {
    return formula;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public CFAEdge getEdge() {
    return edge;
  }

  public int getSourceThread() {
    return sourceThread;
  }

  public ARTElement getSourceARTElement() {
    return sourceARTElement;
  }

  public String toString() {
    return "RelyGuaranteeEnvironemtalTransition from "+this.sourceThread+": "+edge.getRawStatement()+",'"+this.formula+"','"+this.pathFormula+"'";
  }

  // returns true if 'other' is syntactially equivalent to this env transtion
  public boolean equals(RelyGuaranteeEnvironmentalTransition other){
    if (! this.getEdge().equals(other.getEdge())) {
      return false;
    }
    if (! this.formula.equals(other.formula)){
      return false;
    }
    if (! this.pathFormula.equals(other.pathFormula)){
      return false;
    }
    /*if (! this.sourceARTElement.equals(other.sourceARTElement)){
      return false;
    }*/
    return true;
  }

}

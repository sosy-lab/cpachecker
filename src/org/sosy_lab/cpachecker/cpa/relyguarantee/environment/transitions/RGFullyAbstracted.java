/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.relyguarantee.environment.transitions;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;

/**
 * Environmental transitions with a fully abstracted precondition and operation.
 */
public class RGFullyAbstracted implements RGEnvTransition {

  /** Formula over plain and hashed values that abstracts the operation */
  private final Formula abstractTransition;
  /** BDD representation of abstractTransition */
  private final Region  abstractTransitionRegion;
  /** SSA map at the point where the concrete operation was applied */
  private final SSAMap  lowSSA;
  /** SSA map after applying the concrete operation */
  private final SSAMap  highSSA;
  /** ART element created by the concrete operation */
  private final ARTElement targetARTElement;
  /** ART element where the concrete operation was applied */
  private final ARTElement sourceARTElement;
  /** Operation that generated the transition */
  private final CFAEdge operation;
  /** Source thred's id */
  private final int tid;
  /** Additional formula used for the abstraction of this transition. */
  private final PathFormula formulaForAbstraction;
  /** ART elements that generated this transition */
  private ImmutableSet<ARTElement> generatingARTElement;

  public RGFullyAbstracted(Formula abstractTransition, Region abstractTransitionRegion, SSAMap lowSSA, SSAMap highSSA, ARTElement sourceARTElement, ARTElement targetARTElement,  CFAEdge op, int tid, PathFormula abstractionFormula){
    assert !sourceARTElement.isDestroyed();

    this.abstractTransition = abstractTransition;
    this.abstractTransitionRegion = abstractTransitionRegion;
    this.lowSSA  = lowSSA;
    this.highSSA = highSSA;
    this.sourceARTElement = sourceARTElement;
    this.targetARTElement = targetARTElement;
    this.operation = op;
    this.tid = tid;
    this.formulaForAbstraction = abstractionFormula;

    Set<ARTElement> generating = new HashSet<ARTElement>(2);
    generating.add(sourceARTElement);
    generating.add(targetARTElement);
    this.generatingARTElement = ImmutableSet.copyOf(generating);
  }

  @Override
  public RGEnvTransitionType getRGType() {
    return RGEnvTransitionType.FullyAbstracted;
  }

  public Formula getAbstractTransition() {
    return abstractTransition;
  }

  public Region getAbstractTransitionRegion() {
    return abstractTransitionRegion;
  }

  public SSAMap getLowSSA() {
    return lowSSA;
  }

  public SSAMap getHighSSA() {
    return highSSA;
  }

  @Override
  public ARTElement getAbstractionElement() {
    return sourceARTElement;
  }

  @Override
  public String toString(){
    return "fa: "+abstractTransition+", "+sourceARTElement.retrieveLocationElement().getLocationNode()+"->"+targetARTElement.retrieveLocationElement().getLocationNode();
  }

  @Override
  public int getTid() {
    return tid;
  }

  @Override
  public ImmutableCollection<ARTElement> getGeneratingARTElements() {
    return generatingARTElement;
  }

  @Override
  public ARTElement getSourceARTElement() {
    return sourceARTElement;
  }

  @Override
  public ARTElement getTargetARTElement() {
    return targetARTElement;
  }

  @Override
  public CFAEdge getOperation() {
    return operation;
  }

  @Override
  public PathFormula getFormulaAddedForAbstraction() {
    return formulaForAbstraction;
  }



}

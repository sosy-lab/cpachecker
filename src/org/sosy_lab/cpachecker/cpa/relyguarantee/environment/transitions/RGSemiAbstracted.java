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

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;

/**
 * Environmental transitions with an abstracted precondition only.
 */
public class RGSemiAbstracted implements RGEnvTransition{

  /** Abstracted precondition - valuation at the source element */
  private final Formula abstractPrecondition;
  /** Abstracted precondition as a region */
  private final Region abstractPreconditionRegion;
  /** SSA at the source element */
  private final SSAMap ssa;
  /** The operation */
  private final CFAEdge operation;
  /** ART element created by the concrete ooperation */
  private final ARTElement targetARTElement;
  /** ART element where the concrete operation was applied */
  private final ARTElement sourceARTElement;
  /** Source thred's id */
  private final int tid;
  /** Additional formula used for the abstraction of this transition - should be true */
  private final PathFormula formulaForAbstraction;
  /** ART elements that generated this transition */
  private ImmutableSet<ARTElement> generatingARTElement;


  public RGSemiAbstracted(Formula precondition, Region preconditionRegion, SSAMap ssa, CFAEdge operation, ARTElement sourceElem, ARTElement targetElem, int tid, PathFormula formulaForAbs){
    assert !sourceElem.isDestroyed();
    assert formulaForAbs.getFormula().isTrue();

    this.abstractPrecondition = precondition;
    this.abstractPreconditionRegion = preconditionRegion;
    this.ssa = ssa;
    this.operation = operation;
    this.sourceARTElement = sourceElem;
    this.targetARTElement = targetElem;
    this.formulaForAbstraction = formulaForAbs;
    this.tid = tid;

    this.generatingARTElement = ImmutableSet.of(sourceARTElement, targetARTElement);
  }

  @Override
  public RGEnvTransitionType getRGType() {
    return RGEnvTransitionType.SemiAbstracted;
  }

  @Override
  public ARTElement getAbstractionElement() {
    return sourceARTElement;
  }

  @Override
  public int getTid() {
    return tid;
  }

  public Formula getAbstractPrecondition() {
    return abstractPrecondition;
  }

  public Region getAbstractPreconditionRegion() {
    return abstractPreconditionRegion;
  }

  public SSAMap getSsa() {
    return ssa;
  }

  @Override
  public CFAEdge getOperation() {
    return operation;
  }

  @Override
  public String toString(){
    return "sa: "+operation.getRawStatement()+", "+abstractPrecondition+", "+sourceARTElement.getLocationClasses()+"->"+targetARTElement.getLocationClasses();
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
  public PathFormula getFormulaAddedForAbstraction() {
    return formulaForAbstraction;
  }



}

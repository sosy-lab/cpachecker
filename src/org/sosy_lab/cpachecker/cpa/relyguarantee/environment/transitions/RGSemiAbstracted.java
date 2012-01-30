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
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

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
  /** ART element where the operation was applied */
  private final ARTElement sourceARTElement;
  /** Source thred's id */
  private final int tid;

  public RGSemiAbstracted(Formula precondition, Region preconditionRegion, SSAMap ssa, CFAEdge operation, ARTElement sourceElem, int tid){
    this.abstractPrecondition = precondition;
    this.abstractPreconditionRegion = preconditionRegion;
    this.ssa = ssa;
    this.operation = operation;
    this.sourceARTElement = sourceElem;
    this.tid = tid;
  }

  @Override
  public RGEnvTransitionType getRGType() {
    return RGEnvTransitionType.SemiAbstracted;
  }

  @Override
  public ARTElement getSourceARTElement() {
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

  public CFAEdge getOperation() {
    return operation;
  }

  public String toString(){
    return "sa: "+operation.getRawStatement()+", "+abstractPrecondition;
  }

}

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
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

/**
 * Environmental transitions with precondition and operation that were not abstracted.
 */
public class RGSimpleTransition implements RGEnvTransition{

  /** Abstraction formula at the point where the transition was generated.  */
  private final Formula abstraction;
  /** Last abstraction formula unprimed and as a region. */
  private final Region abstractionRegion;
  /** Path formula at the point where the transition was generated. */
  private final PathFormula pf;
  /** The operation */
  private final CFAEdge operation;
  /** Last ART element that was an abstraction point before the operatino was applied.  */
  private final ARTElement sourceARTElement;
  /** Source thread's id */
  private final int tid;

  public RGSimpleTransition(Formula abstraction, Region abstractionReg, PathFormula pf, CFAEdge operation, ARTElement abstractionARTElem, int tid){
    this.abstraction = abstraction;
    this.abstractionRegion = abstractionReg;
    this.pf = pf;
    this.operation = operation;
    this.sourceARTElement = abstractionARTElem;
    this.tid = tid;
  }

  @Override
  public RGEnvTransitionType getRGType() {
    return RGEnvTransitionType.SimpleTransition;
  }

  @Override
  public ARTElement getSourceARTElement() {
    return sourceARTElement;
  }

  @Override
  public int getTid() {
    return tid;
  }


  public Region getAbstractionRegion() {
    return abstractionRegion;
  }

  public Formula getAbstraction() {
    return abstraction;
  }

  public PathFormula getPathFormula() {
    return pf;
  }

  public CFAEdge getOperation() {
    return operation;
  }

  public String toString() {
    return "st: "+operation.getRawStatement()+", "+abstraction+", "+pf;
  }


}

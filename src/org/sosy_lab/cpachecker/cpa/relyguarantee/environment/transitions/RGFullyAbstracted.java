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

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

/**
 * Environmental transitions with a fully abstracted precondition and operation.
 */
public class RGFullyAbstracted implements RGEnvTransition {

  /** Formula over plain and hashed values that abstracts the transition */
  private final Formula abstractTransition;
  /** BDD representation of abstractTransition */
  private final Region  abstractTransitionRegion;
  /** SSA map at the point where the concrete transition was applied */
  private final SSAMap  lowSSA;
  /** SSA map after applying the concrete transition */
  private final SSAMap  highSSA;
  /** ART element created by the concrete transition */
  private final ARTElement sourceARTElement;
  /** Source thred's id */
  private final int tid;

  public RGFullyAbstracted(Formula abstractTransition, Region abstractTransitionRegion, SSAMap lowSSA, SSAMap highSSA, ARTElement sourceARTElement, int tid){
    this.abstractTransition = abstractTransition;
    this.abstractTransitionRegion = abstractTransitionRegion;
    this.lowSSA  = lowSSA;
    this.highSSA = highSSA;
    this.sourceARTElement = sourceARTElement;
    this.tid = tid;
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
  public ARTElement getSourceARTElement() {
    return sourceARTElement;
  }

  @Override
  public String toString(){
    return "fa: "+abstractTransition;
  }

  @Override
  public int getTid() {
    return tid;
  }

}

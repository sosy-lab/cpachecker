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

import java.util.Set;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Describes a formula for interpolation and its scopes. If the formula contains traces from other thread,
 * then the formula might have many scope - one per thread.
 */
class InterpolationBlock {

  private Set<InterpolationBlockScope> scope;
  private PathFormula pf;

  public InterpolationBlock(PathFormula pf, Set<InterpolationBlockScope> scope){
    this.pf = pf;
    this.scope = scope;
  }

  public Set<InterpolationBlockScope> getScope() {
    return scope;
  }

  public PathFormula getPathFormula() {
    return pf;
  }

  public void setScope(Set<InterpolationBlockScope> pScope) {
    scope = pScope;
  }

  public void setPathFormula(PathFormula pPf) {
    pf = pPf;
  }

  @Override
  public String toString(){
    return "InterpolationBlock: "+pf+" "+scope;
  }
}

/**
 * Class describes last abstraction element for a trace and how many times the formula from this trace are primed.
 */
class InterpolationBlockScope {

  private int primedNo;

  private final ARTElement artElement;

  public InterpolationBlockScope(int primedNo, ARTElement artElement){
    this.primedNo = primedNo;
    this.artElement = artElement;
  }

  public void setPrimedNo(int pPrimedNo) {
    primedNo = pPrimedNo;
  }

  public int getPrimedNo() {
    return primedNo;
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  @Override
  public String toString() {
    return primedNo+"-id:"+artElement.getElementId();
  }

}
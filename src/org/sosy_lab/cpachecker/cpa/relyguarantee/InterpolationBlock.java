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

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Contains three pieces of information:
 * - path formula for interpolation
 * - ART element that is the abstraction point for the formula
 * - primed no which identified formula trace.
 */
class InterpolationBlock {

  private final int primedNo;
  private final ARTElement artElement;
  private final PathFormula pf;

  public InterpolationBlock(PathFormula pf, int primedNo, ARTElement artElement){
    this.pf = pf;
    this.primedNo = primedNo;
    this.artElement = artElement;
  }



  public int getPrimedNo() {
    return primedNo;
  }



  public ARTElement getArtElement() {
    return artElement;
  }



  public PathFormula getPathFormula() {
    return pf;
  }


  @Override
  public String toString(){
    return "["+primedNo+"- id:"+artElement.getElementId()+"]: "+pf;
  }
}
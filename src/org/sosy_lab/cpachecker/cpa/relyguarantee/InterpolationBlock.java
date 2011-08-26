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

import java.util.Deque;

import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Contains four pieces of information:
 * - path formula for interpolation,
 * - ART element that is the abstraction point for the formula,
 * - trace no which identifies formula trace,
 * - no of the outer trace, if any
 */
class InterpolationBlock {

  // traceNo is the lowest number of primes in the path formula - it identifies the formula trace
  private final int traceNo;
  // context is the sequence of trace no. trace that encapsulated the current trance
  private final Deque<Integer> context;
  // ART element, where the path formula was abstracted
  private final ARTElement artElement;
  private final PathFormula pathFormula;

  public InterpolationBlock(PathFormula pf, int primedNo, ARTElement artElement, Deque<Integer> context){
    assert context != null;
    this.pathFormula = pf;
    this.traceNo = primedNo;
    this.artElement = artElement;
    this.context = context;
  }




  public Deque<Integer> getContext() {
    return context;
  }

  public int getTraceNo() {
    return traceNo;
  }

  public ARTElement getArtElement() {
    return artElement;
  }

  public PathFormula getPathFormula() {
    return pathFormula;
  }

  @Override
  public String toString(){
    return "[tn:"+traceNo+", c:"+context+", id:"+artElement.getElementId()+"]: "+pathFormula;
  }
}
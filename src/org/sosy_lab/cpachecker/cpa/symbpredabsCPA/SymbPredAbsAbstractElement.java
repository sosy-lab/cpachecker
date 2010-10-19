/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.symbpredabsCPA;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.util.assumptions.Assumption;
import org.sosy_lab.cpachecker.util.symbpredabstraction.Abstraction;
import org.sosy_lab.cpachecker.util.symbpredabstraction.PathFormula;
import org.sosy_lab.cpachecker.util.symbpredabstraction.interfaces.SymbolicFormula;

import com.google.common.base.Preconditions;

/**
 * AbstractElement for Symbolic Predicate Abstraction CPA
 *
 * @author Erkan
 */
public class SymbPredAbsAbstractElement implements AbstractElement, Partitionable {

  /** If the element is on an abstraction location */
  private final boolean isAbstractionNode;

  /** The path formula for the path from the last abstraction node to this node.
   * it is set to true on a new abstraction location and updated with a new
   * non-abstraction location */
  private final PathFormula pathFormula;

  /** The abstraction which is updated only on abstraction locations */
  private Abstraction abstraction;
  
  /**
   * The abstract element this element was merged into.
   * Used for fast coverage checks.
   */
  private SymbPredAbsAbstractElement mergedInto = null;
  
  /**
   * The assumption that is used to compute this state
   * */
  private Assumption assumption;
  
  SymbPredAbsAbstractElement() {
    this.isAbstractionNode = false;
    this.pathFormula = null;
    this.abstraction = null;
  }

  public SymbPredAbsAbstractElement(boolean isAbstractionNode, PathFormula pf, Abstraction a) {
    this.isAbstractionNode = isAbstractionNode;
    this.pathFormula = pf;
    this.abstraction = a;
  }
  
  public Abstraction getAbstraction() {
    return abstraction;
  }

  SymbPredAbsAbstractElement getMergedInto() {
    return mergedInto;
  }
  
  public PathFormula getPathFormula() {
    return pathFormula;
  }

  public boolean isAbstractionNode(){
    return isAbstractionNode;
  }

  public void setAbstraction(Abstraction pAbstraction) {
    abstraction = pAbstraction;
  }

  void setMergedInto(SymbPredAbsAbstractElement pMergedInto) {
    Preconditions.checkNotNull(pMergedInto);
    mergedInto = pMergedInto;
  }

  public Assumption getAssumption() {
    return assumption;
  }

  public void setAssumption(Assumption pAssumption) {
    assumption = pAssumption;
  }
  
  @Override
  public String toString() {
    return "Abstraction location: " + isAbstractionNode
        + " Abstraction: " + abstraction;
  }
  
  @Override
  public Object getPartitionKey() {
    if (isAbstractionNode) {
      // all abstraction nodes are in one block (for coverage checks)
      return null;
    } else {
      return abstraction;
    }
  }
}

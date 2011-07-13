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
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;

/**
 * Stores information about environmental transition
 */
public class RelyGuaranteeEnvironmentalTransition {

  private Formula formula;
  private PathFormula pathFormula;
  private CFAEdge edge;
  private int sourceThread;

  public RelyGuaranteeEnvironmentalTransition (Formula formula, PathFormula pathFormula, CFAEdge edge, int sourceThread) {
    this.formula = formula;
    this.pathFormula = pathFormula;
    this.edge = edge;
    this.sourceThread = sourceThread;
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

  public String toString() {
    return "RelyGuaranteeEnvironemtalTransition from "+this.sourceThread+": "+edge.getRawStatement()+",'"+this.formula+"','"+this.pathFormula+"'";
  }

}

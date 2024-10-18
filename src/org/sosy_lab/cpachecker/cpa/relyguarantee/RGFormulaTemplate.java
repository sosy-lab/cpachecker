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

import java.util.List;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;

/**
 * Information about environmental transitions that appear in the path formula.
 */
public class RGFormulaTemplate {



  // path formula with environmental parts removed - SSA index like in the ordniary path formula
  private final PathFormula localPathFormula;
  // environmental edge together with the  path formula to which it was applied
  private final List<Pair<RGCFAEdge2, PathFormula>> envTransitionApplied;

  public RGFormulaTemplate(PathFormula localPathFormula,  List<Pair<RGCFAEdge2, PathFormula>> envTransitionApplied) {
    this.localPathFormula = localPathFormula;
    this.envTransitionApplied = envTransitionApplied;
  }


  public RGFormulaTemplate(PathFormula localPathFormula) {
    this.localPathFormula = localPathFormula;
    this.envTransitionApplied = new Vector<Pair<RGCFAEdge2, PathFormula>>();
  }


 public PathFormula getLocalPathFormula() {
   return localPathFormula;
 }

 public List<Pair<RGCFAEdge2, PathFormula>> getEnvTransitionApplied() {
   return envTransitionApplied;
 }

 @Override
 public String toString(){
   return "template '"+localPathFormula+"' with "+envTransitionApplied.size()+" env edges";
 }
}
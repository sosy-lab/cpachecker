/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
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
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction;

import common.Pair;

import cpa.symbpredabs.SSAMap;
import cpa.symbpredabs.SymbolicFormula;
import cpa.symbpredabs.SymbolicFormulaManager;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormula;
import cpa.symbpredabs.mathsat.MathsatSymbolicFormulaManager;

public class PathFormula {

  SSAMap ssa;
  SymbolicFormula symbolicFormula;

  public PathFormula(SymbolicFormula pf, SSAMap newssa){
    ssa = newssa;
    symbolicFormula = pf;
  }

  public SSAMap getSsa() {
    return ssa;
  }

  public void setSsa(SSAMap ssa) {
    this.ssa = ssa;
  }

  public SymbolicFormula getSymbolicFormula() {
    return symbolicFormula;
  }

  public PathFormula getInitSymbolicFormula(SymbolicFormulaManager mgr, boolean replace) {
    SSAMap ssa = new SSAMap();
    SymbolicFormula f = mgr.makeFalse();
    Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mp =
      mgr.mergeSSAMaps(ssa, this.getSsa(), false);
    SymbolicFormula curf = this.getSymbolicFormula();
    // TODO modified if
    if (replace) {
      curf = ((MathsatSymbolicFormulaManager)mgr).replaceAssignments((MathsatSymbolicFormula)curf);
    }
    f = mgr.makeAnd(f, mp.getFirst().getFirst());
    curf = mgr.makeAnd(curf, mp.getFirst().getSecond());
    f = mgr.makeOr(f, curf);
    ssa = mp.getSecond();
    return new PathFormula(f,ssa);
  }

  public void setSymbolicFormula(SymbolicFormula symFormula) {
    this.symbolicFormula = symFormula;
  }

  @Override
  public String toString(){
    return symbolicFormula.toString();
  }
  
  @Override
  public int hashCode() {
    return ssa.hashCode() * 17 + symbolicFormula.hashCode();
  }

}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.invariants.balancer;

import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.sosy_lab.cpachecker.util.invariants.Coeff;
import org.sosy_lab.cpachecker.util.invariants.balancer.interfaces.MatrixI;
import org.sosy_lab.cpachecker.util.invariants.interfaces.VariableManager;
import org.sosy_lab.cpachecker.util.invariants.templates.TemplateFormula;


public abstract class FormulaMatriciser {

  public abstract MatrixI buildMatrix(TemplateFormula t, VariableManager vmgr, Map<String, Variable> paramVars, boolean prependTrue);

  protected static List<RationalFunction> makeRationalFunctions(List<Coeff> clist, Map<String, Variable> paramVars) {
    List<RationalFunction> rfs = new Vector<>(clist.size());
    for (Coeff c : clist) {
      rfs.add(c.makeRationalFunction(paramVars));
    }
    return rfs;
  }

  /**
   * @param P A list of coefficients
   * @return The list of all passed coefficients negated
   */
  protected static List<Coeff> negative(List<Coeff> P) {
    Vector<Coeff> N = new Vector<>();
    Coeff C;
    for (int i = 0; i < P.size(); i++) {
      C = P.get(i);
      N.add(C.negative());
    }
    return N;
  }

}

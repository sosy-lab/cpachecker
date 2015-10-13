/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.solver.smtInterpol;

import org.sosy_lab.solver.api.Formula;
import org.sosy_lab.solver.api.FormulaType;
import org.sosy_lab.solver.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.solver.basicimpl.AbstractArrayFormulaManager;

import de.uni_freiburg.informatik.ultimate.logic.Sort;
import de.uni_freiburg.informatik.ultimate.logic.Term;


class SmtInterpolArrayFormulaManager
    extends AbstractArrayFormulaManager<Term, Sort, SmtInterpolEnvironment> {

  private final SmtInterpolEnvironment env;

  SmtInterpolArrayFormulaManager(SmtInterpolFormulaCreator pCreator) {
    super(pCreator);
    env = pCreator.getEnv();
  }

  @Override
  protected Term select(Term pArray, Term pIndex) {
    return env.term("select", pArray, pIndex);
  }

  @Override
  protected Term store(Term pArray, Term pIndex, Term pValue) {
    return env.term("store", pArray, pIndex, pValue);
  }

  @Override
  protected <TI extends Formula, TE extends Formula> Term internalMakeArray(
      String pName, FormulaType<TI> pIndexType, FormulaType<TE> pElementType) {

    final ArrayFormulaType<TI, TE> arrayFormulaType = FormulaType.getArrayType(
        pIndexType, pElementType);
    final Sort smtInterpolArrayType = toSolverType(arrayFormulaType);

    return getFormulaCreator().makeVariable(smtInterpolArrayType, pName);
  }

  @Override
  protected Term equivalence(Term pArray1, Term pArray2) {
    return env.term("=", pArray1, pArray2);
  }
}

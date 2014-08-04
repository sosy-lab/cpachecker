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
package org.sosy_lab.cpachecker.util.predicates.z3;

import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;

public class Z3RationalFormulaManager extends Z3NumeralFormulaManager<NumeralFormula, RationalFormula> {

  public Z3RationalFormulaManager(
          Z3FormulaCreator pCreator,
          Z3FunctionFormulaManager pFunctionManager) {
    super(pCreator, pFunctionManager, RationalFormula.class);
  }

  @Override
  public FormulaType<RationalFormula> getFormulaType() {
    return FormulaType.RationalType;
  }

  @Override
  protected long getNumeralType() {
    return getFormulaCreator().getRealType();
  }
}

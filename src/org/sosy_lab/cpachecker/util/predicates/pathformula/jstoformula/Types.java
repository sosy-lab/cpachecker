/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.pathformula.jstoformula;

import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class Types {
  static final FormulaType<IntegerFormula> SCOPE_TYPE = FormulaType.IntegerType;
  static final ArrayFormulaType<IntegerFormula, IntegerFormula> SCOPE_STACK_TYPE =
      FormulaType.getArrayType(FormulaType.IntegerType, SCOPE_TYPE);
  static final FormulaType<IntegerFormula> VARIABLE_TYPE = FormulaType.IntegerType;
  static final FormulaType<IntegerFormula> JS_TYPE_TYPE = FormulaType.IntegerType;
  static final FloatingPointType NUMBER_TYPE = FormulaType.getDoublePrecisionFloatingPointType();
  static final FormulaType<BooleanFormula> BOOLEAN_TYPE = FormulaType.BooleanType;
  static final FormulaType<IntegerFormula> FUNCTION_TYPE = FormulaType.IntegerType;
  static final FormulaType<IntegerFormula> FUNCTION_DECLARATION_TYPE = FormulaType.IntegerType;
  static final FormulaType<IntegerFormula> STRING_TYPE = FormulaType.IntegerType;
}

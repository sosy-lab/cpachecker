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
package org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;

import com.google.common.base.Function;


/**
 * A BaseFormulaManager because all Abstract*FormulaManager-Classes wrap a FormulaCreator-instance.
 * @param <TFormulaInfo> the solver specific type.
 */
abstract class AbstractBaseFormulaManager<TFormulaInfo, TType, TEnv> {

  private final FormulaCreator<TFormulaInfo, TType, TEnv> formulaCreator;

  final Function<Formula, TFormulaInfo> extractor =
      new Function<Formula, TFormulaInfo>() {
        @Override
        public TFormulaInfo apply(Formula pInput) {
          return extractInfo(pInput);
        }
      };

  AbstractBaseFormulaManager(
          FormulaCreator<TFormulaInfo, TType, TEnv> pFormulaCreator) {
    this.formulaCreator = pFormulaCreator;
  }

  protected final FormulaCreator<TFormulaInfo, TType, TEnv> getFormulaCreator() {
    return formulaCreator;
  }

  final TFormulaInfo extractInfo(Formula pBits) {
    return getFormulaCreator().extractInfo(pBits);
  }

  final BooleanFormula wrapBool(TFormulaInfo pTerm) {
    return getFormulaCreator().encapsulateBoolean(pTerm);
  }

  protected final TType toSolverType(FormulaType<?> formulaType) {
    TType t;
    if (formulaType.isBooleanType()) {
      t = getFormulaCreator().getBoolType();
    } else if (formulaType.isIntegerType()) {
      t = getFormulaCreator().getIntegerType();
    } else if (formulaType.isRationalType()) {
      t = getFormulaCreator().getRationalType();
    } else if (formulaType.isBitvectorType()) {
      FormulaType.BitvectorType bitPreciseType = (FormulaType.BitvectorType) formulaType;
      t = getFormulaCreator().getBitvectorType(bitPreciseType.getSize());
    } else if (formulaType.isFloatingPointType()) {
      FormulaType.FloatingPointType fpType = (FormulaType.FloatingPointType)formulaType;
      t = getFormulaCreator().getFloatingPointType(fpType);
    } else if (formulaType.isArrayType()) {
      FormulaType.ArrayFormulaType<?, ?> arrType = (FormulaType.ArrayFormulaType<?, ?>)formulaType;
      TType indexType = toSolverType(arrType.getIndexType());
      TType elementType = toSolverType(arrType.getElementType());
      t = getFormulaCreator().getArrayType(indexType, elementType);
    } else {
      throw new IllegalArgumentException("Not supported interface");
    }
    return t;
  }
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Objects;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BitvectorFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class ModelValue {

  private final String variableName;

  private final FormulaType<?> formulaType;

  private final Number value;

  public ModelValue(String pVariableName, FormulaType<?> pFormulaType, Number pValue) {
    variableName = pVariableName;
    formulaType = pFormulaType;
    value = pValue;
  }

  public String getVariableName() {
    return variableName;
  }

  public FormulaType<?> getFormulaType() {
    return formulaType;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.format("%s = <%s> %s", variableName, formulaType, value);
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof ModelValue) {
      ModelValue other = (ModelValue) pOther;
      return variableName.equals(other.variableName)
          && value.equals(other.value)
          && formulaType.equals(other.formulaType);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableName, value, formulaType);
  }

  public BooleanFormula toAssignment(FormulaManagerView pFMGR) {
    BooleanFormulaManager bfmgr = pFMGR.getBooleanFormulaManager();
    BitvectorFormulaManager bvm = pFMGR.getBitvectorFormulaManager();
    FloatingPointFormulaManager fpm = pFMGR.getFloatingPointFormulaManager();

    if (formulaType.isIntegerType()) {
      IntegerFormulaManagerView ifm = pFMGR.getIntegerFormulaManager();
      IntegerFormula intVariable = ifm.makeVariable(variableName);
      IntegerFormula intValue;
      if (value instanceof BigInteger) {
        intValue = ifm.makeNumber((BigInteger) value);
      } else {
        intValue = ifm.makeNumber(value.longValue());
      }
      return ifm.equal(intVariable, intValue);
    } else if (formulaType instanceof BitvectorType) {
      BitvectorFormula bvVariable = bvm.makeVariable((BitvectorType) formulaType, variableName);
      BitvectorFormula bvValue;
      if (value instanceof BigInteger) {
        bvValue = bvm.makeBitvector(bvm.getLength(bvVariable), (BigInteger) value);
      } else {
        bvValue = bvm.makeBitvector(bvm.getLength(bvVariable), value.longValue());
      }
      return bvm.equal(bvVariable, bvValue);
    } else if (formulaType instanceof FloatingPointType) {
      FloatingPointFormula fpVariable = fpm.makeVariable(variableName, (FloatingPointType) formulaType);
      FormulaType.FloatingPointType fpType = (FormulaType.FloatingPointType) pFMGR.getFormulaType(fpVariable);
      FloatingPointFormula fpValue;
      if (value instanceof BigDecimal) {
        fpValue = fpm.makeNumber((BigDecimal) value, fpType);
      } else if (value instanceof Double) {
        fpValue = fpm.makeNumber((Double) value, fpType);
      } else if (value instanceof Float) {
        fpValue = fpm.makeNumber((Float) value, fpType);
      } else if (value instanceof Rational) {
        fpValue = fpm.makeNumber((Rational) value, fpType);
      } else {
        fpValue = fpm.makeNumber(value.toString(), fpType);
      }
      return fpm.assignment(fpVariable, fpValue);
    }
    return bfmgr.makeTrue();
  }
}
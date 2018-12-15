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
package org.sosy_lab.cpachecker.cpa.hybrid.abstraction;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.hybrid.value.CompositeValue;
import org.sosy_lab.cpachecker.cpa.hybrid.value.StringValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Base class for transformation of hybrid values into assumptions
 * @param <T> the type to return on transformation
 */
public abstract class HybridValueTransformer<T, TDependentObj> {

  protected final CBinaryExpressionBuilder binaryExpressionBuilder;

  protected HybridValueTransformer(MachineModel pMachineModel, LogManager pLogger) {
    this.binaryExpressionBuilder = new CBinaryExpressionBuilder(pMachineModel, pLogger);
  }

  public abstract T transform(Value pValue, TDependentObj pObj, BinaryOperator pOperator)
    throws InvalidAssumptionException;

  protected CExpression transform(CompositeValue pValue) {
    return null; //TODO
  }

  protected CExpression transform(NumericValue pNumericValue) {
    
    // check different Numeric types
    final float value = pNumericValue.floatValue();
    if(value == (int)value) {
      // the numeric value is actually an integer
      return CIntegerLiteralExpression.createDummyLiteral(pNumericValue.longValue(), CNumericTypes.INT);

    } else {
      return CFloatLiteralExpression.createDummyLiteral(pNumericValue.floatValue(), CNumericTypes.FLOAT);
    }
  }

  protected CExpression transform(BooleanValue pBooleanValue) {
    final long boolAsInt = pBooleanValue.isTrue() ? 1L : 0L;
    return CIntegerLiteralExpression.createDummyLiteral(boolAsInt, CNumericTypes.BOOL); 
  }

  protected CExpression transform(StringValue pStringValue) {
    return new CStringLiteralExpression(FileLocation.DUMMY, CPointerType.POINTER_TO_CHAR, pStringValue.getValue());
  }

}
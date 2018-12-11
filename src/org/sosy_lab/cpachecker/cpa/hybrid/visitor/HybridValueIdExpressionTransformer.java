/*
 *  CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.hybrid.visitor;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueTransformer;
import org.sosy_lab.cpachecker.cpa.hybrid.exception.InvalidAssumptionException;
import org.sosy_lab.cpachecker.cpa.hybrid.value.CompositeValue;
import org.sosy_lab.cpachecker.cpa.hybrid.value.StringValue;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NullValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * A hybrid value transformer for non deterministic function assignments to variable
 */
public class HybridValueIdExpressionTransformer
    extends HybridValueTransformer<CExpression, CIdExpression> {

  public HybridValueIdExpressionTransformer(
      MachineModel pMachineModel,
      LogManager pLogger) {
    super(pMachineModel, pLogger);
  }

  /**
   * @param pValue The value to transform
   * @param pCIdExpression The variable expression to assign the given value to
   * @exception AssertionError Thrown if 
   */
  @Override
  public CExpression transform(Value pValue, CIdExpression pCIdExpression, BinaryOperator pOperator) 
    throws InvalidAssumptionException{

    CExpression rightHandSide = null;

    if(pValue instanceof NumericValue) {
      rightHandSide = transform((NumericValue)pValue);
    }
    else if(pValue instanceof BooleanValue) {
      rightHandSide = transform((BooleanValue)pValue);
    }
    else if(pValue instanceof StringValue) {
      rightHandSide = transform((StringValue)pValue);
    }
    else if(pValue instanceof CompositeValue) {
      rightHandSide = transform((CompositeValue)pValue);
    }
    else if(pValue instanceof NullValue) {
      rightHandSide = transform((NullValue)pValue);
    }

    if(rightHandSide == null) {
      // this should never happen
      throw new InvalidAssumptionException("Unable to create assumption for Hybrid Value with given variable and operator.");
    }

    // this is safe to call
    return binaryExpressionBuilder.buildBinaryExpressionUnchecked(pCIdExpression, rightHandSide, pOperator);
  }

  


}

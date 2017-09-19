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
package org.sosy_lab.cpachecker.cpa.value;

import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.value.type.FunctionValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.states.PointerToMemoryLocation;


public class FunctionPointerExpressionValueVisitor extends ExpressionValueVisitor {

  public FunctionPointerExpressionValueVisitor(ValueAnalysisState pState, String pFunctionName,
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    super(pState, pFunctionName, pMachineModel, pLogger);
  }

 @Override
public MemoryLocation evaluateMemoryLocation(CExpression lValue) throws UnrecognizedCCodeException {
   return lValue.accept(new FunctionPointerMemoryLocationEvaluator(this));
 }

 @Override
public MemoryLocation evaluateRelativeMemLocForStructMember(MemoryLocation pStartLocation,
     String pMemberName, CCompositeType pStructType) throws UnrecognizedCCodeException {

   MemoryLocationEvaluator locationEvaluator = new FunctionPointerMemoryLocationEvaluator(this);

   return locationEvaluator.getStructureFieldLocationFromRelativePoint(
       pStartLocation, pMemberName, pStructType);
 }

 @Override
public MemoryLocation evaluateMemLocForArraySlot(
     final MemoryLocation pArrayStartLocation,
     final int pSlotNumber,
     final CArrayType pArrayType) {
   MemoryLocationEvaluator locationEvaluator = new FunctionPointerMemoryLocationEvaluator(this);

   return locationEvaluator.getArraySlotLocationFromArrayStart(pArrayStartLocation, pSlotNumber, pArrayType);
 }

 @Override
 public Value visit(CUnaryExpression unaryExpression) throws UnrecognizedCCodeException {

   final UnaryOperator unaryOperator = unaryExpression.getOperator();
   final CExpression unaryOperand = unaryExpression.getOperand();

   if (unaryOperator == UnaryOperator.AMPER && unaryOperand.getExpressionType() instanceof CFunctionType) {
     return new FunctionValue(unaryOperand.toString());
   }

   return super.visit(unaryExpression);
 }

 private static class FunctionPointerMemoryLocationEvaluator extends MemoryLocationEvaluator {

   public FunctionPointerMemoryLocationEvaluator(ExpressionValueVisitor pEvv) {
     super(pEvv);
   }

   @Override
   public MemoryLocation visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {

     CType expType = pIastFieldReference.getExpressionType();
     if (expType instanceof CPointerType) {
       if (((CPointerType) expType).getType() instanceof CFunctionType) {
         return PointerToMemoryLocation.valueOf(pIastFieldReference.getFieldName());
       }
     }

     return super.visit(pIastFieldReference);
   }
 }
}
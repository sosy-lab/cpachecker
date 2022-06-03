// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class FunctionPointerExpressionValueVisitor extends ExpressionValueVisitor {

  public FunctionPointerExpressionValueVisitor(
      ValueAnalysisState pState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    super(pState, pFunctionName, pMachineModel, pLogger);
  }

  @Override
  public MemoryLocation evaluateMemoryLocation(CExpression lValue)
      throws UnrecognizedCodeException {
    return lValue.accept(new FunctionPointerMemoryLocationEvaluator(this));
  }

  @Override
  public MemoryLocation evaluateRelativeMemLocForStructMember(
      MemoryLocation pStartLocation, String pMemberName, CCompositeType pStructType)
      throws UnrecognizedCodeException {

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

    return locationEvaluator.getArraySlotLocationFromArrayStart(
        pArrayStartLocation, pSlotNumber, pArrayType);
  }

  @Override
  public Value visit(CUnaryExpression unaryExpression) throws UnrecognizedCodeException {

    final UnaryOperator unaryOperator = unaryExpression.getOperator();
    final CExpression unaryOperand = unaryExpression.getOperand();

    if (unaryOperator == UnaryOperator.AMPER
        && unaryOperand.getExpressionType() instanceof CFunctionType) {
      return new FunctionValue(unaryOperand.toString());
    }

    return super.visit(unaryExpression);
  }

  private static class FunctionPointerMemoryLocationEvaluator extends MemoryLocationEvaluator {

    public FunctionPointerMemoryLocationEvaluator(ExpressionValueVisitor pEvv) {
      super(pEvv);
    }

    @Override
    public MemoryLocation visit(CFieldReference pIastFieldReference)
        throws UnrecognizedCodeException {

      CType expType = pIastFieldReference.getExpressionType();
      if (expType instanceof CPointerType) {
        if (((CPointerType) expType).getType() instanceof CFunctionType) {
          return MemoryLocation.forIdentifier(pIastFieldReference.getFieldName());
        }
      }

      return super.visit(pIastFieldReference);
    }
  }
}

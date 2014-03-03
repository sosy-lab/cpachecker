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
package org.sosy_lab.cpachecker.cpa.value;

import java.util.List;

import javax.annotation.Nullable;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;


/**
 * This Visitor returns the value from an expression.
 * The result may be null, i.e., the value is unknown.
 */
public class ExpressionValueVisitor extends AbstractExpressionValueVisitor {

  private boolean missingPointer = false;

  private final ValueAnalysisState state;

  /** This Visitor returns the numeral value for an expression.
   * @param pState where to get the values for variables (identifiers)
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   * @param pEdge only for logging, not needed */
  public ExpressionValueVisitor(ValueAnalysisState pState, String pFunctionName,
      MachineModel pMachineModel, LogManager pLogger, @Nullable CFAEdge pEdge) {
    super(pFunctionName, pMachineModel, pLogger, pEdge);
    this.state = pState;
  }

  /* additional methods */

  @Override
  public void reset() {
    super.reset();
    missingPointer = false;
  }

  public boolean hasMissingPointer() {
    return missingPointer;
  }

  @Override
  protected Value evaluateCIdExpression(CIdExpression varName) {
    return evaluateAIdExpression(varName);
  }

  @Override
  protected Long evaluateJIdExpression(JIdExpression varName) {
    // return evaluateAIdExpression(varName); TODO implement
    return null;
  }

  /** This method returns the value of a variable from the current state. */
  private Value evaluateAIdExpression(AIdExpression varName) {

    MemoryLocation memLoc;

    if (!ForwardingTransferRelation.isGlobal(varName)) {
      memLoc = MemoryLocation.valueOf(getFunctionName(), varName.getName(), 0);
    } else {
      memLoc = MemoryLocation.valueOf(varName.getName(), 0);
    }

    if (state.contains(memLoc)) {
      return state.getValueFor(memLoc);
    } else {
      return new SymbolicValueFormula(new SymbolicValueFormula.SymbolicValue(memLoc));
    }
  }

  private Value evaluateLValue(CLeftHandSide pLValue) throws UnrecognizedCCodeException {

    MemoryLocation varLoc = evaluateMemoryLocation(pLValue);

    if (varLoc == null) {
      return Value.UnknownValue.getInstance();
    }

    if (getState().contains(varLoc)) {
      return state.getValueFor(varLoc);
    } else {
      return Value.UnknownValue.getInstance();
    }
  }

  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue) throws UnrecognizedCCodeException {
    return evaluateLValue(pLValue);
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue) throws UnrecognizedCCodeException {
    return evaluateLValue(pLValue);
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pVarName) {
    missingPointer = true;
    return Value.UnknownValue.getInstance();
  }

  public boolean canBeEvaluated(CExpression lValue) throws UnrecognizedCCodeException {
    return lValue.accept(new MemoryLocationEvaluator(this)) != null;
  }

  public MemoryLocation evaluateMemoryLocation(CExpression lValue) throws UnrecognizedCCodeException {
    return lValue.accept(new MemoryLocationEvaluator(this));
  }

  private static class MemoryLocationEvaluator extends DefaultCExpressionVisitor<MemoryLocation, UnrecognizedCCodeException> {

    private final ExpressionValueVisitor evv;

    public MemoryLocationEvaluator(ExpressionValueVisitor pEvv) {
      evv = pEvv;
    }

    @Override
    protected MemoryLocation visitDefault(CExpression pExp) throws UnrecognizedCCodeException {
      return null;
    }

    @Override
    public MemoryLocation visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws UnrecognizedCCodeException {

      CExpression arrayExpression = pIastArraySubscriptExpression.getArrayExpression();

      CType arrayExpressionType = arrayExpression.getExpressionType().getCanonicalType();

      /* A subscript Expression can also include an Array Expression.
      In that case, it is a dereference*/
      if (arrayExpressionType instanceof CPointerType) {
        evv.missingPointer = true;
        return null;
      }

      CExpression subscript = pIastArraySubscriptExpression.getSubscriptExpression();

      CType elementType = pIastArraySubscriptExpression.getExpressionType();

      MemoryLocation arrayLoc = arrayExpression.accept(this);

      if (arrayLoc == null) {
        return null;
      }

      Long subscriptValue = subscript.accept(evv).asLong(subscript.getExpressionType());

      if(subscriptValue == null) {
        return null;
      }

      long typeSize = evv.getSizeof(elementType);

      long subscriptOffset = subscriptValue * typeSize;

      if (arrayLoc.isOnFunctionStack()) {

        return MemoryLocation.valueOf(arrayLoc.getFunctionName(),
            arrayLoc.getIdentifier(),
            subscriptOffset);
      } else {

        return MemoryLocation.valueOf(arrayLoc.getIdentifier(),
            subscriptOffset);
      }
    }

    @Override
    public MemoryLocation visit(CFieldReference pIastFieldReference) throws UnrecognizedCCodeException {

      if (pIastFieldReference.isPointerDereference()) {
        evv.missingPointer = true;
        return null;
      }

      CLeftHandSide fieldOwner = (CLeftHandSide) pIastFieldReference.getFieldOwner();

      MemoryLocation memLocOfFieldOwner = fieldOwner.accept(this);

      if (memLocOfFieldOwner == null) {
        return null;
      }

      CType ownerType = fieldOwner.getExpressionType().getCanonicalType();
      String fieldName = pIastFieldReference.getFieldName();

      Integer offset = getFieldOffset(ownerType, fieldName);

      if(offset == null) {
        return null;
      }

      if (memLocOfFieldOwner.isOnFunctionStack()) {

        return MemoryLocation.valueOf(memLocOfFieldOwner.getFunctionName(),
            memLocOfFieldOwner.getIdentifier(),
            memLocOfFieldOwner.getOffset() + offset);
      } else {

        return MemoryLocation.valueOf(memLocOfFieldOwner.getIdentifier(),
            offset + memLocOfFieldOwner.getOffset());
      }
    }

    private Integer getFieldOffset(CType ownerType, String fieldName) throws UnrecognizedCCodeException {

      if (ownerType instanceof CElaboratedType) {
        return getFieldOffset(((CElaboratedType) ownerType).getRealType(), fieldName);
      } else if (ownerType instanceof CCompositeType) {
        return getFieldOffset((CCompositeType) ownerType, fieldName);
      } else if (ownerType instanceof CPointerType) {
        evv.missingPointer = true;
        return null;
      }

      throw new AssertionError();
    }

    private Integer getFieldOffset(CCompositeType ownerType, String fieldName) throws UnrecognizedCCodeException {

      List<CCompositeTypeMemberDeclaration> membersOfType = ownerType.getMembers();

      int offset = 0;

      for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
        String memberName = typeMember.getName();

        if (memberName.equals(fieldName)) {
          return offset;
        }

        if (!(ownerType.getKind() == ComplexTypeKind.UNION)) {

          CType fieldType = typeMember.getType().getCanonicalType();

          offset = (int) (offset + evv.getSizeof(fieldType));
        }
      }

      return null;
    }

    @Override
    public MemoryLocation visit(CIdExpression idExp) throws UnrecognizedCCodeException {

      boolean isGlobal = ForwardingTransferRelation.isGlobal(idExp);

      if(isGlobal) {
        return MemoryLocation.valueOf(idExp.getName(), 0);
      } else {
        return MemoryLocation.valueOf(evv.getFunctionName(), idExp.getName(), 0);
      }
    }

    @Override
    public MemoryLocation visit(CPointerExpression pPointerExpression) throws UnrecognizedCCodeException {
      evv.missingPointer = true;
      return null;
    }

    @Override
    public MemoryLocation visit(CCastExpression pE) throws UnrecognizedCCodeException {
      // TODO reinterpretations for ValueAnalysis
      return pE.getOperand().accept(this);
    }
  }

  public ValueAnalysisState getState() {
    return state;
  }
}

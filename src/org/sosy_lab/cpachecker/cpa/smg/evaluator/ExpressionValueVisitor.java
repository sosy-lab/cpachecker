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
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression.TypeIdOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * This class evaluates expressions that evaluate not to a
 * pointer, array, struct or union type.
 * The result of this evaluation is a {@link SMGSymbolicValue}.
 * The value represents a symbolic value of the SMG.
 *
 */
public class ExpressionValueVisitor extends DefaultCExpressionVisitor<SMGValueAndStateList, CPATransferException>
  implements CRightHandSideVisitor<SMGValueAndStateList, CPATransferException> {

  protected final SMGExpressionEvaluator smgExpressionEvaluator;
  protected final CFAEdge cfaEdge;
  protected final SMGState initialSmgState;

  public ExpressionValueVisitor(SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    smgExpressionEvaluator = pSmgExpressionEvaluator;
    cfaEdge = pEdge;
    initialSmgState = pSmgState;
  }

  @Override
  protected SMGValueAndStateList visitDefault(CExpression pExp) {
    return SMGValueAndStateList.of(getInitialSmgState());
  }

  @Override
  public SMGValueAndStateList visit(CArraySubscriptExpression exp) throws CPATransferException {

    List<SMGAddressAndState> addressAndStateList =
        smgExpressionEvaluator.evaluateArraySubscriptAddress(getInitialSmgState(), getCfaEdge(), exp);

    List<SMGValueAndState> result = new ArrayList<>(addressAndStateList.size());

    for (SMGAddressAndState addressAndState : addressAndStateList) {
      SMGAddress address = addressAndState.getObject();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        result.add(SMGValueAndState.of(newState));
        continue;
      }

      SMGValueAndState symbolicValueResultAndState = smgExpressionEvaluator.readValue(newState, address.getObject(), address.getOffset(), smgExpressionEvaluator.getRealExpressionType(exp), cfaEdge);
      result.add(symbolicValueResultAndState);
    }

    return SMGValueAndStateList.copyOf(result);
  }

  @Override
  public SMGValueAndStateList visit(CIntegerLiteralExpression exp) throws CPATransferException {

    BigInteger value = exp.getValue();

    boolean isZero = value.equals(BigInteger.ZERO);

    SMGSymbolicValue val = (isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance());
    return SMGValueAndStateList.of(getInitialSmgState(), val);
  }

  @Override
  public SMGValueAndStateList visit(CCharLiteralExpression exp) throws CPATransferException {

    char value = exp.getCharacter();

    SMGSymbolicValue val = (value == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
    return SMGValueAndStateList.of(getInitialSmgState(), val);
  }

  @Override
  public SMGValueAndStateList visit(CFieldReference fieldReference) throws CPATransferException {

    List<SMGValueAndState> result = new ArrayList<>(2);
    List<SMGAddressAndState> addressOfFieldAndStates =
        smgExpressionEvaluator.getAddressOfField(getInitialSmgState(), getCfaEdge(), fieldReference);

    for (SMGAddressAndState addressOfFieldAndState : addressOfFieldAndStates) {
      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      SMGState newState = addressOfFieldAndState.getSmgState();


      if (addressOfField.isUnknown()) {
        result.add(SMGValueAndState.of(newState));
        continue;
      }

      CType fieldType = smgExpressionEvaluator.getRealExpressionType(fieldReference);

      SMGValueAndState resultState = smgExpressionEvaluator.readValue(newState, addressOfField.getObject(), addressOfField.getOffset(), fieldType, cfaEdge);

      result.add(resultState);
    }

    return SMGValueAndStateList.copyOf(result);
  }

  @Override
  public SMGValueAndStateList visit(CFloatLiteralExpression exp)
      throws CPATransferException {

    boolean isZero = exp.getValue().equals(BigDecimal.ZERO);

    SMGSymbolicValue val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
    return SMGValueAndStateList.of(getInitialSmgState(), val);
  }

  @Override
  public SMGValueAndStateList visit(CIdExpression idExpression)
      throws CPATransferException {

    CSimpleDeclaration decl = idExpression.getDeclaration();

    if (decl instanceof CEnumerator) {

      long enumValue = ((CEnumerator) decl).getValue();

      SMGSymbolicValue val = enumValue == 0 ? SMGKnownSymValue.ZERO
          : SMGUnknownValue.getInstance();
      return SMGValueAndStateList.of(getInitialSmgState(), val);

    } else if (decl instanceof CVariableDeclaration
        || decl instanceof CParameterDeclaration) {
      SMGState smgState = getInitialSmgState();

      SMGObject variableObject = smgState
          .getObjectForVisibleVariable(idExpression.getName());

      smgState.addElementToCurrentChain(variableObject);
      SMGValueAndState result = smgExpressionEvaluator.readValue(smgState, variableObject, SMGKnownExpValue.ZERO,
          smgExpressionEvaluator.getRealExpressionType(idExpression), cfaEdge);
      result.getSmgState().addElementToCurrentChain(result.getObject());

      return SMGValueAndStateList.of(result);
    }

    return SMGValueAndStateList.of(getInitialSmgState());
  }

  @Override
  public SMGValueAndStateList visit(CUnaryExpression unaryExpression) throws CPATransferException {

    UnaryOperator unaryOperator = unaryExpression.getOperator();
    CExpression unaryOperand = unaryExpression.getOperand();

    switch (unaryOperator) {

    case AMPER:
      throw new UnrecognizedCCodeException("Can't use & of expression " + unaryOperand.toASTString(), cfaEdge,
          unaryExpression);

    case MINUS:

      List<SMGValueAndState> result = new ArrayList<>(2);

      SMGValueAndStateList valueAndStates = unaryOperand.accept(this);

      for (SMGValueAndState valueAndState : valueAndStates.getValueAndStateList()) {

        SMGSymbolicValue value = valueAndState.getObject();

        SMGSymbolicValue val = value.equals(SMGKnownSymValue.ZERO) ? value
            : SMGUnknownValue.getInstance();
        result.add(SMGValueAndState.of(valueAndState.getSmgState(), val));
      }

      return SMGValueAndStateList.copyOf(result);

    case SIZEOF:
      int size = smgExpressionEvaluator.getBitSizeof(cfaEdge, smgExpressionEvaluator.getRealExpressionType(unaryOperand), getInitialSmgState(), unaryOperand);
      SMGSymbolicValue val = (size == 0) ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      return SMGValueAndStateList.of(getInitialSmgState(), val);
    case TILDE:

    default:
      return SMGValueAndStateList.of(getInitialSmgState());
    }
  }

  @Override
  public SMGValueAndStateList visit(CPointerExpression pointerExpression) throws CPATransferException {

    CExpression operand = pointerExpression.getOperand();
    CType operandType = smgExpressionEvaluator.getRealExpressionType(operand);
    CType expType = smgExpressionEvaluator.getRealExpressionType(pointerExpression);

    if (operandType instanceof CPointerType) {
      return dereferencePointer(operand, expType);
    } else if (operandType instanceof CArrayType) {
      return dereferenceArray(operand, expType);
    } else {
      throw new UnrecognizedCCodeException("on pointer expression", cfaEdge, pointerExpression);
    }
  }

  @Override
  public SMGValueAndStateList visit(CTypeIdExpression typeIdExp) throws UnrecognizedCCodeException {

    TypeIdOperator typeOperator = typeIdExp.getOperator();
    CType type = typeIdExp.getType();

    switch (typeOperator) {
    case SIZEOF:
      SMGSymbolicValue val =
          smgExpressionEvaluator.getBitSizeof(cfaEdge, type, getInitialSmgState(), typeIdExp) == 0 ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
      return SMGValueAndStateList.of(getInitialSmgState(), val);
    default:
      return SMGValueAndStateList.of(getInitialSmgState());
    //TODO Investigate the other Operators.
    }
  }

  @Override
  public SMGValueAndStateList visit(CBinaryExpression exp) throws CPATransferException {

    BinaryOperator binaryOperator = exp.getOperator();
    CExpression lVarInBinaryExp = exp.getOperand1();
    CExpression rVarInBinaryExp = exp.getOperand2();
    List<SMGValueAndState> result = new ArrayList<>(4);

    SMGValueAndStateList lValAndStates = smgExpressionEvaluator.evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), lVarInBinaryExp);

    for (SMGValueAndState lValAndState : lValAndStates.getValueAndStateList()) {

      SMGSymbolicValue lVal = lValAndState.getObject();
      SMGState newState = lValAndState.getSmgState();

      SMGValueAndStateList rValAndStates = smgExpressionEvaluator.evaluateExpressionValue(newState, getCfaEdge(), rVarInBinaryExp);

      for (SMGValueAndState rValAndState : rValAndStates.getValueAndStateList()) {

        SMGSymbolicValue rVal = rValAndState.getObject();
        newState = rValAndState.getSmgState();

        if (rVal.equals(SMGUnknownValue.getInstance())
            || lVal.equals(SMGUnknownValue.getInstance())) {
          result.add(SMGValueAndState.of(newState));
          continue;
        }

        SMGValueAndStateList resultValueAndState =
            evaluateBinaryExpression(lVal, rVal, binaryOperator, newState);
        result.addAll(resultValueAndState.getValueAndStateList());
      }
    }

    return SMGValueAndStateList.copyOf(result);
  }

  private SMGValueAndStateList evaluateBinaryExpression(SMGSymbolicValue lVal,
      SMGSymbolicValue rVal, BinaryOperator binaryOperator, SMGState newState)
      throws SMGInconsistentException {

    if (lVal.equals(SMGUnknownValue.getInstance()) || rVal.equals(SMGUnknownValue.getInstance())) {
      return SMGValueAndStateList.of(newState);
    }

    switch (binaryOperator) {
      case PLUS:
      case MINUS:
      case DIVIDE:
      case MULTIPLY:
      case SHIFT_LEFT:
      case MODULO:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR: {

        boolean isZero;

        switch (binaryOperator) {
          case PLUS:
          case SHIFT_LEFT:
          case BINARY_OR:
          case BINARY_XOR:
          case SHIFT_RIGHT:
            isZero = lVal.equals(SMGKnownSymValue.ZERO) && rVal.equals(SMGKnownSymValue.ZERO);
            SMGSymbolicValue val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
            return SMGValueAndStateList.of(newState, val);

          case MINUS:
          case MODULO:
            isZero = (lVal.equals(rVal));
            val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
            return SMGValueAndStateList.of(newState, val);

          case DIVIDE:
            // TODO maybe we should signal a division by zero error?
            if (rVal.equals(SMGKnownSymValue.ZERO)) {
              return SMGValueAndStateList.of(newState);
            }

            isZero = lVal.equals(SMGKnownSymValue.ZERO);
            val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
            return SMGValueAndStateList.of(newState, val);

          case MULTIPLY:
          case BINARY_AND:
            isZero = lVal.equals(SMGKnownSymValue.ZERO)
                || rVal.equals(SMGKnownSymValue.ZERO);
            val = isZero ? SMGKnownSymValue.ZERO : SMGUnknownValue.getInstance();
            return SMGValueAndStateList.of(newState, val);

          default:
            throw new AssertionError();
        }
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL: {

        AssumeVisitor v = smgExpressionEvaluator.getAssumeVisitor(getCfaEdge(), newState);

        SMGValueAndStateList assumptionValueAndStates =
            v.evaluateBinaryAssumption(newState, binaryOperator, lVal, rVal);

        List<SMGValueAndState> result = new ArrayList<>(2);

        for (SMGValueAndState assumptionValueAndState : assumptionValueAndStates.getValueAndStateList()) {
          newState = assumptionValueAndState.getSmgState();
          SMGSymbolicValue assumptionVal = assumptionValueAndState.getObject();

          if (assumptionVal == SMGKnownSymValue.FALSE) {
            SMGValueAndState resultValueAndState = SMGValueAndState.of(newState, SMGKnownSymValue.ZERO);
            result.add(resultValueAndState);
          } else {
            result.add(SMGValueAndState.of(newState));
          }
        }

        return SMGValueAndStateList.copyOf(result);
      }

      default:
        return SMGValueAndStateList.of(getInitialSmgState());
    }
  }

  @Override
  public SMGValueAndStateList visit(CCastExpression cast) throws CPATransferException {
    // For different types we need different visitors,
    // TODO doesn't calculate type reinterpretations
    return smgExpressionEvaluator.evaluateExpressionValue(getInitialSmgState(), getCfaEdge(), cast.getOperand());
  }

  protected SMGValueAndStateList dereferenceArray(CExpression exp, CType derefType) throws CPATransferException {

    List<SMGValueAndState> result = new ArrayList<>(2);

    ArrayVisitor v = smgExpressionEvaluator.getArrayVisitor(getCfaEdge(), getInitialSmgState());

    List<SMGAddressAndState> addressAndStates = exp.accept(v);

    for (SMGAddressAndState addressAndState : addressAndStates) {
      SMGAddress address = addressAndState.getObject();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        // We can't resolve the field to dereference, therefore
        // we must assume, that it is invalid
        result.add(smgExpressionEvaluator.handleUnknownDereference(newState, cfaEdge));
        continue;
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        result.addAll(smgExpressionEvaluator.createAddress(newState, address.getObject(), address.getOffset()).asAddressValueAndStateList());
      } else {
        result.add(smgExpressionEvaluator.readValue(newState, address.getObject(), address.getOffset(), derefType, cfaEdge));
      }
    }

    return SMGValueAndStateList.copyOf(result);
  }

  protected final SMGValueAndStateList dereferencePointer(CExpression exp,
      CType derefType) throws CPATransferException {

    List<SMGValueAndState> result = new ArrayList<>(2);

    SMGAddressValueAndStateList addressAndStates = smgExpressionEvaluator.evaluateAddress(
        getInitialSmgState(), getCfaEdge(), exp);

    for (SMGAddressValueAndState addressAndState : addressAndStates.asAddressValueAndStateList()) {

      SMGAddressValue address = addressAndState.getObject();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        // We can't resolve the field to dereference , therefore
        // we must assume, that it is invalid
        result.add(smgExpressionEvaluator.handleUnknownDereference(newState, getCfaEdge()));
        continue;
      }

      // a == &a[0]
      if (derefType instanceof CArrayType) {
        result.addAll(smgExpressionEvaluator.createAddress(newState, address.getObject(), address.getOffset()).asAddressValueAndStateList());
      } else {
        result.add(smgExpressionEvaluator.readValue(newState, address.getObject(), address.getOffset(), derefType, cfaEdge));
      }
    }

    return SMGValueAndStateList.copyOf(result);
  }

  @Override
  public SMGValueAndStateList visit(CFunctionCallExpression pIastFunctionCallExpression) throws CPATransferException {
    return SMGValueAndStateList.of(getInitialSmgState());
  }

  public SMGState getInitialSmgState() {
    return initialSmgState;
  }

  public CFAEdge getCfaEdge() {
    return cfaEdge;
  }
}
// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableList;
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
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * This class evaluates expressions that evaluate not to a pointer, array, struct or union type. The
 * result of this evaluation is a {@link SMGSymbolicValue}. The value represents a symbolic value of
 * the SMG.
 */
class ExpressionValueVisitor
    extends DefaultCExpressionVisitor<List<? extends SMGValueAndState>, CPATransferException>
    implements CRightHandSideVisitor<List<? extends SMGValueAndState>, CPATransferException> {

  final SMGExpressionEvaluator smgExpressionEvaluator;

  /**
   * The edge should never be used to retrieve any information. It should only be used for logging
   * and debugging, because we do not know the context of the caller.
   */
  final CFAEdge cfaEdge;

  private final SMGState initialSmgState;

  public ExpressionValueVisitor(
      SMGExpressionEvaluator pSmgExpressionEvaluator, CFAEdge pEdge, SMGState pSmgState) {
    smgExpressionEvaluator = pSmgExpressionEvaluator;
    cfaEdge = pEdge;
    initialSmgState = pSmgState;
  }

  @Override
  protected List<? extends SMGValueAndState> visitDefault(CExpression pExp) {
    return singletonList(SMGValueAndState.withUnknownValue(getInitialSmgState()));
  }

  @Override
  public List<? extends SMGValueAndState> visit(CArraySubscriptExpression exp)
      throws CPATransferException {

    List<SMGValueAndState> result = new ArrayList<>();
    for (SMGAddressAndState addressAndState :
        smgExpressionEvaluator.evaluateArraySubscriptAddress(
            getInitialSmgState(), getCfaEdge(), exp)) {
      SMGAddress address = addressAndState.getObject();
      SMGState newState = addressAndState.getSmgState();

      if (address.isUnknown()) {
        result.add(SMGValueAndState.withUnknownValue(newState));
        continue;
      }

      SMGValueAndState symbolicValueResultAndState =
          smgExpressionEvaluator.readValue(
              newState,
              address.getObject(),
              address.getOffset(),
              TypeUtils.getRealExpressionType(exp),
              cfaEdge);
      result.add(symbolicValueResultAndState);
    }

    return result;
  }

  @Override
  public List<? extends SMGValueAndState> visit(CIntegerLiteralExpression exp)
      throws CPATransferException {

    BigInteger value = exp.getValue();

    SMGKnownSymbolicValue symbolicOfExplicit =
        getInitialSmgState().getSymbolicOfExplicit(SMGKnownExpValue.valueOf(value));
    if (symbolicOfExplicit == null) {
      symbolicOfExplicit = SMGKnownSymValue.of();
      getInitialSmgState().putExplicit(symbolicOfExplicit, SMGKnownExpValue.valueOf(value));
    }

    return singletonList(SMGValueAndState.of(getInitialSmgState(), symbolicOfExplicit));
  }

  @Override
  public List<? extends SMGValueAndState> visit(CCharLiteralExpression exp)
      throws CPATransferException {

    char value = exp.getCharacter();

    SMGSymbolicValue val = (value == 0) ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
    return singletonList(SMGValueAndState.of(getInitialSmgState(), val));
  }

  @Override
  public List<? extends SMGValueAndState> visit(CFieldReference fieldReference)
      throws CPATransferException {

    List<SMGValueAndState> result = new ArrayList<>(2);
    List<SMGAddressAndState> addressOfFieldAndStates =
        smgExpressionEvaluator.getAddressOfField(
            getInitialSmgState(), getCfaEdge(), fieldReference);

    for (SMGAddressAndState addressOfFieldAndState : addressOfFieldAndStates) {
      SMGAddress addressOfField = addressOfFieldAndState.getObject();
      SMGState newState = addressOfFieldAndState.getSmgState();

      if (addressOfField.isUnknown()) {
        result.add(SMGValueAndState.withUnknownValue(newState));
        continue;
      }

      CType fieldType = TypeUtils.getRealExpressionType(fieldReference);

      result.add(
          smgExpressionEvaluator.readValue(
              newState,
              addressOfField.getObject(),
              addressOfField.getOffset(),
              fieldType,
              cfaEdge));
    }

    return result;
  }

  @Override
  public List<? extends SMGValueAndState> visit(CFloatLiteralExpression exp)
      throws CPATransferException {

    boolean isZero = exp.getValue().compareTo(BigDecimal.ZERO) == 0;

    SMGSymbolicValue val = isZero ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
    return singletonList(SMGValueAndState.of(getInitialSmgState(), val));
  }

  @Override
  public List<? extends SMGValueAndState> visit(CIdExpression idExpression)
      throws CPATransferException {

    CSimpleDeclaration decl = idExpression.getDeclaration();

    if (decl instanceof CEnumerator) {

      long enumValue = ((CEnumerator) decl).getValue();

      SMGSymbolicValue val = enumValue == 0 ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
      return singletonList(SMGValueAndState.of(getInitialSmgState(), val));

    } else if (decl instanceof CVariableDeclaration || decl instanceof CParameterDeclaration) {
      SMGState smgState = getInitialSmgState();

      SMGObject variableObject =
          smgState.getHeap().getObjectForVisibleVariable(idExpression.getName());

      if (variableObject != null) {
        // Witness validation cannot compute an assignment for some cases.
        // Then the variableObject can be NULL. TODO when exactly does this happen?
        smgState.addElementToCurrentChain(variableObject);
        SMGValueAndState result =
            smgExpressionEvaluator.readValue(
                smgState,
                variableObject,
                SMGZeroValue.INSTANCE,
                TypeUtils.getRealExpressionType(idExpression),
                cfaEdge);
        result.getSmgState().addElementToCurrentChain(result.getObject());

        return singletonList(result);
      }
    }

    return singletonList(SMGValueAndState.withUnknownValue(getInitialSmgState()));
  }

  @Override
  public List<? extends SMGValueAndState> visit(CUnaryExpression unaryExpression)
      throws CPATransferException {

    UnaryOperator unaryOperator = unaryExpression.getOperator();
    CExpression unaryOperand = unaryExpression.getOperand();

    switch (unaryOperator) {
      case AMPER:
        throw new UnrecognizedCodeException(
            "Can't use & of expression " + unaryOperand.toASTString(), cfaEdge, unaryExpression);

      case MINUS:
        ImmutableList.Builder<SMGValueAndState> result = ImmutableList.builderWithExpectedSize(2);

        List<? extends SMGValueAndState> valueAndStates = unaryOperand.accept(this);

        for (SMGValueAndState valueAndState : valueAndStates) {

          SMGValue value = valueAndState.getObject();

          SMGValue val = value.equals(SMGZeroValue.INSTANCE) ? value : SMGUnknownValue.INSTANCE;
          result.add(SMGValueAndState.of(valueAndState.getSmgState(), val));
        }

        return result.build();

      case SIZEOF:
        long size =
            smgExpressionEvaluator.getBitSizeof(
                cfaEdge,
                TypeUtils.getRealExpressionType(unaryOperand),
                getInitialSmgState(),
                unaryOperand);
        SMGSymbolicValue val = (size == 0) ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
        return singletonList(SMGValueAndState.of(getInitialSmgState(), val));
      case TILDE:

      default:
        return singletonList(SMGValueAndState.withUnknownValue(getInitialSmgState()));
    }
  }

  @Override
  public List<? extends SMGValueAndState> visit(CPointerExpression pointerExpression)
      throws CPATransferException {

    CExpression operand = pointerExpression.getOperand();
    CType operandType = TypeUtils.getRealExpressionType(operand);
    CType expType = TypeUtils.getRealExpressionType(pointerExpression);

    if (operandType instanceof CPointerType) {
      return dereferencePointer(operand, expType);
    } else if (operandType instanceof CArrayType) {
      return dereferenceArray(operand, expType);
    } else {
      throw new UnrecognizedCodeException("on pointer expression", cfaEdge, pointerExpression);
    }
  }

  @Override
  public List<? extends SMGValueAndState> visit(CTypeIdExpression typeIdExp)
      throws UnrecognizedCodeException {

    TypeIdOperator typeOperator = typeIdExp.getOperator();
    CType type = typeIdExp.getType();

    switch (typeOperator) {
      case SIZEOF:
        SMGSymbolicValue val =
            smgExpressionEvaluator.getBitSizeof(cfaEdge, type, getInitialSmgState(), typeIdExp) == 0
                ? SMGZeroValue.INSTANCE
                : SMGUnknownValue.INSTANCE;
        return singletonList(SMGValueAndState.of(getInitialSmgState(), val));
      default:
        return singletonList(SMGValueAndState.withUnknownValue(getInitialSmgState()));
        // TODO Investigate the other Operators.
    }
  }

  @Override
  public List<? extends SMGValueAndState> visit(CBinaryExpression exp) throws CPATransferException {

    BinaryOperator binaryOperator = exp.getOperator();
    CExpression lVarInBinaryExp = exp.getOperand1();
    CExpression rVarInBinaryExp = exp.getOperand2();
    List<SMGValueAndState> result = new ArrayList<>(4);

    List<? extends SMGValueAndState> lValAndStates =
        smgExpressionEvaluator.evaluateExpressionValue(
            getInitialSmgState(), getCfaEdge(), lVarInBinaryExp);

    for (SMGValueAndState lValAndState : lValAndStates) {

      SMGValue lVal = lValAndState.getObject();
      SMGState newState = lValAndState.getSmgState();

      List<? extends SMGValueAndState> rValAndStates =
          smgExpressionEvaluator.evaluateExpressionValue(newState, getCfaEdge(), rVarInBinaryExp);

      for (SMGValueAndState rValAndState : rValAndStates) {

        SMGValue rVal = rValAndState.getObject();
        newState = rValAndState.getSmgState();

        if (rVal.equals(SMGUnknownValue.INSTANCE) || lVal.equals(SMGUnknownValue.INSTANCE)) {
          result.add(SMGValueAndState.withUnknownValue(newState));
          continue;
        }

        result.addAll(
            evaluateBinaryExpression(lVal, rVal, lVarInBinaryExp, binaryOperator, newState));
      }
    }

    return result;
  }

  private List<? extends SMGValueAndState> evaluateBinaryExpression(
      SMGValue lVal,
      SMGValue rVal,
      CExpression lVarInBinaryExp,
      BinaryOperator binaryOperator,
      SMGState newState)
      throws SMGInconsistentException {

    if (lVal.equals(SMGUnknownValue.INSTANCE) || rVal.equals(SMGUnknownValue.INSTANCE)) {
      return singletonList(SMGValueAndState.withUnknownValue(newState));
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
      case BINARY_XOR:
        {
          boolean isZero;

          switch (binaryOperator) {
            case PLUS:
            case SHIFT_LEFT:
            case BINARY_OR:
            case BINARY_XOR:
            case SHIFT_RIGHT:
              isZero = lVal.equals(SMGZeroValue.INSTANCE) && rVal.equals(SMGZeroValue.INSTANCE);
              SMGSymbolicValue val = isZero ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
              return singletonList(SMGValueAndState.of(newState, val));

            case MINUS:
              if (lVal instanceof SMGKnownAddressValue && rVal instanceof SMGKnownAddressValue) {
                SMGKnownAddressValue lValAddress = (SMGKnownAddressValue) lVal;
                SMGKnownAddressValue rValAddress = (SMGKnownAddressValue) rVal;
                if (lValAddress.getObject().equals(rValAddress.getObject())) {
                  CType lVarType = lVarInBinaryExp.getExpressionType().getCanonicalType();
                  final CType type;
                  if (lVarType instanceof CPointerType) {
                    // normal pointer type
                    type = ((CPointerType) lVarType).getType();
                  } else if (lVarType instanceof CSimpleType) {
                    // pointers can also be casted as "long int" (32bit) or "long long int" (64bit).
                    // Lets assume, that invalid combinations like "int" for 64bit do not appear.
                    type = lVarType;
                  } else {
                    throw new AssertionError(
                        String.format(
                            "unhandled type '%s' for pointer comparison using '%s'.",
                            lVarType, lVarInBinaryExp));
                  }
                  BigInteger sizeOfLVal = smgExpressionEvaluator.machineModel.getSizeofInBits(type);
                  SMGExplicitValue diff = lValAddress.getOffset().subtract(rValAddress.getOffset());
                  diff = diff.divide(SMGKnownExpValue.valueOf(sizeOfLVal));
                  return singletonList(SMGValueAndState.of(newState, diff));
                }
              }
              // else
              isZero = lVal.equals(rVal);
              val = isZero ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
              return singletonList(SMGValueAndState.of(newState, val));

            case MODULO:
              isZero = lVal.equals(rVal);
              val = isZero ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
              return singletonList(SMGValueAndState.of(newState, val));

            case DIVIDE:
              // TODO maybe we should signal a division by zero error?
              if (rVal.equals(SMGZeroValue.INSTANCE)) {
                return singletonList(SMGValueAndState.withUnknownValue(newState));
              }

              isZero = lVal.equals(SMGZeroValue.INSTANCE);
              val = isZero ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
              return singletonList(SMGValueAndState.of(newState, val));

            case MULTIPLY:
            case BINARY_AND:
              isZero = lVal.equals(SMGZeroValue.INSTANCE) || rVal.equals(SMGZeroValue.INSTANCE);
              val = isZero ? SMGZeroValue.INSTANCE : SMGUnknownValue.INSTANCE;
              return singletonList(SMGValueAndState.of(newState, val));

            default:
              throw new AssertionError();
          }
        }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        {
          AssumeVisitor v = smgExpressionEvaluator.getAssumeVisitor(getCfaEdge(), newState);

          List<? extends SMGValueAndState> assumptionValueAndStates =
              v.evaluateBinaryAssumption(newState, binaryOperator, lVal, rVal);

          ImmutableList.Builder<SMGValueAndState> result = ImmutableList.builderWithExpectedSize(2);

          for (SMGValueAndState assumptionValueAndState : assumptionValueAndStates) {
            newState = assumptionValueAndState.getSmgState();
            SMGValue assumptionVal = assumptionValueAndState.getObject();

            if (assumptionVal.isZero()) {
              SMGValueAndState resultValueAndState =
                  SMGValueAndState.of(newState, SMGZeroValue.INSTANCE);
              result.add(resultValueAndState);
            } else {
              result.add(SMGValueAndState.withUnknownValue(newState));
            }
          }

          return result.build();
        }

      default:
        return singletonList(SMGValueAndState.withUnknownValue(getInitialSmgState()));
    }
  }

  @Override
  public List<? extends SMGValueAndState> visit(CCastExpression cast) throws CPATransferException {
    // For different types we need different visitors,
    // TODO doesn't calculate type reinterpretations
    List<? extends SMGValueAndState> smgValueAndStates =
        smgExpressionEvaluator.evaluateExpressionValue(
            getInitialSmgState(), getCfaEdge(), cast.getOperand());
    CType expressionType = cast.getCastType();
    if (!SMGExpressionEvaluator.isAddressType(expressionType)
        && SMGExpressionEvaluator.isAddressType(
            TypeUtils.getRealExpressionType(cast.getOperand()))) {
      List<SMGValueAndState> castedValueAndStates = new ArrayList<>(smgValueAndStates.size());
      for (SMGValueAndState valueAndState : smgValueAndStates) {
        if (valueAndState instanceof SMGAddressValueAndState) {
          castedValueAndStates.add(valueAndState);
        }
      }
      return castedValueAndStates;
    }
    return smgValueAndStates;
  }

  private List<? extends SMGValueAndState> dereferenceArray(CExpression exp, CType derefType)
      throws CPATransferException {

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
        result.addAll(
            smgExpressionEvaluator.createAddress(
                newState, address.getObject(), address.getOffset()));
      } else {
        result.add(
            smgExpressionEvaluator.readValue(
                newState, address.getObject(), address.getOffset(), derefType, cfaEdge));
      }
    }

    return result;
  }

  private List<? extends SMGValueAndState> dereferencePointer(CExpression exp, CType derefType)
      throws CPATransferException {

    List<SMGValueAndState> result = new ArrayList<>(2);

    List<SMGAddressValueAndState> addressAndStates =
        smgExpressionEvaluator.evaluateAddress(getInitialSmgState(), getCfaEdge(), exp);

    for (SMGAddressValueAndState addressAndState : addressAndStates) {

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
        result.addAll(
            smgExpressionEvaluator.createAddress(
                newState, address.getObject(), address.getOffset()));
      } else {
        result.add(
            smgExpressionEvaluator.readValue(
                newState, address.getObject(), address.getOffset(), derefType, cfaEdge));
      }
    }

    return result;
  }

  @Override
  public List<? extends SMGValueAndState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    return singletonList(SMGValueAndState.withUnknownValue(getInitialSmgState()));
  }

  SMGState getInitialSmgState() {
    return initialSmgState;
  }

  CFAEdge getCfaEdge() {
    return cfaEdge;
  }
}

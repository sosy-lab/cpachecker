// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import static java.util.Collections.singletonList;

import com.google.common.collect.ImmutableList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType.CEnumerator;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * This class evaluates expressions that evaluate not to a pointer, array, struct or union type. The
 * result of this evaluation is a {@link CValue}. The value represents a symbolic value related to a
 * SMGValue.
 */
public class NonPointerExpressionVisitor
    extends DefaultCExpressionVisitor<List<CValueAndSMGState>, CPATransferException>
    implements CRightHandSideVisitor<List<CValueAndSMGState>, CPATransferException> {

  private final SMGState initialSmgState;
  // I don't like this cyclic dependency, but atm this is needed for pointer addresses
  private final AddressEvaluator evaluator;

  public NonPointerExpressionVisitor(SMGState pSmgState, AddressEvaluator pEvaluator) {
    initialSmgState = pSmgState;
    evaluator = pEvaluator;
  }

  @Override
  public List<CValueAndSMGState> visit(CArraySubscriptExpression exp) throws CPATransferException {
    // Cyclic call to evaluate addresses
    return readAddressValueAndState(
        evaluator.evaluateArraySubscriptAddress(initialSmgState, exp), exp);
  }

  @Override
  public List<CValueAndSMGState> visit(CPointerExpression pointerExpression)
      throws CPATransferException {

    CExpression operand = pointerExpression.getOperand();
    CType operandType = TypeUtils.getRealExpressionType(operand);
    CType expType = TypeUtils.getRealExpressionType(pointerExpression);

    // Cyclic calls to evaluate addresses
    if (operandType instanceof CPointerType) {
      return dereference(
          operand, expType, evaluator.evaluateArrayAddress(initialSmgState, operand));
    } else if (operandType instanceof CArrayType) {
      return dereference(operand, expType, evaluator.evaluateAddress(initialSmgState, operand));
    } else {
      throw new UnrecognizedCodeException("on pointer expression", pointerExpression);
    }
  }

  /**
   * Utility function for pointer and array dereferencing. Handles dereference for a Collection of
   * addresses.
   *
   * @param pOperand - the expression to be dereferenced
   * @param pExpType - the expression type
   * @param addresses - the collection of addresses
   * @return List of CValue and SMGState mapping for dereferenced addresses.
   */
  private List<CValueAndSMGState> dereference(
      CExpression pOperand, CType pExpType, Collection<CValueAndSMGState> addresses) {
    return addresses.stream()
        .flatMap(
            addressAndState -> {
              CValue address = addressAndState.getValue();
              SMGState newState = addressAndState.getState();
              if (address.isUnknown()) {
                return Stream.of(evaluator.handleUnknownDereference(newState));
              }
              if (pExpType instanceof CArrayType) {
                // Cyclic call to evaluate addresses
                return evaluator.createAddress(newState, address).stream();
              }
              // Cyclic call to evaluate addresses
              return Stream.of(evaluator.readValue(newState, address, pOperand));
            })
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<CValueAndSMGState> visit(CFieldReference fieldReference) throws CPATransferException {
    // Cyclic call to evaluate addresses
    return readAddressValueAndState(
        evaluator.getAddressOfField(initialSmgState, fieldReference), fieldReference);
  }

  /**
   * Utility function for array and field subscription. Handles subscription for a Collection of
   * addresses.
   *
   * @param expression - the expression to be subscribed
   * @param addresses - the collection of addresses
   * @return List of CValue and SMGState mapping for subscribed addresses.
   */
  private List<CValueAndSMGState> readAddressValueAndState(
      Collection<CValueAndSMGState> addresses, CExpression expression) {
    return addresses.stream()
        .map(
            addressAndState -> {
              CValue address = addressAndState.getValue();
              SMGState newState = addressAndState.getState();
              if (address.isUnknown()) {
                return CValueAndSMGState.ofUnknown(newState);
              }
              // Cyclic call to evaluate addresses
              return evaluator.readValue(newState, address, expression);
            })
        .collect(ImmutableList.toImmutableList());
  }

  @Override
  public List<CValueAndSMGState> visit(CCharLiteralExpression pE) throws CPATransferException {
    // TODO The old implementation only checks for zero and returns unknown else.
    // It needs to be checked whether chars can be handled as ints here
    return valueAndStatesForIntValue(BigInteger.valueOf(pE.getCharacter()));
  }

  @Override
  public List<CValueAndSMGState> visit(CFloatLiteralExpression pE) throws CPATransferException {
    BigDecimal floatValue = pE.getValue();
    BigDecimal floatValueRounded =
        BigDecimal.valueOf(pE.getValue().toBigIntegerExact().longValueExact());
    // TODO The old implementation only checks for zero and returns unknown else.
    // It needs to be checked whether floats which are actual integer can be treated as integer
    if (floatValue.compareTo(floatValueRounded) == 0) {
      return valueAndStatesForIntValue(pE.getValue().toBigIntegerExact());
    }

    return visitDefault(pE);
  }

  @Override
  public List<CValueAndSMGState> visit(CIntegerLiteralExpression pE) throws CPATransferException {
    return valueAndStatesForIntValue(pE.getValue());
  }

  @Override
  public List<CValueAndSMGState> visit(CIdExpression idExpression) throws CPATransferException {

    CSimpleDeclaration decl = idExpression.getDeclaration();

    if (decl instanceof CEnumerator) {
      long enumValue = ((CEnumerator) decl).getValue();
      // TODO check whether handling enumerator as int is sound. The old implementation only handles
      // zero and returns unknown else.
      return valueAndStatesForIntValue(BigInteger.valueOf(enumValue));

    } else if (decl instanceof CVariableDeclaration || decl instanceof CParameterDeclaration) {
      SMGState smgState = initialSmgState;

      Optional<SMGObject> variableObjectOptional =
          smgState.getHeap().getObjectForVisibleVariable(idExpression.getName());

      if (variableObjectOptional.isPresent()) {
        // Witness validation cannot compute an assignment for some cases.
        // Then the variableObject can be NULL. TODO when exactly does this happen?
        smgState = smgState.addElementToCurrentChain(variableObjectOptional.orElseThrow());
        CValueAndSMGState result =
            evaluator.readValue(smgState, variableObjectOptional.orElseThrow(), idExpression);

        return singletonList(
            CValueAndSMGState.of(
                result.getValue(), result.getState().addElementToCurrentChain(result)));
      }
    }

    return singletonList(CValueAndSMGState.ofUnknown(initialSmgState));
  }

  @Override
  public List<CValueAndSMGState> visit(CUnaryExpression unaryExpression)
      throws CPATransferException {

    UnaryOperator unaryOperator = unaryExpression.getOperator();
    CExpression unaryOperand = unaryExpression.getOperand();

    switch (unaryOperator) {
      case AMPER:
        throw new UnrecognizedCodeException(
            "Can't use & of expression " + unaryOperand.toASTString(), unaryExpression);

      case MINUS:
        return unaryOperand.accept(this).stream()
            .map(
                valueAndState -> {
                  CValue val =
                      valueAndState.getValue().isZero() ? CValue.zero() : CValue.getUnknownValue();
                  return CValueAndSMGState.of(val, valueAndState.getState());
                })
            .collect(ImmutableList.toImmutableList());

      case SIZEOF:
        BigInteger size = evaluator.getBitSizeof(initialSmgState, unaryOperand);
        CValue val = size.equals(BigInteger.ZERO) ? CValue.zero() : CValue.getUnknownValue();
        return singletonList(CValueAndSMGState.of(val, initialSmgState));
      case TILDE:

      default:
        return singletonList(CValueAndSMGState.ofUnknown(initialSmgState));
    }
  }

  /**
   * Utility function to assign a SMGValue representation to a value derived by the value analysis.
   * First checks whether there is an existing mapping, otherwise a new mapping is created.
   *
   * @param pValue - the Integer value derived by the value analysis.
   * @return list of CValue and state mappings.
   */
  private List<CValueAndSMGState> valueAndStatesForIntValue(BigInteger pValue) {
    CValue value = CValue.valueOf(pValue);
    Optional<SMGValue> smgValueRepresenationOptional = initialSmgState.getSMGValueForCValue(value);
    SMGValue smgValueRep = smgValueRepresenationOptional.orElseGet(SMGValue::of);
    return Collections.singletonList(
        CValueAndSMGState.of(value, initialSmgState.copyAndAddValue(value, smgValueRep)));
  }

  @Override
  protected List<CValueAndSMGState> visitDefault(CExpression pExp) throws CPATransferException {
    return singletonList(CValueAndSMGState.ofUnknown(initialSmgState));
  }

  @Override
  public List<CValueAndSMGState> visit(CFunctionCallExpression pIastFunctionCallExpression)
      throws CPATransferException {
    return Collections.singletonList(CValueAndSMGState.ofUnknown(initialSmgState));
  }
}

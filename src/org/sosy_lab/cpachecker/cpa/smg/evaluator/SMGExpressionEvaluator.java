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
package org.sosy_lab.cpachecker.cpa.smg.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.SMGState;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndStateList;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGRegion;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGField;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddVal;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

/**
 * This class evaluates expressions using {@link SMGState}.
 * It should not change the {@link SMGState}, to permit
 * evaluating expressions independently of the transfer relation,
 * enabling other cpas to interact more easily with SMGCPA.
 */
public class SMGExpressionEvaluator {

  protected final LogManagerWithoutDuplicates logger;
  protected final MachineModel machineModel;

  public SMGExpressionEvaluator(LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  /**
   * Get the size of the given type in bits.
   *
   * When handling variable array type length,
   * additionally to the type itself, we also need the
   * cfa edge to determine the location of the program
   * we currently handle, the smg state to determine
   * the values of the variables at the current location,
   * and the expression with the given type to determine
   * the smg object that represents the array of the given type.
   *
   * @param pEdge The cfa edge that determines the location in the program.
   * @param pType We want to calculate the size of this type.
   * @param pState The state that contains the current variable values.
   * @param pExpression The expression, which evaluates to the value with the given type.
   * @return The size of the given type in bits.
   */
  public int getBitSizeof(CFAEdge pEdge, CType pType, SMGState pState, CExpression pExpression) throws UnrecognizedCCodeException {
    return getBitSizeof(pEdge, pType, pState, Optional.of(pExpression));
  }

  /**
   * Get the size of the given type in bits.
   *
   * When handling variable array type length,
   * additionally to the type itself, we also need the
   * cfa edge to determine the location of the program
   * we currently handle, and the smg state to determine
   * the values of the variables at the current location..
   *
   * This method can't calculate variable array type length for
   * arrays that are not declared in the cfa edge.
   *
   * @param pEdge The cfa edge that determines the location in the program.
   * @param pType We want to calculate the size of this type.
   * @param pState The state that contains the current variable values.
   * @return The size of the given type in bits.
   */
  public int getBitSizeof(CFAEdge pEdge, CType pType, SMGState pState) throws UnrecognizedCCodeException {
    return getBitSizeof(pEdge, pType, pState, Optional.empty());
  }

  private int getBitSizeof(CFAEdge edge, CType pType, SMGState pState, Optional<CExpression> pExpression) throws UnrecognizedCCodeException {

    if (pType instanceof CBitFieldType) {
      return ((CBitFieldType) pType).getBitFieldSize();
    }

    CSizeOfVisitor v = getSizeOfVisitor(edge, pState, pExpression);

    try {
      return pType.accept(v) * machineModel.getSizeofCharInBits();
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCCodeException("Could not resolve type.", edge);
    }
  }

  List<SMGAddressAndState> getAddressOfField(SMGState pSmgState, CFAEdge cfaEdge, CFieldReference fieldReference)
      throws CPATransferException {

    CExpression fieldOwner = fieldReference.getFieldOwner();
    CType ownerType = getRealExpressionType(fieldOwner);
    List<SMGAddressAndState> result = new ArrayList<>(4);

    /* Points to the start of this struct or union.
    *
    * Note that whether this field Reference is a pointer dereference x->b
    * or not x.b is indirectly resolved by whether the type of x is
    * a pointer type, in which case its expression is evaluated, or
    * a struct type, in which case the address of the expression
    * similar is evaluated.
    */

    SMGAddressValueAndStateList fieldOwnerAddressAndStates = evaluateAddress(pSmgState, cfaEdge, fieldOwner);

    for (SMGAddressValueAndState fieldOwnerAddressAndState : fieldOwnerAddressAndStates.asAddressValueAndStateList()) {

      SMGAddressValue fieldOwnerAddress = fieldOwnerAddressAndState.getObject();
      SMGState newState = fieldOwnerAddressAndState.getSmgState();
      String fieldName = fieldReference.getFieldName();
      SMGField field = getField(ownerType, fieldName);

      if (field.isUnknown() || fieldOwnerAddress.isUnknown()) {
        if (fieldReference.isPointerDereference()) {
          newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
        }
        result.add(SMGAddressAndState.of(newState));
      } else {
        SMGAddress addressOfFieldOwner = fieldOwnerAddress.getAddress();
        SMGExplicitValue fieldOffset = addressOfFieldOwner.add(field.getOffset()).getOffset();
        SMGObject fieldObject = addressOfFieldOwner.getObject();
        SMGAddress address = SMGAddress.valueOf(fieldObject, fieldOffset);
        result.add(SMGAddressAndState.of(newState, address));
      }
    }

    return result;
  }

  public SMGValueAndState readValue(SMGState pSmgState, SMGObject pObject,
      SMGExplicitValue pOffset, CType pType, CFAEdge pEdge) throws SMGInconsistentException, UnrecognizedCCodeException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGValueAndState.of(pSmgState);
    }

    long fieldOffset = pOffset.getAsLong();

    //FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject = fieldOffset < 0
        || fieldOffset + getBitSizeof(pEdge, pType, pSmgState) > pObject.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(Level.WARNING, pEdge.getFileLocation() + ":", "Field " + "("
          + fieldOffset + ", " + pType.toASTString("") + ")"
          + " does not fit object " + pObject.toString() + ".");

      return SMGValueAndState.of(pSmgState);
    }

    // We don't want to modify the state while reading
    SMGSymbolicValue value = pSmgState.readValue(pObject, fieldOffset, pType).getObject();

    return SMGValueAndState.of(pSmgState, value);
  }

  private SMGField getField(CType pOwnerType, String pFieldName) throws UnrecognizedCCodeException {

    if (pOwnerType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) pOwnerType).getRealType();

      if (realType == null) {
        return SMGField.getUnknownInstance();
      }

      return getField(realType, pFieldName);
    } else if (pOwnerType instanceof CCompositeType) {
      return getField((CCompositeType) pOwnerType, pFieldName);
    } else if (pOwnerType instanceof CPointerType) {

      /* We do not explicitly transform x->b,
      so when we try to get the field b the ownerType of x
      is a pointer type.*/

      CType type = ((CPointerType) pOwnerType).getType();

      type = getRealExpressionType(type);

      return getField(type, pFieldName);
    }

    throw new AssertionError();
  }

  private SMGField getField(CCompositeType pOwnerType, String pFieldName) {

    List<CCompositeTypeMemberDeclaration> membersOfType = pOwnerType.getMembers();
    CType resultType = pOwnerType;

    long offset = machineModel.getFieldOffsetInBits(pOwnerType, pFieldName);

    for (CCompositeTypeMemberDeclaration typeMember : membersOfType) {
      if (typeMember.getName().equals(pFieldName)) {
        resultType = typeMember.getType();
      }
    }

    SMGExplicitValue smgValue = null;
    if (!resultType.equals(pOwnerType)) {
      smgValue = SMGKnownExpValue.valueOf(offset);
      resultType = getRealExpressionType(resultType);
    } else {
      smgValue = SMGUnknownValue.getInstance();
    }
    return new SMGField(smgValue, resultType);
  }

  public boolean isStructOrUnionType(CType rValueType) {

    if (rValueType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    if (rValueType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) rValueType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    return false;
  }

  public SMGExplicitValue evaluateExplicitValueV2(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    List<SMGExplicitValueAndState> result = evaluateExplicitValue(smgState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  public List<SMGExplicitValueAndState> evaluateExplicitValue(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    List<SMGExplicitValueAndState> result = new ArrayList<>();

    ExplicitValueVisitor visitor = new ExplicitValueVisitor(this, smgState, null, machineModel, logger, cfaEdge);

    Value value = rValue.accept(visitor);
    SMGState newState = visitor.getNewState();

    if (!value.isExplicitlyKnown() || !value.isNumericValue()) {

      // Sometimes, we can get the explicit Value from SMGCPA, especially if the
      // result happens to
      // be a pointer to the Null Object, or through reinterpretation
      SMGValueAndStateList symbolicValueAndStates = evaluateExpressionValue(
          newState, cfaEdge, rValue);

      for (SMGValueAndState symbolicValueAndState : symbolicValueAndStates.getValueAndStateList()) {
        result.add(deriveExplicitValueFromSymbolicValue(symbolicValueAndState));
      }
    } else {
      long longValue = value.asNumericValue().longValue();
      result.add(SMGExplicitValueAndState.of(newState, SMGKnownExpValue.valueOf(longValue)));
    }

    for (SMGState additionalState : visitor.getSmgStatesToBeProccessed()) {
      result.addAll(evaluateExplicitValue(additionalState, cfaEdge, rValue));
    }

    return result;
  }

  private SMGExplicitValueAndState deriveExplicitValueFromSymbolicValue(SMGValueAndState symbolicValueAndState) {

    SMGSymbolicValue symbolicValue = symbolicValueAndState.getObject();
    SMGState newState = symbolicValueAndState.getSmgState();

    if (!symbolicValue.isUnknown()) {
      if (symbolicValue == SMGKnownSymValue.ZERO) {
        return SMGExplicitValueAndState.of(newState, SMGKnownExpValue.ZERO); }

      if (symbolicValue instanceof SMGAddressValue) {
        SMGAddressValue address = (SMGAddressValue) symbolicValue;

        if (address.getObject() == SMGNullObject.INSTANCE) { return SMGExplicitValueAndState.of(newState,
            SMGKnownExpValue.valueOf(address.getOffset().getAsLong() / machineModel.getSizeofCharInBits())); }
      }
    }

    return SMGExplicitValueAndState.of(newState);

  }

  public SMGSymbolicValue evaluateExpressionValueV2(SMGState smgState,
      CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    SMGValueAndStateList result = evaluateExpressionValue(smgState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.getValueAndStateList().get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  public SMGValueAndStateList evaluateExpressionValue(SMGState smgState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || expressionType instanceof CArrayType
        || isStructOrUnionType(expressionType)
        || expressionType instanceof CFunctionType) {
      /* expressions with Array Types as result
       *  are transformed. a = &(a[0]) */

      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      return evaluateAddress(smgState, cfaEdge, rValue);
    } else {
      return evaluateNonAddressValue(smgState, cfaEdge, rValue);
    }
  }

  private SMGValueAndStateList evaluateNonAddressValue(SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    ExpressionValueVisitor visitor = getExpressionValueVisitor(cfaEdge, newState);

    SMGValueAndStateList symbolicValues = rValue.accept(visitor);

    return symbolicValues;
  }

  protected SMGValueAndStateList evaluateAssumptionValue(SMGState newState,
      CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    ExpressionValueVisitor visitor = getAssumeVisitor(cfaEdge, newState);
    return rValue.accept(visitor);
  }

  public SMGSymbolicValue evaluateAssumptionValueV2(SMGState newState,
      CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {

    SMGValueAndStateList result = evaluateAssumptionValue(newState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.getValueAndStateList().get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  public SMGAddressValueAndStateList evaluateAddress(SMGState pState, CFAEdge cfaEdge, CRightHandSide rValue)
      throws CPATransferException {

    CType expressionType = getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || (expressionType instanceof CFunctionType
            && rValue instanceof CUnaryExpression
            && ((CUnaryExpression) rValue).getOperator() == CUnaryExpression.UnaryOperator.AMPER)) {
      // Cfa treats &foo as CFunctionType

      PointerVisitor visitor = getPointerVisitor(cfaEdge, pState);

      SMGValueAndStateList addressAndStateList = rValue.accept(visitor);
      return getAddressFromSymbolicValues(addressAndStateList);
    } else if (isStructOrUnionType(expressionType)) {
      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      StructAndUnionVisitor visitor = getStructAndUnionVisitor(cfaEdge, pState);
      List<SMGAddressAndState> structAddressAndState = rValue.accept(visitor);
      return createAddresses(structAddressAndState);
    } else if (expressionType instanceof CArrayType) {

      ArrayVisitor visitor = getArrayVisitor(cfaEdge, pState);
      List<SMGAddressAndState> arrayAddressAndState = rValue.accept(visitor);
      return createAddresses(arrayAddressAndState);
    } else {
      throw new AssertionError("The method evaluateAddress may not be called" +
          "with the type " + expressionType.toASTString(""));
    }
  }

  public SMGAddressValue evaluateAddressV2(SMGState newState, CFAEdge cfaEdge,
      CRightHandSide rValue) throws CPATransferException {

    SMGAddressValueAndStateList result = evaluateAddress(newState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.asAddressValueAndStateList().get(0).getObject();
    } else {
      return SMGUnknownValue.getInstance();
    }
  }

  public CType getRealExpressionType(CType type) {
    return type.getCanonicalType();
  }

  public CType getRealExpressionType(CSimpleDeclaration decl) {
    return getRealExpressionType(decl.getType());
  }

  public CType getRealExpressionType(CRightHandSide exp) {
    return getRealExpressionType(exp.getExpressionType());
  }

  SMGAddressValueAndStateList handlePointerArithmetic(SMGState initialSmgState,
      CFAEdge cfaEdge, CExpression address, CExpression pointerOffset,
      CType typeOfPointer, boolean lVarIsAddress,
      CBinaryExpression binaryExp) throws CPATransferException {

    BinaryOperator binaryOperator = binaryExp.getOperator();

    switch (binaryOperator) {
      case PLUS:
      case MINUS: {

        List<SMGAddressValueAndState> result = new ArrayList<>(4);

        SMGAddressValueAndStateList addressValueAndStates = evaluateAddress(
            initialSmgState, cfaEdge, address);

        for (SMGAddressValueAndState addressValueAndState : addressValueAndStates
            .asAddressValueAndStateList()) {

          SMGAddressValue addressValue = addressValueAndState.getObject();
          SMGState newState = addressValueAndState.getSmgState();
          List<SMGExplicitValueAndState> offsetValueAndStates = evaluateExplicitValue(
              newState, cfaEdge, pointerOffset);

          for (SMGExplicitValueAndState offsetValueAndState : offsetValueAndStates) {

            SMGExplicitValue offsetValue = offsetValueAndState.getObject();
            newState = offsetValueAndState.getSmgState();

            if (addressValue.isUnknown() || offsetValue.isUnknown()) {
              result.add(SMGAddressValueAndState.of(newState));
              continue;
            }

            SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getBitSizeof(
                cfaEdge, typeOfPointer, newState, address));
            SMGExplicitValue pointerOffsetValue = offsetValue.multiply(typeSize);
            SMGObject target = addressValue.getObject();
            SMGExplicitValue addressOffset = addressValue.getOffset();

            SMGExplicitValue newAddressOffset;
            switch (binaryOperator) {
              case PLUS:
                newAddressOffset = addressOffset.add(pointerOffsetValue);
                break;
              case MINUS:
                if (lVarIsAddress) {
                  newAddressOffset = addressOffset.subtract(pointerOffsetValue);
                  break;
                } else {
                  throw new UnrecognizedCCodeException("Expected pointer arithmetic "
                      + " with + or - but found " + binaryExp.toASTString(), binaryExp);
                }
              default:
                throw new AssertionError();
            }
            result.addAll(createAddress(newState, target, newAddressOffset).asAddressValueAndStateList());
          }
        }
        return SMGAddressValueAndStateList.copyOfAddressValueList(result);
      }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        throw new UnrecognizedCCodeException(
            "Misinterpreted the expression type of " + binaryExp + " as pointer type",
            cfaEdge, binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case MODULO:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        throw new UnrecognizedCCodeException("The operands of binary Expression "
            + binaryExp.toASTString() + " must have arithmetic types. "
            + address.toASTString() + " has a non arithmetic type",
            cfaEdge, binaryExp);

    default:
      return SMGAddressValueAndStateList.of(initialSmgState);
    }
  }

  List<SMGAddressAndState> evaluateArraySubscriptAddress(
      SMGState initialSmgState, CFAEdge cfaEdge, CArraySubscriptExpression exp)
          throws CPATransferException {

    List<SMGAddressAndState> result = new ArrayList<>(2);

    SMGAddressValueAndStateList arrayAddressAndStates = evaluateAddress(
        initialSmgState, cfaEdge, exp.getArrayExpression());

    for (SMGAddressValueAndState arrayAddressAndState : arrayAddressAndStates.asAddressValueAndStateList()) {
      SMGAddressValue arrayAddress = arrayAddressAndState.getObject();
      SMGState newState = arrayAddressAndState.getSmgState();

      CExpression subscriptExpression = exp.getSubscriptExpression();
      List<SMGExplicitValueAndState> subscriptValueAndStates = evaluateExplicitValue(
          newState, cfaEdge, subscriptExpression);

      for (SMGExplicitValueAndState subscriptValueAndState : subscriptValueAndStates) {
        SMGExplicitValue subscriptValue = subscriptValueAndState.getObject();
        newState = subscriptValueAndState.getSmgState();

        if (subscriptValue.isUnknown()) {
          if (newState.isTrackPredicatesEnabled()  && !arrayAddress.isUnknown()) {
            SMGValueAndStateList subscriptSymbolicValueAndStates =
                evaluateNonAddressValue(newState, cfaEdge, subscriptExpression);
            for (SMGValueAndState symbolicValueAndState: subscriptSymbolicValueAndStates.getValueAndStateList()) {
              SMGSymbolicValue value = symbolicValueAndState.getObject();
              newState = subscriptValueAndState.getSmgState();
              if (!value.isUnknown() && !newState
                  .isObjectExternallyAllocated(arrayAddress.getObject())) {
                int arrayBitSize = arrayAddress.getObject().getSize();
                int typeBitSize = getBitSizeof(cfaEdge, exp.getExpressionType(), newState, exp);
                int maxIndex = arrayBitSize / typeBitSize;
                int subscriptSize = getBitSizeof(cfaEdge, subscriptExpression.getExpressionType(), newState, exp);
                if (subscriptExpression instanceof CCastExpression) {
                  CCastExpression castExpression = (CCastExpression) subscriptExpression;
                  int originSize = getBitSizeof(cfaEdge, castExpression.getOperand().getExpressionType(), newState);
                  subscriptSize = Integer.min(subscriptSize, originSize);
                }
                newState.addErrorPredicate(value, subscriptSize, SMGKnownExpValue.valueOf(maxIndex),
                    subscriptSize, cfaEdge);
              }
            }
          } else {
            // assume address is invalid
            newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
          }

          result.add(SMGAddressAndState.of(newState));
          continue;
        }

        SMGExplicitValue typeSize = SMGKnownExpValue.valueOf(getBitSizeof(cfaEdge,
            exp.getExpressionType(), newState, exp));

        SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

        SMGAddressAndState addressAndStateResult =
            SMGAddressAndState.of(newState, arrayAddress.getAddress().add(subscriptOffset));
        result.add(addressAndStateResult);
      }
    }

    return result;
  }

  private SMGAddressValueAndStateList createAddresses(List<SMGAddressAndState> pAddresses)
      throws SMGInconsistentException {

    List<SMGAddressValueAndState> result = new ArrayList<>(pAddresses.size());

    for (SMGAddressAndState addressAndState : pAddresses) {
      result.addAll(createAddress(addressAndState).asAddressValueAndStateList());
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  private SMGAddressValueAndStateList createAddress(SMGAddressAndState addressAndState) throws SMGInconsistentException {

    SMGState state = addressAndState.getSmgState();
    SMGAddress address = addressAndState.getObject();

    if (address.isUnknown()) {
      return SMGAddressValueAndStateList.of(state);
    }

    return createAddress(state, address.getObject(), address.getOffset());
  }

  /**
   * Is given a list of symbolic Values, looks into the respective smgs to determine if the symbolic
   * values represents pointers, and transform them into a {@link SMGAddressValueAndStateList}.
   *
   * @param pAddressValueAndStateList This contains the list of smgs and symbolic values.
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  SMGAddressValueAndStateList getAddressFromSymbolicValues(SMGValueAndStateList pAddressValueAndStateList)
      throws SMGInconsistentException {

    if (pAddressValueAndStateList instanceof SMGAddressValueAndStateList) {
      return (SMGAddressValueAndStateList) pAddressValueAndStateList;
    } else {

      List<SMGAddressValueAndState> addressAndStateList = new ArrayList<>(pAddressValueAndStateList.size());

      for (SMGValueAndState valueAndState : pAddressValueAndStateList.getValueAndStateList()) {
        addressAndStateList.addAll(getAddressFromSymbolicValue(valueAndState).asAddressValueAndStateList());
      }
      return SMGAddressValueAndStateList.copyOfAddressValueList(addressAndStateList);
    }
  }

  /**
   * Is given a symbolic Value, looks into the smg to determine if the symbolic
   * value represents a pointer, and transforms it into a {@link SMGAddressValue}
   * containing the symbolic value that represents the pointer as well as the
   * address the pointer is pointing to.
   *
   * Because all values in C represent an
   * address, and can e cast to a pointer, the method returns a instance of
   * {@link SMGUnknownValue} if the symbolic value does not represent a pointer
   * in the smg.
   *
   * @param pAddressValueAndState This contains the SMG.
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  SMGAddressValueAndStateList getAddressFromSymbolicValue(SMGValueAndState pAddressValueAndState) throws SMGInconsistentException {

    if (pAddressValueAndState instanceof SMGAddressValueAndState) {
      return SMGAddressValueAndStateList.of((SMGAddressValueAndState) pAddressValueAndState);
    }

    SMGSymbolicValue pAddressValue = pAddressValueAndState.getObject();
    SMGState smgState = pAddressValueAndState.getSmgState();

    if (pAddressValue instanceof SMGAddressValue) {
      return SMGAddressValueAndStateList.of(SMGAddressValueAndState.of(smgState,
          (SMGAddressValue) pAddressValue));
    }

    if (pAddressValue.isUnknown()) {
      return SMGAddressValueAndStateList.of(smgState);
    }

    if (!smgState.isPointer(pAddressValue.getAsInt())) {
      return SMGAddressValueAndStateList.of(smgState);
    }

    SMGAddressValueAndStateList addressValues =
        smgState.getPointerFromValue(pAddressValue.getAsInt());

    return addressValues;
  }

  protected SMGAddressValueAndStateList createAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    SMGAddressValueAndStateList addressValueAndStates = getAddress(pSmgState, pTarget, pOffset);

    List<SMGAddressValueAndState> result = new ArrayList<>(addressValueAndStates.size());

    for (SMGAddressValueAndState addressValueAndState : addressValueAndStates.asAddressValueAndStateList()) {
      if (addressValueAndState.getObject().isUnknown()) {

        SMGKnownSymValue value = SMGKnownSymValue.valueOf(SMGValueFactory
            .getNewValue());
        SMGKnownAddVal addressValue = SMGKnownAddVal.valueOf(pTarget,
            (SMGKnownExpValue) pOffset, value);
        result.add(SMGAddressValueAndState.of(addressValueAndState.getSmgState(), addressValue));
      } else {
        result.add(addressValueAndState);
      }
    }

    return SMGAddressValueAndStateList.copyOfAddressValueList(result);
  }

  SMGAddressValueAndStateList getAddress(SMGState pSmgState, SMGObject pTarget,
      SMGExplicitValue pOffset) throws SMGInconsistentException {

    if (pTarget == null || pOffset.isUnknown()) {
      return SMGAddressValueAndStateList.of(pSmgState);
    }

    SMGRegion regionTarget;

    if(pTarget instanceof SMGRegion) {
      regionTarget = (SMGRegion) pTarget;
    } else if (pTarget == SMGNullObject.INSTANCE) {
      SMGAddressValueAndState result = SMGAddressValueAndState.of(pSmgState, SMGKnownAddVal.valueOf(0, pTarget, pOffset.getAsInt()));
      return SMGAddressValueAndStateList.of(result);
    } else {
      throw new AssertionError("Abstraction " + pTarget.toString() + " was not materialised.");
    }

    Integer address = pSmgState.getAddress(regionTarget, pOffset.getAsInt());

    if (address == null) {
      return SMGAddressValueAndStateList.of(pSmgState);
    }

    SMGAddressValueAndStateList addressValues = pSmgState.getPointerFromValue(address);

    return addressValues;
  }



  /*
   * These Methods are designed to be overwritten to enable
   * sub classes to, for example, change the smgState while
   * evaluating expressions.
   *
   */

  /**
   * @param edge the edge to handle
   */
  protected SMGValueAndState handleUnknownDereference(SMGState smgState, CFAEdge edge) {
    return SMGValueAndState.of(smgState);
  }

  public StructAndUnionVisitor getStructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new StructAndUnionVisitor(this, pCfaEdge, pNewState);
  }

  public ArrayVisitor getArrayVisitor(CFAEdge pCfaEdge, SMGState pSmgState) {
    return new ArrayVisitor(this, pCfaEdge, pSmgState);
  }

  public PointerVisitor getPointerVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new PointerVisitor(this, pCfaEdge, pNewState);
  }

  public AssumeVisitor getAssumeVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new AssumeVisitor(this, pCfaEdge, pNewState);
  }

  public ExpressionValueVisitor getExpressionValueVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new ExpressionValueVisitor(this, pCfaEdge, pNewState);
  }

  public LValueAssignmentVisitor getLValueAssignmentVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new LValueAssignmentVisitor(this, pCfaEdge, pNewState);
  }

  protected CSizeOfVisitor getSizeOfVisitor(CFAEdge pEdge, SMGState pState,
      Optional<CExpression> pExpression) {
    return new CSizeOfVisitor(this, pEdge, pState, pExpression);
  }
}
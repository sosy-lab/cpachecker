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
import java.math.BigInteger;
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
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGAddressValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGExplicitValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.evaluator.SMGAbstractObjectAndState.SMGValueAndState;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMGType;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddress;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGExplicitValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGField;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownExpValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymbolicValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGUnknownValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGZeroValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * This class evaluates expressions using {@link SMGState}. It should not change the {@link
 * SMGState}, to permit evaluating expressions independently of the transfer relation, enabling
 * other cpas to interact more easily with SMGCPA.
 */
public class SMGExpressionEvaluator {

  final LogManagerWithoutDuplicates logger;
  final MachineModel machineModel;

  public SMGExpressionEvaluator(LogManagerWithoutDuplicates pLogger, MachineModel pMachineModel) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  /**
   * Get the size of the given type in bits.
   *
   * <p>When handling variable array type length, additionally to the type itself, we also need the
   * cfa edge to determine the location of the program we currently handle, the smg state to
   * determine the values of the variables at the current location, and the expression with the
   * given type to determine the smg object that represents the array of the given type.
   *
   * @param pEdge The cfa edge that determines the location in the program.
   * @param pType We want to calculate the size of this type.
   * @param pState The state that contains the current variable values.
   * @param pExpression The expression, which evaluates to the value with the given type.
   * @return The size of the given type in bits.
   */
  public long getBitSizeof(CFAEdge pEdge, CType pType, SMGState pState, CExpression pExpression)
      throws UnrecognizedCodeException {
    return getBitSizeof(pEdge, pType, pState, Optional.of(pExpression));
  }

  /**
   * Get the size of the given type in bits.
   *
   * <p>When handling variable array type length, additionally to the type itself, we also need the
   * cfa edge to determine the location of the program we currently handle, and the smg state to
   * determine the values of the variables at the current location..
   *
   * <p>This method can't calculate variable array type length for arrays that are not declared in
   * the cfa edge.
   *
   * @param pEdge The cfa edge that determines the location in the program.
   * @param pType We want to calculate the size of this type.
   * @param pState The state that contains the current variable values.
   * @return The size of the given type in bits.
   */
  public long getBitSizeof(CFAEdge pEdge, CType pType, SMGState pState)
      throws UnrecognizedCodeException {
    return getBitSizeof(pEdge, pType, pState, Optional.empty());
  }

  private long getBitSizeof(
      CFAEdge edge, CType pType, SMGState pState, Optional<CExpression> pExpression)
      throws UnrecognizedCodeException {

    if (pType instanceof CBitFieldType) {
      return ((CBitFieldType) pType).getBitFieldSize();
    }

    CSizeOfVisitor v = getSizeOfVisitor(edge, pState, pExpression);

    try {
      return pType
          .accept(v)
          .multiply(BigInteger.valueOf(machineModel.getSizeofCharInBits()))
          .longValueExact();
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException("Could not resolve type.", edge);
    }
  }

  List<SMGAddressAndState> getAddressOfField(
      SMGState pSmgState, CFAEdge cfaEdge, CFieldReference fieldReference)
      throws CPATransferException {

    CExpression fieldOwner = fieldReference.getFieldOwner();
    CType ownerType = TypeUtils.getRealExpressionType(fieldOwner);
    List<SMGAddressAndState> result = new ArrayList<>(4);

    /* Points to the start of this struct or union.
     *
     * Note that whether this field Reference is a pointer dereference x->b
     * or not x.b is indirectly resolved by whether the type of x is
     * a pointer type, in which case its expression is evaluated, or
     * a struct type, in which case the address of the expression
     * similar is evaluated.
     */
    for (SMGAddressValueAndState fieldOwnerAddressAndState :
        evaluateAddress(pSmgState, cfaEdge, fieldOwner)) {

      SMGAddressValue fieldOwnerAddress = fieldOwnerAddressAndState.getObject();
      SMGState newState = fieldOwnerAddressAndState.getSmgState();
      String fieldName = fieldReference.getFieldName();
      SMGField field = getField(ownerType, fieldName);

      if (field.isUnknown() || fieldOwnerAddress.isUnknown()) {
        if (fieldReference.isPointerDereference()) {
          newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
        }
        result.add(SMGAddressAndState.withUnknownAddress(newState));
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

  public SMGValueAndState readValue(
      SMGState pSmgState, SMGObject pObject, SMGExplicitValue pOffset, CType pType, CFAEdge pEdge)
      throws SMGInconsistentException, UnrecognizedCodeException {

    if (pOffset.isUnknown() || pObject == null) {
      return SMGValueAndState.withUnknownValue(pSmgState);
    }

    long fieldOffset = pOffset.getAsLong();

    // FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject =
        fieldOffset < 0 || fieldOffset + getBitSizeof(pEdge, pType, pSmgState) > pObject.getSize();

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(
          Level.WARNING,
          pEdge.getFileLocation() + ":",
          "Field "
              + "("
              + fieldOffset
              + ", "
              + pType.toASTString("")
              + ")"
              + " does not fit object "
              + pObject
              + ".");

      return SMGValueAndState.withUnknownValue(pSmgState);
    }

    // We don't want to modify the state while reading
    SMGValue value =
        pSmgState
            .readValue(pObject, fieldOffset, machineModel.getSizeofInBits(pType).longValueExact())
            .getObject();

    return SMGValueAndState.of(pSmgState, value);
  }

  private SMGField getField(CType pOwnerType, String pFieldName) {

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

      type = TypeUtils.getRealExpressionType(type);

      return getField(type, pFieldName);
    }

    throw new AssertionError();
  }

  private SMGField getField(CCompositeType pOwnerType, String pFieldName) {
    CType resultType = pOwnerType;

    BigInteger offset = machineModel.getFieldOffsetInBits(pOwnerType, pFieldName);

    for (CCompositeTypeMemberDeclaration typeMember : pOwnerType.getMembers()) {
      if (typeMember.getName().equals(pFieldName)) {
        resultType = typeMember.getType();
      }
    }

    final SMGExplicitValue smgValue;
    if (!resultType.equals(pOwnerType)) {
      smgValue = SMGKnownExpValue.valueOf(offset);
      resultType = TypeUtils.getRealExpressionType(resultType);
    } else {
      smgValue = SMGUnknownValue.INSTANCE;
    }
    return new SMGField(smgValue, resultType);
  }

  public static boolean isStructOrUnionType(CType rValueType) {

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

  public SMGExplicitValue evaluateExplicitValueV2(
      SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    List<SMGExplicitValueAndState> result = evaluateExplicitValue(smgState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.get(0).getObject();
    } else {
      return SMGUnknownValue.INSTANCE;
    }
  }

  public List<SMGExplicitValueAndState> evaluateExplicitValue(
      SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    List<SMGExplicitValueAndState> result = new ArrayList<>();

    ExplicitValueVisitor visitor =
        new ExplicitValueVisitor(this, smgState, null, machineModel, logger, cfaEdge);

    Value value = rValue.accept(visitor);
    SMGState newState = visitor.getState();

    if (!value.isExplicitlyKnown() || !value.isNumericValue()) {

      // Sometimes, we can get the explicit Value from SMGCPA, especially if the
      // result happens to
      // be a pointer to the Null Object, or through reinterpretation
      for (SMGValueAndState symbolicValueAndState :
          evaluateExpressionValue(newState, cfaEdge, rValue)) {
        result.add(deriveExplicitValueFromSymbolicValue(symbolicValueAndState));
      }
    } else {
      BigInteger bigInteger = value.asNumericValue().bigInteger();
      result.add(SMGExplicitValueAndState.of(newState, SMGKnownExpValue.valueOf(bigInteger)));
    }

    for (SMGState additionalState : visitor.getSmgStatesToBeProccessed()) {
      result.addAll(evaluateExplicitValue(additionalState, cfaEdge, rValue));
    }

    return result;
  }

  private SMGExplicitValueAndState deriveExplicitValueFromSymbolicValue(
      SMGValueAndState symbolicValueAndState) {

    SMGValue symbolicValue = symbolicValueAndState.getObject();
    SMGState newState = symbolicValueAndState.getSmgState();

    if (!symbolicValue.isUnknown()) {
      if (symbolicValue.isZero()) {
        return SMGExplicitValueAndState.of(newState, SMGZeroValue.INSTANCE);
      }

      if (symbolicValue instanceof SMGAddressValue) {
        SMGAddressValue address = (SMGAddressValue) symbolicValue;

        if (address.getObject() == SMGNullObject.INSTANCE) {
          return SMGExplicitValueAndState.of(
              newState,
              SMGKnownExpValue.valueOf(
                  address.getOffset().getAsLong() / machineModel.getSizeofCharInBits()));
        }
      }
    }

    return SMGExplicitValueAndState.of(newState, SMGUnknownValue.INSTANCE);
  }

  public SMGValue evaluateExpressionValueV2(
      SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    List<? extends SMGValueAndState> result = evaluateExpressionValue(smgState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.get(0).getObject();
    } else {
      return SMGUnknownValue.INSTANCE;
    }
  }

  public List<? extends SMGValueAndState> evaluateExpressionValue(
      SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    CType expressionType = TypeUtils.getRealExpressionType(rValue);

    if (isAddressType(expressionType)) {
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

  public static boolean isAddressType(CType pExpressionType) {
    CType type = TypeUtils.getRealExpressionType(pExpressionType);
    return type instanceof CPointerType
        || type instanceof CArrayType
        || isStructOrUnionType(type)
        || type instanceof CFunctionType;
  }

  private List<? extends SMGValueAndState> evaluateNonAddressValue(
      SMGState newState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    return rValue.accept(getExpressionValueVisitor(cfaEdge, newState));
  }

  List<? extends SMGValueAndState> evaluateAssumptionValue(
      SMGState newState, CFAEdge cfaEdge, CExpression rValue) throws CPATransferException {
    return rValue.accept(getAssumeVisitor(cfaEdge, newState));
  }

  @Deprecated // unused
  public SMGValue evaluateAssumptionValueV2(SMGState newState, CFAEdge cfaEdge, CExpression rValue)
      throws CPATransferException {

    List<? extends SMGValueAndState> result = evaluateAssumptionValue(newState, cfaEdge, rValue);

    if (result.size() == 1) {
      return result.get(0).getObject();
    } else {
      return SMGUnknownValue.INSTANCE;
    }
  }

  public List<SMGAddressValueAndState> evaluateAddress(
      SMGState pState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {

    CType expressionType = TypeUtils.getRealExpressionType(rValue);

    if (expressionType instanceof CPointerType
        || (expressionType instanceof CFunctionType
            && rValue instanceof CUnaryExpression
            && ((CUnaryExpression) rValue).getOperator() == CUnaryExpression.UnaryOperator.AMPER)) {
      // Cfa treats &foo as CFunctionType

      PointerVisitor visitor = getPointerVisitor(cfaEdge, pState);
      return getAddressFromSymbolicValues(rValue.accept(visitor));
    } else if (isStructOrUnionType(expressionType)) {
      /* expressions with structs or unions as
       * result will be evaluated to their addresses.
       * The address can be used e.g. to copy the struct.
       */

      StructAndUnionVisitor visitor = getStructAndUnionVisitor(cfaEdge, pState);
      return createAddresses(rValue.accept(visitor));
    } else if (expressionType instanceof CArrayType) {

      ArrayVisitor visitor = getArrayVisitor(cfaEdge, pState);
      return createAddresses(rValue.accept(visitor));
    } else {
      throw new AssertionError(
          "The method evaluateAddress may not be called"
              + "with the type "
              + expressionType.toASTString(""));
    }
  }

  List<SMGAddressValueAndState> handlePointerArithmetic(
      SMGState initialSmgState,
      CFAEdge cfaEdge,
      CExpression address,
      CExpression pointerOffset,
      CType typeOfPointer,
      boolean lVarIsAddress,
      CBinaryExpression binaryExp)
      throws CPATransferException {

    BinaryOperator binaryOperator = binaryExp.getOperator();

    switch (binaryOperator) {
      case PLUS:
      case MINUS:
        {
          ImmutableList.Builder<SMGAddressValueAndState> result =
              ImmutableList.builderWithExpectedSize(4);

          for (SMGAddressValueAndState addressValueAndState :
              evaluateAddress(initialSmgState, cfaEdge, address)) {

            SMGAddressValue addressValue = addressValueAndState.getObject();
            SMGState newState = addressValueAndState.getSmgState();
            for (SMGExplicitValueAndState offsetValueAndState :
                evaluateExplicitValue(newState, cfaEdge, pointerOffset)) {

              SMGExplicitValue offsetValue = offsetValueAndState.getObject();
              newState = offsetValueAndState.getSmgState();

              if (addressValue.isUnknown() || offsetValue.isUnknown()) {
                result.add(SMGAddressValueAndState.of(newState));
                continue;
              }

              SMGExplicitValue typeSize =
                  SMGKnownExpValue.valueOf(getBitSizeof(cfaEdge, typeOfPointer, newState, address));
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
                    throw new UnrecognizedCodeException(
                        "Expected pointer arithmetic "
                            + " with + or - but found "
                            + binaryExp.toASTString(),
                        binaryExp);
                  }
                default:
                  throw new AssertionError();
              }
              result.addAll(createAddress(newState, target, newAddressOffset));
            }
          }
          return result.build();
        }

      case EQUALS:
      case NOT_EQUALS:
      case GREATER_THAN:
      case GREATER_EQUAL:
      case LESS_THAN:
      case LESS_EQUAL:
        throw new UnrecognizedCodeException(
            "Misinterpreted the expression type of " + binaryExp + " as pointer type",
            cfaEdge,
            binaryExp);
      case DIVIDE:
      case MULTIPLY:
      case MODULO:
      case SHIFT_LEFT:
      case SHIFT_RIGHT:
      case BINARY_AND:
      case BINARY_OR:
      case BINARY_XOR:
        throw new UnrecognizedCodeException(
            "The operands of binary Expression "
                + binaryExp.toASTString()
                + " must have arithmetic types. "
                + address.toASTString()
                + " has a non arithmetic type",
            cfaEdge,
            binaryExp);

      default:
        return singletonList(SMGAddressValueAndState.of(initialSmgState));
    }
  }

  List<SMGAddressAndState> evaluateArraySubscriptAddress(
      SMGState initialSmgState, CFAEdge cfaEdge, CArraySubscriptExpression exp)
      throws CPATransferException {

    List<SMGAddressAndState> result = new ArrayList<>(2);

    for (SMGAddressValueAndState arrayAddressAndState :
        evaluateAddress(initialSmgState, cfaEdge, exp.getArrayExpression())) {
      SMGAddressValue arrayAddress = arrayAddressAndState.getObject();

      CExpression subscriptExpression = exp.getSubscriptExpression();
      for (SMGExplicitValueAndState subscriptValueAndState :
          evaluateExplicitValue(arrayAddressAndState.getSmgState(), cfaEdge, subscriptExpression)) {
        SMGExplicitValue subscriptValue = subscriptValueAndState.getObject();
        SMGState newState = subscriptValueAndState.getSmgState();

        if (subscriptValue.isUnknown()) {
          if (newState.isTrackErrorPredicatesEnabled() && !arrayAddress.isUnknown()) {
            for (SMGValueAndState symbolicValueAndState :
                evaluateNonAddressValue(newState, cfaEdge, subscriptExpression)) {
              SMGValue value = symbolicValueAndState.getObject();
              newState = subscriptValueAndState.getSmgState();
              if (!value.isUnknown()
                  && !newState.getHeap().isObjectExternallyAllocated(arrayAddress.getObject())) {
                long arrayBitSize = arrayAddress.getObject().getSize();
                long typeBitSize = getBitSizeof(cfaEdge, exp.getExpressionType(), newState, exp);
                long maxIndex = arrayBitSize / typeBitSize;
                CType subscriptType = subscriptExpression.getExpressionType();
                SMGType subscriptSMGType =
                    SMGType.constructSMGType(subscriptType, newState, cfaEdge, this);

                if (subscriptExpression instanceof CCastExpression) {
                  CCastExpression castExpression = (CCastExpression) subscriptExpression;
                  SMGType subscriptOriginSMGType =
                      SMGType.constructSMGType(
                          castExpression.getOperand().getExpressionType(), newState, cfaEdge, this);
                  subscriptSMGType = new SMGType(subscriptSMGType, subscriptOriginSMGType);
                }
                newState.addErrorPredicate(
                    value, subscriptSMGType, SMGKnownExpValue.valueOf(maxIndex), cfaEdge);
              }
            }
          } else {
            if (newState.isCrashOnUnknownEnabled()) {
              throw new CPATransferException("Unknown array index");
            }
            // assume address is invalid
            newState = handleUnknownDereference(newState, cfaEdge).getSmgState();
          }

          result.add(SMGAddressAndState.withUnknownAddress(newState));
          continue;
        }

        SMGExplicitValue typeSize =
            SMGKnownExpValue.valueOf(getBitSizeof(cfaEdge, exp.getExpressionType(), newState, exp));

        SMGExplicitValue subscriptOffset = subscriptValue.multiply(typeSize);

        SMGAddressAndState addressAndStateResult =
            SMGAddressAndState.of(newState, arrayAddress.getAddress().add(subscriptOffset));
        result.add(addressAndStateResult);
      }
    }

    return result;
  }

  /**
   * transforms a list of Addresses (with SMGStates) into a list of AddressValues (with SMGStates).
   * If Address is unknown, AddressValue will also be unknown,
   */
  List<SMGAddressValueAndState> createAddresses(List<SMGAddressAndState> pAddresses)
      throws SMGInconsistentException {
    List<SMGAddressValueAndState> result = new ArrayList<>();
    for (SMGAddressAndState addressAndState : pAddresses) {
      SMGState state = addressAndState.getSmgState();
      SMGAddress address = addressAndState.getObject();
      result.addAll(state.getPointerFromAddress(address));
    }
    return result;
  }

  /**
   * Is given a list of symbolic Values, looks into the respective smgs to determine if the symbolic
   * values represents pointers, and transform them into a list of {@link SMGAddressValueAndState}.
   *
   * @param pAddressValueAndStateList This contains the list of smgs and symbolic values.
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  List<SMGAddressValueAndState> getAddressFromSymbolicValues(
      List<? extends SMGValueAndState> pAddressValueAndStateList) throws SMGInconsistentException {
    List<SMGAddressValueAndState> addressAndStateList = new ArrayList<>();
    for (SMGValueAndState valueAndState : pAddressValueAndStateList) {
      addressAndStateList.addAll(getAddressFromSymbolicValue(valueAndState));
    }
    return addressAndStateList;
  }

  /**
   * Is given a symbolic Value, looks into the smg to determine if the symbolic value represents a
   * pointer, and transforms it into a {@link SMGAddressValue} containing the symbolic value that
   * represents the pointer as well as the address the pointer is pointing to.
   *
   * <p>Because all values in C represent an address, and can e cast to a pointer, the method
   * returns a instance of {@link SMGUnknownValue} if the symbolic value does not represent a
   * pointer in the smg.
   *
   * @param pAddressValueAndState This contains the SMG.
   * @return The address, otherwise unknown
   * @throws SMGInconsistentException thrown if the symbolic address is misinterpreted as a pointer.
   */
  List<SMGAddressValueAndState> getAddressFromSymbolicValue(SMGValueAndState pAddressValueAndState)
      throws SMGInconsistentException {

    if (pAddressValueAndState instanceof SMGAddressValueAndState) {
      return singletonList((SMGAddressValueAndState) pAddressValueAndState);
    }

    SMGValue pAddressValue = pAddressValueAndState.getObject();
    SMGState smgState = pAddressValueAndState.getSmgState();

    if (pAddressValue instanceof SMGAddressValue) {
      return singletonList(SMGAddressValueAndState.of(smgState, (SMGAddressValue) pAddressValue));
    }

    if (pAddressValue.isUnknown()) {
      return singletonList(SMGAddressValueAndState.of(smgState, pAddressValue));
    }

    SMGKnownExpValue explicit = smgState.getExplicit(pAddressValue);
    if (explicit != null && !explicit.isUnknown()) {
      return singletonList(
          SMGAddressValueAndState.of(
              smgState,
              SMGKnownAddressValue.valueOf(
                  (SMGKnownSymbolicValue) pAddressValue, SMGNullObject.INSTANCE, explicit)));
    }

    if (!smgState.getHeap().isPointer(pAddressValue)) {
      return singletonList(SMGAddressValueAndState.of(smgState, pAddressValue));
    }

    return smgState.getPointerFromValue(pAddressValue);
  }

  /** returns all possible AddressValues for a given SMGObject with given offset. */
  List<SMGAddressValueAndState> createAddress(
      SMGState pSmgState, SMGObject pTarget, SMGExplicitValue pOffset)
      throws SMGInconsistentException {
    return pSmgState.getPointerFromAddress(SMGAddress.valueOf(pTarget, pOffset));
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
  SMGValueAndState handleUnknownDereference(SMGState smgState, CFAEdge edge) {
    return SMGValueAndState.withUnknownValue(smgState);
  }

  private StructAndUnionVisitor getStructAndUnionVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new StructAndUnionVisitor(this, pCfaEdge, pNewState);
  }

  ArrayVisitor getArrayVisitor(CFAEdge pCfaEdge, SMGState pSmgState) {
    return new ArrayVisitor(this, pCfaEdge, pSmgState);
  }

  PointerVisitor getPointerVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new PointerVisitor(this, pCfaEdge, pNewState);
  }

  public AssumeVisitor getAssumeVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new AssumeVisitor(this, pCfaEdge, pNewState);
  }

  ExpressionValueVisitor getExpressionValueVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new ExpressionValueVisitor(this, pCfaEdge, pNewState);
  }

  LValueAssignmentVisitor getLValueAssignmentVisitor(CFAEdge pCfaEdge, SMGState pNewState) {
    return new LValueAssignmentVisitor(this, pCfaEdge, pNewState);
  }

  CSizeOfVisitor getSizeOfVisitor(
      CFAEdge pEdge, SMGState pState, Optional<CExpression> pExpression) {
    return new CSizeOfVisitor(this, pEdge, pState, pExpression);
  }
}

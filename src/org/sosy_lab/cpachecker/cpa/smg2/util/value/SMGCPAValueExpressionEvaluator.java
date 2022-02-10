// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.smg.TypeUtils;
import org.sosy_lab.cpachecker.cpa.smg2.SMGSizeOfVisitor;
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGValueAndSMGState;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

@SuppressWarnings("unused")
public class SMGCPAValueExpressionEvaluator
    implements AddressEvaluator {
  // TODO: why does this implement a interface that just defines the methods in this very class?

  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  public SMGCPAValueExpressionEvaluator(
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  private Value throwUnsupportedOperationException(String methodNameString) {
    throw new AssertionError("The operation " + methodNameString + " is not yet supported.");
  }

  /**
   * Read the values and save the mapping of concrete (C values) to symbolic (SMGValues) values.
   *
   * <p>TODO: what is exactly needed here?
   */
  public Collection<SMGState> evaluateValues(
      SMGState pState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    /*
        CType expType = TypeUtils.getRealExpressionType(rValue);
        // TODO: Is the CFAEdge always a CReturnStatementEdge?
        Optional<CAssignment> returnAssignment = ((CReturnStatementEdge) cfaEdge).asAssignment();
        if (returnAssignment.isPresent()) {
          expType = returnAssignment.orElseThrow().getLeftHandSide().getExpressionType();
        }
    */
    return null;
  }

  /**
   * Evaluates the given value with the help of the edge on the current state and returns a new
   * state and the evaluated value. The state may change due to reading the value and the value may
   * be concrete values, an list (arrays etc.) of values or addresses of value/pointers/structs etc.
   *
   * <p>TODO: what is exactly needed here?
   */
  public Collection<ValueAndSMGState> evaluateExpressionValue(
      SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    if (isAddressType(rValue.getExpressionType())) {
      /*
       * expressions with Array Types as result are transformed. a = &(a[0])
       */

      /*
       * expressions with structs or unions as result will be evaluated to their addresses. The
       * address can be used e.g. to copy the struct.If the address is not in the SMG,
       * it is entered. If the address is unknown, it + its value are entered symbolicly.
       */
      return evaluateAddress(smgState, cfaEdge, rValue);
    } else {
      // derive value
      // return rValue.accept(new NonPointerExpressionVisitor(smgState, this));
      return null;
    }
  }

  /** Evaluates the input address and returns the ? for it. */
  private Collection<ValueAndSMGState> evaluateAddress(
      SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue) {
    // TODO Auto-generated method stub
    return null;
  }

  public static boolean isAddressType(CType cType) {
    CType type = getCanonicalType(cType);
    return type instanceof CPointerType
        || type instanceof CArrayType
        || type instanceof CFunctionType
        || isStructOrUnionType(type);
  }

  @Override
  public Collection<ValueAndSMGState> evaluateArraySubscriptAddress(
      SMGState pInitialSmgState, CExpression pExp) {
    // TODO Auto-generated method stub
    return null;
  }

  /** Evaluates the input address and returns the SMGValue/Object for it. */
  @Override
  public Collection<ValueAndSMGState> evaluateAddress(
      SMGState pInitialSmgState, CExpression pOperand) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<ValueAndSMGState> evaluateArrayAddress(
      SMGState pInitialSmgState, CExpression pOperand) {
    // TODO Auto-generated method stub
    return null;
  }

  /** Creates the SMGValue/Object for an address not yet encountered. */
  @Override
  public Collection<ValueAndSMGState> createAddress(SMGState pState, Value pValue) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Checks whether or not the {@link Value} already exists in the mapping and returns the mapped
   * {@link SMGValue} if it does. Else, create a new {@link SMGValue}, map it to the entered value
   * entered into this function and return it + the {@link SMGState} with the value mapping. The
   * SMGValue is not entered into the SMG, this has to be done when writing the value to it!
   *
   * @param pValue the value for the {@link SMGValue}.
   * @param state the current {@link SMGState}
   * @return the {@link SMGValue} created + the {@link SMGState} with the mapping of the value.
   */
  public SMGValueAndSMGState createNewValueAndMap(Value pValue, SMGState state) {
    return state.copyAndAddValue(pValue);
  }

  /*
   * Read the object form the address provided in the Value and the value from the object from the info in the CExpression.
   * Read is tricky once we use abstraction as the SMGs might be merged. In that case accurate read offsets and sizes are important!
   */
  @Override
  public ValueAndSMGState readValue(SMGState pState, Value value, CExpression pExp) {
    return readValue(pState, pState.getPointsToTarget(value), value, pExp);
  }

  public ValueAndSMGState readValue(
      SMGState pState, SMGObject object, Value value, CExpression pExpression) {
    if (!value.isExplicitlyKnown() || object.isZero()) {
      // TODO: Error handling for this!
      return ValueAndSMGState.ofUnknownValue(pState);
    }

    // TODO: visitor for type?
    BigInteger fieldOffset = value.asNumericValue().bigInteger();

    // FIXME Does not work with variable array length.
    boolean doesNotFitIntoObject =
        fieldOffset.compareTo(BigInteger.ZERO) < 0
            || fieldOffset.add(getBitSizeof(pState, pExpression)).compareTo(object.getSize()) > 0;

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      logger.log(
          Level.WARNING,
          pExpression.getFileLocation() + ":",
          "Field "
              + "("
              + fieldOffset
              + ", "
              + pExpression.getExpressionType().toASTString("")
              + ")"
              + " does not fit object "
              + object
              + ".");

      return ValueAndSMGState.ofUnknownValue(pState);
    }
    CType type = TypeUtils.getRealExpressionType(pExpression);

    return pState.readValue(
            object,
            fieldOffset,
        machineModel.getSizeofInBits(type));
  }

  /*
   * Get the address value of the entered field.
   */
  @Override
  public Collection<ValueAndSMGState> getAddressOfField(
      SMGState pInitialSmgState, CFieldReference pExpression) {
    // CExpression fieldOwner = pExpression.getFieldOwner();
    // CType ownerType = TypeUtils.getRealExpressionType(fieldOwner);
    // TODO: rework this method because of 2 reasons: 1. its not understandable and documented and
    // 2. because fields are linked by pointsToEdges, meaning we need only the address fo the
    // general field (SMGObject) and the PointsToEdge holds the offsets, meaning we have to check
    // those! Calculating the address + offset as a numeric value is pointless.
    /*
    return evaluateAddress(pInitialSmgState, fieldOwner)
        .stream()
        .map(
            addressAndState -> {
              Value addressValue = addressAndState.getValue();
              SMGState state = addressAndState.getState();
              String fieldName = pExpression.getFieldName();
              CTypeAndValue field = getField(ownerType, fieldName);
              if (field.getValue().isUnknown() || addressValue.isUnknown()) {
                if (pExpression.isPointerDereference()) {
                  state = handleUnknownDereference(state).getState();
                }
                Value fieldOffset = field.getValue().add(addressValue);
                return ValueAndSMGState.of(fieldOffset, state);
              }

              return ValueAndSMGState.ofUnknownValue(state);
            })
        .collect(ImmutableSet.toImmutableSet());
        */
    return null;
  }

  private CTypeAndValue getField(CType pType, String pFieldName) {

    if (pType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) pType).getRealType();

      if (realType == null) {
        return CTypeAndValue.ofUnknownValue(pType);
      }

      return getField(realType, pFieldName);
    } else if (pType instanceof CCompositeType) {
      return getField((CCompositeType) pType, pFieldName);
    } else if (pType instanceof CPointerType) {

      /*
       * We do not explicitly transform x->b, so when we try to get the field b the ownerType of x
       * is a pointer type.
       */

      CType type = ((CPointerType) pType).getType();

      type = TypeUtils.getRealExpressionType(type);

      return getField(type, pFieldName);
    }

    throw new AssertionError("Unknown CType found: " + pType);
  }

  private CTypeAndValue getField(CCompositeType pOwnerType, String pFieldName) {
    CType resultType = pOwnerType;

    BigInteger offset = machineModel.getFieldOffsetInBits(pOwnerType, pFieldName);

    // TODO: i need to look at this
    for (CCompositeTypeMemberDeclaration typeMember : pOwnerType.getMembers()) {
      if (typeMember.getName().equals(pFieldName)) {
        resultType = typeMember.getType();
      }
    }

    final Value value;
    if (!resultType.equals(pOwnerType)) {
      value = new NumericValue(offset);
      resultType = TypeUtils.getRealExpressionType(resultType);
    } else {
      value = UnknownValue.getInstance();
    }
    return CTypeAndValue.of(resultType, value);
  }

  @Override
  public ValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState) {
    return ValueAndSMGState.ofUnknownValue(pInitialSmgState);
  }

  @Override
  public ValueAndSMGState readValue(
      SMGState pSmgState, SMGObject pVariableObject, CExpression pExpression) {
    return readValue(pSmgState, pVariableObject, new NumericValue(0), pExpression);
  }

  /** TODO: Move all type related stuff into its own class once i rework getBitSizeOf */
  @Override
  public BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pExpression) {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return machineModel.getSizeofInBits(pExpression.getExpressionType());
  }

  // TODO: revisit this and decide if we want to split structs and unions because of the data
  // reinterpretation because unions will most likely not work with SMG join!
  /*
   * Structs and Union types are treated essentially the same in the SMGs. Note: Both can
   * contain methods. Unions can have data reinterpretations based on type.
   * This should be used to not confuse enums with structs/unions.
   */
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

  private BigInteger getBitSizeofType(
      CFAEdge edge, CType pType, SMGState pState, Optional<CExpression> pExpression)
      throws UnrecognizedCodeException {
    // We don't really care about volatility in C
    // Incomplete types can make problems when calculating the size as they might not have all
    // information to get the size in bits

    // The reason why we need a dedicated visitor and not use MachineModel is inside the visitor
    SMGSizeOfVisitor v = new SMGSizeOfVisitor(this, edge, pState, pExpression);

    // We multiply with char size as it is 8 bit ;D
    try {
      return pType.accept(v).multiply(BigInteger.valueOf(machineModel.getSizeofCharInBits()));
    } catch (IllegalArgumentException e) {
      logger.logDebugException(e);
      throw new UnrecognizedCodeException("Could not resolve type.", edge);
    }
  }

  public MachineModel getMachineModel() {
    return machineModel;
  }

  // Get canonical type information
  public static CType getCanonicalType(CType type) {
    return type.getCanonicalType();
  }

  public static CType getCanonicalType(CSimpleDeclaration decl) {
    return getCanonicalType(decl.getType());
  }

  public static CType getCanonicalType(CRightHandSide exp) {
    return getCanonicalType(exp.getExpressionType());
  }
}

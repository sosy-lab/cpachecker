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
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
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
import org.sosy_lab.cpachecker.cpa.smg2.SymbolicProgramConfiguration;
import org.sosy_lab.cpachecker.cpa.smg2.util.SMGObjectAndOffset;
import org.sosy_lab.cpachecker.cpa.value.symbolic.type.PointerExpression;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;

@SuppressWarnings("unused")
public class SMGCPAValueExpressionEvaluator {
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

  public Collection<ValueAndSMGState> evaluateArraySubscriptAddress(
      SMGState pInitialSmgState, CExpression pExp) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * Returns the offset in bits of the field in a struct/union type expression. Example:
   * struct.field1 with field 1 being the first field and 4 byte size, struct.field2 being the
   * second field with 4 bytes. The offset of field 1 would be 0, while the second one would be 4 *
   * 8.
   *
   * @param ownerExprType the {@link CType} of the owner of the field.
   * @param pFieldName the name of the field.
   * @return the offset in bits of a the field as a {@link BigInteger}.
   */
  public BigInteger getFieldOffset(CType ownerExprType, String pFieldName) {
    if (ownerExprType instanceof CElaboratedType) {

      // CElaboratedType is either a struct, union or enum. getRealType returns the correct type
      CType realType = ((CElaboratedType) ownerExprType).getRealType();

      if (realType == null) {
        // TODO: This is possible, i don't know when however, handle once i find out.
        throw new AssertionError();
      }

      return getFieldOffset(realType, pFieldName);
    } else if (ownerExprType instanceof CCompositeType) {

      // Struct or Union type
      return machineModel.getFieldOffsetInBits((CCompositeType) ownerExprType, pFieldName);
    } else if (ownerExprType instanceof CPointerType) {

      // structPointer -> field or (*structPointer).field
      CType type = getCanonicalType(((CPointerType) ownerExprType).getType());

      return getFieldOffset(type, pFieldName);
    }

    throw new AssertionError();
  }

  public Collection<ValueAndSMGState> evaluateArrayAddress(
      SMGState pInitialSmgState, CExpression pOperand) {
    // TODO Auto-generated method stub
    return null;
  }

  /** Creates the SMGValue/Object for an address not yet encountered. */
  public Collection<ValueAndSMGState> createAddress(SMGState pState, Value pValue) {
    // TODO Auto-generated method stub
    return null;
  }

  public SMGState addValueToState(SMGState pState, Value value) {
    return pState.copyAndAddValue(value).getSMGState();
  }

  /**
   * Read the global or stack variable whoes name is given as a {@link String} from the given {@link
   * SMGState} with the given size in bits as a {@link BigInteger}. The Value might be unknown
   * either because it was read as unknown or because the variable was not initialized. Note: this
   * expects that the given {@link CIdExpression} has a not null declaration.
   *
   * @param initialState the {@link SMGState} holding the {@link SymbolicProgramConfiguration} where
   *     the variable should be read from.
   * @param varName name of the global or stack variable to be read.
   * @param sizeInBits size of the type to be read.
   * @param exprForDebug the {@link CIdExpression} from which this variable is read. For debugging
   *     only.
   * @return {@link ValueAndSMGState} with the updated {@link SMGState} and the read {@link Value}.
   *     The Value might be unknown either because it was read as unknown or because the variable
   *     was not initialized.
   */
  public ValueAndSMGState readStackOrGlobalVariable(
      SMGState initialState,
      String varName,
      BigInteger offsetInBits,
      BigInteger sizeInBits,
      CIdExpression exprForDebug) {
    Optional<SMGObject> maybeObject =
        initialState.getMemoryModel().getObjectForVisibleVariable(varName);
    // If there is no object, the variable is not initialized
    if (maybeObject.isEmpty()) {
      // Return unknown; but remember that this was uninitialized
      SMGState errorState = initialState.withUninitializedVariableUsage(exprForDebug);
      return ValueAndSMGState.ofUnknownValue(initialState);
    }
    return readValue(initialState, maybeObject.orElseThrow(), offsetInBits, sizeInBits);
  }

  /**
   * Read the value at the address of the supplied {@link Value} at the offset with the size (type
   * size) given.
   *
   * @param pState current {@link SMGState}.
   * @param value the {@link Value} for the address of the memory to be read. This should map to a
   *     known {@link SMGObject} or a {@link SMGPointsToEdge}.
   * @param pOffset the offset as {@link BigInteger} in bits where to start reading in the object.
   * @param pSizeInBits the size of the type to read in bits as {@link BigInteger}.
   * @return {@link ValueAndSMGState} tuple for the read {@link Value} and the new {@link SMGState}.
   */
  public ValueAndSMGState readValueWithPointerDereference(
      SMGState pState, Value value, BigInteger pOffset, BigInteger pSizeInBits) {
    // This should hold, but shouldn't be important
    assert (value instanceof PointerExpression);

    SMGState currentState = pState;

    if (value.isUnknown()) {
      // The value is unknown and therefore does not point to a valid memory location
      // TODO: The analysis should stop in this case!
      return ValueAndSMGState.ofUnknownValue(
          currentState.withUnknownPointerDereferenceWhenReading(value));
    }

    // Get the SMGObject for the value
    Optional<SMGObjectAndOffset> maybeTargetAndOffset =
        currentState.getMemoryModel().dereferencePointer(value);
    if (maybeTargetAndOffset.isEmpty()) {
      // Not a known pointer

    }
    SMGObject object = maybeTargetAndOffset.orElseThrow().getSMGObject();

    // The object may be null if no such object exists, check and log if 0
    if (object.isZero()) {
      SMGState newState = currentState.withNullPointerDereferenceWhenReading(object);
      // TODO: The analysis should stop in this case!
      return ValueAndSMGState.ofUnknownValue(newState);
    }

    // The offset of the pointer used. (the pointer might point to a offset != 0, the other offset
    // needs to the added to that!)
    BigInteger baseOffset = maybeTargetAndOffset.orElseThrow().getOffsetForObject();
    BigInteger offset = baseOffset.add(pOffset);

    return readValue(currentState, object, offset, pSizeInBits);
  }

  private ValueAndSMGState readValue(
      SMGState currentState, SMGObject object, BigInteger offsetInBits, BigInteger sizeInBits) {
    // This is the most general read that should be used in the end by all read methods in this
    // class!

    // Check that the offset and offset + size actually fit into the SMGObject
    boolean doesNotFitIntoObject =
        offsetInBits.compareTo(BigInteger.ZERO) < 0
            || offsetInBits.add(sizeInBits).compareTo(object.getSize()) > 0;

    if (doesNotFitIntoObject) {
      // Field does not fit size of declared Memory
      // TODO: The analysis should stop in this case!
      return ValueAndSMGState.ofUnknownValue(
          currentState.withOutOfRangeRead(object, offsetInBits, sizeInBits));
    }
    // The read in SMGState checks for validity and external allocation
    return currentState.readValue(object, offsetInBits, sizeInBits);
  }

  /*
   * Get the address value of the entered field.
   */
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

  /*
   * Unknown dereference is different to null dereference in that the object that is dereferenced actually exists, but its unknown.
   * We simply return an unknown value and log it.
   */
  public ValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState) {

    return ValueAndSMGState.ofUnknownValue(pInitialSmgState);
  }

  /** TODO: Move all type related stuff into its own class once i rework getBitSizeOf */
  public BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pExpression) {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return getBitSizeof(pInitialSmgState, pExpression.getExpressionType());
  }

  public BigInteger getBitSizeof(SMGState pInitialSmgState, CType pType) {
    // TODO check why old implementation did not use machineModel
    // Because in abstracted SMGs we might need the current SMG to get the correct type info.
    // TODO: rework because of that.
    return machineModel.getSizeofInBits(pType);
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

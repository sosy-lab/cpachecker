// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util.value;

import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
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
import org.sosy_lab.cpachecker.cpa.smg2.SMGState;
import org.sosy_lab.cpachecker.cpa.smg2.util.CTypeAndCValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

@SuppressWarnings("unused")
public class SMGCPAValueExpressionEvaluator implements AddressEvaluator {

  private final LogManagerWithoutDuplicates logger;
  private final MachineModel machineModel;

  public SMGCPAValueExpressionEvaluator(
      MachineModel pMachineModel, LogManagerWithoutDuplicates pLogger) {
    logger = pLogger;
    machineModel = pMachineModel;
  }

  private Value throwUnsupportedOperationException(String methodNameString) {
    throw new AssertionError("The operation " + methodNameString + " is not yet supported.");
  }

  public Collection<SMGState> evaluateValues(
      SMGState pState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    return null;
  }

  public Collection<CValueAndSMGState> evaluateExpressionValue(
      SMGState smgState, CFAEdge cfaEdge, CRightHandSide rValue) throws CPATransferException {
    if (isAddressType(rValue.getExpressionType())) {
      /*
       * expressions with Array Types as result are transformed. a = &(a[0])
       */

      /*
       * expressions with structs or unions as result will be evaluated to their addresses. The
       * address can be used e.g. to copy the struct.
       */
      return evaluateAddress(smgState, cfaEdge, rValue);
    } else {
      // derive value
      return rValue.accept(new NonPointerExpressionVisitor(smgState, this));
    }
  }

  private Collection<CValueAndSMGState> evaluateAddress(
      SMGState pSmgState, CFAEdge pCfaEdge, CRightHandSide pRValue) {
    // TODO Auto-generated method stub
    return null;
  }

  public static boolean isAddressType(CType cType) {
    if (cType instanceof CElaboratedType) {
      CElaboratedType type = (CElaboratedType) cType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }

    if (cType instanceof CCompositeType) {
      CCompositeType type = (CCompositeType) cType;
      return type.getKind() != CComplexType.ComplexTypeKind.ENUM;
    }
    return cType instanceof CPointerType
        || cType instanceof CArrayType
        || cType instanceof CFunctionType;
  }

  @Override
  public Collection<CValueAndSMGState> evaluateArraySubscriptAddress(
      SMGState pInitialSmgState, CExpression pExp) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<CValueAndSMGState> evaluateAddress(
      SMGState pInitialSmgState, CExpression pOperand) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<CValueAndSMGState> evaluateArrayAddress(
      SMGState pInitialSmgState, CExpression pOperand) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Collection<CValueAndSMGState> createAddress(SMGState pState, CValue pValue) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CValueAndSMGState readValue(SMGState pState, CValue value, CExpression pExp) {
    return readValue(pState, pState.getPointsToTarget(value), value, pExp);
  }

  public CValueAndSMGState readValue(
      SMGState pState, SMGObject object, CValue value, CExpression pExpression) {
    if (value.isUnknown() || object.isZero()) {
      return CValueAndSMGState.ofUnknown(pState);
    }

    BigInteger fieldOffset = value.getExplicitValue();

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

      return CValueAndSMGState.ofUnknown(pState);
    }
    CType type = TypeUtils.getRealExpressionType(pExpression);

    return pState.readValue(object, fieldOffset, machineModel.getSizeofInBits(type));
  }

  @Override
  public Collection<CValueAndSMGState> getAddressOfField(
      SMGState pInitialSmgState, CFieldReference pExpression) {
    CExpression fieldOwner = pExpression.getFieldOwner();
    CType ownerType = TypeUtils.getRealExpressionType(fieldOwner);
    return evaluateAddress(pInitialSmgState, fieldOwner).stream()
        .map(
            addressAndState -> {
              CValue addressCValue = addressAndState.getValue();
              SMGState state = addressAndState.getState();
              String fieldName = pExpression.getFieldName();
              CTypeAndCValue field = getField(ownerType, fieldName);
              if (field.getValue().isUnknown() || addressCValue.isUnknown()) {
                if (pExpression.isPointerDereference()) {
                  state = handleUnknownDereference(state).getState();
                }
                CValue fieldOffset = field.getValue().add(addressCValue);
                return CValueAndSMGState.of(fieldOffset, state);
              }

              return CValueAndSMGState.ofUnknown(state);
            })
        .collect(ImmutableSet.toImmutableSet());
  }

  private CTypeAndCValue getField(CType pType, String pFieldName) {

    if (pType instanceof CElaboratedType) {

      CType realType = ((CElaboratedType) pType).getRealType();

      if (realType == null) {
        return CTypeAndCValue.withUnknownValue(pType);
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

  private CTypeAndCValue getField(CCompositeType pOwnerType, String pFieldName) {
    CType resultType = pOwnerType;

    BigInteger offset = machineModel.getFieldOffsetInBits(pOwnerType, pFieldName);

    for (CCompositeTypeMemberDeclaration typeMember : pOwnerType.getMembers()) {
      if (typeMember.getName().equals(pFieldName)) {
        resultType = typeMember.getType();
      }
    }

    final CValue cValue;
    if (!resultType.equals(pOwnerType)) {
      cValue = CValue.valueOf(offset);
      resultType = TypeUtils.getRealExpressionType(resultType);
    } else {
      cValue = CValue.getUnknownValue();
    }
    return CTypeAndCValue.of(resultType, cValue);
  }

  @Override
  public CValueAndSMGState handleUnknownDereference(SMGState pInitialSmgState) {
    return CValueAndSMGState.ofUnknown(pInitialSmgState);
  }

  @Override
  public CValueAndSMGState readValue(
      SMGState pSmgState, SMGObject pVariableObject, CExpression pExpression) {
    return readValue(pSmgState, pVariableObject, CValue.zero(), pExpression);
  }

  @Override
  public BigInteger getBitSizeof(SMGState pInitialSmgState, CExpression pExpression) {
    // TODO check why old implementation did not use machineModel
    return machineModel.getSizeofInBits(pExpression.getExpressionType());
  }
}

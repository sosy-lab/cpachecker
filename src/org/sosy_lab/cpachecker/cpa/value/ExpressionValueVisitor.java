// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value;

import java.math.BigInteger;
import java.util.Objects;
import java.util.OptionalLong;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.cpachecker.cfa.ast.AIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.java.JClassLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.java.JIdExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.ValueAndType;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.cpa.value.type.Value.UnknownValue;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * This Visitor returns the value from an expression. The result may be null, i.e., the value is
 * unknown.
 */
public class ExpressionValueVisitor extends AbstractExpressionValueVisitor {

  private boolean missingPointer = false;

  // This state is read-only! No writing or modification allowed!
  protected final ValueAnalysisState readableState;

  /**
   * This Visitor returns the numeral value for an expression.
   *
   * @param pState where to get the values for variables (identifiers)
   * @param pFunctionName current scope, used only for variable-names
   * @param pMachineModel where to get info about types, for casting and overflows
   * @param pLogger logging
   */
  public ExpressionValueVisitor(
      ValueAnalysisState pState,
      String pFunctionName,
      MachineModel pMachineModel,
      LogManagerWithoutDuplicates pLogger) {
    super(pFunctionName, pMachineModel, pLogger);
    readableState = pState;
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
  protected Value evaluateJIdExpression(JIdExpression varName) {
    return evaluateAIdExpression(varName);
  }

  /** This method returns the value of a variable from the current state. */
  private Value evaluateAIdExpression(AIdExpression varName) {

    final MemoryLocation memLoc;

    if (varName.getDeclaration() != null) {
      memLoc = MemoryLocation.forDeclaration(varName.getDeclaration());
    } else if (!ForwardingTransferRelation.isGlobal(varName)) {
      memLoc = MemoryLocation.forLocalVariable(getFunctionName(), varName.getName());
    } else {
      memLoc = MemoryLocation.forIdentifier(varName.getName());
    }

    if (readableState.contains(memLoc)) {
      return readableState.getValueFor(memLoc);
    } else {
      return Value.UnknownValue.getInstance();
    }
  }

  private Value evaluateLValue(CLeftHandSide pLValue) throws UnrecognizedCodeException {

    MemoryLocation varLoc = evaluateMemoryLocation(pLValue);

    if (varLoc == null) {
      return Value.UnknownValue.getInstance();
    }

    if (readableState.contains(varLoc)) {
      ValueAndType valueAndType = readableState.getValueAndTypeFor(varLoc);
      CType actualType = (CType) valueAndType.getType();
      CType readType = pLValue.getExpressionType();
      MachineModel machineModel = getMachineModel();
      if (Objects.equals(machineModel.getSizeof(readType), machineModel.getSizeof(actualType))) {

        if (doesRequireUnionFloatConversion(actualType, readType)) {
          if (valueAndType.getValue().isNumericValue()) {
            if (isFloatingPointType(actualType.getCanonicalType())) {
              return extractFloatingPointValueAsIntegralValue(
                  actualType.getCanonicalType(), valueAndType);
            } else if (isFloatingPointType(readType.getCanonicalType())) {
              return extractIntegralValueAsFloatingPointValue(
                  readType.getCanonicalType(), valueAndType);
            }
          }

          return UnknownValue.getInstance();
        }

        return valueAndType.getValue();
      }
    }
    return UnknownValue.getInstance();
  }

  private Value extractFloatingPointValueAsIntegralValue(
      CType pActualType, ValueAndType pValueAndType) {
    if (pActualType instanceof CSimpleType) {
      CBasicType basicType = ((CSimpleType) pActualType.getCanonicalType()).getType();
      NumericValue numericValue = pValueAndType.getValue().asNumericValue();

      if (basicType.equals(CBasicType.FLOAT)) {
        float floatValue = numericValue.floatValue();
        int intBits = Float.floatToIntBits(floatValue);

        return new NumericValue(intBits);
      } else if (basicType.equals(CBasicType.DOUBLE)) {
        double doubleValue = numericValue.doubleValue();
        long longBits = Double.doubleToLongBits(doubleValue);

        return new NumericValue(longBits);
      }
    }
    return UnknownValue.getInstance();
  }

  private Value extractIntegralValueAsFloatingPointValue(
      CType pReadType, ValueAndType pValueAndType) {
    if (pReadType instanceof CSimpleType) {
      CBasicType basicReadType = ((CSimpleType) pReadType.getCanonicalType()).getType();
      NumericValue numericValue = pValueAndType.getValue().asNumericValue();

      if (basicReadType.equals(CBasicType.FLOAT)) {
        int bits = numericValue.bigInteger().intValue();
        float floatValue = Float.intBitsToFloat(bits);

        return new NumericValue(floatValue);
      } else if (basicReadType.equals(CBasicType.DOUBLE)) {
        long bits = numericValue.bigInteger().longValue();
        double doubleValue = Double.longBitsToDouble(bits);

        return new NumericValue(doubleValue);
      }
    }
    return UnknownValue.getInstance();
  }

  private boolean doesRequireUnionFloatConversion(CType pSourceType, CType pTargetType) {
    CType sourceType = pSourceType.getCanonicalType();
    CType targetType = pTargetType.getCanonicalType();
    if (sourceType instanceof CSimpleType && targetType instanceof CSimpleType) {
      // if only one of them is no integer type, a conversion is necessary
      return isFloatingPointType(sourceType) != isFloatingPointType(targetType);
    } else {
      return false;
    }
  }

  private boolean isFloatingPointType(CType pType) {
    if (pType instanceof CSimpleType) {
      return ((CSimpleType) pType).getType().isFloatingPointType();
    }
    return false;
  }

  @Override
  protected Value evaluateCFieldReference(CFieldReference pLValue)
      throws UnrecognizedCodeException {
    return evaluateLValue(pLValue);
  }

  @Override
  protected Value evaluateCArraySubscriptExpression(CArraySubscriptExpression pLValue)
      throws UnrecognizedCodeException {
    return evaluateLValue(pLValue);
  }

  @Override
  protected Value evaluateCPointerExpression(CPointerExpression pVarName) {
    missingPointer = true;
    return Value.UnknownValue.getInstance();
  }

  public boolean canBeEvaluated(CExpression lValue) throws UnrecognizedCodeException {
    return evaluateMemoryLocation(lValue) != null;
  }

  public MemoryLocation evaluateMemoryLocation(CExpression lValue)
      throws UnrecognizedCodeException {
    return lValue.accept(new MemoryLocationEvaluator(this));
  }

  /**
   * Returns the {@link MemoryLocation} of a struct member. It is assumed that the struct of the
   * given type begins at the given memory location.
   *
   * @param pStartLocation the start location of the struct
   * @param pMemberName the name of the member to return the memory location for
   * @param pStructType the type of the struct
   * @return the memory location of the struct member
   */
  public @Nullable MemoryLocation evaluateRelativeMemLocForStructMember(
      MemoryLocation pStartLocation, String pMemberName, CCompositeType pStructType)
      throws UnrecognizedCodeException {

    MemoryLocationEvaluator locationEvaluator = new MemoryLocationEvaluator(this);

    return locationEvaluator.getStructureFieldLocationFromRelativePoint(
        pStartLocation, pMemberName, pStructType);
  }

  public MemoryLocation evaluateMemLocForArraySlot(
      final MemoryLocation pArrayStartLocation,
      final int pSlotNumber,
      final CArrayType pArrayType) {
    MemoryLocationEvaluator locationEvaluator = new MemoryLocationEvaluator(this);

    return locationEvaluator.getArraySlotLocationFromArrayStart(
        pArrayStartLocation, pSlotNumber, pArrayType);
  }

  @Override
  public Value visit(JClassLiteralExpression pJClassLiteralExpression) throws NoException {
    return UnknownValue.getInstance();
  }

  protected static class MemoryLocationEvaluator
      extends DefaultCExpressionVisitor<MemoryLocation, UnrecognizedCodeException> {

    private final ExpressionValueVisitor evv;

    public MemoryLocationEvaluator(ExpressionValueVisitor pEvv) {
      evv = pEvv;
    }

    @Override
    protected MemoryLocation visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public MemoryLocation visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws UnrecognizedCodeException {

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

      Value subscriptValue = subscript.accept(evv);

      if (!subscriptValue.isExplicitlyKnown() || !subscriptValue.isNumericValue()) {
        return null;
      }

      long typeSize = evv.getMachineModel().getSizeof(elementType).longValueExact();

      long subscriptOffset = subscriptValue.asNumericValue().longValue() * typeSize;

      return arrayLoc.withAddedOffset(subscriptOffset);
    }

    @Override
    public MemoryLocation visit(CFieldReference pIastFieldReference)
        throws UnrecognizedCodeException {

      if (pIastFieldReference.isPointerDereference()) {
        evv.missingPointer = true;
        return null;
      }

      CLeftHandSide fieldOwner = (CLeftHandSide) pIastFieldReference.getFieldOwner();

      MemoryLocation memLocOfFieldOwner = fieldOwner.accept(this);

      if (memLocOfFieldOwner == null) {
        return null;
      }

      return getStructureFieldLocationFromRelativePoint(
          memLocOfFieldOwner, pIastFieldReference.getFieldName(), fieldOwner.getExpressionType());
    }

    protected @Nullable MemoryLocation getStructureFieldLocationFromRelativePoint(
        MemoryLocation pStartLocation, String pFieldName, CType pOwnerType)
        throws UnrecognizedCodeException {

      CType canonicalOwnerType = pOwnerType.getCanonicalType();

      OptionalLong offset = getFieldOffsetInBits(canonicalOwnerType, pFieldName);

      if (!offset.isPresent()) {
        return null;
      }

      return pStartLocation.withAddedOffset(offset.orElseThrow());
    }

    private OptionalLong getFieldOffsetInBits(CType ownerType, String fieldName)
        throws UnrecognizedCodeException {

      if (ownerType instanceof CElaboratedType) {
        return getFieldOffsetInBits(((CElaboratedType) ownerType).getRealType(), fieldName);
      } else if (ownerType instanceof CCompositeType) {
        return bitsToByte(
            evv.getMachineModel().getFieldOffsetInBits((CCompositeType) ownerType, fieldName));
      } else if (ownerType instanceof CPointerType) {
        evv.missingPointer = true;
        return OptionalLong.empty();
      } else if (ownerType instanceof CProblemType) {
        /*
         * At this point CProblemType should not occur
         * unless the parsing of the automaton for
         * Counterexample-check failed to determine
         * the type of an assumptions operand.
         *
         * This is unfortunate but not as critical as
         * letting CPAchecker crash here.
         */
        return OptionalLong.empty();
      }

      throw new AssertionError();
    }

    private OptionalLong bitsToByte(BigInteger bits) {
      BigInteger charSizeInBits = BigInteger.valueOf(evv.getMachineModel().getSizeofCharInBits());
      BigInteger[] divAndRemainder = bits.divideAndRemainder(charSizeInBits);
      if (divAndRemainder[1].equals(BigInteger.ZERO)) {
        return OptionalLong.of(divAndRemainder[0].longValueExact());
      }
      return OptionalLong.empty();
    }

    protected MemoryLocation getArraySlotLocationFromArrayStart(
        final MemoryLocation pArrayStartLocation,
        final int pSlotNumber,
        final CArrayType pArrayType) {

      long typeSize = evv.getMachineModel().getSizeof(pArrayType.getType()).longValueExact();
      long offset = typeSize * pSlotNumber;

      return pArrayStartLocation.withAddedOffset(offset);
    }

    @Override
    public MemoryLocation visit(CIdExpression idExp) throws UnrecognizedCodeException {

      if (idExp.getDeclaration() != null) {
        return MemoryLocation.forDeclaration(idExp.getDeclaration());
      }

      boolean isGlobal = ForwardingTransferRelation.isGlobal(idExp);

      if (isGlobal) {
        return MemoryLocation.forIdentifier(idExp.getName());
      } else {
        return MemoryLocation.forLocalVariable(evv.getFunctionName(), idExp.getName());
      }
    }

    @Override
    public MemoryLocation visit(CPointerExpression pPointerExpression)
        throws UnrecognizedCodeException {
      evv.missingPointer = true;
      return null;
    }

    @Override
    public MemoryLocation visit(CCastExpression pE) throws UnrecognizedCodeException {
      // TODO reinterpretations for ValueAnalysis
      return pE.getOperand().accept(this);
    }
  }

  public ValueAnalysisState getState() {
    return readableState;
  }
}

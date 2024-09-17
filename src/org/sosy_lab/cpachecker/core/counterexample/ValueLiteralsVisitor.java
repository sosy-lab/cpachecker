// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.counterexample;

import com.google.common.base.Splitter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator.ExplicitValueLiteral;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator.SubExpressionValueLiteral;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator.UnknownValueLiteral;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator.ValueLiteral;
import org.sosy_lab.cpachecker.core.counterexample.AssumptionToEdgeAllocator.ValueLiterals;
import org.sosy_lab.cpachecker.cpa.value.AbstractExpressionValueVisitor;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;

public class ValueLiteralsVisitor extends DefaultCTypeVisitor<ValueLiterals, NoException> {

  private final AssumptionToEdgeAllocator assumptionToEdgeAllocator;
  private final Object value;
  private final CExpression exp;
  private final ConcreteState concreteState;

  public ValueLiteralsVisitor(
      AssumptionToEdgeAllocator pAssumptionToEdgeAllocator,
      Object pValue,
      CExpression pExp,
      ConcreteState pConcreteState) {
    assumptionToEdgeAllocator = pAssumptionToEdgeAllocator;
    value = pValue;
    exp = pExp;
    concreteState = pConcreteState;
  }

  @Override
  public ValueLiterals visitDefault(CType pT) {
    return createUnknownValueLiterals();
  }

  @Override
  public ValueLiterals visit(CPointerType pointerType) {
    Address address = Address.valueOf(value);
    if (address.isUnknown()) {
      return createUnknownValueLiterals();
    }
    ValueLiteral valueLiteral =
        ExplicitValueLiteral.valueOf(address, assumptionToEdgeAllocator.getMachineModel());
    ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);
    pointerType.accept(new ValueLiteralVisitor(address, valueLiterals, exp));
    return valueLiterals;
  }

  @Override
  public ValueLiterals visit(CArrayType arrayType) {
    Address address = Address.valueOf(value);
    if (address.isUnknown()) {
      return createUnknownValueLiterals();
    }

    ValueLiteral valueLiteral =
        ExplicitValueLiteral.valueOf(address, assumptionToEdgeAllocator.getMachineModel());
    ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);
    arrayType.accept(new ValueLiteralVisitor(address, valueLiterals, exp));
    return valueLiterals;
  }

  @Override
  public ValueLiterals visit(CElaboratedType pT) {
    CType realType = pT.getRealType();
    if (realType != null) {
      return realType.accept(this);
    }
    return createUnknownValueLiterals();
  }

  @Override
  public ValueLiterals visit(CEnumType pT) {

    /*We don't need to resolve enum types */
    return createUnknownValueLiterals();
  }

  @Override
  public ValueLiterals visit(CFunctionType pT) {

    // TODO Implement function resolving for comments
    return createUnknownValueLiterals();
  }

  @Override
  public ValueLiterals visit(CSimpleType simpleType) {
    return new ValueLiterals(getValueLiteral(simpleType, value));
  }

  @Override
  public ValueLiterals visit(CBitFieldType pCBitFieldType) {
    return pCBitFieldType.getType().accept(this);
  }

  @Override
  public ValueLiterals visit(CProblemType pT) {
    return createUnknownValueLiterals();
  }

  @Override
  public ValueLiterals visit(CTypedefType pT) {
    return pT.getRealType().accept(this);
  }

  @Override
  public ValueLiterals visit(CCompositeType compType) {

    if (compType.getKind() == ComplexTypeKind.ENUM) {
      return createUnknownValueLiterals();
    }

    Address address = Address.valueOf(value);

    if (address.isUnknown()) {
      return createUnknownValueLiterals();
    }

    ValueLiteral valueLiteral =
        ExplicitValueLiteral.valueOf(address, assumptionToEdgeAllocator.getMachineModel());
    ValueLiterals valueLiterals = new ValueLiterals(valueLiteral);
    compType.accept(new ValueLiteralVisitor(address, valueLiterals, exp));
    return valueLiterals;
  }

  protected ValueLiteral getValueLiteral(CSimpleType pSimpleType, Object pValue) {
    CSimpleType simpleType = pSimpleType.getCanonicalType();
    CBasicType basicType = simpleType.getType();

    switch (basicType) {
      case BOOL:
      case CHAR:
      case INT:
        return handleIntegerNumbers(pValue, simpleType);
      case FLOAT:
      case DOUBLE:
        if (assumptionToEdgeAllocator.assumeLinearArithmetics()) {
          break;
        }
        return handleFloatingPointNumbers(pValue, simpleType);
      default:
        break;
    }

    return UnknownValueLiteral.getInstance();
  }

  private ValueLiterals createUnknownValueLiterals() {
    return new ValueLiterals();
  }

  private ValueLiteral handleFloatingPointNumbers(Object pValue, CSimpleType pType) {

    if (pValue instanceof Rational) {
      double val = ((Rational) pValue).doubleValue();
      if (Double.isInfinite(val) || Double.isNaN(val)) {
        // TODO return correct value
        return UnknownValueLiteral.getInstance();
      }
      return ExplicitValueLiteral.valueOf(new BigDecimal(val), pType);

    } else if (pValue instanceof Double) {
      double doubleValue = ((Double) pValue);
      if (Double.isInfinite(doubleValue) || Double.isNaN(doubleValue)) {
        // TODO return correct value
        return UnknownValueLiteral.getInstance();
      }
      return ExplicitValueLiteral.valueOf(BigDecimal.valueOf(doubleValue), pType);
    } else if (pValue instanceof Float) {
      float floatValue = ((Float) pValue);
      if (Float.isInfinite(floatValue) || Double.isNaN(floatValue)) {
        // TODO return correct value
        return UnknownValueLiteral.getInstance();
      }
      return ExplicitValueLiteral.valueOf(BigDecimal.valueOf(floatValue), pType);
    }

    BigDecimal val;

    // TODO support rationals
    try {
      val = new BigDecimal(pValue.toString());
    } catch (NumberFormatException e) {

      assumptionToEdgeAllocator
          .getLogger()
          .log(Level.INFO, "Can't parse " + value + " as value for the counter-example path.");
      return UnknownValueLiteral.getInstance();
    }

    return ExplicitValueLiteral.valueOf(val, pType);
  }

  public void resolveStruct(
      CType type, ValueLiterals pValueLiterals, CIdExpression pOwner, String pFunctionName) {

    ValueLiteralStructResolver v =
        new ValueLiteralStructResolver(pValueLiterals, pFunctionName, pOwner);
    type.accept(v);
  }

  private ValueLiteral handleIntegerNumbers(Object pValue, CSimpleType pType) {

    String valueStr = pValue.toString();

    if (valueStr.matches("((-)?)\\d*")) {
      BigInteger integerValue = new BigInteger(valueStr);

      return handlePotentialIntegerOverflow(integerValue, pType);
    } else {
      List<String> numberParts = Splitter.on('.').splitToList(valueStr);

      if (numberParts.size() == 2
          && numberParts.get(1).matches("0*")
          && numberParts.get(0).matches("((-)?)\\d*")) {

        BigInteger integerValue = new BigInteger(numberParts.get(0));
        return handlePotentialIntegerOverflow(integerValue, pType);
      }
    }

    ValueLiteral valueLiteral = handleFloatingPointNumbers(pValue, pType);
    return valueLiteral.isUnknown() ? valueLiteral : valueLiteral.addCast(pType);
  }

  /**
   * Creates a value literal for the given value or computes its wrap-around if it does not fit into
   * the specified type.
   *
   * @param pIntegerValue the value.
   * @param pType the type.
   * @return the value literal.
   */
  public ValueLiteral handlePotentialIntegerOverflow(BigInteger pIntegerValue, CSimpleType pType) {

    BigInteger lowerInclusiveBound =
        assumptionToEdgeAllocator.getMachineModel().getMinimalIntegerValue(pType);
    BigInteger upperInclusiveBound =
        assumptionToEdgeAllocator.getMachineModel().getMaximalIntegerValue(pType);

    assert lowerInclusiveBound.compareTo(upperInclusiveBound) < 0;

    if (pIntegerValue.compareTo(lowerInclusiveBound) < 0
        || pIntegerValue.compareTo(upperInclusiveBound) > 0) {
      if (assumptionToEdgeAllocator.assumeLinearArithmetics()) {
        return UnknownValueLiteral.getInstance();
      }
      LogManagerWithoutDuplicates logManager =
          assumptionToEdgeAllocator.getLogger() instanceof LogManagerWithoutDuplicates
              ? (LogManagerWithoutDuplicates) assumptionToEdgeAllocator.getLogger()
              : new LogManagerWithoutDuplicates(assumptionToEdgeAllocator.getLogger());
      Value castValue =
          AbstractExpressionValueVisitor.castCValue(
              new NumericValue(pIntegerValue),
              pType,
              assumptionToEdgeAllocator.getMachineModel(),
              logManager,
              FileLocation.DUMMY);
      if (castValue.isUnknown()) {
        return UnknownValueLiteral.getInstance();
      }

      Number number = castValue.asNumericValue().getNumber();
      final BigInteger valueAsBigInt;
      if (number instanceof BigInteger) {
        valueAsBigInt = (BigInteger) number;
      } else {
        valueAsBigInt = BigInteger.valueOf(number.longValue());
      }
      pType = enlargeTypeIfValueIsMinimalValue(pType, valueAsBigInt);
      return ExplicitValueLiteral.valueOf(valueAsBigInt, pType);
    }

    return ExplicitValueLiteral.valueOf(pIntegerValue, pType);
  }

  private CSimpleType enlargeTypeIfValueIsMinimalValue(
      CSimpleType pType, final BigInteger valueAsBigInt) {
    // In C there are no negative literals, so to represent the minimal value of an integer
    // type, we need that number as positive literal of the next larger type,
    // and then negate it.
    // For example for LONG_MIN we want to have -9223372036854775808UL, so the literal is
    // of type unsigned long and negated. This is only important when exporting the value
    // e.g. inside a witness, since EclipseCDT will not like -9223372036854775808L.
    if (valueAsBigInt
                .abs()
                .compareTo(
                    assumptionToEdgeAllocator.getMachineModel().getMaximalIntegerValue(pType))
            > 0
        && valueAsBigInt.compareTo(BigInteger.ZERO) < 0
        && pType.getType().isIntegerType()) {
      while (valueAsBigInt
                  .abs()
                  .compareTo(
                      assumptionToEdgeAllocator.getMachineModel().getMaximalIntegerValue(pType))
              > 0
          && !nextLargerIntegerTypeIfPossible(pType).equals(pType)) {
        pType = nextLargerIntegerTypeIfPossible(pType);
      }
    }
    return pType;
  }

  private CSimpleType nextLargerIntegerTypeIfPossible(CSimpleType pType) {
    if (pType.hasSignedSpecifier()) {
      return new CSimpleType(
          pType.isConst(),
          pType.isVolatile(),
          pType.getType(),
          pType.hasLongSpecifier(),
          pType.hasShortSpecifier(),
          false,
          true,
          pType.hasComplexSpecifier(),
          pType.hasImaginarySpecifier(),
          pType.hasLongLongSpecifier());
    } else {
      switch (pType.getType()) {
        case INT:
          if (pType.hasShortSpecifier()) {
            return CNumericTypes.SIGNED_INT;
          } else if (pType.hasLongSpecifier()) {
            return CNumericTypes.SIGNED_LONG_LONG_INT;
          } else if (pType.hasLongLongSpecifier()) {
            // fall through, this is already the largest type
          } else {
            // if it had neither specifier it is a plain (unsigned) int
            return CNumericTypes.SIGNED_LONG_INT;
          }
        // $FALL-THROUGH$
        default:
          // just log and do not throw an exception in order to not break things
          assumptionToEdgeAllocator
              .getLogger()
              .logf(Level.WARNING, "Cannot find next larger type for %s", pType);
          return pType;
      }
    }
  }

  /** Resolves all subexpressions that can be resolved. Stops at duplicate memory location. */
  private class ValueLiteralVisitor extends DefaultCTypeVisitor<Void, NoException> {

    /*Contains references already visited, to avoid descending indefinitely.
     *Shares a reference with all instanced Visitors resolving the given type.*/
    private final Set<Pair<CType, Address>> visited;

    /*
     * Contains the address of the super type of the visited type.
     *
     */
    private final Address address;
    private final ValueLiterals valueLiterals;

    private final CExpression subExpression;

    public ValueLiteralVisitor(
        Address pAddress, ValueLiterals pValueLiterals, CExpression pSubExp) {
      address = pAddress;
      valueLiterals = pValueLiterals;
      visited = new HashSet<>();
      subExpression = pSubExp;
    }

    private ValueLiteralVisitor(
        Address pAddress,
        ValueLiterals pValueLiterals,
        CExpression pSubExp,
        Set<Pair<CType, Address>> pVisited) {
      address = pAddress;
      valueLiterals = pValueLiterals;
      visited = pVisited;
      subExpression = pSubExp;
    }

    @Override
    public @Nullable Void visitDefault(CType pT) {
      return null;
    }

    @Override
    public @Nullable Void visit(CTypedefType pT) {
      return pT.getRealType().accept(this);
    }

    @Override
    public @Nullable Void visit(CElaboratedType pT) {
      CType realType = pT.getRealType();
      return realType == null ? null : realType.getCanonicalType().accept(this);
    }

    @Override
    public @Nullable Void visit(CEnumType pT) {
      return null;
    }

    @Override
    public @Nullable Void visit(CBitFieldType pCBitFieldType) {
      return pCBitFieldType.getType().accept(this);
    }

    @Override
    public @Nullable Void visit(CCompositeType compType) {

      // TODO handle enums and unions

      if (compType.getKind() == ComplexTypeKind.STRUCT) {
        handleStruct(compType);
      }

      return null;
    }

    private void handleStruct(CCompositeType pCompType) {
      Address fieldAddress = address;
      if (!fieldAddress.isConcrete()) {
        return;
      }

      Map<CCompositeTypeMemberDeclaration, BigInteger> bitOffsets =
          assumptionToEdgeAllocator.getMachineModel().getAllFieldOffsetsInBits(pCompType);

      for (Map.Entry<CCompositeTypeMemberDeclaration, BigInteger> memberBitOffset :
          bitOffsets.entrySet()) {
        CCompositeTypeMemberDeclaration memberType = memberBitOffset.getKey();
        Optional<BigInteger> memberOffset =
            AssumptionToEdgeAllocator.bitsToByte(
                memberBitOffset.getValue(), assumptionToEdgeAllocator.getMachineModel());
        // TODO this looses values of bit fields
        if (memberOffset.isPresent()) {
          handleMemberField(memberType, address.addOffset(memberOffset.orElseThrow()));
        }
      }
    }

    private void handleMemberField(CCompositeTypeMemberDeclaration pType, Address fieldAddress) {
      CType expectedType = pType.getType().getCanonicalType();

      assert assumptionToEdgeAllocator.isStructOrUnionType(
          subExpression.getExpressionType().getCanonicalType());

      CExpression subExp;
      boolean isPointerDeref;

      if (subExpression instanceof CPointerExpression) {
        // *a.b <=> a->b
        subExp = ((CPointerExpression) subExpression).getOperand();
        isPointerDeref = true;
      } else {
        subExp = subExpression;
        isPointerDeref = false;
      }

      CFieldReference fieldReference =
          new CFieldReference(
              subExp.getFileLocation(), expectedType, pType.getName(), subExp, isPointerDeref);

      @Nullable Object fieldValue;

      // Arrays and structs are represented as addresses
      if (expectedType instanceof CArrayType
          || assumptionToEdgeAllocator.isStructOrUnionType(expectedType)) {
        fieldValue = fieldAddress;
      } else {
        fieldValue = concreteState.getValueFromMemory(fieldReference, fieldAddress);
      }

      if (fieldValue == null) {
        return;
      }

      ValueLiteral valueLiteral;
      Address valueAddress = Address.getUnknownAddress();

      if (expectedType instanceof CSimpleType) {
        valueLiteral = getValueLiteral(((CSimpleType) expectedType), fieldValue);
      } else {
        valueAddress = Address.valueOf(fieldValue);

        if (valueAddress.isUnknown()) {
          return;
        }

        valueLiteral =
            ExplicitValueLiteral.valueOf(valueAddress, assumptionToEdgeAllocator.getMachineModel());
      }

      Pair<CType, Address> visits = Pair.of(expectedType, fieldAddress);

      if (visited.contains(visits)) {
        return;
      }

      if (!valueLiteral.isUnknown()) {
        visited.add(visits);
        valueLiterals.addSubExpressionValueLiteral(
            new SubExpressionValueLiteral(valueLiteral, fieldReference));
      }

      if (valueAddress != null) {
        ValueLiteralVisitor v =
            new ValueLiteralVisitor(valueAddress, valueLiterals, fieldReference, visited);
        expectedType.accept(v);
      }
    }

    @Override
    public @Nullable Void visit(CArrayType arrayType) {
      if (!address.isConcrete()) {
        return null;
      }
      // For the bound check in handleArraySubscript() we need a statically known size,
      // otherwise we would loop infinitely.
      // TODO in principle we could extract the runtime size from the state?
      if (!arrayType.hasKnownConstantSize()) {
        return null;
      }

      CType expectedType = arrayType.getType().getCanonicalType();
      int subscript = 0;
      boolean memoryHasValue = true;
      while (memoryHasValue) {
        memoryHasValue = handleArraySubscript(address, subscript, expectedType, arrayType);
        subscript++;
      }
      return null;
    }

    private boolean handleArraySubscript(
        Address pArrayAddress, int pSubscript, CType pExpectedType, CArrayType pArrayType) {
      BigInteger typeSize = assumptionToEdgeAllocator.getMachineModel().getSizeof(pExpectedType);
      BigInteger subscriptOffset = BigInteger.valueOf(pSubscript).multiply(typeSize);

      // Check if we are already out of array bound
      if (assumptionToEdgeAllocator
              .getMachineModel()
              .getSizeof(pArrayType)
              .compareTo(subscriptOffset)
          <= 0) {
        return false;
      }

      Address arrayAddressWithOffset = pArrayAddress.addOffset(subscriptOffset);

      BigInteger subscript = BigInteger.valueOf(pSubscript);
      CIntegerLiteralExpression litExp =
          new CIntegerLiteralExpression(FileLocation.DUMMY, CNumericTypes.INT, subscript);
      CArraySubscriptExpression arraySubscript =
          new CArraySubscriptExpression(
              subExpression.getFileLocation(), pExpectedType, subExpression, litExp);

      @Nullable Object concreteValue;

      if (assumptionToEdgeAllocator.isStructOrUnionType(pExpectedType)
          || pExpectedType instanceof CArrayType) {
        // Arrays and structs are represented as addresses
        concreteValue = arrayAddressWithOffset;
      } else {
        concreteValue = concreteState.getValueFromMemory(arraySubscript, arrayAddressWithOffset);
      }

      if (concreteValue == null) {
        return false;
      }

      ValueLiteral valueLiteral;
      Address valueAddress = Address.getUnknownAddress();

      if (pExpectedType instanceof CSimpleType) {
        valueLiteral = getValueLiteral(((CSimpleType) pExpectedType), concreteValue);
      } else {
        valueAddress = Address.valueOf(concreteValue);

        if (valueAddress.isUnknown()) {
          return false;
        }

        valueLiteral =
            ExplicitValueLiteral.valueOf(valueAddress, assumptionToEdgeAllocator.getMachineModel());
      }

      if (!valueLiteral.isUnknown()) {
        SubExpressionValueLiteral subExpressionValueLiteral =
            new SubExpressionValueLiteral(valueLiteral, arraySubscript);
        valueLiterals.addSubExpressionValueLiteral(subExpressionValueLiteral);
      }

      if (!valueAddress.isUnknown()) {
        Pair<CType, Address> visits = Pair.of(pExpectedType, valueAddress);

        if (visited.contains(visits)) {
          return false;
        }

        visited.add(visits);

        ValueLiteralVisitor v =
            new ValueLiteralVisitor(valueAddress, valueLiterals, arraySubscript, visited);
        pExpectedType.accept(v);
      }

      // the check if the array continued was performed at an earlier stage in this function
      return true;
    }

    @Override
    public @Nullable Void visit(CPointerType pointerType) {

      CType expectedType = pointerType.getType().getCanonicalType();

      CPointerExpression pointerExp =
          new CPointerExpression(subExpression.getFileLocation(), expectedType, subExpression);

      @Nullable Object concreteValue;

      if (assumptionToEdgeAllocator.isStructOrUnionType(expectedType)
          || expectedType instanceof CArrayType) {
        // Arrays and structs are represented as addresses
        concreteValue = address;
      } else {
        concreteValue = concreteState.getValueFromMemory(pointerExp, address);
      }

      if (concreteValue == null) {
        return null;
      }

      ValueLiteral valueLiteral;
      Address valueAddress = Address.getUnknownAddress();

      if (expectedType instanceof CSimpleType) {
        valueLiteral = getValueLiteral(((CSimpleType) expectedType), concreteValue);
      } else {
        valueAddress = Address.valueOf(concreteValue);

        if (valueAddress.isUnknown()) {
          return null;
        }

        valueLiteral =
            ExplicitValueLiteral.valueOf(valueAddress, assumptionToEdgeAllocator.getMachineModel());
      }

      if (!valueLiteral.isUnknown()) {

        SubExpressionValueLiteral subExpressionValueLiteral =
            new SubExpressionValueLiteral(valueLiteral, pointerExp);

        valueLiterals.addSubExpressionValueLiteral(subExpressionValueLiteral);
      }

      if (!valueAddress.isUnknown()) {

        Pair<CType, Address> visits = Pair.of(expectedType, valueAddress);

        if (visited.contains(visits)) {
          return null;
        }

        /*Tell all instanced visitors that you visited this memory location*/
        visited.add(visits);

        ValueLiteralVisitor v =
            new ValueLiteralVisitor(valueAddress, valueLiterals, pointerExp, visited);
        expectedType.accept(v);
      }

      return null;
    }
  }

  /*Resolve structs or union fields that are stored in the variable environment*/
  private class ValueLiteralStructResolver extends DefaultCTypeVisitor<Void, NoException> {

    private final ValueLiterals valueLiterals;
    private final String functionName;
    private final CExpression prevSub;

    public ValueLiteralStructResolver(
        ValueLiterals pValueLiterals, String pFunctionName, CFieldReference pPrevSub) {
      valueLiterals = pValueLiterals;
      functionName = pFunctionName;
      prevSub = pPrevSub;
    }

    public ValueLiteralStructResolver(
        ValueLiterals pValueLiterals, String pFunctionName, CIdExpression pOwner) {
      valueLiterals = pValueLiterals;
      functionName = pFunctionName;
      prevSub = pOwner;
    }

    @Override
    public @Nullable Void visitDefault(CType pT) {
      return null;
    }

    @Override
    public @Nullable Void visit(CElaboratedType type) {
      CType realType = type.getRealType();
      return realType == null ? null : realType.getCanonicalType().accept(this);
    }

    @Override
    public @Nullable Void visit(CTypedefType pType) {
      return pType.getRealType().accept(this);
    }

    @Override
    public @Nullable Void visit(CBitFieldType pCBitFieldType) {
      return pCBitFieldType.getType().accept(this);
    }

    @Override
    public @Nullable Void visit(CCompositeType compType) {

      if (compType.getKind() == ComplexTypeKind.ENUM) {
        return null;
      }

      for (CCompositeTypeMemberDeclaration memberType : compType.getMembers()) {
        handleField(memberType.getName(), memberType.getType());
      }

      return null;
    }

    private void handleField(String pFieldName, CType pMemberType) {

      // Can't have pointer dereferences here.
      CFieldReference reference =
          new CFieldReference(prevSub.getFileLocation(), pMemberType, pFieldName, prevSub, false);

      FieldReference fieldReferenceName =
          assumptionToEdgeAllocator.getFieldReference(reference, functionName);

      if (concreteState.hasValueForLeftHandSide(fieldReferenceName)) {
        Object referenceValue = concreteState.getVariableValue(fieldReferenceName);
        addStructSubexpression(referenceValue, reference);
      }

      ValueLiteralStructResolver resolver =
          new ValueLiteralStructResolver(valueLiterals, functionName, reference);

      pMemberType.accept(resolver);
    }

    private void addStructSubexpression(Object pFieldValue, CFieldReference reference) {
      CType realType = reference.getExpressionType();
      ValueLiteral valueLiteral;

      if (realType instanceof CSimpleType) {
        valueLiteral = getValueLiteral(((CSimpleType) realType), pFieldValue);
      } else {
        Address valueAddress = Address.valueOf(pFieldValue);
        if (valueAddress.isUnknown()) {
          return;
        }
        valueLiteral =
            ExplicitValueLiteral.valueOf(valueAddress, assumptionToEdgeAllocator.getMachineModel());
      }

      if (valueLiteral.isUnknown()) {
        return;
      }

      SubExpressionValueLiteral subExpression =
          new SubExpressionValueLiteral(valueLiteral, reference);
      valueLiterals.addSubExpressionValueLiteral(subExpression);
    }
  }
}

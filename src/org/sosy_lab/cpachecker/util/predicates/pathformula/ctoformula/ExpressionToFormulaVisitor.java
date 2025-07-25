// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula;

import static com.google.common.base.Verify.verify;
import static org.sosy_lab.cpachecker.util.BuiltinFloatFunctions.getTypeOfBuiltinFloatFunction;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeUtils.getRealFieldOwner;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.FormulaEncodingOptions.INTERNAL_NONDET_FUNCTION_NAME;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.BaseSizeofVisitor;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;
import org.sosy_lab.cpachecker.util.BuiltinFloatFunctions;
import org.sosy_lab.cpachecker.util.BuiltinFunctions;
import org.sosy_lab.cpachecker.util.floatingpoint.FloatValue;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ErrorConditions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormulaManager;
import org.sosy_lab.java_smt.api.FloatingPointNumber;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.BitvectorType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public class ExpressionToFormulaVisitor
    extends DefaultCExpressionVisitor<Formula, UnrecognizedCodeException>
    implements CRightHandSideVisitor<Formula, UnrecognizedCodeException> {

  private final CtoFormulaConverter conv;
  private final CFAEdge edge;
  private final String function;
  private final Constraints constraints;
  protected final FormulaManagerView mgr;
  protected final SSAMapBuilder ssa;
  private final PointerTargetSetBuilder pts;
  private final ErrorConditions errorConditions;

  public ExpressionToFormulaVisitor(
      CtoFormulaConverter pCtoFormulaConverter,
      FormulaManagerView pFmgr,
      CFAEdge pEdge,
      String pFunction,
      SSAMapBuilder pSsa,
      PointerTargetSetBuilder pPts,
      Constraints pConstraints,
      ErrorConditions pErrorConditions) {

    conv = pCtoFormulaConverter;
    edge = pEdge;
    function = pFunction;
    ssa = pSsa;
    constraints = pConstraints;
    mgr = pFmgr;
    pts = pPts;
    errorConditions = pErrorConditions;
  }

  @Override
  protected Formula visitDefault(CExpression exp) throws UnrecognizedCodeException {
    return conv.makeVariableUnsafe(exp, function, ssa, false);
  }

  protected Formula toFormula(CExpression e) throws UnrecognizedCodeException {
    return e.accept(this);
  }

  public Formula processOperand(CExpression e, CType calculationType, CType returnType)
      throws UnrecognizedCodeException {
    e = conv.convertLiteralToFloatIfNecessary(e, calculationType);
    e = conv.makeCastFromArrayToPointerIfNecessary(e, returnType);
    final CType t = e.getExpressionType();
    Formula f = toFormula(e);
    return conv.makeCast(t, calculationType, f, constraints, edge);
  }

  private CType getPromotedTypeForArithmetic(CExpression exp) {
    CType t = exp.getExpressionType();
    t = t.getCanonicalType();
    t = CTypes.adjustFunctionOrArrayType(t);
    if (CTypes.isIntegerType(t)) {
      // Integer types smaller than int are promoted when an operation is performed on them.
      return conv.machineModel.applyIntegerPromotion(t);
    }
    return t;
  }

  @Override
  public Formula visit(final CBinaryExpression exp) throws UnrecognizedCodeException {
    /* FOR SHIFTS:
     * We would not need to cast the second operand, but we do casting,
     * because Mathsat assumes 2 bitvectors of same length.
     *
     * This could be incorrect in cases of negative shifts and
     * signed/unsigned conversion, example: 5U<<(-1).
     * Instead of "undefined value", we return a possible wrong value.
     *
     * ISO-C 6.5.7 Bitwise shift operators
     * If the value of the right operand is negative or is greater than or equal
     * to the width of the promoted left operand, the behavior is undefined.
     */

    final CType returnType = exp.getExpressionType();
    final CType calculationType = exp.getCalculationType();

    final Formula f1 = processOperand(exp.getOperand1(), calculationType, returnType);
    final Formula f2 = processOperand(exp.getOperand2(), calculationType, returnType);

    return handleBinaryExpression(exp, f1, f2);
  }

  public final Formula handleBinaryExpression(
      final CBinaryExpression exp, final Formula f1, final Formula f2)
      throws UnrecognizedCodeException {
    final BinaryOperator op = exp.getOperator();
    final CType returnType = exp.getExpressionType();
    final CType calculationType = exp.getCalculationType();

    // these operators expect numeric arguments
    final FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(returnType);

    final boolean signed;
    if (calculationType instanceof CSimpleType cSimpleType) {
      signed = conv.machineModel.isSigned(cSimpleType);
    } else {
      // For pointers: happens only for additions/subtractions (for which signedness is irrelevant)
      // and comparisons. GCC implements pointer comparisons as unsigned.
      // For other types like structs signedness should be irrelevant as well.
      signed = false;
    }

    // to INT or bigger
    CType promT1 = getPromotedTypeForArithmetic(exp.getOperand1());
    CType promT2 = getPromotedTypeForArithmetic(exp.getOperand2());

    final Formula ret;

    switch (op) {
      case PLUS -> {
        if (!(promT1 instanceof CPointerType)
            && !(promT2 instanceof CPointerType)) { // Just an addition e.g. 6 + 7
          ret = mgr.makePlus(f1, f2);
        } else if (!(promT2 instanceof CPointerType cPointerType)) {
          // operand1 is a pointer => we should multiply the second summand by the size of the
          // pointer target
          ret =
              mgr.makePlus(
                  f1, mgr.makeMultiply(f2, getSizeExpression(((CPointerType) promT1).getType())));
        } else if (!(promT1 instanceof CPointerType)) {
          // operand2 is a pointer => we should multiply the first summand by the size of the
          // pointer target
          ret = mgr.makePlus(f2, mgr.makeMultiply(f1, getSizeExpression(cPointerType.getType())));
        } else {
          throw new UnrecognizedCodeException("Can't add pointers", edge, exp);
        }
      }
      case MINUS -> {
        if (!(promT1 instanceof CPointerType)
            && !(promT2 instanceof CPointerType)) { // Just a subtraction e.g. 6 - 7
          ret = mgr.makeMinus(f1, f2);
        } else if (!(promT2 instanceof CPointerType)) {
          // operand1 is a pointer => we should multiply the subtrahend by the size of the pointer
          // target
          ret =
              mgr.makeMinus(
                  f1, mgr.makeMultiply(f2, getSizeExpression(((CPointerType) promT1).getType())));
        } else if (promT1 instanceof CPointerType cPointerType) {
          // Pointer subtraction => (operand1 - operand2) / sizeof (*operand1)
          if (promT1.equals(promT2)) {
            ret =
                mgr.makeDivide(
                    mgr.makeMinus(f1, f2), getSizeExpression(cPointerType.getType()), true);
          } else {
            throw new UnrecognizedCodeException(
                "Can't subtract pointers of different types", edge, exp);
          }
        } else {
          throw new UnrecognizedCodeException(
              "Can't subtract a pointer from a non-pointer", edge, exp);
        }
      }
      case MULTIPLY -> ret = mgr.makeMultiply(f1, f2);
      case DIVIDE -> ret = mgr.makeDivide(f1, f2, signed);
      case MODULO -> {
        // Modulo in C is remainder in SMTLIB2
        ret = mgr.makeRemainder(f1, f2, signed);

        addModuloConstraints(exp, f1, f2, signed, ret);
      }
      case BINARY_AND -> ret = mgr.makeAnd(f1, f2);
      case BINARY_OR -> ret = mgr.makeOr(f1, f2);
      case BINARY_XOR -> ret = mgr.makeXor(f1, f2);
      case SHIFT_LEFT ->
          // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
          ret = mgr.makeShiftLeft(f1, f2);
      case SHIFT_RIGHT ->
          // NOTE: The type of the result is that of the promoted left operand. (6.5.7 3)
          ret = mgr.makeShiftRight(f1, f2, signed);
      case GREATER_THAN, GREATER_EQUAL, LESS_THAN, LESS_EQUAL, EQUALS, NOT_EQUALS -> {
        BooleanFormula result =
            switch (op) {
              case GREATER_THAN -> mgr.makeGreaterThan(f1, f2, signed);
              case GREATER_EQUAL -> mgr.makeGreaterOrEqual(f1, f2, signed);
              case LESS_THAN -> mgr.makeLessThan(f1, f2, signed);
              case LESS_EQUAL -> mgr.makeLessOrEqual(f1, f2, signed);
              case EQUALS -> handleEquals(exp, f1, f2);
              case NOT_EQUALS -> conv.bfmgr.not(mgr.makeEqual(f1, f2));
              default -> throw new AssertionError();
            };
        // Here we directly use the returnFormulaType instead of the calculcationType
        // to avoid a useless cast.
        // However, this means that we may not call makeCast() below
        // because it expects the input in the calculationType.
        // So we return here directly.
        return conv.ifTrueThenOneElseZero(returnFormulaType, result);
      }
      default -> throw new UnrecognizedCodeException("Unknown binary operator", edge, exp);
    }

    // The CalculationType could be different from returnType, so we cast the result.
    // If the types are equal, the cast returns the Formula unchanged.
    final Formula castedResult = conv.makeCast(calculationType, returnType, ret, constraints, edge);
    assert returnFormulaType.equals(mgr.getFormulaType(castedResult))
        : "Returntype and Formulatype do not match in visit(CBinaryExpression): " + exp;

    return castedResult;
  }

  private BooleanFormula handleEquals(CBinaryExpression exp, Formula f1, Formula f2)
      throws UnrecognizedCodeException {
    assert exp.getOperator() == BinaryOperator.EQUALS;
    CExpression e1 = exp.getOperand1();
    CExpression e2 = exp.getOperand2();
    if (e2.equals(CIntegerLiteralExpression.ZERO)
        && e1 instanceof CBinaryExpression or
        && ((CBinaryExpression) e1).getOperator() == BinaryOperator.BINARY_OR) {
      // This is code like "(a | b) == 0".
      // According to LDV, GCC sometimes produces this during weaving,
      // but for non-bitprecise analysis it can be handled in a better way as (a == 0) || (b == 0).
      // TODO Maybe refactor AutomatonASTComparator into something generic
      // and use this to match such cases.

      final Formula zero = f2;
      final Formula a =
          processOperand(or.getOperand1(), exp.getCalculationType(), exp.getExpressionType());
      final Formula b =
          processOperand(or.getOperand2(), exp.getCalculationType(), exp.getExpressionType());

      return conv.bfmgr.and(mgr.makeEqual(a, zero), mgr.makeEqual(b, zero));
    }
    return mgr.makeEqual(f1, f2);
  }

  /**
   * Some solvers (Mathsat, Princess) do not support MODULO/REMAINDER and replace it with an UF.
   * Thus, we limit the result of the UF with additional constraints.
   */
  private void addModuloConstraints(
      final CBinaryExpression exp,
      final Formula f1,
      final Formula f2,
      final boolean signed,
      final Formula ret) {
    BooleanFormulaManagerView bfmgr = mgr.getBooleanFormulaManager();

    if (exp.getOperand2() instanceof CIntegerLiteralExpression cIntegerLiteralExpression) {
      // We use a BigInteger because it can always be made positive, this is not true for type long!
      BigInteger modulo = cIntegerLiteralExpression.getValue();
      if (!modulo.equals(BigInteger.ZERO)) {
        // modular congruence expects a positive modulo. If our divisor b in a%b is negative, we
        // actually want to generate a modular congruence condition mod (-b):
        modulo = modulo.abs();
        BooleanFormula modularCongruence = mgr.makeModularCongruence(ret, f1, modulo, signed);
        if (!bfmgr.isTrue(modularCongruence)) {
          constraints.addConstraint(modularCongruence);
        }
      }
    }

    FormulaType<Formula> numberType = mgr.getFormulaType(f1);
    Formula zero = mgr.makeNumber(numberType, 0L);

    // Sign of the remainder is set by the sign of the
    // numerator, and it is bounded by the numerator.
    BooleanFormula signAndNumBound =
        bfmgr.ifThenElse(
            mgr.makeGreaterOrEqual(f1, zero, signed),
            mgr.makeRangeConstraint(ret, zero, f1, signed), // ret in [zero, f1] (both positive)
            mgr.makeRangeConstraint(ret, f1, zero, signed) // ret in [f1, zero] (both negative)
            );

    BooleanFormula denomBound =
        bfmgr.ifThenElse(
            mgr.makeGreaterOrEqual(f2, zero, signed),

            // Denominator is positive => remainder is strictly less than denominator.
            mgr.makeLessThan(ret, f2, signed),

            // Denominator is negative => remainder is strictly more.
            mgr.makeLessThan(f2, ret, signed));

    BooleanFormula newConstraints =
        bfmgr.ifThenElse(
            mgr.makeEqual(f2, zero),
            bfmgr.makeTrue(), // if divisor is zero, make no constraint
            bfmgr.and(signAndNumBound, denomBound));

    constraints.addConstraint(newConstraints);
  }

  @Override
  public Formula visit(CCastExpression cexp) throws UnrecognizedCodeException {
    CExpression op = cexp.getOperand();
    op = conv.makeCastFromArrayToPointerIfNecessary(op, cexp.getExpressionType());

    Formula operand = toFormula(op);

    CType after = cexp.getExpressionType();
    CType before = op.getExpressionType();
    return conv.makeCast(before, after, operand, constraints, edge);
  }

  @Override
  public Formula visit(CIdExpression idExp) throws UnrecognizedCodeException {

    if (idExp.getDeclaration() instanceof CEnumerator enumerator) {
      CType t = idExp.getExpressionType();
      return mgr.makeNumber(conv.getFormulaTypeFromCType(t), enumerator.getValue());
    }

    return conv.makeVariable(
        idExp.getDeclaration().getQualifiedName(), idExp.getExpressionType(), ssa);
  }

  @Override
  public Formula visit(CFieldReference fExp) throws UnrecognizedCodeException {
    if (conv.options.handleFieldAccess()) {
      CExpression fieldOwner = getRealFieldOwner(fExp);
      Formula f = toFormula(fieldOwner);
      return conv.accessField(fExp, f);
    }

    CExpression fieldRef = fExp.getFieldOwner();
    if (fieldRef instanceof CIdExpression cIdExpression) {
      CSimpleDeclaration decl = cIdExpression.getDeclaration();
      if (decl instanceof CDeclaration cDeclaration && cDeclaration.isGlobal()) {
        // this is the reference to a global field variable

        // we can omit the warning (no pointers involved),
        // and we don't need to scope the variable reference
        return conv.makeVariable(
            CtoFormulaConverter.exprToVarNameUnscoped(fExp), fExp.getExpressionType(), ssa);
      }
    }

    return super.visit(fExp);
  }

  @Override
  public Formula visit(CCharLiteralExpression cExp) throws UnrecognizedCodeException {
    // we just take the byte value
    FormulaType<?> t = conv.getFormulaTypeFromCType(cExp.getExpressionType());
    return mgr.makeNumber(t, cExp.getCharacter());
  }

  @Override
  public Formula visit(CIntegerLiteralExpression iExp) throws UnrecognizedCodeException {
    FormulaType<?> t = conv.getFormulaTypeFromCType(iExp.getExpressionType());
    return mgr.makeNumber(t, iExp.getValue());
  }

  @Override
  public Formula visit(CImaginaryLiteralExpression exp) throws UnrecognizedCodeException {
    return toFormula(exp.getValue());
  }

  @Override
  public Formula visit(CFloatLiteralExpression fExp) throws UnrecognizedCodeException {
    FloatingPointFormulaManager fmgr = mgr.getFloatingPointFormulaManager();
    FloatValue value = fExp.getValue();
    FloatingPointNumber converted = value.toFloatingPointNumber();

    return fmgr.makeNumber(
        converted.getExponent(),
        converted.getMantissa(),
        converted.getSign(),
        FormulaType.getFloatingPointType(value.getFormat().expBits(), value.getFormat().sigBits()));
  }

  @Override
  public Formula visit(CStringLiteralExpression lexp) throws UnrecognizedCodeException {
    // we create a string constant representing the given
    // string literal
    return conv.makeStringLiteral(lexp.getContentWithNullTerminator(), constraints);
  }

  @Override
  public Formula visit(CUnaryExpression exp) throws UnrecognizedCodeException {
    CExpression operand = exp.getOperand();
    UnaryOperator op = exp.getOperator();
    return switch (op) {
      case MINUS, TILDE -> {
        // Handle Integer Promotion
        CType t = operand.getExpressionType();
        CType promoted = t.getCanonicalType();
        if (CTypes.isIntegerType(promoted)) {
          // Integer types smaller than int are promoted when an operation is performed on them.
          promoted = conv.machineModel.applyIntegerPromotion(promoted);
        }
        Formula operandFormula = toFormula(operand);
        operandFormula = conv.makeCast(t, promoted, operandFormula, constraints, edge);
        Formula ret;
        if (op == UnaryOperator.MINUS) {
          ret = mgr.makeNegate(operandFormula);
        } else {
          assert op == UnaryOperator.TILDE : "This case should be impossible because of switch";
          ret = mgr.makeNot(operandFormula);
        }

        CType returnType = exp.getExpressionType();
        FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(returnType);
        if (!returnFormulaType.equals(mgr.getFormulaType(ret))) {
          ret = conv.makeCast(promoted, returnType, ret, constraints, edge);
        }
        assert returnFormulaType.equals(mgr.getFormulaType(ret))
            : "Returntype "
                + returnFormulaType
                + " and Formulatype "
                + mgr.getFormulaType(ret)
                + " do not match in visit(CUnaryExpression) for "
                + exp;
        yield ret;
      }
      case AMPER -> visitDefault(exp);

      case SIZEOF -> {
        CType lCType = exp.getOperand().getExpressionType();
        yield getSizeExpression(lCType);
      }
      case ALIGNOF -> handleAlignOf(exp, exp.getOperand().getExpressionType());
    };
  }

  @Override
  public Formula visit(CTypeIdExpression tIdExp) throws UnrecognizedCodeException {
    CType lCType = tIdExp.getType();

    return switch (tIdExp.getOperator()) {
      case SIZEOF -> getSizeExpression(lCType);
      case ALIGNOF -> handleAlignOf(tIdExp, lCType);
      default -> visitDefault(tIdExp);
    };
  }

  /**
   * This is a SizeofVisitor that always returns 0 for types that do not have a statically known
   * size.
   */
  private static class StaticSizeofVisitor extends BaseSizeofVisitor<NoException> {

    /**
     * Compute the size of the given composite type excluding all members that do not have a
     * statically known size.
     */
    BigInteger computeStaticSizeof(CCompositeType pType) {
      // Note that this.visit(pType) would return 0 because pType has !hasKnownConstantSize(),
      // so we need to call super.visit() here such that only the struct members get handled by
      // this.visit()
      return super.visit(pType);
    }

    private StaticSizeofVisitor(MachineModel pModel) {
      super(pModel);
    }

    @Override
    public BigInteger visit(CArrayType pArrayType) {
      if (pArrayType.hasKnownConstantSize()) {
        return super.visit(pArrayType);
      }
      return BigInteger.ZERO;
    }

    @Override
    public BigInteger visit(CCompositeType pCompositeType) {
      if (pCompositeType.hasKnownConstantSize()) {
        return super.visit(pCompositeType);
      }
      return BigInteger.ZERO;
    }
  }

  public Formula getSizeExpression(CType type) throws UnrecognizedCodeException {
    type = type.getCanonicalType();
    if (type.hasKnownConstantSize()) {
      return mgr.makeNumber(
          conv.typeHandler.getPointerType(), conv.typeHandler.getExactSizeof(type));

    } else if (type instanceof CArrayType arrayType) {
      Formula elementSize = getSizeExpression(arrayType.getType());

      Formula elementCount = arrayType.getLength().accept(this);
      elementCount =
          conv.makeCast(
              arrayType.getLength().getExpressionType(),
              CPointerType.POINTER_TO_VOID,
              elementCount,
              constraints,
              edge);

      return mgr.makeMultiply(elementSize, elementCount);

    } else if (type instanceof CCompositeType compositeType) {
      Deque<Formula> memberSizes = new ArrayDeque<>();

      // It is not enough to just compute the sum/maximum over all member sizes because of padding.
      // But we also do not want to reimplement the complex padding and bitfield computations here.
      // So we help us with a trick: The next line computes the size of the whole struct/union as if
      // the variable-length parts would have length 0. But this includes all other fields and all
      // paddings (even those for variable-length arrays, because padding is always known).
      // Then we just have to create expressions for the sizes of the variable-length parts and
      // compute the final sum/maximum.
      BigInteger staticallyKnownSize =
          new StaticSizeofVisitor(conv.machineModel).computeStaticSizeof(compositeType);

      for (CCompositeTypeMemberDeclaration member : compositeType.getMembers()) {
        if (!member.getType().hasKnownConstantSize() && !member.isFlexibleArrayMember()) {
          // Size is not statically known (exceptions are flexible array members with size 0).
          memberSizes.add(getSizeExpression(member.getType()));
        }
        // else the size is already part of staticallyKnownSize
      }

      verify(!memberSizes.isEmpty());
      memberSizes.add(mgr.makeNumber(conv.typeHandler.getPointerType(), staticallyKnownSize));

      // Now compute sum/maximum of memberSizes. Especially for the maximum (with if-then-else)
      // a balanced tree of nested maximums seems to be likely better for the solver.
      return switch (compositeType.getKind()) {
        case STRUCT -> balancedDestructiveReduce(memberSizes, mgr::makePlus);
        case UNION ->
            balancedDestructiveReduce(
                memberSizes,
                (a, b) -> conv.bfmgr.ifThenElse(mgr.makeGreaterOrEqual(a, b, false), a, b));
        default -> throw new AssertionError();
      };

    } else {
      throw new AssertionError("unexpected type without known constant size: " + type);
    }
  }

  /**
   * Reduce the elements of a given deque in a balanced manner, e.g. for [1,2,3,4] and "+" it will
   * produce (1+2)+(3+4). The deque will be drained.
   */
  private static <T> T balancedDestructiveReduce(
      Deque<T> deque, BiFunction<? super T, ? super T, ? extends T> reducer) {
    while (deque.size() > 1) {
      T a = deque.pollFirst();
      T b = deque.pollFirst();
      deque.addLast(reducer.apply(a, b));
    }
    return deque.getFirst();
  }

  private Formula handleAlignOf(CExpression pExp, CType pCType) {
    return mgr.makeNumber(
        conv.getFormulaTypeFromCType(pExp.getExpressionType()),
        conv.machineModel.getAlignof(pCType));
  }

  @Override
  public Formula visit(CFunctionCallExpression e) throws UnrecognizedCodeException {
    final CExpression functionNameExpression = e.getFunctionNameExpression();
    final CType returnType = e.getExpressionType();
    final List<CExpression> parameters = e.getParameterExpressions();

    // First let's handle special cases such as assumes, allocations, nondets, external models, etc.
    final String functionName;
    if (functionNameExpression instanceof CIdExpression cIdExpression) {
      functionName = cIdExpression.getName();

      if (conv.options.isNondetFunction(functionName)
          || conv.options.isMemoryAllocationFunction(functionName)
          || conv.options.isMemoryAllocationFunctionWithZeroing(functionName)) {
        // Function call like "random()".
        // Also "malloc()" etc. just return a random value, so handle them similarly.
        // Ignore parameters and just create a fresh variable for it.
        return conv.makeNondet(functionName, returnType, ssa, constraints);

      } else if (BuiltinFunctions.matchesFscanf(functionName)) {

        ValidatedFScanFParameter receivingParameter = validateFscanfParameters(parameters, e);

        if (receivingParameter.receiver() instanceof CUnaryExpression unaryParameter) {
          UnaryOperator operator = unaryParameter.getOperator();
          CExpression operand = unaryParameter.getOperand();
          if (operator.equals(UnaryOperator.AMPER)
              && operand instanceof CIdExpression idExpression) {
            // For simplicity, we start with the case where only parameters of the form "&id" occur
            CType variableType = idExpression.getExpressionType();

            if (!isCompatibleWithScanfFormatString(receivingParameter.format(), variableType)) {
              throw new UnsupportedCodeException(
                  "fscanf with receiving type <-> format specifier mismatch is not supported.",
                  edge,
                  e);
            }

            CFunctionDeclaration nondetFun =
                new CFunctionDeclaration(
                    edge.getFileLocation(),
                    CFunctionType.functionTypeWithReturnType(variableType),
                    INTERNAL_NONDET_FUNCTION_NAME,
                    ImmutableList.of(),
                    ImmutableSet.of());
            CIdExpression nondetFunctionName =
                new CIdExpression(
                    edge.getFileLocation(), variableType, nondetFun.getName(), nondetFun);

            CFunctionCallExpression rhs =
                new CFunctionCallExpression(
                    edge.getFileLocation(),
                    variableType,
                    nondetFunctionName,
                    ImmutableList.of(),
                    nondetFun);
            try {
              BooleanFormula assignment =
                  conv.makeAssignment(
                      idExpression,
                      idExpression,
                      rhs,
                      edge,
                      function,
                      ssa,
                      pts,
                      constraints,
                      errorConditions);
              constraints.addConstraint(assignment);
            } catch (InterruptedException interruptedException) {
              CtoFormulaConverter.propagateInterruptedException(interruptedException);
            }
          } else {
            throw new UnsupportedCodeException(
                "Currently, only fscanf with a single parameter of the form &id is supported.",
                edge,
                e);
          }
        } else {
          throw new UnsupportedCodeException(
              "Currently, only fscanf with a single parameter of the form &id is supported.",
              edge,
              e);
        }

        // fscanf(FILE *stream, const char *format, ...) returns the number of assigned items
        // or EOF if no item has been assigned before the end of the file occurred.
        // We can over-approximate the value with nondet.
        return conv.makeNondet(functionName, returnType, ssa, constraints);

      } else if (conv.options.isExternModelFunction(functionName)) {
        ExternModelLoader loader = new ExternModelLoader(conv, conv.bfmgr, conv.fmgr);
        BooleanFormula result = loader.handleExternModelFunction(parameters, ssa);
        FormulaType<?> returnFormulaType = conv.getFormulaTypeFromCType(e.getExpressionType());
        return conv.ifTrueThenOneElseZero(returnFormulaType, result);

      } else if (BuiltinFunctions.isSetjmpFunction(functionName)) {
        // setjmp always returns 0 on the "regular" return, and we don't support longjmp
        return mgr.makeNumber(conv.getFormulaTypeFromCType(returnType), 0);

      } else if (BuiltinFunctions.isPopcountFunction(functionName)) {
        return handlePopCount(functionName, returnType, parameters, e);
      } else if (BuiltinFloatFunctions.matchesInfinity(functionName)) {

        if (parameters.isEmpty()) {
          CType resultType = getTypeOfBuiltinFloatFunction(functionName);

          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(resultType);
          if (formulaType.isFloatingPointType()) {
            return mgr.getFloatingPointFormulaManager()
                .makePlusInfinity((FormulaType.FloatingPointType) formulaType);
          }
        }

      } else if (BuiltinFloatFunctions.matchesHugeVal(functionName)) {

        if (parameters.isEmpty()) {
          CType resultType = getTypeOfBuiltinFloatFunction(functionName);

          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(resultType);
          if (formulaType.isFloatingPointType()) {
            return mgr.getFloatingPointFormulaManager()
                .makePlusInfinity((FormulaType.FloatingPointType) formulaType);
          }
        }

      } else if (BuiltinFloatFunctions.matchesNaN(functionName)) {

        if (parameters.size() == 1) {
          CType resultType = getTypeOfBuiltinFloatFunction(functionName);

          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(resultType);
          if (formulaType.isFloatingPointType()) {
            return mgr.getFloatingPointFormulaManager()
                .makeNaN((FormulaType.FloatingPointType) formulaType);
          }
        }

      } else if (BuiltinFloatFunctions.matchesAbsolute(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula zero =
                fpfmgr.makeNumber(0.0, (FormulaType.FloatingPointType) formulaType);
            FloatingPointFormula nan = fpfmgr.makeNaN((FormulaType.FloatingPointType) formulaType);

            BooleanFormula isNegative =
                mgr.makeOr(
                    mgr.makeLessThan(param, zero, true),
                    mgr.makeAnd(
                        fpfmgr.isZero(param), conv.bfmgr.not(fpfmgr.assignment(zero, param))));
            BooleanFormula isNan = fpfmgr.isNaN(param);

            return conv.bfmgr.ifThenElse(
                isNegative, mgr.makeNegate(param), conv.bfmgr.ifThenElse(isNan, nan, param));
          }
        }

      } else if (BuiltinFloatFunctions.matchesFinite(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            return conv.bfmgr.ifThenElse(
                conv.bfmgr.or(fpfmgr.isInfinity(param), fpfmgr.isNaN(param)),
                mgr.makeNumber(resultType, 0),
                mgr.makeNumber(resultType, 1));
          }
        }

      } else if (BuiltinFloatFunctions.matchesIsNaN(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            return conv.bfmgr.ifThenElse(
                fpfmgr.isNaN(param), mgr.makeNumber(resultType, 1), mgr.makeNumber(resultType, 0));
          }
        }

      } else if (BuiltinFloatFunctions.matchesIsInfinity(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula fp_zero =
                fpfmgr.makeNumber(0, (FormulaType.FloatingPointType) formulaType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            Formula zero = mgr.makeNumber(resultType, 0);
            Formula one = mgr.makeNumber(resultType, 1);
            Formula minus_one = mgr.makeNumber(resultType, -1);

            return conv.bfmgr.ifThenElse(
                fpfmgr.isInfinity(param),
                conv.bfmgr.ifThenElse(fpfmgr.lessThan(param, fp_zero), minus_one, one),
                zero);
          }
        }

      } else if (BuiltinFloatFunctions.matchesIsInfinitySign(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula fp_zero =
                fpfmgr.makeNumber(0, (FormulaType.FloatingPointType) formulaType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            Formula zero = mgr.makeNumber(resultType, 0);
            Formula one = mgr.makeNumber(resultType, 1);
            Formula minus_one = mgr.makeNumber(resultType, -1);

            return conv.bfmgr.ifThenElse(
                fpfmgr.isInfinity(param),
                conv.bfmgr.ifThenElse(fpfmgr.lessThan(param, fp_zero), minus_one, one),
                zero);
          }
        }

      } else if (BuiltinFloatFunctions.matchesFloatClassify(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            Formula zero = mgr.makeNumber(resultType, 0);
            Formula one = mgr.makeNumber(resultType, 1);
            Formula two = mgr.makeNumber(resultType, 2);
            Formula three = mgr.makeNumber(resultType, 3);
            Formula four = mgr.makeNumber(resultType, 4);

            return conv.bfmgr.ifThenElse(
                fpfmgr.isNaN(param),
                zero,
                conv.bfmgr.ifThenElse(
                    fpfmgr.isInfinity(param),
                    one,
                    conv.bfmgr.ifThenElse(
                        fpfmgr.isZero(param),
                        two,
                        conv.bfmgr.ifThenElse(fpfmgr.isSubnormal(param), three, four))));
          }
        }

      } else if (BuiltinFloatFunctions.matchesCopysign(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param0 =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula param1 =
                (FloatingPointFormula) processOperand(parameters.get(1), paramType, paramType);

            FloatingPointFormula zero =
                fpfmgr.makeNumber(0.0, (FormulaType.FloatingPointType) formulaType);
            FloatingPointFormula anything =
                (FloatingPointFormula)
                    conv.makeNondet(functionName + "_NondetAnything", paramType, ssa, constraints);

            BooleanFormula isFirstNegative =
                mgr.makeOr(
                    mgr.makeLessThan(param0, zero, true),
                    mgr.makeAnd(
                        fpfmgr.isZero(param0),
                        mgr.makeOr(
                            conv.bfmgr.not(fpfmgr.assignment(param0, zero)),
                            mgr.makeAnd(fpfmgr.isNaN(param0), fpfmgr.assignment(anything, zero)))));
            BooleanFormula isSecondNegative =
                mgr.makeOr(
                    mgr.makeLessThan(param1, zero, true),
                    mgr.makeAnd(
                        fpfmgr.isZero(param1),
                        mgr.makeOr(
                            conv.bfmgr.not(fpfmgr.assignment(param1, zero)),
                            mgr.makeAnd(fpfmgr.isNaN(param1), fpfmgr.assignment(anything, zero)))));
            BooleanFormula haveSameSign = conv.bfmgr.equivalence(isFirstNegative, isSecondNegative);

            return conv.bfmgr.ifThenElse(haveSameSign, param0, fpfmgr.negate(param0));
          }
        }

      } else if (BuiltinFloatFunctions.matchesFmod(functionName)
          || BuiltinFloatFunctions.matchesFremainder(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param0 =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula param1 =
                (FloatingPointFormula) processOperand(parameters.get(1), paramType, paramType);

            BooleanFormula isFirstInfinity = fpfmgr.isInfinity(param0);
            BooleanFormula isSecondInfinity = fpfmgr.isInfinity(param1);
            BooleanFormula isFirstNaN = fpfmgr.isNaN(param0);
            BooleanFormula isSecondNaN = fpfmgr.isNaN(param1);
            BooleanFormula isFirstZero = fpfmgr.isZero(param0);
            BooleanFormula isSecondZero = fpfmgr.isZero(param1);

            BooleanFormula domainErr =
                conv.bfmgr.or(isFirstInfinity, isFirstNaN, isSecondNaN, isSecondZero);
            BooleanFormula noOpNeeded = conv.bfmgr.or(isSecondInfinity, isFirstZero);

            // Description of fmod from Linux manpage:
            // The fmod() function computes the floating-point remainder of dividing x by y.
            // The return value is x - n * y, where n is the quotient of x / y.
            // N is rounded toward zero to an integer for function fmod and toward the nearest
            // integer (to the even one in case of a tie) for function remainderf.

            FloatingPointFormula n;
            // x / y -> rounded towards 0
            if (BuiltinFloatFunctions.matchesFmod(functionName)) {
              n = fpfmgr.divide(param0, param1);
              n = fpfmgr.round(n, FloatingPointRoundingMode.TOWARD_ZERO);
            } else {
              n = fpfmgr.divide(param0, param1);
              n = fpfmgr.round(n, FloatingPointRoundingMode.NEAREST_TIES_TO_EVEN);
            }

            // x - (n * y)
            FloatingPointFormula mainCalculation =
                fpfmgr.subtract(param0, fpfmgr.multiply(n, param1));

            return conv.bfmgr.ifThenElse(
                domainErr,
                fpfmgr.makeNaN((FloatingPointType) formulaType),
                conv.bfmgr.ifThenElse(noOpNeeded, param0, mainCalculation));
          }
        }
      } else if (BuiltinFloatFunctions.matchesFmin(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param0 =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula param1 =
                (FloatingPointFormula) processOperand(parameters.get(1), paramType, paramType);

            BooleanFormula isFirstNaN = fpfmgr.isNaN(param0);
            BooleanFormula isSecondNaN = fpfmgr.isNaN(param1);
            BooleanFormula firstLessSecond = fpfmgr.lessThan(param0, param1);

            return conv.bfmgr.ifThenElse(
                isFirstNaN,
                param1,
                conv.bfmgr.ifThenElse(conv.bfmgr.or(isSecondNaN, firstLessSecond), param0, param1));
          }
        }
      } else if (BuiltinFloatFunctions.matchesFmax(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param0 =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula param1 =
                (FloatingPointFormula) processOperand(parameters.get(1), paramType, paramType);

            BooleanFormula isFirstNaN = fpfmgr.isNaN(param0);
            BooleanFormula isSecondNaN = fpfmgr.isNaN(param1);
            BooleanFormula firstGreaterSecond = fpfmgr.greaterThan(param0, param1);

            return conv.bfmgr.ifThenElse(
                isFirstNaN,
                param1,
                conv.bfmgr.ifThenElse(
                    conv.bfmgr.or(isSecondNaN, firstGreaterSecond), param0, param1));
          }
        }
      } else if (BuiltinFloatFunctions.matchesFdim(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param0 =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula param1 =
                (FloatingPointFormula) processOperand(parameters.get(1), paramType, paramType);
            FloatingPointFormula zero =
                fpfmgr.makeNumber(0, (FormulaType.FloatingPointType) formulaType);

            BooleanFormula isFirstNaN = fpfmgr.isNaN(param0);
            BooleanFormula isSecondNaN = fpfmgr.isNaN(param1);

            FloatingPointFormula diff = fpfmgr.subtract(param0, param1);

            return conv.bfmgr.ifThenElse(
                isFirstNaN,
                param0,
                conv.bfmgr.ifThenElse(
                    isSecondNaN,
                    param1,
                    conv.bfmgr.ifThenElse(fpfmgr.greaterThan(diff, zero), diff, zero)));
          }
        }
      } else if (BuiltinFloatFunctions.matchesIsless(functionName)) {

        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        Formula result = inequalityBuiltin(functionName, parameters, fpfmgr::lessThan, fpfmgr);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesIslessequal(functionName)) {

        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        Formula result = inequalityBuiltin(functionName, parameters, fpfmgr::lessOrEquals, fpfmgr);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesIsgreater(functionName)) {

        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        Formula result = inequalityBuiltin(functionName, parameters, fpfmgr::greaterThan, fpfmgr);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesIsgreaterequal(functionName)) {

        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        Formula result =
            inequalityBuiltin(functionName, parameters, fpfmgr::greaterOrEquals, fpfmgr);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesIslessgreater(functionName)) {

        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        Formula result =
            inequalityBuiltin(
                functionName,
                parameters,
                (e1, e2) -> conv.bfmgr.not(fpfmgr.equalWithFPSemantics(e1, e2)),
                fpfmgr);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesIsunordered(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param0 =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula param1 =
                (FloatingPointFormula) processOperand(parameters.get(1), paramType, paramType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            Formula zero = mgr.makeNumber(resultType, 0);
            Formula one = mgr.makeNumber(resultType, 1);

            return conv.bfmgr.ifThenElse(
                fpfmgr.isNaN(param0), one, conv.bfmgr.ifThenElse(fpfmgr.isNaN(param1), one, zero));
          }
        }
      } else if (BuiltinFloatFunctions.matchesSignbit(functionName)) {

        if (parameters.size() == 1) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);
            FloatingPointFormula fp_zero = fpfmgr.makeNumber(0, (FloatingPointType) formulaType);

            FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
            Formula zero = mgr.makeNumber(resultType, 0);
            Formula not_zero =
                conv.makeNondet(functionName + "_NonZero", CNumericTypes.INT, ssa, constraints);
            // Since the SMT-Solvers we use do not differentiate between NaN and -NaN we prefer to
            // label, in doubt, a case involving a NaN/-NaN to be an alarm
            Formula anything =
                conv.makeNondet(
                    functionName + "_NondetAnything", CNumericTypes.INT, ssa, constraints);
            constraints.addConstraint(mgr.makeNot(mgr.makeEqual(not_zero, zero)));

            return conv.bfmgr.ifThenElse(
                fpfmgr.isZero(param),
                conv.bfmgr.ifThenElse(fpfmgr.assignment(param, fp_zero), zero, not_zero),
                conv.bfmgr.ifThenElse(
                    fpfmgr.isNaN(param),
                    anything,
                    conv.bfmgr.ifThenElse(fpfmgr.lessThan(param, fp_zero), not_zero, zero)));
          }
        }
      } else if (BuiltinFloatFunctions.matchesModf(functionName)) {

        if (parameters.size() == 2) {
          CType paramType = getTypeOfBuiltinFloatFunction(functionName);
          FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
          if (formulaType.isFloatingPointType()) {
            FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
            FloatingPointFormula param =
                (FloatingPointFormula) processOperand(parameters.get(0), paramType, paramType);

            FloatingPointFormula zero = fpfmgr.makeNumber(0, (FloatingPointType) formulaType);
            FloatingPointFormula nan = fpfmgr.makeNaN((FloatingPointType) formulaType);
            FloatingPointFormula rounded =
                fpfmgr.round(param, FloatingPointRoundingMode.TOWARD_ZERO);

            return conv.bfmgr.ifThenElse(
                fpfmgr.isNaN(param),
                nan,
                conv.bfmgr.ifThenElse(
                    fpfmgr.isInfinity(param), zero, fpfmgr.subtract(param, rounded)));
          }
        }
      } else if (BuiltinFloatFunctions.matchesCeil(functionName)) {

        Formula result =
            roundingBuiltin(functionName, parameters, FloatingPointRoundingMode.TOWARD_POSITIVE);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesFloor(functionName)) {

        Formula result =
            roundingBuiltin(functionName, parameters, FloatingPointRoundingMode.TOWARD_NEGATIVE);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesTrunc(functionName)) {

        Formula result =
            roundingBuiltin(functionName, parameters, FloatingPointRoundingMode.TOWARD_ZERO);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesRound(functionName)) {

        Formula result = roundNearestTiesAway(parameters, functionName, false, false);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesLround(functionName)) {

        Formula result = roundNearestTiesAway(parameters, functionName, true, false);

        if (result != null) {
          return result;
        }
      } else if (BuiltinFloatFunctions.matchesLlround(functionName)) {

        Formula result = roundNearestTiesAway(parameters, functionName, true, true);

        if (result != null) {
          return result;
        }
      } else if (functionName.equals("__CPACHECKER_atexit_next")) {
        // __CPACHECKER_atexit_next is not a constant function, but returns a different function
        // pointer from the atexit stack every time it is being called. We model this by returning a
        // fresh variable that may point to any function in the program. The function pointer CPA,
        // which will be run in parallel, tracks the actual target of the pointer and makes sure
        // that the right function is always called.
        return conv.makeNondet(functionName, returnType, ssa, constraints);

      } else if (!CtoFormulaConverter.PURE_EXTERNAL_FUNCTIONS.contains(functionName)) {
        if (parameters.isEmpty()) {
          // function of arity 0
          conv.logger.logOnce(
              Level.INFO, "Assuming external function", functionName, "to be a constant function.");
        } else {
          conv.logger.logOnce(
              Level.INFO, "Assuming external function", functionName, "to be a pure function.");
        }
      }

      final String isUnsupported = conv.isUnsupportedFunction(functionName);
      if (isUnsupported != null) {
        throw new UnsupportedCodeException(isUnsupported, edge, e);
      }

    } else {
      conv.logfOnce(
          Level.WARNING,
          edge,
          "Ignoring function call through function pointer %s",
          functionNameExpression);
      String escapedName = CtoFormulaConverter.exprToVarName(functionNameExpression, function);
      functionName = ("<func>{" + escapedName + "}").intern();
    }

    // Now let's handle "normal" functions assumed to be pure
    if (parameters.isEmpty()) {
      // This is a function of arity 0 and we assume its constant.
      return conv.makeConstant(functionName, returnType);

    } else {
      final CFunctionDeclaration functionDeclaration = e.getDeclaration();
      if (functionDeclaration == null) {
        if (functionNameExpression instanceof CIdExpression) {
          // This happens only if there are undeclared functions.
          conv.logger.logfOnce(
              Level.WARNING,
              "Cannot get declaration of function %s, ignoring calls to it.",
              functionNameExpression);
        }
        return conv.makeNondet(functionName, returnType, ssa, constraints);
      }

      if (functionDeclaration.getType().takesVarArgs()) {
        // Create a fresh variable instead of an UF for varargs functions.
        // This is sound but slightly more imprecise (we loose the UF axioms).
        return conv.makeNondet(functionName, returnType, ssa, constraints);
      }

      final List<CType> formalParameterTypes = functionDeclaration.getType().getParameters();
      if (formalParameterTypes.size() != parameters.size()) {
        throw new UnrecognizedCodeException(
            "Function "
                + functionDeclaration
                + " received "
                + parameters.size()
                + " parameters"
                + " instead of the expected "
                + formalParameterTypes.size(),
            edge,
            e);
      }

      final List<Formula> arguments = new ArrayList<>(parameters.size());
      final Iterator<CType> formalParameterTypesIt = formalParameterTypes.iterator();
      final Iterator<CExpression> parametersIt = parameters.iterator();
      while (formalParameterTypesIt.hasNext() && parametersIt.hasNext()) {
        final CType formalParameterType = formalParameterTypesIt.next();
        CExpression parameter = parametersIt.next();
        parameter = conv.makeCastFromArrayToPointerIfNecessary(parameter, formalParameterType);

        Formula argument = toFormula(parameter);
        arguments.add(
            conv.makeCast(
                parameter.getExpressionType(), formalParameterType, argument, constraints, edge));
      }
      assert !formalParameterTypesIt.hasNext() && !parametersIt.hasNext();

      final CType realReturnType = conv.getReturnType(e, edge);
      final FormulaType<?> resultFormulaType = conv.getFormulaTypeFromCType(realReturnType);
      return conv.ffmgr.declareAndCallUF(functionName, resultFormulaType, arguments);
    }
  }

  private record ValidatedFScanFParameter(String format, CExpression receiver) {}

  /**
   * Checks whether the format specifier in the second argument of fscanf agrees with the type of
   * the parameter it writes to. Paragraph § 7.21.6.2 (10) of the C Standard says, that input item
   * read form the stream is converted to the `appropriate` type according to the conversion
   * specifier, e.g., %d. Further § 7.21.6.2 (11-12) tells us the expected argument (receiver) type
   * for each argument, corresponding to a conversion specifier and length modifier .The exact
   * mapping brought forward by the standard is reflected in {@link
   * BuiltinFunctions#getTypeFromScanfFormatSpecifier(String)}.
   *
   * @param formatString the scanf format string
   * @param pVariableType the type of the receiving variable
   * @return whether the scanf-format-specifier agrees with the type it writes to
   * @throws UnsupportedCodeException if the format specifier is not supported
   */
  private boolean isCompatibleWithScanfFormatString(String formatString, CType pVariableType)
      throws UnsupportedCodeException {
    CType expectedType =
        BuiltinFunctions.getTypeFromScanfFormatSpecifier(formatString)
            .orElseThrow(
                () ->
                    new UnsupportedCodeException(
                        "format specifier " + formatString + " not supported.", edge));

    return pVariableType.getCanonicalType().equals(expectedType.getCanonicalType());
  }

  private ValidatedFScanFParameter validateFscanfParameters(
      List<CExpression> pParameters, CFunctionCallExpression e) throws UnrecognizedCodeException {
    if (pParameters.size() < 2) {
      throw new UnrecognizedCodeException("fscanf() needs at least 2 parameters", edge, e);
    }

    if (pParameters.size() > 3) {
      throw new UnsupportedCodeException(
          "fscanf() with more than 3 parameters is not supported", edge, e);
    }

    CExpression file = pParameters.get(0);

    if (file instanceof CIdExpression idExpression) {
      if (!isFilePointer(idExpression.getExpressionType())) {
        throw new UnrecognizedCodeException("First parameter of fscanf() must be a FILE*", edge, e);
      }
    }

    CExpression format = pParameters.get(1);
    String formatString =
        checkFscanfFormatString(format)
            .orElseThrow(
                () ->
                    new UnsupportedCodeException(
                        "Format string of fscanf is not supported", edge, e));

    return new ValidatedFScanFParameter(formatString, pParameters.get(2));
  }

  private boolean isFilePointer(CType pType) {
    if (pType instanceof CPointerType pointerType) {
      if (pointerType.getType() instanceof CTypedefType typedefType) {
        CType realType = typedefType.getRealType();
        if (realType instanceof CElaboratedType elaboratedType) {
          return elaboratedType.getKind() == ComplexTypeKind.STRUCT
              && elaboratedType.getName().equals("_IO_FILE");
        }
      }
    }
    return false;
  }

  private Optional<String> checkFscanfFormatString(CExpression pFormat) {
    ImmutableSet<String> allowlistedFormatStrings =
        BuiltinFunctions.getAllowedScanfFormatSpecifiers();
    if (pFormat instanceof CStringLiteralExpression stringLiteral) {
      String content = stringLiteral.getContentWithoutNullTerminator();
      if (allowlistedFormatStrings.contains(content)) {
        return Optional.of(content);
      }
    }

    return Optional.empty();
  }

  private @Nullable Formula roundNearestTiesAway(
      List<CExpression> pParameters, String pFunctionName, boolean pIsLRound, boolean pIsLongLong)
      throws UnrecognizedCodeException {

    if (pParameters.size() == 1) {
      CType paramType = getTypeOfBuiltinFloatFunction(pFunctionName);
      FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
      if (formulaType.isFloatingPointType()) {
        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        FloatingPointFormula param =
            (FloatingPointFormula) processOperand(pParameters.get(0), paramType, paramType);
        FloatingPointFormula zero = fpfmgr.makeNumber(0, (FloatingPointType) formulaType);
        FloatingPointFormula fp_half = fpfmgr.makeNumber(0.5, (FloatingPointType) formulaType);
        FloatingPointFormula fp_neg_half = fpfmgr.makeNumber(-0.5, (FloatingPointType) formulaType);

        FloatingPointFormula integral = fpfmgr.round(param, FloatingPointRoundingMode.TOWARD_ZERO);
        FloatingPointFormula rounded_negative_Infinity =
            fpfmgr.round(param, FloatingPointRoundingMode.TOWARD_NEGATIVE);
        FloatingPointFormula rounded_positive_Infinity =
            fpfmgr.round(param, FloatingPointRoundingMode.TOWARD_POSITIVE);

        Formula castIntegral = null;
        Formula castNegative = null;
        Formula castPositive = null;

        // the lround and llround functions return "long int" and "long long int", respectively
        if (pIsLRound) {
          FormulaType<?> type =
              pIsLongLong
                  ? conv.getFormulaTypeFromCType(CNumericTypes.LONG_LONG_INT)
                  : conv.getFormulaTypeFromCType(CNumericTypes.LONG_INT);
          final boolean signed = true; // LongLongInt and LongInt are signed

          castIntegral = fpfmgr.castTo(integral, signed, type);
          castNegative = fpfmgr.castTo(rounded_negative_Infinity, signed, type);
          castPositive = fpfmgr.castTo(rounded_positive_Infinity, signed, type);
        }

        // XXX: Currently, MathSAT does not support the rounding mode NEAREST_TIE_AWAY,
        // which corresponds to the semantics of 'round'.
        // Hence, we represent those semantics by the formula below, until there
        // is a release of MathSAT supporting NEAREST_TIE_AWAY.
        //
        // It would be possible to rewrite this code calling roundingBuiltin with
        // NEAREST_TIE_AWAY, catching IllegalArgumentExceptions and in this case
        // proceeding with the hand-built formula below.
        // The benefits of that try-catch approach are debatable and I don't consider
        // it to be of much help for the readability of the code.
        return conv.bfmgr.ifThenElse(
            fpfmgr.greaterThan(param, zero),
            conv.bfmgr.ifThenElse(
                fpfmgr.greaterOrEquals(fpfmgr.subtract(param, integral), fp_half),
                (pIsLRound ? castPositive : rounded_positive_Infinity),
                (pIsLRound ? castIntegral : integral)),
            conv.bfmgr.ifThenElse(
                fpfmgr.lessOrEquals(fpfmgr.subtract(param, integral), fp_neg_half),
                (pIsLRound ? castNegative : rounded_negative_Infinity),
                (pIsLRound ? castIntegral : integral)));
      }
    }

    return null;
  }

  /**
   * The built-in rounding functions of C can all be expressed by the SMT floating point function
   * <code>round</code>, given the corresponding <code>RoundingMode</code>.
   *
   * @param pFunctionName name of built-in function
   * @param pParameters parameter list of built-in function
   * @param pRoundingMode the <code>RoundindMode</code> corresponding to the built-in function
   * @return a {@link Formula} representing the semantics of the rounding function, <code>null
   *     </code> if the length of pParameters or the type of its members do not match
   * @throws UnrecognizedCodeException re-throw from internal calls
   */
  private @Nullable Formula roundingBuiltin(
      String pFunctionName, List<CExpression> pParameters, FloatingPointRoundingMode pRoundingMode)
      throws UnrecognizedCodeException {

    if (pParameters.size() == 1) {
      CType paramType = getTypeOfBuiltinFloatFunction(pFunctionName);
      FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
      if (formulaType.isFloatingPointType()) {
        FloatingPointFormulaManagerView fpfmgr = mgr.getFloatingPointFormulaManager();
        FloatingPointFormula param =
            (FloatingPointFormula) processOperand(pParameters.get(0), paramType, paramType);

        FloatingPointFormula rounded = fpfmgr.round(param, pRoundingMode);

        return rounded;
      }
    }

    return null;
  }

  /**
   * The built-in inequality macros of C behave similar to each other, except the function <code>
   * isunordered</code>.
   *
   * @param pFunctionName name of built-in function
   * @param pParameters parameter list of built-in function
   * @param pFunction inequality function of pFpfmgr, representing the respective built-in function
   * @param pFpfmgr {@link FloatingPointFormulaManagerView} for internal usage
   * @return resulting {@link Formula}
   * @throws UnrecognizedCodeException re-throw from internal calls
   */
  private @Nullable Formula inequalityBuiltin(
      String pFunctionName,
      List<CExpression> pParameters,
      BiFunction<FloatingPointFormula, FloatingPointFormula, BooleanFormula> pFunction,
      FloatingPointFormulaManagerView pFpfmgr)
      throws UnrecognizedCodeException {

    if (pParameters.size() == 2) {
      CType paramType = getTypeOfBuiltinFloatFunction(pFunctionName);
      FormulaType<?> formulaType = conv.getFormulaTypeFromCType(paramType);
      if (formulaType.isFloatingPointType()) {
        FloatingPointFormula param0 =
            (FloatingPointFormula) processOperand(pParameters.get(0), paramType, paramType);
        FloatingPointFormula param1 =
            (FloatingPointFormula) processOperand(pParameters.get(1), paramType, paramType);

        FormulaType<?> resultType = conv.getFormulaTypeFromCType(CNumericTypes.INT);
        Formula zero = mgr.makeNumber(resultType, 0);
        Formula one = mgr.makeNumber(resultType, 1);

        BooleanFormula isFirstNaN = pFpfmgr.isNaN(param0);
        BooleanFormula isSecondNaN = pFpfmgr.isNaN(param1);

        return conv.bfmgr.ifThenElse(
            isFirstNaN,
            zero,
            conv.bfmgr.ifThenElse(
                isSecondNaN,
                zero,
                conv.bfmgr.ifThenElse(pFunction.apply(param0, param1), one, zero)));
      }
    }

    return null;
  }

  /**
   * Handle calls to __builtin_popcount, __builtin_popcountl, and __builtin_popcountll. Popcount
   * sums up all 1-bits of an int, long or long long. Test c programs available:
   * test/programs/simple/builtin_popcount32_x.c and test/programs/simple/builtin_popcount64_x.c
   */
  private Formula handlePopCount(
      String pFunctionName,
      CType pReturnType,
      List<CExpression> pParameters,
      CFunctionCallExpression e)
      throws UnrecognizedCodeException {
    if (pParameters.size() == 1) {
      CType paramType = BuiltinFunctions.getParameterTypeOfBuiltinPopcountFunction(pFunctionName);
      FormulaType<?> paramFormulaType = conv.getFormulaTypeFromCType(paramType);
      FormulaType<?> formulaReturnType = conv.getFormulaTypeFromCType(pReturnType);

      if (paramFormulaType.isBitvectorType()) {
        BitvectorFormulaManagerView bvMgrv = mgr.getBitvectorFormulaManager();
        BitvectorFormula bvParameter = (BitvectorFormula) toFormula(pParameters.get(0));
        BitvectorType bvParamType = (BitvectorType) paramFormulaType;
        BitvectorType bvReturnType = (BitvectorType) formulaReturnType;
        int offset = 0;
        BitvectorFormula result = bvMgrv.makeBitvector(bvReturnType, 0);
        while (offset < bvParamType.getSize()) {
          BitvectorFormula bitAtOffset = bvMgrv.extract(bvParameter, offset, offset++);
          BitvectorFormula bitAtOffsetAsBV =
              bvMgrv.extend(bitAtOffset, bvReturnType.getSize() - 1, false);
          result = bvMgrv.add(result, bitAtOffsetAsBV);
        }
        return result;
      }
      throw new IllegalArgumentException(
          "Popcount implementation does not support non bitvector and non integer type "
              + paramFormulaType
              + " for Edge: "
              + edge);
    }
    throw new UnrecognizedCodeException(
        "Function "
            + pFunctionName
            + " received "
            + pParameters.size()
            + " parameters"
            + " instead of the expected "
            + 1,
        edge,
        e);
  }
}

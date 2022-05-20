// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static org.sosy_lab.cpachecker.cfa.types.c.CBasicType.DOUBLE;
import static org.sosy_lab.cpachecker.cfa.types.c.CBasicType.FLOAT;
import static org.sosy_lab.cpachecker.cfa.types.c.CBasicType.FLOAT128;
import static org.sosy_lab.cpachecker.cfa.types.c.CBasicType.INT;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypes;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/** This Class build binary expression.
 * It handles the promotion and conversion of C-types,
 * depending on the operands,
 * as defined in ISO-C99.
 *
 * This class should be used to get the correct CTypes for a binary operation.
 * There are always 4 types in  a binary operation, that may be different:
 * <ul>
 * <li> the two types of the two operands.
 * <li> the type, in which the calculation is done.
 * <li> the type of the result of the calculation.
 * </ul>
 *
 * <p>Example: <code>{@code "short l = (char)4 < (long)5;"}</code><p>
 *
 * <ul>
 * <li> The values 4 and 5 are used in the calculation as values of type
 *      'char' and 'long', because they are casted to.
 * <li> For the calculation the values are widened to 'long',
 *      because this is the 'longest type' (informally).
 *      Then the calculation is performed as 'long'.
 * <li> The result has type 'int', because the operation is a relational operator (LESS_THAN).
 *      This needs a cast from 'long' to 'int'.
 * </ul>
 * <p>
 * As last step the result would be casted to 'short'.
 * However, this last step is not part of the binary operation,
 * but only of the assignment, and this is not handled within this class.
 *
 */
public class CBinaryExpressionBuilder {

  private final static Set<BinaryOperator> shiftOperators = Sets.immutableEnumSet(
      BinaryOperator.SHIFT_LEFT,
      BinaryOperator.SHIFT_RIGHT);

  private final static Set<BinaryOperator> additiveOperators = Sets.immutableEnumSet(
      BinaryOperator.PLUS,
      BinaryOperator.MINUS);

  @SuppressWarnings("unused")
  private final static Set<BinaryOperator> multiplicativeOperators = Sets.immutableEnumSet(
      BinaryOperator.MULTIPLY,
      BinaryOperator.MODULO,
      BinaryOperator.DIVIDE);

  private final static Set<BinaryOperator> bitwiseOperators = Sets.immutableEnumSet(
      BinaryOperator.BINARY_AND,
      BinaryOperator.BINARY_OR,
      BinaryOperator.BINARY_XOR);

  private final MachineModel machineModel;
  private final LogManager logger;


  /** This constructor does nothing, except initializing fields.
  *
  * @param pMachineModel where to get info about types, for casting and overflows
  * @param pLogger logging
  */
  public CBinaryExpressionBuilder(MachineModel pMachineModel, LogManager pLogger) {
    this.logger = pLogger;
    this.machineModel = pMachineModel;
  }


  /** This method returns a binaryExpression.
   * It is better (safer) to use this method instead of the Constructor of the CBinaryExpression,
   * because the C-type of the expression is build from the operands and the operator.
   *
   *  @param op1 first operand
   *  @param op2 second operand
   *  @param op operator between the operands */
  public CBinaryExpression buildBinaryExpression(CExpression op1, CExpression op2, BinaryOperator op) throws UnrecognizedCodeException {

    // TODO if calculation- and result-function are never needed independent
    // from each other, we could merge them. --> speedup?

    CType t1 = op1.getExpressionType().getCanonicalType();
    CType t2 = op2.getExpressionType().getCanonicalType();

    t1 = handleEnum(t1);
    t2 = handleEnum(t2);

    // For calculation type determination, bit-field types are treated the same as their actual types
    t1 = unwrapBitFields(t1);
    t2 = unwrapBitFields(t2);

    final CType calculationType;
    final CType resultType;

    // if parser cannot determinate type, we ignore the type
    // TODO do we use the correct CProblemType?
    // TODO in special cases (depending on the operator) we could return the correct type
    if (t1 instanceof CProblemType) {
      calculationType = resultType = t1;
    } else if (t2 instanceof CProblemType) {
      calculationType = resultType = t2;

    } else {
      calculationType = getCalculationTypeForBinaryOperation(t1, t2, op, op1, op2);
      resultType = getResultTypeForBinaryOperation(t1, t2, op, op1, op2);
    }

    return new CBinaryExpression(op1.getFileLocation(), resultType, calculationType, op1, op2, op);
  }


  /**
   * This method does the same as {@link #buildBinaryExpression(CExpression, CExpression, BinaryOperator)},
   * but does not throw a checked exception.
   * Use this whenever the operation is guaranteed to be valid.
   */
  public CBinaryExpression buildBinaryExpressionUnchecked(CExpression op1, CExpression op2, BinaryOperator op) {
    try {
      return buildBinaryExpression(op1, op2, op);
    } catch (UnrecognizedCodeException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Create an expression that is the negation of the input.
   * This is better than just building "expr == 0" because if "expr" contains a relational operator
   * we can swap it instead.
   * This makes it easier for many CPAs to handle it.
   */
  public CBinaryExpression negateExpressionAndSimplify(final CExpression expr)
      throws UnrecognizedCodeException {

    if (expr instanceof CBinaryExpression) {
      final CBinaryExpression binExpr = (CBinaryExpression) expr;
      BinaryOperator binOp = binExpr.getOperator();
      // some binary expressions can be directly negated: "!(a==b)" --> "a!=b"
      if (binExpr.getOperator().isLogicalOperator()) {
        BinaryOperator inverseOperator = binOp.getOppositLogicalOperator();
        return buildBinaryExpression(binExpr.getOperand1(), binExpr.getOperand2(), inverseOperator);
      }
      // others can be negated using De Morgan's law:
      if (binOp.equals(BinaryOperator.BINARY_AND) || binOp.equals(BinaryOperator.BINARY_OR)) {
        if (binExpr.getOperand1() instanceof CBinaryExpression
            && binExpr.getOperand2() instanceof CBinaryExpression) {
          final CBinaryExpression binExpr1 = (CBinaryExpression) binExpr.getOperand1();
          final CBinaryExpression binExpr2 = (CBinaryExpression) binExpr.getOperand2();
          if (binExpr1.getOperator().isLogicalOperator()
              && binExpr2.getOperator().isLogicalOperator()) {
            BinaryOperator negatedOperator =
                binOp.equals(BinaryOperator.BINARY_AND)
                    ? BinaryOperator.BINARY_OR
                    : BinaryOperator.BINARY_AND;
            CBinaryExpression newOp1 =
                buildBinaryExpression(
                    binExpr1.getOperand1(),
                    binExpr1.getOperand2(),
                    binExpr1.getOperator().getOppositLogicalOperator());
            CBinaryExpression newOp2 =
                buildBinaryExpression(
                    binExpr2.getOperand1(),
                    binExpr2.getOperand2(),
                    binExpr2.getOperator().getOppositLogicalOperator());
            return buildBinaryExpression(newOp1, newOp2, negatedOperator);
          }
        }
      }
    }

    // at this point, we have an expression, that is not directly boolean (!a, !(a+b), !123), so we compare it with Zero.
    // ISO-C 6.5.3.3: Unary arithmetic operators: The expression !E is equivalent to (0==E).
    // TODO do not wrap numerals, replace them directly with the result? This may be done later with SimplificationVisitor.
    return buildBinaryExpression(CIntegerLiteralExpression.ZERO, expr, BinaryOperator.EQUALS);
  }

  /**
   * This method returns an SIGNED_INT for all enums, and the type itself otherwise.
   */
  private CType handleEnum(final CType pType) {
    /* 6.7.2.2 Enumeration specifiers
     * The expression, that defines the value of an enumeration constant,
     * shall be an integer constant expression, that has a value representable as an int.
     */
    if (pType instanceof CEnumType ||
        (pType instanceof CElaboratedType
        && ((CElaboratedType) pType).getKind() == ComplexTypeKind.ENUM)) {
      return CNumericTypes.SIGNED_INT;
    } else if (pType instanceof CBitFieldType) {
      CBitFieldType bitFieldType = (CBitFieldType) pType;
      CType handledInnerType = handleEnum(bitFieldType.getType());
      if (handledInnerType == bitFieldType.getType()) {
        return pType;
      }
      return new CBitFieldType(handledInnerType, bitFieldType.getBitFieldSize());
    } else {
      return pType;
    }
  }

  /**
   * This method returns the wrapped type for all bit-field types, and the type itself otherwise.
   *
   * @param pType the type.
   * @return the wrapped type for all bit-field types, and the type itself otherwise.
   */
  private CType unwrapBitFields(CType pType) {
    return pType instanceof CBitFieldType ? ((CBitFieldType) pType).getType() : pType;
  }

  /**
   * private method, only visible for Junit-test.
   * <p>
   * This method calculates the type of the result of a binary operation.
   * This type may be different from the type of the calculation itself,
   * in most cases it is equal, only sometimes it is smaller.
   * It should not be bigger than the type of the calculation.
   * The result of the calculation must be casted to the type, that this method returns.
   *
   * @param pType1 type of the first operand
   * @param pType2 type of the second operand
   * @param pBinOperator used to get the result-type
   * @param op1 for logging only
   * @param op2 for logging only
   */
  @VisibleForTesting
  CType getResultTypeForBinaryOperation(final CType pType1, final CType pType2,
      final BinaryOperator pBinOperator, final CExpression op1, final CExpression op2) throws UnrecognizedCodeException {
    /*
     * ISO-C99 (6.5.8 #6): Relational operators
     * Each of the operators <, >, <=, and >= shall yield 1
     * if the specified relation is true and 0 if it is false.
     * The result has type 'int'.
     *
     * ISO-C99 (6.5.9 #3): Equality operators
     * The == and != operators are analogous to the relational operators [...]
     * The result has type 'int'.
     */
    if (pBinOperator.isLogicalOperator()) { return CNumericTypes.SIGNED_INT; }

    /*
     * ISO-C99 (6.5.7 #3): Bitwise shift operators
     * The integer promotions are performed on each of the operands.
     * The type of the result is that of the promoted left operand.
     */
    if (shiftOperators.contains(pBinOperator)) {
      checkIntegerType(pType1, pBinOperator, op1);
      checkIntegerType(pType2, pBinOperator, op2);
      return machineModel.applyIntegerPromotion(pType1);
    }

    if (bitwiseOperators.contains(pBinOperator)) {
      checkIntegerType(pType1, pBinOperator, op1);
      checkIntegerType(pType2, pBinOperator, op2);
    }

    return getCalculationTypeForBinaryOperation(pType1, pType2, pBinOperator, op1, op2);
  }

  /**
   * private method, only visible for Junit-test.
   * <p>
   * This method returns the "common real type" of a binary expression.
   * For detail see ISO-C-Standard:
   * <ul>
   * <li>6.3.1.8 Usual arithmetic conversions
   * <li>6.5.5   Multiplicative Operators
   * <li>6.5.6   Additive Operators
   * </ul>
   *
   * @param pType1 type of the first operand
   * @param pType2 type of the second operand
   * @param pBinOperator for logging only
   * @param op1 for logging only
   * @param op2 for logging only
   */
  @VisibleForTesting
  CType getCalculationTypeForBinaryOperation(CType pType1, CType pType2,
      final BinaryOperator pBinOperator, final CExpression op1, final CExpression op2) throws UnrecognizedCodeException {

    /* CalculationType of SHIFT is the type of the first operand.
     *
     * ISO-C99 (6.5.7 #3): Bitwise shift operators
     * The integer promotions are performed on each of the operands.
     * The type of the result is that of the promoted left operand.
     */
    if (shiftOperators.contains(pBinOperator)) {
      checkIntegerType(pType1, pBinOperator, op1);
      checkIntegerType(pType2, pBinOperator, op2);
      return machineModel.applyIntegerPromotion(pType1);
    }

    // both are simple types, we need a common simple type --> USUAL ARITHMETIC CONVERSIONS
    if (pType1 instanceof CSimpleType && pType2 instanceof CSimpleType) {
      // TODO we need an recursive analysis for wrapped binaryExp, like "((1+2)+3)+4".

      final CType commonType = getCommonSimpleTypeForBinaryOperation((CSimpleType) pType1, (CSimpleType) pType2);

      logger.logf(Level.ALL, "type-conversion: %s (%s, %s) -> %s",
          pBinOperator, pType1, pType2, commonType);

      return commonType;
    }


    if (pType1 instanceof CSimpleType) { return getSecondTypeToSimpleType(pType2, pBinOperator, op1, op2); }
    if (pType2 instanceof CSimpleType) { return getSecondTypeToSimpleType(pType1, pBinOperator, op1, op2); }


    // both are pointer or function-pointer
    if ((pType1 instanceof CPointerType || pType1 instanceof CFunctionType || pType1 instanceof CArrayType)
        && (pType2 instanceof CPointerType || pType2 instanceof CFunctionType || pType2 instanceof CArrayType)) {

      if (pBinOperator == BinaryOperator.MINUS) { return machineModel.getPointerDiffType(); }

      if (!pBinOperator.isLogicalOperator()) {
        throw new UnrecognizedCodeException(
            "Operator " + pBinOperator + " cannot be used with two pointer operands",
            getDummyBinExprForLogging(pBinOperator, op1, op2));
      }

      // we compare function-pointer and function, so return function-pointer
      if (pType1 instanceof CPointerType && pType2 instanceof CFunctionType) {
        if (((CPointerType) pType1).getType() instanceof CFunctionType) { return pType1; }
      } else if (pType2 instanceof CPointerType && pType1 instanceof CFunctionType) {
        if (((CPointerType) pType2).getType() instanceof CFunctionType) { return pType2; }
      }

      // there are 2 pointers and this is a comparison, so type does actually not matter.

      if (pType1.equals(pType2)) { return pType1; }

      logger.logf(Level.FINEST,
          "using calculationtype POINTER_TO_VOID for %s (%s, %s) of (%s, %s)",
          pBinOperator, pType1, pType2, pType1.getClass(), pType2.getClass());

      return CPointerType.POINTER_TO_VOID;
    }

    if (pType1 instanceof CCompositeType || pType1 instanceof CElaboratedType
        || pType2 instanceof CCompositeType || pType2 instanceof CElaboratedType) {
      throw new UnrecognizedCodeException(
          "Operator " + pBinOperator + " cannot be used with composite-type operand",
          getDummyBinExprForLogging(pBinOperator, op1, op2));
    }

    // TODO check if there are other types, that can be used in binaryExp
    throw new AssertionError(String.format(
        "unhandled type-conversion: %s (%s, %s) of (%s, %s)",
        pBinOperator, pType1, pType2, pType1.getClass(), pType2.getClass()));
  }


  /**
   * This method returns the (simplified) type of the second operand,
   * if the first operand was from CSimpleType.
   * This method does not depend on the first type.
   *
   * @param pType type to analyse
   * @param pBinOperator for checks and logging only
   * @param op1 for checks and logging only
   * @param op2 for checks and logging only
   */
  private CType getSecondTypeToSimpleType(final CType pType,
      final BinaryOperator pBinOperator, final CExpression op1, final CExpression op2) throws UnrecognizedCodeException {

    // if one type is an pointer, return the pointer.
    if (pType instanceof CPointerType) {
      if (!additiveOperators.contains(pBinOperator) && !pBinOperator.isLogicalOperator()) {
        throw new UnrecognizedCodeException(
            "Operator " + pBinOperator + " cannot be used with pointer operand",
            getDummyBinExprForLogging(pBinOperator, op1, op2));
      }
      return pType;
    }

    // if one type is an array, return the pointer-equivalent to the array-type.
    if (pType instanceof CArrayType) {
      if (!additiveOperators.contains(pBinOperator) && !pBinOperator.isLogicalOperator()) {
        throw new UnrecognizedCodeException(
            "Operator " + pBinOperator + " cannot be used with array operand",
            getDummyBinExprForLogging(pBinOperator, op1, op2));
      }
      final CArrayType at = ((CArrayType) pType);
      return new CPointerType(at.isConst(), at.isVolatile(), at.getType());
    }

    if (pType instanceof CProblemType) { return CNumericTypes.SIGNED_INT; }
    throw new AssertionError("unhandled type (secondtype to simple type): " + pType);
  }


  /** This method should return the "common real type" of an arithmetic binary expression.
   * For detail see C-Standard: 6.3.1.8 Usual arithmetic conversions.
   * This method should be better than the (current) eclipse-parser,
   * that does not always return the correct common type.
   * <p>
   * (Example: char + char --> int, not char.
   * For chars there could be an error in the calculation of 120*8/8.)
   *
   * @param t1 canonical type of the operand
   * @param t2 canonical type of the operand
   * @return common simple type for t1 and t2
   */
  private CSimpleType getCommonSimpleTypeForBinaryOperation(CSimpleType t1, CSimpleType t2) {

    assert t1.equals(t1.getCanonicalType()) && t2.equals(t2.getCanonicalType());

    // shortcut is not possible, because 'char' is casted to 'int'.
    // --> if(t1.equals(t2)) return t1;

    /* ________ USUAL ARITHMETIC CONVERSIONS ________
     *
     *  taken from ISO-C99 6.3.1.8
     *  see also:
     *  http://msdn.microsoft.com/en-us/library/3t4w2bkb%28v=vs.80%29.aspx (same as ISO-C99, but without LongLong)
     */

    /* First, if the corresponding real type of either operand is long double,
     * the other operand is converted, without change of type domain,
     * to a type whose corresponding real type is long double. */

    if (t1.getType() == DOUBLE && t1.isLong()) { return t1; }
    if (t2.getType() == DOUBLE && t2.isLong()) { return t2; }

    /* Otherwise, if the corresponding real type of either operand is double,
     * the other operand is converted, without change of type domain,
     * to a type whose corresponding real type is double. */

    if (t1.getType() == DOUBLE) { return t1; }
    if (t2.getType() == DOUBLE) { return t2; }

    /* Otherwise, if the corresponding real type of either operand is float,
     * the other operand is converted, without change of type domain,
     * to a type whose corresponding real type is float. */

    if (t1.getType() == FLOAT) { return t1; }
    if (t2.getType() == FLOAT) { return t2; }

    if (t1.getType() == FLOAT128) {
      return t1;
    }
    if (t2.getType() == FLOAT128) {
      return t2;
    }

    /* Otherwise, the integer promotions are performed on both operands. */

    return getLongestIntegerPromotion(t1, t2);
  }


  private CSimpleType getLongestIntegerPromotion(CSimpleType t1, CSimpleType t2) {

    assert (t1.getType() != INT || (t1.isUnsigned() ^ t1.isSigned())) : "INT must be signed xor unsigned: " + t1;
    assert (t2.getType() != INT || (t2.isUnsigned() ^ t2.isSigned())) : "INT must be signed xor unsigned: " + t2;
    assert !(t1.isLong() && t1.isLongLong()) : "type cannot be long and longlong: " + t1;
    assert !(t2.isLong() && t2.isLongLong()) : "type cannot be long and longlong: " + t2;

    /* see also:
     * https://www.securecoding.cert.org/confluence/display/seccode/INT02-C.+Understand+integer+conversion+rules
     * http://www.idryman.org/blog/2012/11/21/integer-promotion (real c-code-example for integer promotion)
     * http://en.wikibooks.org/wiki/C%2B%2B_Programming/Programming_Languages/C%2B%2B/Code/Statements/Variables/Type_Casting#Promotion
     * http://de.wikipedia.org/wiki/Promotion_%28Typumwandlung%29
     */

    /* ________ USUAL ARITHMETIC CONVERSIONS ________
     *
     *  taken from ISO-C99 6.3.1.8, part 2
     */

    /* Otherwise, the integer promotions are performed on both operands.
     * Then the following rules are applied to the promoted operands: */

    t1 = (CSimpleType) machineModel.applyIntegerPromotion(t1);
    t2 = (CSimpleType) machineModel.applyIntegerPromotion(t2);

    final int rank1 = getConversionRank(t1);
    final int rank2 = getConversionRank(t2);

    /* If both operands have the same type, then no further conversion is needed. */
    //      --> this is implicitly handled with next case

    /* Otherwise, if both operands have signed integer types or both have
     * unsigned integer types, the operand with the type of lesser integer
     * conversion rank is converted to the type of the operand with greater rank. */

    if (t1.isUnsigned() == t2.isUnsigned()) {
      assert t1.isSigned() == t2.isSigned();
      return (rank1 > rank2) ? t1 : t2;
    }

    final int size1 = machineModel.getSizeof(t1);
    final int size2 = machineModel.getSizeof(t2);

    /* Otherwise, if the operand that has unsigned integer type has rank
     * greater or equal to the rank of the type of the other operand,
     * then the operand with signed integer type is converted to the
     * type of the operand with unsigned integer type. */

    if (t1.isUnsigned() && rank1 >= rank2) { return t1; }
    if (t2.isUnsigned() && rank2 >= rank1) { return t2; }

    /* Otherwise, if the type of the operand with signed integer type
     * can represent all of the values of the type of the operand with
     * unsigned integer type, then the operand with unsigned integer type
     * is converted to the type of the operand with signed integer type. */

    // a full representation of 'unsigned' as 'signed' needs a bigger bitsize
    if (t1.isSigned() && size1 > size2) { return t1; }
    if (t2.isSigned() && size2 > size1) { return t2; }

    /* Otherwise, both operands are converted to the unsigned integer type
     * corresponding to the type of the operand with signed integer type. */

    if (t1.isSigned()) { return new CSimpleType(false, false, INT,
        t1.isLong(), false, false, true, false, false, t1.isLongLong()); }

    if (t2.isSigned()) { return new CSimpleType(false, false, INT,
        t2.isLong(), false, false, true, false, false, t2.isLongLong()); }

    throw new AssertionError("unhandled type: " + t1 + " or " + t2);
  }

  /** returns an index, so that: BOOL < CHAR < SHORT < INT < LONG < LONGLONG < INT128. */
  private static int getConversionRank(CSimpleType t) {

    CBasicType type = t.getType();

    switch (type) {
    case BOOL:
      // The rank of _Bool shall be less than the rank of all other standard integer types.
      return 10;

    case CHAR:
      // The rank of char shall equal the rank of signed char and unsigned char.
      return 20;

    case INT:
      /* The rank of any unsigned integer type shall equal the rank of the
       * corresponding signed integer type, if any.
       * The rank of long long int shall be greater than the rank of long int,
       * which shall be greater than the rank of int,
       * which shall be greater than the rank of short int,
       * which shall be greater than the rank of signed char.
       */
      if (t.isShort()) { return 30; }
      if (t.isLong()) { return 50; }
      if (t.isLongLong()) { return 60; }
      return 40;

      case INT128:
        return 70;

    default:
      throw new AssertionError("unhandled CSimpleType: " + t);
    }
  }

  /** only for logging or exceptions */
  private static CBinaryExpression getDummyBinExprForLogging(
          final BinaryOperator pBinOperator, final CExpression op1, final CExpression op2) {
    return new CBinaryExpression(op1.getFileLocation(), null, null, op1, op2, pBinOperator);
  }

  private static void checkIntegerType(final CType pType, final BinaryOperator op, CExpression e) throws UnrecognizedCodeException {
    if (!CTypes.isIntegerType(pType)) {
      throw new UnrecognizedCodeException(
          "Operator " + op + " needs integer type, but is used with " + pType, e);
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import com.google.common.base.Splitter;
import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBitVectorConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibSmtFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

public class FormulaToSvLibVisitor implements FormulaVisitor<SvLibTerm> {

  private final FormulaManagerView fmgr;
  private final SvLibScope scope;

  public FormulaToSvLibVisitor(FormulaManagerView pFmgr, SvLibScope pScope) {
    fmgr = pFmgr;
    scope = pScope;
  }

  private SvLibSmtLibType formulaTypeToSvLibType(FormulaType<?> formulaType) {
    if (formulaType.equals(FormulaType.BooleanType)) {
      return SvLibSmtLibPredefinedType.BOOL;
    } else if (formulaType.equals(FormulaType.IntegerType)) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (formulaType.equals(FormulaType.RationalType)) {
      return SvLibSmtLibPredefinedType.REAL;
    } else if (formulaType instanceof ArrayFormulaType<?, ?> pFormulaType) {
      SvLibSmtLibType indexType = formulaTypeToSvLibType(pFormulaType.getIndexType());
      SvLibSmtLibType elementType = formulaTypeToSvLibType(pFormulaType.getElementType());
      return new SvLibSmtLibArrayType(indexType, elementType);
    } else if (formulaType instanceof FormulaType.BitvectorType bitVectorType) {
      return new SvLibSmtLibBitVectorType(bitVectorType.getSize());
    } else if (formulaType instanceof FormulaType.FloatingPointType floatingPointType) {
      return new SvLibSmtLibFloatingPointType(
          floatingPointType.getExponentSize(), floatingPointType.getMantissaSizeWithHiddenBit());
    } else if (formulaType.isFloatingPointRoundingModeType()) {
      return SvLibSmtLibPredefinedType.ROUNDING_MODE;
    }

    throw new UnsupportedOperationException("Unsupported formula type: " + formulaType);
  }

  /**
   * The name that is used for the given bitvector operator below.
   *
   * <p>Every solver names these operators differently: MathSAT5 for example appends the bit widths
   * to the name and calls a left shift {@code bvlshl_32} and a zero extension {@code bvzext_8_32},
   * where Z3 uses {@code bvshl} and {@code zero_extend}. The solver-independent declaration kind is
   * the same for all solvers, so it is used instead of the name.
   *
   * @return the name for the operator, or an empty {@link Optional} if the kind does not identify a
   *     bitvector operator.
   */
  private static Optional<String> canonicalNameOfBitvectorOperator(FunctionDeclarationKind pKind) {
    return Optional.ofNullable(
        switch (pKind) {
          case BV_EXTRACT -> "extract";
          case BV_CONCAT -> "concat";
          case BV_ZERO_EXTENSION -> "zero_extend";
          case BV_SIGN_EXTENSION -> "sign_extend";
          case BV_NOT -> "bvnot";
          case BV_NEG -> "bvneg";
          case BV_AND -> "bvand";
          case BV_OR -> "bvor";
          case BV_XOR -> "bvxor";
          case BV_ADD -> "bvadd";
          case BV_SUB -> "bvsub";
          case BV_MUL -> "bvmul";
          case BV_SDIV -> "bvsdiv";
          case BV_UDIV -> "bvudiv";
          case BV_SREM -> "bvsrem";
          case BV_UREM -> "bvurem";
          case BV_SHL -> "bvshl";
          case BV_LSHR -> "bvlshr";
          case BV_ASHR -> "bvashr";
          case BV_SLT -> "bvslt";
          case BV_ULT -> "bvult";
          case BV_SLE -> "bvsle";
          case BV_ULE -> "bvule";
          case BV_SGT -> "bvsgt";
          case BV_UGT -> "bvugt";
          case BV_SGE -> "bvsge";
          case BV_UGE -> "bvuge";
          case BV_EQ -> "=";
          default -> null;
        });
  }

  /**
   * Pattern for the name that MathSAT5 uses for an extraction, which contains the index of the most
   * significant and of the least significant extracted bit and the width of the argument.
   */
  private static final Pattern MATHSAT_BITVECTOR_EXTRACT_PATTERN =
      Pattern.compile("^bvextract_([0-9]+)_([0-9]+)_([0-9]+)$");

  /**
   * The declaration of the extraction that the given solver-specific name denotes.
   *
   * <p>An extraction is only well defined together with the indices of the extracted bits, and
   * those are not part of the information that the SMT solvers provide about a function declaration
   * in general. They can be recovered from the name for MathSAT5, which contains them.
   */
  private static SvLibFunctionDeclaration bitVectorExtractDeclaration(
      String pName, int pArgTypeSize, int pReturnTypeSize) {
    Matcher matcher = MATHSAT_BITVECTOR_EXTRACT_PATTERN.matcher(pName.replace("`", ""));
    if (matcher.matches()) {
      int mostSignificantBit = Integer.parseInt(matcher.group(1));
      int leastSignificantBit = Integer.parseInt(matcher.group(2));
      Verify.verify(mostSignificantBit - leastSignificantBit + 1 == pReturnTypeSize);
      return SmtLibTheoryDeclarations.bitVectorExtract(
          pArgTypeSize, mostSignificantBit, leastSignificantBit);
    }
    if (pReturnTypeSize == pArgTypeSize) {
      // An extraction of all bits is the identity, so the indices are known.
      return SmtLibTheoryDeclarations.bitVectorExtract(pArgTypeSize, pArgTypeSize - 1, 0);
    }
    throw new UnsupportedOperationException(
        "The indices of the bits that the extraction "
            + pName
            + " extracts cannot be determined, so it cannot be transformed to SV-LIB.");
  }

  /**
   * The term for an operator that none of the theories above handles.
   *
   * <p>If it is an uninterpreted function, which is how CPAchecker encodes the bitwise operators
   * when bitvectors are encoded as integers, it is declared in the generated script so that the
   * analysis of that script knows as little about it as the analysis of the C program does.
   */
  private SvLibIdTerm uninterpretedFunctionOrUnsupported(
      String pName,
      FunctionDeclarationKind pKind,
      SvLibType pReturnType,
      List<@NonNull SvLibSmtLibType> pArgTypes) {
    if (pKind != FunctionDeclarationKind.UF) {
      throw new UnsupportedOperationException("Unknown formula type: " + pName);
    }
    return new SvLibIdTerm(
        declareUninterpretedFunction(pName, pReturnType, pArgTypes), FileLocation.DUMMY);
  }

  /**
   * Declare the given uninterpreted function in the scope, so that the generated script contains
   * its declaration, and return the declaration to use for its applications.
   */
  private SvLibFunctionDeclaration declareUninterpretedFunction(
      String pName, SvLibType pReturnType, List<@NonNull SvLibSmtLibType> pArgTypes) {
    ImmutableList<SvLibType> argumentTypes = ImmutableList.copyOf(pArgTypes);
    String name = asSymbol(pName);
    scope.addFunctionDeclaration(
        new SvLibSmtFunctionDeclaration(FileLocation.DUMMY, name, argumentTypes, pReturnType));
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(argumentTypes, pReturnType),
        name,
        name,
        ImmutableList.of());
  }

  /**
   * A simple symbol of SMT-LIB, which consists of letters, digits and some punctuation and does not
   * start with a digit.
   */
  private static final Pattern SIMPLE_SYMBOL =
      Pattern.compile("[A-Za-z~!@$%^&*_+=<>.?/-][A-Za-z0-9~!@$%^&*_+=<>.?/-]*");

  /**
   * The given name of an operator of the formulas as a symbol of SMT-LIB, quoted if it is not a
   * simple one.
   *
   * <p>The name that a solver uses for an operator can contain characters that a symbol must not,
   * such as the parentheses in "_concat(32,32)" for the concatenation of two bitvectors that
   * CPAchecker encodes as integers.
   */
  private static String asSymbol(String pName) {
    return SIMPLE_SYMBOL.matcher(pName).matches() ? pName : "|" + pName + "|";
  }

  /** Is the given kind an operator of the theory of floating point numbers? */
  private static boolean isFloatingPointOperator(FunctionDeclarationKind pKind) {
    return FLOATING_POINT_OPERATORS.containsKey(pKind);
  }

  /**
   * The names of the operators of the theory of floating point numbers, and whether the operator
   * rounds, i.e. whether its first argument is a rounding mode.
   */
  private static final ImmutableMap<FunctionDeclarationKind, FloatingPointOperator>
      FLOATING_POINT_OPERATORS =
          ImmutableMap.<FunctionDeclarationKind, FloatingPointOperator>builder()
              .put(FunctionDeclarationKind.FP_ADD, new FloatingPointOperator("fp.add", true))
              .put(FunctionDeclarationKind.FP_SUB, new FloatingPointOperator("fp.sub", true))
              .put(FunctionDeclarationKind.FP_MUL, new FloatingPointOperator("fp.mul", true))
              .put(FunctionDeclarationKind.FP_DIV, new FloatingPointOperator("fp.div", true))
              .put(FunctionDeclarationKind.FP_SQRT, new FloatingPointOperator("fp.sqrt", true))
              .put(
                  FunctionDeclarationKind.FP_ROUND_TO_INTEGRAL,
                  new FloatingPointOperator("fp.roundToIntegral", true))
              .put(FunctionDeclarationKind.FP_REM, new FloatingPointOperator("fp.rem", false))
              .put(FunctionDeclarationKind.FP_NEG, new FloatingPointOperator("fp.neg", false))
              .put(FunctionDeclarationKind.FP_ABS, new FloatingPointOperator("fp.abs", false))
              .put(FunctionDeclarationKind.FP_MAX, new FloatingPointOperator("fp.max", false))
              .put(FunctionDeclarationKind.FP_MIN, new FloatingPointOperator("fp.min", false))
              .put(FunctionDeclarationKind.FP_LT, new FloatingPointOperator("fp.lt", false))
              .put(FunctionDeclarationKind.FP_LE, new FloatingPointOperator("fp.leq", false))
              .put(FunctionDeclarationKind.FP_GT, new FloatingPointOperator("fp.gt", false))
              .put(FunctionDeclarationKind.FP_GE, new FloatingPointOperator("fp.geq", false))
              .put(FunctionDeclarationKind.FP_EQ, new FloatingPointOperator("fp.eq", false))
              .put(FunctionDeclarationKind.FP_IS_NAN, new FloatingPointOperator("fp.isNaN", false))
              .put(
                  FunctionDeclarationKind.FP_IS_INF,
                  new FloatingPointOperator("fp.isInfinite", false))
              .put(
                  FunctionDeclarationKind.FP_IS_ZERO, new FloatingPointOperator("fp.isZero", false))
              .put(
                  FunctionDeclarationKind.FP_IS_NEGATIVE,
                  new FloatingPointOperator("fp.isNegative", false))
              .put(
                  FunctionDeclarationKind.FP_IS_SUBNORMAL,
                  new FloatingPointOperator("fp.isSubnormal", false))
              .put(
                  FunctionDeclarationKind.FP_IS_NORMAL,
                  new FloatingPointOperator("fp.isNormal", false))
              .buildOrThrow();

  /**
   * An operator of the theory of floating point numbers.
   *
   * @param name the name of the operator in SMT-LIB
   * @param rounds whether its first argument is a rounding mode
   */
  private record FloatingPointOperator(String name, boolean rounds) {}

  /** The declaration of the given operator of the theory of floating point numbers. */
  private SvLibFunctionDeclaration floatingPointDeclaration(
      FunctionDeclarationKind pKind,
      String pName,
      SvLibType pReturnType,
      List<@NonNull SvLibSmtLibType> pArgTypes) {
    FloatingPointOperator operator = FLOATING_POINT_OPERATORS.get(pKind);
    // The type of the operands is the floating point type, which for the operators that round is
    // not the type of the first argument, and for the predicates is not the return type.
    SvLibSmtLibFloatingPointType floatingPointType =
        pArgTypes.stream()
            .filter(SvLibSmtLibFloatingPointType.class::isInstance)
            .map(SvLibSmtLibFloatingPointType.class::cast)
            .findFirst()
            .orElseThrow(
                () ->
                    new UnsupportedOperationException(
                        "The operator " + pName + " is not applied to a floating point number"));
    if (operator.rounds()) {
      return SmtLibTheoryDeclarations.floatingPointArithmetic(
          operator.name(), pArgTypes.size() - 1, floatingPointType);
    }
    return SmtLibTheoryDeclarations.floatingPointOperation(
        operator.name(), pArgTypes.size(), floatingPointType, pReturnType);
  }

  private SvLibIdTerm functionToIdTerm(
      String pName, SvLibType pReturnType, List<@NonNull SvLibSmtLibType> pArgTypes) {

    String actualName =
        pName
            // Remove type suffixes from overloaded operators, like '_int'
            .replace("_int", "")
            .replace("_rat", "")
            .replaceAll("_T" + Pattern.quote("(") + "[0-9]+" + Pattern.quote(")"), "");

    // To ensure that ITE is always detected, it must be processed before the other Boolean
    // operators; otherwise, ITE with only Boolean parameters will cause an exception
    if (pArgTypes.size() == 3
        && pArgTypes.getFirst().equals(SvLibSmtLibPredefinedType.BOOL)
        && pArgTypes.get(1).equals(pArgTypes.get(2))
        && actualName.equals("if")) {
      return new SvLibIdTerm(SmtLibTheoryDeclarations.ite(pArgTypes.get(1)), FileLocation.DUMMY);
    } else if (pReturnType == SvLibSmtLibPredefinedType.BOOL
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.BOOL))) {
      return switch (actualName) {
        case "and" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.boolConjunction(pArgTypes.size()), FileLocation.DUMMY);
        case "or" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.boolDisjunction(pArgTypes.size()), FileLocation.DUMMY);
        case "not" -> new SvLibIdTerm(SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.BOOL
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.INT))) {
      return switch (actualName) {
        case "=" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY);
        case "<" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY);
        case "<=" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN, FileLocation.DUMMY);
        case ">" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_GREATER_THAN, FileLocation.DUMMY);
        case ">=" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_GREATER_EQUAL_THAN, FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.INT
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.INT))) {
      return switch (actualName) {
        case "+" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.intAddition(pArgTypes.size()), FileLocation.DUMMY);
        case "-" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.intSubtraction(pArgTypes.size()), FileLocation.DUMMY);
        case "*", "Integer_*_" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MULTIPLICATION, FileLocation.DUMMY);
        case "/", "Integer_/_", "div" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_DIV, FileLocation.DUMMY);
        case "_%_" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MOD, FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.REAL
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.REAL))) {
      return switch (actualName) {
        case "+" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.realAddition(pArgTypes.size()), FileLocation.DUMMY);
        case "-" -> new SvLibIdTerm(SmtLibTheoryDeclarations.REAL_MINUS, FileLocation.DUMMY);
        case "*" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.REAL_MULTIPLICATION, FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.INT
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.REAL))) {
      return switch (actualName) {
        case "floor" -> new SvLibIdTerm(SmtLibTheoryDeclarations.REAL_FLOOR, FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pArgTypes.size() == 2
        && pArgTypes.getFirst() instanceof SvLibSmtLibArrayType pArrayType
        && pArrayType.getKeysType().equals(pArgTypes.get(1))
        && pArrayType.getValuesType().equals(pReturnType)
        && (actualName.equals("read") || actualName.equals("select"))) {
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.arraySelect(
              pArrayType.getKeysType(), pArrayType.getValuesType()),
          FileLocation.DUMMY);
    } else if (pArgTypes.size() == 3
        && pArgTypes.getFirst() instanceof SvLibSmtLibArrayType pArrayType
        && pArrayType.getKeysType().equals(pArgTypes.get(1))
        && pArrayType.getValuesType().equals(pArgTypes.get(2))
        && pReturnType.equals(pArrayType)
        && (actualName.equals("write") || actualName.equals("store"))) {
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.arrayStore(pArrayType.getKeysType(), pArrayType.getValuesType()),
          FileLocation.DUMMY);
    } else if (actualName.equals("=")
        && pArgTypes.size() == 2
        && pReturnType.equals(SvLibSmtLibPredefinedType.BOOL)
        && pArgTypes.getFirst().equals(pArgTypes.get(1))
        && pArgTypes.getFirst() instanceof SvLibSmtLibArrayType pArrayType) {
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.arrayEquality(
              pArrayType.getKeysType(), pArrayType.getValuesType()),
          FileLocation.DUMMY);
    } else if (pReturnType == SvLibSmtLibPredefinedType.BOOL
        && pArgTypes.size() == 2
        && FluentIterable.from(pArgTypes).allMatch(type -> type instanceof SvLibSmtLibBitVectorType)
        && pArgTypes.getFirst() instanceof SvLibSmtLibBitVectorType bitVector) {
      int size = bitVector.getSize();
      return switch (actualName) {
        case "=" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorEquality(size), FileLocation.DUMMY);
        case "bvule" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorUnsignedLessEqual(size), FileLocation.DUMMY);
        case "bvult" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorUnsignedLessThan(size), FileLocation.DUMMY);
        case "bvuge" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorUnsignedGreaterEqual(size), FileLocation.DUMMY);
        case "bvugt" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorUnsignedGreaterThan(size), FileLocation.DUMMY);
        case "bvsle" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignedLessEqual(size), FileLocation.DUMMY);
        case "bvslt" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignedLessThan(size), FileLocation.DUMMY);
        case "bvsge" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignedGreaterEqual(size), FileLocation.DUMMY);
        case "bvsgt" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignedGreaterThan(size), FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pReturnType instanceof SvLibSmtLibBitVectorType returnBitVectorType
        && pArgTypes.size() == 1
        && pArgTypes.getFirst() instanceof SvLibSmtLibBitVectorType argBitVectorType
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type instanceof SvLibSmtLibBitVectorType)) {
      int returnTypeSize = returnBitVectorType.getSize();
      int argTypeSize = argBitVectorType.getSize();
      return switch (actualName) {
        case "extract" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorExtract(argTypeSize, returnTypeSize),
                FileLocation.DUMMY);
        case "bvneg" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorComplementNegation(returnTypeSize),
                FileLocation.DUMMY);
        case "bvnot" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorBitwiseNegation(returnTypeSize),
                FileLocation.DUMMY);
        case "zero_extend" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorZeroExtend(argTypeSize, returnTypeSize),
                FileLocation.DUMMY);
        case "sign_extend" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignExtend(argTypeSize, returnTypeSize),
                FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    } else if (pReturnType instanceof SvLibSmtLibBitVectorType bitVector
        && pArgTypes.size() == 2
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type instanceof SvLibSmtLibBitVectorType)) {
      int size = bitVector.getSize();
      return switch (actualName) {
        // TODO case "concat" -> new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorConcat());
        case "bvand" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorAnd(size), FileLocation.DUMMY);
        case "bvor" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorOr(size), FileLocation.DUMMY);
        case "bvadd" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorAddition(size), FileLocation.DUMMY);
        case "bvmul" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorMul(size), FileLocation.DUMMY);
        case "bvudiv" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorUnsignedDivision(size), FileLocation.DUMMY);
        case "bvurem" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorUnsignedRemainder(size), FileLocation.DUMMY);
        case "bvshl" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorShiftLeft(size), FileLocation.DUMMY);
        case "bvlshr" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorLogicalShiftRight(size), FileLocation.DUMMY);
        case "bvsdiv" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignedDivision(size), FileLocation.DUMMY);
        case "bvsrem" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSignedRemainder(size), FileLocation.DUMMY);
        case "bvsub" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorSubstraction(size), FileLocation.DUMMY);
        default -> throw new UnsupportedOperationException("Unknown formula type: " + pName);
      };
    }

    throw new UnsupportedOperationException("Unknown formula type: " + pName);
  }

  @Override
  public SvLibTerm visitFreeVariable(Formula pFormula, String pS) {
    String nameWithoutSSA = pS.replaceAll("@$", "").replaceAll("@[0-9]+", "");

    if (nameWithoutSSA.startsWith("__ADDRESS_OF_")) {
      SvLibSimpleDeclaration addressVariableDeclaration =
          scope.getVariable("|" + nameWithoutSSA + "|").toSimpleDeclaration();
      return new SvLibIdTerm(addressVariableDeclaration, FileLocation.DUMMY);
    }

    SvLibSimpleDeclaration variableDeclaration =
        scope.getVariableForQualifiedName(nameWithoutSSA).toSimpleDeclaration();
    return new SvLibIdTerm(variableDeclaration, FileLocation.DUMMY);
  }

  @Override
  public SvLibTerm visitConstant(Formula pFormula, Object pO) {
    if (pO instanceof Boolean pBoolean) {
      return new SvLibBooleanConstantTerm(pBoolean, FileLocation.DUMMY);
    } else if (pO instanceof BigInteger pInteger && pFormula instanceof IntegerFormula) {
      return new SvLibIntegerConstantTerm(pInteger, FileLocation.DUMMY);
    } else if (pO instanceof BigInteger pInteger && pFormula instanceof BitvectorFormula) {
      FormulaType<?> formulaType = fmgr.getFormulaType(pFormula);
      Verify.verify(
          formulaType instanceof FormulaType.BitvectorType,
          "Obtained a bitvector formula which does not have the bitvector type");
      return new SvLibBitVectorConstantTerm(
          pInteger, ((FormulaType.BitvectorType) formulaType).getSize(), FileLocation.DUMMY);
    } else if (pO instanceof Rational pRational) {
      return new SvLibRealConstantTerm(pRational, FileLocation.DUMMY);
    }
    throw new UnsupportedOperationException("Unsupported constant type: " + pO);
  }

  @Override
  public SvLibTerm visitFunction(
      Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {

    SvLibType formulaType = formulaTypeToSvLibType(pFunctionDeclaration.getType());
    List<SvLibSmtLibType> argTypes =
        pFunctionDeclaration.getArgumentTypes().stream().map(this::formulaTypeToSvLibType).toList();
    String functionName = pFunctionDeclaration.getName().replace("`", "");

    List<SvLibTerm> args = pList.stream().map(f -> fmgr.visit(f, this)).toList();

    if (formulaType.equals(SvLibSmtLibPredefinedType.BOOL)
        && argTypes.size() == 2
        && argTypes.getFirst().equals(argTypes.get(1))
        && functionName.startsWith("int_mod_congr_")) {
      // Handle congruence relations generated by the SMT solver for modular arithmetic
      // This is apparently only done by MathSAT. We rewrite these back into equalities.
      SvLibIntegerConstantTerm modulusTerm =
          new SvLibIntegerConstantTerm(
              new BigInteger(Splitter.on("_").splitToList(functionName).getLast()),
              FileLocation.DUMMY);
      SvLibTerm leftTerm = args.getFirst();
      SvLibTerm rightTerm = args.get(1);
      return new SvLibSymbolApplicationTerm(
          new SvLibIdTerm(SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY),
          ImmutableList.of(
              new SvLibSymbolApplicationTerm(
                  new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MOD, FileLocation.DUMMY),
                  ImmutableList.of(leftTerm, modulusTerm),
                  FileLocation.DUMMY),
              new SvLibSymbolApplicationTerm(
                  new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MOD, FileLocation.DUMMY),
                  ImmutableList.of(rightTerm, modulusTerm),
                  FileLocation.DUMMY)),
          FileLocation.DUMMY);
    } else {
      SvLibIdTerm functionIdTerm = functionToIdTerm(functionName, formulaType, argTypes);

      return new SvLibSymbolApplicationTerm(functionIdTerm, args, FileLocation.DUMMY);
    }
  }

  @Override
  public SvLibTerm visitQuantifier(
      BooleanFormula pBooleanFormula,
      Quantifier pQuantifier,
      List<Formula> pList,
      BooleanFormula pBooleanFormula1) {
    throw new UnsupportedOperationException(
        "The conversion of quantified formulas back into SV-LIB is not supported.");
  }
}

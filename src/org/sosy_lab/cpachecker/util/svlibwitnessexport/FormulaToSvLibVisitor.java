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
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFloatingPointConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRoundingModeConstantTerm;
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
import org.sosy_lab.java_smt.api.FloatingPointNumber;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.FunctionDeclarationKind;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
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
   * The term for an operator that none of the theories above handles, which has to be an
   * uninterpreted function.
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
   * Declare the given uninterpreted function in the scope if it is not declared yet, and return the
   * declaration to use for its applications.
   */
  private SvLibFunctionDeclaration declareUninterpretedFunction(
      String pName, SvLibType pReturnType, List<@NonNull SvLibSmtLibType> pArgTypes) {
    ImmutableList<SvLibType> argumentTypes = ImmutableList.copyOf(pArgTypes);
    scope.addFunctionDeclaration(
        new SvLibSmtFunctionDeclaration(FileLocation.DUMMY, pName, argumentTypes, pReturnType));
    return SvLibFunctionDeclaration.uninterpretedFunction(
        FileLocation.DUMMY, new SvLibFunctionType(argumentTypes, pReturnType), pName);
  }

  /**
   * The declaration of the operator of the theory of floating point numbers with the given kind.
   *
   * @return the declaration, or an empty {@link Optional} if the kind does not identify such an
   *     operator.
   */
  private static Optional<SvLibFunctionDeclaration> floatingPointDeclaration(
      FunctionDeclarationKind pKind, List<@NonNull SvLibSmtLibType> pArgTypes) {
    // The type of the operands, which for the operators that round is not the type of the first
    // argument, and for the predicates is not the return type
    Optional<SvLibSmtLibFloatingPointType> operandType =
        pArgTypes.stream()
            .filter(SvLibSmtLibFloatingPointType.class::isInstance)
            .map(SvLibSmtLibFloatingPointType.class::cast)
            .findFirst();
    if (operandType.isEmpty()) {
      return Optional.empty();
    }
    SvLibSmtLibFloatingPointType type = operandType.orElseThrow();
    return Optional.ofNullable(
        switch (pKind) {
          case FP_ADD -> SmtLibTheoryDeclarations.floatingPointAddition(type);
          case FP_SUB -> SmtLibTheoryDeclarations.floatingPointSubtraction(type);
          case FP_MUL -> SmtLibTheoryDeclarations.floatingPointMultiplication(type);
          case FP_DIV -> SmtLibTheoryDeclarations.floatingPointDivision(type);
          case FP_SQRT -> SmtLibTheoryDeclarations.floatingPointSquareRoot(type);
          case FP_ROUND_TO_INTEGRAL -> SmtLibTheoryDeclarations.floatingPointRoundToIntegral(type);
          case FP_REM -> SmtLibTheoryDeclarations.floatingPointRemainder(type);
          case FP_NEG -> SmtLibTheoryDeclarations.floatingPointNegation(type);
          case FP_ABS -> SmtLibTheoryDeclarations.floatingPointAbsolute(type);
          case FP_MAX -> SmtLibTheoryDeclarations.floatingPointMaximum(type);
          case FP_MIN -> SmtLibTheoryDeclarations.floatingPointMinimum(type);
          case FP_LT -> SmtLibTheoryDeclarations.floatingPointLessThan(type);
          case FP_LE -> SmtLibTheoryDeclarations.floatingPointLessEqual(type);
          case FP_GT -> SmtLibTheoryDeclarations.floatingPointGreaterThan(type);
          case FP_GE -> SmtLibTheoryDeclarations.floatingPointGreaterEqual(type);
          case FP_EQ -> SmtLibTheoryDeclarations.floatingPointEquality(type);
          case FP_IS_NAN -> SmtLibTheoryDeclarations.floatingPointIsNaN(type);
          case FP_IS_INF -> SmtLibTheoryDeclarations.floatingPointIsInfinite(type);
          case FP_IS_ZERO -> SmtLibTheoryDeclarations.floatingPointIsZero(type);
          case FP_IS_NEGATIVE -> SmtLibTheoryDeclarations.floatingPointIsNegative(type);
          case FP_IS_SUBNORMAL -> SmtLibTheoryDeclarations.floatingPointIsSubnormal(type);
          case FP_IS_NORMAL -> SmtLibTheoryDeclarations.floatingPointIsNormal(type);
          default -> null;
        });
  }

  private SvLibIdTerm functionToIdTerm(
      String pName,
      FunctionDeclarationKind pKind,
      SvLibType pReturnType,
      List<@NonNull SvLibSmtLibType> pArgTypes) {

    String actualName =
        canonicalNameOfBitvectorOperator(pKind)
            .orElseGet(
                () ->
                    pName
                        // Remove type suffixes from overloaded operators, like '_int'
                        .replace("_int", "")
                        .replace("_rat", "")
                        .replaceAll("_T" + Pattern.quote("(") + "[0-9]+" + Pattern.quote(")"), ""));

    if (pKind == FunctionDeclarationKind.BV_SCASTTO_FP
        || pKind == FunctionDeclarationKind.BV_UCASTTO_FP
        || pKind == FunctionDeclarationKind.FP_CASTTO_FP) {
      Verify.verify(pArgTypes.size() == 2, "A conversion into a floating point number rounds");
      Verify.verify(pReturnType instanceof SvLibSmtLibFloatingPointType);
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.toFloatingPoint(
              pKind == FunctionDeclarationKind.BV_UCASTTO_FP,
              pArgTypes.get(1),
              (SvLibSmtLibFloatingPointType) pReturnType),
          FileLocation.DUMMY);
    }

    if (pKind == FunctionDeclarationKind.FP_CASTTO_SBV
        || pKind == FunctionDeclarationKind.FP_CASTTO_UBV) {
      Verify.verify(pArgTypes.size() == 2, "A conversion of a floating point number rounds");
      Verify.verify(pArgTypes.get(1) instanceof SvLibSmtLibFloatingPointType);
      Verify.verify(pReturnType instanceof SvLibSmtLibBitVectorType);
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.floatingPointToBitVector(
              pKind == FunctionDeclarationKind.FP_CASTTO_SBV,
              (SvLibSmtLibFloatingPointType) pArgTypes.get(1),
              (SvLibSmtLibBitVectorType) pReturnType),
          FileLocation.DUMMY);
    }

    if (pKind == FunctionDeclarationKind.SBV_TO_INT
        || pKind == FunctionDeclarationKind.UBV_TO_INT) {
      Verify.verify(pArgTypes.size() == 1);
      Verify.verify(pArgTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.bitVectorToInt(
              pKind == FunctionDeclarationKind.SBV_TO_INT,
              ((SvLibSmtLibBitVectorType) pArgTypes.getFirst()).getSize()),
          FileLocation.DUMMY);
    }

    if (pKind == FunctionDeclarationKind.INT_TO_BV) {
      Verify.verify(pArgTypes.size() == 1);
      Verify.verify(pReturnType instanceof SvLibSmtLibBitVectorType);
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.intToBitVector(
              ((SvLibSmtLibBitVectorType) pReturnType).getSize()),
          FileLocation.DUMMY);
    }

    if (pKind == FunctionDeclarationKind.FP_AS_IEEEBV) {
      Verify.verify(pArgTypes.size() == 1);
      Verify.verify(pArgTypes.getFirst() instanceof SvLibSmtLibFloatingPointType);
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.floatingPointAsBitVector(
              (SvLibSmtLibFloatingPointType) pArgTypes.getFirst()),
          FileLocation.DUMMY);
    }

    // Not every solver reports the conversion of the bits of the representation of IEEE 754 with
    // the kind for it, so an application of one bitvector that has as many bits as the floating
    // point number it returns is recognized as such a conversion as well.
    if (pKind == FunctionDeclarationKind.FP_FROM_IEEEBV
        || (pArgTypes.size() == 1
            && pReturnType instanceof SvLibSmtLibFloatingPointType floatingPointType
            && pArgTypes.getFirst() instanceof SvLibSmtLibBitVectorType bitVectorType
            && bitVectorType.getSize()
                == floatingPointType.getExponentSize() + floatingPointType.getSignificandSize())) {
      Verify.verify(pArgTypes.size() == 1);
      Verify.verify(pArgTypes.getFirst() instanceof SvLibSmtLibBitVectorType);
      Verify.verify(pReturnType instanceof SvLibSmtLibFloatingPointType);
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.floatingPointFromBitVector(
              (SvLibSmtLibBitVectorType) pArgTypes.getFirst(),
              (SvLibSmtLibFloatingPointType) pReturnType),
          FileLocation.DUMMY);
    }

    if (pKind == FunctionDeclarationKind.EQ
        && pArgTypes.size() == 2
        && pArgTypes.getFirst() instanceof SvLibSmtLibFloatingPointType floatingPointType) {
      // The equality of the core theory of SMT-LIB is generic over the type of its arguments, and
      // for floating point numbers it is not the same as fp.eq, which does not distinguish the two
      // zeros and is false for NaN.
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.floatingPointIdentity(floatingPointType), FileLocation.DUMMY);
    }

    Optional<SvLibFunctionDeclaration> floatingPointDeclaration =
        floatingPointDeclaration(pKind, pArgTypes);
    if (floatingPointDeclaration.isPresent()) {
      return new SvLibIdTerm(floatingPointDeclaration.orElseThrow(), FileLocation.DUMMY);
    }

    // To ensure that ITE is always detected, it must be processed before the other Boolean
    // operators; otherwise, ITE with only Boolean parameters will cause an exception.
    // The declaration kind is used instead of the name because every solver names this
    // operator differently ("if" for Z3, "ite" for SMTInterpol, "ite_int" for MathSAT5).
    if (pArgTypes.size() == 3
        && pArgTypes.getFirst().equals(SvLibSmtLibPredefinedType.BOOL)
        && pArgTypes.get(1).equals(pArgTypes.get(2))
        && pKind == FunctionDeclarationKind.ITE) {
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
        // MathSAT5 calls the equality of two Boolean values "iff".
        case "iff", "=" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.BOOL_EQUALITY, FileLocation.DUMMY);
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
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
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
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
        case "*" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MULTIPLICATION, FileLocation.DUMMY);
        case "/", "div" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_DIV, FileLocation.DUMMY);
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
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
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.INT
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.REAL))) {
      return switch (actualName) {
        case "floor" -> new SvLibIdTerm(SmtLibTheoryDeclarations.REAL_TO_INT, FileLocation.DUMMY);
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
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
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
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
                bitVectorExtractDeclaration(pName, argTypeSize, returnTypeSize),
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
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
      };
    } else if (pReturnType instanceof SvLibSmtLibBitVectorType bitVector
        && pArgTypes.size() == 2
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type instanceof SvLibSmtLibBitVectorType)) {
      int size = bitVector.getSize();
      return switch (actualName) {
        case "concat" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorConcat(
                    ((SvLibSmtLibBitVectorType) pArgTypes.getFirst()).getSize(),
                    ((SvLibSmtLibBitVectorType) pArgTypes.get(1)).getSize()),
                FileLocation.DUMMY);
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
        case "bvashr" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.bitVectorArithmeticShiftRight(size), FileLocation.DUMMY);
        case "bvxor" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorXor(size), FileLocation.DUMMY);
        case "bvnand" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorNand(size), FileLocation.DUMMY);
        case "bvxnor" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.bitVectorXnor(size), FileLocation.DUMMY);
        default -> uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
      };
    }

    return uninterpretedFunctionOrUnsupported(pName, pKind, pReturnType, pArgTypes);
  }

  public static String nameWithoutSSA(String pName) {
    return pName.replaceAll("@$", "").replaceAll("@[0-9]+", "");
  }

  @Override
  public SvLibTerm visitFreeVariable(Formula pFormula, String pS) {
    SvLibSimpleDeclaration variableDeclaration =
        scope.getVariableForQualifiedName(nameWithoutSSA(pS)).toSimpleDeclaration();
    return new SvLibIdTerm(variableDeclaration, FileLocation.DUMMY);
  }

  @Override
  public SvLibTerm visitConstant(Formula pFormula, Object pO) {
    return switch (pO) {
      case Boolean pBoolean -> new SvLibBooleanConstantTerm(pBoolean, FileLocation.DUMMY);
      case BigInteger pInteger when pFormula instanceof IntegerFormula ->
          new SvLibIntegerConstantTerm(pInteger, FileLocation.DUMMY);
      case BigInteger pInteger when pFormula instanceof BitvectorFormula -> {
        FormulaType<?> formulaType = fmgr.getFormulaType(pFormula);
        Verify.verify(
            formulaType instanceof FormulaType.BitvectorType,
            "Obtained a bitvector formula which does not have the bitvector type");
        yield new SvLibBitVectorConstantTerm(
            pInteger, ((FormulaType.BitvectorType) formulaType).getSize(), FileLocation.DUMMY);
      }
      // The solvers report a rational constant whose value happens to be integral as a BigInteger
      // and not as a Rational.
      case BigInteger pInteger when pFormula instanceof RationalFormula ->
          new SvLibRealConstantTerm(Rational.ofBigInteger(pInteger), FileLocation.DUMMY);
      case FloatingPointNumber pFloatingPointNumber ->
          new SvLibFloatingPointConstantTerm(pFloatingPointNumber, FileLocation.DUMMY);
      case FloatingPointRoundingMode pRoundingMode ->
          new SvLibRoundingModeConstantTerm(pRoundingMode, FileLocation.DUMMY);
      case Rational pRational -> new SvLibRealConstantTerm(pRational, FileLocation.DUMMY);
      default ->
          throw new UnsupportedOperationException(
              "Unsupported constant "
                  + pO
                  + " of type "
                  + fmgr.getFormulaType(pFormula)
                  + " when transforming a formula to SV-LIB.");
    };
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
    } else if (pFunctionDeclaration.getKind() == FunctionDeclarationKind.BV_MUL
        && getFactorThatIsNegated(args).isPresent()) {
      // A multiplication with -1, which is how the solvers build the negation of a bitvector, is
      // written as a negation: the multiplication makes the solver that reads the program build a
      // multiplier for it, which it can be much slower at solving.
      SvLibTerm negatedFactor = getFactorThatIsNegated(args).orElseThrow();
      return new SvLibSymbolApplicationTerm(
          functionToIdTerm(
              "bvneg",
              FunctionDeclarationKind.BV_NEG,
              formulaType,
              ImmutableList.of((SvLibSmtLibType) negatedFactor.getExpressionType())),
          ImmutableList.of(negatedFactor),
          FileLocation.DUMMY);
    } else {
      SvLibIdTerm functionIdTerm =
          functionToIdTerm(functionName, pFunctionDeclaration.getKind(), formulaType, argTypes);

      return new SvLibSymbolApplicationTerm(
          functionIdTerm, withIntegersAsReals(functionIdTerm, args), FileLocation.DUMMY);
    }
  }

  /**
   * The arguments with every integer that the function takes as a real converted with {@code
   * to_real}, since SMT-LIB does not convert them implicitly, unlike the solvers.
   */
  private static ImmutableList<SvLibTerm> withIntegersAsReals(
      SvLibIdTerm pFunction, List<SvLibTerm> pArgs) {
    if (!(pFunction.getExpressionType() instanceof SvLibFunctionType functionType)
        || functionType.getInputTypes().size() != pArgs.size()) {
      return ImmutableList.copyOf(pArgs);
    }
    ImmutableList.Builder<SvLibTerm> args = ImmutableList.builder();
    for (int i = 0; i < pArgs.size(); i++) {
      SvLibTerm arg = pArgs.get(i);
      if (functionType.getInputTypes().get(i).equals(SvLibSmtLibPredefinedType.REAL)
          && arg.getExpressionType().equals(SvLibSmtLibPredefinedType.INT)) {
        arg =
            new SvLibSymbolApplicationTerm(
                new SvLibIdTerm(SmtLibTheoryDeclarations.INT_TO_REAL, FileLocation.DUMMY),
                ImmutableList.of(arg),
                FileLocation.DUMMY);
      }
      args.add(arg);
    }
    return args.build();
  }

  /**
   * The factor of the given multiplication that is negated by it, if the other factor is the
   * bitvector in which every bit is set, which is the representation of -1.
   */
  private static Optional<SvLibTerm> getFactorThatIsNegated(List<SvLibTerm> pFactors) {
    if (pFactors.size() != 2) {
      return Optional.empty();
    }
    for (int index = 0; index < 2; index++) {
      if (pFactors.get(index) instanceof SvLibBitVectorConstantTerm constant
          && constant
              .getValue()
              .equals(BigInteger.TWO.pow(constant.getSize()).subtract(BigInteger.ONE))) {
        return Optional.of(pFactors.get(1 - index));
      }
    }
    return Optional.empty();
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

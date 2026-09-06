// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.svlibtoformula.SvLibToSmtConverterUtils.cleanVariableNameForJavaSMT;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBitVectorConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFloatingPointConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibGeneralSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRoundingModeConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibAtTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibRelationalTerm;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibBitVectorType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibFloatingPointType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BitvectorFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FloatingPointFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FloatingPointRoundingMode;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class SvLibTermToFormulaConverter {

  /**
   * Compute by how many bits a zero_extend or sign_extend application extends its argument, based
   * on the difference between the size of the resulting bitvector and the size of the argument.
   */
  private static int getExtensionBits(
      SvLibGeneralSymbolApplicationTerm pTerm,
      BitvectorFormula pArgument,
      BitvectorFormulaManagerView pBvmgr) {
    Verify.verify(pTerm.getExpressionType() instanceof SvLibSmtLibBitVectorType);
    int extensionBits =
        ((SvLibSmtLibBitVectorType) pTerm.getExpressionType()).getSize()
            - pBvmgr.getLength(pArgument);
    Verify.verify(extensionBits >= 0);
    return extensionBits;
  }

  /** Create a constant array of the given type mapping every index to the default element. */
  @SuppressWarnings({"unchecked", "rawtypes"})
  private static @NonNull Formula makeConstArray(
      SvLibSmtLibArrayType pArrayType, Formula pDefaultElement, FormulaManagerView pFmgr) {
    return pFmgr
        .getArrayFormulaManager()
        .makeArray(
            (FormulaType) pArrayType.getKeysType().toFormulaType(),
            (FormulaType) pArrayType.getValuesType().toFormulaType(),
            pDefaultElement);
  }

  public static @NonNull Formula convertTerm(
      SvLibRelationalTerm pSvLibRelationalTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      // TODO: This is very ugly, but I don't see an easy way around it at the moment
      SvLibToFormulaConverter pConverter) {
    return switch (pSvLibRelationalTerm) {
      case SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm ->
          convertApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
      case SvLibConstantTerm pSvLibConstantTerm -> convertConstant(pSvLibConstantTerm, fmgr);
      case SvLibIdTerm pSvLibIdTerm -> convertVariable(pSvLibIdTerm, ssa, fmgr, pConverter);
      case SvLibAtTerm pSvLibAtTerm ->
          throw new UnsupportedOperationException("Not yet implemented");
      // TODO: remove once we have modules such that we can seal the classes
      default -> throw new IllegalStateException("Unexpected value: " + pSvLibRelationalTerm);
    };
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there is none, it
   * creates one with the value 1.
   *
   * @return the index of the variable
   */
  protected static int getIndex(
      String name, SvLibType type, SSAMapBuilder ssa, SvLibToFormulaConverter pConverter) {
    Type existingType = ssa.getType(name);
    if (existingType != null && !type.equals(existingType)) {
      throw new IllegalArgumentException(
          "Variable " + name + " has conflicting types: " + ssa.getType(name) + " and " + type);
    }
    return pConverter.getExistingOrNewIndex(name, type, ssa);
  }

  private static @NonNull Formula convertConstant(
      SvLibConstantTerm pSvLibConstantTerm, FormulaManagerView fmgr) {
    return switch (pSvLibConstantTerm) {
      case SvLibIntegerConstantTerm pSvLibIntegerConstantTerm ->
          fmgr.getIntegerFormulaManager().makeNumber(pSvLibIntegerConstantTerm.getValue());
      case SvLibBooleanConstantTerm pSvLibBooleanConstantTerm ->
          fmgr.getBooleanFormulaManager().makeBoolean(pSvLibBooleanConstantTerm.getValue());
      case SvLibRealConstantTerm pSvLibRealConstantTerm ->
          fmgr.getRationalFormulaManager().makeNumber(pSvLibRealConstantTerm.getValue());
      case SvLibBitVectorConstantTerm pSvLibBitVectorConstantTerm ->
          fmgr.getBitvectorFormulaManager()
              .makeBitvector(
                  pSvLibBitVectorConstantTerm.getSize(), pSvLibBitVectorConstantTerm.getValue());
      case SvLibFloatingPointConstantTerm pSvLibFloatingPointConstantTerm ->
          fmgr.getFloatingPointFormulaManager()
              .makeNumber(pSvLibFloatingPointConstantTerm.getValue());
      case SvLibRoundingModeConstantTerm pSvLibRoundingModeConstantTerm ->
          fmgr.getFloatingPointFormulaManager()
              .makeRoundingMode(pSvLibRoundingModeConstantTerm.getValue());
    };
  }

  private static @NonNull Formula convertVariable(
      SvLibIdTerm pSvLibIdTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    SvLibSimpleDeclaration variable = pSvLibIdTerm.getDeclaration();
    String varName = cleanVariableNameForJavaSMT(variable.getQualifiedName());
    int useIndex = getIndex(varName, variable.getType(), ssa, pConverter);
    return fmgr.makeVariable(
        ((SvLibSmtLibType) pSvLibIdTerm.getExpressionType()).toFormulaType(), varName, useIndex);
  }

  private static @NonNull Formula convertApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    // A declared function is uninterpreted, so nothing is known about it. This has to be checked
    // before the theories, because such a function can have the same argument types as an operator
    // of a theory.
    if (pConverter.isUninterpretedFunction(
        pSvLibGeneralSymbolApplicationTerm.getSymbol().getName())) {
      return pConverter.makeUninterpretedFunctionApplication(
          pSvLibGeneralSymbolApplicationTerm.getSymbol().getName(),
          ((SvLibSmtLibType) pSvLibGeneralSymbolApplicationTerm.getExpressionType())
              .toFormulaType(),
          transformedImmutableListCopy(
              pSvLibGeneralSymbolApplicationTerm.getTerms(),
              term -> convertTerm(term, ssa, fmgr, pConverter)));
    }

    if (pSvLibGeneralSymbolApplicationTerm instanceof SvLibSymbolApplicationTerm pTerm
        && isArrayAccess(pTerm)) {
      return convertArrayAccess(pTerm, ssa, fmgr, pConverter);
    } else if (pSvLibGeneralSymbolApplicationTerm instanceof SvLibSymbolApplicationTerm pTerm
        && pTerm.getSymbol().getName().equals("=")
        && pTerm.getTerms().size() == 2
        && pTerm.getTerms().getFirst().getExpressionType() instanceof SvLibSmtLibArrayType) {
      Formula lhs = convertTerm(pTerm.getTerms().getFirst(), ssa, fmgr, pConverter);
      Formula rhs = convertTerm(pTerm.getTerms().get(1), ssa, fmgr, pConverter);
      return makeEqualOfCoreTheory(lhs, rhs, fmgr);
    } else if (pSvLibGeneralSymbolApplicationTerm instanceof SvLibSymbolApplicationTerm pTerm
        && pTerm.getSymbol().getName().equals("distinct")) {
      // distinct is the negation of equality and is generic over the argument type, so we handle it
      // here for all types (booleans, bitvectors, arrays, ...) at once. All arguments must be
      // pairwise different, which for the common binary case is simply the negation of equality.
      List<? extends SvLibRelationalTerm> terms = pTerm.getTerms();
      Verify.verify(terms.size() >= 2);
      BooleanFormula allDistinct = fmgr.getBooleanFormulaManager().makeTrue();
      for (int i = 0; i < terms.size(); i++) {
        for (int j = i + 1; j < terms.size(); j++) {
          Formula lhs = convertTerm(terms.get(i), ssa, fmgr, pConverter);
          Formula rhs = convertTerm(terms.get(j), ssa, fmgr, pConverter);
          allDistinct =
              fmgr.makeAnd(allDistinct, fmgr.makeNot(makeEqualOfCoreTheory(lhs, rhs, fmgr)));
        }
      }
      return allDistinct;
    } else if (pSvLibGeneralSymbolApplicationTerm instanceof SvLibSymbolApplicationTerm pTerm
        && pTerm.getSymbol().getName().equals("const")
        && pTerm.getTerms().size() == 1
        && pTerm.getExpressionType() instanceof SvLibSmtLibArrayType arrayType) {
      return makeConstArray(
          arrayType, convertTerm(pTerm.getTerms().getFirst(), ssa, fmgr, pConverter), fmgr);
    } else if (pSvLibGeneralSymbolApplicationTerm instanceof SvLibSymbolApplicationTerm pTerm
        && pTerm.getSymbol().getName().equals("ite")
        && pTerm.getTerms().size() == 3
        // canBeCastTo instead of equals?
        && pTerm.getTerms().getFirst().getExpressionType().equals(SvLibSmtLibPredefinedType.BOOL)
        && pTerm
            .getTerms()
            .get(1)
            .getExpressionType()
            .equals(pTerm.getTerms().get(2).getExpressionType())) {
      // The symbol has to be checked, because otherwise every application of a symbol to three
      // arguments with a Boolean first argument would be treated as an if-then-else, in
      // particular a conjunction or a disjunction of three Boolean terms.
      return convertIteApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    } else if (isConversionBetweenTheories(pSvLibGeneralSymbolApplicationTerm)) {
      // A conversion has to be handled before the theories, because its argument and its result
      // belong to different ones.
      return convertBetweenTheories(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    } else if (isFloatingPointConversion(pSvLibGeneralSymbolApplicationTerm)) {
      return convertFloatingPointConversion(
          pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    } else if (FluentIterable.from(pSvLibGeneralSymbolApplicationTerm.getTerms())
        .transform(SvLibRelationalTerm::getExpressionType)
        .allMatch(type -> SvLibType.canBeCastTo(type, SvLibSmtLibPredefinedType.INT))) {
      return convertIntegerApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    } else if (FluentIterable.from(pSvLibGeneralSymbolApplicationTerm.getTerms())
        .transform(SvLibRelationalTerm::getExpressionType)
        .allMatch(type -> SvLibType.canBeCastTo(type, SvLibSmtLibPredefinedType.BOOL))) {
      return convertBooleanApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    } else if (FluentIterable.from(pSvLibGeneralSymbolApplicationTerm.getTerms())
        .transform(SvLibRelationalTerm::getExpressionType)
        .allMatch(type -> type instanceof SvLibSmtLibBitVectorType)) {
      return convertBitvectorApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    } else if (pSvLibGeneralSymbolApplicationTerm.getSymbol().getName().equals("=")
        && pSvLibGeneralSymbolApplicationTerm.getTerms().size() == 2
        && pSvLibGeneralSymbolApplicationTerm.getTerms().getFirst().getExpressionType()
            instanceof SvLibSmtLibFloatingPointType) {
      // The equality of the core theory of SMT-LIB is generic over the type of its arguments.
      List<? extends SvLibRelationalTerm> terms = pSvLibGeneralSymbolApplicationTerm.getTerms();
      return makeEqualOfCoreTheory(
          convertTerm(terms.getFirst(), ssa, fmgr, pConverter),
          convertTerm(terms.get(1), ssa, fmgr, pConverter),
          fmgr);
    } else if (pSvLibGeneralSymbolApplicationTerm.getSymbol().getName().startsWith("fp.")) {
      return convertFloatingPointApplication(
          pSvLibGeneralSymbolApplicationTerm, ssa, fmgr, pConverter);
    }

    throw new UnsupportedOperationException(
        "Conversion of application term not supported: "
            + pSvLibGeneralSymbolApplicationTerm.toASTString());
  }

  /**
   * The equality of the core theory of SMT-LIB, which holds if the two values are the same.
   *
   * <p>For floating point numbers this is not the equality of their theory: NaN is equal to itself
   * and a positive zero is different from a negative one, while {@code fp.eq} says the opposite.
   */
  static BooleanFormula makeEqualOfCoreTheory(Formula pLhs, Formula pRhs, FormulaManagerView fmgr) {
    // This is what an assignment means, i.e. that the two sides are the same value afterwards,
    // which for floating point numbers is not what their equality says.
    return fmgr.assignment(pLhs, pRhs);
  }

  private static @NonNull Formula convertIteApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    BooleanFormula conditionFormula =
        (BooleanFormula)
            convertTerm(
                pSvLibGeneralSymbolApplicationTerm.getTerms().getFirst(), ssa, fmgr, pConverter);
    Formula thenFormula =
        convertTerm(pSvLibGeneralSymbolApplicationTerm.getTerms().get(1), ssa, fmgr, pConverter);
    Formula elseFormula =
        convertTerm(pSvLibGeneralSymbolApplicationTerm.getTerms().get(2), ssa, fmgr, pConverter);

    BooleanFormulaManagerView bmgr = fmgr.getBooleanFormulaManager();
    return bmgr.ifThenElse(conditionFormula, thenFormula, elseFormula);
  }

  private static @NonNull Formula convertIntegerApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    String functionName = pSvLibGeneralSymbolApplicationTerm.getSymbol().getDeclaration().getName();
    List<IntegerFormula> args =
        transformedImmutableListCopy(
            pSvLibGeneralSymbolApplicationTerm.getTerms(),
            term -> (IntegerFormula) convertTerm(term, ssa, fmgr, pConverter));
    IntegerFormulaManagerView imgr = fmgr.getIntegerFormulaManager();
    return switch (functionName) {
      case "+" -> {
        Verify.verify(args.size() == 2);
        yield imgr.add(args.getFirst(), args.get(1));
      }
      case "-" -> {
        if (args.size() == 1) {
          yield imgr.negate(args.getFirst());
        }
        Verify.verify(args.size() == 2);
        yield imgr.subtract(args.getFirst(), args.get(1));
      }
      case "=" -> {
        Verify.verify(args.size() == 2);
        yield imgr.equal(args.getFirst(), args.get(1));
      }
      case "<" -> {
        Verify.verify(args.size() == 2);
        yield imgr.lessThan(args.getFirst(), args.get(1));
      }
      case "<=" -> {
        Verify.verify(args.size() == 2);
        yield imgr.lessOrEquals(args.getFirst(), args.get(1));
      }
      case ">" -> {
        Verify.verify(args.size() == 2);
        yield imgr.greaterThan(args.getFirst(), args.get(1));
      }
      case ">=" -> {
        Verify.verify(args.size() == 2);
        yield imgr.greaterOrEquals(args.getFirst(), args.get(1));
      }
      case "mod" -> {
        Verify.verify(args.size() == 2);
        yield imgr.modulo(args.getFirst(), args.get(1));
      }
      case "div" -> {
        Verify.verify(args.size() == 2);
        yield imgr.divide(args.getFirst(), args.get(1));
      }
      case "*" -> {
        Verify.verify(args.size() == 2);
        yield imgr.multiply(args.getFirst(), args.get(1));
      }
      default ->
          throw new IllegalStateException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from an integer term into a formula.");
    };
  }

  private static @NonNull Formula convertBooleanApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    String functionName =
        cleanVariableNameForJavaSMT(
            pSvLibGeneralSymbolApplicationTerm.getSymbol().getDeclaration().getQualifiedName());
    List<BooleanFormula> args =
        transformedImmutableListCopy(
            pSvLibGeneralSymbolApplicationTerm.getTerms(),
            term -> (BooleanFormula) convertTerm(term, ssa, fmgr, pConverter));
    BooleanFormulaManagerView bmgr = fmgr.getBooleanFormulaManager();
    return switch (functionName) {
      case "not" -> {
        Verify.verify(args.size() == 1);
        yield bmgr.not(args.getFirst());
      }
      case "and" -> {
        Verify.verify(args.size() >= 2);
        yield bmgr.and(args);
      }
      case "or" -> {
        Verify.verify(args.size() >= 2);
        yield bmgr.or(args);
      }
      case "=" -> {
        Verify.verify(args.size() == 2);
        yield bmgr.equivalence(args.getFirst(), args.get(1));
      }
      case "=>" -> {
        Verify.verify(args.size() == 2);
        yield bmgr.implication(args.getFirst(), args.get(1));
      }
      default ->
          throw new IllegalStateException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from a boolean term into a formula.");
    };
  }

  /** Pattern for the name of a conversion of an integer into a bitvector. */
  private static final Pattern INT_TO_BITVECTOR_PATTERN =
      Pattern.compile("^\\(_ int_to_bv ([0-9]+)\\)$");

  /**
   * Does the given application convert a value of one theory into a value of another one, i.e.
   * between the theory of bitvectors and the one of integers or the bits of a floating point number
   * into a bitvector?
   */
  private static boolean isConversionBetweenTheories(SvLibGeneralSymbolApplicationTerm pTerm) {
    String name = pTerm.getSymbol().getName();
    return name.equals("sbv_to_int")
        || name.equals("ubv_to_int")
        || name.equals("fp.to_ieee_bv")
        || INT_TO_BITVECTOR_PATTERN.matcher(name).matches();
  }

  /** Convert an application that converts a value of one theory into a value of another one. */
  private static @NonNull Formula convertBetweenTheories(
      SvLibGeneralSymbolApplicationTerm pTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    String name = pTerm.getSymbol().getName();
    ImmutableList<? extends SvLibRelationalTerm> terms = ImmutableList.copyOf(pTerm.getTerms());
    Verify.verify(terms.size() == 1, "A conversion between theories takes one argument");
    Formula argument = convertTerm(terms.getFirst(), ssa, fmgr, pConverter);

    if (name.equals("fp.to_ieee_bv")) {
      return fmgr.getFloatingPointFormulaManager().toIeeeBitvector((FloatingPointFormula) argument);
    }
    Matcher intToBitVector = INT_TO_BITVECTOR_PATTERN.matcher(name);
    if (intToBitVector.matches()) {
      return fmgr.getBitvectorFormulaManager()
          .makeBitvector(Integer.parseInt(intToBitVector.group(1)), (IntegerFormula) argument);
    }
    return fmgr.getBitvectorFormulaManager()
        .toIntegerFormula((BitvectorFormula) argument, name.equals("sbv_to_int"));
  }

  /** Pattern for the name of a conversion into or out of a floating point number. */
  private static final Pattern FLOATING_POINT_CONVERSION_PATTERN =
      Pattern.compile("^\\(_ (to_fp|to_fp_unsigned|fp\\.to_sbv|fp\\.to_ubv) [0-9 ]+\\)$");

  private static boolean isFloatingPointConversion(SvLibGeneralSymbolApplicationTerm pTerm) {
    return FLOATING_POINT_CONVERSION_PATTERN.matcher(pTerm.getSymbol().getName()).matches();
  }

  /**
   * Convert a conversion into or out of a floating point number. Its first argument is the rounding
   * mode, and the type of the result is part of the name of the conversion.
   */
  private static @NonNull Formula convertFloatingPointConversion(
      SvLibGeneralSymbolApplicationTerm pTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    Matcher matcher = FLOATING_POINT_CONVERSION_PATTERN.matcher(pTerm.getSymbol().getName());
    Verify.verify(matcher.matches());
    String conversion = matcher.group(1);
    ImmutableList<? extends SvLibRelationalTerm> terms = ImmutableList.copyOf(pTerm.getTerms());
    FloatingPointFormulaManagerView fpmgr = fmgr.getFloatingPointFormulaManager();
    if (terms.size() == 1) {
      // A conversion of the bits of the representation of IEEE 754 loses no information and
      // therefore takes no rounding mode.
      Verify.verify(conversion.equals("to_fp"));
      Verify.verify(pTerm.getExpressionType() instanceof SvLibSmtLibFloatingPointType);
      return fpmgr.fromIeeeBitvector(
          (BitvectorFormula) convertTerm(terms.getFirst(), ssa, fmgr, pConverter),
          (FormulaType.FloatingPointType)
              ((SvLibSmtLibType) pTerm.getExpressionType()).toFormulaType());
    }
    Verify.verify(
        terms.size() == 2 && terms.getFirst() instanceof SvLibRoundingModeConstantTerm,
        "A conversion of a floating point number takes a rounding mode and one argument");
    FloatingPointRoundingMode roundingMode =
        ((SvLibRoundingModeConstantTerm) terms.getFirst()).getValue();
    Formula argument = convertTerm(terms.get(1), ssa, fmgr, pConverter);

    if (conversion.startsWith("to_fp")) {
      Verify.verify(pTerm.getExpressionType() instanceof SvLibSmtLibFloatingPointType);
      return fpmgr.castFrom(
          argument,
          conversion.equals("to_fp"),
          (FormulaType.FloatingPointType)
              ((SvLibSmtLibType) pTerm.getExpressionType()).toFormulaType(),
          roundingMode);
    }
    return fpmgr.castTo(
        (FloatingPointFormula) argument,
        conversion.equals("fp.to_sbv"),
        ((SvLibSmtLibType) pTerm.getExpressionType()).toFormulaType(),
        roundingMode);
  }

  /** Convert an application of an operator of the theory of floating point numbers. */
  private static @NonNull Formula convertFloatingPointApplication(
      SvLibGeneralSymbolApplicationTerm pTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    String functionName = pTerm.getSymbol().getName();
    FloatingPointFormulaManagerView fpmgr = fmgr.getFloatingPointFormulaManager();

    // The operators that round take the rounding mode as their first argument.
    List<? extends SvLibRelationalTerm> terms = ImmutableList.copyOf(pTerm.getTerms());
    Optional<FloatingPointRoundingMode> roundingMode = Optional.empty();
    if (!terms.isEmpty()
        && terms.getFirst() instanceof SvLibRoundingModeConstantTerm roundingModeTerm) {
      roundingMode = Optional.of(roundingModeTerm.getValue());
      terms = terms.subList(1, terms.size());
    }
    List<FloatingPointFormula> args =
        transformedImmutableListCopy(
            terms, term -> (FloatingPointFormula) convertTerm(term, ssa, fmgr, pConverter));

    return switch (functionName) {
      case "fp.add" -> {
        Verify.verify(args.size() == 2);
        yield roundingMode.isPresent()
            ? fpmgr.add(args.getFirst(), args.get(1), roundingMode.orElseThrow())
            : fpmgr.add(args.getFirst(), args.get(1));
      }
      case "fp.sub" -> {
        Verify.verify(args.size() == 2);
        yield roundingMode.isPresent()
            ? fpmgr.subtract(args.getFirst(), args.get(1), roundingMode.orElseThrow())
            : fpmgr.subtract(args.getFirst(), args.get(1));
      }
      case "fp.mul" -> {
        Verify.verify(args.size() == 2);
        yield roundingMode.isPresent()
            ? fpmgr.multiply(args.getFirst(), args.get(1), roundingMode.orElseThrow())
            : fpmgr.multiply(args.getFirst(), args.get(1));
      }
      case "fp.div" -> {
        Verify.verify(args.size() == 2);
        yield roundingMode.isPresent()
            ? fpmgr.divide(args.getFirst(), args.get(1), roundingMode.orElseThrow())
            : fpmgr.divide(args.getFirst(), args.get(1));
      }
      case "fp.sqrt" -> {
        Verify.verify(args.size() == 1);
        yield roundingMode.isPresent()
            ? fpmgr.sqrt(args.getFirst(), roundingMode.orElseThrow())
            : fpmgr.sqrt(args.getFirst());
      }
      case "fp.roundToIntegral" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.round(args.getFirst(), roundingMode.orElseThrow());
      }
      case "fp.rem" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.remainder(args.getFirst(), args.get(1));
      }
      case "fp.max" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.max(args.getFirst(), args.get(1));
      }
      case "fp.min" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.min(args.getFirst(), args.get(1));
      }
      case "fp.neg" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.negate(args.getFirst());
      }
      case "fp.abs" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.abs(args.getFirst());
      }
      case "fp.lt" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.lessThan(args.getFirst(), args.get(1));
      }
      case "fp.leq" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.lessOrEquals(args.getFirst(), args.get(1));
      }
      case "fp.gt" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.greaterThan(args.getFirst(), args.get(1));
      }
      case "fp.geq" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.greaterOrEquals(args.getFirst(), args.get(1));
      }
      case "fp.eq" -> {
        Verify.verify(args.size() == 2);
        yield fpmgr.equalWithFPSemantics(args.getFirst(), args.get(1));
      }
      case "fp.isNaN" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.isNaN(args.getFirst());
      }
      case "fp.isInfinite" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.isInfinity(args.getFirst());
      }
      case "fp.isZero" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.isZero(args.getFirst());
      }
      case "fp.isNegative" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.isNegative(args.getFirst());
      }
      case "fp.isSubnormal" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.isSubnormal(args.getFirst());
      }
      case "fp.isNormal" -> {
        Verify.verify(args.size() == 1);
        yield fpmgr.isNormal(args.getFirst());
      }
      default ->
          throw new IllegalStateException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from a floating point term into a formula.");
    };
  }

  @SuppressWarnings("RefactorSwitch")
  private static @NonNull Formula convertBitvectorApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr,
      SvLibToFormulaConverter pConverter) {
    SvLibSimpleDeclaration symbolDeclaration =
        pSvLibGeneralSymbolApplicationTerm.getSymbol().getDeclaration();
    String functionName = cleanVariableNameForJavaSMT(symbolDeclaration.getQualifiedName());
    // The extensions are indexed identifiers, so their name contains the number of added bits,
    // e.g. "(_ zero_extend 8)". That number is derived from the type of the application below,
    // so the name only has to be reduced to the operator here.
    functionName =
        functionName.replaceAll(
            "^"
                + Pattern.quote("(_ ")
                + "(zero_extend|sign_extend) [0-9]+"
                + Pattern.quote(")")
                + "$",
            "$1");
    List<BitvectorFormula> args =
        transformedImmutableListCopy(
            pSvLibGeneralSymbolApplicationTerm.getTerms(),
            term -> (BitvectorFormula) convertTerm(term, ssa, fmgr, pConverter));
    BitvectorFormulaManagerView bvmgr = fmgr.getBitvectorFormulaManager();

    // The extract operation is an indexed identifier, whose indices are part of the name of its
    // declaration, e.g. "(_ extract 31 31)".
    if (symbolDeclaration instanceof SvLibFunctionDeclaration pFunctionDeclaration
        && Pattern.compile("\\(_ extract (\\d+) (\\d+)\\)").matcher(functionName).matches()) {
      Verify.verify(args.size() == 1);
      Optional<ImmutableList<Integer>> functionSymbolUnderscoreTerms =
          pFunctionDeclaration.getFunctionSymbolUnderscoreTerms();

      Verify.verify(functionSymbolUnderscoreTerms.isPresent());
      Verify.verify(functionSymbolUnderscoreTerms.orElseThrow().size() == 2);
      return bvmgr.extract(
          args.getFirst(),
          functionSymbolUnderscoreTerms.orElseThrow().getFirst(),
          functionSymbolUnderscoreTerms.orElseThrow().get(1));
    }

    // The repeat operation is an indexed identifier just like extract, and concatenates the
    // given amount of copies of its argument.
    if (symbolDeclaration instanceof SvLibFunctionDeclaration pFunctionDeclaration
        && Pattern.compile("\\(_ repeat (\\d+)\\)").matcher(functionName).matches()) {
      Verify.verify(args.size() == 1);
      Optional<ImmutableList<Integer>> functionSymbolUnderscoreTerms =
          pFunctionDeclaration.getFunctionSymbolUnderscoreTerms();

      Verify.verify(functionSymbolUnderscoreTerms.isPresent());
      Verify.verify(functionSymbolUnderscoreTerms.orElseThrow().size() == 1);

      int count = functionSymbolUnderscoreTerms.orElseThrow().getFirst();
      Verify.verify(count > 0);
      BitvectorFormula operand = args.getFirst();
      BitvectorFormula result = operand;
      for (int i = 1; i < count; i++) {
        result = bvmgr.concat(result, operand);
      }
      return result;
    }

    switch (functionName) {
      case "concat" -> {
        Verify.verify(args.size() == 2);
        // In SMT-LIB the first argument of concat provides the high-order bits
        return bvmgr.concat(args.getFirst(), args.get(1));
      }
      case "bvnot" -> {
        Verify.verify(args.size() == 1);
        return bvmgr.not(args.getFirst());
      }
      case "bvneg" -> {
        Verify.verify(args.size() == 1);
        return bvmgr.negate(args.getFirst());
      }
      case "bvand" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.and(args.getFirst(), args.get(1));
      }
      case "bvnand" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.not(bvmgr.and(args.getFirst(), args.get(1)));
      }
      case "bvor" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.or(args.getFirst(), args.get(1));
      }
      case "bvadd" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.add(args.getFirst(), args.get(1));
      }
      case "bvmul" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.multiply(args.getFirst(), args.get(1));
      }
      case "bvudiv" -> {
        Verify.verify(args.size() == 2);
        // TODO double check that args correct
        return bvmgr.divide(args.getFirst(), args.get(1), false);
      }
      case "bvurem" -> {
        // TODO double check that args correct
        return bvmgr.remainder(args.getFirst(), args.get(1), false);
      }
      case "bvshl" -> {
        Verify.verify(args.size() == 2);
        // In SMT-LIB (bvshl s t) shifts s to the left by t, which matches the argument order
        // of JavaSMT's shiftLeft
        return bvmgr.shiftLeft(args.getFirst(), args.get(1));
      }
      case "bvlshr" -> {
        Verify.verify(args.size() == 2);
        // In SMT-LIB (bvlshr s t) shifts s to the right by t, which matches the argument order
        // of JavaSMT's shiftRight
        return bvmgr.shiftRight(args.getFirst(), args.get(1), false);
      }

      /* not in SMT-LIB FixedSizeBitVectors but used by solvers Z3 & MathSAT */
      case "redxor" -> {
        Verify.verify(args.size() == 1);
        BitvectorFormula operand = args.getFirst();
        BitvectorFormula result = bvmgr.extract(operand, 0, 0);
        for (int i = 1; i < bvmgr.getLength(operand); i++) {
          result = bvmgr.xor(result, bvmgr.extract(operand, i, i));
        }
        return result;
      }
      case "redor" -> {
        Verify.verify(args.size() == 1);
        BitvectorFormula operand = args.getFirst();
        BitvectorFormula result = bvmgr.extract(operand, 0, 0);
        for (int i = 1; i < bvmgr.getLength(operand); i++) {
          result = bvmgr.or(result, bvmgr.extract(operand, i, i));
        }
        return result;
      }
      case "redand" -> {
        Verify.verify(args.size() == 1);
        BitvectorFormula operand = args.getFirst();
        BitvectorFormula result = bvmgr.extract(operand, 0, 0);
        for (int i = 1; i < bvmgr.getLength(operand); i++) {
          result = bvmgr.and(result, bvmgr.extract(operand, i, i));
        }
        return result;
      }
      case "bvcomp" -> {
        Verify.verify(args.size() == 2);
        return fmgr.getBooleanFormulaManager()
            .ifThenElse(
                bvmgr.equal(args.getFirst(), args.get(1)),
                bvmgr.makeBitvector(1, 1),
                bvmgr.makeBitvector(1, 0));
      }
      case "bvxor" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.xor(args.getFirst(), args.get(1));
      }
      case "bvxnor" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.not(bvmgr.xor(args.getFirst(), args.get(1)));
      }
      case "bvashr" -> {
        Verify.verify(args.size() == 2);
        // In SMT-LIB (bvashr s t) shifts s to the right by t, which matches the argument order
        // of JavaSMT's shiftRight
        return bvmgr.shiftRight(args.getFirst(), args.get(1), true);
      }
      case "=" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.equal(args.getFirst(), args.get(1));
      }
      case "bvule" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.lessOrEquals(args.getFirst(), args.get(1), false);
      }
      case "bvult" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.lessThan(args.getFirst(), args.get(1), false);
      }
      case "bvuge" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.greaterOrEquals(args.getFirst(), args.get(1), false);
      }
      case "bvugt" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.greaterThan(args.getFirst(), args.get(1), false);
      }
      case "bvsle" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.lessOrEquals(args.getFirst(), args.get(1), true);
      }
      case "bvslt" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.lessThan(args.getFirst(), args.get(1), true);
      }
      case "bvsge" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.greaterOrEquals(args.getFirst(), args.get(1), true);
      }
      case "bvsgt" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.greaterThan(args.getFirst(), args.get(1), true);
      }

      case "bvsdiv" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.divide(args.getFirst(), args.get(1), true);
      }
      case "bvsrem" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.remainder(args.getFirst(), args.get(1), true);
      }
      case "bvsub" -> {
        Verify.verify(args.size() == 2);
        return bvmgr.subtract(args.getFirst(), args.get(1));
      }
      case "zero_extend" -> {
        Verify.verify(args.size() == 1);
        return bvmgr.extend(
            args.getFirst(),
            getExtensionBits(pSvLibGeneralSymbolApplicationTerm, args.getFirst(), bvmgr),
            false);
      }
      case "sign_extend" -> {
        Verify.verify(args.size() == 1);
        return bvmgr.extend(
            args.getFirst(),
            getExtensionBits(pSvLibGeneralSymbolApplicationTerm, args.getFirst(), bvmgr),
            true);
      }

      default ->
          throw new IllegalArgumentException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from a bitvector term into a formula.");
    }
  }

  // We can do the casts safely since all terms are well-typed, which guarantees that the types
  // match.
  @SuppressWarnings("unchecked")
  private static @NonNull <T1 extends Formula, T2 extends Formula> Formula convertArrayAccess(
      SvLibSymbolApplicationTerm pTerm,
      SSAMapBuilder pSsa,
      FormulaManagerView pFmgr,
      SvLibToFormulaConverter pConverter) {
    if (pTerm.getSymbol().getName().equals("select")) {
      T1 indexFormula = (T1) convertTerm(pTerm.getTerms().get(1), pSsa, pFmgr, pConverter);
      ArrayFormula<T1, T2> arrayFormula =
          (ArrayFormula<T1, T2>) convertTerm(pTerm.getTerms().getFirst(), pSsa, pFmgr, pConverter);
      return pFmgr.getArrayFormulaManager().select(arrayFormula, indexFormula);
    } else if (pTerm.getSymbol().getName().equals("store")) {
      ArrayFormula<T1, T2> arrayFormula =
          (ArrayFormula<T1, T2>) convertTerm(pTerm.getTerms().getFirst(), pSsa, pFmgr, pConverter);
      T1 indexFormula = (T1) convertTerm(pTerm.getTerms().get(1), pSsa, pFmgr, pConverter);
      T2 valueFormula = (T2) convertTerm(pTerm.getTerms().get(2), pSsa, pFmgr, pConverter);
      return pFmgr.getArrayFormulaManager().store(arrayFormula, indexFormula, valueFormula);
    } else {
      throw new IllegalStateException(
          "Unexpected array access operation: " + pTerm.getSymbol().getName());
    }
  }

  private static boolean isArrayAccess(SvLibSymbolApplicationTerm pTerm) {
    if (pTerm.getSymbol().getName().equals("select")) {
      return pTerm.getTerms().size() == 2
          && SvLibType.canBeCastTo(
              pTerm.getTerms().getFirst().getExpressionType(),
              new SvLibSmtLibArrayType(
                  (SvLibSmtLibType) pTerm.getTerms().get(1).getExpressionType(),
                  (SvLibSmtLibType) pTerm.getExpressionType()));
    } else if (pTerm.getSymbol().getName().equals("store")) {
      return pTerm.getTerms().size() == 3
          && SvLibType.canBeCastTo(
              pTerm.getTerms().getFirst().getExpressionType(),
              new SvLibSmtLibArrayType(
                  (SvLibSmtLibType) pTerm.getTerms().get(1).getExpressionType(),
                  (SvLibSmtLibType) pTerm.getTerms().get(2).getExpressionType()))
          && SvLibType.canBeCastTo(
              pTerm.getExpressionType(), pTerm.getTerms().getFirst().getExpressionType());
    } else {
      return false;
    }
  }
}

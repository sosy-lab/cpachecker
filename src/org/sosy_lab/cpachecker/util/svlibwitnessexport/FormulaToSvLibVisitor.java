// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.math.BigInteger;
import java.util.List;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTerm;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.SvLibScope;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibSmtLibType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.ArrayFormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
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
    }

    throw new UnsupportedOperationException("Unsupported formula type: " + formulaType);
  }

  private SvLibIdTerm functionToIdTerm(
      String pName, SvLibType pReturnType, List<@NonNull SvLibSmtLibType> pArgTypes) {

    String actualName =
        pName
            // Remove type suffixes from overloaded operators, like '_int'
            .replace("_int", "")
            .replace("_rat", "")
            .replaceAll("_T" + Pattern.quote("(") + "[0-9]+" + Pattern.quote(")"), "");
    if (pReturnType == SvLibSmtLibPredefinedType.BOOL
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
        case "*" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MULTIPLICATION, FileLocation.DUMMY);
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
        && actualName.equals("read")) {
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.arraySelect(
              pArrayType.getKeysType(), pArrayType.getValuesType()),
          FileLocation.DUMMY);
    } else if (pArgTypes.size() == 3
        && pArgTypes.getFirst() instanceof SvLibSmtLibArrayType pArrayType
        && pArrayType.getKeysType().equals(pArgTypes.get(1))
        && pArrayType.getValuesType().equals(pArgTypes.get(2))
        && pReturnType.equals(pArrayType)
        && actualName.equals("write")) {
      return new SvLibIdTerm(
          SmtLibTheoryDeclarations.arrayStore(pArrayType.getKeysType(), pArrayType.getValuesType()),
          FileLocation.DUMMY);
    }

    throw new UnsupportedOperationException("Unknown formula type: " + pName);
  }

  @Override
  public SvLibTerm visitFreeVariable(Formula pFormula, String pS) {
    SvLibSimpleDeclaration variableDeclaration =
        scope.getVariableForQualifiedName(pS).toSimpleDeclaration();
    return new SvLibIdTerm(variableDeclaration, FileLocation.DUMMY);
  }

  @Override
  public SvLibTerm visitConstant(Formula pFormula, Object pO) {
    if (pO instanceof Boolean pBoolean) {
      return new SvLibBooleanConstantTerm(pBoolean, FileLocation.DUMMY);
    } else if (pO instanceof BigInteger pInteger) {
      return new SvLibIntegerConstantTerm(pInteger, FileLocation.DUMMY);
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

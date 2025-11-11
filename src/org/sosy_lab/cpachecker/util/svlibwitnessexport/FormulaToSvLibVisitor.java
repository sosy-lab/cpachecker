// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.svlibwitnessexport;

import com.google.common.collect.FluentIterable;
import java.math.BigInteger;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.rationals.Rational;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.builder.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.cfa.ast.svlib.parser.SvLibScope;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

public class FormulaToSvLibVisitor implements FormulaVisitor<SvLibFinalRelationalTerm> {

  private final FormulaManagerView fmgr;
  private final SvLibScope scope;

  public FormulaToSvLibVisitor(FormulaManagerView pFmgr, SvLibScope pScope) {
    fmgr = pFmgr;
    scope = pScope;
  }

  private SvLibType formulaTypeToSvLibType(FormulaType<?> formulaType) {
    if (formulaType.equals(FormulaType.BooleanType)) {
      return SvLibSmtLibPredefinedType.BOOL;
    } else if (formulaType.equals(FormulaType.IntegerType)) {
      return SvLibSmtLibPredefinedType.INT;
    } else if (formulaType.equals(FormulaType.RationalType)) {
      return SvLibSmtLibPredefinedType.REAL;
    }

    throw new AssertionError("Unsupported formula type: " + formulaType);
  }

  private SvLibIdTerm functionToIdTerm(
      String pName, SvLibType pReturnType, List<@NonNull SvLibType> pArgTypes) {

    String actualName =
        pName
            // Remove backticks from the name
            .replace("`", "")
            // Remove type suffixes from overloaded operators, like '_int'
            .replace("_int", "")
            .replace("_rat", "");
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
        default -> throw new AssertionError("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.BOOL
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.INT))) {
      return switch (actualName) {
        case "=" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY);
        case "<" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY);
        case "<=" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN, FileLocation.DUMMY);
        default -> throw new AssertionError("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.INT
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.INT))) {
      return switch (actualName) {
        case "+" ->
            new SvLibIdTerm(
                SmtLibTheoryDeclarations.intAddition(pArgTypes.size()), FileLocation.DUMMY);
        case "-" -> new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MINUS, FileLocation.DUMMY);
        case "*" ->
            new SvLibIdTerm(SmtLibTheoryDeclarations.INT_MULTIPLICATION, FileLocation.DUMMY);
        default -> throw new AssertionError("Unknown formula type: " + pName);
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
        default -> throw new AssertionError("Unknown formula type: " + pName);
      };
    } else if (pReturnType == SvLibSmtLibPredefinedType.INT
        && FluentIterable.from(pArgTypes)
            .allMatch(type -> type.equals(SvLibSmtLibPredefinedType.REAL))) {
      return switch (actualName) {
        case "floor" -> new SvLibIdTerm(SmtLibTheoryDeclarations.REAL_FLOOR, FileLocation.DUMMY);
        default -> throw new AssertionError("Unknown formula type: " + pName);
      };
    }

    throw new AssertionError("Unknown formula type: " + pName);
  }

  @Override
  public SvLibFinalRelationalTerm visitFreeVariable(Formula pFormula, String pS) {
    SvLibSimpleDeclaration variableDeclaration = scope.getVariableForQualifiedName(pS);
    return new SvLibIdTerm(variableDeclaration, FileLocation.DUMMY);
  }

  @Override
  public SvLibFinalRelationalTerm visitConstant(Formula pFormula, Object pO) {
    if (pO instanceof Boolean pBoolean) {
      return new SvLibBooleanConstantTerm(pBoolean, FileLocation.DUMMY);
    } else if (pO instanceof BigInteger pInteger) {
      return new SvLibIntegerConstantTerm(pInteger, FileLocation.DUMMY);
    } else if (pO instanceof Rational pRational) {
      return new SvLibRealConstantTerm(pRational, FileLocation.DUMMY);
    }
    throw new AssertionError("Unsupported constant type: " + pO);
  }

  @Override
  public SvLibFinalRelationalTerm visitFunction(
      Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {

    SvLibIdTerm functionIdTerm =
        functionToIdTerm(
            pFunctionDeclaration.getName(),
            formulaTypeToSvLibType(pFunctionDeclaration.getType()),
            pFunctionDeclaration.getArgumentTypes().stream()
                .map(this::formulaTypeToSvLibType)
                .toList());

    List<SvLibFinalRelationalTerm> args = pList.stream().map(f -> fmgr.visit(f, this)).toList();

    return new SvLibSymbolApplicationRelationalTerm(functionIdTerm, args, FileLocation.DUMMY);
  }

  @Override
  public SvLibFinalRelationalTerm visitQuantifier(
      BooleanFormula pBooleanFormula,
      Quantifier pQuantifier,
      List<Formula> pList,
      BooleanFormula pBooleanFormula1) {
    throw new AssertionError(
        "The conversion of quantified formulas back into SV-LIB is not supported.");
  }
}

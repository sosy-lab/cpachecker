// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.k3witnessexport;

import com.google.common.collect.FluentIterable;
import java.math.BigInteger;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SymbolApplicationRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.K3Scope;
import org.sosy_lab.cpachecker.cfa.ast.k3.parser.SmtLibTheoryDeclarations;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

public class FormulaToK3Visitor implements FormulaVisitor<K3RelationalTerm> {

  private final FormulaManagerView fmgr;
  private final K3Scope scope;

  public FormulaToK3Visitor(FormulaManagerView pFmgr, K3Scope pScope) {
    fmgr = pFmgr;
    scope = pScope;
  }

  private K3Type formulaTypeToK3Type(FormulaType<?> formulaType) {
    if (formulaType.equals(FormulaType.BooleanType)) {
      return K3SmtLibType.BOOL;
    } else if (formulaType.equals(FormulaType.IntegerType)) {
      return K3SmtLibType.INT;
    }

    throw new AssertionError("Unsupported formula type: " + formulaType);
  }

  private K3IdTerm functionToIdTerm(
      String pName, K3Type pReturnType, List<@NonNull K3Type> pArgTypes) {

    String actualName =
        pName
            // Remove backticks from the name
            .replace("`", "")
            // Remove type suffixes from overloaded operators, like '_int'
            .replace("_int", "");
    if (pReturnType == K3SmtLibType.BOOL
        && FluentIterable.from(pArgTypes).allMatch(type -> type.equals(K3SmtLibType.BOOL))) {
      return switch (actualName) {
        case "and" ->
            new K3IdTerm(
                SmtLibTheoryDeclarations.boolConjunction(pArgTypes.size()), FileLocation.DUMMY);
        case "or" ->
            new K3IdTerm(
                SmtLibTheoryDeclarations.boolDisjunction(pArgTypes.size()), FileLocation.DUMMY);
        case "not" -> new K3IdTerm(SmtLibTheoryDeclarations.BOOL_NEGATION, FileLocation.DUMMY);
        default -> throw new AssertionError("Unknown formula type: " + pName);
      };
    } else if (pReturnType == K3SmtLibType.BOOL
        && FluentIterable.from(pArgTypes).allMatch(type -> type.equals(K3SmtLibType.INT))) {
      return switch (actualName) {
        case "=" -> new K3IdTerm(SmtLibTheoryDeclarations.INT_EQUALITY, FileLocation.DUMMY);
        case "<" -> new K3IdTerm(SmtLibTheoryDeclarations.INT_LESS_THAN, FileLocation.DUMMY);
        case "<=" -> new K3IdTerm(SmtLibTheoryDeclarations.INT_LESS_EQUAL_THAN, FileLocation.DUMMY);
        default -> throw new AssertionError("Unknown formula type: " + pName);
      };
    }

    throw new AssertionError("Unknown formula type: " + pName);
  }

  @Override
  public K3RelationalTerm visitFreeVariable(Formula pFormula, String pS) {
    K3SimpleDeclaration variableDeclaration = scope.getVariableForQualifiedName(pS);
    return new K3IdTerm(variableDeclaration, FileLocation.DUMMY);
  }

  @Override
  public K3RelationalTerm visitConstant(Formula pFormula, Object pO) {
    if (pO instanceof Boolean pBoolean) {
      return new K3BooleanConstantTerm(pBoolean, FileLocation.DUMMY);
    } else if (pO instanceof BigInteger pInteger) {
      return new K3IntegerConstantTerm(pInteger, FileLocation.DUMMY);
    }

    throw new AssertionError("Unsupported constant type: " + pO);
  }

  @Override
  public K3RelationalTerm visitFunction(
      Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {

    K3IdTerm functionIdTerm =
        functionToIdTerm(
            pFunctionDeclaration.getName(),
            formulaTypeToK3Type(pFunctionDeclaration.getType()),
            pFunctionDeclaration.getArgumentTypes().stream()
                .map(this::formulaTypeToK3Type)
                .toList());

    List<K3RelationalTerm> args = pList.stream().map(f -> fmgr.visit(f, this)).toList();

    return new K3SymbolApplicationRelationalTerm(functionIdTerm, args, FileLocation.DUMMY);
  }

  @Override
  public K3RelationalTerm visitQuantifier(
      BooleanFormula pBooleanFormula,
      Quantifier pQuantifier,
      List<Formula> pList,
      BooleanFormula pBooleanFormula1) {
    throw new AssertionError(
        "The conversion of quantified formulas back into K3 is not supported.");
  }
}

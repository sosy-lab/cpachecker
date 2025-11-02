// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;
import static org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula.K3ToSmtConverterUtils.cleanVariableNameForJavaSMT;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3BooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3FinalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GeneralSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SmtLibType;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3Type;
import org.sosy_lab.cpachecker.util.predicates.pathformula.LanguageToSmtConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class K3TermToFormulaConverter {

  public static @NonNull Formula convertTerm(
      K3FinalRelationalTerm pK3Term, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    return switch (pK3Term) {
      case K3GeneralSymbolApplicationTerm pK3ApplicationTerm ->
          convertApplication(pK3ApplicationTerm, ssa, fmgr);
      case K3ConstantTerm pK3ConstantTerm -> convertConstant(pK3ConstantTerm, fmgr);
      case K3IdTerm pK3IdTerm -> convertVariable(pK3IdTerm, ssa, fmgr);
      case K3FinalTerm pK3FinalTerm ->
          throw new UnsupportedOperationException("Not yet implemented");
    };
  }

  private static @NonNull Formula convertConstant(
      K3ConstantTerm pK3ConstantTerm, FormulaManagerView fmgr) {
    return switch (pK3ConstantTerm) {
      case K3IntegerConstantTerm pK3IntegerConstantTerm ->
          fmgr.getIntegerFormulaManager().makeNumber(pK3IntegerConstantTerm.getValue());
      case K3BooleanConstantTerm pK3BooleanConstantTerm ->
          fmgr.getBooleanFormulaManager().makeBoolean(pK3BooleanConstantTerm.getValue());
    };
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there is none, it
   * creates one with the value 1.
   *
   * @return the index of the variable
   */
  protected static int getIndex(String name, K3Type type, SSAMapBuilder ssa) {
    K3Type existingType = (K3Type) ssa.getType(name);
    if (existingType != null && !type.equals(existingType)) {
      throw new IllegalArgumentException(
          "Variable " + name + " has conflicting types: " + ssa.getType(name) + " and " + type);
    }

    int idx = ssa.getIndex(name);
    if (idx <= 0) {
      idx = LanguageToSmtConverter.VARIABLE_UNINITIALIZED;

      // It is important to store the index in the variable here.
      // If getIndex() was called with a specific name,
      // this means that name@idx will appear in formulas.
      // Thus, we need to make sure that calls to FormulaManagerView.instantiate()
      // will also add indices for this name,
      // which it does exactly if the name is in the SSAMap.
      ssa.setIndex(name, type, idx);
    }

    return idx;
  }

  private static @NonNull Formula convertVariable(
      K3IdTerm pK3IdTerm, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    K3SimpleDeclaration variable = pK3IdTerm.getDeclaration();
    String varName = cleanVariableNameForJavaSMT(variable.getQualifiedName());
    int useIndex = getIndex(varName, variable.getType(), ssa);
    return fmgr.makeVariable(pK3IdTerm.getExpressionType().toFormulaType(), varName, useIndex);
  }

  private static @NonNull Formula convertApplication(
      K3GeneralSymbolApplicationTerm pK3ApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    if (FluentIterable.from(pK3ApplicationTerm.getTerms())
        .transform(K3FinalRelationalTerm::getExpressionType)
        .allMatch(type -> type.equals(K3SmtLibType.INT))) {
      return convertIntegerApplication(pK3ApplicationTerm, ssa, fmgr);
    } else if (FluentIterable.from(pK3ApplicationTerm.getTerms())
        .transform(K3FinalRelationalTerm::getExpressionType)
        .allMatch(type -> type.equals(K3SmtLibType.BOOL))) {
      return convertBooleanApplication(pK3ApplicationTerm, ssa, fmgr);
    }

    throw new UnsupportedOperationException(
        "Conversion of application term not supported: " + pK3ApplicationTerm);
  }

  private static @NonNull Formula convertIntegerApplication(
      K3GeneralSymbolApplicationTerm pK3ApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    String functionName = pK3ApplicationTerm.getSymbol().getDeclaration().getName();
    List<IntegerFormula> args =
        transformedImmutableListCopy(
            pK3ApplicationTerm.getTerms(), term -> (IntegerFormula) convertTerm(term, ssa, fmgr));
    IntegerFormulaManagerView imgr = fmgr.getIntegerFormulaManager();
    return switch (functionName) {
      case "+" -> {
        Verify.verify(args.size() == 2);
        yield imgr.add(args.getFirst(), args.get(1));
      }
      case "-" -> {
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
      default ->
          throw new IllegalStateException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from an integer term into a formula.");
    };
  }

  private static @NonNull Formula convertBooleanApplication(
      K3GeneralSymbolApplicationTerm pK3ApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    String functionName =
        cleanVariableNameForJavaSMT(
            pK3ApplicationTerm.getSymbol().getDeclaration().getQualifiedName());
    List<BooleanFormula> args =
        transformedImmutableListCopy(
            pK3ApplicationTerm.getTerms(), term -> (BooleanFormula) convertTerm(term, ssa, fmgr));
    BooleanFormulaManagerView bmgr = fmgr.getBooleanFormulaManager();
    switch (functionName) {
      case "not" -> {
        Verify.verify(args.size() == 1);
        return bmgr.not(args.getFirst());
      }
      default ->
          throw new IllegalStateException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from a boolean term into a formula.");
    }
  }
}

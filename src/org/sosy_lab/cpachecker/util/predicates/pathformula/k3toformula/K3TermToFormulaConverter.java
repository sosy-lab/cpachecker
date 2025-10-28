// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.k3toformula;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3ConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3GeneralSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IdTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3IntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3OldTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3RelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.k3.K3SmtLibType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class K3TermToFormulaConverter {

  public static @NonNull Formula convertTerm(
      K3RelationalTerm pK3Term, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    return switch (pK3Term) {
      case K3GeneralSymbolApplicationTerm pK3ApplicationTerm ->
          convertApplication(pK3ApplicationTerm, ssa, fmgr);
      case K3ConstantTerm pK3ConstantTerm -> convertConstant(pK3ConstantTerm, fmgr);
      case K3IdTerm pK3IdTerm -> convertVariable(pK3IdTerm, ssa, fmgr);
      case K3OldTerm pK3OldTerm -> throw new UnsupportedOperationException("Not yet implemented");
    };
  }

  private static @NonNull Formula convertConstant(
      K3ConstantTerm pK3ConstantTerm, FormulaManagerView fmgr) {
    return switch (pK3ConstantTerm) {
      case K3IntegerConstantTerm pK3IntegerConstantTerm ->
          fmgr.getIntegerFormulaManager().makeNumber(pK3IntegerConstantTerm.getValue());
    };
  }

  private static @NonNull Formula convertVariable(
      K3IdTerm pK3IdTerm, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    String varName = pK3IdTerm.getVariable().getQualifiedName();
    return fmgr.makeVariable(
        pK3IdTerm.getExpressionType().toFormulaType(), varName, ssa.getIndex(varName));
  }

  private static @NonNull Formula convertApplication(
      K3GeneralSymbolApplicationTerm pK3ApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    if (FluentIterable.from(pK3ApplicationTerm.getTerms())
        .transform(K3RelationalTerm::getExpressionType)
        .allMatch(type -> type.equals(K3SmtLibType.INT))) {
      return convertIntegerApplication(pK3ApplicationTerm, ssa, fmgr);
    } else if (FluentIterable.from(pK3ApplicationTerm.getTerms())
        .transform(K3RelationalTerm::getExpressionType)
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
    String functionName = pK3ApplicationTerm.getSymbol().getVariable().getName();
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
    String functionName = pK3ApplicationTerm.getSymbol().getVariable().getName();
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

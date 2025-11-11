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
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibBooleanConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalRelationalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibFinalTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibGeneralSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIdTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibIntegerConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibRealConstantTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtLibArrayType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSmtLibPredefinedType;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibSymbolApplicationTerm;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.LanguageToSmtConverter;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap.SSAMapBuilder;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.IntegerFormulaManagerView;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class SvLibTermToFormulaConverter {

  public static @NonNull Formula convertTerm(
      SvLibFinalRelationalTerm pSvLibFinalRelationalTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    return switch (pSvLibFinalRelationalTerm) {
      case SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm ->
          convertApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr);
      case SvLibConstantTerm pSvLibConstantTerm -> convertConstant(pSvLibConstantTerm, fmgr);
      case SvLibIdTerm pSvLibIdTerm -> convertVariable(pSvLibIdTerm, ssa, fmgr);
      case SvLibFinalTerm pSvLibFinalTerm ->
          throw new UnsupportedOperationException("Not yet implemented");
    };
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
    };
  }

  /**
   * This method returns the index of the given variable in the ssa map, if there is none, it
   * creates one with the value 1.
   *
   * @return the index of the variable
   */
  protected static int getIndex(String name, SvLibType type, SSAMapBuilder ssa) {
    SvLibType existingType = (SvLibType) ssa.getType(name);
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
      SvLibIdTerm pSvLibIdTerm, SSAMapBuilder ssa, FormulaManagerView fmgr) {
    SvLibSimpleDeclaration variable = pSvLibIdTerm.getDeclaration();
    String varName = cleanVariableNameForJavaSMT(variable.getQualifiedName());
    int useIndex = getIndex(varName, variable.getType(), ssa);
    return fmgr.makeVariable(pSvLibIdTerm.getExpressionType().toFormulaType(), varName, useIndex);
  }

  private static @NonNull Formula convertApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    if (FluentIterable.from(pSvLibGeneralSymbolApplicationTerm.getTerms())
        .transform(SvLibFinalRelationalTerm::getExpressionType)
        .allMatch(type -> SvLibType.canBeCastTo(type, SvLibSmtLibPredefinedType.INT))) {
      return convertIntegerApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr);
    } else if (FluentIterable.from(pSvLibGeneralSymbolApplicationTerm.getTerms())
        .transform(SvLibFinalRelationalTerm::getExpressionType)
        .allMatch(type -> SvLibType.canBeCastTo(type, SvLibSmtLibPredefinedType.BOOL))) {
      return convertBooleanApplication(pSvLibGeneralSymbolApplicationTerm, ssa, fmgr);
    } else if (pSvLibGeneralSymbolApplicationTerm instanceof SvLibSymbolApplicationTerm pTerm
        && isArrayAccess(pTerm)) {
      return convertArrayAccess(pTerm, ssa, fmgr);
    }

    throw new UnsupportedOperationException(
        "Conversion of application term not supported: "
            + pSvLibGeneralSymbolApplicationTerm.toASTString());
  }

  private static @NonNull Formula convertIntegerApplication(
      SvLibGeneralSymbolApplicationTerm pSvLibGeneralSymbolApplicationTerm,
      SSAMapBuilder ssa,
      FormulaManagerView fmgr) {
    String functionName = pSvLibGeneralSymbolApplicationTerm.getSymbol().getDeclaration().getName();
    List<IntegerFormula> args =
        transformedImmutableListCopy(
            pSvLibGeneralSymbolApplicationTerm.getTerms(),
            term -> (IntegerFormula) convertTerm(term, ssa, fmgr));
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
      case "mod" -> {
        Verify.verify(args.size() == 2);
        yield imgr.modulo(args.getFirst(), args.get(1));
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
      FormulaManagerView fmgr) {
    String functionName =
        cleanVariableNameForJavaSMT(
            pSvLibGeneralSymbolApplicationTerm.getSymbol().getDeclaration().getQualifiedName());
    List<BooleanFormula> args =
        transformedImmutableListCopy(
            pSvLibGeneralSymbolApplicationTerm.getTerms(),
            term -> (BooleanFormula) convertTerm(term, ssa, fmgr));
    BooleanFormulaManagerView bmgr = fmgr.getBooleanFormulaManager();
    switch (functionName) {
      case "not" -> {
        Verify.verify(args.size() == 1);
        return bmgr.not(args.getFirst());
      }
      case "and" -> {
        Verify.verify(args.size() >= 2);
        return bmgr.and(args);
      }
      case "or" -> {
        Verify.verify(args.size() >= 2);
        return bmgr.or(args);
      }
      default ->
          throw new IllegalStateException(
              "Unexpected value: '"
                  + functionName
                  + "' when converting from a boolean term into a formula.");
    }
  }

  private static @NonNull <T1 extends Formula, T2 extends Formula> Formula convertArrayAccess(
      SvLibSymbolApplicationTerm pTerm, SSAMapBuilder pSsa, FormulaManagerView pFmgr) {
    if (pTerm.getSymbol().getName().equals("select")) {
      T1 indexFormula = (T1) convertTerm(pTerm.getTerms().get(1), pSsa, pFmgr);
      ArrayFormula<T1, T2> arrayFormula =
          (ArrayFormula<T1, T2>) convertTerm(pTerm.getTerms().getFirst(), pSsa, pFmgr);
      return pFmgr.getArrayFormulaManager().select(arrayFormula, indexFormula);
    } else if (pTerm.getSymbol().getName().equals("store")) {
      ArrayFormula<T1, T2> arrayFormula =
          (ArrayFormula<T1, T2>) convertTerm(pTerm.getTerms().getFirst(), pSsa, pFmgr);
      T1 indexFormula = (T1) convertTerm(pTerm.getTerms().get(1), pSsa, pFmgr);
      T2 valueFormula = (T2) convertTerm(pTerm.getTerms().get(2), pSsa, pFmgr);
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
                  pTerm.getTerms().get(1).getExpressionType(), pTerm.getExpressionType()));
    } else if (pTerm.getSymbol().getName().equals("store")) {
      return pTerm.getTerms().size() == 3
          && SvLibType.canBeCastTo(
              pTerm.getTerms().getFirst().getExpressionType(),
              new SvLibSmtLibArrayType(
                  pTerm.getTerms().get(1).getExpressionType(),
                  pTerm.getTerms().get(2).getExpressionType()))
          && SvLibType.canBeCastTo(
              pTerm.getExpressionType(), pTerm.getTerms().getFirst().getExpressionType());
    } else {
      return false;
    }
  }
}

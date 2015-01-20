/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.FormulaCreator;
import org.sosy_lab.cpachecker.util.predicates.matching.AbstractSmtAstMatcher;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatchResult;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstMatchResultImpl;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPattern;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtFunctionApplicationPattern;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtQuantificationPattern;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtQuantificationPattern.QuantifierType;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

class Z3AstMatcher extends AbstractSmtAstMatcher {

  private final long ctx;
  private final Z3FormulaManager fm;
  private final FormulaCreator<Long, Long, Long> fmc;

  protected static class QuantifiedVariable {
    final FormulaType<?> variableType;
    final String nameInFormula;
    final int deBruijnIndex;

    public QuantifiedVariable(FormulaType<?> pVariableType, String pNameInFormula, int pDeBruijnIndex) {
      variableType = pVariableType;
      nameInFormula = pNameInFormula;
      deBruijnIndex = pDeBruijnIndex;
    }

    public String getDeBruijnName() {
      return "?" + deBruijnIndex;
    }
  }

  public Z3AstMatcher(FormulaManager pMgr) {
    super();

    this.fm = (Z3FormulaManager) pMgr;
    this.ctx = fm.getEnvironment();
    this.fmc = fm.getFormulaCreator();
  }

  @Override
  protected SmtAstMatchResult internalPerform(
      final Formula pParentFormula,
      final Formula pRootFormula,
      final Stack<String> pQuantifiedVariables,
      final SmtAstPattern pP, Optional<Multimap<String, Formula>> pBindingRestrictions) {

    final SmtAstMatchResultImpl result = new SmtAstMatchResultImpl();
    result.setMatchingRootFormula(pRootFormula);

    if (pP.getBindMatchTo().isPresent()) {
      final String bindMatchTo = pP.getBindMatchTo().get();
      result.putBoundVaribale(bindMatchTo, pRootFormula);
      result.putBoundVaribale(bindMatchTo + ".parent" , pParentFormula);

      if (!bindMatchTo.contains("?")) {
        if (pBindingRestrictions.isPresent()) {
          Collection<Formula> variableAlreadyBoundTo = pBindingRestrictions.get().get(bindMatchTo);
          assert variableAlreadyBoundTo.size() <= 1;
          if (variableAlreadyBoundTo.size() > 0) {
            if (!variableAlreadyBoundTo.contains(pRootFormula)) {
              return newMatchFailedResult(String.format("Binding of variable %s does not match!", bindMatchTo));
            }
          }
        }
      }
    }

    final long ast = fm.extractInfo(pRootFormula);
    final int astKind = get_ast_kind(ctx, ast);

    switch (astKind) {
    case Z3_NUMERAL_AST: // handle this as an unary function
    case Z3_VAR_AST: // handle bound variables as unary function
    case Z3_APP_AST:  // k-nary function // -------------------------------------------------
      if (!(pP instanceof SmtFunctionApplicationPattern)) {
        return newMatchFailedResult("No function application!");
      }
      SmtFunctionApplicationPattern fp = (SmtFunctionApplicationPattern) pP;

      if (fp.customFormulaMatcher.isPresent()) {
        if (!fp.customFormulaMatcher.get().formulaMatches(fm, pRootFormula)) {
          return newMatchFailedResult("Custom matcher not matched!");
        }
      }

      final String functionSymbol;
      final int functionParameterCount;

      if (astKind == Z3_VAR_AST) {
        final long index = get_index_value(ctx, ast);
        functionSymbol = "?" + index;
        functionParameterCount = 0;
      } else if (astKind == Z3_NUMERAL_AST) {
        functionSymbol = ast_to_string(ctx, ast);
        functionParameterCount = 0;
      } else {
        functionSymbol = Z3NativeApiHelpers.getDeclarationSymbolText(ctx, get_app_decl(ctx, ast));
        functionParameterCount = get_app_num_args(ctx, ast);
      }

      // No match if we are on a variable, that was not bound by a quantifier, but it is expected to be so.
      if (functionParameterCount == 0) {
        if (pP.getBindMatchTo().isPresent()) {
          final String bindMatchTo = pP.getBindMatchTo().get();
          if (bindMatchTo.startsWith(".")) {
            if (!pQuantifiedVariables.contains(functionSymbol)) {
              return newMatchFailedResult("Variable not quantified");
            }
          }
        }
      }

      final ArrayList<Formula> functionArguments = Lists.newArrayList();
      for (int i=0; i<functionParameterCount; i++) {
        final long argAst = get_app_arg(ctx, ast, i);
        final Formula argFormula = encapsulateAstAsFormula(argAst);
        functionArguments.add(argFormula);
      }

      return handleFunctionApplication(
          pRootFormula, pQuantifiedVariables, result,
          fp, functionSymbol, functionArguments, pBindingRestrictions);

    case Z3_QUANTIFIER_AST: // -------------------------------------------------
      if (!(pP instanceof SmtQuantificationPattern)) {
        return newMatchFailedResult("No function application!");
      }
      SmtQuantificationPattern qp = (SmtQuantificationPattern) pP;

      SmtQuantificationPattern.QuantifierType quantifierType
        = is_quantifier_forall(ctx, ast)
          ? QuantifierType.FORALL
          : QuantifierType.EXISTS;

      if (qp.matchQuantificationWithType.isPresent()) {
        if (!qp.matchQuantificationWithType.get().equals(quantifierType)) {
          return newMatchFailedResult("Different quantifier!");
        }
      }

      BooleanFormula bodyFormula = (BooleanFormula) encapsulateAstAsFormula(get_quantifier_body(ctx, ast));

      ArrayList<QuantifiedVariable> boundVariables = getQuantifiedVariables(ast);
      int stackElementsBeforeRecursion = pQuantifiedVariables.size();
      for (QuantifiedVariable v: boundVariables) {
        pQuantifiedVariables.push(v.getDeBruijnName());
      }
      final SmtAstMatchResult r = handleQuantification(
          pRootFormula, pQuantifiedVariables, result,
          qp, quantifierType, boundVariables, bodyFormula, pBindingRestrictions);

      while (pQuantifiedVariables.size() > stackElementsBeforeRecursion) {
        pQuantifiedVariables.pop();
      }

      return r;

    case Z3_SORT_AST:
    case Z3_FUNC_DECL_AST:
    default:
      break;
    }

    return newMatchFailedResult("Unknown structure (or elements) of formula!");
  }

  private ArrayList<QuantifiedVariable> getQuantifiedVariables(final long ast) {
    final ArrayList<QuantifiedVariable> quantifiedVariables = Lists.newArrayList();

    final int quantWeight = get_quantifier_weight(ctx, ast);
    final int boundCount = get_quantifier_num_bound(ctx, ast);

    for (int b=0; b<boundCount; b++) {
      long boundVariableSort = get_quantifier_bound_sort(ctx, ast, b);
      FormulaType<?> boundVariableType = ((Z3FormulaCreator) fmc).getFormulaTypeFromSort(boundVariableSort);
      String boundVariableName = get_symbol_string(ctx, get_quantifier_bound_name(ctx, ast, b));

      int deBruijnIndex = quantWeight-b;

      quantifiedVariables.add(new QuantifiedVariable(
          boundVariableType,
          boundVariableName,
          deBruijnIndex));
    }
    return quantifiedVariables;
  }

  private Formula encapsulateAstAsFormula(final long ast) {
    inc_ref(ctx, ast); // TODO: This should be done within 'FormulaCreator.encapsulate'
    FormulaType<?> formulaType = fmc.getFormulaType(ast);
    Formula f = fmc.encapsulate(formulaType, ast);
    return f;
  }


  private SmtAstMatchResult handleQuantification(
      final Formula pRootFormula,
      final Stack<String> pQuantifiedVariables,
      final SmtAstMatchResultImpl pResult,
      final SmtQuantificationPattern pQp,
      final QuantifierType pQuantifierType,
      final ArrayList<QuantifiedVariable> pBoundVariables,
      final BooleanFormula pBodyFormula,
      final Optional<Multimap<String, Formula>> bBindingRestrictions) {

    final List<BooleanFormula> bodyConjuncts = extractConjuncts(pBodyFormula, false);

    SmtAstMatchResult bodyMatchingResult = matchFormulaChildsInSequence(
        pRootFormula, pQuantifiedVariables,
        bodyConjuncts, pQp.quantorBodyMatchers, bBindingRestrictions, false);

    if (!bodyMatchingResult.matches() && bodyConjuncts.size() > 0) {
      bodyMatchingResult = matchFormulaChildsInSequence(
          pRootFormula, pQuantifiedVariables,
          bodyConjuncts, pQp.quantorBodyMatchers, bBindingRestrictions, true);
    }

    if (bodyMatchingResult.matches()) {
      pResult.addSubResults(bodyMatchingResult);
      return pResult;
    } else {
      // Encodes the reason for the failure
      return bodyMatchingResult;
    }
  }

  private List<BooleanFormula> extractConjuncts(BooleanFormula pFormula, boolean pRecursive) {
    List<BooleanFormula> result = Lists.newArrayList();

    final long ast = fm.extractInfo(pFormula);
    final int astKind = get_ast_kind(ctx, ast);

    if (astKind == Z3_APP_AST) {
      final long decl = get_app_decl(ctx, ast);
      final long declKind = get_decl_kind(ctx, decl);

      if (declKind == Z3_OP_AND) {
        final int args = get_app_num_args(ctx, ast);

        for (int i=0; i<args; i++) {
          final long argAst = get_app_arg(ctx, ast, i);
          final BooleanFormula argFormula = (BooleanFormula) encapsulateAstAsFormula(argAst);

          if (pRecursive) {
            List<BooleanFormula> argFormulaChildConjuncts = extractConjuncts(argFormula, pRecursive);
            result.addAll(argFormulaChildConjuncts);
          } else {
            result.add(argFormula);
          }
        }
      } else {
        result.add(pFormula);
      }
    }

    return result;
  }

  @Override
  public <T1 extends Formula, T2 extends Formula> T1 substitute(T1 pF, Map<T2, T2> pFromToMapping) {
    return fm.getUnsafeFormulaManager().substitute(pF, pFromToMapping);
  }


}

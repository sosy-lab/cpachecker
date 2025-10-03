// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

/**
 * A visitor that traverses Boolean formulas (Abstraction formulas) and extracts normalized atomic
 * expressions, enabling structural pattern matching. Each atom is represented as a
 * DelegatingRefinerNormalizedAtom, encoding a canonical S-expression for DSL rule matching. The
 * output atoms are used by the DelegatingRefinerHeuristicRedundantPredicates to identify redundant
 * predicates via DSL rule matching. The normalizer supports logical operators, bitvector functions,
 * and equality expressions and skips bound variables unless quantifiers are explicitly present.
 *
 * <p>The normalizer performs
 *
 * <ul>
 *   <li>Operator normalization: maps SMT operators such as bvadd_32 or =_T(18) to canonical forms
 *   <li>Structural decomposition: flattens nested functions
 *   <li>Operand stringification: converts constants, variables, and subterms into matchable tokens.
 * </ul>
 */
class DelegatingRefinerAtomNormalizer
    implements FormulaVisitor<ImmutableList<DelegatingRefinerNormalizedAtom>> {

  private final FormulaManagerView formulaManager;
  private final LogManager logger;

  private static final String NULL_OPERATOR = "<null>";

  private static final Map<String, String> OPERATOR_MAP =
      ImmutableMap.<String, String>builder()
          .put("bvult", "<")
          .put("bvule", "<=")
          .put("bvugt", ">")
          .put("bvuge", ">=")
          .put("not", "!")
          .put("and", "and")
          .put("or", "or")
          .put("=", "=")
          .put("==", "=")
          .put("=_T", "=")
          .buildKeepingLast();

  DelegatingRefinerAtomNormalizer(FormulaManagerView pFormulaManager, LogManager pLogger) {
    this.formulaManager = checkNotNull(pFormulaManager);
    this.logger = pLogger;
  }

  ImmutableList<DelegatingRefinerNormalizedAtom> normalizeFormula(BooleanFormula pFormula) {
    return formulaManager.visit(pFormula, this);
  }

  private String normalizeOperator(String pOperator) {
    if (pOperator == null) {
      return NULL_OPERATOR;
    }
    if (pOperator.startsWith("bvextract_")) {
      return pOperator;
    }

    // Strip surrounding backticks: `and` -> and
    if (pOperator.length() > 1
        && pOperator.charAt(0) == '`'
        && pOperator.charAt(pOperator.length() - 1) == '`') {
      pOperator = pOperator.substring(1, pOperator.length() - 1);
    }

    int parenthesisIndex = pOperator.indexOf('(');
    if (parenthesisIndex > 0) {
      pOperator = pOperator.substring(0, parenthesisIndex);
    }
    if (OPERATOR_MAP.containsKey(pOperator)) {
      return OPERATOR_MAP.get(pOperator);
    }

    int position = pOperator.length() - 1;
    boolean hasDigit = false;

    // Strip trailing digits after underscore: bvadd_32 -> bvadd
    while (position >= 0 && Character.isDigit(pOperator.charAt(position))) {
      hasDigit = true;
      position--;
    }
    if (hasDigit && position >= 0 && pOperator.charAt(position) == '_') {
      pOperator = pOperator.substring(0, position);
    }
    return pOperator;
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitFreeVariable(
      Formula pFormula, String pS) {
    return ImmutableList.of(new DelegatingRefinerNormalizedAtom(pS));
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitBoundVariable(
      Formula pFormula, int pI) {
    logger.logf(Level.FINEST, "Skipping bound variable");
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitConstant(Formula pFormula, Object pO) {
    return ImmutableList.of(new DelegatingRefinerNormalizedAtom(String.valueOf(pO)));
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitQuantifier(
      BooleanFormula pBooleanFormula,
      Quantifier pQuantifier,
      List<Formula> pList,
      BooleanFormula pBooleanFormula1) {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("(quant").append(pQuantifier.toString().toLowerCase(Locale.ROOT));
    if (pList != null) {
      for (Formula formula : pList) {
        ImmutableList<DelegatingRefinerNormalizedAtom> atoms = formulaManager.visit(formula, this);
        for (DelegatingRefinerNormalizedAtom atom : atoms) {
          stringBuilder.append(" ").append(atom.toSExpr());
        }
      }
    }
    stringBuilder.append(")");
    return ImmutableList.of(new DelegatingRefinerNormalizedAtom(stringBuilder.toString()));
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitFunction(
      Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {
    String operator = normalizeOperator(pFunctionDeclaration.getName());
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append("(").append(operator);

    if (pList != null) {
      for (Formula formula : pList) {
        ImmutableList<DelegatingRefinerNormalizedAtom> atoms = formulaManager.visit(formula, this);
        for (DelegatingRefinerNormalizedAtom atom : atoms) {
          stringBuilder.append(" ").append(atom.toSExpr());
        }
      }
    }

    stringBuilder.append(")");
    return ImmutableList.of(new DelegatingRefinerNormalizedAtom(stringBuilder.toString()));
  }
}

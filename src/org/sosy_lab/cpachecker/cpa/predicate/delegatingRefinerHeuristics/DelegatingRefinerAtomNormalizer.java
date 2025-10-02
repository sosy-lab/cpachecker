// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerHeuristics;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
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
 * expressions. Each atom is represented as a DelegatingRefinerNormalizedAtom, encoding a canonical
 * S-expression for DSL rule matching. The normalizer handles logical operators, bitvector functions
 * and equality expressions. It performs structural decomposition, operator normalization and
 * operand stringification. It is used by the DelegatingRefinerRedundantPredicates heuristic to
 * normalize abstraction formulas.
 */
class DelegatingRefinerAtomNormalizer
    implements FormulaVisitor<ImmutableList<DelegatingRefinerNormalizedAtom>> {

  private final FormulaManagerView formulaManager;
  private final LogManager logger;

  private static final String NULL_OPERATOR = "<null>";
  private static final String BOOLEAN_TRUE = "true";
  private static final String EQUALITY_OPERATOR = "=";
  private static final String CONSTANT = "const";
  private static final String FREE_VARIABLE = "var";
  private static final String BOUND_VARIABLE = "bound";
  private static final String FREE_VARIABLE_ID_OPERATOR = "id";
  private static final String BOUND_VARIABLE_ID_OPERATOR = "idx";

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

  private String normalizeOperand(Formula pFormula) {
    OperandFinder operandFinder = new OperandFinder();
    if (pFormula == null) {
      return NULL_OPERATOR;
    }

    try {

      String shortString = formulaManager.visit(pFormula, operandFinder);
      if (!Strings.isNullOrEmpty(shortString)) {
        return shortString;
      }
    } catch (ClassCastException e) {
      logger.logf(Level.FINEST, "OperandFinder failed on %s", pFormula);
    }
    return pFormula.toString();
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitFreeVariable(
      Formula pFormula, String pS) {
    return ImmutableList.of(
        new DelegatingRefinerNormalizedAtom(pS, FREE_VARIABLE_ID_OPERATOR, FREE_VARIABLE));
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitBoundVariable(
      Formula pFormula, int pI) {
    return ImmutableList.of(
        new DelegatingRefinerNormalizedAtom(
            BOUND_VARIABLE, BOUND_VARIABLE_ID_OPERATOR, String.valueOf(pI)));
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitConstant(Formula pFormula, Object pO) {
    return ImmutableList.of(
        new DelegatingRefinerNormalizedAtom(CONSTANT, EQUALITY_OPERATOR, String.valueOf(pO)));
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitQuantifier(
      BooleanFormula pBooleanFormula,
      Quantifier pQuantifier,
      List<Formula> pList,
      BooleanFormula pBooleanFormula1) {
    return ImmutableList.of();
  }

  @Override
  public ImmutableList<DelegatingRefinerNormalizedAtom> visitFunction(
      Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {

    String operator = normalizeOperator(pFunctionDeclaration.getName());
    if (operator.equals("and") || operator.equals("or")) {
      return handleAndOR(pList);
    } else if (operator.equals("!")) {
      return handleNegation(pList);
    } else if (pList != null && pList.size() == 2) {
      return handleBinary(operator, pList);
    } else {
      return handleFallBack(operator, pList);
    }
  }

  private ImmutableList<DelegatingRefinerNormalizedAtom> handleAndOR(List<Formula> pList) {
    ImmutableList.Builder<DelegatingRefinerNormalizedAtom> atomsBuilder = ImmutableList.builder();

    if (pList != null) {
      for (Formula listElement : pList) {
        if (listElement instanceof BooleanFormula pBooleanFormula) {
          atomsBuilder.addAll(formulaManager.visit(pBooleanFormula, this));
        } else {
          atomsBuilder.add(
              new DelegatingRefinerNormalizedAtom(
                  normalizeOperand(listElement), EQUALITY_OPERATOR, BOOLEAN_TRUE));
        }
      }
    }

    ImmutableList<DelegatingRefinerNormalizedAtom> atoms = atomsBuilder.build();

    ImmutableList.Builder<DelegatingRefinerNormalizedAtom> fallbackAtomsBuilder =
        ImmutableList.builder();
    for (DelegatingRefinerNormalizedAtom atom : atoms) {
      fallbackAtomsBuilder.add(atom);
    }
    return fallbackAtomsBuilder.build();
  }

  private ImmutableList<DelegatingRefinerNormalizedAtom> handleNegation(List<Formula> pList) {
    if (pList == null || pList.isEmpty()) {
      return ImmutableList.of();
    }
    ImmutableList<DelegatingRefinerNormalizedAtom> innerAtoms =
        formulaManager.visit((BooleanFormula) pList.getFirst(), this);
    ImmutableList.Builder<DelegatingRefinerNormalizedAtom> result = ImmutableList.builder();
    for (DelegatingRefinerNormalizedAtom atom : innerAtoms) {
      result.add(new DelegatingRefinerNormalizedAtom(atom.leftAtom(), "!", atom.rightAtom()));
    }
    return result.build();
  }

  private ImmutableList<DelegatingRefinerNormalizedAtom> handleBinary(
      String pOperator, List<Formula> pList) {
    String leftAtom = normalizeOperand(pList.getFirst());
    String rightAtom = normalizeOperand(pList.get(1));
    DelegatingRefinerNormalizedAtom atom =
        new DelegatingRefinerNormalizedAtom(leftAtom, pOperator, rightAtom);
    return ImmutableList.of(atom);
  }

  private ImmutableList<DelegatingRefinerNormalizedAtom> handleFallBack(
      String pOperator, List<Formula> pList) {
    ImmutableList.Builder<DelegatingRefinerNormalizedAtom> fallback = ImmutableList.builder();
    if (pList != null) {
      for (Formula listElement : pList) {
        if (listElement instanceof BooleanFormula pBooleanFormula) {
          fallback.addAll(formulaManager.visit(pBooleanFormula, this));
        } else {
          String s = normalizeOperand(listElement);
          fallback.add(new DelegatingRefinerNormalizedAtom(s, pOperator, ""));
        }
      }
    }
    return fallback.build();
  }

  private class OperandFinder implements FormulaVisitor<String> {
    @Override
    public String visitFreeVariable(Formula pFormula, String pS) {
      return pS;
    }

    @Override
    public String visitBoundVariable(Formula pFormula, int pI) {
      return "<b" + pI + ">";
    }

    @Override
    public String visitConstant(Formula pFormula, Object pO) {
      return String.valueOf(pO);
    }

    @Override
    public String visitFunction(
        Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {
      String operator = normalizeOperator(pFunctionDeclaration.getName());

      StringBuilder pStringBuilder = new StringBuilder();
      pStringBuilder.append("(").append(operator);
      if (pList != null) {
        for (Formula childFormula : pList) {
          pStringBuilder.append(" ").append(normalizeOperand(childFormula));
        }
      }

      pStringBuilder.append(")");
      return pStringBuilder.toString();
    }

    @Override
    public String visitQuantifier(
        BooleanFormula pBooleanFormula,
        Quantifier pQuantifier,
        List<Formula> pList,
        BooleanFormula pBooleanFormula1) {
      return "<quant>";
    }
  }
}

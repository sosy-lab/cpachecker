// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sosy_lab.common.collect.Collections3.elementsAndList;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

/**
 * Normalizes a Boolean formula (Abstraction formulas) into an s-expression tree for structural
 * matching. The visitor traverses the Boolean formula and produces a tree of {@link
 * DelegatingRefinerSExpression} nodes used for rule-based structural pattern matching.
 *
 * <p>The normalizer performs
 *
 * <ul>
 *   <li>Operator normalization: SMT operators names such as {@code bvadd_32} or {@code =_T(18)} are
 *       mapped to canonical forms.
 *   <li>Bit slicing: Functions such as {@code bvextract_31_31} are recognized and normalized to
 *       structured trees of {@code "bvextract"} with subatoms {@code (hi, lo, argument)}.
 *   <li>Structural decomposition: N-ary nested functions are normalized to operator nodes whose
 *       children are recursively normalized subatoms.
 *   <li>Symbol extraction: Free variables and constants are normalized to atoms with their String
 *       representation.
 *   <li>Skipping Bound Variables: Bound variables are not relevant in this context and are skipped
 *       unless a quantifier is detected.
 * </ul>
 */
public class DelegatingRefinerAtomNormalizer
    implements FormulaVisitor<DelegatingRefinerSExpression> {

  private final FormulaManagerView formulaManager;

  private static final String NULL_OPERATOR = "<null>";

  private static final Map<String, String> OPERATOR_MAP =
      ImmutableMap.<String, String>builder()
          .put("bvult", "<")
          .put("bvule", "<=")
          .put("bvugt", ">")
          .put("bvuge", ">=")
          .put("not", "not")
          .put("and", "and")
          .put("or", "or")
          .put("=", "=")
          .put("==", "=")
          .put("=_T", "=")
          .buildKeepingLast();

  public DelegatingRefinerAtomNormalizer(FormulaManagerView pFormulaManager) {
    this.formulaManager = checkNotNull(pFormulaManager);
  }

  public DelegatingRefinerSExpression buildAtom(BooleanFormula pFormula) {
    return formulaManager.visit(pFormula, this);
  }

  private String normalizeOperator(String pOperator) {
    if (pOperator == null) {
      return NULL_OPERATOR;
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

  // BVextract needs special processing to handle bit-slicing, e.g. BVextract_32_32
  private static DelegatingRefinerSExpression normalizeBitvectorIfNeeded(
      String pRawOperator, String pNormalized, ImmutableList<DelegatingRefinerSExpression> pList) {
    Optional<HiLo> hiLo = parseBVExtract(pRawOperator);

    if (hiLo.isPresent() && pList.size() == 1) {

      ImmutableList<DelegatingRefinerSExpression> expressionsList =
          elementsAndList(
              new DelegatingRefinerSExpressionAtom(hiLo.orElseThrow().hi()),
              new DelegatingRefinerSExpressionAtom(hiLo.orElseThrow().lo()),
              pList);
      return new DelegatingRefinerSExpressionSExpressionOperator("bvextract", expressionsList);
    }
    return new DelegatingRefinerSExpressionSExpressionOperator(pNormalized, pList);
  }

  private static Optional<HiLo> parseBVExtract(String pOperator) {
    if (pOperator == null) {
      return Optional.empty();
    }

    if (pOperator.length() > 1
        && pOperator.charAt(0) == '`'
        && pOperator.charAt(pOperator.length() - 1) == '`') {
      pOperator = pOperator.substring(1, pOperator.length() - 1);
    }

    if (!pOperator.startsWith("bvextract_")) {
      return Optional.empty();
    }

    String rest = pOperator.substring("bvextract_".length()); // e.g. _31_32
    int firstSlice = rest.indexOf('_');
    int secondSlice = rest.indexOf('_', firstSlice + 1);
    if (firstSlice < 0 || secondSlice < 0) {
      return Optional.empty();
    }
    String hi = rest.substring(0, firstSlice);
    String lo = rest.substring(firstSlice + 1, secondSlice);
    if (hi.isEmpty() || lo.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new HiLo(hi, lo));
  }

  @Override
  public DelegatingRefinerSExpression visitFreeVariable(Formula pFormula, String pS) {
    return new DelegatingRefinerSExpressionAtom(pS);
  }

  @SuppressWarnings("deprecation")
  @Override
  public DelegatingRefinerSExpression visitBoundVariable(Formula pFormula, int pI) {
    return null;
  }

  @Override
  public DelegatingRefinerSExpression visitConstant(Formula pFormula, Object pO) {
    return new DelegatingRefinerSExpressionAtom(String.valueOf(pO));
  }

  @Override
  public DelegatingRefinerSExpression visitQuantifier(
      BooleanFormula pBooleanFormula,
      Quantifier pQuantifier,
      List<Formula> pList,
      BooleanFormula pBooleanFormula1) {
    ImmutableList.Builder<DelegatingRefinerSExpression> subAtomsBuilder = ImmutableList.builder();
    subAtomsBuilder.add(
        new DelegatingRefinerSExpressionAtom(
            "quant" + pQuantifier.toString().toLowerCase(Locale.ROOT)));
    if (pList != null) {
      for (Formula formula : pList) {
        DelegatingRefinerSExpression subAtom = formulaManager.visit(formula, this);
        if (subAtom != null) {
          subAtomsBuilder.add(subAtom);
        }
      }

      DelegatingRefinerSExpression outerAtom = formulaManager.visit(pBooleanFormula1, this);
      if (outerAtom != null) {
        subAtomsBuilder.add(outerAtom);
      }
    }

    return new DelegatingRefinerSExpressionSExpressionOperator(
        "quantifier", subAtomsBuilder.build());
  }

  @Override
  public DelegatingRefinerSExpression visitFunction(
      Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {
    String rawOperator = pFunctionDeclaration.getName();
    String normalizedOperator = normalizeOperator(rawOperator);
    ImmutableList.Builder<DelegatingRefinerSExpression> subAtomsBuilder = ImmutableList.builder();

    if (pList != null) {
      for (Formula formula : pList) {
        DelegatingRefinerSExpression subAtom = formulaManager.visit(formula, this);
        if (subAtom != null) {
          subAtomsBuilder.add(subAtom);
        }
      }
    }
    ImmutableList<DelegatingRefinerSExpression> subAtoms = subAtomsBuilder.build();
    return normalizeBitvectorIfNeeded(rawOperator, normalizedOperator, subAtoms);
  }
}

record HiLo(String hi, String lo) {}

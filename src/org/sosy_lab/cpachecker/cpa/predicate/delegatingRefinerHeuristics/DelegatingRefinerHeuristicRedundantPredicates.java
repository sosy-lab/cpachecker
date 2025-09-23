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
import com.google.common.collect.ImmutableMultiset;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetDelta;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.QuantifiedFormulaManager.Quantifier;
import org.sosy_lab.java_smt.api.visitors.FormulaVisitor;

/**
 * A heuristic which checks for redundancy in the added predicates and returns false if they are
 * above an acceptable threshold.
 */
public class DelegatingRefinerHeuristicRedundantPredicates implements DelegatingRefinerHeuristic {
  private final double redundancyThreshold;
  private final FormulaManagerView formulaManager;
  private final LogManager logger;

  public DelegatingRefinerHeuristicRedundantPredicates(
      double acceptableRedundancyThreshold,
      FormulaManagerView pFormulaManager,
      final LogManager pLogger) {
    this.redundancyThreshold = acceptableRedundancyThreshold;
    this.formulaManager = checkNotNull(pFormulaManager);
    this.logger = pLogger;
  }

  @Override
  public boolean fulfilled(
      UnmodifiableReachedSet pReached, ImmutableList<ReachedSetDelta> pDeltas) {
    PatternNormalizer patternNormalizer = new PatternNormalizer(formulaManager);
    ImmutableMultiset.Builder<NormalizedPatternType> patternBuilder = ImmutableMultiset.builder();

    for (ReachedSetDelta delta : pDeltas) {
      for (AbstractState pState : delta.getAddedStates()) {
        PredicateAbstractState predState =
            checkNotNull(AbstractStates.extractStateByType(pState, PredicateAbstractState.class));

        if (predState.isAbstractionState()) {
          BooleanFormula abstractionFormula = predState.getAbstractionFormula().asFormula();
          NormalizedPatternType normalizedPattern =
              formulaManager.visit(abstractionFormula, patternNormalizer);
          patternBuilder.add(normalizedPattern);
        }
      }
    }

    ImmutableMultiset<NormalizedPatternType> patternSequence = patternBuilder.build();
    int totalNumberPatterns = patternSequence.size();
    if (totalNumberPatterns == 0) {
      return true;
    }

    int maxPatternCount = 0;
    NormalizedPatternType maxPatternName = null;

    for (NormalizedPatternType pattern : patternSequence.elementSet()) {
      int patternCount = patternSequence.count(pattern);

      if (patternCount > maxPatternCount) {
        maxPatternCount = patternCount;
        maxPatternName = pattern;
      }
    }

    double dominanceRate = (double) maxPatternCount / totalNumberPatterns;

    if (maxPatternName != null) {
      logger.logf(
          Level.FINEST,
          "Checking current redundancy rate in predicates: %.2f. Most redundant pattern is %s.",
          dominanceRate,
          checkNotNull(maxPatternName.describeForLogs()));
    } else {
      logger.logf(
          Level.FINEST,
          "Checking current redundancy rate in predicates: %.2f. No dominant pattern found.",
          dominanceRate);
    }

    logger.logf(
        Level.FINEST,
        "Redundancy rate in predicates is too high: %.2f. Heuristic REDUNDANT_PREDICATES is no"
            + " longer applicable.",
        dominanceRate);
    return dominanceRate <= redundancyThreshold;
  }

  public double getRedundancyThreshold() {
    return redundancyThreshold;
  }

  private enum NormalizedPatternType {
    CONST_ONLY,
    CONST_AND_VAR,
    EQ_VAR_VAR,
    NESTED_AND,
    NESTED_OR,
    NEGATION,
    BITVECTOR_OP,
    QUANTIFIED,
    OTHER;

    private String describeForLogs() {
      return switch (this) {
        case CONST_ONLY -> "only predicates with constants";
        case CONST_AND_VAR -> "predicates that mix constants and variables";
        case EQ_VAR_VAR -> "predicates with equality between variables";
        case NESTED_AND -> "predicates with nested conjunctions";
        case NESTED_OR -> "predicates with nested disjunctions";
        case NEGATION -> "Negated predicates";
        case BITVECTOR_OP -> "predicates with bitvector operations";
        case QUANTIFIED -> "Quantified formulas";
        case OTHER -> "Other, not classified predicates";
      };
    }
  }

  private static class PatternNormalizer implements FormulaVisitor<NormalizedPatternType> {
    private final FormulaManagerView formulaManager;

    private PatternNormalizer(FormulaManagerView pFormulaManager) {
      this.formulaManager = checkNotNull(pFormulaManager);
    }

    private boolean isNot(String pS) {
      return "not".equals(pS);
    }

    private boolean isEqual(String pS) {
      return "eq".equals(pS);
    }

    private boolean isOr(String pS) {
      return "or".equals(pS);
    }

    private boolean isAnd(String pS) {
      return "and".equals(pS);
    }

    private String transformOperator(FunctionDeclaration<?> pFunctionDeclaration) {
      String raw = pFunctionDeclaration.getName();
      if (raw.startsWith("`") && raw.endsWith("`") && raw.length() > 1) {
        raw = raw.substring(1, raw.length() - 1);
      }

      if (raw.startsWith("=_T")) {
        return "eq";
      }
      if (raw.startsWith("bv")) {
        return "bitvector";
      }
      int position = raw.length() - 1;
      boolean hasDigit = false;
      while (position >= 0 && Character.isDigit(raw.charAt(position))) {
        hasDigit = true;
        position--;
      }
      if (hasDigit && position >= 0 && raw.charAt(position) == '_') {
        raw = raw.substring(0, position);
      }
      return raw;
    }

    private NormalizedPatternType classifyFunction(String operand, List<Formula> pList) {
      // Classify negations and bitvector operations first
      if (isNot(operand)) {
        return NormalizedPatternType.NEGATION;
      }

      if ("bitvector".equals(operand)) {
        return NormalizedPatternType.BITVECTOR_OP;
      }

      // Count remaining patterns
      int constCount = 0;
      int varCount = 0;
      boolean nestedAnd = false;
      boolean nestedOr = false;

      for (Formula toVisit : pList) {
        NormalizedPatternType sub = formulaManager.visit(toVisit, this);

        switch (sub) {
          case CONST_ONLY -> constCount++;
          case CONST_AND_VAR -> {
            constCount++;
            varCount++;
          }
          case EQ_VAR_VAR, OTHER -> varCount++;
          case NESTED_AND -> nestedAnd = true;
          case NESTED_OR -> nestedOr = true;
          default -> {}
        }
      }

      // Classify remaining pattern based on count and operators
      if (constCount == pList.size()) {
        return NormalizedPatternType.CONST_ONLY;
      }
      if (constCount > 0 && varCount > 0) {
        return NormalizedPatternType.CONST_AND_VAR;
      }
      if (isEqual(operand) && pList.size() == 2 && constCount == 0) {
        return NormalizedPatternType.EQ_VAR_VAR;
      }
      if (isAnd(operand) && nestedAnd) {
        return NormalizedPatternType.NESTED_AND;
      }
      if (isOr(operand) && nestedOr) {
        return NormalizedPatternType.NESTED_OR;
      }
      return NormalizedPatternType.OTHER;
    }

    @Override
    public NormalizedPatternType visitFreeVariable(Formula pFormula, String pS) {
      return NormalizedPatternType.OTHER;
    }

    @Override
    public NormalizedPatternType visitBoundVariable(Formula pFormula, int pI) {
      return NormalizedPatternType.OTHER;
    }

    @Override
    public NormalizedPatternType visitConstant(Formula pFormula, Object pO) {
      return NormalizedPatternType.CONST_ONLY;
    }

    @Override
    public NormalizedPatternType visitFunction(
        Formula pFormula, List<Formula> pList, FunctionDeclaration<?> pFunctionDeclaration) {
      String operand = transformOperator(pFunctionDeclaration);
      return classifyFunction(operand, pList);
    }

    @Override
    public NormalizedPatternType visitQuantifier(
        BooleanFormula pBooleanFormula,
        Quantifier pQuantifier,
        List<Formula> pList,
        BooleanFormula pBooleanFormula1) {

      return NormalizedPatternType.QUANTIFIED;
    }
  }
}

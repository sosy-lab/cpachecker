// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.coverage.CoverageData;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateCoverageCPATransferRelation extends AbstractSingleWrapperTransferRelation {

  private final TransferRelation predicateTransferRelation;
  private Instant startTime = Instant.MIN;
  private final Map<Long, Double> timeStampsPerCoverage;
  private final FormulaManagerView fmgr;
  private final CoverageData cov;

  PredicateCoverageCPATransferRelation(
      TransferRelation pDelegateTransferRelation,
      FormulaManagerView pFmgr,
      Map<Long, Double> pTimeStampsPerCoverage,
      CoverageData pCov) {
    super(pDelegateTransferRelation);
    predicateTransferRelation = pDelegateTransferRelation;
    timeStampsPerCoverage = pTimeStampsPerCoverage;
    fmgr = Preconditions.checkNotNull(pFmgr);
    cov = Preconditions.checkNotNull(pCov);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {
    return predicateTransferRelation.getAbstractSuccessors(state, precision);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    if (startTime.equals(Instant.MIN)) {
      startTime = Instant.now();
    }
    if (precision instanceof PredicatePrecision) {
      PredicatePrecision predicatePrecision = (PredicatePrecision) precision;
      addTimeStampsForePredicateCoverage(getAllPredicates(predicatePrecision).size());
      addPredicateConsideredNode(cfaEdge, predicatePrecision);
    }
    return predicateTransferRelation.getAbstractSuccessorsForEdge(state, precision, cfaEdge);
  }

  private void addPredicateConsideredNode(CFAEdge cfaEdge, PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates =
        getAllPredicatesForNode(cfaEdge.getPredecessor(), precision);
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      CAssumeEdge a = (CAssumeEdge) cfaEdge;
      CExpression exp = a.getExpression();
      String assumeExpression = exp.toQualifiedASTString();
      if (coversPredicate(allPredicates, assumeExpression)) {
        cov.addPredicateConsideredNode(cfaEdge);
      }
    } else {
      cov.addPredicateConsideredNode(cfaEdge);
    }
  }

  private boolean coversPredicate(
      Set<AbstractionPredicate> allPredicates, String assumeExpression) {
    for (AbstractionPredicate predicate : allPredicates) {
      BooleanFormula formula = predicate.getSymbolicAtom();
      Set<String> predicateVariableNames = fmgr.extractVariableNames(formula);
      Set<String> assumeEdgeVariableNames = extractVariableNames(assumeExpression);

      boolean expressionConsidered = true;
      for (String assumeEdgeVariableName : assumeEdgeVariableNames) {
        boolean oneSideCovered = false;
        for (String predicateVariableName : predicateVariableNames) {
          if (assumeEdgeVariableName.equals(predicateVariableName)) {
            oneSideCovered = true;
            break;
          }
        }
        if (!oneSideCovered) {
          expressionConsidered = false;
          break;
        }
      }
      if (expressionConsidered) {
        return true;
      }
    }
    return false;
  }

  private Set<String> extractVariableNames(String assumeExpression) {
    String purifiedAssumeExpression = assumeExpression.replaceAll("[()]|[\\[\\]]|!|\\s+|\\d", "");
    purifiedAssumeExpression = purifiedAssumeExpression.replaceAll("__", "::");
    String[] assumeParts = purifiedAssumeExpression.split("=|==");
    return new HashSet<>(Arrays.asList(assumeParts));
  }

  private void addTimeStampsForePredicateCoverage(double predicateCoverage) {
    long durationInNanos = Duration.between(startTime, Instant.now()).toNanos();
    long durationInMicros = TimeUnit.NANOSECONDS.toMicros(durationInNanos);
    timeStampsPerCoverage.put(durationInMicros, predicateCoverage);
  }

  private Set<AbstractionPredicate> getAllPredicates(PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates = new HashSet<>();
    allPredicates.addAll(precision.getLocationInstancePredicates().values());
    allPredicates.addAll(precision.getLocalPredicates().values());
    allPredicates.addAll(precision.getFunctionPredicates().values());
    allPredicates.addAll(precision.getGlobalPredicates());
    return allPredicates;
  }

  private Set<AbstractionPredicate> getAllPredicatesForNode(
      CFANode node, PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates = new HashSet<>();
    allPredicates.addAll(precision.getLocalPredicates().get(node));
    allPredicates.addAll(precision.getFunctionPredicates().values());
    allPredicates.addAll(precision.getGlobalPredicates());
    return allPredicates;
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    return predicateTransferRelation.strengthen(state, otherStates, cfaEdge, precision);
  }
}

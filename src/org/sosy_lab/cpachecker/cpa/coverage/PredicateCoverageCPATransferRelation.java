// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateCoverageCPATransferRelation extends AbstractSingleWrapperTransferRelation {

  private final TransferRelation predicateTransferRelation;
  private final CoverageData coverageData;
  private final TimeDependentCoverageData predicateTDCG;
  private final TimeDependentCoverageData predicateConsideredTDCG;
  private final FormulaManagerView fmgr;
  private final CFA cfa;

  PredicateCoverageCPATransferRelation(
      TransferRelation pDelegateTransferRelation,
      FormulaManagerView pFmgr,
      CFA pCfa,
      CoverageData pCoverageData) {
    super(pDelegateTransferRelation);
    predicateTransferRelation = pDelegateTransferRelation;
    fmgr = Preconditions.checkNotNull(pFmgr);
    cfa = Preconditions.checkNotNull(pCfa);
    coverageData = Preconditions.checkNotNull(pCoverageData);
    TimeDependentCoverageHandler coverageHandler = coverageData.getTDCGHandler();
    predicateTDCG = coverageHandler.getData(TimeDependentCoverageType.Predicate);
    predicateConsideredTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateConsidered);
    coverageData.addInitialNodes(cfa, predicateConsideredTDCG);
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
    if (precision instanceof PredicatePrecision) {
      PredicatePrecision predicatePrecision = (PredicatePrecision) precision;
      predicateTDCG.addTimeStamp(getAllPredicates(predicatePrecision).size());
      if (shouldAddPredicateConsideredNode(cfaEdge, predicatePrecision)) {
        coverageData.addPredicateConsideredNode(cfaEdge);
        predicateConsideredTDCG.addTimeStamp(coverageData.getTempPredicateConsideredCoverage(cfa));
      }
    }
    return predicateTransferRelation.getAbstractSuccessorsForEdge(state, precision, cfaEdge);
  }

  private boolean shouldAddPredicateConsideredNode(CFAEdge cfaEdge, PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates =
        getAllPredicatesForNode(cfaEdge.getPredecessor(), precision);
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      CAssumeEdge a = (CAssumeEdge) cfaEdge;
      CExpression exp = a.getExpression();
      String assumeExpression = exp.toQualifiedASTString();
      if (coversPredicate(allPredicates, assumeExpression)) {
        return true;
      }
    } else {
      return true;
    }
    return false;
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

  private Set<AbstractionPredicate> getAllPredicates(PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates =
        new HashSet<>(precision.getLocationInstancePredicates().values());
    allPredicates.addAll(precision.getLocalPredicates().values());
    allPredicates.addAll(precision.getFunctionPredicates().values());
    allPredicates.addAll(precision.getGlobalPredicates());
    return allPredicates;
  }

  private Set<AbstractionPredicate> getAllPredicatesForNode(
      CFANode node, PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates =
        new HashSet<>(precision.getLocalPredicates().get(node));
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

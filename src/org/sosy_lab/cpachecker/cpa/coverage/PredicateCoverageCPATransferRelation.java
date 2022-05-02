// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
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
  private final TimeDependentCoverageData predicateRelevantVariablesTDCG;
  private final TimeDependentCoverageData predicateCoveredNodesTDCG;
  private final FormulaManagerView fmgr;
  private final CFA cfa;

  static final double FREQUENCY_REMOVAL_QUOTIENT = 0.5;
  private int predicatesInUse = 0;

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
    predicateRelevantVariablesTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateRelevantVariables);
    predicateCoveredNodesTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateCoveredNodes);
    coverageData.addInitialNodes(
        cfa, predicateConsideredTDCG, TimeDependentCoverageType.PredicateConsidered);
    coverageData.addInitialNodes(
        cfa, predicateRelevantVariablesTDCG, TimeDependentCoverageType.PredicateRelevantVariables);
    coverageData.addInitialNodes(
        cfa, predicateCoveredNodesTDCG, TimeDependentCoverageType.PredicateCoveredNodes);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state, Precision precision) throws CPATransferException, InterruptedException {
    return predicateTransferRelation.getAbstractSuccessors(state, precision);
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

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    processAllCoverageMeasures(precision, cfaEdge);
    return predicateTransferRelation.getAbstractSuccessorsForEdge(state, precision, cfaEdge);
  }

  private void processAllCoverageMeasures(Precision precision, CFAEdge cfaEdge) {
    if (precision instanceof PredicatePrecision) {
      PredicatePrecision predicatePrecision = (PredicatePrecision) precision;
      processPredicates(predicatePrecision);
      processPredicatesConsideredCoverage(predicatePrecision, cfaEdge);
      processPredicateRelevantVariablesCoverage(predicatePrecision, cfaEdge);
      processPredicatesAbstractStateCoverage(predicatePrecision, cfaEdge);
    }
  }

  private void processPredicatesAbstractStateCoverage(PredicatePrecision precision, CFAEdge edge) {
    Set<CFANode> allPredicateCoveredNodes = getAllPredicateCoveredNodes(precision);
    coverageData.addPredicateCoveredNodes(allPredicateCoveredNodes, edge);
    predicateCoveredNodesTDCG.addTimeStamp(coverageData.getTempPredicateCoveredNodesCoverage(cfa));
  }

  private Set<CFANode> getAllPredicateCoveredNodes(PredicatePrecision precision) {
    return precision.getLocalPredicates().keys().elementSet();
  }

  private void processPredicateRelevantVariablesCoverage(
      PredicatePrecision precision, CFAEdge cfaEdge) {
    Set<String> predicateVariables = getRelevantVariables(cfaEdge, precision);
    Set<String> assumeVariables = convertAssumeToVariables(cfaEdge);
    if (shouldAddPredicateRelevantVariableNode(assumeVariables, predicateVariables)) {
      coverageData.addPredicateRelevantVariablesNodes(cfaEdge);
      predicateRelevantVariablesTDCG.addTimeStamp(
          coverageData.getTempPredicateRelevantVariablesCoverage(cfa));
    }
  }

  private Set<String> getRelevantVariables(CFAEdge cfaEdge, PredicatePrecision precision) {
    Multiset<String> relevantVariableNames = HashMultiset.create();
    Set<AbstractionPredicate> allPredicates =
        getAllPredicatesForNode(cfaEdge.getPredecessor(), precision);
    for (AbstractionPredicate predicate : allPredicates) {
      BooleanFormula formula = predicate.getSymbolicAtom();
      Set<String> predicateVariableNames = fmgr.extractVariableNames(formula);
      relevantVariableNames.addAll(predicateVariableNames);
    }
    return filterRelevantVariables(relevantVariableNames, FREQUENCY_REMOVAL_QUOTIENT);
  }

  private Set<String> filterRelevantVariables(Multiset<String> variables, double percentage) {
    Set<String> nonRelevantVariables = new HashSet<>();
    Set<String> allVariables = variables.elementSet();
    int variableCount = allVariables.size();
    for (var variable : Multisets.copyHighestCountFirst(variables).elementSet()) {
      int nonRelevantVariableCount = nonRelevantVariables.size();
      if (nonRelevantVariableCount / (double) variableCount > percentage) {
        break;
      }
      nonRelevantVariables.add(variable);
    }
    allVariables.removeAll(nonRelevantVariables);
    return allVariables;
  }

  private boolean shouldAddPredicateRelevantVariableNode(
      Set<String> assumeVariables, Set<String> predicateVariables) {
    return predicateVariables.containsAll(assumeVariables);
  }

  private void processPredicates(PredicatePrecision precision) {
    if (shouldAddPredicate(precision)) {
      predicatesInUse = getAllPredicates(precision).size();
      predicateTDCG.addTimeStamp(predicatesInUse);

      predicateRelevantVariablesTDCG.resetTimeStamps();
      coverageData.resetPredicateRelevantVariablesNodes();
      coverageData.addInitialNodes(
          cfa,
          predicateRelevantVariablesTDCG,
          TimeDependentCoverageType.PredicateRelevantVariables);
    }
  }

  private void processPredicatesConsideredCoverage(PredicatePrecision precision, CFAEdge cfaEdge) {
    if (shouldAddPredicateConsideredNode(cfaEdge, precision)) {
      coverageData.addPredicateConsideredNode(cfaEdge);
      predicateConsideredTDCG.addTimeStamp(coverageData.getTempPredicateConsideredCoverage(cfa));
    }
  }

  private boolean shouldAddPredicate(PredicatePrecision precision) {
    return getAllPredicates(precision).size() != predicatesInUse;
  }

  private boolean shouldAddPredicateConsideredNode(CFAEdge cfaEdge, PredicatePrecision precision) {
    Set<AbstractionPredicate> allPredicates =
        getAllPredicatesForNode(cfaEdge.getPredecessor(), precision);
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      if (coversPredicate(allPredicates, convertAssumeToVariables(cfaEdge))) {
        return true;
      }
    } else {
      return true;
    }
    return false;
  }

  private Set<String> convertAssumeToVariables(CFAEdge cfaEdge) {
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      CAssumeEdge a = (CAssumeEdge) cfaEdge;
      CExpression exp = a.getExpression();
      String assumeExpression = exp.toQualifiedASTString();
      return extractVariableNames(assumeExpression);
    }
    return new HashSet<>();
  }

  private boolean coversPredicate(
      Set<AbstractionPredicate> allPredicates, Set<String> assumeVariables) {
    for (AbstractionPredicate predicate : allPredicates) {
      BooleanFormula formula = predicate.getSymbolicAtom();
      Set<String> predicateVariableNames = fmgr.extractVariableNames(formula);
      if (coversPredicateHelper(assumeVariables, predicateVariableNames)) {
        return true;
      }
    }
    return false;
  }

  private boolean coversPredicateHelper(
      Set<String> assumeVariables, Set<String> predicateVariables) {
    boolean expressionConsidered = true;
    for (String assumeEdgeVariableName : assumeVariables) {
      boolean oneSideCovered = false;
      for (String predicateVariableName : predicateVariables) {
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
}

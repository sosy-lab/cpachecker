// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage.predicate;

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
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;
import org.sosy_lab.cpachecker.util.coverage.collectors.PredicateAnalysisCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicateCoverageCPATransferRelation extends AbstractSingleWrapperTransferRelation {
  /* ##### Class Fields ##### */
  private final TransferRelation predicateTransferRelation;
  private final PredicateAnalysisCoverageCollector coverageCollector;
  private final TimeDependentCoverageData predicateTDCG;
  private final TimeDependentCoverageData predicateConsideredTDCG;
  private final TimeDependentCoverageData predicateRelevantVariablesTDCG;
  private final FormulaManagerView fmgr;
  private final CFA cfa;
  static final double FREQUENCY_REMOVAL_QUOTIENT = 0.5;
  private int predicatesInUse = 0;

  /* ##### Constructors ##### */
  PredicateCoverageCPATransferRelation(
      TransferRelation pDelegateTransferRelation,
      FormulaManagerView pFmgr,
      CFA pCfa,
      CoverageCollectorHandler pCovCollectorHandler) {
    super(pDelegateTransferRelation);
    predicateTransferRelation = pDelegateTransferRelation;
    fmgr = Preconditions.checkNotNull(pFmgr);
    cfa = Preconditions.checkNotNull(pCfa);
    coverageCollector =
        Preconditions.checkNotNull(pCovCollectorHandler.getPredicateAnalysisCoverageCollector());
    TimeDependentCoverageHandler coverageHandler = pCovCollectorHandler.getTDCGHandler();
    predicateTDCG = coverageHandler.getData(TimeDependentCoverageType.PredicatesGenerated);
    predicateConsideredTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateConsideredLocations);
    predicateRelevantVariablesTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateRelevantVariables);
    coverageCollector.addInitialNodesForTDCG(
        cfa, predicateConsideredTDCG, TimeDependentCoverageType.PredicateConsideredLocations);
    coverageCollector.addInitialNodesForTDCG(
        cfa, predicateRelevantVariablesTDCG, TimeDependentCoverageType.PredicateRelevantVariables);
  }

  /* ##### Inherited Methods ##### */
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
    processAllCoverageMeasures(precision, cfaEdge, state);
    return predicateTransferRelation.getAbstractSuccessorsForEdge(state, precision, cfaEdge);
  }

  /* ##### Helper Methods ##### */
  private void processAllCoverageMeasures(
      Precision precision, CFAEdge cfaEdge, AbstractState state) {
    if (precision instanceof PredicatePrecision) {
      PredicatePrecision predicatePrecision = (PredicatePrecision) precision;
      processPredicates(predicatePrecision);
      processPredicatesConsideredCoverage(predicatePrecision, cfaEdge);
      processPredicateRelevantVariablesCoverage(predicatePrecision, cfaEdge);
      processRelevantAbstractionVariablesCoverage(cfaEdge, state);
    }
  }

  private void processRelevantAbstractionVariablesCoverage(CFAEdge edge, AbstractState state) {
    Set<String> variableNames = getAllAbstractStateVariables(state);
    coverageCollector.addAbstractionVariables(variableNames, edge);
    coverageCollector.addRelevantAbstractionVariables(variableNames, edge);
  }

  private Set<String> getAllAbstractStateVariables(AbstractState state) {
    if (state instanceof PredicateAbstractState) {
      PredicateAbstractState predicateAbstractState = (PredicateAbstractState) state;
      BooleanFormula abstractionFormula =
          predicateAbstractState.getAbstractionFormula().asFormula();
      return fmgr.extractVariableNames(abstractionFormula);
    }
    return new HashSet<>();
  }

  private void processPredicateRelevantVariablesCoverage(
      PredicatePrecision precision, CFAEdge cfaEdge) {
    Set<String> predicateVariables = getRelevantVariables(cfaEdge, precision);
    Set<String> assumeVariables = convertAssumeToVariables(cfaEdge);
    if (shouldAddPredicateRelevantVariableNode(assumeVariables, predicateVariables)) {
      coverageCollector.addPredicateRelevantVariablesNodes(cfaEdge);
      predicateRelevantVariablesTDCG.addTimeStamp(
          coverageCollector.getTempPredicateRelevantVariablesCoverage(cfa));
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
    for (String variable : Multisets.copyHighestCountFirst(variables).elementSet()) {
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
      coverageCollector.resetPredicateRelevantVariablesNodes();
      coverageCollector.addInitialNodesForTDCG(
          cfa,
          predicateRelevantVariablesTDCG,
          TimeDependentCoverageType.PredicateRelevantVariables);
    }
  }

  private void processPredicatesConsideredCoverage(PredicatePrecision precision, CFAEdge cfaEdge) {
    if (shouldAddPredicateConsideredNode(cfaEdge, precision)) {
      coverageCollector.addPredicateConsideredNode(cfaEdge);
      predicateConsideredTDCG.addTimeStamp(
          coverageCollector.getTempPredicateConsideredCoverage(cfa));
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

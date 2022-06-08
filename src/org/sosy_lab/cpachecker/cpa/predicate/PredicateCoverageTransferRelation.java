// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;
import org.sosy_lab.cpachecker.util.coverage.collectors.PredicateAnalysisCoverageCollector;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageData;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageHandler;
import org.sosy_lab.cpachecker.util.coverage.tdcg.TimeDependentCoverageType;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.variableclassification.VariablesCollectingVisitor;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Transfer Relation which behaves like a PredicateTransferRelation but delegates in addition the
 * calculation of predicate-analysis specific coverage measures and TDCG (Time-dependent Coverage
 * Graph) data series.
 */
public class PredicateCoverageTransferRelation extends PredicateTransferRelation {
  private final PredicateAnalysisCoverageCollector coverageCollector;
  private final TimeDependentCoverageData predicateTDCG;
  private final TimeDependentCoverageData predicateConsideredTDCG;
  private final TimeDependentCoverageData predicateRelevantVariablesTDCG;
  private final TimeDependentCoverageData predicateAbstractionVariablesTDCG;
  private final FormulaManagerView fmgr;

  // should be a value between 0 and 1, the lower, the more variables will be sorted out
  private static final double RELEVANT_VARIABLES_FREQUENCY_FACTOR = 0.75;

  private int predicatesInUse = 0;
  private boolean isFirstTransferRelation = true;

  public PredicateCoverageTransferRelation(
      LogManager pLogger,
      AnalysisDirection pDirection,
      FormulaManagerView pFmgr,
      PathFormulaManager pPfmgr,
      BlockOperator pBlk,
      PredicateAbstractionManager pPredAbsManager,
      PredicateStatistics pStatistics,
      PredicateCpaOptions pOptions,
      CoverageCollectorHandler pCovCollectorHandler) {
    super(pLogger, pDirection, pFmgr, pPfmgr, pBlk, pPredAbsManager, pStatistics, pOptions);
    fmgr = Preconditions.checkNotNull(pFmgr);
    coverageCollector =
        Preconditions.checkNotNull(pCovCollectorHandler.getPredicateAnalysisCollector());
    TimeDependentCoverageHandler coverageHandler = pCovCollectorHandler.getTDCGHandler();
    predicateTDCG = coverageHandler.getData(TimeDependentCoverageType.PredicatesGenerated);
    predicateConsideredTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateConsideredLocations);
    predicateRelevantVariablesTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateRelevantVariables);
    predicateAbstractionVariablesTDCG =
        coverageHandler.getData(TimeDependentCoverageType.PredicateAbstractionVariables);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    addInitialTDCGTimestamps();
    processAllCoverageMeasures(precision, cfaEdge, state);
    return super.getAbstractSuccessorsForEdge(state, precision, cfaEdge);
  }

  private void addInitialTDCGTimestamps() {
    if (isFirstTransferRelation) {
      predicateTDCG.addTimestamp(0);
      predicateConsideredTDCG.addTimestamp(0);
      predicateRelevantVariablesTDCG.addTimestamp(0);
      predicateAbstractionVariablesTDCG.addTimestamp(0);
      isFirstTransferRelation = false;
    }
  }

  private void processAllCoverageMeasures(
      Precision precision, CFAEdge cfaEdge, AbstractState state) {
    if (precision instanceof PredicatePrecision) {
      PredicatePrecision predicatePrecision = (PredicatePrecision) precision;
      processPredicates(predicatePrecision);
      processPredicatesConsideredCoverage(predicatePrecision, cfaEdge, state);
      processPredicateRelevantVariablesCoverage(predicatePrecision, cfaEdge, state);
      processRelevantAbstractionVariablesCoverage(state);
    }
  }

  /**
   * Collects all abstractions variables and relevant abstraction variables from an abstract state.
   * It is important to note, that we just look at the variable names here not at their actual
   * value.
   *
   * @param state state which is considered for the variable extraction.
   */
  private void processRelevantAbstractionVariablesCoverage(AbstractState state) {
    Set<String> variableNames = getAllAbstractStateVariables(state);
    coverageCollector.addRelevantAbstractionVariables(variableNames);
    predicateAbstractionVariablesTDCG.addTimestamp(
        coverageCollector.getTempPredicateAbstractionVariablesCoverage());
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
      PredicatePrecision precision, CFAEdge cfaEdge, AbstractState state) {
    Set<String> predicateVariables = getRelevantVariables(cfaEdge, precision, state);
    Set<String> assumeVariables = convertAssumeToVariables(cfaEdge);
    if (shouldAddPredicateRelevantVariableNode(cfaEdge, assumeVariables, predicateVariables)) {
      coverageCollector.addPredicateRelevantVariablesLocation(cfaEdge);
      predicateRelevantVariablesTDCG.addTimestamp(
          coverageCollector.getTempPredicateRelevantVariablesCoverage());
    }
  }

  private Set<String> getRelevantVariables(
      CFAEdge cfaEdge, PredicatePrecision precision, AbstractState state) {
    Multiset<String> relevantVariableNames = HashMultiset.create();
    Set<AbstractionPredicate> allPredicates =
        getAllPredicatesForNode(precision, state, cfaEdge.getPredecessor());
    for (AbstractionPredicate predicate : allPredicates) {
      BooleanFormula formula = predicate.getSymbolicAtom();
      Set<String> predicateVariableNames = fmgr.extractVariableNames(formula);
      relevantVariableNames.addAll(predicateVariableNames);
    }
    return filterRelevantVariables(relevantVariableNames, RELEVANT_VARIABLES_FREQUENCY_FACTOR);
  }

  private Set<String> filterRelevantVariables(Multiset<String> variables, double percentage) {
    Set<String> relevantVariables = new HashSet<>();
    if (percentage == 0.0) {
      return relevantVariables;
    }
    for (String variable : Multisets.copyHighestCountFirst(variables).elementSet()) {
      if (relevantVariables.size() <= variables.elementSet().size() * percentage) {
        relevantVariables.add(variable);
      }
    }
    return relevantVariables;
  }

  private boolean shouldAddPredicateRelevantVariableNode(
      CFAEdge cfaEdge, Set<String> assumeVariables, Set<String> predicateVariables) {
    if (cfaEdge.getEdgeType() != CFAEdgeType.AssumeEdge) {
      return true;
    }
    if (assumeVariables == null || assumeVariables.isEmpty()) {
      return true;
    }
    return predicateVariables.containsAll(assumeVariables);
  }

  /**
   * Retrieves and processes all predicates. Processing in this context means that we add them to
   * the TDCG and coverage collector.
   *
   * @param precision The precision where we get the predicates
   */
  private void processPredicates(PredicatePrecision precision) {
    if (shouldAddPredicate(precision)) {
      predicatesInUse = getAllPredicates(precision).size();
      predicateTDCG.addTimestamp(predicatesInUse);
    }
  }

  private void processPredicatesConsideredCoverage(
      PredicatePrecision precision, CFAEdge cfaEdge, AbstractState state) {
    if (shouldAddPredicateConsideredNode(cfaEdge, precision, state)) {
      coverageCollector.addPredicateConsideredLocation(cfaEdge);
      predicateConsideredTDCG.addTimestamp(coverageCollector.getTempPredicateConsideredCoverage());
    }
  }

  /**
   * Indicates to add a predicate the collector set if there is a change regarding the predicate set
   * size.
   *
   * @param precision The precision for this abstract state.
   * @return true if a potential predicate candidate should be added to set of predicates.
   */
  private boolean shouldAddPredicate(PredicatePrecision precision) {
    return getAllPredicates(precision).size() != predicatesInUse;
  }

  private boolean shouldAddPredicateConsideredNode(
      CFAEdge cfaEdge, PredicatePrecision precision, AbstractState state) {
    CFANode location = cfaEdge.getPredecessor();
    Set<AbstractionPredicate> allPredicates = getAllPredicatesForNode(precision, state, location);
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      if (coversPredicate(allPredicates, convertAssumeToVariables(cfaEdge))) {
        return true;
      }
    } else {
      return true;
    }
    return false;
  }

  /**
   * Get all variable names from an assume-edge
   *
   * @param cfaEdge the CFA edge which we check if it is an assume-edge
   * @return Set of String variable names contained within the assume statement
   */
  private Set<String> convertAssumeToVariables(CFAEdge cfaEdge) {
    if (cfaEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      CAssumeEdge assumeEdge = (CAssumeEdge) cfaEdge;
      CExpression exp = assumeEdge.getExpression();
      VariablesCollectingVisitor visitor = new VariablesCollectingVisitor(cfaEdge.getPredecessor());
      return exp.accept(visitor);
    }
    return new HashSet<>();
  }

  private boolean coversPredicate(
      Set<AbstractionPredicate> allPredicates, Set<String> assumeVariables) {
    Set<String> predicateVariableNames = new HashSet<>();
    for (AbstractionPredicate predicate : allPredicates) {
      BooleanFormula formula = predicate.getSymbolicAtom();
      predicateVariableNames.addAll(fmgr.extractVariableNames(formula));
    }
    if (assumeVariables == null || assumeVariables.isEmpty()) {
      return true;
    }
    return predicateVariableNames.containsAll(assumeVariables);
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
      PredicatePrecision precision, AbstractState state, CFANode node) {
    Set<AbstractionPredicate> allPredicatesForNode = new HashSet<>();
    if (state instanceof PredicateAbstractState) {
      PredicateAbstractState predicateState = (PredicateAbstractState) state;
      PersistentMap<CFANode, Integer> abstractionLocations =
          predicateState.getAbstractionLocationsOnPath();
      int locInstance = abstractionLocations.getOrDefault(node, 0);
      allPredicatesForNode.addAll(precision.getPredicates(node, locInstance));
    }
    return allPredicatesForNode;
  }
}

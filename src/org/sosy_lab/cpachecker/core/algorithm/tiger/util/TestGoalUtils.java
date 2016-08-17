/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import com.google.common.collect.Sets;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.AllCFAEdgesGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.ClusteringCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class TestGoalUtils {

  public static FQLSpecification parseFQLQuery(String fqlQuery)
      throws InvalidConfigurationException {

    final FQLSpecification result = FQLSpecificationUtil.getFQLSpecification(fqlQuery);

    if (result.hasPassingClause()) {
      // TODO fix this restriction
      throw new InvalidConfigurationException("No PASSING clauses supported at the moment!");
    }

    if (result.hasPredicate()) {
      // TODO fix this restriction
      throw new InvalidConfigurationException("No predicates in FQL queries supported at the moment!");
    }

    return result;
  }

  public static Set<Goal> extractTestGoalPatterns(
      FQLSpecification pFqlSpecification,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator,
      boolean pOptimizeGoalAutomata,
      boolean pUseOmegaLabel,
      boolean removeFeaturesAsTestGoals,
      GuardedEdgeLabel pAlphaLabel,
      GuardedEdgeLabel pInverseAlphaLabel,
      GuardedLabel pOmegaLabel) {

    // (ii) translate query into set of test goals
    // I didn't move this operation to the constructor since it is a potentially expensive operation.
    List<ElementaryCoveragePattern> goalPatterns = extractTestGoalPatterns(pFqlSpecification, pCoverageSpecificationTranslator);
    Set<Goal> goalsToCover = Sets.newLinkedHashSet();

    for (int i = 0; i < goalPatterns.size(); i++) {
      ElementaryCoveragePattern pattern = goalPatterns.get(i);
      Goal lGoal = constructGoal(i + 1, pattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel,
          pOptimizeGoalAutomata, pUseOmegaLabel, removeFeaturesAsTestGoals);
      if (lGoal != null) {
        goalsToCover.add(lGoal);
      }
    }

    return goalsToCover;
  }

  private static List<ElementaryCoveragePattern> extractTestGoalPatterns(FQLSpecification pFQLQuery,
               CoverageSpecificationTranslator pCoverageSpecificationTranslator) {
    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation

    IncrementalCoverageSpecificationTranslator lTranslator =
        new IncrementalCoverageSpecificationTranslator(pCoverageSpecificationTranslator.mPathPatternTranslator);

    Iterator<ElementaryCoveragePattern> lGoalIterator = lTranslator.translate(pFQLQuery.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    int numberOfTestGoals = lTranslator.getNumberOfTestGoals(pFQLQuery.getCoverageSpecification());
    for (int lGoalIndex = 0; lGoalIndex < numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    return lGoalPatterns;
  }

  /**
   * Constructs a test goal from the given pattern.
   * @param pGoalPattern
   * @param pAlphaLabel
   * @param pInverseAlphaLabel
   * @param pOmegaLabel
   * @param pUseAutomatonOptimization
   * @param pUseOmegaLabel
   * @param pRemoveFeaturesAsTestGoals
   * @return
   */
  public static Goal constructGoal(int pIndex, ElementaryCoveragePattern pGoalPattern,
      GuardedEdgeLabel pAlphaLabel,
      GuardedEdgeLabel pInverseAlphaLabel, GuardedLabel pOmegaLabel,
      boolean pUseAutomatonOptimization, boolean pUseOmegaLabel, boolean pRemoveFeaturesAsTestGoals) {

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
        ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel,
            pOmegaLabel, pUseOmegaLabel);

    // remove all automatons where features are test goals
    if (pRemoveFeaturesAsTestGoals && isFeatureAutomaton(automaton)) {
      return null;
    }

    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pUseAutomatonOptimization);

    return new Goal(pIndex, pGoalPattern, automaton);
  }

  private static boolean isFeatureAutomaton(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    for (State state : pAutomaton.getStates()) {
      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge edge : pAutomaton
          .getIncomingEdges(state)) {
        String label = edge.getLabel().toString();
        if (label.contains("__SELECTED_FEATURE_")) { return true; }
      }
    }

    return false;
  }

}

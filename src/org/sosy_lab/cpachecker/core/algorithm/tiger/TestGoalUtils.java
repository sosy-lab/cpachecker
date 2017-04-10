/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger;

import com.google.common.collect.Sets;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;

public class TestGoalUtils {

  private LogManager logger;
  private GuardedEdgeLabel mAlphaLabel;

  public TestGoalUtils(LogManager pLogger, GuardedEdgeLabel pMAlphaLabel) {
    logger = pLogger;

    mAlphaLabel = pMAlphaLabel;
  }

  public Set<Goal> extractTestGoalPatterns(FQLSpecification pFqlSpecification,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator,
      boolean pOptimizeGoalAutomata) {
    LinkedList<ElementaryCoveragePattern> goalPatterns =
        extractTestGoalPatterns(pFqlSpecification, pCoverageSpecificationTranslator);
    Set<Goal> goalsToCover = Sets.newLinkedHashSet();
    for (int i = 0; i < goalPatterns.size(); i++) {
      Goal lGoal = constructGoal(i + 1, goalPatterns.get(i), mAlphaLabel,
          pOptimizeGoalAutomata);
      if (lGoal != null) {
        goalsToCover.add(lGoal);
      }
    }
    return goalsToCover;
  }

  private Goal constructGoal(int pI, ElementaryCoveragePattern pElementaryCoveragePattern,
      GuardedEdgeLabel pMAlphaLabel, boolean pOptimizeGoalAutomata) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
        ToGuardedAutomatonTranslator.toAutomaton(pElementaryCoveragePattern, pMAlphaLabel);
    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pOptimizeGoalAutomata);

    Goal lGoal = new Goal(pI, pElementaryCoveragePattern, automaton);

    if (isFeatureAutomaton(automaton)) { return null; }

    return lGoal;
  }

  private LinkedList<ElementaryCoveragePattern> extractTestGoalPatterns(
      FQLSpecification pFqlSpecification,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator) {
    logger.logf(Level.INFO, "Extracting test goals.");

    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation

    IncrementalCoverageSpecificationTranslator lTranslator =
        new IncrementalCoverageSpecificationTranslator(
            pCoverageSpecificationTranslator.mPathPatternTranslator);

    Iterator<ElementaryCoveragePattern> lGoalIterator =
        lTranslator.translate(pFqlSpecification.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    int numberOfTestGoals =
        lTranslator.getNumberOfTestGoals(pFqlSpecification.getCoverageSpecification());
    for (int lGoalIndex = 0; lGoalIndex < numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    return lGoalPatterns;
  }

  private boolean isFeatureAutomaton(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    return false;
  }

  public FQLSpecification parseFQLQuery(String fqlQuery) throws InvalidConfigurationException {
    FQLSpecification fqlSpecification = null;

    logger.logf(Level.INFO, "FQL query string: %s", fqlQuery);

    fqlSpecification = FQLSpecificationUtil.getFQLSpecification(fqlQuery);

    logger.logf(Level.INFO, "FQL query: %s", fqlSpecification.toString());

    // TODO fix this restriction
    if (fqlSpecification.hasPassingClause()) {
      logger.logf(Level.SEVERE, "No PASSING clauses supported at the moment!");

      throw new InvalidConfigurationException("No PASSING clauses supported at the moment!");
    }

    // TODO fix this restriction
    if (fqlSpecification.hasPredicate()) {
      logger.logf(Level.SEVERE, "No predicates in FQL queries supported at the moment!");

      throw new InvalidConfigurationException(
          "No predicates in FQL queries supported at the moment!");
    }

    return fqlSpecification;
  }

}

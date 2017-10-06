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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.FQLSpecificationUtil;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.InverseGuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.CoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp.IncrementalCoverageSpecificationTranslator;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.Goal;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

public class TestGoalUtils {

  private LogManager logger;
  private GuardedEdgeLabel mAlphaLabel;
  private InverseGuardedEdgeLabel mInverseAlphaLabel;
  private GuardedEdgeLabel mOmegaLabel;
  private boolean optimizeGoalAutomata;



  public TestGoalUtils(LogManager pLogger, GuardedEdgeLabel pMAlphaLabel,
      InverseGuardedEdgeLabel pMInverseAlphaLabel, GuardedEdgeLabel pMOmegaLabel,
      boolean pOptimizeGoalAutomata) {
    logger = pLogger;
    mAlphaLabel = pMAlphaLabel;
    mInverseAlphaLabel = pMInverseAlphaLabel;
    mOmegaLabel = pMOmegaLabel;
    optimizeGoalAutomata = pOptimizeGoalAutomata;
  }


  private Goal constructGoal(int pIndex, ElementaryCoveragePattern pGoalPattern,
      GuardedEdgeLabel pAlphaLabel, InverseGuardedEdgeLabel pInverseAlphaLabel,
      GuardedEdgeLabel pOmegaLabel, boolean pUseAutomatonOptimization, Region pPresenceCondition) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton =
        ToGuardedAutomatonTranslator.toAutomaton(pGoalPattern, pAlphaLabel, pInverseAlphaLabel,
            pOmegaLabel);
    automaton = FQLSpecificationUtil.optimizeAutomaton(automaton, pUseAutomatonOptimization);

    Goal lGoal = new Goal(pIndex, pGoalPattern, automaton, pPresenceCondition);

    return lGoal;
  }


  public Set<Goal> extractTestGoalPatterns(FQLSpecification pFqlSpecification,
      CoverageSpecificationTranslator pCoverageSpecificationTranslator) {

    logger.logf(Level.INFO, "Extracting test goals.");


    // TODO check for (temporarily) unsupported features

    // TODO enable use of infeasibility propagation


    IncrementalCoverageSpecificationTranslator lTranslator =
        new IncrementalCoverageSpecificationTranslator(
            pCoverageSpecificationTranslator.mPathPatternTranslator);

    int statistics_numberOfTestGoals =
        lTranslator.getNumberOfTestGoals(pFqlSpecification.getCoverageSpecification());

    Iterator<ElementaryCoveragePattern> lGoalIterator =
        lTranslator.translate(pFqlSpecification.getCoverageSpecification());
    LinkedList<ElementaryCoveragePattern> lGoalPatterns = new LinkedList<>();

    for (int lGoalIndex = 0; lGoalIndex < statistics_numberOfTestGoals; lGoalIndex++) {
      lGoalPatterns.add(lGoalIterator.next());
    }

    LinkedList<Pair<ElementaryCoveragePattern, Region>> pTestGoalPatterns = new LinkedList<>();
    for (int i = 0; i < lGoalPatterns.size(); i++) {
      pTestGoalPatterns.add(Pair.of(lGoalPatterns.get(i), (Region) null));
    }

    int goalIndex = 1;
    Set<Goal> pGoalsToCover = new LinkedHashSet<>();
    for (Pair<ElementaryCoveragePattern, Region> pair : pTestGoalPatterns) {
      Goal lGoal =
          constructGoal(goalIndex, pair.getFirst(), mAlphaLabel, mInverseAlphaLabel, mOmegaLabel,
              optimizeGoalAutomata,
              pair.getSecond());
      logger.log(Level.INFO, lGoal.getName());
      pGoalsToCover.add(lGoal);
      goalIndex++;
    }

    return pGoalsToCover;
  }

}

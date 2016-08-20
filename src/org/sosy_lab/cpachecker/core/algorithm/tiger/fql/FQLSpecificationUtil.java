/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.tiger.fql;

import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.FQLSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.ToGuardedAutomatonTranslator;
import org.sosy_lab.cpachecker.util.automaton.NFA;


public class FQLSpecificationUtil {

  public static FQLSpecification getFQLSpecification(String pFQLSpecification) {
    // Parse FQL Specification
    FQLSpecification lFQLSpecification;
    try {
      lFQLSpecification = FQLSpecification.parse(pFQLSpecification);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return lFQLSpecification;
  }

  public static NFA<GuardedEdgeLabel> optimizeAutomaton(NFA<GuardedEdgeLabel> pAutomaton, boolean pUseAutomatonOptimization) {
    if (pUseAutomatonOptimization) {
      // TODO: fix for condition coverage
//      NFA<GuardedEdgeLabel> lGoalAutomaton1 = ToGuardedAutomatonTranslator.removeInfeasibleTransitions(pAutomaton);
//      NFA<GuardedEdgeLabel> lGoalAutomaton2 = ToGuardedAutomatonTranslator.removeDeadEnds(pAutomaton);
//      NFA<GuardedEdgeLabel> lGoalAutomaton3 = ToGuardedAutomatonTranslator.reduceEdgeSets(lGoalAutomaton2);
//      NFA<GuardedEdgeLabel> lGoalAutomaton4 = ToGuardedAutomatonTranslator.removeEmptySelfLoops(lGoalAutomaton3);
//      NFA<GuardedEdgeLabel> lGoalAutomaton5 = ToGuardedAutomatonTranslator.removeDeadEnds(lGoalAutomaton4);
//      NFA<GuardedEdgeLabel> lGoalAutomaton6 = ToGuardedAutomatonTranslator.removeRedundantEdges(lGoalAutomaton5);
//      NFA<GuardedEdgeLabel> lGoalAutomaton7 = ToGuardedAutomatonTranslator.removeSingleStutterEdges(lGoalAutomaton6);

    NFA<GuardedEdgeLabel> lGoalAutomaton1 = ToGuardedAutomatonTranslator.removeEmptySelfLoops(pAutomaton);
    NFA<GuardedEdgeLabel> lGoalAutomaton2 = ToGuardedAutomatonTranslator.removeNotMainSelfLoop(lGoalAutomaton1);
    NFA<GuardedEdgeLabel> lGoalAutomaton3 = ToGuardedAutomatonTranslator.removeInfeasibleTransitions(lGoalAutomaton2);
    NFA<GuardedEdgeLabel> lGoalAutomaton4 = ToGuardedAutomatonTranslator.removeDeadEnds(lGoalAutomaton3);
    NFA<GuardedEdgeLabel> lGoalAutomaton5 = ToGuardedAutomatonTranslator.reduceEdgeSets(lGoalAutomaton4);
    NFA<GuardedEdgeLabel> lGoalAutomaton6 = ToGuardedAutomatonTranslator.removeDeadEnds(lGoalAutomaton5);
    NFA<GuardedEdgeLabel> lGoalAutomaton7 = ToGuardedAutomatonTranslator.removeRedundantEdges(lGoalAutomaton6);
    NFA<GuardedEdgeLabel> lGoalAutomaton8 = ToGuardedAutomatonTranslator.removeSingleStutterEdges(lGoalAutomaton7);

      return lGoalAutomaton8;
    }

    return pAutomaton;
  }

}

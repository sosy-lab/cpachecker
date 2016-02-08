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
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;


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

  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> optimizeAutomaton(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, boolean pUseAutomatonOptimization) {
    if (pUseAutomatonOptimization) {
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton1 = ToGuardedAutomatonTranslator.removeInfeasibleTransitions(pAutomaton);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton2 = ToGuardedAutomatonTranslator.removeDeadEnds(lGoalAutomaton1);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton3 = ToGuardedAutomatonTranslator.reduceEdgeSets(lGoalAutomaton2);
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> lGoalAutomaton4 = ToGuardedAutomatonTranslator.removeSelfLoops(lGoalAutomaton3);

      return lGoalAutomaton4;
    }

    return pAutomaton;
  }

}

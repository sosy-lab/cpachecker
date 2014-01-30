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
package org.sosy_lab.cpachecker.tiger.testgen;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonCPA;
import org.sosy_lab.cpachecker.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;


/**
 * Runs analysis for a given test goal automaton. May resue reachability information from other runs.
 */
public interface AnalysisWithReuse {

  /**
   * Runs analysis with reachability reuse.
   * @param pCFA
   * @param reachedSet
   * @param previousAutomaton
   * @param automatonCPA
   * @param entryNode
   * @param passingCPA
   * @return
   */
  Pair<Boolean, CounterexampleInfo> analyse(CFA pCFA, ReachedSet reachedSet, NondeterministicFiniteAutomaton<GuardedEdgeLabel> previousAutomaton,
      GuardedEdgeAutomatonCPA automatonCPA, FunctionEntryNode entryNode, GuardedEdgeAutomatonCPA passingCPA);


}

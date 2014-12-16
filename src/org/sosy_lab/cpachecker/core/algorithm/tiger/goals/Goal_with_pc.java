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
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals;

import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedLabel;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;


public class Goal_with_pc extends Goal {

  private Region mPresenceCondition;

  public Goal_with_pc(int pIndex, ElementaryCoveragePattern pPattern, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedLabel pOmegaLabel,
      Region pPresenceCondition) {
    super(pIndex, pPattern, pAlphaLabel, pInverseAlphaLabel, pOmegaLabel);
    assert pPresenceCondition != null;
    mPresenceCondition = pPresenceCondition;
  }

  public Goal_with_pc(int pIndex, ElementaryCoveragePattern pPattern, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, Region pPresenceCondition) {
    super(pIndex, pPattern, pAutomaton);
    assert pPresenceCondition != null;
    mPresenceCondition = pPresenceCondition;
  }

  public Region getPresenceCondition() {
    return mPresenceCondition;
  }

  public void setPresenceCondition(Region r) {
    mPresenceCondition = r;
  }
}

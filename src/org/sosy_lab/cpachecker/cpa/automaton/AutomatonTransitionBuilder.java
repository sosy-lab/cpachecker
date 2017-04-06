/*
 * CPAchecker is a tool for configurable software verification.
 *
 *  Copyright (C) 2016-2017  University of Passau
 *
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
 */
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

public final class AutomatonTransitionBuilder {

  private AutomatonBoolExpr trigger = null;

  private boolean assumptionTruth = true;
  private ImmutableList<AStatement> assumption = ImmutableList.of();
  private ExpressionTree<AExpression> candidateInvariants = ExpressionTrees.getTrue();
  private ImmutableList<AutomatonAction> actions = ImmutableList.of();

  private ImmutableList<AAstNode> shadowCodePre = ImmutableList.of();
  private ImmutableList<AAstNode> shadowCodePost = ImmutableList.of();

  private ImmutableSet<? extends SafetyProperty> violatedWhenEnteringTarget = ImmutableSet.of();

  private String followStateName = null;
  private AutomatonInternalState followState = null;

  AutomatonTransitionBuilder() {
  }

  public AutomatonTransitionBuilder setTrigger(AutomatonBoolExpr pTrigger) {
    Preconditions.checkNotNull(pTrigger);
    trigger = pTrigger;
    return this;
  }

  public AutomatonTransitionBuilder setAssumeTruth(boolean pAssumptionTruth) {
    assumptionTruth = pAssumptionTruth;
    return this;
  }

  public AutomatonTransitionBuilder setAssumptions(List<AStatement> pAssumption) {
    assumption = ImmutableList.copyOf(pAssumption);
    return this;
  }

  public AutomatonTransitionBuilder setCandidateInvariants(ExpressionTree<AExpression> pCandidateInvariants) {
    candidateInvariants = pCandidateInvariants;
    return this;
  }

  public AutomatonTransitionBuilder setActions(List<AutomatonAction> pActions) {
    actions = ImmutableList.copyOf(pActions);
    return this;
  }

  public AutomatonTransitionBuilder setShadowCodePre(List<AAstNode> pShadowCodePre) {
    if (pShadowCodePre == null) {
      shadowCodePre = ImmutableList.of();
    }
    shadowCodePre = ImmutableList.copyOf(pShadowCodePre);
    return this;
  }

  public AutomatonTransitionBuilder setShadowCodePost(List<AAstNode> pShadowCodePost) {
    if (pShadowCodePost == null) {
      shadowCodePost = ImmutableList.of();
    }
    shadowCodePost = ImmutableList.copyOf(pShadowCodePost);
    return this;
  }

  public AutomatonTransitionBuilder setViolatedWhenEnteringTarget(Set<? extends SafetyProperty> pViolatedWhenEnteringTarget) {
    violatedWhenEnteringTarget = ImmutableSet.copyOf(pViolatedWhenEnteringTarget);
    return this;
  }

  public AutomatonTransitionBuilder setFollowStateName(String pFollowStateName) {
    followStateName = pFollowStateName;
    return this;
  }

  public AutomatonTransitionBuilder setFollowState(AutomatonInternalState pFollowState) {
    Preconditions.checkNotNull(pFollowState);
    followState = pFollowState;
    followStateName = pFollowState.getName();
    return this;
  }

  public AutomatonTransition build() {
    return new AutomatonTransition(trigger, assumption, assumptionTruth,
        shadowCodePre, shadowCodePost, candidateInvariants, actions, followStateName,
        followState, violatedWhenEnteringTarget);
  }

}

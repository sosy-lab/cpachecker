/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPGuard;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPUnion;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPVisitor;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.ThreeValuedAnswer;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonAction;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpressionArguments;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;

public class AutomatonGoal extends Goal {
  private ElementaryCoveragePattern mPattern;
  private NondeterministicFiniteAutomaton<GuardedEdgeLabel> mAutomaton;

  public AutomatonGoal(
      int pIndex,
      ElementaryCoveragePattern pPattern,
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton,
      Region pPresenceCondition) {
    init(pIndex, pPresenceCondition);
    mPattern = pPattern;
    mAutomaton = pAutomaton;
  }


  public ElementaryCoveragePattern getPattern() {
    return mPattern;
  }

  public NondeterministicFiniteAutomaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }

  public CFAEdge getCriticalEdge() {
    final ECPVisitor<CFAEdge> visitor = new ECPVisitor<CFAEdge>() {

      @Override
      public CFAEdge visit(ECPEdgeSet pEdgeSet) {
        if (pEdgeSet.size() == 1) {
          return pEdgeSet.iterator().next();
        } else {
          return null;
        }
      }

      @Override
      public CFAEdge visit(ECPNodeSet pNodeSet) {
        return null;
      }

      @Override
      public CFAEdge visit(ECPPredicate pPredicate) {
        return null;
      }

      @Override
      public CFAEdge visit(ECPConcatenation pConcatenation) {
        CFAEdge edge = null;

        for (int i = 0; i < pConcatenation.size(); i++) {
          ElementaryCoveragePattern ecp = pConcatenation.get(i);

          CFAEdge tmpEdge = ecp.accept(this);

          if (tmpEdge != null) {
            edge = tmpEdge;
          }
        }

        return edge;
      }

      @Override
      public CFAEdge visit(ECPUnion pUnion) {
        return null;
      }

      @Override
      public CFAEdge visit(ECPRepetition pRepetition) {
        return null;
      }

    };

    return getPattern().accept(visitor);
  }

  @Override
  public String getName() {
    CFAEdge ce = getCriticalEdge();
    CFANode pred = ce.getPredecessor();
    if (pred instanceof CLabelNode && !((CLabelNode) pred).getLabel().isEmpty()) {
      return ((CLabelNode) pred).getLabel();
    } else {
      return Integer.toString(getIndex());
    }
  }

  /**
   * Converts the NondeterministicFiniteAutomaton<GuardedEdgeLabel> into a ControlAutomaton
   *
   * @return A control automaton
   */
  public Automaton createControlAutomaton() {
    Preconditions.checkNotNull(mAutomaton);

    final String automatonName = getName();
    final String initialStateName = Integer.toString(mAutomaton.getInitialState().ID);
    final List<AutomatonInternalState> automatonStates = Lists.newArrayList();

    for (State q : mAutomaton.getStates()) {

      final boolean isTarget = mAutomaton.getFinalStates().contains(q);
      final String stateName = Integer.toString(q.ID);
      final List<AutomatonTransition> transitions = Lists.newArrayList();

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge t : mAutomaton
          .getOutgoingEdges(q)) {

        final String sucessorStateName = Integer.toString(t.getTarget().ID);
        final AutomatonBoolExpr trigger = createMatcherForLabel(t.getLabel());
        final ImmutableList<AExpression> assumptions = createAssumesForLabel(t.getLabel());

        AutomatonTransition ct =
            new AutomatonTransition(
                trigger,
                Collections.<AutomatonBoolExpr>emptyList(),
                assumptions,
                Collections.<AutomatonAction>emptyList(),
                sucessorStateName);

        transitions.add(ct);
      }

      automatonStates.add(new AutomatonInternalState(stateName, transitions, isTarget, true));
    }

    try {
      return new Automaton(
          automatonName,
          Maps.<String, AutomatonVariable>newHashMap(),
          automatonStates,
          initialStateName);

    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("Conversion failed!", e);
    }
  }

  private ImmutableList<AExpression> createAssumesForLabel(GuardedEdgeLabel pLabel) {
    Builder<AExpression> result = ImmutableList.builder();

    for (ECPGuard g : pLabel) {
      if (g instanceof ECPPredicate) {
        throw new RuntimeException("ECPPredicate not yet supported as an assumption!");
      }
    }

    return result.build();
  }

  private static class GuardedEdgeMatcher implements AutomatonBoolExpr {

    private final GuardedEdgeLabel label;

    public GuardedEdgeMatcher(GuardedEdgeLabel pLabel) {
      this.label = pLabel;
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs)
        throws CPATransferException {
      return label.contains(pArgs.getCfaEdge()) ? CONST_TRUE : CONST_FALSE;
    }

  }

  private AutomatonBoolExpr createMatcherForLabel(GuardedEdgeLabel pLabel) {
    return new GuardedEdgeMatcher(pLabel);
  }

  @Override
  public ThreeValuedAnswer getsCoveredByPath(List<CFAEdge> pPath) {
    Set<NondeterministicFiniteAutomaton.State> lCurrentStates = new HashSet<>();
    Set<NondeterministicFiniteAutomaton.State> lNextStates = new HashSet<>();

    lCurrentStates.add(mAutomaton.getInitialState());

    boolean lHasPredicates = false;

    for (CFAEdge lCFAEdge : pPath) {
      for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
        // Automaton accepts as soon as it sees a final state (implicit self-loop)
        if (mAutomaton.getFinalStates().contains(lCurrentState)) {
          return ThreeValuedAnswer.ACCEPT;
        }

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : mAutomaton
            .getOutgoingEdges(lCurrentState)) {
          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          if (lLabel.hasGuards()) {
            lHasPredicates = true;
          } else {
            if (lLabel.contains(lCFAEdge)) {
              lNextStates.add(lOutgoingEdge.getTarget());
            }
          }
        }
      }

      lCurrentStates.addAll(lNextStates);
    }

    for (NondeterministicFiniteAutomaton.State lCurrentState : lCurrentStates) {
      // Automaton accepts as soon as it sees a final state (implicit self-loop)
      if (mAutomaton.getFinalStates().contains(lCurrentState)) {
        return ThreeValuedAnswer.ACCEPT;
      }
    }

    if (lHasPredicates) {
      return ThreeValuedAnswer.UNKNOWN;
    } else {
      return ThreeValuedAnswer.REJECT;
    }
  }
}

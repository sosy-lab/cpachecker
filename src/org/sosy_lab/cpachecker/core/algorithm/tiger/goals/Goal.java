/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
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
import org.sosy_lab.cpachecker.core.algorithm.tiger.util.TestCase;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonAction;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.StringExpression;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpressionArguments;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonSafetyPropertyFactory;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.cpa.automaton.SafetyProperty;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton.State;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Region;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class Goal implements SafetyProperty {

  private int mIndex;
  private ElementaryCoveragePattern mPattern;
  private NondeterministicFiniteAutomaton<GuardedEdgeLabel> mAutomaton;
  private Automaton mCheckedWithAutomaton;
  private Region mRemainingPresenceCondition;
  private Map<TestCase, Region> mCoveringTestCases;
  private Region mInfeasiblePresenceCondition;

  public Goal(int pIndex, ElementaryCoveragePattern pPattern,
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton, Region pRemainingPresenceCondition) {
    mIndex = pIndex;
    mPattern = pPattern;
    mAutomaton = pAutomaton;
    setRemainingPresenceCondition(pRemainingPresenceCondition);
    mCoveringTestCases = new HashMap<>();
  }

  public int getIndex() {
    return mIndex;
  }

  public ElementaryCoveragePattern getPattern() {
    return mPattern;
  }

  public NondeterministicFiniteAutomaton<GuardedEdgeLabel> getAutomaton() {
    return mAutomaton;
  }

  public Region getRemainingPresenceCondition() {
    return mRemainingPresenceCondition;
  }

  public void setRemainingPresenceCondition(Region pRemainingPresenceCondition) {
    mRemainingPresenceCondition = pRemainingPresenceCondition;
  }

  public Map<TestCase, Region> getCoveringTestCases() {
    return mCoveringTestCases;
  }

  public void addCoveringTestCase(TestCase pTestCase, Region pPresenceCondition) {
    if (!mCoveringTestCases.containsKey(pTestCase)) {
      mCoveringTestCases.put(pTestCase, pPresenceCondition);
    }
  }

  public Region getInfeasiblePresenceCondition() {
    return mInfeasiblePresenceCondition;
  }

  public void setInfeasiblePresenceCondition(Region pInfeasiblePresenceCondition) {
    mInfeasiblePresenceCondition = pInfeasiblePresenceCondition;
  }

  @Override
  public String toString() {
    return getName();
  }

  public String getName() {
    CFAEdge ce = getCriticalEdge();
    CFANode pred = ce.getPredecessor();
    if (pred instanceof CLabelNode && !((CLabelNode) pred).getLabel().isEmpty()) {
      return ((CLabelNode) pred).getLabel();
    } else {
      return Integer.toString(getIndex());
    }
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

  public String toSkeleton() {
    final ECPVisitor<Boolean> booleanVisitor = new ECPVisitor<Boolean>() {

      @Override
      public Boolean visit(ECPEdgeSet pEdgeSet) {
        if (pEdgeSet.size() <= 1) { return true; }

        return false;
      }

      @Override
      public Boolean visit(ECPNodeSet pNodeSet) {
        throw new RuntimeException("Unsupported Function");
      }

      @Override
      public Boolean visit(ECPPredicate pPredicate) {
        throw new RuntimeException("Unsupported Function");
      }

      @Override
      public Boolean visit(ECPConcatenation pConcatenation) {
        for (int i = 0; i < pConcatenation.size(); i++) {
          ElementaryCoveragePattern ecp = pConcatenation.get(i);

          if (ecp.accept(this)) { return true; }
        }

        return false;
      }

      @Override
      public Boolean visit(ECPUnion pUnion) {
        for (int i = 0; i < pUnion.size(); i++) {
          ElementaryCoveragePattern ecp = pUnion.get(i);

          if (ecp.accept(this)) { return true; }
        }

        return false;
      }

      @Override
      public Boolean visit(ECPRepetition pRepetition) {
        if (pRepetition.getSubpattern().accept(this)) { return true; }

        return false;
      }

    };

    ECPVisitor<String> visitor = new ECPVisitor<String>() {

      @Override
      public String visit(ECPEdgeSet pEdgeSet) {
        if (pEdgeSet.size() == 1) {
          return "[" + pEdgeSet.toString() + "]";
        } else if (pEdgeSet.size() == 0) {
          return "{}";
        } else {
          return "E";
        }
      }

      @Override
      public String visit(ECPNodeSet pNodeSet) {
        throw new RuntimeException("Unsupported Function");
      }

      @Override
      public String visit(ECPPredicate pPredicate) {
        throw new RuntimeException("Unsupported Function");
      }

      @Override
      public String visit(ECPConcatenation pConcatenation) {
        boolean b = false;
        StringBuffer str = new StringBuffer();

        boolean a = false;
        for (int i = 0; i < pConcatenation.size(); i++) {
          ElementaryCoveragePattern ecp = pConcatenation.get(i);

          if (ecp.accept(booleanVisitor)) {
            b = true;

            if (a) {
              str.append(".");
            } else if (i > 0) {
              str.append("E.");
            }
            str.append(ecp.accept(this));

            a = true;
          } else if (b) {
            b = false;

            if (a) {
              str.append(".");
            }
            str.append("E");

            a = true;
          }
        }

        return str.toString();
      }

      @Override
      public String visit(ECPUnion pUnion) {
        boolean b = false;
        StringBuffer str = new StringBuffer();

        boolean a = false;
        for (int i = 0; i < pUnion.size(); i++) {
          ElementaryCoveragePattern ecp = pUnion.get(i);

          if (ecp.accept(booleanVisitor)) {
            b = true;

            if (a) {
              str.append("+");
            } else if (i > 0) {
              str.append("E+");
            }
            str.append(ecp.accept(this));

            a = true;
          } else if (b) {
            b = false;

            if (a) {
              str.append("+");
            }
            str.append("E");

            a = true;
          }
        }

        return str.toString();
      }

      @Override
      public String visit(ECPRepetition pRepetition) {
        if (pRepetition.getSubpattern().accept(booleanVisitor)) {
          return "(" + pRepetition.getSubpattern().accept(this) + "*)";
        } else {
          return "E";
        }
      }

    };

    return getPattern().accept(visitor);
  }

  /**
   * Converts the NondeterministicFiniteAutomaton<GuardedEdgeLabel>
   *    into a ControlAutomaton
   *
   * @return  A control automaton
   */
  public Automaton createControlAutomaton() {
    Preconditions.checkNotNull(mAutomaton);

    // TODO: add/handle alpha, and omega edges!!

    final String automatonName = getName();
    final String initialStateName = Integer.toString(mAutomaton.getInitialState().ID);
    final List<AutomatonInternalState> automatonStates = Lists.newArrayList();

    for (State q : mAutomaton.getStates()) {

      final boolean isTarget = mAutomaton.getFinalStates().contains(q);
      final String stateName = Integer.toString(q.ID);
      final List<AutomatonTransition> transitions = Lists.newArrayList();

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge t : mAutomaton.getOutgoingEdges(q)) {

        final String sucessorStateName = Integer.toString(t.getTarget().ID);
        final AutomatonBoolExpr trigger = createMatcherForLabel(t.getLabel());
        final ImmutableList<AStatement> assumptions = createAssumesForLabel(t.getLabel());

        AutomatonTransition ct = new AutomatonTransition(
            trigger,
            Collections.<AutomatonBoolExpr> emptyList(),
            assumptions,
            true,
            Collections.<AutomatonAction> emptyList(),
            sucessorStateName,
            null,
            ImmutableSet.<SafetyProperty>of(this),
            ImmutableSet.<SafetyProperty>of());

        transitions.add(ct);
      }

      if (isTarget) {
        // Disable the automata after the goal (target state)
        //  has been reached by transiting to BOTTOM
        AutomatonTransition t = new AutomatonTransition(
            AutomatonBoolExpr.TRUE,
            null,
            true,
            Collections.<AutomatonAction>emptyList(),
            AutomatonInternalState.BOTTOM, ImmutableSet.<SafetyProperty>of());

        transitions.add(t);
      }

      automatonStates.add(new AutomatonInternalState(stateName, transitions, isTarget, true));
    }

    try {
      return new Automaton(new AutomatonSafetyPropertyFactory(Configuration.defaultConfiguration(), ""),
          automatonName, Maps.<String, AutomatonVariable> newHashMap(),
          automatonStates, initialStateName);

    } catch (InvalidConfigurationException| InvalidAutomatonException e) {
      throw new RuntimeException("Conversion failed!", e);
    }
  }

  private ImmutableList<AStatement> createAssumesForLabel(GuardedEdgeLabel pLabel) {
    Builder<AStatement> result = ImmutableList.builder();

    for (ECPGuard g : pLabel) {
      if (g instanceof ECPPredicate) { throw new RuntimeException("ECPPredicate not yet supported as an assumption!"); }
    }

    return result.build();
  }

  private static class GuardedEdgeMatcher implements AutomatonBoolExpr {

    private final GuardedEdgeLabel label;

    public GuardedEdgeMatcher(GuardedEdgeLabel pLabel) {
      this.label = Preconditions.checkNotNull(pLabel);
    }

    @Override
    public ResultValue<Boolean> eval(AutomatonExpressionArguments pArgs) throws CPATransferException {
      return label.contains(pArgs.getCfaEdge()) ? CONST_TRUE : CONST_FALSE;
    }

    @Override
    public String toString() {
      return label.toString();
    }

    /**
     * Does this matcher behave semantically equal to
     * other matchers?
     */
    @Override
    public Equality equalityTo(Object pOther) {
      if (!(pOther instanceof GuardedEdgeMatcher)) {
        // Also matches that are implemented in other classes might
        // implement the semantically same behavior, i.e.,
        //  they might match exactly the same set of control-flow transitions
        return Equality.UNKNOWN;
      }

      GuardedEdgeMatcher other = (GuardedEdgeMatcher) pOther;

      if (this.label.equals(other.label)) {
        return Equality.EQUAL;
      }

      return Equality.UNKNOWN;
    }

  }

  private AutomatonBoolExpr createMatcherForLabel(GuardedEdgeLabel pLabel) {
    return new GuardedEdgeMatcher(pLabel);
  }

  @Override
  public ResultValue<?> instantiate(AutomatonExpressionArguments pArgs) {
    return StringExpression.empty().eval(pArgs);
  }

  @Override
  public void setAutomaton(Automaton pAutomaton) {
    mCheckedWithAutomaton = pAutomaton;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((mAutomaton == null) ? 0 : mAutomaton.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof Goal)) { return false; }
    Goal other = (Goal) obj;
    if (mAutomaton == null) {
      if (other.mAutomaton != null) { return false; }
    } else if (!mAutomaton.equals(other.mAutomaton)) { return false; }
    return true;
  }


}

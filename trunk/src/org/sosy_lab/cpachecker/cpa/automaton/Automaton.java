// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

@SuppressFBWarnings(
    value = "VA_FORMAT_STRING_USES_NEWLINE",
    justification = "consistent Unix-style line endings")
public class Automaton {
  private final String name;
  /* The internal variables used by the actions/ assignments of this automaton.
   * This reference of the Map is unused because the actions/assignments get their reference from the parser.
   */
  private final ImmutableMap<String, AutomatonVariable> initVars;
  private final ImmutableList<AutomatonInternalState> states;
  private final AutomatonInternalState initState;

  public Automaton(
      String pName,
      Map<String, AutomatonVariable> pVars,
      List<AutomatonInternalState> pStates,
      String pInitialStateName)
      throws InvalidAutomatonException {
    name = pName;
    initVars = ImmutableMap.copyOf(pVars);
    states = ImmutableList.copyOf(pStates);

    Map<String, AutomatonInternalState> statesMap = Maps.newHashMapWithExpectedSize(pStates.size());
    for (AutomatonInternalState s : pStates) {
      if (statesMap.put(s.getName(), s) != null) {
        throw new InvalidAutomatonException(
            "State " + s.getName() + " exists twice in automaton " + pName);
      }
    }

    initState = statesMap.get(pInitialStateName);
    if (initState == null) {
      throw new InvalidAutomatonException(
          "Inital state " + pInitialStateName + " not found in automaton " + pName);
    }

    // set the FollowStates of all Transitions
    for (AutomatonInternalState s : pStates) {
      s.setFollowStates(statesMap);
    }
  }

  public List<AutomatonInternalState> getStates() {
    return states;
  }

  public String getName() {
    return name;
  }

  AutomatonInternalState getInitialState() {
    return initState;
  }

  public int getNumberOfStates() {
    return states.size();
  }

  /**
   * Prints the contents of a DOT file representing this automaton to the PrintStream.
   *
   * @param pOut the appendable to write to
   */
  public void writeDotFile(Appendable pOut) throws IOException {
    pOut.append("digraph " + name + "{\n");

    boolean errorState = false;
    boolean bottomState = false;

    for (AutomatonInternalState s : states) {
      String color = initState.equals(s) ? "green" : "black";

      pOut.append(formatState(s, color));

      for (AutomatonTransition t : s.getTransitions()) {
        pOut.append(formatTransition(s, t));

        errorState = errorState || t.getFollowState().equals(AutomatonInternalState.ERROR);
        bottomState = bottomState || t.getFollowState().equals(AutomatonInternalState.BOTTOM);
      }
    }

    if (errorState) {
      pOut.append(formatState(AutomatonInternalState.ERROR, "red"));
    }

    if (bottomState) {
      pOut.append(formatState(AutomatonInternalState.BOTTOM, "red"));
    }
    pOut.append("}\n");
  }

  private static String formatState(AutomatonInternalState s, String color) {
    String name = s.getName().replace("_predefinedState_", "");
    String shape = s.isTarget() ? "doublecircle" : "circle";
    return String.format(
        "%d [shape=\"" + shape + "\" color=\"%s\" label=\"%s\"]\n", s.getStateId(), color, name);
  }

  private static String formatTransition(
      AutomatonInternalState sourceState, AutomatonTransition t) {
    return String.format(
        "%d -> %d [label=\"%s\"]\n",
        sourceState.getStateId(),
        t.getFollowState().getStateId(),
        t.toString().replace("\"", "\\\""));
  }

  public ImmutableMap<String, AutomatonVariable> getInitialVariables() {
    return initVars;
  }

  public Collection<ExpressionTree<AExpression>> getAllCandidateInvariants() {
    Collection<ExpressionTree<AExpression>> invariants = new ArrayList<>(states.size());
    ExpressionTree<AExpression> invariant;

    for (AutomatonInternalState state : states) {
      for (AutomatonTransition trans : state.getTransitions()) {
        invariant = trans.getCandidateInvariants();
        if (invariant == ExpressionTrees.<AExpression>getTrue()
            || invariant == ExpressionTrees.<AExpression>getFalse()) {
          continue;
        }
        invariants.add(invariant);
      }
    }
    return invariants;
  }

  /**
   * Assert this automaton fulfills the requirements of an ObserverAutomaton. This means the
   * Automaton does not modify other CPAs (Keyword MODIFY) and does not use the BOTTOM element
   * (Keyword STOP).
   *
   * @throws InvalidConfigurationException if the requirements are not fulfilled
   */
  public void assertObserverAutomaton() throws InvalidConfigurationException {
    for (AutomatonInternalState s : states) {
      for (AutomatonTransition t : s.getTransitions()) {
        if (!t.meetsObserverRequirements()) {
          throw new InvalidConfigurationException(
              "The transition \""
                  + t
                  + "\" in state \""
                  + s
                  + "\" is not valid for an ObserverAutomaton.");
        }
      }
    }
  }

  @Override
  public String toString() {
    final StringBuilder str = new StringBuilder();

    str.append("CONTROL AUTOMATON ").append(getName()).append("\n\n");

    for (Entry<String, AutomatonVariable> e : initVars.entrySet()) {
      str.append(String.format("LOCAL int %s = %s;%n%n", e.getKey(), e.getValue()));
    }

    str.append("INITIAL STATE ").append(initState).append(";\n\n");

    for (AutomatonInternalState s : states) {
      str.append("STATE ")
          .append(s.isNonDetState() ? "USEALL " : "USEFIRST ")
          .append(s.getName())
          .append(":\n");
      for (AutomatonTransition t : s.getTransitions()) {
        str.append("    ").append(t);
        if (t.getFollowState() != AutomatonInternalState.BOTTOM
            && t.getFollowState() != AutomatonInternalState.ERROR) {
          str.append("GOTO ");
        }
        str.append(t.getFollowState()).append(";\n");
      }
      str.append("\n");
    }

    str.append("END AUTOMATON\n");

    return str.toString();
  }

  /**
   * Determine, if automaton is relevant for the given CFA, i.e. if there is at least one automaton
   * transition, which matches at least one CFA edge. In this case automaton may be used during the
   * analysis of the given CFA.
   */
  public boolean isRelevantForCFA(CFA cfa) {
    for (CFANode node : cfa.getAllNodes()) {
      for (int i = 0; i < node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        for (AutomatonTransition transition : initState.getTransitions()) {
          AutomatonExpressionArguments args =
              new AutomatonExpressionArguments(
                  null,
                  ImmutableMap.of(),
                  ImmutableList.of(),
                  edge,
                  LogManager.createNullLogManager());
          try {
            if (!transition.getTrigger().eval(args).canNotEvaluate()
                && transition.getTrigger().eval(args).getValue()) {
              return true;
            }
          } catch (CPATransferException e) {
            // ignore it, since we cannot process all transition triggers here.
          }
        }
      }
    }
    return false;
  }
}

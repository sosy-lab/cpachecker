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
package org.sosy_lab.cpachecker.cpa.automaton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import org.sosy_lab.common.configuration.InvalidConfigurationException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings(value = "VA_FORMAT_STRING_USES_NEWLINE",
    justification = "consistent Unix-style line endings")
public class Automaton {

  private final String name;
  /* The internal variables used by the actions/ assignments of this automaton.
   * This reference of the Map is unused because the actions/assignments get their reference from the parser.
   */
  private final Map<String, AutomatonVariable> initVars;
  private final Set<AutomatonInternalState> states = Sets.newHashSet();
  private final Set<AutomatonInternalState> targetStates = Sets.newHashSet();
  private final AutomatonInternalState initState;
  private final Set<AutomatonSafetyProperty> encodedProperties = Sets.newHashSet();

  private Optional<Boolean> isObservingOnly = Optional.absent();

  public Automaton(String pName, Map<String, AutomatonVariable> pVars, List<AutomatonInternalState> pStates,
      String pInitialStateName) throws InvalidAutomatonException {

    this.name = pName;
    this.initVars = pVars;

    Map<String, AutomatonInternalState> nameToState = Maps.newHashMapWithExpectedSize(pStates.size());
    for (AutomatonInternalState q : pStates) {
      // Check for duplicated state names in the input
      if (nameToState.put(q.getName(), q) != null) {
        throw new InvalidAutomatonException("State " + q.getName() + " exists twice in automaton " + pName);
      }

      // The collection of states is a set (unique names)
      this.states.add(q);
      if (q.isTarget()) {
        this.targetStates.add(q);
      }

      for (AutomatonTransition t: q.getTransitions()) {
        if (t.getFollowState().isTarget()) {
          AutomatonSafetyProperty p = new AutomatonSafetyProperty(this, t);
          encodedProperties.add(p);
        }
      }
    }

    initState = nameToState.get(pInitialStateName);
    if (initState == null) {
      throw new InvalidAutomatonException("Inital state " + pInitialStateName + " not found in automaton " + pName);
    }

    // set the FollowStates of all Transitions
    for (AutomatonInternalState q : pStates) {
      q.setFollowStates(nameToState);
    }
  }

  public Set<AutomatonInternalState> getStates() {
    return states;
  }

  public int getTransitionsToTargetStatesCount() {
    ArrayList<AutomatonTransition> result = Lists.newArrayList();

    for (AutomatonInternalState q: states) {
      for (AutomatonTransition t: q.getTransitions()) {

        if (t.getFollowState().isTarget()) {
          result.add(t);
        }
      }
    }

    return result.size();
  }

  public Set<AutomatonInternalState> getTargetStates() {
    return targetStates;
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
   * @param pOut
   * @throws IOException
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
    StringBuilder sb = new StringBuilder();

    final String name = s.getName().replace("_predefinedState_", "");
    sb.append(String.format("%d [label=\"%s\" ", s.getStateId(), name));

    String shape = s.isTarget() ? "doublecircle" : "circle";
    sb.append("shape=\"" + shape + "\" ");

    if (s.getDoesMatchAll()) {
      sb.append("style=filled ");
    }

    sb.append(String.format("]\n"));

    return sb.toString();
  }

  private static String formatTransition(AutomatonInternalState sourceState, AutomatonTransition t) {
    return String.format("%d -> %d [label=\"%s\"]\n", sourceState.getStateId(), t.getFollowState().getStateId(), t.toString().replaceAll("\"", Matcher.quoteReplacement("\\\"")));
  }


  public Map<String, AutomatonVariable> getInitialVariables() {
    return initVars;
  }

  /**
   * Assert this automaton fulfills the requirements of an ObserverAutomaton.
   * This means the Automaton does not modify other CPAs (Keyword MODIFY) and does not use the BOTTOM element (Keyword STOP).
   * @throws InvalidConfigurationException if the requirements are not fulfilled
   */
  public void assertObserverAutomaton() throws InvalidConfigurationException {
    for (AutomatonInternalState s : this.states) {
        for (AutomatonTransition t : s.getTransitions()) {
          if (!t.meetsObserverRequirements()) {
            throw new InvalidConfigurationException("The transition " + t
                + " in state \"" + s + "\" is not valid for an ObserverAutomaton.");
        }
      }
    }

    this.isObservingOnly = Optional.of(Boolean.TRUE);
  }

  public boolean getIsObservingOnly() {
    if (isObservingOnly.isPresent()) {
      return isObservingOnly.get();
    }
    return false;
  }

  public Set<AutomatonInternalState> getStatesThatMightReachTargetOverapprox(AutomatonInternalState pTarget) {
    // All target states is a valid overapproximation
    //    TODO!!
    return targetStates;
  }

  public Set<AutomatonInternalState> getReachableTargetStatesOverapprox() {
    return ImmutableSet.copyOf(states);
  }

  public ImmutableSet<AutomatonSafetyProperty> getEncodedProperties() {
    return ImmutableSet.copyOf(encodedProperties);
  }

}

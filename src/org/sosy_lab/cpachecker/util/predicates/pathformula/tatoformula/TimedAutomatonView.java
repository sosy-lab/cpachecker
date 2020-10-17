// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAEncodingOptions.SpecialActionType;

public class TimedAutomatonView {
  private Iterable<TaDeclaration> allAutomata;
  private Iterable<TCFANode> allNodes;
  private final Map<TCFAEdge, TaVariable> dummyActionsByEdge;
  private Map<TCFANode, Collection<TCFAEdge>> edgesByPredecessor;
  private final Map<TaDeclaration, TaVariable> idleActionByAutomaton;
  private final Map<TaDeclaration, TaVariable> delayActionByAutomaton;
  private final Map<TaDeclaration, Iterable<TCFAEdge>> edgesByAutomaton;
  private final Map<TaDeclaration, Iterable<TCFANode>> nodesByAutomaton;
  private final Map<TaDeclaration, Iterable<TaVariable>> actionsByAutomaton;
  private final Map<TaDeclaration, Iterable<TCFANode>> initialNodesByAutomaton;
  private final Map<TaDeclaration, Set<TaVariable>> clocksByAutomaton;

  private final TAEncodingOptions options;

  public TimedAutomatonView(CFA pCfa, TAEncodingOptions pOptions) {
    options = pOptions;

    dummyActionsByEdge = new HashMap<>();
    idleActionByAutomaton = new HashMap<>();
    delayActionByAutomaton = new HashMap<>();
    edgesByAutomaton = new HashMap<>();
    nodesByAutomaton = new HashMap<>();
    actionsByAutomaton = new HashMap<>();
    clocksByAutomaton = new HashMap<>();
    initialNodesByAutomaton = new HashMap<>();

    // Methods depend on each other, do not change the order
    initializeAllAutomata(pCfa);
    initializeAllNodes(pCfa);
    getAllAutomata().forEach(this::initializeAutomatonMaps);
    initializeEdgesByPredecessor();
  }

  private void initializeAllAutomata(CFA pCfa) {
    allAutomata =
        from(pCfa.getAllFunctions().values())
            .filter(instanceOf(TCFAEntryNode.class))
            .transform(entry -> (TaDeclaration) entry.getFunction())
            .toSet();
  }

  private void initializeAllNodes(CFA pCfa) {
    allNodes =
        from(pCfa.getAllNodes())
            .filter(instanceOf(TCFANode.class))
            .transform(location -> (TCFANode) location)
            .toSet();
  }

  private void initializeAutomatonMaps(TaDeclaration pAutomaton) {
    // Methods depend on each other, do not change the order
    initializeNodesByAutomaton(pAutomaton);
    initializeEdgesByAutomaton(pAutomaton);
    initializeActions(pAutomaton);
    initializeClocks(pAutomaton);
  }

  private void initializeNodesByAutomaton(TaDeclaration pAutomaton) {
    var nodes = from(allNodes).filter(node -> node.getAutomatonDeclaration() == pAutomaton);
    nodesByAutomaton.put(pAutomaton, nodes.toSet());
    initialNodesByAutomaton.put(pAutomaton, nodes.filter(node -> node.isInitialState()).toSet());
  }

  private void initializeEdgesByAutomaton(TaDeclaration pAutomaton) {
    var edges =
        from(getNodesByAutomaton(pAutomaton))
            .transformAndConcat(node -> CFAUtils.allEnteringEdges(node))
            .filter(instanceOf(TCFAEdge.class))
            .transform(edge -> (TCFAEdge) edge);
    edgesByAutomaton.put(pAutomaton, edges.toSet());
  }

  private void initializeActions(TaDeclaration pAutomaton) {
    var initialActions = from(pAutomaton.getActions());
    var dummyActions =
        from(edgesByAutomaton.get(pAutomaton))
            .filter(edge -> !edge.getAction().isPresent())
            .transform(this::createAndAddDummyAction);
    makeDelayActionForAutomaton(pAutomaton);
    makeIdleActionForAutomaton(pAutomaton);

    actionsByAutomaton.put(pAutomaton, initialActions.append(dummyActions).toSet());
  }

  private void initializeClocks(TaDeclaration pAutomaton) {
    clocksByAutomaton.put(pAutomaton, ImmutableSet.copyOf(pAutomaton.getClocks()));
  }

  private void initializeEdgesByPredecessor() {
    var allEdges = from(edgesByAutomaton.values()).transformAndConcat(Functions.identity());
    edgesByPredecessor = allEdges.index(edge -> (TCFANode) edge.getPredecessor()).asMap();
  }

  private void makeIdleActionForAutomaton(TaDeclaration pAutomaton) {
    if (options.idleActionType == SpecialActionType.NONE) {
      return;
    }

    var isLocal = options.idleActionType == SpecialActionType.LOCAL;
    createAndAddSpecialVariable(pAutomaton, "idle", isLocal, idleActionByAutomaton);
  }

  private void makeDelayActionForAutomaton(TaDeclaration pAutomaton) {
    if (options.delayActionType == SpecialActionType.NONE) {
      return;
    }

    var isLocal = options.delayActionType == SpecialActionType.LOCAL;
    createAndAddSpecialVariable(pAutomaton, "delay", isLocal, delayActionByAutomaton);
  }

  private void createAndAddSpecialVariable(
      TaDeclaration automaton,
      String namePrefix,
      boolean addAsLocalVariable,
      Map<TaDeclaration, TaVariable> specialVariablesMap) {
    TaVariable newSpecialVariable;
    if (addAsLocalVariable) {
      newSpecialVariable =
          TaVariable.createDummyVariable(namePrefix + "#dummy", automaton.getName(), true);
    } else {
      // all automata share the same variable
      var existingVariable = specialVariablesMap.values().stream().findAny();
      newSpecialVariable =
          existingVariable.orElse(
              TaVariable.createDummyVariable(namePrefix + "#dummy", "global", false));
    }

    specialVariablesMap.put(automaton, newSpecialVariable);
  }

  private TaVariable createAndAddDummyAction(TCFAEdge pEdge) {
    var predecessor = (TCFANode) pEdge.getPredecessor();
    var successor = (TCFANode) pEdge.getSuccessor();
    var name = predecessor.getName() + successor.getName() + "#dummy_" + pEdge.hashCode();
    var dummyAction =
        TaVariable.createDummyVariable(name, predecessor.getAutomatonDeclaration().getName(), true);

    dummyActionsByEdge.put(pEdge, dummyAction);
    return dummyAction;
  }

  public Iterable<TCFANode> getAllNodes() {
    return allNodes;
  }

  public Iterable<TaDeclaration> getAllAutomata() {
    return allAutomata;
  }

  public Iterable<TCFANode> getInitialNodesByAutomaton(TaDeclaration pAutomaton) {
    return initialNodesByAutomaton.get(pAutomaton);
  }

  public Iterable<TCFANode> getNodesByAutomaton(TaDeclaration pAutomaton) {
    return nodesByAutomaton.get(pAutomaton);
  }

  public Iterable<TCFAEdge> getEdgesByAutomaton(TaDeclaration pAutomaton) {
    return edgesByAutomaton.get(pAutomaton);
  }

  public Iterable<TaVariable> getActionsByAutomaton(TaDeclaration pAutomaton) {
    var result = from(actionsByAutomaton.get(pAutomaton));
    if (idleActionByAutomaton.containsKey(pAutomaton)) {
      result = result.append(idleActionByAutomaton.get(pAutomaton));
    }
    if (delayActionByAutomaton.containsKey(pAutomaton)) {
      result = result.append(delayActionByAutomaton.get(pAutomaton));
    }
    return result.toSet();
  }

  public Iterable<TaVariable> getSharedActionsByAutomaton(TaDeclaration pAutomaton) {
    var allActions = getActionsByAutomaton(pAutomaton);
    return from(allActions).filter(action -> !action.isLocal()).toSet();
  }

  public Iterable<TaVariable> getAllActions() {
    return from(getAllAutomata()).transformAndConcat(automaton -> getActionsByAutomaton(automaton));
  }

  public Iterable<TaVariable> getClocksByAutomaton(TaDeclaration pAutomaton) {
    return clocksByAutomaton.get(pAutomaton);
  }

  public TaVariable getActionOrDummy(TCFAEdge pEdge) {
    var action = pEdge.getAction().toJavaUtil();
    return action.or(() -> java.util.Optional.of(dummyActionsByEdge.get(pEdge))).orElseThrow();
  }

  public Optional<TaVariable> getIdleAction(TaDeclaration pAutomaton) {
    return Optional.fromNullable(idleActionByAutomaton.get(pAutomaton));
  }

  public Optional<TaVariable> getDelayAction(TaDeclaration pAutomaton) {
    return Optional.fromNullable(delayActionByAutomaton.get(pAutomaton));
  }

  public Collection<TaDeclaration> getAutomataWithAction(TaVariable pVariable) {
    return from(getAllAutomata())
        .filter(automaton -> from(getActionsByAutomaton(automaton)).contains(pVariable))
        .toSet();
  }

  public Iterable<TCFAEdge> getEdgesByPredecessor(TCFANode pPredecessor) {
    return edgesByPredecessor.getOrDefault(pPredecessor, ImmutableSet.of());
  }

  public TaVariable addClockToAutomaton(TaDeclaration pAutomaton, String clockName) {
    var variable = TaVariable.createDummyVariable(clockName, pAutomaton.getName(), true);
    var clocks = clocksByAutomaton.get(pAutomaton);
    var clocksUpdated = from(clocks).append(variable).toSet();
    clocksByAutomaton.put(pAutomaton, clocksUpdated);

    return variable;
  }
}

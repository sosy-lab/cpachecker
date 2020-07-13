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

import com.google.common.base.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TaEncodingOptions.SpecialActionType;

public class TimedAutomatonView {
  private final CFA cfa;
  private final Map<TCFAEdge, TaVariable> dummyActionsByEdge;
  private final Map<TaDeclaration, Iterable<TaVariable>> actionsByAutomaton;
  private final Map<TaDeclaration, TaVariable> idleActionByAutomaton;
  private final Map<TaDeclaration, TaVariable> delayActionByAutomaton;
  private final Map<TaDeclaration, Iterable<TCFAEdge>> edgesByAutomaton;
  private final Map<TaDeclaration, Iterable<TCFANode>> nodesByAutomaton;
  private final Iterable<TaDeclaration> allAutomata;

  // todo remove later maybe
  private final TaEncodingOptions options;

  public TimedAutomatonView(CFA pCfa, TaEncodingOptions pOptions) {
    cfa = pCfa;
    options = pOptions;

    allAutomata =
        from(cfa.getAllFunctions().values())
            .filter(instanceOf(TCFAEntryNode.class))
            .transform(entry -> (TaDeclaration) entry.getFunction())
            .toSet();

    dummyActionsByEdge = new HashMap<>();
    actionsByAutomaton = new HashMap<>();
    idleActionByAutomaton = new HashMap<>();
    delayActionByAutomaton = new HashMap<>();
    edgesByAutomaton = new HashMap<>();
    nodesByAutomaton = new HashMap<>();

    for (var automaton : getAllAutomata()) {
      var nodes =
          from(cfa.getAllNodes())
              .filter(instanceOf(TCFANode.class))
              .transform(node -> (TCFANode) node)
              .filter(node -> node.getAutomatonDeclaration() == automaton);
      nodesByAutomaton.put(automaton, nodes.toSet());

      var edges =
          from(getNodesByAutomaton(automaton))
              .transformAndConcat(node -> CFAUtils.allEnteringEdges(node))
              .filter(instanceOf(TCFAEdge.class))
              .transform(edge -> (TCFAEdge) edge);
      edgesByAutomaton.put(automaton, edges.toSet());

      var initialActions = from(automaton.getActions());
      var dummyActions =
          edges
              .filter(edge -> !edge.getAction().isPresent())
              .transform(
                  edge -> {
                    var dummyAction = createDummyAction(edge);
                    dummyActionsByEdge.put(edge, dummyAction);
                    return dummyAction;
                  });
      makeDelayActionForAutomaton(automaton);
      makeIdleActionForAutomaton(automaton);

      actionsByAutomaton.put(automaton, initialActions.append(dummyActions).toSet());
    }
  }

  private void makeIdleActionForAutomaton(TaDeclaration pAutomaton) {
    TaVariable dummyVar;
    if (options.idleAction == SpecialActionType.GLOBAL) {
      if (idleActionByAutomaton.values().isEmpty()) {
        dummyVar = TaVariable.createDummyVariable("idle#dummy", "global", false);
      } else {
        // all automata share the same variable
        dummyVar = idleActionByAutomaton.values().iterator().next();
      }
    } else if (options.idleAction == SpecialActionType.LOCAL) {
      dummyVar = TaVariable.createDummyVariable("idle#dummy", pAutomaton.getName(), true);
    } else {
      return;
    }
    idleActionByAutomaton.put(pAutomaton, dummyVar);
  }

  private void makeDelayActionForAutomaton(TaDeclaration pAutomaton) {
    TaVariable dummyVar;
    if (options.delayAction == SpecialActionType.GLOBAL) {
      if (delayActionByAutomaton.values().isEmpty()) {
        dummyVar = TaVariable.createDummyVariable("delay#dummy", "global", false);
      } else {
        // all automata share the same variable
        dummyVar = delayActionByAutomaton.values().iterator().next();
      }
    } else if (options.delayAction == SpecialActionType.LOCAL) {
      dummyVar = TaVariable.createDummyVariable("delay#dummy", pAutomaton.getName(), true);
    } else {
      return;
    }
    delayActionByAutomaton.put(pAutomaton, dummyVar);
  }

  private TaVariable createDummyAction(TCFAEdge pEdge) {
    var predecessor = (TCFANode) pEdge.getPredecessor();
    var successor = (TCFANode) pEdge.getSuccessor();
    var name = predecessor.getName() + successor.getName() + "#dummy_" + pEdge.hashCode();
    return TaVariable.createDummyVariable(
        name, predecessor.getAutomatonDeclaration().getName(), true);
  }

  public Iterable<TCFANode> getAllNodes() {
    return from(cfa.getAllNodes())
        .filter(instanceOf(TCFANode.class))
        .transform(location -> (TCFANode) location);
  }

  public Iterable<TaDeclaration> getAllAutomata() {
    return allAutomata;
  }

  public Iterable<TCFANode> getInitialNodesByAutomaton(TaDeclaration pAutomaton) {
    var result = new HashSet<TCFANode>();
    var nodes = from(cfa.getAllNodes()).filter(instanceOf(TCFANode.class));
    for (var node : nodes) {
      var tNode = (TCFANode) node;
      if (tNode.isInitialState() && tNode.getAutomatonDeclaration() == pAutomaton) {
        result.add(tNode);
      }
    }

    return result;
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

  public Iterable<TaVariable> getAllActions() {
    return from(getAllAutomata()).transformAndConcat(automaton -> getActionsByAutomaton(automaton));
  }

  public Iterable<TaVariable> getClocksByAutomaton(TaDeclaration pAutomaton) {
    return pAutomaton.getClocks();
  }

  public TaVariable getActionOrDummy(TCFAEdge pEdge) {
    return dummyActionsByEdge.getOrDefault(pEdge, pEdge.getAction().get());
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
}

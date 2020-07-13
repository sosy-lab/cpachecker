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

import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEntryNode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class TimedAutomatonView {
  private final CFA cfa;

  public TimedAutomatonView(CFA pCfa) {
    cfa = pCfa;
  }

  public Iterable<TCFANode> getAllNodes() {
    return from(cfa.getAllNodes())
        .filter(instanceOf(TCFANode.class))
        .transform(location -> (TCFANode) location);
  }

  public Iterable<TaDeclaration> getAllAutomata() {
    return from(cfa.getAllFunctions().values())
        .filter(instanceOf(TCFAEntryNode.class))
        .transform(entry -> (TaDeclaration) entry.getFunction());
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
    return from(cfa.getAllNodes())
        .filter(instanceOf(TCFANode.class))
        .transform(node -> (TCFANode) node)
        .filter(node -> node.getAutomatonDeclaration() == pAutomaton);
  }

  public Iterable<TCFAEdge> getEdgesByAutomaton(TaDeclaration pAutomaton) {
    return from(getNodesByAutomaton(pAutomaton))
        .transformAndConcat(node -> CFAUtils.allEnteringEdges(node))
        .filter(instanceOf(TCFAEdge.class))
        .transform(edge -> (TCFAEdge) edge);
  }

  public Iterable<TaVariable> getActionsByAutomaton(TaDeclaration pAutomaton) {
    return pAutomaton.getActions();
  }

  public Iterable<TaVariable> getAllActions() {
    return from(getAllAutomata()).transformAndConcat(automaton -> getActionsByAutomaton(automaton));
  }

  public Iterable<TaVariable> getClocksByAutomaton(TaDeclaration pAutomaton) {
    return pAutomaton.getClocks();
  }
}

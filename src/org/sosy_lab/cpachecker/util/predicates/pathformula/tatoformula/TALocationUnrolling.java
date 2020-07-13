// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.EncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TALocationUnrolling extends AutomatonEncoding {
  private final Table<TaDeclaration, TCFANode, Collection<TCFAEdge>> edgesByPredecessor;

  public TALocationUnrolling(
      FormulaManagerView pFmgr,
      CFA pCfa,
      TimeEncoding pTime,
      ActionEncoding pActions,
      LocationEncoding pLocations,
      Iterable<EncodingExtension> pExtensions) {
    super(pFmgr, pCfa, pTime, pActions, pLocations, pExtensions);

    edgesByPredecessor = HashBasedTable.create();
    for (var automatonEdges : edgesByAutomaton.entrySet()) {
      var automaton = automatonEdges.getKey();
      for (var edge : automatonEdges.getValue()) {
        var predecessor = (TCFANode) edge.getPredecessor();
        if (!edgesByPredecessor.contains(automaton, predecessor)) {
          edgesByPredecessor.put(automaton, predecessor, new HashSet<>());
        }
        edgesByPredecessor.get(automaton, predecessor).add(edge);
      }
    }
  }

  @Override
  protected BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    var delayFormula = makeDelayTransition(pAutomaton, pLastReachedIndex);
    var idleFormula = makeIdleTransition(pAutomaton, pLastReachedIndex);

    var edgesByNode = edgesByPredecessor.row(pAutomaton);
    for (var node : edgesByNode.keySet()) {
      var nodeFormula = locations.makeLocationEqualsFormula(pAutomaton, pLastReachedIndex, node);
      var discreteSteps =
          from(edgesByNode.get(node))
              .transform(edge -> makeDiscreteStep(pAutomaton, pLastReachedIndex, edge));
      var locationStep = bFmgr.or(delayFormula, idleFormula, bFmgr.or(discreteSteps.toSet()));
      result = bFmgr.and(bFmgr.implication(nodeFormula, locationStep), result);
    }

    return result;
  }
}

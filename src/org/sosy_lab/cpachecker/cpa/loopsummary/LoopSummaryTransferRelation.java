// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.BaseStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/*
 *
 * Precision adjustement in CPA Classe, den nachfolger generieren der aktuellen precission
 * prec operator, siehe CPA++
 *
 * Precision lattice as list of Strategies
 *  Alle merken welche Strategien an welcher stelle geblacklisted werden sollen
 *  Wenn sie schon benutzt wurden
 *  Könnte auch als index der Lattice (Liste der Strategien implementiert)
 *
 *
 *  LoopSummaryCPA muss von ArgCPA erben und da die funktion getEdgeToChild überschreiben fürs refinement
 *
 *  Generieren von zwei zuständen im CFA (new DummyCFANode) (sie könnten auch derselbe sein, das wichtige ist die Kante)
 *  mit summary kante zwischen ihnen. Springe vom aktuellen state zum ersten erstellten
 *  State durch anpassen der Location CPA und gehe die kante entlang mittels getAbstractSuccessors und springe vom neuen
 *  Zustand zurück zum eigentlich zustands ende durch anpassen der Location CPA. Siehe bild von Martin
 *      Da Location und Composite States immutable sind, kann man durch einen Visitor Pattern durchlaufen,
 *      falls es eine Location ist tausch sie aus und generier den neuen zustand
 *
 *      Für die CFAEdge muss es eine CExpressionAssignmentStatement und da auf SymbolicLocationPathFormulaBuilder
 *      schauen, da gibt es sowas in die richtung
 *
 *  Ghost CFA in Ghost CFA, also beschleunigung von schleifen in schleifen; Stack Basierter Ansatz wäre sinnvoll
 *
 */

public class LoopSummaryTransferRelation extends AbstractSingleWrapperTransferRelation {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;
  private final LoopSummaryCPAStatistics stats;

  private int baseStrategyPosition = -1;

  @SuppressWarnings("unused")
  private final CFA originalCFA;

  @SuppressWarnings("unused")
  private CFANode startNodeGhostCFA;

  private List<StrategyInterface> strategies;

  @SuppressWarnings("unused")
  private int lookaheadAmntNodes;

  @SuppressWarnings("unused")
  private int lookaheadIterations;

  protected LoopSummaryTransferRelation(
      AbstractLoopSummaryCPA pLoopSummaryCPA,
      ShutdownNotifier pShutdownNotifier,
      List<StrategyInterface> pStrategies,
      int pLookaheadamntnodes,
      int pLookaheaditerations,
      CFA pCfa) {
    super(pLoopSummaryCPA.getWrappedCpa().getTransferRelation());
    stats = pLoopSummaryCPA.getStatistics();
    logger = pLoopSummaryCPA.getLogger();
    shutdownNotifier = pShutdownNotifier;
    strategies = pStrategies;
    lookaheadAmntNodes = pLookaheadamntnodes;
    lookaheadIterations = pLookaheaditerations;
    originalCFA = pCfa;
    startNodeGhostCFA = CFANode.newDummyCFANode("STARTNODEINTERN");
    for (int i = 0; i < this.strategies.size(); i++) {
      if (this.strategies.get(i) instanceof BaseStrategy) {
        baseStrategyPosition = i;
        break;
      }
    }
    assert baseStrategyPosition >= 0 : "The Base Strategy should always be present as Startegy";
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException("Unimplemented");
  }

  private Optional<Collection<? extends AbstractState>> applyStrategyIfAlreadyApplied(
      AbstractState pState, Precision pPrecision, TransferRelation pTransferRelation)
      throws CPATransferException, InterruptedException {
    Optional<Integer> edgeIndex = Optional.empty();
    for (int i = 0; i < AbstractStates.extractLocation(pState).getNumLeavingEdges(); i++) {
      CFAEdge currentEdge = AbstractStates.extractLocation(pState).getLeavingEdge(i);
      if (isGhostEdgeForStrategy(currentEdge, pPrecision)) {
        edgeIndex = Optional.of(i);
        break;
      }
    }
    if (edgeIndex.isEmpty()) {
      return Optional.empty();
    } else {
      List<CFAEdge> removedEdges = new ArrayList<>();
      CFANode node = AbstractStates.extractLocation(pState);
      CFAEdge edge = AbstractStates.extractLocation(pState).getLeavingEdge(edgeIndex.orElseThrow());

      while (node.getNumLeavingEdges() != 0) {
        removedEdges.add(node.getLeavingEdge(0));
        node.removeLeavingEdge(node.getLeavingEdge(0));
      }

      AbstractStates.extractLocation(pState).addLeavingEdge(edge);

      Collection<? extends AbstractState> successors =
          pTransferRelation.getAbstractSuccessors(pState, pPrecision);

      node.removeLeavingEdge(edge);

      for (CFAEdge e : removedEdges) {
        node.addLeavingEdge(e);
      }

      return Optional.of(successors);
    }

  }

  private static boolean isGhostEdgeForStrategy(CFAEdge pCurrentEdge, Precision pPrecision) {
    return pCurrentEdge
        .getRawStatement()
        .equals(
            "true GHOST CFA Strategy " + ((LoopSummaryPrecision) pPrecision).getStrategyCounter());
  }

  private static boolean isGhostEdge(CFAEdge currentEdge) {
    return currentEdge.getRawStatement().startsWith("true GHOST CFA Strategy ");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws InterruptedException, CPATransferException {

    Optional<Collection<? extends AbstractState>> summarizedState =
        applyStrategyIfAlreadyApplied(pState, pPrecision, transferRelation);
    while (summarizedState.isEmpty()) {
      List<CFAEdge> removedEdges = new ArrayList<>();
      int i = 0;
      while (i < AbstractStates.extractLocation(pState).getNumLeavingEdges()) {
        // Remove Edges of Other Strategies in order for the Strategy Calculation to work with the
        // Original CFA
        CFAEdge currentEdge = AbstractStates.extractLocation(pState).getLeavingEdge(i);
        if (isGhostEdge(currentEdge)) {
          removedEdges.add(currentEdge);
          AbstractStates.extractLocation(pState)
              .removeLeavingEdge(removedEdges.get(removedEdges.size() - 1));
        } else {
          i += 1;
        }
      }
      summarizedState =
          strategies
              .get(((LoopSummaryPrecision) pPrecision).getStrategyCounter())
              .summarizeLoopState(
                  pState, ((LoopSummaryPrecision) pPrecision).getPrecision(), transferRelation);
      // Reinsert Removed Edges
      for (CFAEdge e : removedEdges) {
        AbstractStates.extractLocation(pState).addLeavingEdge(e);
      }
      if (summarizedState.isEmpty()) {
        // If the Strategy cannot be applied we see if the next Strategy was someday applied, else
        // we see if we can apply it and generate the ghost CFA
        ((LoopSummaryPrecision) pPrecision).updateStrategy();
        summarizedState = this.applyStrategyIfAlreadyApplied(pState, pPrecision, transferRelation);
      }
    }

    stats.incrementStrategyUsageCount(
        strategies
            .get(((LoopSummaryPrecision) pPrecision).getStrategyCounter())
            .getClass()
            .getSimpleName());

    ((LoopSummaryPrecision) pPrecision)
        .setLoopHead(
            ((LoopSummaryPrecision) pPrecision).getStrategyCounter() != this.baseStrategyPosition);
    return summarizedState.orElseThrow();
  }

}

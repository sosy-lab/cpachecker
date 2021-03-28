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
import java.util.HashMap;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.StrategyInterface;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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

public abstract class AbstractLoopSummaryTransferRelation<EX extends CPAException>
    extends AbstractSingleWrapperTransferRelation implements TransferRelation {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("unused")
  private final CFA originalCFA;

  @SuppressWarnings("unused")
  private CFANode startNodeGhostCFA;

  private ArrayList<StrategyInterface> strategies;
  private Map<CFANode, Integer> currentStrategyForCFANode;

  @SuppressWarnings("unused")
  private int lookaheadAmntNodes;

  @SuppressWarnings("unused")
  private int lookaheadIterations;

  protected AbstractLoopSummaryTransferRelation(
      AbstractLoopSummaryCPA pLoopSummaryCPA,
      ShutdownNotifier pShutdownNotifier,
      ArrayList<StrategyInterface> pStrategies,
      int pLookaheadamntnodes,
      int pLookaheaditerations,
      CFA pCfa) {
    super(pLoopSummaryCPA.getWrappedCpa().getTransferRelation());
    logger = pLoopSummaryCPA.getLogger();
    shutdownNotifier = pShutdownNotifier;
    strategies = pStrategies;
    currentStrategyForCFANode = new HashMap<>();
    lookaheadAmntNodes = pLookaheadamntnodes;
    lookaheadIterations = pLookaheaditerations;
    originalCFA = pCfa;
    startNodeGhostCFA = CFANode.newDummyCFANode("STARTNODEINTERN");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException("Unimplemented");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws InterruptedException, CPATransferException {
    CFANode node = AbstractStates.extractLocation(pState);
    if (!currentStrategyForCFANode.containsKey(node)) {
      currentStrategyForCFANode.put(node, 0);
    }
    /*
     * Problems when executing scripts/cpa.sh -config config/predicateAnalysis--overflow.properties -setprop limits.time.cpu=900s -setprop counterexample.export.enabled=false test/programs/benchmarks/termination-crafted/2Nested-2.c
     * Result is True, while correct result is False
     * Probably something to do with bad refinement
     */

    /*
     *
     * Problem when executing following command line
     * -config config/loop-summary/predicateAnalysis-loopsummary.properties -setprop counterexample.export.enabled=false -timelimit 900s -stats -spec ../../sv-benchmarks/c/properties/no-overflow.prp -64 ../../sv-benchmarks/c/termination-crafted/Arrays01-EquivalentConstantIndices-1.c
     *
     * Problem related to precision adjustment
     *
     */

    Optional<Collection<? extends AbstractState>> summarizedState =
        strategies
            .get(currentStrategyForCFANode.get(node))
            .summarizeLoopState(pState, pPrecision, transferRelation);
    while (summarizedState.isEmpty()) {
      currentStrategyForCFANode.put(node, currentStrategyForCFANode.get(node) + 1);
      summarizedState =
          strategies
              .get(currentStrategyForCFANode.get(node))
              .summarizeLoopState(pState, pPrecision, transferRelation);
    }
    return summarizedState.get();
  }

  /**
   * Return the successor using the wrapped CPA.
   *
   * @param pState current abstract state
   * @param pPrecision current precisionloopOutgoingConditionEdge
   * @param pNode current location
   * @throws EX thrown in subclass
   */
  protected Collection<? extends AbstractState> getWrappedTransferSuccessor(
      final ARGState pState, final Precision pPrecision, final CFANode pNode)
      throws EX, InterruptedException, CPATransferException {
    return transferRelation.getAbstractSuccessors(pState, pPrecision);
  }

  /*
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      Iterable<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {
    shutdownNotifier.shutdownIfNecessary();
    return super.strengthen(pState, pOtherStates, pCfaEdge, pPrecision);
  }
  */
}

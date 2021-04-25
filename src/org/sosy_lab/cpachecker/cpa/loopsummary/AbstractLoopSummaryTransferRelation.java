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
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.AbstractStrategy;
import org.sosy_lab.cpachecker.cpa.loopsummary.strategies.BaseStrategy;
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

  protected AbstractLoopSummaryTransferRelation(
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
    assert baseStrategyPosition >= 0: "The Base Strategy should always be present as Startegy";
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException("Unimplemented");
  }

  private Optional<Collection<? extends AbstractState>> applyStrategyIfAlreadyApplied(
      AbstractState pState, Precision pPrecision) {
    for (int i = 0; i < AbstractStates.extractLocation(pState).getNumLeavingEdges(); i++) {
      if (AbstractStates.extractLocation(pState)
          .getLeavingEdge(i)
          .getRawStatement()
          .equals(
              "true GHOST CFA Strategy "
                  + ((LoopSummaryPrecision) pPrecision).getStrategyCounter())) {
        CFANode startGhotNode =
            AbstractStates.extractLocation(pState).getLeavingEdge(i).getSuccessor();
        // Teleport to the Ghost CFA
        LocationState oldLocationState =
            AbstractStates.extractStateByType(pState, LocationState.class);
        LocationState ghostStartLocationState =
            new LocationState(startGhotNode, oldLocationState.getFollowFunctionCalls());
        AbstractState dummyStateStart =
            AbstractStrategy.overwriteLocationState(pState, ghostStartLocationState);
        ((ARGState) dummyStateStart).addParent((ARGState) pState);
        List<AbstractState> finalStates = new ArrayList<>();
        finalStates.add(dummyStateStart);
        return Optional.of(finalStates);
      }
    }
    return Optional.empty();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws InterruptedException, CPATransferException {

    /*
     *
     * Problem when executing following command line
     * scripts/cpa.sh -config config/loop-summary/predicateAnalysis-loopsummary.properties -setprop counterexample.export.enabled=false -timelimit 900s -stats -spec ../../sv-benchmarks/c/properties/no-overflow.prp -64 ../../sv-benchmarks/c/termination-crafted/Arrays01-EquivalentConstantIndices-1.c
     *
     * Problem related to precision adjustment
     *
     */

    /*
    * When executing
    *
    * scripts/cpa.sh -heap 10000M -config config/loop-summary/predicateAnalysis-loopsummary.properties -setprop counterexample.export.enabled=false -preprocess -timelimit 900s -spec test/programs/benchmarks/properties/no-overflow.prp -64 test/programs/benchmarks/uthash-2.0.2/uthash_JEN_test7-2.c
    *
    * Exception in thread "main" java.lang.AssertionError: Found imprecise counterexample in PredicateCPA. If this is expected for this configuration (e.g., because of UF-based heap encoding), set counterexample.export.allowImpreciseCounterexamples=true. Otherwise please report this as a bug.
       at org.sosy_lab.cpachecker.util.predicates.PathChecker.createImpreciseCounterexample(PathChecker.java:204)
       at org.sosy_lab.cpachecker.util.predicates.PathChecker.handleFeasibleCounterexample(PathChecker.java:126)
       at org.sosy_lab.cpachecker.cpa.predicate.PredicateCPARefiner.performRefinementForPath(PredicateCPARefiner.java:314)
       at org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner.performRefinementForPath(AbstractARGBasedRefiner.java:157)
       at org.sosy_lab.cpachecker.cpa.arg.AbstractARGBasedRefiner.performRefinement(AbstractARGBasedRefiner.java:102)
       at org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.refine(CEGARAlgorithm.java:294)
       at org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.run(CEGARAlgorithm.java:240)
       at org.sosy_lab.cpachecker.core.CPAchecker.runAlgorithm(CPAchecker.java:532)
       at org.sosy_lab.cpachecker.core.CPAchecker.run(CPAchecker.java:399)
       at org.sosy_lab.cpachecker.cmdline.CPAMain.main(CPAMain.java:170)
    *
    */

    /*
         * scripts/cpa.sh -heap 10000M -config config/loop-summary/predicateAnalysis-loopsummary.properties -setprop counterexample.export.enabled=false -timelimit 900s -stats -spec test/programs/benchmarks/properties/no-overflow.prp -64 test/programs/benchmarks/termination-crafted-lit/ChenFlurMukhopadhyay-SAS2012-Ex2.06.c
         *
         * Using summary Strategy: class org.sosy_lab.cpachecker.cpa.loopsummary.strategies.NaiveLoopAcceleration (LoopSummaryCPA:AbstractLoopSummaryTransferRelation.getAbstractSuccessors, INFO)

    Exception in thread "main" java.util.NoSuchElementException
        at java.base/java.util.ArrayList$Itr.next(ArrayList.java:1000)
        at java.base/java.util.Collections$UnmodifiableCollection$1.next(Collections.java:1047)
        at com.google.common.collect.Iterators.getOnlyElement(Iterators.java:301)
        at com.google.common.collect.Iterables.getOnlyElement(Iterables.java:254)
        at org.sosy_lab.cpachecker.cpa.arg.ARGPrecisionAdjustment.elementHasSiblings(ARGPrecisionAdjustment.java:150)
        at org.sosy_lab.cpachecker.cpa.arg.ARGPrecisionAdjustment.prec(ARGPrecisionAdjustment.java:98)
        at org.sosy_lab.cpachecker.cpa.arg.ARGPrecisionAdjustment.prec(ARGPrecisionAdjustment.java:54)
        at org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.handleState(CPAAlgorithm.java:340)
        at org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.run0(CPAAlgorithm.java:273)
        at org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm.run(CPAAlgorithm.java:245)
        at org.sosy_lab.cpachecker.core.algorithm.CEGARAlgorithm.run(CEGARAlgorithm.java:229)
        at org.sosy_lab.cpachecker.core.CPAchecker.runAlgorithm(CPAchecker.java:532)
        at org.sosy_lab.cpachecker.core.CPAchecker.run(CPAchecker.java:399)
        at org.sosy_lab.cpachecker.cmdline.CPAMain.main(CPAMain.java:170)
         *
         */

    /*
     * For some reason busybox-1.22.0/ls-incomplete-2.c cannot be parsed when calling
     * scripts/cpa.sh -heap 10000M -config config/loop-summary/predicateAnalysis-loopsummary.properties -setprop counterexample.export.enabled=false -preprocess -timelimit 900s -spec test/programs/benchmarks/properties/no-overflow.prp -64 test/programs/benchmarks/busybox-1.22.0/ls-incomplete-2.c
     *
     */

    /*
     * How do you do the refinement in this case?
     *
     */
    Optional<Collection<? extends AbstractState>> summarizedState =
        this.applyStrategyIfAlreadyApplied(pState, pPrecision);
    while (summarizedState.isEmpty()) {
      List<CFAEdge> removedEdges = new ArrayList<>();
      int i = 0;
      while (i < AbstractStates.extractLocation(pState).getNumLeavingEdges()) {
        // Remove Edges of Other Strategies in order for the Strategy Calculation to work with the
        // Original CFA
        if (AbstractStates.extractLocation(pState)
            .getLeavingEdge(i)
            .getRawStatement()
            .startsWith("true GHOST CFA Strategy ")) {
          removedEdges.add(AbstractStates.extractLocation(pState).getLeavingEdge(i));
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
        summarizedState = this.applyStrategyIfAlreadyApplied(pState, pPrecision);
      }
    }
    stats.updateSummariesUsed(
        strategies
            .get(((LoopSummaryPrecision) pPrecision).getStrategyCounter())
            .getClass()
            .getName(),
        1);

    ((LoopSummaryPrecision) pPrecision)
        .setLoopHead(
            ((LoopSummaryPrecision) pPrecision).getStrategyCounter() != this.baseStrategyPosition);
    return summarizedState.orElseThrow();
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

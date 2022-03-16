// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.parallel;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PropertyChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.bam.BAMARGBlockStartState;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.propertychecker.NoTargetStateChecker;
import org.sosy_lab.cpachecker.pcc.strategy.SequentialReadStrategy;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Uses ProofChecker interface to check an ARG (certificate) in parallel. Methods used for checking
 * especially those implemented by ProofChecker used in checking must be 1) executable in parallel
 * 2) independent of the order when an ARG state is checked
 */
@Options
public class ARGProofCheckerParallelStrategy extends SequentialReadStrategy {

  private ARGState[] args;
  private ProofChecker checker;
  private PropertyChecker propChecker;

  public ARGProofCheckerParallelStrategy(
      Configuration pConfig, LogManager pLogger, Path pProofFile, @Nullable ProofChecker pChecker)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pProofFile);
    checker = pChecker;
    propChecker = new NoTargetStateChecker();
    if (pChecker instanceof PropertyCheckerCPA) {
      propChecker = ((PropertyCheckerCPA) pChecker).getPropChecker();
    }
  }

  @Override
  public void constructInternalProofRepresentation(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    if (correctReachedSetFormatForProof(pReached)) {
      args = orderBAMBlockStartStates((ARGState) pReached.getFirstState());
      args[args.length - 1] = (ARGState) pReached.getFirstState();
    }
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet)
      throws CPAException, InterruptedException {
    // TODO does not account for strengthen yet (proof check will fail if strengthen is needed to
    // explain successor states)
    // TODO if ARG too small avoid parallel checking, check with less threads

    logger.log(Level.INFO, "Proof check algorithm started");
    try {

      StateCheckingHelper[] helper = new StateCheckingHelper[numThreads - 1];
      Thread[] helperThreads = new Thread[numThreads - 1];
      CyclicBarrier barrier = new CyclicBarrier(numThreads);
      CommonResult result = new CommonResult(numThreads);

      ThreadFactory threadFactory =
          new ThreadFactoryBuilder()
              .setNameFormat("ARGProofCheckerParallelStrategy-checkCertificate-%d")
              .build();
      for (int i = 0; i < helper.length; i++) {
        helper[i] = new StateCheckingHelper(barrier, result, propChecker, checker);
        helperThreads[i] = threadFactory.newThread(helper[i]);
        helperThreads[i].start();
      }

      // check BAMARG blocks
      Block block;
      BAMARGBlockStartState bamState;
      Collection<ARGState> returnNodes;
      List<ARGState> partialReturnNodes = new ArrayList<>();
      List<ARGState> argStates;
      int numElems;
      for (int i = 0; i < args.length - 2; i++) {
        bamState = (BAMARGBlockStartState) args[i];
        block =
            ((BAMCPA) checker)
                .getBlockPartitioning()
                .getBlockForCallNode(AbstractStates.extractLocation(bamState));

        // traverse
        argStates = getARGElements(bamState.getAnalyzedBlock());

        // init checking
        numElems = argStates.size() / numThreads;
        for (int j = 0; j < helper.length; j++) {
          helper[j].setCheckingInfo(j * numElems, numElems, argStates, block);
        }
        barrier.await();

        // check
        for (int j = helper.length * numElems; j < argStates.size(); j++) {
          if (!checkInnerElement(
              propChecker, checker, argStates.get(j), block, partialReturnNodes)) {
            return false;
          }
        }

        result.addReturnNodes(partialReturnNodes);
        partialReturnNodes.clear();

        // wait for every Thread to finish
        returnNodes = result.getResult();
        if (returnNodes == null) {
          // checking failed
          return false;
        }

        // add ARG as checked
        ((BAMCPA) checker).getBamPccManager().setCorrectARG(Pair.of(args[i], block), returnNodes);
      }

      // check main block

      // check root
      ARGState root = args[args.length - 1];

      AbstractState initialState = pReachedSet.popFromWaitlist();

      logger.log(Level.FINE, "Checking root state");

      if (!(checker.isCoveredBy(initialState, root) && checker.isCoveredBy(root, initialState))) {
        return false;
      }

      // traverse
      argStates = getARGElements(root);

      // init checking
      numElems = argStates.size() / numThreads;
      for (int j = 0; j < helper.length; j++) {
        helper[j].setCheckingInfo(j * numElems, numElems, argStates);
      }

      barrier.await();

      // check
      for (int j = helper.length * numElems; j < argStates.size(); j++) {
        if (!checkElement(propChecker, checker, argStates.get(j))) {
          return false;
        }
      }

      // wait for every Thread to finish
      for (int j = 0; j < helper.length; j++) {
        helperThreads[j].join();
        if (!result.isSuccess()) {
          return false;
        }
      }
      returnNodes = result.getResult();
      if (returnNodes == null) {
        // checking failed
        return false;
      }
    } catch (BrokenBarrierException e) {
      logger.log(Level.SEVERE, "Synchronization with barrier faild.");
      return false;
    }
    return true;
  }

  private List<ARGState> getARGElements(ARGState pRoot) {
    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> toVisit = new ArrayDeque<>();
    seen.add(pRoot);
    toVisit.add(pRoot);
    ARGState current;

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (current.isCovered()) {
        if (seen.add(current.getCoveringState())) {
          toVisit.add(current.getCoveringState());
        }
      } else {
        for (ARGState state : current.getChildren()) {
          if (seen.add(state)) {
            toVisit.add(state);
          }
        }
      }
    }

    return new ArrayList<>(seen);
  }

  private static boolean checkInnerElement(
      PropertyChecker propChecker,
      ProofChecker checker,
      ARGState toCheck,
      Block block,
      Collection<ARGState> returnNodes) {
    if (checkElement(propChecker, checker, toCheck)) {
      if (!propChecker.satisfiesProperty(toCheck)) {
        returnNodes.add(toCheck);

        CFANode node = extractLocation(toCheck);
        if (block.isReturnNode(node)) {
          returnNodes.add(toCheck);
        }
      }
      return true;
    }
    return false;
  }

  private static boolean checkElement(
      PropertyChecker propChecker, ProofChecker checker, ARGState toCheck) {
    try {
      if (!propChecker.satisfiesProperty(toCheck)) {
        return false;
      }

      if (toCheck.isCovered()) {
        if (!isCoveringCycleFree(toCheck)) {
          return false;
        }
        if (!checker.isCoveredBy(toCheck, toCheck.getCoveringState())) {
          return false;
        }
      } else {
        Collection<ARGState> successors = toCheck.getChildren();
        if (!checker.areAbstractSuccessors(toCheck, null, successors)) {
          return false;
        }
      }
    } catch (InterruptedException | CPAException e) {
      return false;
    }
    return true;
  }

  private static boolean isCoveringCycleFree(ARGState pState) {
    Set<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) {
        return false;
      }
    }
    return true;
  }

  private boolean correctReachedSetFormatForProof(UnmodifiableReachedSet pReached) {
    if (!(pReached.getFirstState() instanceof ARGState)
        || (extractLocation(pReached.getFirstState()) == null)) {
      logger.log(
          Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return false;
    }
    return true;
  }

  @Override
  protected Object getProofToWrite(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa) {
    constructInternalProofRepresentation(pReached, pCpa);
    return args;
  }

  @Override
  protected void prepareForChecking(Object pReadProof) throws InvalidConfigurationException {
    try {
      stats.getPreparationTimer().start();
      if (!(pReadProof instanceof ARGState[] || ((ARGState[]) pReadProof).length < 1)) {
        throw new InvalidConfigurationException("Proof Strategy requires ARG.");
      }
      args = (ARGState[]) pReadProof;
    } finally {
      stats.getPreparationTimer().stop();
    }
  }

  private ARGState[] orderBAMBlockStartStates(ARGState pMainRoot) {
    Map<BAMARGBlockStartState, Pair<Integer, BitSet>> map = new HashMap<>();
    Deque<BAMARGBlockStartState> blocksToVisit = new ArrayDeque<>();
    int nextIndex = 0;

    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> toVisit = new ArrayDeque<>();
    seen.add(pMainRoot);
    toVisit.add(pMainRoot);
    ARGState current;

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (current.isCovered()) {
        if (seen.add(current.getCoveringState())) {
          toVisit.add(current.getCoveringState());
        }
      } else {

        if (current instanceof BAMARGBlockStartState && !map.containsKey(current)) {
          map.put((BAMARGBlockStartState) current, Pair.of(nextIndex, new BitSet()));
          nextIndex++;
          blocksToVisit.add((BAMARGBlockStartState) current);
        }

        for (ARGState state : current.getChildren()) {
          if (seen.add(state)) {
            toVisit.add(state);
          }
        }
      }
    }

    while (!blocksToVisit.isEmpty()) {
      traverseARG(blocksToVisit.pop(), map, blocksToVisit, nextIndex);
    }
    return topologySort(map);
  }

  private void traverseARG(
      BAMARGBlockStartState pRoot,
      Map<BAMARGBlockStartState, Pair<Integer, BitSet>> graphToComplete,
      Deque<BAMARGBlockStartState> pBlocksToVisit,
      int pNextIndex) {

    Set<ARGState> seen = new HashSet<>();
    Deque<ARGState> toVisit = new ArrayDeque<>();
    seen.add(pRoot.getAnalyzedBlock());
    toVisit.add(pRoot.getAnalyzedBlock());
    ARGState current;

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (current.isCovered()) {
        if (seen.add(current.getCoveringState())) {
          toVisit.add(current.getCoveringState());
        }
      } else {

        if (current instanceof BAMARGBlockStartState) {
          if (!graphToComplete.containsKey(current)) {
            graphToComplete.put((BAMARGBlockStartState) current, Pair.of(pNextIndex, new BitSet()));
            pNextIndex++;
            pBlocksToVisit.add((BAMARGBlockStartState) current);
          }
          graphToComplete.get(pRoot).getSecond().set(graphToComplete.get(current).getFirst());
        }

        for (ARGState state : current.getChildren()) {
          if (seen.add(state)) {
            toVisit.add(state);
          }
        }
      }
    }
  }

  // returns array which is one entry greater than pMap so at last position top most ARG can be
  // added
  private ARGState[] topologySort(Map<BAMARGBlockStartState, Pair<Integer, BitSet>> pMap) {
    ARGState[] result = new ARGState[pMap.size() + 1];

    int nextPos = 0, size = 0;
    List<BAMARGBlockStartState> consider = new ArrayList<>(pMap.keySet());

    while (!consider.isEmpty()) {
      if (size == consider.size()) {
        logger.log(Level.WARNING, "Cannot topology sort ARGs for blocks due to recursion.");
        return new ARGState[1];
      }
      size = consider.size();
      List<Integer> deleteEdges = new ArrayList<>();

      for (int i = consider.size() - 1; i >= 0; i--) {
        if (pMap.get(consider.get(i)).getSecond().cardinality() == 0) {
          deleteEdges.add(pMap.get(consider.get(i)).getFirst());
          result[nextPos] = consider.remove(i);
          nextPos++;
        }
      }

      for (int i = consider.size() - 1; i >= 0; i--) {
        BitSet set = pMap.get(consider.get(i)).getSecond();
        for (int deleteEdge : deleteEdges) {
          set.clear(deleteEdge);
        }
      }
    }

    return result;
  }

  private static class StateCheckingHelper implements Runnable {

    private boolean lastRound;
    private int startCheck;
    private int numElemsToCheck;
    private CyclicBarrier barrier;
    private List<ARGState> states;
    private CommonResult result;
    private PropertyChecker propC;
    private ProofChecker proofC;
    private Block currentB;

    public StateCheckingHelper(
        CyclicBarrier pBarrier,
        CommonResult pResult,
        PropertyChecker pPropCheck,
        ProofChecker pProofCheck) {
      barrier = pBarrier;
      result = pResult;
      propC = pPropCheck;
      proofC = pProofCheck;
    }

    public void setCheckingInfo(int pStartIndex, int pNumberElems, List<ARGState> argStates) {
      lastRound = true;
      startCheck = pStartIndex;
      numElemsToCheck = pNumberElems;
      states = argStates;
    }

    public void setCheckingInfo(
        int pStartIndex, int pNumberElems, List<ARGState> argStates, Block block) {
      lastRound = false;
      startCheck = pStartIndex;
      numElemsToCheck = pNumberElems;
      currentB = block;
      states = argStates;
    }

    @Override
    public void run() {
      int end;
      List<ARGState> returnNodes = new ArrayList<>();
      boolean fail = false;
      try {
        do {
          barrier.await();

          end = startCheck + numElemsToCheck;
          for (int i = startCheck; i < end; i++) {
            if (lastRound) {
              if (!checkElement(propC, proofC, states.get(i))) {
                fail = true;
                break;
              }
            } else {
              if (!checkInnerElement(propC, proofC, states.get(i), currentB, returnNodes)) {
                fail = true;
                break;
              }
            }
          }

          if (!fail) {
            if (!lastRound) {
              result.addReturnNodes(returnNodes);
              returnNodes.clear();
            }
          } else {
            result.setFailure();
          }

        } while (!lastRound && !fail);
      } catch (BrokenBarrierException | InterruptedException e) {
        result.setFailure();
      }
    }
  }

  private static class CommonResult {

    private boolean success = true;
    private int max;
    private int numSetResults;
    private List<ARGState> returnNodes;

    public CommonResult(int maxParticipants) {
      max = maxParticipants;
      numSetResults = 0;
      returnNodes = new ArrayList<>();
    }

    public boolean isSuccess() {
      return success;
    }

    public synchronized void setFailure() {
      success = false;
      increaseSetResults();
    }

    public synchronized void addReturnNodes(Collection<ARGState> partialReturnNodes) {
      returnNodes.addAll(partialReturnNodes);
      increaseSetResults();
    }

    public synchronized Collection<ARGState> getResult() throws InterruptedException {
      try {
        while (numSetResults != max) {
          wait();
        }
        if (!success) {
          return null;
        } else {
          return returnNodes;
        }
      } finally {
        numSetResults = 0;
        returnNodes = new ArrayList<>();
      }
    }

    private void increaseSetResults() {
      numSetResults++;
      if (numSetResults == max) {
        notify();
      }
    }
  }
}

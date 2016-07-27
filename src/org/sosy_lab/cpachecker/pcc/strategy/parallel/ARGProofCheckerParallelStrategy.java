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
package org.sosy_lab.cpachecker.pcc.strategy.parallel;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;

import javax.annotation.Nullable;

/**
 * Uses ProofChecker interface to check an ARG (certificate) in parallel.
 * Methods used for checking especially those implemented by ProofChecker used in checking must be
 * 1) executable in parallel
 * 2) independent of the order when an ARG state is checked
 */
@Options
public class ARGProofCheckerParallelStrategy extends SequentialReadStrategy {

  private ARGState[] args;
  private ProofChecker checker;
  private PropertyChecker propChecker;

  public ARGProofCheckerParallelStrategy(
      Configuration pConfig, LogManager pLogger, @Nullable ProofChecker pChecker)
      throws InvalidConfigurationException {
    super(pConfig, pLogger);
    checker = pChecker;
    propChecker = new NoTargetStateChecker();
    if (pChecker instanceof PropertyCheckerCPA) {
      propChecker = ((PropertyCheckerCPA) pChecker).getPropChecker();
    }
  }

  @Override
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached) {
    if (correctReachedSetFormatForProof(pReached)) {
      args = orderBAMBlockStartStates((ARGState) pReached.getFirstState());
      args[args.length - 1] = (ARGState) pReached.getFirstState();
    }
  }

  @Override
  public boolean checkCertificate(final ReachedSet pReachedSet) throws CPAException, InterruptedException {
    //TODO does not account for strengthen yet (proof check will fail if strengthen is needed to explain successor states)
    // TODO if ARG too small avoid parallel checking, check with less threads

    logger.log(Level.INFO, "Proof check algorithm started");
    try {

      StateCheckingHelper helper[] = new StateCheckingHelper[numThreads - 1];
      Thread helperThreads[] = new Thread[numThreads - 1];
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

      //check BAMARG blocks
      Block block;
      BAMARGBlockStartState bamState;
      Collection<ARGState> returnNodes;
      ArrayList<ARGState> partialReturnNodes = new ArrayList<>();
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
          if (!checkInnerElement(propChecker, checker, argStates.get(j), block, partialReturnNodes)) { return false; }
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
        ((BAMCPA) checker).getBamPccManager().setCorrectARG(Pair.of(args[i], block),
            returnNodes);
      }

      // check main block

      // check root
      ARGState root = args[args.length - 1];

      AbstractState initialState = pReachedSet.popFromWaitlist();

      logger.log(Level.FINE, "Checking root state");

      if (!(checker.isCoveredBy(initialState, root) && checker.isCoveredBy(root, initialState))) { return false; }

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
        if (!checkElement(propChecker, checker, argStates.get(j))) { return false; }
      }

      // wait for every Thread to finish
      for (int j = 0; j < helper.length; j++) {
        helperThreads[j].join();
        if (!result.isSuccess()) { return false; }
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
    HashSet<ARGState> seen = new HashSet<>();
    Stack<ARGState> toVisit = new Stack<>();
    seen.add(pRoot);
    toVisit.add(pRoot);
    ARGState current;

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (current.isCovered()) {
        if (!seen.contains(current.getCoveringState())) {
          seen.add(current.getCoveringState());
          toVisit.add(current.getCoveringState());
        }
      } else {
        for (ARGState state : current.getChildren()) {
          if (!seen.contains(state)) {
            seen.add(state);
            toVisit.add(state);
          }
        }
      }
    }

    return new ArrayList<>(seen);
  }

  private static boolean checkInnerElement(PropertyChecker propChecker, ProofChecker checker, ARGState toCheck,
      Block block, Collection<ARGState> returnNodes) {
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

  private static boolean checkElement(PropertyChecker propChecker, ProofChecker checker, ARGState toCheck) {
    try {
      if (!propChecker.satisfiesProperty(toCheck)) { return false; }

      if (toCheck.isCovered()) {
        if (!isCoveringCycleFree(toCheck)) { return false; }
        if (!checker.isCoveredBy(toCheck, toCheck.getCoveringState())) { return false; }
      } else {
        Collection<ARGState> successors = toCheck.getChildren();
        if (!checker.areAbstractSuccessors(toCheck, null, successors)) { return false; }
      }
    } catch (InterruptedException | CPAException e) {
      return false;
    }
    return true;
  }

  private static boolean isCoveringCycleFree(ARGState pState) {
    HashSet<ARGState> seen = new HashSet<>();
    seen.add(pState);
    while (pState.isCovered()) {
      pState = pState.getCoveringState();
      boolean isNew = seen.add(pState);
      if (!isNew) { return false; }
    }
    return true;
  }

  private boolean correctReachedSetFormatForProof(UnmodifiableReachedSet pReached) {
    if (pReached.getFirstState() == null
        || !(pReached.getFirstState() instanceof ARGState)
        || (extractLocation(pReached.getFirstState()) == null)) {
      logger.log(Level.SEVERE, "Proof cannot be generated because checked property not known to be true.");
      return false;
    }
    return true;
  }

  @Override
  protected Object getProofToWrite(UnmodifiableReachedSet pReached) {
    constructInternalProofRepresentation(pReached);
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
    HashMap<BAMARGBlockStartState, Pair<Integer, BitSet>> map = new HashMap<>();
    Stack<BAMARGBlockStartState> blocksToVisit = new Stack<>();
    int nextIndex = 0;

    HashSet<ARGState> seen = new HashSet<>();
    Stack<ARGState> toVisit = new Stack<>();
    seen.add(pMainRoot);
    toVisit.add(pMainRoot);
    ARGState current;

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (current.isCovered()) {
        if (!seen.contains(current.getCoveringState())) {
          seen.add(current.getCoveringState());
          toVisit.add(current.getCoveringState());
        }
      } else {

        if (current instanceof BAMARGBlockStartState && !map.containsKey(current)) {
          map.put((BAMARGBlockStartState) current, Pair.of(nextIndex, new BitSet()));
          nextIndex++;
          blocksToVisit.add((BAMARGBlockStartState) current);
        }

        for (ARGState state : current.getChildren()) {
          if (!seen.contains(state)) {
            seen.add(state);
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

  private void traverseARG(BAMARGBlockStartState pRoot,
      HashMap<BAMARGBlockStartState, Pair<Integer, BitSet>> graphToComplete,
      Stack<BAMARGBlockStartState> pBlocksToVisit, int pNextIndex) {

    HashSet<ARGState> seen = new HashSet<>();
    Stack<ARGState> toVisit = new Stack<>();
    seen.add(pRoot.getAnalyzedBlock());
    toVisit.add(pRoot.getAnalyzedBlock());
    ARGState current;

    while (!toVisit.isEmpty()) {
      current = toVisit.pop();

      if (current.isCovered()) {
        if (!seen.contains(current.getCoveringState())) {
          seen.add(current.getCoveringState());
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
          if (!seen.contains(state)) {
            seen.add(state);
            toVisit.add(state);
          }
        }

      }
    }
  }

  // returns array which is one entry greater than pMap so at last position top most ARG can be added
  private ARGState[] topologySort(HashMap<BAMARGBlockStartState, Pair<Integer, BitSet>> pMap) {
    ARGState[] result = new ARGState[pMap.size() + 1];

    int nextPos = 0, size = 0;
    ArrayList<Integer> deleteEdges = new ArrayList<>();
    ArrayList<BAMARGBlockStartState> consider = new ArrayList<>(pMap.keySet());
    BitSet set;

    while (consider.size() > 0) {
      if (size == consider.size()) {
        logger.log(Level.WARNING, "Cannot topology sort ARGs for blocks due to recursion.");
        return new ARGState[1];
      }
      size = consider.size();
      deleteEdges.clear();

      for (int i = consider.size() - 1; i >= 0; i--) {
        if (pMap.get(consider.get(i)).getSecond().cardinality() == 0) {
          deleteEdges.add(pMap.get(consider.get(i)).getFirst());
          result[nextPos] = consider.remove(i);
          nextPos++;
        }
      }

      for (int i = consider.size() - 1; i >= 0; i--) {
        set = pMap.get(consider.get(i)).getSecond();
        for (int j = 0; j < deleteEdges.size(); j++) {
          set.clear(deleteEdges.get(j));
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

    public StateCheckingHelper(CyclicBarrier pBarrier, CommonResult pResult, PropertyChecker pPropCheck,
        ProofChecker pProofCheck) {
      this.barrier = pBarrier;
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

    public void setCheckingInfo(int pStartIndex, int pNumberElems, List<ARGState> argStates, Block block) {
      lastRound = false;
      startCheck = pStartIndex;
      numElemsToCheck = pNumberElems;
      currentB = block;
      states = argStates;
    }

    @Override
    public void run() {
      int end;
      ArrayList<ARGState> returnNodes = new ArrayList<>();
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
    private ArrayList<ARGState> returnNodes;

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

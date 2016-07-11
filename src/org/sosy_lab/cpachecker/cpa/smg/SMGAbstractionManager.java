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
package org.sosy_lab.cpachecker.cpa.smg;

import com.google.common.collect.ImmutableSet;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.dls.SMGDoublyLinkedListCandidateFinder;
import org.sosy_lab.cpachecker.cpa.smg.objects.sll.SMGSingleLinkedListFinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public class SMGAbstractionManager {

  private final LogManager logger;
  private final CLangSMG smg;
  private final SMGState smgState;

  private final List<SMGAbstractionCandidate> abstractionCandidates = new ArrayList<>();
  private final Set<SMGAbstractionBlock> blocks;
  private final SMGDoublyLinkedListCandidateFinder dllCandidateFinder;
  private final SMGSingleLinkedListFinder sllCandidateFinder;

  public SMGAbstractionManager(LogManager pLogger, CLangSMG pSMG, SMGState pSMGstate) {
    smg = pSMG;
    smgState = pSMGstate;
    logger = pLogger;
    blocks = ImmutableSet.of();
    dllCandidateFinder = new SMGDoublyLinkedListCandidateFinder();
    sllCandidateFinder = new SMGSingleLinkedListFinder();
  }

  public SMGAbstractionManager(LogManager pLogger, CLangSMG pSMG, SMGState pSMGstate,
      Set<SMGAbstractionBlock> pBlocks) {
    smg = pSMG;
    smgState = pSMGstate;
    logger = pLogger;
    blocks = pBlocks;
    dllCandidateFinder = new SMGDoublyLinkedListCandidateFinder();
    sllCandidateFinder = new SMGSingleLinkedListFinder();
  }

  public SMGAbstractionManager(LogManager pLogger, CLangSMG pSMG, SMGState pSMGstate,
      Set<SMGAbstractionBlock> pBlocks, int equalSeq, int entailSeq, int incSeq) {
    smg = pSMG;
    smgState = pSMGstate;
    logger = pLogger;
    blocks = pBlocks;
    dllCandidateFinder = new SMGDoublyLinkedListCandidateFinder(equalSeq, entailSeq, incSeq);
    sllCandidateFinder = new SMGSingleLinkedListFinder(equalSeq, entailSeq, incSeq);
  }

  private boolean hasCandidates() throws SMGInconsistentException {

    Set<SMGAbstractionCandidate> candidates = dllCandidateFinder.traverse(smg, smgState, blocks);
    abstractionCandidates.addAll(candidates);
    abstractionCandidates.addAll(sllCandidateFinder.traverse(smg, smgState, blocks));

    return (!abstractionCandidates.isEmpty());
  }

  private SMGAbstractionCandidate getBestCandidate() {

    SMGAbstractionCandidate bestCandidate = abstractionCandidates.get(0);

    for (SMGAbstractionCandidate candidate : abstractionCandidates) {
      if (candidate.getScore() > bestCandidate.getScore()) {
        bestCandidate = candidate;
      }
    }

    return bestCandidate;
  }

  public boolean execute() throws SMGInconsistentException {

    SMGAbstractionCandidate currentAbstraction = executeOneStep();

    if (currentAbstraction.isEmpty()) {
      return false;
    }

    while (!currentAbstraction.isEmpty()) {
      currentAbstraction = executeOneStep();
    }

    return true;
  }

  public SMGAbstractionCandidate executeOneStep() throws SMGInconsistentException {

    if (hasCandidates()) {
      SMGAbstractionCandidate best = getBestCandidate();
      logger.log(Level.ALL, "Execute abstraction of ", best);
      best.execute(smg, smgState);
      invalidateCandidates();
      logger.log(Level.ALL, "Finish executing abstraction of ", best);
      return best;
    } else {
      return new SMGAbstractionCandidate() {

        @Override
        public boolean isEmpty() {
          return true;
        }

        @Override
        public int getScore() {
          return 0;
        }

        @Override
        public CLangSMG execute(CLangSMG pSMG, SMGState pSmgState) throws SMGInconsistentException {
          return pSMG;
        }

        @Override
        public SMGAbstractionBlock createAbstractionBlock(SMGState pSmgState) {
          throw new IllegalArgumentException(
              "Can't create abstraction block of empty abstraction candidate.");
        }
      };
    }
  }

  private void invalidateCandidates() {
    abstractionCandidates.clear();
  }
}

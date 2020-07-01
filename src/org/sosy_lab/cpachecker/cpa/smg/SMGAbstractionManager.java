/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.dll.SMGDoublyLinkedListFinder;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.sll.SMGSingleLinkedListFinder;

public class SMGAbstractionManager {

  private final LogManager logger;
  private final CLangSMG smg;
  private final SMGState smgState;

  private final Set<SMGAbstractionBlock> blocks;
  private final SMGDoublyLinkedListFinder dllCandidateFinder;
  private final SMGSingleLinkedListFinder sllCandidateFinder;

  @VisibleForTesting
  public SMGAbstractionManager(LogManager pLogger, CLangSMG pSMG, SMGState pSMGstate) {
    smg = pSMG;
    smgState = pSMGstate;
    logger = pLogger;
    blocks = ImmutableSet.of();
    dllCandidateFinder = new SMGDoublyLinkedListFinder();
    sllCandidateFinder = new SMGSingleLinkedListFinder();
  }

  public SMGAbstractionManager(LogManager pLogger, CLangSMG pSMG, SMGState pSMGstate,
      Set<SMGAbstractionBlock> pBlocks, int equalSeq, int entailSeq, int incSeq) {
    smg = pSMG;
    smgState = pSMGstate;
    logger = pLogger;
    blocks = pBlocks;
    dllCandidateFinder = new SMGDoublyLinkedListFinder(equalSeq, entailSeq, incSeq);
    sllCandidateFinder = new SMGSingleLinkedListFinder(equalSeq, entailSeq, incSeq);
  }

  private List<SMGAbstractionCandidate> getCandidates() throws SMGInconsistentException {
    return ImmutableList.<SMGAbstractionCandidate>builder()
        .addAll(dllCandidateFinder.traverse(smg, smgState, blocks))
        .addAll(sllCandidateFinder.traverse(smg, smgState, blocks))
        .build();
  }

  private SMGAbstractionCandidate getBestCandidate(
      List<SMGAbstractionCandidate> abstractionCandidates) {
    return Collections.max(
        abstractionCandidates, Comparator.comparing(SMGAbstractionCandidate::getScore));
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
    List<SMGAbstractionCandidate> abstractionCandidates = getCandidates();
    if (!abstractionCandidates.isEmpty()) {
      SMGAbstractionCandidate best = getBestCandidate(abstractionCandidates);
      logger.log(Level.ALL, "Execute abstraction of ", best);
      best.execute(smg, smgState);
      logger.log(Level.ALL, "Finish executing abstraction of ", best);
      return best;
    } else {
      return EmptyAbstractionCandidate.INSTANCE;
    }
  }
}

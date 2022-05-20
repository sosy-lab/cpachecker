// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

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

  public SMGAbstractionManager(
      LogManager pLogger,
      CLangSMG pSMG,
      SMGState pSMGstate,
      Set<SMGAbstractionBlock> pBlocks,
      int equalSeq,
      int entailSeq,
      int incSeq) {
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

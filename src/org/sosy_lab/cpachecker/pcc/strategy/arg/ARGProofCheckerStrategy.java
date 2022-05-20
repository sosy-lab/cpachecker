// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy.arg;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.pcc.propertychecker.NoTargetStateChecker;

@Options
public class ARGProofCheckerStrategy extends AbstractARGStrategy {

  private final ProofChecker checker;
  private Set<ARGState> postponedStates;
  private Set<ARGState> waitingForUnexploredParents;
  private Set<ARGState> inWaitlist;


  public ARGProofCheckerStrategy(
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final Path pProofFile,
      final @Nullable ProofChecker pChecker)
      throws InvalidConfigurationException {
    super(pConfig, pLogger, pChecker instanceof PropertyCheckerCPA ? ((PropertyCheckerCPA) pChecker).getPropChecker()
        : new NoTargetStateChecker(), pShutdownNotifier, pProofFile);
    checker = pChecker;
  }

  @Override
  protected void initChecking(final ARGState pRoot) {
    postponedStates = new HashSet<>();
    waitingForUnexploredParents = new HashSet<>();
    inWaitlist = new HashSet<>();
    inWaitlist.add(pRoot);
  }

  @Override
  protected boolean checkForStatePropertyAndOtherStateActions(ARGState pState) {
    inWaitlist.remove(pState);
    return super.checkForStatePropertyAndOtherStateActions(pState);
  }

  @Override
  protected boolean checkCovering(final ARGState pCovered, final ARGState pCovering, final Precision pPrecision) throws CPAException,
      InterruptedException {
    return checker.isCoveredBy(pCovered, pCovering);
  }

  @Override
  protected boolean isCheckSuccessful() {
    return waitingForUnexploredParents.isEmpty();
  }

  @Override
  protected boolean isCheckComplete() {
    return postponedStates.isEmpty();
  }

  @Override
  protected boolean prepareNextWaitlistIteration(final ReachedSet pReachedSet) {
    for (ARGState e : postponedStates) {
      if (!pReachedSet.contains(e.getCoveringState())) {
        logger.log(Level.WARNING, "Covering state", e.getCoveringState(), "was not found in reached set");
        return false;
      }
      pReachedSet.reAddToWaitlist(e);
    }
    postponedStates.clear();
    return true;
  }

  @Override
  protected boolean checkSuccessors(final ARGState pPredecessor, final Collection<ARGState> pSuccessors,
      final Precision pPrecision) throws CPATransferException, InterruptedException {
    return checker.areAbstractSuccessors(pPredecessor, null, pSuccessors);
  }

  @Override
  protected boolean addSuccessors(final Collection<ARGState> pSuccessors, final ReachedSet pReachedSet, final Precision pPrecision) {
    boolean unexploredParent;
    for (ARGState e : pSuccessors) {
      unexploredParent = false;
      for (ARGState p : e.getParents()) {
        if (!pReachedSet.contains(p) || inWaitlist.contains(p)) {
          waitingForUnexploredParents.add(e);
          unexploredParent = true;
          break;
        }
      }
      if (unexploredParent) {
        continue;
      }
      if (pReachedSet.contains(e)) {
        // state unknown parent of e
        logger.log(Level.WARNING, "State", e, "has other parents than", e.getParents());
        return false;
      } else {
        waitingForUnexploredParents.remove(e);
        pReachedSet.add(e, pPrecision);
        inWaitlist.add(e);
      }
    }
    return true;
  }

  @Override
  protected boolean treatStateIfCoveredByUnkownState(ARGState pCovered, ARGState pCoveringState, ReachedSet pReachedSet,
      Precision pPrecision) {
    postponedStates.add(pCovered);
    return true;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.base.Preconditions.checkNotNull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.GhostUpdateRecord;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.ghost.UpdatesRecord;

@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
class ARGToWitnessV2dG extends ARGToYAMLWitness {

  public ARGToWitnessV2dG(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /** Create a {@link GhostUpdateRecord} for a lock/unlock operation between pParent and pChild. */
  private GhostUpdateRecord createGhostUpdate(
      @NonNull ARGState pParent, @NonNull ARGState pChild, int pValue) {

    checkNotNull(pParent);
    checkNotNull(pChild);
    CFAEdge lockEdge = pParent.getEdgeToChild(pChild);
    assert lockEdge != null : "no edge connects " + pParent + " and " + pChild;
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAfterLocation(
            lockEdge.getFileLocation(),
            lockEdge.getSuccessor().getFunctionName(),
            cfa.getAstCfaRelation());
    String lockId = getLockedId(pParent, pChild);
    return new GhostUpdateRecord(locationRecord, List.of(new UpdatesRecord(lockId, pValue)));
  }

  /* TODO
  WitnessExportResult exportWitness(ARGState pRootState, Path pPath) {
    // Collect the information about the states relevant for ghost variables
    CollectedARGStates statesCollector = getRelevantStates(pRootState);

    ImmutableList.Builder<GhostUpdateRecord> ghostUpdates = ImmutableList.builder();
    // Handle ghost updates through locks
    for (var entry : statesCollector.lockUpdates.entries()) {
      ghostUpdates.add(createGhostUpdate(entry.getKey(), entry.getValue(), 1));
    }
    // Handle ghost updates through unlocks
    for (var entry : statesCollector.unlockUpdates.entries()) {
      ghostUpdates.add(createGhostUpdate(entry.getKey(), entry.getValue(), 0));
    }
    return null;
  }*/

  /**
   * Returns the lock String id as used in {@link ThreadingState} that is updated between pParent
   * and pChild and throws an {@link AssertionError} if no lock is found.
   */
  private String getLockedId(ARGState pParent, ARGState pChild) {
    if (pParent.getWrappedState() instanceof ThreadingState parent
        && pChild.getWrappedState() instanceof ThreadingState child) {
      assert parent.locks.size() + 1 == child.locks.size()
          : "there must be exactly one lock update between pParent and pChild";
      for (String lock : child.locks.keySet()) {
        if (!parent.locks.containsKey(lock)) {
          return lock;
        }
      }
    }
    throw new AssertionError("both pParent and pChild must be ThreadingStates");
  }

  /**
   * Returns the lock String id as used in {@link ThreadingState} that is updated between pParent
   * and pChild and throws an {@link AssertionError} if no lock is found.
   */
  private String getUnlockedId(ARGState pParent, ARGState pChild) {
    if (pParent.getWrappedState() instanceof ThreadingState parent
        && pChild.getWrappedState() instanceof ThreadingState child) {
      assert parent.locks.size() == child.locks.size() + 1
          : "there must be exactly one lock update between pParent and pChild";
      for (String lock : parent.locks.keySet()) {
        if (!child.locks.containsKey(lock)) {
          return lock;
        }
      }
    }
    throw new AssertionError("both pParent and pChild must be ThreadingStates");
  }
}

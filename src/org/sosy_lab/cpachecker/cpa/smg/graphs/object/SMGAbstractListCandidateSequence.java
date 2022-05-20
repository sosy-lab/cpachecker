// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownAddressValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;

public abstract class SMGAbstractListCandidateSequence<C extends SMGListCandidate<?>>
    implements SMGAbstractionCandidate {

  protected final C candidate;
  protected final int length;
  private final SMGJoinStatus seqStatus;
  private final boolean includesList;

  protected SMGAbstractListCandidateSequence(
      C pCandidate, int pLength, SMGJoinStatus pSmgJoinStatus, boolean pIncludesList) {
    candidate = pCandidate;
    length = pLength;
    seqStatus = pSmgJoinStatus;
    includesList = pIncludesList;
  }

  public C getCandidate() {
    return candidate;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getScore() {
    int score = getLength() + getStatusScore();

    if (includesList) {
      score = score + 2;
    }

    return score;
  }

  private int getStatusScore() {
    switch (seqStatus) {
      case EQUAL:
        return 50;
      case LEFT_ENTAIL:
        return 31;
      case RIGHT_ENTAIL:
        return 30;
      case INCOMPARABLE:
      default:
        return 0;
    }
  }

  @Override
  public boolean isEmpty() {
    return false;
  }

  protected void addPointsToEdges(
      CLangSMG pSMG, SMGObject targetObject, SMGObject newAbsObj, SMGTargetSpecifier direction) {
    Map<Long, SMGValue> reached = new HashMap<>();
    for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(targetObject, pSMG)) {
      pSMG.removePointsToEdge(pte.getValue());

      if (pte.getTargetSpecifier() == SMGTargetSpecifier.ALL) {
        pSMG.addPointsToEdge(
            new SMGEdgePointsTo(
                pte.getValue(), newAbsObj, pte.getOffset(), SMGTargetSpecifier.ALL));
      } else {
        if (reached.containsKey(pte.getOffset())) {
          pSMG.replaceValue(reached.get(pte.getOffset()), pte.getValue());
        } else {
          SMGEdgePointsTo newPte =
              new SMGEdgePointsTo(pte.getValue(), newAbsObj, pte.getOffset(), direction);
          pSMG.addPointsToEdge(newPte);
          reached.put(newPte.getOffset(), newPte.getValue());
        }
      }
    }
  }

  protected void replaceSourceValues(CLangSMG pSMG, SMGObject pTargetObject) {
    Set<SMGEdgePointsTo> ptes =
        pSMG.getPtEdges(SMGEdgePointsToFilter.targetObjectFilter(pTargetObject));
    for (SMGEdgePointsTo pt : ptes) {
      SMGValue val = pt.getValue();
      if (val instanceof SMGKnownAddressValue) {
        pSMG.replaceValue(SMGKnownSymValue.of(), val);
      }
    }
  }
}

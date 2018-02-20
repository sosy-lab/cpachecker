/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.object;

import java.util.HashMap;
import java.util.Map;
import org.sosy_lab.cpachecker.cpa.smg.SMGAbstractionCandidate;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.SMGUtils;
import org.sosy_lab.cpachecker.cpa.smg.graphs.CLangSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;

public abstract class SMGAbstractListCandidateSequence<C extends SMGListCandidate<?>> implements SMGAbstractionCandidate {

  protected final C candidate;
  protected final int length;
  private final SMGJoinStatus seqStatus;
  private final boolean includesList;

  public SMGAbstractListCandidateSequence(C pCandidate,
      int pLength, SMGJoinStatus pSmgJoinStatus, boolean pIncludesList) {
    candidate = pCandidate;
    length = pLength;
    seqStatus = pSmgJoinStatus;
    includesList = pIncludesList;
  }

  public C getCandidate() {
    return candidate;
  }

  public int getLength() {
    return length;
  }

  @Override
  public int getScore() {
    int score = getLength() + getStatusScore() + getRecursivScore();

    if (includesList) {
      score = score + 2;
    }

    return score;
  }

  private int getRecursivScore() {
    return candidate.hasRecursiveFields() ? 10 : 0;
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

  protected void addPointsToEdges(CLangSMG pSMG, SMGObject targetObject, SMGObject newAbsObj, SMGTargetSpecifier direction) {
    Map<Long, Integer> reached = new HashMap<>();
    for (SMGEdgePointsTo pte : SMGUtils.getPointerToThisObject(targetObject, pSMG)) {
      pSMG.removePointsToEdge(pte.getValue());

      if (pte.getTargetSpecifier() == SMGTargetSpecifier.ALL) {
        SMGEdgePointsTo newPte = new SMGEdgePointsTo(pte.getValue(), newAbsObj, pte.getOffset(),
            SMGTargetSpecifier.ALL);
        pSMG.addPointsToEdge(newPte);
      } else {
        if (reached.containsKey(pte.getOffset())) {
          pSMG.mergeValues(reached.get(pte.getOffset()), pte.getValue());
        } else {
          SMGEdgePointsTo newPte = new SMGEdgePointsTo(pte.getValue(), newAbsObj, pte.getOffset(),
              direction);
          pSMG.addPointsToEdge(newPte);
          reached.put(newPte.getOffset(), newPte.getValue());
        }
      }
    }
  }
}
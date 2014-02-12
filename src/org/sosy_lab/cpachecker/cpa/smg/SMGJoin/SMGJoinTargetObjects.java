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
package org.sosy_lab.cpachecker.cpa.smg.SMGJoin;

import org.sosy_lab.cpachecker.cpa.smg.SMG;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGInconsistentException;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGRegion;

final class SMGJoinTargetObjects {
  private SMGJoinStatus status;
  private boolean defined = false;
  private boolean recoverable = false;
  private SMG inputSMG1;
  private SMG inputSMG2;
  private SMG destSMG;
  private Integer value;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;

  private static boolean matchOffsets(SMGJoinTargetObjects pJto, SMGEdgePointsTo pt1, SMGEdgePointsTo pt2) {
    if (pt1.getOffset() != pt2.getOffset()) {
      pJto.defined = false;
      pJto.recoverable = true;
      return true;
    }

    return false;
  }

  private static boolean checkAlreadyJoined(SMGJoinTargetObjects pJto, SMGObject pObj1, SMGObject pObj2,
                                            Integer pAddress1, Integer pAddress2) {
    if ((! pObj1.notNull()) && (! pObj2.notNull()) ||
        (pJto.mapping1.containsKey(pObj1) && pJto.mapping2.containsKey(pObj2) && pJto.mapping1.get(pObj1) == pJto.mapping2.get(pObj2))) {
      SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(pJto.inputSMG1, pJto.inputSMG2, pJto.destSMG,
                                                        pJto.mapping1, pJto.mapping2,
                                                        pAddress1, pAddress2);
      pJto.defined = true;
      pJto.destSMG = mta.getSMG();
      pJto.mapping1 = mta.getMapping1();
      pJto.mapping2 = mta.getMapping2();
      pJto.value = mta.getValue();
      return true;
    }

    return false;
  }

  private static boolean checkObjectMatch(SMGJoinTargetObjects pJto, SMGObject pObj1, SMGObject pObj2) {
    SMGJoinMatchObjects mo = new SMGJoinMatchObjects(pJto.status, pJto.inputSMG1, pJto.inputSMG2, pJto.mapping1, pJto.mapping2, pObj1, pObj2);
    if (! mo.isDefined()) {
      pJto.defined = false;
      pJto.recoverable = true;
      return true;
    }

    pJto.status = mo.getStatus();
    return false;
  }

  public SMGJoinTargetObjects(SMGJoinStatus pStatus,
                              SMG pSMG1, SMG pSMG2, SMG pDestSMG,
                              SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                              Integer pAddress1, Integer pAddress2) throws SMGInconsistentException {

    inputSMG1 = pSMG1;
    inputSMG2 = pSMG2;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    destSMG = pDestSMG;
    status = pStatus;

    SMGEdgePointsTo pt1 = inputSMG1.getPointer(pAddress1);
    SMGEdgePointsTo pt2 = inputSMG2.getPointer(pAddress2);

    if (SMGJoinTargetObjects.matchOffsets(this, pt1, pt2)) {
      return;
    }

    SMGObject target1 = pt1.getObject();
    SMGObject target2 = pt2.getObject();

    if (SMGJoinTargetObjects.checkAlreadyJoined(this, target1, target2, pAddress1, pAddress2)) {
      return;
    }

    if (SMGJoinTargetObjects.checkObjectMatch(this, target1, target2)) {
      return;
    }

    SMGObject newObject;
    if (target1.isAbstract()) {
      throw new UnsupportedOperationException("Cannot join abstract objects yet");
    } else {
      newObject = new SMGRegion((SMGRegion)target1);
    }
    destSMG.addObject(newObject);

    if (mapping1.containsKey(target1)) {
      throw new UnsupportedOperationException("Delayed join not yet implemented");
    }

    mapping1.map(target1, newObject);
    mapping2.map(target2, newObject);

    SMGJoinMapTargetAddress mta = new SMGJoinMapTargetAddress(inputSMG1, inputSMG2, destSMG, mapping1, mapping2, pAddress1, pAddress2);
    destSMG = mta.getSMG();
    mapping1 = mta.getMapping1();
    mapping2 = mta.getMapping2();
    value = mta.getValue();

    SMGJoinSubSMGs jss = new SMGJoinSubSMGs(status, inputSMG1, inputSMG2, destSMG,
                                            mapping1, mapping2,
                                            target1, target2, newObject);
    if (jss.isDefined()) {
      defined = true;
    }
  }

  public boolean isDefined() {
    return defined;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public SMG getInputSMG1() {
    return inputSMG1;
  }

  public SMG getDestinationSMG() {
    return destSMG;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public Integer getValue() {
    return value;
  }

  public boolean isRecoverable() {
    return recoverable;
  }

  public SMG getInputSMG2() {
    return inputSMG2;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }
}
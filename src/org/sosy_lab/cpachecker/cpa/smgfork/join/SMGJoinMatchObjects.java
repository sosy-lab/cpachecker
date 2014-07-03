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
package org.sosy_lab.cpachecker.cpa.smgfork.join;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smgfork.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smgfork.graphs.ReadableSMG;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smgfork.objects.SMGObject;

import com.google.common.collect.Iterators;

final class SMGJoinMatchObjects {
  private boolean defined = false;
  private SMGJoinStatus status;

  final private static boolean checkNull(SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.notNull() && pObj2.notNull()) {
      return false;
    }

    return true;
  }

  final private static boolean checkMatchingMapping(SMGObject pObj1, SMGObject pObj2,
                                                    SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                                                    ReadableSMG pSMG1, ReadableSMG pSMG2) {
    if (pMapping1.containsKey(pObj1) && pMapping2.containsKey(pObj2) &&
        pMapping1.get(pObj1) != pMapping2.get(pObj2)) {
      return true;
    }

    return false;
  }

  final private static boolean checkConsistentMapping(SMGObject pObj1, SMGObject pObj2,
                                                      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                                                      ReadableSMG pSMG1, ReadableSMG pSMG2) {
    if ((pMapping1.containsKey(pObj1) && pMapping2.containsValue(pMapping1.get(pObj1))) ||
        (pMapping2.containsKey(pObj2) && pMapping1.containsValue(pMapping2.get(pObj2)))) {
      return true;
    }

    return false;
  }

  final private static boolean checkConsistentObjects(SMGObject pObj1, SMGObject pObj2,
                                                      ReadableSMG pSMG1, ReadableSMG pSMG2) {
    if ((pObj1.getSize() != pObj2.getSize()) ||
        (pSMG1.isObjectValid(pObj1) != pSMG2.isObjectValid(pObj2))) {
      return true;
    }

    return false;
  }

  final private static boolean checkConsistentFields(SMGObject pObj1, SMGObject pObj2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      ReadableSMG pSMG1, ReadableSMG pSMG2) {

    List<SMGEdgeHasValue> fields = new ArrayList<>();

    fields.addAll(pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1)));
    fields.addAll(pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2)));

    //TODO: We go through some fields twice, fix
    for (SMGEdgeHasValue hv : fields) {
      Set<SMGEdgeHasValue> hv1 = pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1).filterByType(hv.getType()).filterAtOffset(hv.getOffset()));
      Set<SMGEdgeHasValue> hv2 = pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2).filterByType(hv.getType()).filterAtOffset(hv.getOffset()));
      if (hv1.size() > 0 && hv2.size() > 0) {
        Integer v1 = Iterators.getOnlyElement(hv1.iterator()).getValue();
        Integer v2 = Iterators.getOnlyElement(hv2.iterator()).getValue();
        if (pMapping1.containsKey(v1) && pMapping2.containsKey(v2) && !(pMapping1.get(v1).equals(pMapping2.get(v2)))){
          return true;
        }
      }
    }

    return false;
  }

  private static boolean checkMatchingAbstractions(SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.isAbstract() && pObj2.isAbstract()) {
      SMGAbstractObject pAbstract1 = (SMGAbstractObject)pObj1;
      SMGAbstractObject pAbstract2 = (SMGAbstractObject)pObj2;

      //TODO: It should be possible to join some of the different generic shapes, i.e. a SLL
      //      might be a more general segment than a DLL
      if (! (pAbstract1.matchGenericShape(pAbstract2) && pAbstract1.matchSpecificShape(pAbstract2))) {
          return true;
      }
    }

    return false;
  }

  public SMGJoinMatchObjects(SMGJoinStatus pStatus, ReadableSMG pSMG1, ReadableSMG pSMG2,
                             SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                             SMGObject pObj1, SMGObject pObj2){
    if ((! pSMG1.getObjects().contains(pObj1)) || (! pSMG2.getObjects().contains(pObj2))) {
      throw new IllegalArgumentException();
    }

    if (SMGJoinMatchObjects.checkNull(pObj1, pObj2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkMatchingMapping(pObj1, pObj2, pMapping1, pMapping2, pSMG1, pSMG2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkConsistentMapping(pObj1, pObj2, pMapping1, pMapping2, pSMG1, pSMG2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkConsistentObjects(pObj1, pObj2, pSMG1, pSMG2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkMatchingAbstractions(pObj1, pObj2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkConsistentFields(pObj1, pObj2, pMapping1, pMapping2, pSMG1, pSMG2)){
      return;
    }

    status = SMGJoinMatchObjects.updateStatusForAbstractions(pObj1, pObj2, pStatus);
    defined = true;
  }

  private static SMGJoinStatus updateStatusForAbstractions(SMGObject pObj1, SMGObject pObj2, SMGJoinStatus pStatus) {
    if (pObj1.isMoreGeneral(pObj2)) {
      return SMGJoinStatus.updateStatus(pStatus, SMGJoinStatus.LEFT_ENTAIL);
    } else if (pObj2.isMoreGeneral(pObj1)) {
      return SMGJoinStatus.updateStatus(pStatus, SMGJoinStatus.RIGHT_ENTAIL);
    }
    return pStatus;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public boolean isDefined() {
    return defined;
  }
}

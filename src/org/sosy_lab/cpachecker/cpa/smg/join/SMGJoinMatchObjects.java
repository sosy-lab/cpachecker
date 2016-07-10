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
package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.collect.Iterators;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObjectKind;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class SMGJoinMatchObjects {
  private boolean defined = false;
  private SMGJoinStatus status;

  private static boolean checkNull(SMGObject pObj1, SMGObject pObj2) {
    if (pObj1.notNull() && pObj2.notNull()) {
      return false;
    }

    return true;
  }

  private static boolean checkMatchingMapping(SMGObject pObj1, SMGObject pObj2,
                                                    SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {
    if (pMapping1.containsKey(pObj1) && pMapping2.containsKey(pObj2) &&
        pMapping1.get(pObj1) != pMapping2.get(pObj2)) {
      return true;
    }

    return false;
  }

  private static boolean checkConsistentMapping(SMGObject pObj1, SMGObject pObj2,
                                                      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {
    if ((pMapping1.containsKey(pObj1) && pMapping2.containsValue(pMapping1.get(pObj1))) ||
        (pMapping2.containsKey(pObj2) && pMapping1.containsValue(pMapping2.get(pObj2)))) {
      return true;
    }

    return false;
  }

  private static boolean checkConsistentObjects(SMGObject pObj1, SMGObject pObj2,
                                                      SMG pSMG1, SMG pSMG2) {
    if ((pObj1.getSize() != pObj2.getSize()) ||
        (pSMG1.isObjectValid(pObj1) != pSMG2.isObjectValid(pObj2))) {
      return true;
    }

    return false;
  }

  private static boolean checkConsistentFields(SMGObject pObj1, SMGObject pObj2,
      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
      SMG pSMG1, SMG pSMG2) {

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
        if (pMapping1.containsKey(v1) && pMapping2.containsKey(v2) && !(pMapping1.get(v1).equals(pMapping2.get(v2)))) {
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

        /*An optional object can be matched with dll or sll of the same size.*/
        if(pObj1.getSize() != pObj2.getSize()) {
          return true;
        }

        switch (pObj1.getKind()) {
          case OPTIONAL:
            switch (pObj2.getKind()) {
              case SLL:
              case DLL:
              case OPTIONAL:
                return false;
              default:
                return true;
            }
          case SLL:
          case DLL:
            return pObj2.getKind() != SMGObjectKind.OPTIONAL;
          default:
            return true;
        }
      }
    }

    return false;
  }

  public SMGJoinMatchObjects(SMGJoinStatus pStatus, SMG pSMG1, SMG pSMG2,
                             SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                             SMGObject pObj1, SMGObject pObj2) {
    if ((! pSMG1.getObjects().contains(pObj1)) || (! pSMG2.getObjects().contains(pObj2))) {
      throw new IllegalArgumentException();
    }

    if (SMGJoinMatchObjects.checkNull(pObj1, pObj2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkMatchingMapping(pObj1, pObj2, pMapping1, pMapping2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkConsistentMapping(pObj1, pObj2, pMapping1, pMapping2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkConsistentObjects(pObj1, pObj2, pSMG1, pSMG2)) {
      return;
    }

    if (pObj1.getKind() == pObj2.getKind() && pObj1.isAbstract() && pObj2.isAbstract()) {

      SMGAbstractObject l1 = (SMGAbstractObject) pObj1;
      SMGAbstractObject l2 = (SMGAbstractObject) pObj2;

      if (!l1.matchSpecificShape(l2)) {
        return;
      }
    }

    if (SMGJoinMatchObjects.checkMatchingAbstractions(pObj1, pObj2)) {
      return;
    }

    if (SMGJoinMatchObjects.checkConsistentFields(pObj1, pObj2, pMapping1, pMapping2, pSMG1, pSMG2)) {
      return;
    }

    status = SMGJoinMatchObjects.updateStatusForAbstractions(pObj1, pObj2, pStatus);
    defined = true;
  }

  private static SMGJoinStatus updateStatusForAbstractions(SMGObject pObj1, SMGObject pObj2, SMGJoinStatus pStatus) {
    SMGJoinStatus result = pStatus;

    if (pObj1.join(pObj2, pObj1.getLevel()).isMoreGeneral(pObj2)) {
      result = SMGJoinStatus.updateStatus(result, SMGJoinStatus.LEFT_ENTAIL);
    }

    if (pObj2.join(pObj1, pObj2.getLevel()).isMoreGeneral(pObj1)) {
      result = SMGJoinStatus.updateStatus(result, SMGJoinStatus.RIGHT_ENTAIL);
    }

    return result;
  }

  public SMGJoinStatus getStatus() {
    return status;
  }

  public boolean isDefined() {
    return defined;
  }
}
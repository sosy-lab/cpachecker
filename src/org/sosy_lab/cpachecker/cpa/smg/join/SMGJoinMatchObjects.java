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
package org.sosy_lab.cpachecker.cpa.smg.join;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgeHasValueFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGAbstractObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

final class SMGJoinMatchObjects {
  private boolean defined = false;
  private SMGJoinStatus status;

  private static boolean checkNull(SMGObject pObj1, SMGObject pObj2) {
    return pObj1 == SMGNullObject.INSTANCE || pObj2 == SMGNullObject.INSTANCE;
  }

  private static boolean checkMatchingMapping(SMGObject pObj1, SMGObject pObj2,
                                                    SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {
    return pMapping1.containsKey(pObj1)
        && pMapping2.containsKey(pObj2)
        && pMapping1.get(pObj1) != pMapping2.get(pObj2);
  }

  private static boolean checkConsistentMapping(SMGObject pObj1, SMGObject pObj2,
                                                      SMGNodeMapping pMapping1, SMGNodeMapping pMapping2) {
    return (pMapping1.containsKey(pObj1) && pMapping2.containsValue(pMapping1.get(pObj1)))
        || (pMapping2.containsKey(pObj2) && pMapping1.containsValue(pMapping2.get(pObj2)));
  }

  private static boolean checkConsistentObjects(
      SMGObject pObj1, SMGObject pObj2, UnmodifiableSMG pSMG1, UnmodifiableSMG pSMG2) {
    return pObj1.getSize() != pObj2.getSize()
        || pSMG1.isObjectValid(pObj1) != pSMG2.isObjectValid(pObj2);
  }

  private static boolean checkConsistentShape(SMGObject pObj1, SMGObject pObj2) {
    return pObj1.getKind() == pObj2.getKind()
        && pObj1.isAbstract()
        && pObj2.isAbstract()
        && !((SMGAbstractObject) pObj1).matchSpecificShape((SMGAbstractObject) pObj2);
  }

  private static boolean checkConsistentFields(
      SMGObject pObj1,
      SMGObject pObj2,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      UnmodifiableSMG pSMG1,
      UnmodifiableSMG pSMG2) {

    Set<SMGEdgeHasValue> edges1 = pSMG1.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj1));
    Set<SMGEdgeHasValue> edges2 = pSMG2.getHVEdges(SMGEdgeHasValueFilter.objectFilter(pObj2));

    //TODO: We go through some fields twice, fix
    for (SMGEdgeHasValue hv : Sets.union(edges1, edges2)) {
      // edges are already filtered for given object, just filter again for type and offset.
      SMGEdgeHasValueFilter filter =
          new SMGEdgeHasValueFilter()
              .filterBySize(hv.getSizeInBits())
              .filterAtOffset(hv.getOffset());
      Iterable<SMGEdgeHasValue> hv1 = filter.filter(edges1);
      Iterable<SMGEdgeHasValue> hv2 = filter.filter(edges2);
      if (Iterables.size(hv1) > 0 && Iterables.size(hv2) > 0) {
        SMGValue v1 = Iterators.getOnlyElement(hv1.iterator()).getValue();
        SMGValue v2 = Iterators.getOnlyElement(hv2.iterator()).getValue();
        if (pMapping1.containsKey(v1)
            && pMapping2.containsKey(v2)
            && !pMapping1.get(v1).equals(pMapping2.get(v2))) {
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

      // TODO: It should be possible to join some of the different generic shapes, i.e. a SLL
      //      might be a more general segment than a DLL
      if (!pAbstract1.matchGenericShape(pAbstract2) || !pAbstract1.matchSpecificShape(pAbstract2)) {

        /*An optional object can be matched with dll or sll of the same size.*/
        if (pObj1.getSize() != pObj2.getSize()) {
          return true;
        }

        return pObj1.getKind().isContainedIn(pObj2.getKind());
      }
    }

    return false;
  }

  /** Algorithm 8 from FIT-TR-2012-04 */
  public SMGJoinMatchObjects(
      SMGJoinStatus pStatus,
      UnmodifiableSMG pSMG1,
      UnmodifiableSMG pSMG2,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      SMGObject pObj1,
      SMGObject pObj2) {

    checkArgument(pSMG1.getObjects().contains(pObj1) && pSMG2.getObjects().contains(pObj2));

    // Algorithm 8 from FIT-TR-2012-04, line 1
    if (checkNull(pObj1, pObj2)) {
      return;
    }

    // Algorithm 8 from FIT-TR-2012-04, line 2
    if (checkMatchingMapping(pObj1, pObj2, pMapping1, pMapping2)) {
      return;
    }

    // Algorithm 8 from FIT-TR-2012-04, line 3+4
    if (checkConsistentMapping(pObj1, pObj2, pMapping1, pMapping2)) {
      return;
    }

    // Algorithm 8 from FIT-TR-2012-04, line 5
    if (checkConsistentObjects(pObj1, pObj2, pSMG1, pSMG2)) {
      return;
    }

    // Algorithm 8 from FIT-TR-2012-04, line 6
    if (checkConsistentShape(pObj1, pObj2)) {
      return;
    }

    if (checkMatchingAbstractions(pObj1, pObj2)) {
      return;
    }

    // Algorithm 8 from FIT-TR-2012-04, line 7+8
    if (checkConsistentFields(pObj1, pObj2, pMapping1, pMapping2, pSMG1, pSMG2)) {
      return;
    }

    // Algorithm 8 from FIT-TR-2012-04, line 9+10
    status = updateStatusForAbstractions(pObj1, pObj2, pStatus);
    defined = true;
  }

  private static SMGJoinStatus updateStatusForAbstractions(SMGObject pObj1, SMGObject pObj2, SMGJoinStatus pStatus) {
    SMGJoinStatus result = pStatus;

    if (pObj1.join(pObj2, pObj1.getLevel()).isMoreGeneral(pObj2)) {
      result = result.updateWith(SMGJoinStatus.LEFT_ENTAIL);
    }

    if (pObj2.join(pObj1, pObj2.getLevel()).isMoreGeneral(pObj1)) {
      result = result.updateWith(SMGJoinStatus.RIGHT_ENTAIL);
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
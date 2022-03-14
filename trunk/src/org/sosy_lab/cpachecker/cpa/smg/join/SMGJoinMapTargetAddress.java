// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.join;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import java.util.Set;
import org.sosy_lab.cpachecker.cpa.smg.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.cpa.smg.graphs.SMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.UnmodifiableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.graphs.edge.SMGEdgePointsToFilter;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGNullObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;
import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObjectKind;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGKnownSymValue;
import org.sosy_lab.cpachecker.cpa.smg.graphs.value.SMGValue;

final class SMGJoinMapTargetAddress {
  private final SMG smg;
  @VisibleForTesting final SMGNodeMapping mapping1;
  @VisibleForTesting final SMGNodeMapping mapping2;
  private final SMGValue value;

  /** Algorithm 7 from FIT-TR-2012-04 */
  public SMGJoinMapTargetAddress(
      UnmodifiableSMG pSMG1,
      UnmodifiableSMG pSMG2,
      SMG destSMG,
      SMGNodeMapping pMapping1,
      SMGNodeMapping pMapping2,
      SMGValue pAddress1,
      SMGValue pAddress2) {
    smg = destSMG;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    SMGObject target = SMGNullObject.INSTANCE;

    // TODO: Ugly, refactor
    SMGEdgePointsTo pt = pSMG1.getPointer(pAddress1);
    SMGEdgePointsTo pt2 = pSMG2.getPointer(pAddress2);

    // Algorithm 7 from FIT-TR-2012-04, line 2
    if (pt.getObject() != SMGNullObject.INSTANCE) {
      target = pMapping1.get(pt.getObject());
    }

    SMGTargetSpecifier tg;

    /*When mapping optional object to other abstract object, use tg of other object.*/
    // Algorithm 7 from FIT-TR-2012-04, line 3
    if ((pt.getObject().isAbstract() && pt.getObject().getKind() != SMGObjectKind.OPTIONAL)
        || pt2 == null
        || pt2.getObject().getKind() == SMGObjectKind.OPTIONAL) {
      tg = pt.getTargetSpecifier();
    } else {
      tg = pt2.getTargetSpecifier();
    }

    // Algorithm 7 from FIT-TR-2012-04, line 4
    Set<SMGEdgePointsTo> edges =
        smg.getPtEdges(
            SMGEdgePointsToFilter.targetObjectFilter(target)
                .filterAtTargetOffset(pt.getOffset())
                .filterByTargetSpecifier(tg));

    if (!edges.isEmpty()) {
      value = Iterables.getOnlyElement(edges).getValue();
      return;
    }

    if (pAddress1.equals(pAddress2)) {
      value = pAddress1;
    } else {
      value = SMGKnownSymValue.of();
    }

    // Algorithm 7 from FIT-TR-2012-04, line 5
    smg.addValue(value);

    SMGEdgePointsTo nPtEdge = new SMGEdgePointsTo(value, target, pt.getOffset(), tg);

    smg.addPointsToEdge(nPtEdge);

    // Algorithm 7 from FIT-TR-2012-04, line 6
    mapping1.map(pAddress1, value);
    mapping2.map(pAddress2, value);
  }

  public SMG getSMG() {
    return smg;
  }

  public SMGValue getValue() {
    return value;
  }
}

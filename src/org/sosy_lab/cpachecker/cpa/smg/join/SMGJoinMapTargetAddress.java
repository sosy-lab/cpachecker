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

import java.util.Collection;

import org.sosy_lab.cpachecker.cpa.smg.SMGEdgePointsTo;
import org.sosy_lab.cpachecker.cpa.smg.SMGValueFactory;
import org.sosy_lab.cpachecker.cpa.smg.graphs.ReadableSMG;
import org.sosy_lab.cpachecker.cpa.smg.graphs.WritableSMG;
import org.sosy_lab.cpachecker.cpa.smg.objects.SMGObject;

final class SMGJoinMapTargetAddress {
  private WritableSMG smg;
  private SMGNodeMapping mapping1;
  private SMGNodeMapping mapping2;
  private Integer value;

  public SMGJoinMapTargetAddress(ReadableSMG pSMG1, ReadableSMG pSMG2, WritableSMG destSMG,
                             SMGNodeMapping pMapping1, SMGNodeMapping pMapping2,
                             Integer pAddress1, Integer pAddress2) {
    smg = destSMG;
    mapping1 = pMapping1;
    mapping2 = pMapping2;
    SMGObject target = destSMG.getNullObject();

    // TODO: Ugly, refactor
    SMGEdgePointsTo pt = pSMG1.getPointer(pAddress1);
    if (pt.getObject().notNull()) {
      target = pMapping1.get(pt.getObject());
    }

    // TODO: Ugly, refactor
    Collection<SMGEdgePointsTo> edges = smg.getPTEdges();
    for (SMGEdgePointsTo edge : edges) {
      if ((edge.getObject() == target) &&
          (edge.getOffset() == pt.getOffset())) {
        value = edge.getValue();
        return;
      }
    }

    value = SMGValueFactory.getNewValue();
    smg.addValue(value);
    smg.addPointsToEdge(new SMGEdgePointsTo(value, target, pt.getOffset()));
    mapping1.map(pAddress1, value);
    mapping2.map(pAddress2, value);
  }

  public WritableSMG getSMG() {
    return smg;
  }

  public SMGNodeMapping getMapping1() {
    return mapping1;
  }

  public SMGNodeMapping getMapping2() {
    return mapping2;
  }

  public Integer getValue() {
    return value;
  }
}
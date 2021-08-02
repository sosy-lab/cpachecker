// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.join;

import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cpa.smg.join.SMGJoinStatus;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;
import org.sosy_lab.cpachecker.util.smg.graph.SMGPointsToEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGSymbolicValue;
import org.sosy_lab.cpachecker.util.smg.graph.SMGTargetSpecifier;
import org.sosy_lab.cpachecker.util.smg.graph.SMGValue;

/**
 * Class implementing join algorithm from FIT-TR-2013-4 (Appendix C.3)
 */
public class SMGMapTargetAddress extends SMGAbstractJoinValues {


  public SMGMapTargetAddress(
      SMGJoinStatus pStatus,
      SMG pInputSMG1,
      SMG pInputSMG2,
      SMG pDestSMG,
      NodeMapping pMapping1,
      NodeMapping pMapping2,
      SMGValue pValue1,
      SMGValue pValue2,
      int pNestingLevelDiff) {
    super(pStatus, pInputSMG1, pInputSMG2, pDestSMG, pMapping1, pMapping2);
    mapTargetAddress(pValue1, pValue2);
  }

  private void mapTargetAddress(SMGValue v1, SMGValue v2) {
    // Step 1
    SMGPointsToEdge ptoEdge1 = inputSMG1.getPTEdge(v2);
    SMGPointsToEdge pToEdge2 = inputSMG2.getPTEdge(v2);
    // Step 2
    SMGObject resSmgObject =
        ptoEdge1.pointsTo().isZero()
            ? SMGObject.nullInstance()
            : mapping1.getMappedObject(ptoEdge1.pointsTo());
    // Step 3
    SMGTargetSpecifier tg =
        isDLLS(ptoEdge1.pointsTo()) ? ptoEdge1.targetSpecifier() : pToEdge2.targetSpecifier();

    // Step 4
    Optional<Map.Entry<SMGValue, SMGPointsToEdge>> matchingAddressOptional =
        destSMG.getPTEdgeMapping().entrySet().stream()
            .filter(
                entry ->
                    entry.getValue().getOffset().equals(ptoEdge1.getOffset())
                        && entry.getValue().targetSpecifier().equals(tg)
                        && entry.getValue().pointsTo().equals(resSmgObject))
            .findAny();
    if (matchingAddressOptional.isPresent()) {
      value = matchingAddressOptional.get().getKey();
    } else {
      // Step 5
      value = SMGSymbolicValue.of(0); // TODO nesting level zero???
      destSMG = destSMG.copyAndAddValue(value);
      SMGPointsToEdge newEdge = new SMGPointsToEdge(resSmgObject, ptoEdge1.getOffset(), tg);
      destSMG = destSMG.copyAndAddPTEdge(newEdge, value);
      // Step 6
      mapping1.addMapping(v1, value);
      mapping2.addMapping(v2, value);
    }

  }



}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import java.util.Optional;
import org.sosy_lab.cpachecker.util.smg.exception.SMGInconsistentcyException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/**
 * Utility class for SMG consistency checks.
 */
public final class SMGConsistencyChecker {

  /**
   * Checks consistency of a given SMG.
   *
   * @param smg - the SMG to be checked
   * @throws SMGInconsistentcyException - if the given SMG is inconsistent.
   */
  public static void checkBasicConsistency(SMG smg) throws SMGInconsistentcyException {
    SMGObject nullPointer = smg.getNullObject();
    if (nullPointer.isValid()
        || nullPointer.getSize().intValue() != 0
        || nullPointer.getNestingLevel() != 0
        || nullPointer.getOffset().intValue() != 0) {
      throw new SMGInconsistentcyException(
          "Inconsistent smg: " + smg.toString() + "\n Invalid nullObject");
    }

    checkInvalidRegionConsistency(smg);
    checkValidDLLConsistency(smg);

  }

  /**
   * Valid DLLs consistency check, part of {@link #checkBasicConsistency(SMG)}.
   *
   * @param smg - the SMG to be checked
   */
  private static void checkValidDLLConsistency(SMG smg) {
    Optional<SMGListSegment> invalidDLL = smg.getDLLs().stream().filter(l -> !l.isValid()).findAny();
    if (invalidDLL.isPresent()) {
        throw new SMGInconsistentcyException(
            "Inconsistent smg: "
                + smg.toString()
                + "\n Invalid DLL found: "
                + invalidDLL.toString());

    }
  }

  /**
   * Invalid regions consistency check, part of {@link #checkBasicConsistency(SMG)}.
   *
   * @param smg - the SMG to be checked
   */
  private static void checkInvalidRegionConsistency(SMG smg) {

    for (SMGObject region : smg.getObjects()) {
      if (!region.isValid()) {

        if (!smg.getEdges(region).isEmpty()) {
          throw new SMGInconsistentcyException(
              "Inconsistent smg: "
                  + smg.toString()
                  + "\n Invalid region "
                  + region.toString()
                  + " has outgoing edges.");
        }
      }
    }
  }

}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg;

import java.util.Optional;
import org.sosy_lab.cpachecker.util.smg.exception.SMGInconsistencyException;
import org.sosy_lab.cpachecker.util.smg.graph.SMGDoublyLinkedListSegment;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;
import org.sosy_lab.cpachecker.util.smg.graph.SMGObject;

/** Utility class for SMG consistency checks. */
public final class SMGConsistencyChecker {

  /**
   * Checks consistency of a given SMG.
   *
   * @param smg - the SMG to be checked
   * @throws SMGInconsistencyException - if the given SMG is inconsistent.
   */
  public static void checkBasicConsistency(SMG smg) throws SMGInconsistencyException {
    SMGObject nullPointer = smg.getNullObject();
    if (smg.isValid(nullPointer)
        || nullPointer.getSize().intValue() != 0
        || nullPointer.getNestingLevel() != 0
        || nullPointer.getOffset().intValue() != 0) {
      throw new SMGInconsistencyException("Inconsistent smg: " + smg + "\n Invalid nullObject");
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
    Optional<SMGDoublyLinkedListSegment> invalidDLL =
        smg.getDLLs().stream().filter(l -> !smg.isValid(l)).findAny();
    if (invalidDLL.isPresent()) {
      throw new SMGInconsistencyException(
          "Inconsistent smg: " + smg + "\n Invalid DLL found: " + invalidDLL);
    }
  }

  /**
   * Invalid regions consistency check, part of {@link #checkBasicConsistency(SMG)}.
   *
   * @param smg - the SMG to be checked
   */
  private static void checkInvalidRegionConsistency(SMG smg) {

    for (SMGObject region : smg.getObjects()) {
      if (!smg.isValid(region)) {

        if (!smg.getEdges(region).isEmpty()) {
          throw new SMGInconsistencyException(
              "Inconsistent smg: " + smg + "\n Invalid region " + region + " has outgoing edges.");
        }
      }
    }
  }

  /**
   * Checks field consistency of a given SMG.
   *
   * @param smg - the SMG to be checked
   * @throws SMGInconsistencyException - if the given SMG is inconsistent.
   */
  public static void checkFieldConsistency(SMG smg) throws SMGInconsistencyException {
    smg.getHVEdges().forEach(e -> checkFieldConsistency(smg, e));
  }

  private static void checkFieldConsistency(SMG smg, SMGHasValueEdge edge) {
    if (edge.getSizeInBits().compareTo(edge.getOffset()) <= 0) {
      throw new SMGInconsistencyException(
          "Inconsistent smg: " + smg + "\n Edge " + edge + " points outside of it's field.");
    }
  }
}

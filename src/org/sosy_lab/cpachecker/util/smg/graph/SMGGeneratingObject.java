// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

import java.math.BigInteger;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

/**
 * Used to generate new SMGObjects and Values when read (similar to SLL/DLL, but also usable in
 * arrays). Note: nesting level determines whether the target value is pointed to by all
 * materialized regions, or a new copy is created each time (level == 0 -> all point to the same
 * object).
 */
public class SMGGeneratingObject extends SMGObject {

  protected SMGGeneratingObject(int pNestingLevel, Value pSize, BigInteger pOffset) {
    super(pNestingLevel, pSize, pOffset);
  }

  protected SMGGeneratingObject(int pNestingLevel, Value pSize, BigInteger pOffset, String pName) {
    super(pNestingLevel, pSize, pOffset, pName);
  }

  public static SMGObject copyWithNewSize(SMGGeneratingObject objectToCopy, Value newSize) {
    if (objectToCopy.hasName()) {
      return new SMGGeneratingObject(
          objectToCopy.getNestingLevel(),
          newSize,
          objectToCopy.getOffset(),
          objectToCopy.getName());
    } else {
      return new SMGGeneratingObject(
          objectToCopy.getNestingLevel(), newSize, objectToCopy.getOffset());
    }
  }
}

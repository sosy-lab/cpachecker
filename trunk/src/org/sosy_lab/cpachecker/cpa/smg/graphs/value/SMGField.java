// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** A class to represent a field. This class is mainly used to store field Information. */
public final class SMGField {

  private static final SMGField UNKNOWN =
      new SMGField(SMGUnknownValue.INSTANCE, new CProblemType("unknown"));

  /** the offset of this field relative to the memory this field belongs to. */
  private final SMGExplicitValue offset;

  /**
   * The type of this field, it determines its size and the way information stored in this field is
   * read.
   */
  private final CType type;

  public SMGField(SMGExplicitValue pOffset, CType pType) {
    checkNotNull(pOffset);
    checkNotNull(pType);
    offset = pOffset;
    type = pType;
  }

  public SMGExplicitValue getOffset() {
    return offset;
  }

  public CType getType() {
    return type;
  }

  public boolean isUnknown() {
    return offset.isUnknown() || type instanceof CProblemType;
  }

  @Override
  public String toString() {
    return "offset: " + offset + "Type:" + type.toASTString("");
  }

  public static SMGField getUnknownInstance() {
    return UNKNOWN;
  }
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.smg.graphs.value;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

/** A class to represent a field. This class is mainly used to store field Information. */
public final class SMGField {

  private static final SMGField UNKNOWN =
      new SMGField(SMGUnknownValue.getInstance(), new CProblemType("unknown"));

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


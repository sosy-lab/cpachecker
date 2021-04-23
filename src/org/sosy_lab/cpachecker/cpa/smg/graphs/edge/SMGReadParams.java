// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg.graphs.edge;

import org.sosy_lab.cpachecker.cpa.smg.graphs.object.SMGObject;

public class SMGReadParams {
  private final SMGObject object;
  private final long size;
  private final long offset;

  private SMGReadParams(SMGObject pObject, long pSize, long pOffset) {
    object = pObject;
    size = pSize;
    offset = pOffset;
  }

  public static SMGReadParams of(SMGObject pObject, long pSize, long pOffset) {
    return new SMGReadParams(pObject, pSize, pOffset);
  }

  public SMGObject getObject() {
    return object;
  }

  public long getSize() {
    return size;
  }

  public long getOffset() {
    return offset;
  }
}

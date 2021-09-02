// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.pointer.model;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

public interface PointerStatePrecision {

  Iterable<PointerState.Key> keys(CType pType, MemorySegment pSegment);

  TargetSet union(PointerState.Key pKey, TargetSet pFstTargetSet, TargetSet pSndTargetSet);
}

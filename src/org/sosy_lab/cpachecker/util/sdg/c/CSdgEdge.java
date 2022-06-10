// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg.c;

import org.sosy_lab.cpachecker.util.sdg.AbstractSdgEdge;
import org.sosy_lab.cpachecker.util.sdg.SdgEdge;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class CSdgEdge extends AbstractSdgEdge<MemoryLocation> {

  CSdgEdge(SdgEdge<MemoryLocation> pEdge) {
    super(pEdge);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.util.smg.SMG;
import org.sosy_lab.cpachecker.util.smg.graph.SMGHasValueEdge;

/*
 * This is meant for reads on the SMG and returns a potentially new, maybe the old, SMG, at least 1 read value (maybe more iff the read area covered multiple values) and the information of the edges that were read in context to their offsets and sizes packaged in their HasValueEdges.
 */
public class SMGAndHasValueEdges {

  private final SMG smg;

  private final List<SMGHasValueEdge> hvEdges;

  private SMGAndHasValueEdges(SMG pSmg, List<SMGHasValueEdge> pHvEdges) {
    Preconditions.checkNotNull(pSmg);
    Preconditions.checkNotNull(pHvEdges);
    Preconditions.checkArgument(!pHvEdges.isEmpty());
    smg = pSmg;
    hvEdges = pHvEdges;
  }

  public static SMGAndHasValueEdges of(SMG pSmg, List<SMGHasValueEdge> pHvEdges) {
    return new SMGAndHasValueEdges(pSmg, pHvEdges);
  }

  public static SMGAndHasValueEdges of(SMG pSmg, SMGHasValueEdge pHvEdge) {
    return new SMGAndHasValueEdges(pSmg, ImmutableList.of(pHvEdge));
  }

  public SMG getSMG() {
    return smg;
  }

  public List<SMGHasValueEdge> getHvEdges() {
    return hvEdges;
  }
}

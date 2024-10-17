// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.export.json.serialization;

import com.fasterxml.jackson.databind.util.StdConverter;
import com.google.common.collect.ImmutableList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonData;
import org.sosy_lab.cpachecker.cfa.export.json.CfaJsonExport;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/**
 * A converter that transforms a set of {@link CFAEdge} objects into a sorted list of {@link
 * CFAEdge} objects.
 *
 * <p>The sorting is based on the node numbers of the predecessor and successor nodes of the edges.
 *
 * @see CfaJsonExport
 * @see CfaJsonData
 */
public final class CfaEdgeSetToSortedListConverter
    extends StdConverter<Set<CFAEdge>, List<CFAEdge>> {

  @Override
  public List<CFAEdge> convert(Set<CFAEdge> pSet) {
    return ImmutableList.sortedCopyOf(
        Comparator.comparingInt((CFAEdge edge) -> edge.getPredecessor().getNodeNumber())
            .thenComparingInt(edge -> edge.getSuccessor().getNodeNumber()),
        pSet);
  }
}

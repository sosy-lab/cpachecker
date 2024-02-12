// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * Collection of utility methods for dealing with importing/exporting from/to the invariant store.
 */
public class InvariantStoreUtil {

  /**
   * Returns all nodes that potentially belong to the given file location. Note that "belongs" means
   * that properties (i.e. existence of an invariant in this case) at the node also apply at the
   * file location.
   *
   * @param fileLocation FileLocation to match
   * @param cfa CFA of the program
   * @return All matching nodes
   */
  public static Collection<CFANode> getNodesAtFileLocation(FileLocation fileLocation, CFA cfa) {

    ImmutableSet.Builder<CFANode> result = ImmutableSet.builder();
    for (CFANode candidate : cfa.nodes()) {
      if (candidate instanceof FunctionEntryNode || candidate instanceof FunctionExitNode) {
        // We only consider loop invariants
        continue;
      }

      if (nodeMatchesFileLocation(candidate, fileLocation)) {
        result.add(candidate);
      }
    }

    return result.build();
  }

  /**
   * Heuristic for {@link #getNodesAtFileLocation(FileLocation, CFA)}. A node matches a file
   * location if it has entering edges with statement before (or at) the file location and outgoing
   * edges after (or at) the file location.
   */
  private static boolean nodeMatchesFileLocation(CFANode node, FileLocation fileLocation) {
    boolean existsEdgeBefore =
        CFAUtils.enteringEdges(node)
            .filter(edge -> !edge.getFileLocation().equals(FileLocation.DUMMY))
            .anyMatch(
                edge -> edge.getFileLocation().getNodeOffset() <= fileLocation.getNodeOffset());
    boolean existsEdgeAfter =
        CFAUtils.leavingEdges(node)
            .filter(edge -> !edge.getFileLocation().equals(FileLocation.DUMMY))
            .anyMatch(
                edge -> edge.getFileLocation().getNodeOffset() >= fileLocation.getNodeOffset());

    return existsEdgeBefore && existsEdgeAfter;
  }
}

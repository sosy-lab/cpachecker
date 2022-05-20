// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
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
   * Returns a map that contains an entry for each given file, where an entry is a list that maps
   * each line to its starting offset in the file. The lines are indexed starting with 0. The method
   * reads the given files from disk. So be aware of IO operations and potential failure when the
   * files can not be accessed.
   *
   * <p>For example, the first line has offset 0. If the length of the first line is 5 symbols, then
   * the second line has offset 5.
   *
   * @param filePaths Paths of the file to process
   * @return Immutable map
   * @throws IOException if the files can not be accessed.
   */
  public static ListMultimap<String, Integer> getLineOffsetsByFile(Collection<Path> filePaths)
      throws IOException {
    ImmutableListMultimap.Builder<String, Integer> result = ImmutableListMultimap.builder();

    for (Path filePath : filePaths) {
      if (Files.isRegularFile(filePath)) {
        String fileContent = Files.readString(filePath);

        int currentOffset = 0;
        List<String> sourceLines = Splitter.onPattern("\\n").splitToList(fileContent);
        for (String sourceLine : sourceLines) {
          result.put(filePath.toString(), currentOffset);
          currentOffset += sourceLine.length() + 1;
        }
      }
    }
    return result.build();
  }

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
    for (CFANode candidate : cfa.getAllNodes()) {
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

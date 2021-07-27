// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.invariantwitness.exchange;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class InvariantStoreUtil {
  public static Table<String, Integer, Integer> getLineOffsetsByFile(Collection<Path> filenames)
      throws InvalidConfigurationException {
    ImmutableTable.Builder<String, Integer, Integer> result = ImmutableTable.builder();

    for (Path filename : filenames) {
      if (Files.isRegularFile(filename)) {
        String fileContent;
        try {
          fileContent = Files.readString(filename);
        } catch (IOException pE) {
          throw new InvalidConfigurationException("Can not read source file", pE);
        }

        List<String> sourceLines = Splitter.onPattern("\\n").splitToList(fileContent);
        int currentOffset = 0;
        for (int lineNumber = 0; lineNumber < sourceLines.size(); lineNumber++) {
          result.put(filename.toString(), lineNumber + 1, currentOffset);
          currentOffset += sourceLines.get(lineNumber).length() + 1;
        }
      }
    }
    return result.build();
  }

  public static Collection<CFANode> getNodeCandiadates(FileLocation fileLocation, CFA cfa) {

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

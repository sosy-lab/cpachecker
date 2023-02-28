// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.util.Pair;

public class SequenceGenUtils {
  private static final CharSequence DELIMITER = System.lineSeparator();
  private LogManager logger;

  public SequenceGenUtils(LogManager pLogger) {
    logger = pLogger;
  }

  public List<Pair<Boolean, Integer>> computeSequenceForLoopbound(ARGPath pARGPath) {

    // Check, if the given path is sat by conjoining the path formulae of the abstraction locations.
    // If not, cut off the last part and recursively continue.
    List<Pair<Boolean, Integer>> decisionNodesTaken = new ArrayList<>();

    for (CFAEdge edge : pARGPath.getFullPath()) {
      if (edge instanceof AssumeEdge) {
        AssumeEdge assumeEdge = (AssumeEdge) edge;

        if (assumeEdge.getTruthAssumption()) {
          if (assumeEdge.isSwapped()) {
            decisionNodesTaken.add(Pair.of(false, edge.getLineNumber()));
          } else {

            decisionNodesTaken.add(Pair.of(true, edge.getLineNumber()));
          }
        } else {
          if (assumeEdge.isSwapped()) {
            decisionNodesTaken.add(Pair.of(true, edge.getLineNumber()));
          } else {

            decisionNodesTaken.add(Pair.of(false, edge.getLineNumber()));
          }
        }
      }
    }
    logger.log(Level.INFO, decisionNodesTaken);
    return decisionNodesTaken;
  }

  public void printFileToOutput(List<Pair<Boolean, Integer>> pInputs, Path testcaseName) throws IOException {

    logger.logf(Level.INFO, "Storing the testcase at %s", testcaseName.toAbsolutePath().toString());
    List<String> content = new ArrayList<>();

    content.add(
        String.join(
            DELIMITER,
            pInputs.stream().map(pair-> pair.getSecond().toString() +","+pair.getFirst().toString()).collect(ImmutableList.toImmutableList())));
    IO.writeFile(testcaseName, Charset.defaultCharset(), Joiner.on("\n").join(content));
  }


}

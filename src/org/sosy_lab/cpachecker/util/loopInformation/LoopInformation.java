// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.loopInformation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;

/** This class initiates the information collection from all the loops in the specified program */
@Options(prefix = "LoopInfo")
public class LoopInformation implements StatisticsProvider {

  @Option(
      secure = true,
      name = "logfile",
      description = "Dumps infos about the loops in the tested program to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("LoopInformation.log");

  private CFA cfa;
  private List<LoopData> loopData;
  private final LogManager logger;

  private static final int FIRST_ELEMENT_OF_LIST = 0;
  private static final int VALID_STATE = 0;

  public LoopInformation(Configuration config, LogManager pLogger, CFA cfa)
      throws InvalidConfigurationException {
    logger = checkNotNull(pLogger);
    config.inject(this);

    this.cfa = cfa;
    loopData = new ArrayList<>();
    lookForLoops();
  }

  /**
   * This Method looks for all the loops in the specified program, collects the information and
   * "dumps" a information file in the output that gives the user all of the information
   */
  public void lookForLoops() {

    ImmutableCollection<Loop> allLoops = cfa.getLoopStructure().orElseThrow().getAllLoops();

    for (Loop loop : allLoops) {

      List<CFANode> loopNodes = new ArrayList<>();

      Iterables.addAll(loopNodes, loop.getLoopNodes());

      CFANode loopHead = loopNodes.get(FIRST_ELEMENT_OF_LIST);

      CFAEdge tempEdge = loopHead.getLeavingEdge(VALID_STATE);
      CFANode tempNode = null;
      boolean flag = false;
      if (tempEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)
          || tempEdge.getCode().contains("CPAchecker_TMP")) {
        while (tempEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)
            || tempEdge.getCode().contains("CPAchecker_TMP")) {
          for (int i = 0; i < tempEdge.getSuccessor().getNumLeavingEdges(); i++) {
            if (!tempEdge
                    .getSuccessor()
                    .getLeavingEdge(i)
                    .getEdgeType()
                    .equals(CFAEdgeType.AssumeEdge)
                || tempEdge.getCode().contains("CPAchecker_TMP")) {
              if (tempEdge.getCode().contains("__VERIFIER_nondet_")) {
                tempNode = tempEdge.getSuccessor().getLeavingEdge(1).getSuccessor();
              } else if (!tempEdge.getCode().contains("CPAchecker_TMP")) {
                tempNode = tempEdge.getSuccessor();
              }
            }
          }
          if (!(tempEdge.getCode().contains("CPAchecker_TMP")
              && tempEdge.getCode().contains("=="))) {
            tempEdge = tempEdge.getSuccessor().getLeavingEdge(VALID_STATE);
          } else {
            tempEdge =
                tempEdge
                    .getPredecessor()
                    .getLeavingEdge(1)
                    .getSuccessor()
                    .getLeavingEdge(VALID_STATE);
          }
        }
      } else {
        tempNode = loopHead;
        flag = true;
      }

      if (!(tempNode == null) || flag) {
        loopData.add(new LoopData(loopHead, tempNode, cfa, loopNodes, loop, logger));
      }
    }

    sortLoopDataList();

    if (dumpfile != null) { // option -noout
      try (Writer w = IO.openOutputFile(dumpfile, Charset.defaultCharset())) {
        for (LoopData x : loopData) {
          w.append(x.toString());
          w.append("\n");
        }

      } catch (IOException e) {
        logger.logUserException(
            Level.WARNING, e, "Could not write variable classification to file");
      }
    }
  }

  private void sortLoopDataList() {
    Collections.sort(loopData);
  }

  public List<LoopData> getLoopData() {
    return loopData;
  }

  public CFA getCFA() {
    return cfa;
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(new LoopStatistics(loopData));
  }
}

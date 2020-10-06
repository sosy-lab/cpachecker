/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package LoopAcc;

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

/**
 * This class initiates the information collection from all the loops in the specified program
 */
@Options(prefix = "LoopAcc.LoopInformation")
public class LoopInformation implements StatisticsProvider {

  @Option(secure = true, name = "logfile", description = "Dump LoopInformation to a file.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path dumpfile = Paths.get("LoopInformation.log");

  @Option(secure = true, name = "loopInfo", description = "Get information about all the loops")
  private boolean loopInfo = false;

  private CFA cfa;
  private ArrayList<LoopData> loopData;
  private final LogManager logger;

  private final int FIRST_ELEMENT_OF_LIST = 0;
  private final int VALID_STATE = 0;

  public LoopInformation(Configuration config, LogManager pLogger, CFA cfa)
      throws InvalidConfigurationException {
    logger = checkNotNull(pLogger);
    config.inject(this);

    this.cfa = cfa;
    loopData = new ArrayList<>();
    if (loopInfo) {
    lookForLoops();
  }
  }

  /**
   * This Method looks for all the loops in the specified program, collects the information and
   * "dumps" a information file in the output that gives the user all of the information
   */
  public void lookForLoops() {

    ImmutableCollection<Loop> allLoops = cfa.getLoopStructure().orElseThrow().getAllLoops();

    for (Loop loop : allLoops) {

      ArrayList<CFANode> loopNodes = new ArrayList<>();

      Iterables.addAll(loopNodes, loop.getLoopNodes());

      CFANode loopHead = loopNodes.get(FIRST_ELEMENT_OF_LIST);

      CFAEdge tempEdge = loopHead.getLeavingEdge(VALID_STATE);
      CFANode tempNode = null;
      boolean flag = false;
      if (tempEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
      // suche erste nicht assume edge -> kann eig auch in loopdata ausgelagert werden
      while (tempEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        for(int i = 0; i < tempEdge.getSuccessor().getNumLeavingEdges(); i++) {
          if (!(tempEdge.getSuccessor()
              .getLeavingEdge(i)
              .getEdgeType()
              .equals(CFAEdgeType.AssumeEdge))) {
            tempNode = tempEdge.getSuccessor();
          }
        }
        tempEdge = tempEdge.getSuccessor().getLeavingEdge(VALID_STATE);
      }
    } else {
      tempNode = loopHead;
      flag = true;
    }
    if (!(tempNode == null) || flag) {
      loopData.add(new LoopData(loopHead, tempNode, cfa, loopNodes, loop, logger, flag));
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
        logger
            .logUserException(Level.WARNING, e, "Could not write variable classification to file");
      }
    }
  }

  private void sortLoopDataList() {
    Collections.sort(loopData);
  }

  public ArrayList<LoopData> getLoopData() {
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

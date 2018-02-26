/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.local;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix = "precision")
public class LocalStatistics implements Statistics {
  @Option(description = "A path to a precision output", name = "path", secure = true)
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path outputFileName = Paths.get("localsave");

  private final LogManager logger;

  public LocalStatistics(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    logger = pLogger;
    pConfig.inject(this);
    /*String fName = pConfig.getProperty("precision.path");
    if (fName != null) {
      outputFileName = fName;
    }*/
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    if (pReached.size() <= 2) {
      // evil hack: means we are called from general statistics collector
      // wait until BAM provides its handmade reached set
      return;
    }
    try {
      Map<CFANode, LocalState> reachedStatistics = new TreeMap<>();
      // Path p = Paths.get(outputFileName);
      try (Writer writer =
          Files.newBufferedWriter(Paths.get(outputFileName.toString()), Charset.defaultCharset())) {
        logger.log(Level.FINE, "Write precision to " + outputFileName);
        for (AbstractState state : pReached.asCollection()) {
          CFANode node = AbstractStates.extractLocation(state);
          LocalState lState = AbstractStates.extractStateByType(state, LocalState.class);
          if (!reachedStatistics.containsKey(node)) {
            reachedStatistics.put(node, lState);
          } else {
            LocalState previousState = reachedStatistics.get(node);
            reachedStatistics.put(node, previousState.join(lState));
          }
        }
        for (Map.Entry<CFANode, LocalState> entry : reachedStatistics.entrySet()) {
          writer.append(entry.getKey().toString() + "\n");
          writer.append(entry.getValue().toLog() + "\n");
        }
      }
    } catch (FileNotFoundException e) {
      logger.log(
          Level.SEVERE,
          "Cannot open file " + outputFileName + " for output result of shared analysis");
      return;
    } catch (IOException e) {
      logger.log(Level.SEVERE, e.getMessage());
      return;
    }
  }

  @Override
  public String getName() {
    return "LocalCPA";
  }
}

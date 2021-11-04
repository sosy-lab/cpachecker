// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.local;

import com.google.common.io.MoreFiles;
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
      // As the analysis is used as preanalysis the output directory may not be created
      MoreFiles.createParentDirectories(outputFileName);
      try (Writer writer = Files.newBufferedWriter(outputFileName, Charset.defaultCharset())) {
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
          writer.append(entry.getKey() + "\n");
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

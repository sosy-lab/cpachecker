// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics;

import com.google.common.collect.Iterables;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.sosy_lab.common.JSON;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

/**
 * The StatisticsCPAStatistics implements the Statistics interface and takes care of printing out
 * the analysis results. With the cpa.statistics.statisticsCPAFile you can print out results to a
 * target file in the json format. With the cpa.statistics.printOut option you can print out the
 * results to the standard output in a human readable format.
 */
@Options(prefix = "cpa.statistics")
public class StatisticsCPAStatistics implements Statistics {

  @Option(secure = true, description = "target file to hold the statistics")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path statisticsCPAFile = null;

  private final StatisticsCPA cpa;
  private final LogManager logger;

  public StatisticsCPAStatistics(Configuration config, LogManager logger, StatisticsCPA cpa)
      throws InvalidConfigurationException {
    config.inject(this);
    this.cpa = cpa;
    this.logger = logger;
  }

  @SuppressWarnings("unchecked")
  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    StatisticsData statistics;
    if (cpa.isAnalysis()) {
      statistics = cpa.getFactory().getGlobalAnalysis();
    } else {
      StatisticsState lastState = (StatisticsState) reached.getLastState();
      if (lastState == null) {
        lastState = (StatisticsState) Iterables.getLast(reached);
      }

      statistics = lastState.getStatistics();
    }

    Map<String, Object> jsonMap = new HashMap<>();
    for (Entry<StatisticsProvider, StatisticsDataProvider> entry : statistics) {
      StatisticsProvider provider = entry.getKey();
      StatisticsDataProvider data = entry.getValue();
      String propName = provider.getPropertyName();
      Object value = data.getPropertyValue();
      String mergeInfo = "";
      if (!cpa.isAnalysis()) {
        String mergeType = provider.getMergeType();
        mergeInfo = "_" + mergeType;
        // Save in json with merge type
        Map<String, Object> innerJsonMap;
        if (jsonMap.containsKey(propName)) {
          innerJsonMap = (Map<String, Object>) jsonMap.get(propName);
        } else {
          innerJsonMap = new HashMap<>();
          jsonMap.put(propName, innerJsonMap);
        }
        innerJsonMap.put(mergeType, value);
      } else {
        // Save in json without merge type
        jsonMap.put(propName, value);
      }
      out.println("\t" + propName + mergeInfo + ": " + value);
    }

    if (statisticsCPAFile != null) {
      try {
        JSON.writeJSONString(jsonMap, statisticsCPAFile);
      } catch (IOException e) {
        logger.logUserException(Level.WARNING, e, "Could not write statistics to file");
      }
    }
  }

  @Override
  public String getName() {
    return "StatisticsCPA";
  }
}

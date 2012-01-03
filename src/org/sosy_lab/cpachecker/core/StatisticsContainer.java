/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.CallableInAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ContinuousStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsConsumer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import com.google.common.base.Strings;

@Options(prefix="continuousstatistics")
public class StatisticsContainer implements StatisticsConsumer, Statistics, CallableInAlgorithm, ContinuousStatistics {

  @Option(name="export", description="write some statistics for each algorithm iteration to disk?")
  private boolean exportStatistics = true;

  @Option(name="file", description="write some statistics for each algorithm iteration to disk")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private File exportStatisticsFile = new File("ContinuousStatistics.txt");

  private final List<Statistics> overallStatistics;
  private final List<ContinuousStatistics> continuousStatistics;
  private final List<String> continuousStatisticsColumns;
  private final List<StatisticsContainer> childStatisticsContainer;
  private final Map<ContinuousStatistics, Integer> numberOfColsByStat;
  private final String nameOfContainer;
  private final PrintStream continuousPrintWriter;

  private int statisticSequenceNumber = 0;
  private boolean csvHeaderWritten = false;

  public StatisticsContainer(Configuration pConfig, String pNameOfContainer, StatisticsContainer pParentContainer) throws InvalidConfigurationException {
    pConfig.inject(this);

    this.overallStatistics = new ArrayList<Statistics>();
    this.continuousStatistics = new ArrayList<ContinuousStatistics>();
    this.continuousStatisticsColumns = new ArrayList<String>();
    this.numberOfColsByStat = new HashMap<ContinuousStatistics, Integer>();
    this.childStatisticsContainer = new ArrayList<StatisticsContainer>();
    this.nameOfContainer = pNameOfContainer;

    if (pParentContainer != null) {
      this.continuousPrintWriter = null;
      pParentContainer.addChildStatisticsContainer(this);
    } else {
      try {
        if (exportStatisticsFile != null) {
          com.google.common.io.Files.createParentDirs(exportStatisticsFile);
          this.continuousPrintWriter = new PrintStream(exportStatisticsFile);
        } else {
          this.continuousPrintWriter = null;
        }
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    addContinuousStatistics(this);
  }

  public void printCsvHeader(PrintStream pTargetStream) {
    StringBuilder csvRow = new StringBuilder();

    for (String column : this.continuousStatisticsColumns) {
      csvRow.append(column);
      csvRow.append("\t");
    }

    String line = csvRow.toString();
    if (line.length() > 0) {
      pTargetStream.println(line);
    }
  }

  @Override
  public void addTerminationStatistics(Statistics pStats) {
    this.overallStatistics.add(pStats);
  }

  @Override
  public void addContinuousStatistics(ContinuousStatistics pStats) {
    this.continuousStatistics.add(pStats);

    String[] cols = pStats.announceStatisticColumns();
    if (cols == null) {
      cols = new String[]{};
    }

    for (String col : cols) {
      continuousStatisticsColumns.add(col);
    }
    numberOfColsByStat.put(pStats, cols.length);
  }

  public void appendContinuousStatisticsSnapshot(ReachedSet pReached, PrintStream pTargetStream) {
    if (!csvHeaderWritten) {
      printCsvHeader(pTargetStream);
      csvHeaderWritten = true;
    }

    StringBuilder csvRow = new StringBuilder();
    for (ContinuousStatistics stat : this.continuousStatistics) {
      Object[] statValues = stat.provideStatisticValues(pReached);
      if (statValues.length == numberOfColsByStat.get(stat)) {
        for (Object value : statValues) {
          csvRow.append(value);
          csvRow.append("\t");
        }
      } else {
        throw new RuntimeException("Invalid number of statistics columns provided!");
      }
    }

    String line = csvRow.toString();
    if (line.length() > 0) {
      pTargetStream.println(line);
    }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, ReachedSet pReached) {
    for (Statistics s : overallStatistics) {
      String name = s.getName();
      if (!Strings.isNullOrEmpty(name)) {
        name = name + " statistics";
        pOut.println(name);
        pOut.println(Strings.repeat("-", name.length()));
      }

      s.printStatistics(pOut, pResult, pReached);

      if (!Strings.isNullOrEmpty(name)) {
        pOut.println();
      }
    }
  }

  @Override
  public String getName() {
    return "CPAchecker";
  }

  @Override
  public void call(ReachedSet pReached) {
    if (!exportStatistics) {
      return;
    }

    if (continuousPrintWriter == null) {
      return;
    }

    this.appendContinuousStatisticsSnapshot(pReached, continuousPrintWriter);
    for (StatisticsContainer child : childStatisticsContainer) {
      child.appendContinuousStatisticsSnapshot(pReached, continuousPrintWriter);
    }
  }

  @Override
  public void resetStatistics() {
    this.continuousStatistics.clear();
    this.overallStatistics.clear();
    this.numberOfColsByStat.clear();
    this.continuousStatisticsColumns.clear();
  }

  public void addChildStatisticsContainer(StatisticsContainer pChild) {
    this.childStatisticsContainer.add(pChild);
  }

  @Override
  public String[] announceStatisticColumns() {
    return new String[]{"StatisticName", "SequenceNumber", "TimeInMilliSec"};
  }

  @Override
  public Object[] provideStatisticValues(ReachedSet pReached) {
    return new Object[]{this.nameOfContainer, statisticSequenceNumber++, System.currentTimeMillis()};
  }

}

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
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.ContinuousStatistics;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsConsumer;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;

import com.google.common.base.Strings;

@Options(prefix="ContinuousStatistics")
public class StatisticsContainer implements StatisticsConsumer, Statistics, Runnable {

  private final List<Statistics> overallStatistics;
  private final List<ContinuousStatistics> continuousStatistics;
  private final List<String> continuousStatisticsColumns;
  private final List<StatisticsContainer> childStatisticsContainer;
  private final Map<ContinuousStatistics, Integer> numberOfColsByStat;
  private final String nameOfContainer;
  private PrintStream continuousPrintWriter = null;
  private int statisticSequenceNumber = 0;
  private boolean csvHeaderWritten = false;

  @Option(name="export", description="write some statistics for each algorithm iteration to disk?")
  private boolean exportStatistics = true;

  @Option(name="file", type=Option.Type.OUTPUT_FILE,
      description="write some statistics for each algorithm iteration to disk")
  private File exportStatisticsFile = new File("ContinuousStatistics.txt");

  public StatisticsContainer(String pNameOfContainer, StatisticsContainer pParentContainer) {
    this.overallStatistics = new ArrayList<Statistics>();
    this.continuousStatistics = new ArrayList<ContinuousStatistics>();
    this.continuousStatisticsColumns = new ArrayList<String>();
    this.numberOfColsByStat = new HashMap<ContinuousStatistics, Integer>();
    this.childStatisticsContainer = new ArrayList<StatisticsContainer>();
    this.nameOfContainer = pNameOfContainer;

    continuousStatisticsColumns.add("StatisticName");
    continuousStatisticsColumns.add("SequenceNumber");
    continuousStatisticsColumns.add("TimeInMilliSec");


    if (pParentContainer != null) {
      pParentContainer.addChildStatisticsContainer(this);
    } else {
      try {
        if (exportStatisticsFile != null) {
          continuousPrintWriter = new PrintStream(exportStatisticsFile);
        } else {
          continuousPrintWriter = null;
        }
      } catch (FileNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
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
  public void addTerminationStatistics(Statistics[] pStats) {
    for (Statistics s : pStats) {
      this.overallStatistics.add(s);
    }
  }

  @Override
  public void addContinuousStatistics(ContinuousStatistics[] pStats) {
    for (ContinuousStatistics s : pStats) {
      this.continuousStatistics.add(s);

      String[] cols = s.announceStatisticColumns();
      for (String col : cols) {
        continuousStatisticsColumns.add(col);
      }
      numberOfColsByStat.put(s, cols.length);
    }
  }

  public void appendContinuousStatisticsSnapshot(PrintStream pTargetStream) {
    if (!csvHeaderWritten) {
      printCsvHeader(pTargetStream);
      csvHeaderWritten = true;
    }

    StringBuilder csvRow = new StringBuilder();
    csvRow.append(this.getName());
    csvRow.append("\t");
    csvRow.append(statisticSequenceNumber++);
    csvRow.append("\t");
    csvRow.append(System.currentTimeMillis());
    csvRow.append("\t");

    for (ContinuousStatistics stat : this.continuousStatistics) {
      Object[] statValues = stat.provideStatisticValues();
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
  public void run() {
    if (!exportStatistics) {
      return;
    }

    if (continuousPrintWriter == null) {
      return;
    }

    this.appendContinuousStatisticsSnapshot(continuousPrintWriter);

    for (StatisticsContainer child : childStatisticsContainer) {
      child.appendContinuousStatisticsSnapshot(continuousPrintWriter);
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

}

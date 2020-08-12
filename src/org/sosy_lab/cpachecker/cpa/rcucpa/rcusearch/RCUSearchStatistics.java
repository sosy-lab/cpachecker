/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.rcusearch")
public class RCUSearchStatistics implements Statistics {

  public static class RCUSearchStateStatistics {

    final StatTimer equalsTimer = new StatTimer("Overall time for equals check");
    final StatTimer equalsPointerTimer = new StatTimer("Time for pointer equals check");
    final StatTimer joinTimer = new StatTimer("Time for join states");
    final StatTimer joinPointerTimer = new StatTimer("Time for join pointer states");
    final StatTimer lessOrEqualsTimer = new StatTimer("Time for isLessOrEquals");
    final StatTimer lessOrEqualsPointerTimer =
        new StatTimer("Time for isLessOrEquals of pointer states");

    private final static RCUSearchStateStatistics instance = new RCUSearchStateStatistics();

    private RCUSearchStateStatistics() {
    }

    public void printStatistics(StatisticsWriter writer) {
      writer.beginLevel()
          .put(equalsTimer)
          .beginLevel()
          .put(equalsPointerTimer)
          .endLevel()
          .put(joinTimer)
          .beginLevel()
          .put(joinPointerTimer)
          .endLevel()
          .put(lessOrEqualsTimer)
          .beginLevel()
          .put(lessOrEqualsPointerTimer)
          .endLevel();
    }

    public static RCUSearchStateStatistics getInstance() {
      return instance;
    }
  }

  @Option(secure = true, name = "output", description = "name of a file to hold information about"
      + " RCU pointers and their aliases")
  @FileOption(Type.OUTPUT_FILE)
  private Path output = Paths.get("RCUPointers");

  private final LogManager logger;
  final StatTimer transferTimer = new StatTimer("Overall time for transfer relation");
  final StatTimer rcuSearchTimer = new StatTimer("Time for RCU search part");
  final StatTimer pointerTimer = new StatTimer("Time for pointer analysis");
  final StatTimer reducerTimer = new StatTimer("Overall time for reducer");
  final StatTimer rcuSearchReducerTimer = new StatTimer("Time for RCU search part");
  final StatTimer pointerReducerTimer = new StatTimer("Time for pointer analysis");

  final StatInt rcuPointers = new StatInt(StatKind.SUM, "Number of RCU pointers");
  final StatInt rcuAliases = new StatInt(StatKind.SUM, "Number of RCU aliases");

  RCUSearchStatistics(Configuration config, LogManager pLogger) throws
                                                                 InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);
  }

  @Override
  @SuppressWarnings("serial")
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {

    Set<MemoryLocation> rcuAndAliases = new TreeSet<>();

    // TODO: BAM specifics?
    for (AbstractState state : reached) {
      RCUSearchState searchState = AbstractStates.extractStateByType(state, RCUSearchState.class);
      if (searchState != null) {
        rcuAndAliases.addAll(searchState.getRcuPointers());
        PointerState pState = (PointerState) searchState.getWrappedState();
        rcuAndAliases.addAll(getAliases(pState, searchState.getRcuPointers()));
      }
    }

    if (output != null) {
      // May be disabled
      try (Writer writer = Files.newBufferedWriter(output, Charset.defaultCharset())) {
        for (MemoryLocation loc : rcuAndAliases) {
          writer.write(loc.toString() + "\n");
        }
        logger.log(Level.INFO, "Ended dump of RCU-aliases in file " + output);
      } catch (IOException pE) {
        logger.log(Level.WARNING, pE.getMessage());
      }
    }
    String info = "";
    info += "Number of fictional pointers:  " + getFictionalPointersNumber(rcuAndAliases) + "\n";
    out.append(info);
    logger.log(Level.ALL, "RCU with aliases: " + rcuAndAliases);
    StatisticsWriter writer = StatisticsWriter.writingStatisticsTo(out);
    writer.beginLevel()
        .put(rcuPointers)
        .put(rcuAliases)
        .spacer()
        .put(transferTimer)
        .beginLevel()
        .put(rcuSearchTimer)
        .put(pointerTimer)
        .endLevel()
        .put(reducerTimer)
        .beginLevel()
        .put(rcuSearchReducerTimer)
        .put(pointerReducerTimer)
        .endLevel()
        .endLevel()
        .spacer();
    RCUSearchStateStatistics.getInstance().printStatistics(writer);
  }

  @Nullable
  @Override
  public String getName() {
    return "RCU Search";
  }

  private Collection<MemoryLocation>
      getAliases(PointerState pState, Set<MemoryLocation> targetPointers) {

    rcuPointers.setNextValue(targetPointers.size());
    Collection<MemoryLocation> result = new TreeSet<>();
    Set<MemoryLocation> overallLocations = pState.getTrackedMemoryLocations();

    for (MemoryLocation pointer : targetPointers) {
      LocationSet pointerPointTo = pState.getPointsToSet(pointer);
      if (pointerPointTo == LocationSetTop.INSTANCE) {
        // pointer can point anywhere
        result.addAll(overallLocations);
        break;
      } else if (pointerPointTo != LocationSetBot.INSTANCE) {
        for (MemoryLocation rcuLoc : (ExplicitLocationSet) pointerPointTo) {
          for (MemoryLocation other : overallLocations) {
            if (pState.mayPointTo(other, rcuLoc)) {
              result.add(other);
            }
          }
        }
      }
    }

    rcuAliases.setNextValue(result.size());
    return result;
  }

  private int getFictionalPointersNumber(Set<MemoryLocation> ptrs) {
    int result = 0;
    for (MemoryLocation iter : ptrs) {
      if (PointerState.isFictionalPointer(iter)) {
        ++result;
      }
    }
    return result;
  }
}

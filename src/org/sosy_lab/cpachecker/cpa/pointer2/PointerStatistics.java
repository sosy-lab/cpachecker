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
package org.sosy_lab.cpachecker.cpa.pointer2;

//import com.jsoniter.DecodingMode;
//import com.jsoniter.JsonIterator;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.annotation.Nullable;
import org.sosy_lab.common.JSON;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Reducer;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetBot;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSetTop;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class PointerStatistics implements Statistics {

  private Path path = Paths.get("PointsToMap");
  private boolean noOutput = true;
  private final PointerTransferRelation transfer;
  private final PointerReducer reducer;

  private static final MemoryLocation replLocSetTop = MemoryLocation.valueOf("_LOCATION_SET_TOP_");
  private static final MemoryLocation replLocSetBot = MemoryLocation.valueOf("_LOCATION_SET_BOT_");

  PointerStatistics(boolean pNoOutput, Path pPath, TransferRelation tr, Reducer rd) {
    noOutput = pNoOutput;
    path = pPath;
    transfer = (PointerTransferRelation) tr;
    reducer = (PointerReducer) rd;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    AbstractState state = reached.getLastState();
    PointerState ptState = AbstractStates.extractStateByType(state, PointerState.class);
    String stats = "Common part" + '\n';

    if (ptState != null) {
      Map<MemoryLocation, LocationSet> locationSetMap = ptState.getPointsToMap();

      if (locationSetMap != null) {

        Map<MemoryLocation, Set<MemoryLocation>> pointsTo = replaceTopsAndBots(locationSetMap);

        int values = 0;
        int fictionalKeys = 0;
        int fictionalValues = 0;

        for (Entry<MemoryLocation, Set<MemoryLocation>> entry : pointsTo.entrySet()) {
          Set<MemoryLocation> buf = entry.getValue();
          values += buf.size();
          for (MemoryLocation location : buf) {
            if (PointerState.isFictionalPointer(location)) {
              ++fictionalValues;
            }
          }
          if (PointerState.isFictionalPointer(entry.getKey())) {
            ++fictionalKeys;
          }
        }

        stats += "  Points-To map size:         " + pointsTo.size() + '\n';
        stats += "  Fictional keys:             " + fictionalKeys + '\n';
        stats += "  Points-To map values size:  " + values + '\n';
        stats += "  Fictional values:           " + fictionalValues + '\n';

        if (!noOutput) {
          try (Writer writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            JSON.writeJSONString(pointsTo, writer);
          } catch (IOException pE) {
            stats += "  IOError: " + pE.getMessage() + '\n';
          }
        } else {
          stats += "  Points-To map output is disabled" + '\n';
        }

      } else {
        out.append("  Empty pointTo\n");
      }
    } else {
      out.append("  Last state of PointerCPA is not of PointerState class");
    }

    stats += "\n  Time for transfer relation:         " + transfer.transferTime + '\n';
    stats += "  Time for equality checks:       " + transfer.equalityTime + '\n';
    stats += "  Reduce time:  " + reducer.reduceTime + '\n';
    stats += "  Expand time:  " + reducer.expandTime + '\n';

    out.append(stats);
    out.append('\n');
  }

  public static Map<MemoryLocation, Set<MemoryLocation>> replaceTopsAndBots(Map<MemoryLocation,
                                                              LocationSet> pPointsTo) {
    Map<MemoryLocation, Set<MemoryLocation>> result = new TreeMap<>();
    for (Entry<MemoryLocation, LocationSet> entry : pPointsTo.entrySet()) {
      LocationSet locationSet = entry.getValue();
      Set<MemoryLocation> set;

      if (locationSet instanceof LocationSetBot) {
        set = Collections.singleton(replLocSetBot);
      } else if (locationSet instanceof LocationSetTop) {
        set = Collections.singleton(replLocSetTop);
      } else {
        set = new TreeSet<>();
        for (MemoryLocation loc : (ExplicitLocationSet) locationSet) {
          set.add(loc);
        }
      }
      result.put(entry.getKey(), set);
    }

    return result;
  }

  public static MemoryLocation getReplLocSetTop() {
    return replLocSetTop;
  }

  public static MemoryLocation getReplLocSetBot() {
    return replLocSetBot;
  }

  @Nullable
  @Override
  public String getName() {
    return "Points-To";
  }

}

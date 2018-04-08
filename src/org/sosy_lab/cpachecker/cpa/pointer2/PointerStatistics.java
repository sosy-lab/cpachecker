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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
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

        for (MemoryLocation key : pointsTo.keySet()) {
          Set<MemoryLocation> buf = pointsTo.get(key);
          values += buf.size();
          for (MemoryLocation location : buf) {
            if (PointerState.isFictionalPointer(location)) {
              ++fictionalValues;
            }
          }
          if (PointerState.isFictionalPointer(key)) {
            ++fictionalKeys;
          }
        }

        stats += "  Points-To map size:         " + pointsTo.size() + '\n';
        stats += "  Fictional keys:             " + fictionalKeys + '\n';
        stats += "  Points-To map values size:  " + values + '\n';
        stats += "  Fictional values:           " + fictionalValues + '\n';

        if (!noOutput) {
          try (Writer writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            Gson builder = new Gson();
            java.lang.reflect.Type type = new TypeToken<Map<MemoryLocation, Set<MemoryLocation>>>(){}
            .getType();
            builder.toJson(pointsTo, type, writer);
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

    stats += "\n  Time for edge handling:         " + transfer.handlingTime + '\n';
    stats += "  Time for equality checks:       " + transfer.equalityTime + '\n';
    stats += "  Time for determining pointsTo:  " + PointerTransferRelation.pointsToTime + '\n';
    stats += "  Time for strengthen operator:   " + PointerTransferRelation.strengthenTime + '\n';

    stats += "BAM Part" + '\n';
    stats += "  Reduce time:  " + reducer.reduceTime + '\n';
    stats += "  Expand time:  " + reducer.expandTime + '\n';

    out.append(stats);
    out.append('\n');
  }

  public static Map<MemoryLocation, Set<MemoryLocation>> replaceTopsAndBots(Map<MemoryLocation,
                                                              LocationSet> pPointsTo) {
    Map<MemoryLocation, Set<MemoryLocation>> result = new HashMap<>();
    for (MemoryLocation key : pPointsTo.keySet()) {
      LocationSet locationSet = pPointsTo.get(key);
      if (locationSet instanceof LocationSetBot) {
        result.put(key, Collections.singleton(replLocSetBot));
      } else if (locationSet instanceof LocationSetTop) {
        result.put(key, Collections.singleton(replLocSetTop));
      } else {
        Set<MemoryLocation> buf = new HashSet<>();
        Iterator<MemoryLocation> iter = ((ExplicitLocationSet) locationSet).iterator();
        while (iter.hasNext()) {
          buf.add(iter.next());
        }
        result.put(key, buf);
      }
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

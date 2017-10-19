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
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
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

  private static final MemoryLocation replLocSetTop = MemoryLocation.valueOf("_LOCATION_SET_TOP_");
  private static final MemoryLocation replLocSetBot = MemoryLocation.valueOf("_LOCATION_SET_BOT_");

  PointerStatistics(boolean pNoOutput, Path pPath) {
    noOutput = pNoOutput;
    path = pPath;
  }

  @Override
  public void printStatistics(PrintStream out, Result result, UnmodifiableReachedSet reached) {
    AbstractState state = reached.getLastState();
    PointerState ptState = AbstractStates.extractStateByType(state, PointerState.class);
    boolean err = false;

    if (ptState != null) {
      Map<MemoryLocation, LocationSet> pointsTo = ptState.getPointsToMap();

      if (pointsTo != null) {

        pointsTo = replaceTopsAndBots(pointsTo);

        if (!noOutput) {
          try (Writer writer = Files.newBufferedWriter(path, Charset.defaultCharset())) {
            Gson builder = new Gson();
            java.lang.reflect.Type type = new TypeToken<Map<MemoryLocation, LocationSet>>(){}.getType();
            builder.toJson(pointsTo, type, writer);
          } catch (IOException pE) {
            err = true;
          }
        }

        int values = 0;

        for (MemoryLocation key : pointsTo.keySet()) {
          values += ((ExplicitLocationSet) pointsTo.get(key)).getSize();
        }

        String stats = "Points-To map size: " + pointsTo.size() + '\n' +
                        "Points-To map values size: " + values + '\n' +
                        "IO Errors: " + err + '\n';

        out.append(stats);
        out.append('\n');
      } else {
        out.append("Empty pointTo\n");
      }
    } else {
      out.append("Last state of PointerCPA is not of PointerState class");
    }
  }

  private Map<MemoryLocation, LocationSet> replaceTopsAndBots(Map<MemoryLocation,
                                                              LocationSet> pPointsTo) {
    Map<MemoryLocation, LocationSet> result = new HashMap<>(pPointsTo);
    for (MemoryLocation key : result.keySet()) {
      LocationSet locationSet = result.get(key);
      if (locationSet instanceof LocationSetBot) {
        result.put(key, ExplicitLocationSet.from(replLocSetBot));
      } else if (locationSet instanceof LocationSetTop) {
        result.put(key, ExplicitLocationSet.from(replLocSetTop));
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

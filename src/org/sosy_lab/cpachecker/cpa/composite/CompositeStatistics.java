/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.composite;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.rcucpa.rcusearch.RCUSearchState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;

public class CompositeStatistics implements Statistics {
  @Override
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {
    Map<LocationState, Map<CallstackState, List<RCUSearchState>>> states = new HashMap<>();
    String output = "";

    for (AbstractState state : reached) {
      LocationState loc = AbstractStates.extractStateByType(state, LocationState.class);
      CallstackState call = AbstractStates.extractStateByType(state, CallstackState.class);
      if (loc != null && call != null) {
        states.putIfAbsent(loc, new HashMap<>());
        states.get(loc).putIfAbsent(call, new ArrayList<>());
        RCUSearchState searchState = AbstractStates.extractStateByType(state, RCUSearchState.class);
        if (searchState != null) {
          states.get(loc).get(call).add(searchState);
        }
      }
    }

    if (states.size() > 0) {
      for (LocationState key : states.keySet()) {
        if (states.get(key).size() > 1) {
          output += "Several Callstack states for Location state: " + key + " # " + states.get(key) + "\n";
        } else {
          Map<CallstackState, List<RCUSearchState>> buf = states.get(key);
          for (CallstackState state : buf.keySet()) {
            if (buf.get(state).size() > 1) {
              output += "Several RCUSearchStates for Callstack state" + state + " $ " + buf.get(state) + "\n";
            }
          }
        }
      }

      if (output.length() == 0) {
        output += "All states are okay. No weird states.\n";
      }

      output += states.entrySet().size() + "\n";

      out.append(output);
    }
  }

  @Nullable
  @Override
  public String getName() {
    return "Composite CPA";
  }
}

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

import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
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
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.PointerStatistics;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.rcusearch")
public class RCUSearchStatistics implements Statistics {

  @Option(secure = true, name = "input", description = "name of a file that holds the Points-To "
      + "information")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path input = Paths.get("PointsToMap");

  @Option(secure = true, name = "output", description = "name of a file to hold information about"
      + " RCU pointers and their aliases")
  @FileOption(Type.OUTPUT_FILE)
  private Path output = Paths.get("RCUPointers");

  private final LogManager logger;

  RCUSearchStatistics(Configuration config, LogManager pLogger) throws
                                                                 InvalidConfigurationException {
    logger = pLogger;
    config.inject(this);
  }

  @Override
  @SuppressWarnings("serial")
  public void printStatistics(
      PrintStream out, Result result, UnmodifiableReachedSet reached) {

    Set<MemoryLocation> allRcuPointers = new HashSet<>();
    Map<MemoryLocation, Set<MemoryLocation>> allPointsTo = new HashMap<>();
    for (AbstractState state : reached) {
      RCUSearchState searchState = AbstractStates.extractStateByType(state, RCUSearchState.class);
      if (searchState != null) {
        allRcuPointers.addAll(searchState.getRcuPointers());
        Map<MemoryLocation, Set<MemoryLocation>> bufPT = searchState.getPointsTo();
        for (MemoryLocation key : bufPT.keySet()) {
          allPointsTo.putIfAbsent(key, new HashSet<>());
          allPointsTo.get(key).addAll(bufPT.get(key));
        }
      }
    }

    Multimap<MemoryLocation, MemoryLocation> aliases = getAliases(allPointsTo);

    logger.log(Level.ALL, "RCU pointers in the last state: " + allRcuPointers);

    Set<MemoryLocation> rcuAndAliases = new HashSet<>(allRcuPointers);

    for (MemoryLocation pointer : allRcuPointers) {
      if (!aliases.containsKey(pointer)) {
        logger.log(Level.WARNING, "No RCU pointer <" + pointer.toString() + "> in aliases");
      } else {
        Collection<MemoryLocation> buf = aliases.get(pointer);
        logger.log(Level.ALL, "Aliases for RCU pointer " + pointer + ": " + buf);
        rcuAndAliases.addAll(buf);
      }
    }
    try (Writer writer = Files.newBufferedWriter(output, Charset.defaultCharset())) {
      Gson builder = new Gson();
      java.lang.reflect.Type type = new TypeToken<Set<MemoryLocation>>(){
      }.getType();
      builder.toJson(rcuAndAliases, type, writer);
      logger.log(Level.INFO, "Ended dump of RCU-aliases in file " + output);
    } catch (IOException pE) {
      logger.log(Level.WARNING, pE.getMessage());
    }
    String info = "";
    info += "Number of RCU pointers:        " + allRcuPointers.size() + "\n";
    info += "Number of RCU aliases:         " + (rcuAndAliases.size() - allRcuPointers.size()) + "\n";
    info += "Number of fictional pointers:  " + getFictionalPointersNumber(rcuAndAliases) + "\n";
    out.append(info);
    logger.log(Level.ALL, "RCU with aliases: " + rcuAndAliases);

  }

  @Nullable
  @Override
  public String getName() {
    return "RCU Search";
  }

  private Multimap<MemoryLocation, MemoryLocation> getAliases(Map<MemoryLocation,
                                                                     Set<MemoryLocation>> pointsTo) {
    Multimap<MemoryLocation, MemoryLocation> aliases = HashMultimap.create();
    for (MemoryLocation pointer : pointsTo.keySet()) {
      Set<MemoryLocation> pointerPointTo = pointsTo.get(pointer);
      if (pointerPointTo.contains(PointerStatistics.getReplLocSetTop())) {
        // pointer can point anywhere
        aliases.putAll(pointer, pointsTo.keySet());
        for (MemoryLocation other : pointsTo.keySet()) {
          // logger.log(Level.ALL, "Adding ", pointer, " to ", other, " as an alias");
          aliases.put(other, pointer);
        }
      } else if (!pointerPointTo.contains(PointerStatistics.getReplLocSetBot())) {
        Set<MemoryLocation> commonElems;
        for (MemoryLocation other : pointsTo.keySet()) {
          if (!other.equals(pointer)) {
            commonElems = new HashSet<>(pointsTo.get(other));
            commonElems.retainAll(pointerPointTo);
            if (!commonElems.isEmpty()) {
              aliases.put(pointer, other);
              aliases.put(other, pointer);
            }
          }
        }
      }
    }
    return aliases;
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
  /*
  private Map<MemoryLocation, Set<MemoryLocation>> parseFile(Path input, LogManager logger) {
    Map<MemoryLocation, Set<MemoryLocation>> result = new HashMap<>();

    JsonIterator.setMode(DecodingMode.REFLECTION_MODE);

    try {
      byte[] encoded = Files.readAllBytes(input);
      String str = new String(encoded, Charset.defaultCharset());

      JsonIterator iter = JsonIterator.parse(str);
      Map<String, Any> contents = iter.readAny().asMap();

      for (String key : contents.keySet()) {
        List<Any> idList = contents.get(key).asList();
        MemoryLocation loc;
        String fname = null, id = null;
        Long offset = null;
        Set<MemoryLocation> set = new HashSet<>();

        for (Any elem : idList) {
          Map<String, Any> mapElem = elem.asMap();
          if (mapElem.containsKey("functionname")) {
            fname = mapElem.get("functionname").toString();
          }
          if (mapElem.containsKey("identifier")) {
            id = mapElem.get("identifier").toString();
          }
          if (mapElem.containsKey("offset")) {
            offset = mapElem.get("offset").toLong();
          }

          if (fname != null && offset != null) {
            loc = MemoryLocation.valueOf(fname, id, offset);
          } else if (offset != null) {
            loc = MemoryLocation.valueOf(id, offset);
          } else if (fname != null){
            loc = MemoryLocation.valueOf(fname, id);
          } else {
            loc = MemoryLocation.valueOf(id);
          }

          set.add(loc);
        }

        MemoryLocation locKey = MemoryLocation.valueOf(key);
        result.put(locKey, set);
      }

    } catch (IOException pE) {
      logger.log(Level.WARNING, pE.getMessage());
    }
    return result;
  }
  */
}

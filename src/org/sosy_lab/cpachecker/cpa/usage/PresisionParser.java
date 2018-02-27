/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.util.identifiers.GeneralGlobalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralLocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralStructureFieldIdentifier;

public class PresisionParser {
  private String file;
  private CFA cfa;
  private final LogManager logger;
  private Map<Integer, CFANode> idToNodeMap;

  PresisionParser(String filename, CFA pCfa, LogManager l) {
    file = filename;
    cfa = pCfa;
    logger = l;
    idToNodeMap = new HashMap<>();
    cfa.getAllNodes().forEach(n -> idToNodeMap.put(n.getNodeNumber(), n));
  }

  public UsagePrecision parse(UsagePrecision precision) {
    try (BufferedReader reader =
        Files.newBufferedReader(Paths.get(file), Charset.defaultCharset())) {
      String line;
      CFANode node = null;
      List<String> localSet;
      DataType type;
      Map<GeneralIdentifier, DataType> info = null;
      GeneralIdentifier id;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("N")) {
          // N1 - it's node identifier
          if (node != null && info != null) {
            if (!precision.add(node, info)) {
              logger.log(Level.WARNING, "Node " + node + " is already in presision");
            }
          }
          node = getNode(Integer.parseInt(line.substring(1)));
          info = new HashMap<>();
        } else if (line.length() > 0) {
          // it's information about local statistics
          localSet = Splitter.on(";").splitToList(line);

          if (shouldBeSkipped(localSet)) {
            continue;
          }
          id = parseId(localSet.get(0), localSet.get(1), Integer.parseInt(localSet.get(2)));
          Preconditions.checkNotNull(
              id, line + " can not be parsed, please, move all checks to shouldBeSkipped()");
          type = DataType.valueOf(localSet.get(3).toUpperCase());
          info.put(id, type);
        }
      }
      if (node != null && info != null) {
        if (!precision.add(node, info)) {
          logger.log(Level.WARNING, "Node " + node + " is already in presision");
        }
      }
    } catch (FileNotFoundException e) {
      logger.log(Level.WARNING, "Cannot open file " + file);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Exception during precision parsing: " + e.getMessage());
    }
    return precision;
  }

  private GeneralIdentifier parseId(String type, String name, Integer deref) {
    if (type.equalsIgnoreCase("g")) {
      // Global variable
      return new GeneralGlobalVariableIdentifier(name, deref);
    } else if (type.equalsIgnoreCase("l")) {
      // Local identifier
      return new GeneralLocalVariableIdentifier(name, deref);
    } else if (type.equalsIgnoreCase("s") || type.equalsIgnoreCase("f")) {
      // Structure (field) identifier
      return new GeneralStructureFieldIdentifier(name, deref);
    } else {
      logger.log(Level.WARNING, "Can't resolve such type: " + type);
    }
    return null;
  }

  private boolean shouldBeSkipped(List<String> set) {
    // Return identifier, it's not interesting for us
    return set.get(0).equalsIgnoreCase("r");
  }

  private CFANode getNode(int id) {
    return idToNodeMap.get(id);
  }
}

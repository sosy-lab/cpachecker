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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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
    for (CFANode n : cfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
  }

  public UsagePrecision parse(UsagePrecision precision) {
    try (BufferedReader reader = Files.newBufferedReader(Paths.get(file), Charset.defaultCharset())){
      String line;
      CFANode node = null;
      String[] localSet;
      DataType type;
      Map<GeneralIdentifier, DataType> info = null;
      GeneralIdentifier id;
      while ((line = reader.readLine()) != null) {
        if (line.startsWith("N")) {
          //N1 - it's node identifier
          if (node != null && info != null) {
            if (!precision.add(node, info)) {
              throw new CPAException("Node " + node + " is already in presision");
            }
          }
          node = getNode(Integer.parseInt(line.substring(1)));
          info = new HashMap<>();
        } else if (line.length() > 0) {
          // it's information about local statistics
          localSet = line.split(";");
          if (localSet[0].equalsIgnoreCase("g")) {
            //Global variable
            id = new GeneralGlobalVariableIdentifier(localSet[1], Integer.parseInt(localSet[2]));
          } else if (localSet[0].equalsIgnoreCase("l")) {
            //Local identifier
            id = new GeneralLocalVariableIdentifier(localSet[1], Integer.parseInt(localSet[2]));
          } else if (localSet[0].equalsIgnoreCase("s") || localSet[0].equalsIgnoreCase("f")) {
            //Structure (field) identifier
            id = new GeneralStructureFieldIdentifier(localSet[1], Integer.parseInt(localSet[2]));
          } else if (localSet[0].equalsIgnoreCase("r")) {
            //Return identifier, it's not interesting for us
            continue;
          } else {
            logger.log(Level.WARNING, "Can't resolve such line: " + line);
            continue;
          }
          if (localSet[3].equalsIgnoreCase("global")) {
            type = DataType.GLOBAL;
          } else if (localSet[3].equalsIgnoreCase("local")){
            type = DataType.LOCAL;
          } else {
            logger.log(Level.WARNING, "Can't resolve such data type: " + localSet[3]);
            continue;
          }
          info.put(id, type);
        }
      }
      if (node != null && info != null) {
        if (!precision.add(node, info)) {
          throw new CPAException("Node " + node + " is already in presision");
        }
      }
    } catch(FileNotFoundException e) {
      logger.log(Level.WARNING, "Cannot open file " + file);
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Exception during precision parsing: " + e.getMessage());
    } catch (CPAException e) {
      logger.log(Level.WARNING, "Can't parse presision: " + e.getMessage());
    }
    return precision;
  }

  private CFANode getNode(int id) {
    return idToNodeMap.get(id);
  }
}

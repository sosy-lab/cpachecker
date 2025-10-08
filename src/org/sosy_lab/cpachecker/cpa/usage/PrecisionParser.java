// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.usage;

import com.google.common.base.Ascii;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.local.LocalState.DataType;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralLocalVariableIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GeneralStructureFieldIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.GlobalVariableIdentifier;

public class PrecisionParser {
  private CFA cfa;
  private final LogManager logger;

  PrecisionParser(CFA pCfa, LogManager l) {
    cfa = pCfa;
    logger = l;
  }

  public Map<CFANode, Map<AbstractIdentifier, DataType>> parse(Path file) {
    Map<CFANode, Map<AbstractIdentifier, DataType>> localStatistics = new HashMap<>();
    Map<Integer, CFANode> idToNodeMap = new HashMap<>();
    cfa.nodes().forEach(n -> idToNodeMap.put(n.getNodeNumber(), n));

    try (BufferedReader reader = Files.newBufferedReader(file, Charset.defaultCharset())) {
      String line;
      CFANode node = null;
      Map<AbstractIdentifier, DataType> info = null;
      Pattern nodePattern = Pattern.compile("N[0-9]*$");

      while ((line = reader.readLine()) != null) {
        Matcher matcher = nodePattern.matcher(line);
        if (matcher.find()) {
          // N1 - it's node identifier
          // put all previous information into the map
          putIntoMap(localStatistics, node, info);
          // Get node number
          String nodeId = matcher.group().substring(1);
          node = idToNodeMap.get(Integer.parseInt(nodeId));
          info = new HashMap<>();
        } else if (!line.isEmpty()) {
          if (info == null) {
            logger.log(
                Level.WARNING,
                "Cannot parse precision file %s, node id needs to appear first.",
                file);
            return ImmutableMap.of();
          }
          // it's information about local statistics
          List<String> localSet = Splitter.on(";").splitToList(line);

          if (shouldBeSkipped(localSet)) {
            continue;
          }
          AbstractIdentifier id = parseId(localSet);
          DataType type = DataType.valueOf(Ascii.toUpperCase(localSet.get(3)));
          info.put(id, type);
        }
      }
      putIntoMap(localStatistics, node, info);
      return ImmutableMap.copyOf(localStatistics);
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Cannot parse precision file");
      return ImmutableMap.of();
    }
  }

  private AbstractIdentifier parseId(List<String> splittedLine) {
    String type = splittedLine.getFirst();
    String name = splittedLine.get(1);
    int deref = Integer.parseInt(splittedLine.get(2));

    if (type.equalsIgnoreCase("g")) {
      // Global variable
      return new GlobalVariableIdentifier(name, null, deref);
    } else if (type.equalsIgnoreCase("l")) {
      // Local identifier
      return new GeneralLocalVariableIdentifier(name, deref);
    } else if (type.equalsIgnoreCase("s") || type.equalsIgnoreCase("f")) {
      // Structure (field) identifier
      return new GeneralStructureFieldIdentifier(name, deref);
    } else {
      throw new UnsupportedOperationException(
          splittedLine + " can not be parsed, please, move all checks to shouldBeSkipped()");
    }
  }

  private boolean shouldBeSkipped(List<String> set) {
    // Return identifier, it's not interesting for us
    return set.getFirst().equalsIgnoreCase("r") || set.getFirst().equalsIgnoreCase("func");
  }

  private void putIntoMap(
      Map<CFANode, Map<AbstractIdentifier, DataType>> map,
      CFANode node,
      Map<AbstractIdentifier, DataType> info) {
    if (node != null && info != null) {
      if (map.containsKey(node)) {
        logger.log(Level.WARNING, "Node " + node + " is already in precision");
      } else {
        map.put(node, info);
      }
    }
  }
}

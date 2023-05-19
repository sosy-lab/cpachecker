// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision.Increment;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.RefinableConstraintsPrecision.PrecisionType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class InitialConstraintsPrecisionCreator {

  private final LogManager logger;

  //for ConstraintBasedConstraintsPrecision
  private Set<String> trackedGlobalVariables = new HashSet<>();
  private Multimap<String, String> trackedFunctionsVariables = HashMultimap.create();
  private Multimap<CFANode, String> trackedLocationVariables = HashMultimap.create();

  //for LocationBasedConstraintPrecision
  private Set<CFANode> locationMap = new HashSet<>();

  public InitialConstraintsPrecisionCreator(LogManager pLogger) {
    logger = pLogger;
  }

  public ConstraintsPrecision transformValueToConstraintsPrecision(
      final PrecisionType precisionType, final Path pInitialPrecisionFile, final CFA pCfa) {
    restoreMappingFromFile(pCfa, pInitialPrecisionFile);
    switch (precisionType) {
      case CONSTRAINTS:
        // create a ConstraintBasedConstraintsPrecision, that tracks important variables of value
        // precision
        return new VariableTrackingConstraintsPrecision(trackedFunctionsVariables,
            trackedLocationVariables, trackedGlobalVariables,
            ConstraintBasedConstraintsPrecision.getEmptyPrecision());

      case LOCATION:
        // create LocationBasedConstraintsPrecision with important locations of value precision
        Multimap<CFANode, Constraint> cfaMultiMap = HashMultimap.create();
        for (CFANode n : locationMap) {
          cfaMultiMap.put(n, null);
        }
        Increment locInc = Increment.builder().locallyTracked(cfaMultiMap).build();
        return LocationBasedConstraintsPrecision.getEmptyPrecision().withIncrement(locInc);
      default:
        throw new AssertionError("Unhandled precision type " + precisionType);
    }
  }

  private void restoreMappingFromFile(CFA pCfa, Path pInitialPrecisionFile) {
    List<String> contents = new ArrayList<>();
    boolean scopeValuePrecision = false;
    Multimap<CFANode, MemoryLocation> locationMemoryMap = HashMultimap.create();
    try {
      contents = Files.readAllLines(pInitialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING, e, "Could not read precision from file named " + pInitialPrecisionFile);
    }

    Map<Integer, CFANode> idToCfaNode = createMappingForCFANodes(pCfa);
    final Pattern CFA_NODE_PATTERN = Pattern.compile("N([0-9][0-9]*)");

    CFANode location = getDefaultLocation(idToCfaNode);
    String scopeSelectors = "";
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;
      } else if (currentLine.endsWith(":")) {
        scopeSelectors = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFA_NODE_PATTERN.matcher(scopeSelectors);
        if (matcher.matches()) {
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        }
      } else {
        MemoryLocation memoryLocation = MemoryLocation.parseExtendedQualifiedName(currentLine);
        String trackedVariable = memoryLocation.getIdentifier();
        // * represents global variable
        if (scopeSelectors.equals("*")) {
          trackedGlobalVariables.add(trackedVariable);
          scopeValuePrecision = true;
        } else if (memoryLocation.isOnFunctionStack(scopeSelectors)) {
          // function name represents function variable
          trackedFunctionsVariables.put(scopeSelectors, scopeSelectors + "::" + trackedVariable);
          // if not global or function variable
        } else {
          locationMemoryMap.put(location, memoryLocation);
        }

        locationMap.add(location);
      }
    }
    if (scopeValuePrecision) {
      for (CFANode l : locationMemoryMap.keys()) {
        for (MemoryLocation m : locationMemoryMap.get(l)) {
          trackedLocationVariables.put(l, m.getIdentifier());
        }
      }
    } else {
      for (CFANode l : locationMemoryMap.keys()) {
        for (MemoryLocation m : locationMemoryMap.get(l)) {
          getFunctionFromLocationValuePrecision(m);
        }
      }
    }
  }

  private CFANode getDefaultLocation(Map<Integer, CFANode> idToCfaNode) {
    return idToCfaNode.values().iterator().next();
  }

  private Map<Integer, CFANode> createMappingForCFANodes(CFA pCfa) {
    Map<Integer, CFANode> idToNodeMap = new HashMap<>();
    for (CFANode n : pCfa.nodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }

  /**
   * In case the value precision is of type LocalizedRefinablePrecision, the scope selector will
   * always be a location, also if the memoryLocation points on a function. Therefore it is checked
   * if the memory location is on the function stack.
   */
  private void getFunctionFromLocationValuePrecision(MemoryLocation m) {
    if (m.isOnFunctionStack()) {
      trackedFunctionsVariables.put(m.getFunctionName(),
          m.getFunctionName() + "::" + m.getIdentifier());
    } else {
      trackedGlobalVariables.add(m.getIdentifier());
    }
  }
}

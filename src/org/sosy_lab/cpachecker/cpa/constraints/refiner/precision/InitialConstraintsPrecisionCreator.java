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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.cpa.constraints.refiner.precision.ConstraintsPrecision.Increment;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "cpa.constraints.refinement")
public class InitialConstraintsPrecisionCreator {

  public enum PrecisionType {
    CONSTRAINTS,
    LOCATION
  }

  @Option(description = "Type of precision to use. Has to be LOCATION if"
      + " PredicateExtractionRefiner is used.", toUppercase = true)
  private PrecisionType precisionType = PrecisionType.CONSTRAINTS;

  @Option(description = "Whether to get initial constraint precision from a "
      + "value precision or not.", toUppercase = true)
  private boolean initialValueConstraintsPrecision = false;

  @Option(secure = true, description = "get an initial precision from file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialPrecisionFile = null;

  private final CFA cfa;
  private Configuration config;
  private LogManager logger;

  //for ConstraintBasedConstraintsPrecision
  private Set<String> trackedGlobalVariables = new HashSet<>();
  private Multimap<String, String> trackedFunctionsVariables = HashMultimap.create();
  private Multimap<CFANode, String> trackedLocationVariables = HashMultimap.create();

  //for LocationBasedConstraintPrecision
  private Multimap<CFANode, MemoryLocation> locationMap = HashMultimap.create();

  public InitialConstraintsPrecisionCreator(
      Configuration pConfig, CFA pcfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    cfa = pcfa;
    logger = LogManager.createTestLogManager();
  }


  public ConstraintsPrecision create() throws InvalidConfigurationException {
    // if fromValuePrecision flags are set, create an initial constraints precision based on the
    // given value precision
    if (initialValueConstraintsPrecision && cfa != null && initialPrecisionFile != null) {
      restoreMappingFromFile(cfa);
      return transformValueToConstraintsPrecision();
    } else {
      // if flags are set, but no value precision is given, the initial constraint precision is empty
      if (initialValueConstraintsPrecision) {
        logger.log(Level.INFO,
            "No cpa.constraints.refinement.initialPrecisionFile set. "
                + "Continuing with empty initial precision");
      }
      // empty initial constraints precision
      switch (precisionType) {
        case CONSTRAINTS:
          return ConstraintBasedConstraintsPrecision.getEmptyPrecision();
        case LOCATION:
          return LocationBasedConstraintsPrecision.getEmptyPrecision();
        default:
          throw new AssertionError("Unhandled precision type " + precisionType);
      }
    }
  }


  private ConstraintsPrecision transformValueToConstraintsPrecision() {
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
        for (CFANode n : locationMap.keySet()) {
          cfaMultiMap.put(n, null);
        }
        Increment locInc = Increment.builder().locallyTracked(cfaMultiMap).build();
        return LocationBasedConstraintsPrecision.getEmptyPrecision().withIncrement(locInc);
      default:
        throw new AssertionError("Unhandled precision type " + precisionType);
    }
  }

  private void restoreMappingFromFile(CFA pCfa) {
    List<String> contents = null;
    try {
      contents = Files.readAllLines(initialPrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e,
          "Could not read precision from file named " + initialPrecisionFile);
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
        } else if (memoryLocation.isOnFunctionStack(scopeSelectors)) {
          // function name represents function variable
          trackedFunctionsVariables.put(scopeSelectors, scopeSelectors + "::" + trackedVariable);
          // if not global or function variable
        } else {
          getFunctionFromLocationValuePrecision(location, memoryLocation);
        }

        locationMap.put(location, memoryLocation);
      }
    }
  }

  private CFANode getDefaultLocation(Map<Integer, CFANode> idToCfaNode) {
    return idToCfaNode.values().iterator().next();
  }

  private Map<Integer, CFANode> createMappingForCFANodes(CFA pCfa) {
    Map<Integer, CFANode> idToNodeMap = new HashMap<>();
    for (CFANode n : pCfa.getAllNodes()) {
      idToNodeMap.put(n.getNodeNumber(), n);
    }
    return idToNodeMap;
  }

  /**
   * In case of LOCATION value precision, the scope selector will always be a location, also if the
   * memoryLocation points on a function. Therefor it is checked if the memory location is on the
   * function stack.
   */
  private void getFunctionFromLocationValuePrecision(CFANode n, MemoryLocation m) {
    if (m.isOnFunctionStack()) {
      trackedFunctionsVariables.put(m.getFunctionName(),
          m.getFunctionName() + "::" + m.getIdentifier());
    } else {  //TODO add case global
      trackedLocationVariables.put(n, m.getIdentifier());
    }
  }
}

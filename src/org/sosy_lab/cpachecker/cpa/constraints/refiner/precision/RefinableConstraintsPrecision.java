// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.constraints.refiner.precision;

import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Matcher;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.constraints.constraint.Constraint;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Refinable {@link ConstraintsPrecision}. */
@Options(prefix = "cpa.constraints.refinement")
public class RefinableConstraintsPrecision implements ConstraintsPrecision {

  public enum PrecisionType {
    CONSTRAINTS,
    LOCATION
  }

  @Option(
      secure = true,
      description =
          "Type of precision to use. Has to be LOCATION if"
              + " PredicateExtractionRefiner is used.",
      toUppercase = true)
  private PrecisionType precisionType = PrecisionType.CONSTRAINTS;

  @Option(
      secure = true,
      description =
          "derive an initial constraint precision from value precision stored in this file")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path initialValuePrecisionFile = null;

  @Option(
      secure = true,
      description =
          "enable if variables from value precision should be considered in variable's scope"
              + " instead of scope specified in precision")
  private boolean applyInScope = false;

  @Option(
      secure = true,
      description =
          "enable to track constraints based on value precision only if all variables occurring in"
              + " constraint are relevant in variable precision. If disabled it is sufficient that"
              + " one variable is relevant.")
  private boolean mustTrackAll = false;

  private final ConstraintsPrecision delegate;

  public RefinableConstraintsPrecision(
      final Configuration pConfig, final CFA pCfa, final LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    if (initialValuePrecisionFile != null) {
      Preconditions.checkNotNull(pCfa);
      delegate = transformValueToConstraintsPrecision(pCfa, pLogger);
    } else {
      // empty initial constraints precision
      delegate =
          switch (precisionType) {
            case CONSTRAINTS -> ConstraintBasedConstraintsPrecision.getEmptyPrecision();
            case LOCATION -> LocationBasedConstraintsPrecision.getEmptyPrecision();
          };
    }
  }

  private RefinableConstraintsPrecision(final ConstraintsPrecision pDelegate) {
    delegate = pDelegate;
  }

  @Override
  public boolean isTracked(Constraint pConstraint, CFANode pLocation) {
    return delegate.isTracked(pConstraint, pLocation);
  }

  @Override
  public ConstraintsPrecision join(ConstraintsPrecision pOther) {
    assert pOther instanceof RefinableConstraintsPrecision;
    final ConstraintsPrecision otherDelegate = ((RefinableConstraintsPrecision) pOther).delegate;

    return new RefinableConstraintsPrecision(delegate.join(otherDelegate));
  }

  @Override
  public ConstraintsPrecision withIncrement(Increment pIncrement) {
    return new RefinableConstraintsPrecision(delegate.withIncrement(pIncrement));
  }

  @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
  @Override
  public boolean equals(Object o) {
    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    int result = precisionType.hashCode();
    result = 31 * result + delegate.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  private ConstraintsPrecision transformValueToConstraintsPrecision(
      final CFA pCfa, final LogManager pLogger) {
    Set<String> trackedGlobalVariables = new HashSet<>();
    Multimap<String, String> trackedFunctionsVariables = HashMultimap.create();
    Multimap<CFANode, String> trackedLocationVariables = HashMultimap.create();

    restoreMappingFromFile(
        pCfa, pLogger, trackedGlobalVariables, trackedFunctionsVariables, trackedLocationVariables);
    switch (precisionType) {
      case CONSTRAINTS:
        // create a ConstraintBasedConstraintsPrecision, that tracks important variables of value
        // precision
        return new VariableTrackingConstraintsPrecision(
            trackedFunctionsVariables,
            trackedLocationVariables,
            trackedGlobalVariables,
            ConstraintBasedConstraintsPrecision.getEmptyPrecision(),
            mustTrackAll);

      case LOCATION:
        // create LocationBasedConstraintsPrecision with important locations of value precision
        Multimap<CFANode, Constraint> cfaMultiMap = HashMultimap.create();
        if (!trackedGlobalVariables.isEmpty()) {
          for (CFANode node : pCfa.nodes()) {
            cfaMultiMap.put(node, null); // null okay because constraint ignored by precision
          }
        } else {
          for (String funName : trackedFunctionsVariables.keySet()) {
            for (CFANode node :
                Collections2.filter(pCfa.nodes(), node -> node.getFunctionName().equals(funName))) {
              cfaMultiMap.put(node, null);
            }
          }

          for (CFANode node : trackedLocationVariables.keySet()) {
            cfaMultiMap.put(node, null); // null okay because constraint ignored by precision
          }
        }
        Increment locInc = Increment.Builder.builder().locallyTracked(cfaMultiMap).build();
        return LocationBasedConstraintsPrecision.getEmptyPrecision().withIncrement(locInc);
      default:
        throw new AssertionError("Unsupported precision type");
    }
  }

  private void restoreMappingFromFile(
      final CFA pCfa,
      final LogManager logger,
      final Set<String> pTrackedGlobalVariables,
      final Multimap<String, String> pTrackedFunctionsVariables,
      final Multimap<CFANode, String> pTrackedLocationVariables) {
    List<String> contents = new ArrayList<>();
    try {
      contents = Files.readAllLines(initialValuePrecisionFile, Charset.defaultCharset());
    } catch (IOException e) {
      logger.logUserException(
          Level.WARNING,
          e,
          "Could not read precision from file named " + initialValuePrecisionFile);
    }

    Map<Integer, CFANode> idToCfaNode = CFAUtils.getMappingFromNodeIDsToCFANodes(pCfa);

    // initialize location with some default location
    CFANode location = idToCfaNode.values().iterator().next();
    boolean isNodeScope = false;

    String scopeSelector = "";
    for (String currentLine : contents) {
      if (currentLine.trim().isEmpty()) {
        continue;
      } else if (currentLine.endsWith(":")) {
        scopeSelector = currentLine.substring(0, currentLine.indexOf(":"));
        Matcher matcher = CFAUtils.CFA_NODE_NAME_PATTERN.matcher(scopeSelector);
        if (matcher.matches()) {
          isNodeScope = true;
          location = idToCfaNode.get(Integer.parseInt(matcher.group(1)));
        } else {
          isNodeScope = false;
        }
      } else {
        MemoryLocation memoryLocation = MemoryLocation.parseExtendedQualifiedName(currentLine);
        if (applyInScope) {
          if (memoryLocation.isOnFunctionStack()) {
            pTrackedFunctionsVariables.put(
                memoryLocation.getFunctionName(), memoryLocation.getExtendedQualifiedName());
          } else {
            pTrackedGlobalVariables.add(memoryLocation.getExtendedQualifiedName());
          }
        } else if (isNodeScope) {
          // local scope
          if (location != null) {
            pTrackedLocationVariables.put(location, memoryLocation.getExtendedQualifiedName());
          }
        } else if (scopeSelector.equals("*")) { // * represents global scope
          pTrackedGlobalVariables.add(memoryLocation.getQualifiedName());
        } else { // in function scope
          pTrackedFunctionsVariables.put(scopeSelector, memoryLocation.getExtendedQualifiedName());
        }
      }
    }
  }
}

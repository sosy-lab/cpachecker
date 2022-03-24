// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.MoreCollectors;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class CFAToCExporter {

  // Use original, unqualified names for variables
  private static final boolean NAMES_QUALIFIED = false;

  private SetMultimap<FileLocation, CFAEdge> locationToEdgesMappingForGlobalDeclarations;
  private Map<FileLocation, String> locationToFunctionStringMapping;

  /**
   * Exports the given {@link CFA} to a C program.
   *
   * @param pCfa the CFA to export
   * @return C representation of the given CFA
   * @throws InvalidConfigurationException if the given CFA is not the CFA of a C program
   */
  public String exportCfa(final CFA pCfa) throws InvalidConfigurationException {
    if (pCfa.getLanguage() != Language.C) {
      throw new InvalidConfigurationException(
          "CFA can only be exported to C for C input programs, at the moment");
    }

    locationToEdgesMappingForGlobalDeclarations =
        MultimapBuilder.treeKeys().hashSetValues().build();
    locationToFunctionStringMapping = new TreeMap<>();

    for (final FunctionEntryNode functionEntryNode : pCfa.getAllFunctionHeads()) {
      locationToFunctionStringMapping.put(
          functionEntryNode.getFileLocation(), exportFunction(functionEntryNode));
    }

    final StringBuilder programBuilder = new StringBuilder();
    programBuilder.append(exportGlobalDeclarations());
    for (final String functionString : locationToFunctionStringMapping.values()) {
      programBuilder.append(functionString);
    }
    return programBuilder.toString();
  }

  private String exportFunction(final FunctionEntryNode pFunctionEntryNode) {
    final StringBuilder functionBuilder = new StringBuilder();
    functionBuilder.append(
        pFunctionEntryNode
            .getFunctionDefinition()
            .toASTString(NAMES_QUALIFIED)
            .replace(";", " {\n"));

    final SetMultimap<FileLocation, CFAEdge> locationToEdgesMappingWithinFunction =
        collectCfaEdgesByFileLocation(pFunctionEntryNode);
    final Map<FileLocation, String> locationToCCodeMappingWithinFunction =
        processCfaEdgesByFileLocation(locationToEdgesMappingWithinFunction);

    for (final Map.Entry<FileLocation, String> locWithCCode :
        locationToCCodeMappingWithinFunction.entrySet()) {
      functionBuilder.append(locWithCCode.getValue()).append("\n");
    }
    functionBuilder.append("}\n");
    return functionBuilder.toString();
  }

  private SetMultimap<FileLocation, CFAEdge> collectCfaEdgesByFileLocation(
      final FunctionEntryNode pFunctionEntryNode) {
    final SetMultimap<FileLocation, CFAEdge> locationToEdgesMappingWithinFunction =
        MultimapBuilder.treeKeys().hashSetValues().build();

    final Queue<CFANode> waitList = new ArrayDeque<>();
    waitList.offer(pFunctionEntryNode);

    while (!waitList.isEmpty()) {
      final CFANode currentNode = waitList.poll();

      for (final CFAEdge cfaEdge : CFAUtils.leavingEdges(currentNode)) {

        if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge
            && ((CDeclarationEdge) cfaEdge).getDeclaration().isGlobal()) {
          locationToEdgesMappingForGlobalDeclarations.put(cfaEdge.getFileLocation(), cfaEdge);
        } else {
          locationToEdgesMappingWithinFunction.put(cfaEdge.getFileLocation(), cfaEdge);
        }
        waitList.offer(cfaEdge.getSuccessor());
      }
    }
    return locationToEdgesMappingWithinFunction;
  }

  private Map<FileLocation, String> processCfaEdgesByFileLocation(
      final SetMultimap<FileLocation, CFAEdge> pLocationToEdgesMapping) {
    final Map<FileLocation, String> locationToCCodeMapping = new TreeMap<>();

    for (final FileLocation loc : pLocationToEdgesMapping.keySet()) {
      if (loc.isRealLocation()) {
        final Set<CFAEdge> edgesWithSameLocation = pLocationToEdgesMapping.get(loc);

        if (edgesWithSameLocation.size() == 1) {
          final CFAEdge cfaEdge = Iterables.getOnlyElement(edgesWithSameLocation);

          if (cfaEdge.getPredecessor() instanceof CFALabelNode) {
            locationToCCodeMapping.put(loc, processLabelEdge(cfaEdge));
            continue;
          }
        }
        final String rawStatement =
            Iterables.getOnlyElement(
                edgesWithSameLocation.stream()
                    .map(CFAEdge::getRawStatement)
                    .collect(ImmutableSet.toImmutableSet()));
        locationToCCodeMapping.put(loc, rawStatement);
      }
    }
    return locationToCCodeMapping;
  }

  private String exportGlobalDeclarations() {
    final StringBuilder globalDeclarationsBuilder = new StringBuilder();

    for (final FileLocation loc : locationToEdgesMappingForGlobalDeclarations.keySet()) {
      // only export original declarations
      if (!locationToFunctionStringMapping.containsKey(loc)) {
        assert loc.isRealLocation();
        final Set<CFAEdge> edgesWithSameLocation =
            locationToEdgesMappingForGlobalDeclarations.get(loc);
        // assert, that there is exactly one edge with this location
        final String rawStatement =
            edgesWithSameLocation.stream()
                .map(CFAEdge::getRawStatement)
                .collect(MoreCollectors.onlyElement());
        globalDeclarationsBuilder.append(rawStatement).append("\n");
      }
    }
    return globalDeclarationsBuilder.toString();
  }

  private String processLabelEdge(final CFAEdge pLabelEdge) {
    final FileLocation labelLoc = pLabelEdge.getFileLocation();

    final List<FileLocation> fileLocationsOfFollowingCfaEdges =
        CFAUtils.leavingEdges(pLabelEdge.getSuccessor()).stream()
            .map(CFAEdge::getFileLocation)
            .collect(ImmutableList.toImmutableList());
    final FileLocation nextFileLocation = FileLocation.merge(fileLocationsOfFollowingCfaEdges);

    // the raw statement of a CFAEdge after a LabelNode can overlap with the following CFAEdge(s)
    final int beginningOfDuplicatePart =
        nextFileLocation.getNodeOffset() - labelLoc.getNodeOffset();
    final int endOfUniquePart = Math.min(labelLoc.getNodeLength(), beginningOfDuplicatePart);
    return pLabelEdge.getRawStatement().substring(0, endOfUniquePart);
  }
}

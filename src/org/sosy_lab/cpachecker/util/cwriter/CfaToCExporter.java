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
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.MoreCollectors;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
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

public class CfaToCExporter {

  // Use original, unqualified names for variables
  private static final boolean NAMES_QUALIFIED = false;

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

    final ImmutableSortedMap.Builder<FileLocation, GlobalDeclaration> globalDeclarationBuilder =
        ImmutableSortedMap.naturalOrder();
    final ImmutableSortedMap.Builder<FileLocation, String> functionCodeBuilder =
        ImmutableSortedMap.naturalOrder();

    for (final FunctionEntryNode functionEntryNode : pCfa.getAllFunctionHeads()) {
      functionCodeBuilder.put(
          functionEntryNode.getFileLocation(),
          exportFunction(functionEntryNode, globalDeclarationBuilder));
    }
    final ImmutableSortedMap<FileLocation, String> functionCodeSortedByFileLocation =
        functionCodeBuilder.build();

    final StringBuilder programBuilder = new StringBuilder();
    programBuilder.append(
        exportGlobalDeclarations(
            globalDeclarationBuilder.build(), functionCodeSortedByFileLocation.keySet()));
    for (final String functionCode : functionCodeSortedByFileLocation.values()) {
      programBuilder.append(functionCode);
    }
    return programBuilder.toString();
  }

  private String exportFunction(
      final FunctionEntryNode pFunctionEntryNode,
      final ImmutableSortedMap.Builder<FileLocation, GlobalDeclaration> globalDeclarationBuilder) {

    final StringBuilder functionBuilder = new StringBuilder();
    functionBuilder.append(
        pFunctionEntryNode
            .getFunctionDefinition()
            .toASTString(NAMES_QUALIFIED)
            .replace(";", " {\n"));

    final ImmutableSortedMap<FileLocation, BlockItem> blockItemsWithinFunctionSortedByFileLocation =
        collectCfaEdgesOfFunctionIntoBlockItems(pFunctionEntryNode, globalDeclarationBuilder);

    for (final Entry<FileLocation, BlockItem> entry :
        blockItemsWithinFunctionSortedByFileLocation.entrySet()) {
      if (entry.getKey().isRealLocation()) {
        functionBuilder.append(entry.getValue().toCCode()).append("\n");
      }
    }
    functionBuilder.append("}\n");
    return functionBuilder.toString();
  }

  private ImmutableSortedMap<FileLocation, BlockItem> collectCfaEdgesOfFunctionIntoBlockItems(
      final FunctionEntryNode pFunctionEntryNode,
      final ImmutableSortedMap.Builder<FileLocation, GlobalDeclaration> globalDeclarationBuilder) {

    final TreeMap<FileLocation, BlockItem> blockItemsSortedByFileLocation = new TreeMap<>();
    final Queue<CFANode> waitList = new ArrayDeque<>();
    waitList.offer(pFunctionEntryNode);

    while (!waitList.isEmpty()) {
      final CFANode currentNode = waitList.poll();

      for (final CFAEdge cfaEdge : CFAUtils.leavingEdges(currentNode)) {
        final FileLocation loc = cfaEdge.getFileLocation();

        if (cfaEdge.getEdgeType() == CFAEdgeType.DeclarationEdge
            && ((CDeclarationEdge) cfaEdge).getDeclaration().isGlobal()) {

          globalDeclarationBuilder.put(loc, new GlobalDeclaration(cfaEdge));

        } else {

          if (blockItemsSortedByFileLocation.containsKey(loc)) {
            final BlockItem blockItem = blockItemsSortedByFileLocation.get(loc);
            assert blockItem instanceof SimpleBlockItem
                : "Only statements and declarations can be split into multiple edges";
            ((SimpleBlockItem) blockItem).addEdge(cfaEdge);
            continue;
          }

          if (currentNode instanceof CFALabelNode) {
            blockItemsSortedByFileLocation.put(loc, new LabeledStatement(cfaEdge));
            continue;
          }

          blockItemsSortedByFileLocation.put(loc, new SimpleBlockItem(cfaEdge));
        }

        waitList.offer(cfaEdge.getSuccessor());
      }
    }
    return ImmutableSortedMap.copyOfSorted(blockItemsSortedByFileLocation);
  }

  private String exportGlobalDeclarations(
      final ImmutableSortedMap<FileLocation, GlobalDeclaration>
          pGlobalDeclarationsSortedByFileLocation,
      final ImmutableSet<FileLocation> pFileLocationsOfFunctionDefinitions) {
    final StringBuilder globalDeclarationsBuilder = new StringBuilder();

    for (final Entry<FileLocation, GlobalDeclaration> entry :
        pGlobalDeclarationsSortedByFileLocation.entrySet()) {
      final FileLocation loc = entry.getKey();

      // only export original global declarations, ignore the generated ones
      if (!pFileLocationsOfFunctionDefinitions.contains(loc)) {
        assert loc.isRealLocation();
        globalDeclarationsBuilder.append(entry.getValue().toCCode()).append("\n");
      }
    }
    return globalDeclarationsBuilder.toString();
  }

  private abstract static class BlockItem {

    // TODO introduce flag to prohibit exporting multiple times?

    protected abstract String toCCode();
  }

  private static class SimpleBlockItem extends BlockItem {

    // TODO introduce flag to prohibit adding edges after export?

    protected final ImmutableList.Builder<CFAEdge> edgeBuilder;

    private SimpleBlockItem(final CFAEdge pEdge) {
      edgeBuilder = ImmutableList.<CFAEdge>builder().add(pEdge);
    }

    protected void addEdge(final CFAEdge pEdge) {
      edgeBuilder.add(pEdge);
    }

    @Override
    protected String toCCode() {
      // assert that all edges have the same raw statement
      return Iterables.getOnlyElement(
          edgeBuilder.build().stream()
              .map(CFAEdge::getRawStatement)
              .collect(ImmutableSet.toImmutableSet()));
    }
  }

  private static class GlobalDeclaration extends SimpleBlockItem {

    private GlobalDeclaration(final CFAEdge pEdge) {
      super(pEdge);
    }

    @Override
    protected String toCCode() {
      // assert that the global declaration consists of exactly one edge
      return edgeBuilder.build().stream()
          .map(CFAEdge::getRawStatement)
          .collect(MoreCollectors.onlyElement());
    }
  }

  private static class LabeledStatement extends SimpleBlockItem {

    private LabeledStatement(final CFAEdge pEdge) {
      super(pEdge);
    }

    @Override
    protected void addEdge(final CFAEdge pEdge) {
      throw new AssertionError("Labeled statements consist of exactly one edge.");
    }

    @Override
    protected String toCCode() {
      // assert that the labeled statement consists of exactly one edge
      final CFAEdge labelEdge = Iterables.getOnlyElement(edgeBuilder.build());
      final FileLocation labelLoc = labelEdge.getFileLocation();

      final List<FileLocation> fileLocationsOfFollowingCfaEdges =
          CFAUtils.leavingEdges(labelEdge.getSuccessor()).stream()
              .map(CFAEdge::getFileLocation)
              .collect(ImmutableList.toImmutableList());
      final FileLocation nextFileLocation = FileLocation.merge(fileLocationsOfFollowingCfaEdges);

      // the raw statement of a CFAEdge after a LabelNode can overlap with the following CFAEdge(s)
      final int beginningOfDuplicatePart =
          nextFileLocation.getNodeOffset() - labelLoc.getNodeOffset();
      final int endOfUniquePart = Math.min(labelLoc.getNodeLength(), beginningOfDuplicatePart);
      return labelEdge.getRawStatement().substring(0, endOfUniquePart);
    }
  }
}

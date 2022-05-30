// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter;

import com.google.common.collect.FluentIterable;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.blankEdges.ForLoopIndicatingEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.MergePoint;

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
  public static String exportCfa(final CFA pCfa) throws InvalidConfigurationException {
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
        functionCodeBuilder.buildOrThrow();

    final StringBuilder programBuilder = new StringBuilder();
    programBuilder
        .append(
            exportGlobalDeclarations(
                globalDeclarationBuilder.buildOrThrow(), functionCodeSortedByFileLocation.keySet()))
        .append("\n");
    for (final String functionCode : functionCodeSortedByFileLocation.values()) {
      programBuilder.append(functionCode).append("\n");
    }
    return programBuilder.toString();
  }

  private static String exportFunction(
      final FunctionEntryNode pFunctionEntryNode,
      final ImmutableSortedMap.Builder<FileLocation, GlobalDeclaration> pGlobalDeclarationBuilder) {

    final StringBuilder functionBuilder = new StringBuilder();
    functionBuilder.append(
        pFunctionEntryNode
            .getFunctionDefinition()
            .toASTString(NAMES_QUALIFIED)
            .replace(";", " {\n"));

    final FunctionExitNode functionExitNode = pFunctionEntryNode.getExitNode();
    final MergePoint<CFANode> functionMergePoint =
        new MergePoint<>(
            functionExitNode, n -> CFAUtils.successorsOf(n), n -> CFAUtils.predecessorsOf(n));
    final ImmutableSortedMap<FileLocation, BlockItem> blockItemsWithinFunctionSortedByFileLocation =
        collectCfaEdgesIntoBlockItems(
            pFunctionEntryNode, functionExitNode, functionMergePoint, pGlobalDeclarationBuilder);

    functionBuilder
        .append(exportBlockItemsSortedByFileLocation(blockItemsWithinFunctionSortedByFileLocation))
        .append("}\n");
    return functionBuilder.toString();
  }

  private static ImmutableSortedMap<FileLocation, BlockItem> collectCfaEdgesIntoBlockItems(
      final CFANode pStartNode,
      final CFANode pEndNode,
      final MergePoint<CFANode> pFunctionMergePoint,
      final ImmutableSortedMap.Builder<FileLocation, GlobalDeclaration> pGlobalDeclarationBuilder) {

    final TreeMap<FileLocation, BlockItem> blockItemsSortedByFileLocation = new TreeMap<>();
    final Queue<CFANode> waitList = new ArrayDeque<>();
    waitList.offer(pStartNode);

    while (!waitList.isEmpty()) {
      final CFANode currentNode = waitList.poll();
      if (currentNode.equals(pEndNode)) {
        break;
      }
      final FluentIterable<CFAEdge> leavingEdges = CFAUtils.leavingEdges(currentNode);
      final int numLeavingEdges = leavingEdges.size();

      if (numLeavingEdges > 1) {

        final CFANode mergeNode = pFunctionMergePoint.findMergePoint(currentNode);
        final IfElseStatement ifElse =
            new IfElseStatement(
                currentNode, mergeNode, pFunctionMergePoint, pGlobalDeclarationBuilder);
        final FileLocation blockLoc = ifElse.getFileLocation();
        blockItemsSortedByFileLocation.put(blockLoc, ifElse);
        waitList.offer(mergeNode);

      } else if (numLeavingEdges == 1) {

        final CFAEdge edge = Iterables.getOnlyElement(leavingEdges);

        if (edge instanceof ForLoopIndicatingEdge) {
          final CFANode loopStart = findNextBranching(currentNode);
          assert loopStart.isLoopStart();
          // TODO handle for statement
          continue;
        }

        waitList.offer(edge.getSuccessor());
        final FileLocation loc = edge.getFileLocation();
        if (!loc.isRealLocation()) {
          continue;
        }

        if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge
            && ((CDeclarationEdge) edge).getDeclaration().isGlobal()) {

          pGlobalDeclarationBuilder.put(loc, new GlobalDeclaration(edge));

        } else if (blockItemsSortedByFileLocation.containsKey(loc)) {

          final BlockItem blockItem = blockItemsSortedByFileLocation.get(loc);
          assert blockItem instanceof SimpleBlockItem
              : "Only statements and declarations can be split into multiple edges";
          ((SimpleBlockItem) blockItem).addEdge(edge);

        } else if (currentNode instanceof CFALabelNode) {

          blockItemsSortedByFileLocation.put(loc, new LabeledStatement(edge));

        } else {

          blockItemsSortedByFileLocation.put(loc, new SimpleBlockItem(edge));
        }
      }
    }
    return ImmutableSortedMap.copyOfSorted(blockItemsSortedByFileLocation);
  }

  private static CFANode findNextBranching(final CFANode pStartNode) {
    // TODO better for finding loop start?
    //    if (pStartNode.isLoopStart()) {
    //      return pStartNode;
    //    }
    final FluentIterable<CFANode> successors = CFAUtils.successorsOf(pStartNode);
    assert !successors.isEmpty() : "Expected a branching but did not find one";
    if (successors.size() > 1) {
      return pStartNode;
    }
    return findNextBranching(Iterables.getOnlyElement(successors));
  }

  private static String exportBlockItemsSortedByFileLocation(
      final ImmutableSortedMap<FileLocation, BlockItem> pBlockItemsSortedByFileLocation) {

    final StringBuilder blockBuilder = new StringBuilder();
    for (final BlockItem blockItem : pBlockItemsSortedByFileLocation.values()) {
      blockBuilder.append(blockItem.exportToCCode()).append("\n");
    }
    return blockBuilder.toString();
  }

  private static String exportGlobalDeclarations(
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
        globalDeclarationsBuilder.append(entry.getValue().exportToCCode()).append("\n");
      }
    }
    return globalDeclarationsBuilder.toString();
  }

  private abstract static class BlockItem {

    // TODO introduce flag to prohibit exporting multiple times?

    protected abstract String exportToCCode();
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
    protected String exportToCCode() {
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
    protected String exportToCCode() {
      // assert that the global declaration consists of exactly one edge
      return edgeBuilder.build().stream()
          .map(CFAEdge::getRawStatement)
          .collect(MoreCollectors.onlyElement());
    }
  }

  private static class LabeledStatement extends BlockItem {

    final CFAEdge labelEdge;

    private LabeledStatement(final CFAEdge pEdge) {
      // TODO only allow special label edges (subtype of BlankEdge)
      labelEdge = pEdge;
    }

    @Override
    protected String exportToCCode() {
      final FileLocation labelLoc = labelEdge.getFileLocation();
      // TODO it is probably enough to look at the directly following CFAEdges
      final FileLocation nextFileLocation = findNextRealFileLocation(labelEdge);

      // the raw statement of the CFAEdge of a labeled statement can overlap with the following
      // CFAEdge(s)
      if (!nextFileLocation.isRealLocation()
          || nextFileLocation.getNodeOffset()
              < labelLoc.getNodeOffset() + labelLoc.getNodeLength()) {

        final int endOfLabel = labelEdge.getRawStatement().indexOf(':');
        assert endOfLabel != -1;
        return labelEdge.getRawStatement().substring(0, endOfLabel + 1);
      }
      return labelEdge.getRawStatement();
    }

    private static FileLocation findNextRealFileLocation(final CFAEdge pEdge) {
      FluentIterable<CFAEdge> nextEdges = CFAUtils.leavingEdges(pEdge.getSuccessor());
      if (nextEdges.isEmpty()) {
        return FileLocation.DUMMY;
      }
      final List<FileLocation> fileLocationsOfFollowingCfaEdges =
          nextEdges.stream().map(CFAEdge::getFileLocation).collect(ImmutableList.toImmutableList());
      final FileLocation nextFileLocation = FileLocation.merge(fileLocationsOfFollowingCfaEdges);

      if (nextFileLocation.isRealLocation()) {
        return nextFileLocation;
      }
      return FileLocation.merge(
          nextEdges.stream()
              .map(edge -> findNextRealFileLocation(edge))
              .collect(ImmutableList.toImmutableList()));
    }
  }

  private static class IfElseStatement extends BlockItem {

    private final String condition;
    private final ImmutableSortedMap<FileLocation, BlockItem> thenBranch;
    private final ImmutableSortedMap<FileLocation, BlockItem> elseBranch;

    private IfElseStatement(
        final CFANode pBranchingNode,
        final CFANode pMergeNode,
        final MergePoint<CFANode> pFunctionMergePoint,
        final ImmutableSortedMap.Builder<FileLocation, GlobalDeclaration>
            pGlobalDeclarationBuilder) {

      final FluentIterable<CFAEdge> leavingEdges = CFAUtils.leavingEdges(pBranchingNode);
      assert leavingEdges.size() == 2 : "Branchings with more than two branches are not supported";
      for (final CFAEdge edge : leavingEdges) {
        assert edge instanceof CAssumeEdge : "Branchings have to consist of two CAssumeEdges";
      }
      final CAssumeEdge firstAssumeEdge = (CAssumeEdge) leavingEdges.get(0);
      final CAssumeEdge secondAssumeEdge = (CAssumeEdge) leavingEdges.get(1);

      final CAssumeEdge thenEdge, elseEdge;
      if (firstAssumeEdge.getTruthAssumption() && !firstAssumeEdge.isSwapped()) {
        assert !secondAssumeEdge.getTruthAssumption() && !secondAssumeEdge.isSwapped();
        thenEdge = firstAssumeEdge;
        elseEdge = secondAssumeEdge;
      } else {
        assert (!firstAssumeEdge.getTruthAssumption() && secondAssumeEdge.getTruthAssumption())
            || (firstAssumeEdge.isSwapped() && secondAssumeEdge.isSwapped());
        thenEdge = secondAssumeEdge;
        elseEdge = firstAssumeEdge;
      }

      final String rawCondition = thenEdge.getRawStatement();
      // omit the square brackets
      condition = rawCondition.substring(1, rawCondition.length() - 1);
      thenBranch =
          collectCfaEdgesIntoBlockItems(
              thenEdge.getSuccessor(), pMergeNode, pFunctionMergePoint, pGlobalDeclarationBuilder);
      elseBranch =
          collectCfaEdgesIntoBlockItems(
              elseEdge.getSuccessor(), pMergeNode, pFunctionMergePoint, pGlobalDeclarationBuilder);
    }

    private FileLocation getFileLocation() {
      return FileLocation.merge(
          ImmutableList.<FileLocation>builder()
              .addAll(thenBranch.keySet())
              .addAll(elseBranch.keySet())
              .build());
    }

    @Override
    protected String exportToCCode() {
      return "if ("
          + condition
          + ") {\n"
          + exportBlockItemsSortedByFileLocation(thenBranch)
          + "} else {\n"
          + exportBlockItemsSortedByFileLocation(elseBranch)
          + "}";
    }
  }
}

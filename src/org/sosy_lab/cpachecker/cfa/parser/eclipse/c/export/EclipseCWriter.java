// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.ExportStatement.createNewLabelName;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.CfaTransformationRecords;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.export.CWriter;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.ExportStatement.ElseStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.ExportStatement.GlobalDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.ExportStatement.IfStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.ExportStatement.PlaceholderStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.ExportStatement.SimpleStatement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** Writer based on Eclipse CDT. */
class EclipseCWriter implements CWriter {

  private final EclipseCdtWrapper eclipseCdt;

  public EclipseCWriter(final ParserOptions pOptions, final ShutdownNotifier pShutdownNotifier) {
    eclipseCdt = new EclipseCdtWrapper(pOptions, pShutdownNotifier);
  }

  // TODO split into smaller methods
  @Override
  public String exportCfa(final CFA pCfa) throws IOException, CPAException, InterruptedException {

    checkArgument(
        pCfa.getLanguage() == Language.C,
        "CFA can only be exported to C for C programs, at the moment.");

    final CfaTransformationRecords records;
    final IASTTranslationUnit originalAst;

    // When the CFA does not have CfaTransformationRecords we assume it is completely changed, i.e.,
    // that all edges and nodes were added and that the AST node substitutions were the identity.
    // In that case, we can not parse the original AST, but we also do not require it.
    if (pCfa.getTransformationRecords().isEmpty()) {
      records = createTransformationRecordsForCfaWithout(pCfa);
      originalAst = null;

    } else {
      records = pCfa.getTransformationRecords().orElseThrow();
      originalAst = parseOriginalAst(records.getCfaBeforeTransformation().orElseThrow());
    }

    final GlobalExportInformation exportInfo = new GlobalExportInformation(records);

    for (final FunctionEntryNode functionEntryNode : pCfa.getAllFunctionHeads()) {
      traverseFunctionCfaAndCreateCode(functionEntryNode, exportInfo);
    }

    // TODO combine the original AST nodes of the unchanged parts with the newly created AST nodes
    // for the changed parts to get the C export

    assert originalAst != null;
    return originalAst.getRawSignature(); // TODO adjust in case of changes
  }

  private IASTTranslationUnit parseOriginalAst(final CFA pOriginalCfa)
      throws InterruptedException, IOException, CPAException {
    checkArgument(
        pOriginalCfa.getFileNames().size() == 1,
        "CFA can only be exported for a single input file, at the moment.");

    try {
      final IASTTranslationUnit originalAst =
          eclipseCdt.getASTTranslationUnit(
              EclipseCdtWrapper.wrapFile(pOriginalCfa.getFileNames().get(0)));
      Verify.verify(
          originalAst.getPreprocessorProblemsCount() == 0,
          "Problems should have been caught during CFA generation.");
      return originalAst;

    } catch (final CoreException pE) {
      throw new CPAException("Failed to export CFA to C program because AST parsing failed.");
    }
  }

  private static CfaTransformationRecords createTransformationRecordsForCfaWithout(final CFA pCfa) {
    final Set<CFANode> allNodes = ImmutableSet.copyOf(pCfa.getAllNodes());
    final ImmutableSet.Builder<CFAEdge> allEdges = ImmutableSet.builder();
    final ImmutableBiMap.Builder<CFANode, CFANode> identityBiMapOfNodes = ImmutableBiMap.builder();
    final ImmutableBiMap.Builder<CFAEdge, CFAEdge> identityBiMapOfEdges = ImmutableBiMap.builder();

    for (final CFANode node : allNodes) {
      identityBiMapOfNodes.put(node, node);

      final FluentIterable<CFAEdge> edges = CFAUtils.leavingEdges(node);
      for (final CFAEdge edge : edges) {
        allEdges.add(edge);
        identityBiMapOfEdges.put(edge, edge);
      }
    }

    return new CfaTransformationRecords(
        /* pCfaBeforeTransformation = */ Optional.empty(),
        /* pAddedEdges = */ allEdges.build(),
        /* pRemovedEdges =  */ ImmutableSet.of(),
        /* pOldEdgeToNewEdgeAfterAstNodeSubstitution = */ identityBiMapOfEdges.buildOrThrow(),
        /* pAddedNodes =  */ allNodes,
        /* pRemovedNodes = */ ImmutableSet.of(),
        /* pOldNodeToNewNodeAfterAstNodeSubstitution = */ identityBiMapOfNodes.buildOrThrow());
  }

  private static void traverseFunctionCfaAndCreateCode(
      final FunctionEntryNode pFunctionEntryNode, final GlobalExportInformation pExportInfo) {

    final CfaTransformationRecords records = pExportInfo.getTransformationRecords();
    final FunctionExportInformation functionInfo = new FunctionExportInformation();
    pExportInfo.addFunction(pFunctionEntryNode, functionInfo);

    final Deque<CFAEdge> waitList =
        new ArrayDeque<>(CFAUtils.leavingEdges(pFunctionEntryNode).toSet());

    while (!waitList.isEmpty()) {
      // TODO get next element with a more elaborate strategy due to branchings?
      final CFAEdge currentEdge = waitList.poll();
      findAndStoreLastRealFileLocationSeenBeforeReachingEdge(currentEdge, records, functionInfo);

      final CFANode nextNode = getNextNodeInFunctionCfa(currentEdge);
      final FluentIterable<CFAEdge> nextEdges = getLeavingEdgesWithRelevantAstNodes(nextNode);

      if (records.isNew(currentEdge)) {

        final ExportStatement statement = createStatementForEdge(currentEdge);
        final Optional<FileLocation> lastRealFileLocBefore =
            functionInfo.getLastRealFileLocationSeenBeforeReachingEdge(currentEdge);
        if (statement instanceof GlobalDeclaration) {
          pExportInfo.addNewGlobalDeclaration((GlobalDeclaration) statement, lastRealFileLocBefore);
        } else {
          functionInfo.addNewStatement(statement, lastRealFileLocBefore);
        }

        if (nextEdges.anyMatch(edge -> functionInfo.wasAlreadyHandled(edge))) {
          createGoto(statement, nextNode, nextEdges, functionInfo, records);

          // nextNode was already traversed, so we do not want to add its leaving edges to the
          // waitList again
          continue;
        }

        // TODO also create a goto (and label if necessary) when the next edge is an original edge
        //  that was not yet traversed, but whose FileLocation is not up next (i.e., when the
        //  transformations have altered the order of appearance of the FileLocations in the CFA)

      } else {
        // edge already existed before the CFA transformation
        final CFANode predecessor = currentEdge.getPredecessor();

        if (records.isNew(predecessor) && predecessor instanceof CFALabelNode) {

          functionInfo.addNewLabelBeforeOldEdge(
              ((CFALabelNode) predecessor).getLabel(),
              records.getEdgeBeforeTransformation(currentEdge).orElseThrow());
        }

        // TODO check whether edge is part of a set of edges representing the same statement (same
        //  FileLocation) and whether something was changed in between these edges (if yes, we can
        //  not use the original AST for export and have to create the code here

        if (nextEdges.anyMatch(edge -> functionInfo.wasAlreadyHandled(edge))) {
          // we do not need to create a goto, because this is an original edge

          // nextNode was already traversed, so we do not want to add its leaving edges to the
          // waitList again
          continue;
        }

        functionInfo.addOldEdge(currentEdge);
      }

      // TODO handle two leaving edges differently? (branching or loop)
      // TODO handle zero leaving edges differently? (add abort statement?)
      for (final CFAEdge nextEdge : nextEdges) {
        waitList.offer(nextEdge);
      }
    }
  }

  /**
   * Returns the CFANode to continue with after handling the given CFAEdge. Usually, this is the
   * successor of the given CFAEdge. However, for {@link FunctionCallEdge}s it is the successor of
   * the corresponding {@link CFunctionSummaryEdge}, so that the CFA traversal does not continue in
   * a different function.
   */
  private static CFANode getNextNodeInFunctionCfa(final CFAEdge currentEdge) {
    final CFANode nextNode;
    if (currentEdge.getEdgeType().equals(CFAEdgeType.FunctionCallEdge)) {
      nextNode = ((CFunctionCallEdge) currentEdge).getSummaryEdge().getSuccessor();
    } else {
      nextNode = currentEdge.getSuccessor();
    }
    return nextNode;
  }

  /**
   * Returns the CFAEdges leaving the given node which store the relevant {@link
   * org.sosy_lab.cpachecker.cfa.ast.AAstNode}s.
   */
  private static FluentIterable<CFAEdge> getLeavingEdgesWithRelevantAstNodes(final CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .filter(edge -> !(edge instanceof FunctionReturnEdge))
        .filter(edge -> !(edge instanceof CFunctionSummaryEdge));
  }

  /**
   * Returns all CFAEdges entering the given node (including the summary edge if the node has one)
   * whose predecessor is part of the same function (omits calls and returns from other functions).
   */
  private static FluentIterable<CFAEdge> getAllEnteringEdgesWithinFunction(final CFANode pNode) {
    return CFAUtils.allEnteringEdges(pNode)
        .filter(edge -> !(edge instanceof FunctionCallEdge))
        .filter(edge -> !(edge instanceof FunctionReturnEdge));
  }

  private static ExportStatement createStatementForEdge(final CFAEdge pEdge) {

    switch (pEdge.getEdgeType()) {
      case BlankEdge:
        {
          // in case we need to add a label here later
          return new PlaceholderStatement(pEdge);
        }

      case AssumeEdge:
        {
          final AssumeEdge assumption = (AssumeEdge) pEdge;
          if (isRealTruthAssumption(assumption)) {
            return new IfStatement(assumption);
          } else {
            // TODO assert other AssumeEdge has been handled?
            return new ElseStatement(assumption);
          }
        }

      case DeclarationEdge:
        {
          final CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;

          if (declarationEdge.getDeclaration().isGlobal()) {
            return new GlobalDeclaration(declarationEdge);
          }

          return new SimpleStatement(pEdge);
        }

      case StatementEdge:
      case ReturnStatementEdge:
      case FunctionCallEdge:
        {
          return new SimpleStatement(pEdge);
        }

      case FunctionReturnEdge:
      case CallToReturnEdge:
        {
          // this edge should not have been taken
          throw new AssertionError(
              "Edge " + pEdge + " of type " + pEdge.getEdgeType() + " in path");
        }

      default:
        {
          throw new AssertionError("Unexpected edge " + pEdge + "of type " + pEdge.getEdgeType());
        }
    }
  }

  private static boolean isRealTruthAssumption(final AssumeEdge pAssumption) {
    return pAssumption.getTruthAssumption() != pAssumption.isSwapped();
  }

  /**
   * Finds the real {@link FileLocation} that is seen last before reaching the given edge when
   * traversing the {@link CFA}. The result is stored to the given {@link
   * FunctionExportInformation}.
   *
   * @param pEdge the edge
   * @param pTransformationRecords the {@link CfaTransformationRecords} of the CFA the given edge is
   *     a part of
   * @param functionInfo the {@link FunctionExportInformation} of the function the given edge is a
   *     part of
   */
  private static void findAndStoreLastRealFileLocationSeenBeforeReachingEdge(
      final CFAEdge pEdge,
      final CfaTransformationRecords pTransformationRecords,
      final FunctionExportInformation functionInfo) {

    if (!pTransformationRecords.isNew(pEdge)) {
      // has to exist because edge is not new
      final CFAEdge edgeBeforeTransformation =
          pTransformationRecords.getEdgeBeforeTransformation(pEdge).orElseThrow();

      // better use the FileLocation of the edgeBeforeTransformation in case the FileLocation was
      // not adopted for the trivial substitute edge
      final FileLocation originalFileLoc = edgeBeforeTransformation.getFileLocation();

      if (originalFileLoc.isRealLocation()) {
        functionInfo.storeEdgeWithLastRealFileLocationSeenBefore(
            pEdge, Optional.of(originalFileLoc));
        return;
      }
    }

    final List<FileLocation> lastRealFileLocsBefore =
        getAllEnteringEdgesWithinFunction(pEdge.getPredecessor())
            .filter(edge -> functionInfo.isLastRealFileLocationKnown(edge))
            .transform(edge -> functionInfo.getLastRealFileLocationSeenBeforeReachingEdge(edge))
            .filter(optional -> optional.isPresent())
            .transform(optional -> optional.orElseThrow())
            .toList();

    if (lastRealFileLocsBefore.isEmpty()) {
      functionInfo.storeEdgeWithLastRealFileLocationSeenBefore(pEdge, Optional.empty());
      return;
    }

    final FileLocation mergedLastRealFileLocBefore = FileLocation.merge(lastRealFileLocsBefore);
    functionInfo.storeEdgeWithLastRealFileLocationSeenBefore(
        pEdge, Optional.of(mergedLastRealFileLocBefore));
  }

  private static void createGoto(
      final ExportStatement pStatement,
      final CFANode pGotoTarget,
      final FluentIterable<CFAEdge> pNextEdges,
      final FunctionExportInformation pFunctionInfo,
      final CfaTransformationRecords pRecords) {

    if (pGotoTarget instanceof CFALabelNode) {
      pStatement.addGotoTo(((CFALabelNode) pGotoTarget).getLabel());
      return;
    }

    final CFAEdge targetEdge = getTargetEdgeOfGoto(pNextEdges);
    final String label = getOrCreateLabelAtGotoTarget(targetEdge, pFunctionInfo, pRecords);
    pStatement.addGotoTo(label);
  }

  private static String getOrCreateLabelAtGotoTarget(
      final CFAEdge pTargetEdge,
      final FunctionExportInformation pFunctionInfo,
      final CfaTransformationRecords pRecords) {

    if (pRecords.isNew(pTargetEdge)) {
      // there has to be a created statement, because the edge was already traversed
      final ExportStatement targetStatement =
          pFunctionInfo.getCreatedStatementForOrigin(pTargetEdge).orElseThrow();
      return targetStatement.getOrCreateLabel();
    }

    // the edge already existed in the untransformed CFA
    return pFunctionInfo.getOrCreateLabelBeforeOldEdge(pTargetEdge);
  }

  private static CFAEdge getTargetEdgeOfGoto(final FluentIterable<CFAEdge> pNextEdges) {
    assert !pNextEdges.isEmpty()
        : "Goto creation should not have been invoked, there is nowhere to go to";

    if (pNextEdges.size() == 1) {
      return pNextEdges.first().get();
    }

    // pGotoTarget must be a branching point
    assert pNextEdges.size() == 2 : "Branches with more than two options are not supported";
    assert pNextEdges.allMatch(edge -> edge instanceof AssumeEdge)
        : "Branches must have conditions";

    // at a branching point, we want to label the IfStatement
    final FluentIterable<AssumeEdge> realTruthAssumption =
        pNextEdges.transform(edge -> (AssumeEdge) edge).filter(edge -> isRealTruthAssumption(edge));
    assert realTruthAssumption.size() == 1
        : "There has to be exactly one real truth assumption at a branching point";
    return realTruthAssumption.first().get();
  }

  private static class GlobalExportInformation {

    private final CfaTransformationRecords transformationRecords;

    private final Map<FunctionEntryNode, FunctionExportInformation> functions = new HashMap<>();
    private final Multimap<Optional<FileLocation>, GlobalDeclaration>
        newGlobalDeclarationsByFileLoc = MultimapBuilder.hashKeys().linkedListValues().build();

    private GlobalExportInformation(final CfaTransformationRecords pTransformationRecords) {
      transformationRecords = pTransformationRecords;
    }

    private CfaTransformationRecords getTransformationRecords() {
      return transformationRecords;
    }

    private void addFunction(
        final FunctionEntryNode pFunctionEntryNode, final FunctionExportInformation pFunctionInfo) {

      functions.put(pFunctionEntryNode, pFunctionInfo);
    }

    private void addNewGlobalDeclaration(
        final GlobalDeclaration pDeclaration, final Optional<FileLocation> pLastRealFileLocBefore) {

      newGlobalDeclarationsByFileLoc.put(pLastRealFileLocBefore, pDeclaration);
    }
  }

  private static class FunctionExportInformation {

    private final Map<CFAEdge, Optional<FileLocation>> edgeToLastRealFileLoc = new HashMap<>();

    private final Map<CFAEdge, ExportStatement> newStatementsByOrigin = new HashMap<>();
    private final Multimap<Optional<FileLocation>, ExportStatement>
        newStatementsByFileLocToBeInsertedAfter =
            MultimapBuilder.hashKeys().linkedListValues().build();

    private final Multimap<FileLocation, CFAEdge> visitedOldEdgesByFileLocation =
        MultimapBuilder.treeKeys().linkedListValues().build();
    private final Map<CFAEdge, String> newLabelsOnOldEdges = new HashMap<>();
    private final Map<FileLocation, String> newLabelsAtOldFileLocations = new HashMap<>();

    private FunctionExportInformation() {}

    private boolean isLastRealFileLocationKnown(final CFAEdge pEdge) {
      return edgeToLastRealFileLoc.containsKey(pEdge);
    }

    private Optional<FileLocation> getLastRealFileLocationSeenBeforeReachingEdge(
        final CFAEdge pEdge) {

      assert isLastRealFileLocationKnown(pEdge)
          : "Asked for last real FileLocation before looking for it";
      return edgeToLastRealFileLoc.get(pEdge);
    }

    private void storeEdgeWithLastRealFileLocationSeenBefore(
        final CFAEdge pEdge, final Optional<FileLocation> pLastRealFileLocBefore) {

      edgeToLastRealFileLoc.put(pEdge, pLastRealFileLocBefore);
    }

    private void addNewStatement(
        final ExportStatement pStatement, final Optional<FileLocation> pLastRealFileLocBefore) {

      final CFAEdge origin = pStatement.getOrigin();
      assert !newStatementsByOrigin.containsKey(origin) : "Edge " + origin + " was already handled";

      newStatementsByOrigin.put(origin, pStatement);
      newStatementsByFileLocToBeInsertedAfter.put(pLastRealFileLocBefore, pStatement);
    }

    private void addOldEdge(final CFAEdge pEdge) {
      visitedOldEdgesByFileLocation.put(pEdge.getFileLocation(), pEdge);
    }

    private void addNewLabelBeforeOldEdge(final String pLabel, final CFAEdge pOldEdge) {
      checkArgument(
          !(pOldEdge instanceof CDeclarationEdge
              && ((CDeclarationEdge) pOldEdge).getDeclaration().isGlobal()),
          "Global declarations can not be labeled.");

      final FileLocation fileLoc = pOldEdge.getFileLocation();

      if (fileLoc.isRealLocation()) {
        newLabelsOnOldEdges.put(pOldEdge, pLabel);
        newLabelsAtOldFileLocations.put(fileLoc, pLabel);
        return;
      }

      addNewStatement(
          new PlaceholderStatement(pOldEdge),
          getLastRealFileLocationSeenBeforeReachingEdge(pOldEdge));
    }

    private boolean wasAlreadyHandled(final CFAEdge pEdge) {
      return newStatementsByOrigin.containsKey(pEdge)
          || visitedOldEdgesByFileLocation.containsValue(pEdge);
    }

    private Optional<ExportStatement> getCreatedStatementForOrigin(final CFAEdge pOrigin) {
      return Optional.ofNullable(newStatementsByOrigin.get(pOrigin));
    }

    private String getOrCreateLabelBeforeOldEdge(final CFAEdge pOldEdge) {
      if (newLabelsOnOldEdges.containsKey(pOldEdge)) {
        // there already is a label
        return newLabelsOnOldEdges.get(pOldEdge);
      }

      final String newLabel = createNewLabelName();
      addNewLabelBeforeOldEdge(newLabel, pOldEdge);
      return newLabel;
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.cfa.CfaTransformationRecords.createTransformationRecordsForCompletelyTransformedCfa;
import static org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.createNewLabelName;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.CfaTransformationRecords;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.ClosingBraceStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.ElseStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.EmptyCCfaEdgeStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.GlobalDeclaration;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.IfStatement;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export.CCfaEdgeStatement.SimpleCCfaEdgeStatement;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.MergePoint;

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
      records = createTransformationRecordsForCompletelyTransformedCfa(pCfa);
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

  private static void traverseFunctionCfaAndCreateCode(
      final FunctionEntryNode pFunctionEntryNode, final GlobalExportInformation pExportInfo) {

    final CfaTransformationRecords records = pExportInfo.getTransformationRecords();
    final FunctionExportInformation functionInfo =
        new FunctionExportInformation(pFunctionEntryNode);
    pExportInfo.addFunction(pFunctionEntryNode, functionInfo);

    final Deque<CFAEdge> waitList =
        new ArrayDeque<>(CFAUtils.leavingEdges(pFunctionEntryNode).toSet());

    while (!waitList.isEmpty()) {
      // TODO get next element with a more elaborate strategy due to branchings?
      final CFAEdge currentEdge = waitList.poll();
      findAndStoreLastRealFileLocationSeenBeforeReachingEdge(currentEdge, records, functionInfo);

      handlePotentialJoinNode(currentEdge.getPredecessor(), functionInfo);

      final CFANode nextNode = getNextNodeInFunctionCfa(currentEdge);
      final FluentIterable<CFAEdge> nextEdges = getRelevantLeavingEdges(nextNode);

      if (records.isNew(currentEdge)) {

        final CCfaEdgeStatement statement = createStatementForEdge(currentEdge);
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

      pushToWaitList(waitList, nextNode, nextEdges, functionInfo);
    }

    closeRemainingBranchingPoints(functionInfo);
  }

  private static void handlePotentialJoinNode(
      final CFANode pPotentialJoinNode, final FunctionExportInformation pFunctionInfo) {

    final FluentIterable<CFAEdge> alreadyHandledEnteringEdges =
        getRelevantEnteringEdges(pPotentialJoinNode)
            .filter(edge -> pFunctionInfo.wasAlreadyHandled(edge));

    if (alreadyHandledEnteringEdges.size() < 2) {
      // can not be a join node
      return;
    }

    assert alreadyHandledEnteringEdges.allMatch(
            edge -> pFunctionInfo.isLastRealFileLocationKnown(edge))
        : "The last real FileLocation before reaching these edges must be known, because they were"
            + " already handled";
    final Optional<FileLocation> mergedLastRealFileLocSeenBeforeReachingPotentialJoinNode =
        mergeLastRealFileLocationsSeenBeforeReachingEdges(
            alreadyHandledEnteringEdges.toList(), pFunctionInfo);
    pFunctionInfo.closeBranchingPointsAtJoinNode(
        pPotentialJoinNode, mergedLastRealFileLocSeenBeforeReachingPotentialJoinNode);
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
   * Returns the CFAEdges leaving the given node which store the relevant {@link AAstNode}s. {@link
   * FunctionReturnEdge}s are ignored because their successor is not part of the same function
   * anymore. {@link CFunctionSummaryEdge}s are ignored, because the corresponding {@link
   * FunctionCallEdge} stores the relevant {@link AAstNode}.
   */
  private static FluentIterable<CFAEdge> getRelevantLeavingEdges(final CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .filter(edge -> !(edge instanceof FunctionReturnEdge))
        .filter(edge -> !(edge instanceof CFunctionSummaryEdge));
  }

  /**
   * Returns the CFAEdges entering the given node which store the relevant {@link AAstNode}s. This
   * means that entering {@link FunctionCallEdge}s and {@link FunctionReturnEdge}s are ignored,
   * because they are not relevant as their predecessor is not part of the same function anymore. On
   * the other hand, an entering {@link FunctionSummaryEdge}s is replaced with the corresponding
   * {@link FunctionCallEdge}, because the latter stores the relevant {@link AAstNode}.
   */
  private static FluentIterable<CFAEdge> getRelevantEnteringEdges(final CFANode pNode) {
    final FluentIterable<CFAEdge> relevantEnteringEdges =
        CFAUtils.allEnteringEdges(pNode)
            .filter(edge -> !(edge instanceof FunctionCallEdge))
            .filter(edge -> !(edge instanceof FunctionReturnEdge));

    if (pNode.getEnteringSummaryEdge() == null) {
      return relevantEnteringEdges;
    }

    final CFAEdge correspondingFunctionCallEdge =
        getOnlyElement(CFAUtils.leavingEdges(pNode.getEnteringSummaryEdge().getPredecessor()));
    assert correspondingFunctionCallEdge instanceof CFunctionCallEdge
        : "The only leaving edge of the predecessor of a CFunctionSummaryEdge should be a"
            + " CFunctionCallEdge";
    return relevantEnteringEdges.append(correspondingFunctionCallEdge);
  }

  private static CCfaEdgeStatement createStatementForEdge(final CFAEdge pEdge) {

    switch (pEdge.getEdgeType()) {
      case BlankEdge:
        {
          // in case we need to add a label here later
          return new EmptyCCfaEdgeStatement(pEdge);
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
          if (isGlobalDeclaration(pEdge)) {
            return new GlobalDeclaration((CDeclarationEdge) pEdge);
          }

          return new SimpleCCfaEdgeStatement(pEdge);
        }

      case StatementEdge:
      case ReturnStatementEdge:
      case FunctionCallEdge:
        {
          return new SimpleCCfaEdgeStatement(pEdge);
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

  private static boolean isGlobalDeclaration(final CFAEdge pEdge) {
    return pEdge instanceof CDeclarationEdge
        && ((CDeclarationEdge) pEdge).getDeclaration().isGlobal();
  }

  /**
   * Finds the real {@link FileLocation} that is seen last before reaching the given edge when
   * traversing the {@link CFA}. The result is stored to the given {@link
   * FunctionExportInformation}.
   *
   * @param pCurrentEdge the current edge
   * @param pTransformationRecords the {@link CfaTransformationRecords} of the CFA the given edge is
   *     a part of
   * @param functionInfo the {@link FunctionExportInformation} of the function the given edge is a
   *     part of
   */
  private static void findAndStoreLastRealFileLocationSeenBeforeReachingEdge(
      final CFAEdge pCurrentEdge,
      final CfaTransformationRecords pTransformationRecords,
      final FunctionExportInformation functionInfo) {

    if (!pTransformationRecords.isNew(pCurrentEdge)) {
      // has to exist because edge is not new
      final CFAEdge edgeBeforeTransformation =
          pTransformationRecords.getEdgeBeforeTransformation(pCurrentEdge).orElseThrow();

      // better use the FileLocation of the edgeBeforeTransformation in case the FileLocation was
      // not adopted for the trivial substitute edge
      final FileLocation originalFileLoc = edgeBeforeTransformation.getFileLocation();

      if (originalFileLoc.isRealLocation()) {
        functionInfo.storeEdgeWithLastRealFileLocationSeenBefore(
            pCurrentEdge, Optional.of(originalFileLoc));
        return;
      }
    }

    final Optional<FileLocation> lastRealFileLocBefore =
        getLastRealFileLocationFromPriorEdgesOfEdge(pCurrentEdge, functionInfo);

    functionInfo.storeEdgeWithLastRealFileLocationSeenBefore(pCurrentEdge, lastRealFileLocBefore);
  }

  private static Optional<FileLocation> getLastRealFileLocationFromPriorEdgesOfEdge(
      final CFAEdge pCurrentEdge, final FunctionExportInformation pFunctionInfo) {

    final Collection<CFAEdge> relevantEdges = new ArrayList<>();

    // consider prior edges within the same function
    for (final CFAEdge priorEdge : getRelevantEnteringEdges(pCurrentEdge.getPredecessor())) {

      // if the current edge is a global declaration, only consider global declarations, otherwise
      // do not consider global declarations
      if (isGlobalDeclaration(pCurrentEdge) != isGlobalDeclaration(priorEdge)) {
        continue;
      }

      // only consider edges that were already traversed
      if (!pFunctionInfo.isLastRealFileLocationKnown(priorEdge)) {
        continue;
      }

      relevantEdges.add(priorEdge);
    }

    return mergeLastRealFileLocationsSeenBeforeReachingEdges(relevantEdges, pFunctionInfo);
  }

  private static Optional<FileLocation> mergeLastRealFileLocationsSeenBeforeReachingEdges(
      final Collection<CFAEdge> pEdges, final FunctionExportInformation pFunctionInfo) {

    checkArgument(pEdges.stream().allMatch(pFunctionInfo::isLastRealFileLocationKnown));
    final List<FileLocation> lastRealFileLocsBeforeReachingEdges = new ArrayList<>();

    for (final CFAEdge edge : pEdges) {
      final Optional<FileLocation> optionalLastRealFileLocBeforePriorEdge =
          pFunctionInfo.getLastRealFileLocationSeenBeforeReachingEdge(edge);

      if (optionalLastRealFileLocBeforePriorEdge.isEmpty()) {
        continue;
      }
      lastRealFileLocsBeforeReachingEdges.add(optionalLastRealFileLocBeforePriorEdge.orElseThrow());
    }

    if (lastRealFileLocsBeforeReachingEdges.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(FileLocation.merge(lastRealFileLocsBeforeReachingEdges));
  }

  private static void createGoto(
      final CCfaEdgeStatement pStatement,
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
      final CCfaEdgeStatement targetStatement =
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

  private static void pushToWaitList(
      final Deque<CFAEdge> pWaitList,
      final CFANode pNextNode,
      final FluentIterable<CFAEdge> pNextEdges,
      final FunctionExportInformation pFunctionInfo) {

    if (pNextEdges.isEmpty()) {
      // TODO add `abort()` statement (in some cases)?
      // nothing to do, there are no next edges
      return;
    }

    if (pWaitList.containsAll(pNextEdges.toSet())) {
      // nothing to do, next edges were already added to the waitList
      return;
    }

    final Consumer<CFAEdge> waitListAction;
    final boolean acceptRealTruthAssumptionFirst;

    if (pFunctionInfo.isMergePointForAnyUnclosedBranchingPoint(pNextNode)) {
      // offer and in consequence wait with handling these edges, because when first encountering a
      // merge point, only one of the entering edges has been handled, and we want to wait until as
      // many entering edges as possible have been handled
      waitListAction = pWaitList::offer;
      // we need to offer the real truth assumption first to have it polled first
      acceptRealTruthAssumptionFirst = true;

    } else {
      // otherwise push, so that at branching points the then-branch is fully traversed before
      // starting the traversal of the second branch
      waitListAction = pWaitList::push;
      // we need to push the real truth assumption last to have it polled first
      acceptRealTruthAssumptionFirst = false;
    }

    if (pNextEdges.size() == 1) {
      waitListAction.accept(pNextEdges.first().get());
      return;
    }

    pushMultipleEdgesToWaitList(pNextEdges, waitListAction, acceptRealTruthAssumptionFirst);
  }

  private static void pushMultipleEdgesToWaitList(
      final FluentIterable<CFAEdge> pNextEdges,
      final Consumer<CFAEdge> pWaitListAction,
      final boolean pAcceptRealTruthAssumptionFirst) {

    assert pNextEdges.size() == 2 : "Branches with more than two options are not supported";
    assert pNextEdges.allMatch(edge -> edge instanceof AssumeEdge)
        : "Both branches have to have a condition";
    final AssumeEdge first = (AssumeEdge) pNextEdges.first().get();
    final AssumeEdge second = (AssumeEdge) pNextEdges.last().get();

    // make sure the real truth assumption is polled before the complimentary assumption, because
    // the then-branch has to be fully traversed before starting the traversal of the else-branch
    if (isRealTruthAssumption(first) && pAcceptRealTruthAssumptionFirst) {
      pWaitListAction.accept(first);
      pWaitListAction.accept(second);
    } else {
      pWaitListAction.accept(second);
      pWaitListAction.accept(first);
    }
  }

  private static void closeRemainingBranchingPoints(final FunctionExportInformation functionInfo) {

    if (!functionInfo.areAllBranchingPointsClosed()) {

      final Optional<FileLocation> mergedFileLocOfAllVisitedOldEdges =
          mergeLastRealFileLocationsSeenBeforeReachingEdges(
              functionInfo.getAllVisitedOldEdges(), functionInfo);
      functionInfo.closeBranchingPointsAtJoinNode(
          functionInfo.getFunctionEntryNode().getExitNode(), mergedFileLocOfAllVisitedOldEdges);
    }
    assert functionInfo.areAllBranchingPointsClosed();
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

    private final FunctionEntryNode functionEntryNode;
    private final MergePoint<CFANode> mergePoint;
    private final Set<CFANode> unclosedBranchingPoints = new HashSet<>();

    private final Map<CFAEdge, Optional<FileLocation>> edgeToLastRealFileLoc = new HashMap<>();

    private final Map<CFAEdge, CCfaEdgeStatement> newStatementsByOrigin = new HashMap<>();
    private final Multimap<Optional<FileLocation>, CCfaEdgeStatement>
        newStatementsByFileLocToBeInsertedAfter =
            MultimapBuilder.hashKeys().linkedListValues().build();

    private final Multimap<FileLocation, CFAEdge> visitedOldEdgesByFileLocation =
        MultimapBuilder.treeKeys().linkedListValues().build();
    private final Map<CFAEdge, String> newLabelsOnOldEdges = new HashMap<>();
    private final Map<FileLocation, String> newLabelsAtOldFileLocations = new HashMap<>();

    private FunctionExportInformation(final FunctionEntryNode pFunctionEntryNode) {
      functionEntryNode = pFunctionEntryNode;
      mergePoint =
          new MergePoint<>(
              pFunctionEntryNode.getExitNode(),
              CFAUtils::allSuccessorsOf,
              CFAUtils::allPredecessorsOf);
    }

    private FunctionEntryNode getFunctionEntryNode() {
      return functionEntryNode;
    }

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
        final CCfaEdgeStatement pStatement, final Optional<FileLocation> pLastRealFileLocBefore) {

      assert pStatement.getOrigin().isPresent()
          : "Expected some CFAEdge as origin for statement " + pStatement;
      final CFAEdge origin = pStatement.getOrigin().orElseThrow();
      assert !newStatementsByOrigin.containsKey(origin) : "Edge " + origin + " was already handled";

      if (pStatement instanceof IfStatement) {
        // add when encountering IfStatement so that merge point detection is possible in advance
        unclosedBranchingPoints.add(origin.getPredecessor());
      }

      newStatementsByOrigin.put(origin, pStatement);
      newStatementsByFileLocToBeInsertedAfter.put(pLastRealFileLocBefore, pStatement);
    }

    private void addOldEdge(final CFAEdge pEdge) {
      visitedOldEdgesByFileLocation.put(pEdge.getFileLocation(), pEdge);
    }

    private void addNewLabelBeforeOldEdge(final String pLabel, final CFAEdge pOldEdge) {
      checkArgument(!isGlobalDeclaration(pOldEdge), "Global declarations can not be labeled.");

      final FileLocation fileLoc = pOldEdge.getFileLocation();

      if (fileLoc.isRealLocation()) {
        newLabelsOnOldEdges.put(pOldEdge, pLabel);
        newLabelsAtOldFileLocations.put(fileLoc, pLabel);
        return;
      }

      addNewStatement(
          new EmptyCCfaEdgeStatement(pOldEdge),
          getLastRealFileLocationSeenBeforeReachingEdge(pOldEdge));
    }

    private boolean wasAlreadyHandled(final CFAEdge pEdge) {
      return newStatementsByOrigin.containsKey(pEdge)
          || visitedOldEdgesByFileLocation.containsValue(pEdge);
    }

    private Optional<CCfaEdgeStatement> getCreatedStatementForOrigin(final CFAEdge pOrigin) {
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

    private void closeBranchingPointsAtJoinNode(
        final CFANode pJoinNode, final Optional<FileLocation> pLastFileLocBefore) {

      final Collection<CFANode> closedBranchingPoints = new HashSet<>();

      for (final CFANode branchingPoint : unclosedBranchingPoints) {

        if (mergePoint.findMergePoint(branchingPoint).equals(pJoinNode)) {

          newStatementsByFileLocToBeInsertedAfter.put(
              pLastFileLocBefore, new ClosingBraceStatement());
          closedBranchingPoints.add(branchingPoint);
        }
      }

      unclosedBranchingPoints.removeAll(closedBranchingPoints);
    }

    private boolean areAllBranchingPointsClosed() {
      return unclosedBranchingPoints.isEmpty();
    }

    private Collection<CFAEdge> getAllVisitedOldEdges() {
      return visitedOldEdgesByFileLocation.values();
    }

    private boolean isMergePointForAnyUnclosedBranchingPoint(final CFANode pNode) {
      return unclosedBranchingPoints.stream()
          .anyMatch(branchingPoint -> mergePoint.findMergePoint(branchingPoint).equals(pNode));
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c.export;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Verify;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.Set;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.core.runtime.CoreException;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.ParserOptions;
import org.sosy_lab.cpachecker.cfa.CfaTransformationRecords;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.export.CWriter;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
      final CFA originalCfa = records.getCfaBeforeTransformation().orElseThrow();

      checkArgument(
          originalCfa.getFileNames().size() == 1,
          "CFA can only be exported for a single input file, at the moment.");

      try {
        originalAst =
            eclipseCdt.getASTTranslationUnit(
                EclipseCdtWrapper.wrapFile(originalCfa.getFileNames().get(0)));

        Verify.verify(
            originalAst.getPreprocessorProblemsCount() == 0,
            "Problems should have been caught during CFA generation.");

      } catch (final CoreException pE) {
        throw new CPAException("Failed to export CFA to C program because AST parsing failed.");
      }
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

    final Deque<CFAEdge> waitList =
        new ArrayDeque<>(CFAUtils.leavingEdges(pFunctionEntryNode).toSet());

    while (!waitList.isEmpty()) {
      // TODO get next element with a more elaborate strategy due to branchings?
      final CFAEdge currentEdge = waitList.poll();

      // TODO check whether predecessor is new CFALabelNode => add label

      if (records.isNew(currentEdge)) {

        final ExportStatement statement = createStatementForEdge(currentEdge);

        // TODO traverse CFA backwards to find the last original and real FileLocation ( somehow
        //  save this information so that we do not traverse the whole CFA everytime)
        // TODO store the statement with the FileLocation in the FunctionExportInformation

        // TODO if CFAEdge connects to a already handled CFANode => create goto (and label if
        //  necessary)

      } else {
        // TODO check whether edge is part of a set of edges representing the same statement (same
        //  FileLocation) and whether something was changed in between these edges (if yes, we can
        //  not use the original AST for export and have to create the code here
      }

      final CFANode nextNode = getNextNodeInFunctionCfa(currentEdge);
      // TODO handle two leaving edges differently? (branching or loop)
      // TODO handle zero leaving edges differently? (add abort statement?)
      for (final CFAEdge nextEdge : getRelevantLeavingEdges(nextNode)) {
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
  private static FluentIterable<CFAEdge> getRelevantLeavingEdges(final CFANode pNode) {
    return CFAUtils.leavingEdges(pNode)
        .filter(edge -> !(edge instanceof FunctionReturnEdge))
        .filter(edge -> !(edge instanceof CFunctionSummaryEdge));
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
          if (assumption.getTruthAssumption() != assumption.isSwapped()) {
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

  private static class GlobalExportInformation {

    private final CfaTransformationRecords transformationRecords;

    private GlobalExportInformation(final CfaTransformationRecords pTransformationRecords) {
      transformationRecords = pTransformationRecords;
    }

    private CfaTransformationRecords getTransformationRecords() {
      return transformationRecords;
    }
  }
}

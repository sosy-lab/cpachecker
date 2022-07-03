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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.EclipseCdtWrapper;
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

    // TODO collect true changes (edges and nodes, in control-flow order, by traversing the CFA)
    records.getAddedEdges();

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
}

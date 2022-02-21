// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.ASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;

abstract class AbstractSlice implements Slice {

  private final CFA originalCfa;
  private final ImmutableCollection<CFAEdge> slicingCriteria;
  private final ImmutableSet<CFAEdge> relevantEdges;
  private final ImmutableSet<ASimpleDeclaration> relevantDeclarations;

  AbstractSlice(
      CFA pOriginalCfa,
      Collection<CFAEdge> pSlicingCriteria,
      Collection<CFAEdge> pRelevantEdges,
      Set<ASimpleDeclaration> pRelevantDeclarations) {

    originalCfa = pOriginalCfa;
    slicingCriteria = ImmutableList.copyOf(pSlicingCriteria);
    relevantEdges = ImmutableSet.copyOf(pRelevantEdges);
    relevantDeclarations = ImmutableSet.copyOf(pRelevantDeclarations);
  }

  static ImmutableSet<ASimpleDeclaration> computeRelevantDeclarations(
      Collection<CFAEdge> pRelevantEdges,
      Predicate<ASimpleDeclaration> pRelevantDeclarationFilter) {

    var relevantDeclarationCollectingVisitor =
        new RelevantDeclarationCollectingVisitor(pRelevantDeclarationFilter);

    for (CFAEdge relevantEdge : pRelevantEdges) {

      if (relevantEdge instanceof CDeclarationEdge) {
        ((CDeclarationEdge) relevantEdge)
            .getDeclaration()
            .accept(relevantDeclarationCollectingVisitor);
      }

      for (CFANode relevantNode :
          ImmutableList.of(relevantEdge.getPredecessor(), relevantEdge.getSuccessor())) {
        if (relevantNode instanceof FunctionEntryNode) {

          FunctionEntryNode relevantFunctionEntryNode = (FunctionEntryNode) relevantNode;
          Optional<? extends ASimpleDeclaration> optionalReturnVariable =
              relevantFunctionEntryNode.getReturnVariable();

          optionalReturnVariable
              .filter(returnVariable -> returnVariable instanceof CVariableDeclaration)
              .map(returnVariable -> (CVariableDeclaration) returnVariable)
              .ifPresent(
                  returnVariable -> returnVariable.accept(relevantDeclarationCollectingVisitor));
        }
      }
    }

    return ImmutableSet.copyOf(relevantDeclarationCollectingVisitor.getRelevantDeclarations());
  }

  @Override
  public CFA getOriginalCfa() {
    return originalCfa;
  }

  @Override
  public ImmutableCollection<CFAEdge> getSlicingCriteria() {
    return slicingCriteria;
  }

  @Override
  public ImmutableSet<CFAEdge> getRelevantEdges() {
    return relevantEdges;
  }

  @Override
  public ImmutableSet<ASimpleDeclaration> getRelevantDeclarations() {
    return relevantDeclarations;
  }

  // TODO: don't use TransformingCAstNodeVisitor
  // This is not an AST node transformation. We just want to find all declarations in an AST node.
  // Use a more appropriate CAstNodeVisitor instead or implement CAstNodeVisitor directly (be aware
  // of cyclic references between variable declarations and their initializer expressions that may
  // lead to infinite recursive calls).
  private static final class RelevantDeclarationCollectingVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final Predicate<ASimpleDeclaration> relevantDeclarationFilter;
    private Set<ASimpleDeclaration> relevantDeclarations;

    private RelevantDeclarationCollectingVisitor(
        Predicate<ASimpleDeclaration> pRelevantDeclarationFilter) {

      relevantDeclarationFilter = pRelevantDeclarationFilter;
      relevantDeclarations = new LinkedHashSet<>();
    }

    private Set<ASimpleDeclaration> getRelevantDeclarations() {
      return relevantDeclarations;
    }

    private CAstNode collectDeclarationIfRelevant(
        ASimpleDeclaration pDeclaration, Supplier<CAstNode> pSuperReturnValueSupplier) {

      if (relevantDeclarationFilter.test(pDeclaration)) {
        relevantDeclarations.add(pDeclaration);
      }

      return pSuperReturnValueSupplier.get();
    }

    @Override
    public CAstNode visit(CVariableDeclaration pDeclaration) {
      return collectDeclarationIfRelevant(pDeclaration, () -> super.visit(pDeclaration));
    }

    @Override
    public CAstNode visit(CParameterDeclaration pDeclaration) {
      return collectDeclarationIfRelevant(pDeclaration, () -> super.visit(pDeclaration));
    }

    @Override
    public CAstNode visit(CFunctionDeclaration pDeclaration) {
      return collectDeclarationIfRelevant(pDeclaration, () -> super.visit(pDeclaration));
    }

    @Override
    public CAstNode visit(CComplexTypeDeclaration pDeclaration) {
      return collectDeclarationIfRelevant(pDeclaration, () -> super.visit(pDeclaration));
    }

    @Override
    public CAstNode visit(CTypeDefDeclaration pDeclaration) {
      return collectDeclarationIfRelevant(pDeclaration, () -> super.visit(pDeclaration));
    }
  }
}

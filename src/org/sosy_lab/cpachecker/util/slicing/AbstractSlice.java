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

  private final CFA cfa;
  private final ImmutableCollection<CFAEdge> criteria;
  private final ImmutableSet<CFAEdge> relevantEdges;
  private final ImmutableSet<ASimpleDeclaration> relevantDeclarations;

  AbstractSlice(
      CFA pCfa,
      Collection<CFAEdge> pSlicingCriteria,
      Collection<CFAEdge> pRelevantEdges,
      Predicate<ASimpleDeclaration> pRelevantDeclarationFilter) {

    cfa = pCfa;
    relevantEdges = ImmutableSet.copyOf(pRelevantEdges);
    criteria = ImmutableList.copyOf(pSlicingCriteria);
    relevantDeclarations = computeRelevantDeclarations(pRelevantEdges, pRelevantDeclarationFilter);
  }

  private static ImmutableSet<ASimpleDeclaration> computeRelevantDeclarations(
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

      CFANode relevantNode = relevantEdge.getSuccessor();
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

    return ImmutableSet.copyOf(relevantDeclarationCollectingVisitor.getRelevantDeclarations());
  }

  @Override
  public CFA getOriginalCfa() {
    return cfa;
  }

  @Override
  public ImmutableCollection<CFAEdge> getSlicingCriteria() {
    return criteria;
  }

  @Override
  public ImmutableSet<CFAEdge> getRelevantEdges() {
    return relevantEdges;
  }

  @Override
  public ImmutableSet<ASimpleDeclaration> getRelevantDeclarations() {
    return relevantDeclarations;
  }

  // TODO: don't use TransformingCAstNodeVisitor, use a more appropriate CAstNodeVisitor instead
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

    @Override
    public CAstNode visit(CVariableDeclaration pCVariableDeclaration) {

      if (relevantDeclarationFilter.test(pCVariableDeclaration)) {
        relevantDeclarations.add(pCVariableDeclaration);
      }

      return super.visit(pCVariableDeclaration);
    }

    @Override
    public CAstNode visit(CParameterDeclaration pCParameterDeclaration) {

      if (relevantDeclarationFilter.test(pCParameterDeclaration)) {
        relevantDeclarations.add(pCParameterDeclaration);
      }

      return super.visit(pCParameterDeclaration);
    }

    @Override
    public CAstNode visit(CFunctionDeclaration pCFunctionDeclaration) {

      if (relevantDeclarationFilter.test(pCFunctionDeclaration)) {
        relevantDeclarations.add(pCFunctionDeclaration);
      }

      return super.visit(pCFunctionDeclaration);
    }

    @Override
    public CAstNode visit(CComplexTypeDeclaration pCComplexTypeDeclaration) {

      if (relevantDeclarationFilter.test(pCComplexTypeDeclaration)) {
        relevantDeclarations.add(pCComplexTypeDeclaration);
      }

      return super.visit(pCComplexTypeDeclaration);
    }

    @Override
    public CAstNode visit(CTypeDefDeclaration pCTypeDefDeclaration) {

      if (relevantDeclarationFilter.test(pCTypeDefDeclaration)) {
        relevantDeclarations.add(pCTypeDefDeclaration);
      }

      return super.visit(pCTypeDefDeclaration);
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;


import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCfaNetwork;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

final class CfaSimplifications {

  private CfaSimplifications() {}

  /**
   * Returns a simplified CFA with only a single array operation per CFA-edge.
   *
   * @param pCfa the CFA to simplify
   * @return the simplified CFA
   */
  static CFA simplifyArrayAccesses(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pCfa,
      VariableGenerator pVariableGenerator) {

    MutableCfaNetwork graph = MutableCfaNetwork.of(pCfa);
    CAstNodeSubstitution substitution = new CAstNodeSubstitution();

    // copy of edges to prevent concurrent modification of graph
    for (CFAEdge edge : ImmutableSet.copyOf(graph.edges())) {

      Set<ArrayAccess> remainingArrayAccesses = new HashSet<>(ArrayAccess.findArrayAccesses(edge));

      // finished array access ---> substitute for finished array access
      Map<ArrayAccess, CExpression> finished = new HashMap<>();

      // only a single writing array access per edge is supported
      assert remainingArrayAccesses.stream().filter(ArrayAccess::isWrite).count() <= 1;

      // a single array access is allowed to remain
      while (remainingArrayAccesses.size() > 1) {

        Iterator<ArrayAccess> remainingArrayAccessesIterator = remainingArrayAccesses.iterator();
        while (remainingArrayAccessesIterator.hasNext()) {

          ArrayAccess current = remainingArrayAccessesIterator.next();
          ImmutableSet<ArrayAccess> currentArrayAccesses =
              ArrayAccess.findArrayAccesses(current.getExpression());

          // array accesses can only be processes after all the array accesses they rely on are
          // finished (e.g., a[b[10]] -> process b[10] -> b[10] finished -> process a[b[10]])
          if (Sets.difference(currentArrayAccesses, finished.keySet()).size() == 1) {
            remainingArrayAccessesIterator.remove();

            if (current.isRead()) {

              FileLocation fileLocation = edge.getFileLocation();
              CFANode predecessor = graph.incidentNodes(edge).nodeU();

              CInitializerExpression initializerExpression =
                  new CInitializerExpression(fileLocation, current.getExpression());
              String newVarName = pVariableGenerator.createNewVariableName();
              String newVarQualifiedName =
                  MemoryLocation.forLocalVariable(predecessor.getFunctionName(), newVarName)
                      .getExtendedQualifiedName();

              CVariableDeclaration declaration =
                  new CVariableDeclaration(
                      fileLocation,
                      false,
                      CStorageClass.AUTO,
                      current.getExpression().getExpressionType(),
                      newVarName,
                      newVarName,
                      newVarQualifiedName,
                      initializerExpression);

              CFAEdge newDeclarationEdge =
                  new CDeclarationEdge(
                      "",
                      fileLocation,
                      CFANode.newDummyCFANode(),
                      CFANode.newDummyCFANode(),
                      declaration);

              for (Map.Entry<ArrayAccess, CExpression> finishedEntry : finished.entrySet()) {
                substitution.insertSubstitute(
                    newDeclarationEdge,
                    finishedEntry.getKey().getExpression(),
                    finishedEntry.getValue());
              }

              graph.insertPredecessor(
                  new CFANode(predecessor.getFunction()), predecessor, newDeclarationEdge);

              CIdExpression substituteExpression = new CIdExpression(fileLocation, declaration);
              substitution.insertSubstitute(edge, current.getExpression(), substituteExpression);

              finished.put(current, substituteExpression);
            }
          }
        }
      }
    }

    return CCfaTransformer.createCfa(
        pConfiguration,
        pLogger,
        pCfa,
        graph,
        (edge, originalAstNode) ->
            ArrayAccessSubstitutingCAstNodeVisitor.substitute(substitution, edge, originalAstNode));
  }

  static CFA simplifyIncDecLoopEdges(Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    MutableCfaNetwork graph = MutableCfaNetwork.of(pCfa);
    Map<CFAEdge, Map<CSimpleDeclaration, CExpression>> substitution = new HashMap<>();

    VariableClassification variableClassification = pCfa.getVarClassification().orElseThrow();

    for (TransformableLoop loop : TransformableLoop.getTransformableLoops(pCfa)) {
      for (CFAEdge innerLoopEdge : loop.getInnerLoopEdges()) {
        Optional<TransformableLoop.IncDecEdge> optIncDecEdge =
            TransformableLoop.createIncDecEdge(innerLoopEdge);
        if (optIncDecEdge.isPresent()) {
          TransformableLoop.IncDecEdge incDecEdge = optIncDecEdge.orElseThrow();
          CSimpleDeclaration declaration = incDecEdge.getDeclaration();

          if (declaration.equals(loop.getIndexVariable())) {
            continue;
          }

          if (loop.hasOutgoingUses(declaration)) {
            continue;
          }

          String qualifiedName = declaration.getQualifiedName();
          if (variableClassification.getAddressedVariables().contains(qualifiedName)) {
            continue;
          }

          if (loop.countInnerLoopDefs(declaration) > 1) {
            continue;
          }

          if (incDecEdge.getOperator() != BinaryOperator.PLUS) {
            continue;
          }

          CFAEdge updateIndexEdge = loop.getUpdateIndexEdge();
          CIdExpression indexIdExpression =
              new CIdExpression(innerLoopEdge.getFileLocation(), loop.getIndexVariable());

          ImmutableSet<CFAEdge> indexDominated = loop.getDominatedInnerLoopEdges(updateIndexEdge);
          ImmutableSet<CFAEdge> indexPostDominated =
              loop.getPostDominatedInnerLoopEdges(updateIndexEdge);

          ImmutableSet<CFAEdge> dominated = loop.getDominatedInnerLoopEdges(innerLoopEdge);
          ImmutableSet<CFAEdge> postDominated = loop.getPostDominatedInnerLoopEdges(innerLoopEdge);

          for (CFAEdge edge : Iterables.concat(dominated, postDominated)) {
            int indexPlus = 0;
            if (indexDominated.contains(edge) && postDominated.contains(edge)) {
              indexPlus = -1;
            } else if (indexPostDominated.contains(edge) && dominated.contains(edge)) {
              indexPlus = 1;
            }

            CIntegerLiteralExpression indexPlusExpression =
                new CIntegerLiteralExpression(
                    edge.getFileLocation(), CNumericTypes.INT, BigInteger.valueOf(indexPlus));

            CIntegerLiteralExpression stepExpression =
                new CIntegerLiteralExpression(
                    edge.getFileLocation(), CNumericTypes.INT, incDecEdge.getConstant());

            CBinaryExpression indexBinaryExpression =
                new CBinaryExpression(
                    edge.getFileLocation(),
                    declaration.getType(),
                    declaration.getType(),
                    indexIdExpression,
                    indexPlusExpression,
                    BinaryOperator.PLUS);

            CBinaryExpression stepIndexResultExpression =
                new CBinaryExpression(
                    edge.getFileLocation(),
                    declaration.getType(),
                    declaration.getType(),
                    indexBinaryExpression,
                    stepExpression,
                    BinaryOperator.MULTIPLY);

            CIdExpression startValueExpression =
                new CIdExpression(edge.getFileLocation(), declaration);
            CBinaryExpression substituteExpression =
                new CBinaryExpression(
                    edge.getFileLocation(),
                    declaration.getType(),
                    declaration.getType(),
                    startValueExpression,
                    stepIndexResultExpression,
                    BinaryOperator.PLUS);

            Map<CSimpleDeclaration, CExpression> declarationSubstitution =
                substitution.computeIfAbsent(edge, key -> new HashMap<>());
            declarationSubstitution.put(declaration, substituteExpression);
          }

          var endpoints = graph.incidentNodes(innerLoopEdge);
          graph.removeEdge(innerLoopEdge);

          BlankEdge placeholderEdge =
              new BlankEdge(
                  "",
                  innerLoopEdge.getFileLocation(),
                  innerLoopEdge.getPredecessor(),
                  innerLoopEdge.getSuccessor(),
                  "");
          graph.addEdge(endpoints, placeholderEdge);
        }
      }
    }

    return CCfaTransformer.createCfa(
        pConfiguration,
        pLogger,
        pCfa,
        graph,
        (edge, originalAstNode) ->
            IdExpressionSubstitutingCAstNodeVisitor.substitute(
                substitution, edge, originalAstNode));
  }

  private static final class ArrayAccessSubstitutingCAstNodeVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final CAstNodeSubstitution substitution;
    private final CFAEdge edge;

    public ArrayAccessSubstitutingCAstNodeVisitor(
        CAstNodeSubstitution pSubstitution, CFAEdge pEdge) {
      substitution = pSubstitution;
      edge = pEdge;
    }

    private static CAstNode substitute(
        CAstNodeSubstitution pSubstitution, CFAEdge pEdge, CAstNode pOriginalAstNode) {

      ArrayAccessSubstitutingCAstNodeVisitor transformingVisitor =
          new ArrayAccessSubstitutingCAstNodeVisitor(pSubstitution, pEdge);

      return pOriginalAstNode.accept(transformingVisitor);
    }

    @Override
    public CAstNode visit(CArraySubscriptExpression pCArraySubscriptExpression) {

      CAstNode substitute = substitution.getSubstitute(edge, pCArraySubscriptExpression);

      if (substitute != null) {
        return substitute;
      }

      return super.visit(pCArraySubscriptExpression);
    }

    @Override
    public CAstNode visit(CPointerExpression pCPointerExpression) {

      CAstNode substitute = substitution.getSubstitute(edge, pCPointerExpression);

      if (substitute != null) {
        return substitute;
      }

      return super.visit(pCPointerExpression);
    }
  }

  private static final class IdExpressionSubstitutingCAstNodeVisitor
      extends AbstractTransformingCAstNodeVisitor<NoException> {

    private final Map<CSimpleDeclaration, CExpression> substitution;

    public IdExpressionSubstitutingCAstNodeVisitor(
        Map<CSimpleDeclaration, CExpression> pSubstitution) {
      substitution = pSubstitution;
    }

    private static CAstNode substitute(
        Map<CFAEdge, Map<CSimpleDeclaration, CExpression>> pSubstitution,
        CFAEdge pEdge,
        CAstNode pOriginalAstNode) {

      Map<CSimpleDeclaration, CExpression> declarationSubstitution = pSubstitution.get(pEdge);

      if (declarationSubstitution != null) {

        IdExpressionSubstitutingCAstNodeVisitor transformingVisitor =
            new IdExpressionSubstitutingCAstNodeVisitor(declarationSubstitution);

        return pOriginalAstNode.accept(transformingVisitor);
      }

      return pOriginalAstNode;
    }

    @Override
    public CAstNode visit(CIdExpression pCIdExpression) throws NoException {

      CSimpleDeclaration declaration = pCIdExpression.getDeclaration();
      CExpression substitute = substitution.get(declaration);

      if (substitute != null) {
        return substitute;
      }

      return super.visit(pCIdExpression);
    }
  }
}

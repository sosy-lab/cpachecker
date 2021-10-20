// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableMap;
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
import java.util.function.BiFunction;
import java.util.function.Function;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CCfaTransformer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCfaNetwork;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.AbstractTransformingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
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
   * @param pConfiguration the configuration to use
   * @param pLogger the logger to use
   * @param pCfa the CFA to simplify
   * @param pVariableGenerator the variable generator to use
   * @return the simplified CFA
   */
  static CFA simplifyArrayAccesses(
      Configuration pConfiguration,
      LogManager pLogger,
      CFA pCfa,
      VariableGenerator pVariableGenerator) {

    MutableCfaNetwork graph = MutableCfaNetwork.of(pCfa);
    Map<CFAEdge, Map<ArrayAccess, CAstNode>> substitution = new HashMap<>();

    // copy of edges to prevent concurrent modification of graph
    for (CFAEdge edge : ImmutableSet.copyOf(graph.edges())) {

      // skip function summary edges here, modify them when the corresponding call edge is modified
      // (to prevent doing the same modification twice)
      if (edge instanceof FunctionSummaryEdge) {
        continue;
      }

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
                substitution
                    .computeIfAbsent(newDeclarationEdge, key -> new HashMap<>())
                    .put(finishedEntry.getKey(), finishedEntry.getValue());
              }

              graph.insertPredecessor(
                  new CFANode(predecessor.getFunction()), predecessor, newDeclarationEdge);

              CIdExpression substituteExpression = new CIdExpression(fileLocation, declaration);
              substitution
                  .computeIfAbsent(edge, key -> new HashMap<>())
                  .put(current, substituteExpression);
              if (edge instanceof FunctionCallEdge) {
                FunctionSummaryEdge summaryEdge = edge.getPredecessor().getLeavingSummaryEdge();
                assert summaryEdge != null : "Missing summary edge for call edge";
                substitution
                    .computeIfAbsent(summaryEdge, key -> new HashMap<>())
                    .put(current, substituteExpression);
              }

              finished.put(current, substituteExpression);
            }
          }
        }
      }
    }

    BiFunction<CFAEdge, CAstNode, CAstNode> substitutionFunction =
        (edge, originalAstNode) -> {
          Map<ArrayAccess, CAstNode> arrayAccessSubstitution = substitution.get(edge);

          if (arrayAccessSubstitution == null) {
            return originalAstNode;
          }

          // there should be no writing accesses, we only replace reading accesses
          assert arrayAccessSubstitution.keySet().stream()
              .filter(ArrayAccess::isWrite)
              .findAny()
              .isEmpty();

          Function<Map.Entry<ArrayAccess, ?>, CAstNode> extractExpression =
              entry -> entry.getKey().getExpression();
          ImmutableMap<CAstNode, CAstNode> astNodeSubstitution =
              arrayAccessSubstitution.entrySet().stream()
                  .collect(ImmutableMap.toImmutableMap(extractExpression, Map.Entry::getValue));

          // TODO: add support for more edges/statements that can contain writing array accesses
          if (edge instanceof CStatementEdge) {
            CStatement statement = ((CStatementEdge) edge).getStatement();
            if (statement instanceof CExpressionAssignmentStatement) {
              var assignStatement = (CExpressionAssignmentStatement) statement;
              CAstNode rhs =
                  assignStatement
                      .getRightHandSide()
                      .accept(new SubstitutingCAstNodeVisitor(astNodeSubstitution::get));
              return new CExpressionAssignmentStatement(
                  assignStatement.getFileLocation(),
                  assignStatement.getLeftHandSide(),
                  (CExpression) rhs);
            }
          }

          return originalAstNode.accept(new SubstitutingCAstNodeVisitor(astNodeSubstitution::get));
        };

    return CCfaTransformer.createCfa(pConfiguration, pLogger, pCfa, graph, substitutionFunction);
  }

  /**
   * Returns a simplified CFA where certain loop carried dependencies are eliminated.
   *
   * <p>Example simplification:
   *
   * <pre>
   * {@code int j = 0; for (int i = 0; i < 100; i++) { print(j); j = j + 5; } }
   * </pre>
   *
   * <p>is turned into
   *
   * <pre>
   * {@code int j = 0; for (int i = 0; i < 100; i++) { print(j + (i * 5)); } }
   * </pre>
   *
   * @param pConfiguration the configuration to use
   * @param pLogger the logger to use
   * @param pCfa the CFA to simplify
   * @return the simplified CFA
   */
  static CFA simplifyIncDecLoopEdges(Configuration pConfiguration, LogManager pLogger, CFA pCfa) {

    MutableCfaNetwork graph = MutableCfaNetwork.of(pCfa);
    Map<CFAEdge, Map<CSimpleDeclaration, CExpression>> substitution = new HashMap<>();

    VariableClassification variableClassification = pCfa.getVarClassification().orElseThrow();

    for (TransformableLoop loop : TransformableLoop.findTransformableLoops(pCfa)) {

      // we only support loops with indices that are increased by one every loop iteration
      // TODO: this can be further optimized if necessary
      if (!loop.getIndex().getUpdateOperation().getStepValue().equals(BigInteger.ONE)) {
        continue;
      }

      for (CFAEdge innerLoopEdge : loop.getInnerLoopEdges()) {
        Optional<SpecialOperation.UpdateAssign> optUpdateAssign =
            SpecialOperation.UpdateAssign.forEdge(
                innerLoopEdge, pCfa.getMachineModel(), ImmutableMap.of());
        if (optUpdateAssign.isPresent()) {

          SpecialOperation.UpdateAssign updateAssign = optUpdateAssign.orElseThrow();
          CSimpleDeclaration declaration = updateAssign.getDeclaration();

          // the index cannot cannot be eliminated
          if (declaration.equals(loop.getIndex().getVariableDeclaration())) {
            continue;
          }

          // we don't remember the value when leaving the loop
          // TODO: this can be further optimized if necessary
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

          // we only support variables that are increased every loop iteration
          // TODO: this can be further optimized if necessary
          if (updateAssign.getStepValue().compareTo(BigInteger.ZERO) <= 0) {
            continue;
          }

          if (!loop.isExecutedEveryIteration(innerLoopEdge)) {
            continue;
          }

          CFAEdge updateIndexEdge = loop.getIndex().getUpdateEdge();
          CIdExpression indexIdExpression =
              new CIdExpression(
                  innerLoopEdge.getFileLocation(), loop.getIndex().getVariableDeclaration());

          ImmutableSet<CFAEdge> indexDominated = loop.getDominatedInnerLoopEdges(updateIndexEdge);
          ImmutableSet<CFAEdge> indexPostDominated =
              loop.getPostDominatedInnerLoopEdges(updateIndexEdge);

          ImmutableSet<CFAEdge> currentDominated = loop.getDominatedInnerLoopEdges(innerLoopEdge);
          ImmutableSet<CFAEdge> currentPostDominated =
              loop.getPostDominatedInnerLoopEdges(innerLoopEdge);

          for (CFAEdge edge : Iterables.concat(currentDominated, currentPostDominated)) {

            // formula: (j at edge) = (j before loop) + (((index at edge) + indexPlus) * (j update
            // step))

            // edge permutations => indexPlus:
            //   indexUpdate currentUpdate edge =>  0
            //   indexUpdate edge currentUpdate => -1
            //   currentUpdate indexUpdate edge =>  0
            //   currentUpdate edge indexUpdate => +1
            //   edge indexUpdate currentUpdate =>  0
            //   edge currentUpdate indexUpdate =>  0

            int indexPlus = 0;
            if (indexDominated.contains(edge) && currentPostDominated.contains(edge)) {
              indexPlus = -1;
            } else if (indexPostDominated.contains(edge) && currentDominated.contains(edge)) {
              indexPlus = 1;
            }

            CIntegerLiteralExpression indexPlusExpression =
                new CIntegerLiteralExpression(
                    edge.getFileLocation(), CNumericTypes.INT, BigInteger.valueOf(indexPlus));

            CBinaryExpression indexBinaryExpression =
                new CBinaryExpression(
                    edge.getFileLocation(),
                    declaration.getType(),
                    declaration.getType(),
                    indexIdExpression,
                    indexPlusExpression,
                    BinaryOperator.PLUS);

            CIntegerLiteralExpression stepExpression =
                new CIntegerLiteralExpression(
                    edge.getFileLocation(), CNumericTypes.INT, updateAssign.getStepValue());

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

          // replace update edge with empty placeholder
          var endpoints = graph.incidentNodes(innerLoopEdge);
          graph.removeEdge(innerLoopEdge);
          graph.addEdge(
              endpoints,
              new BlankEdge(
                  "",
                  innerLoopEdge.getFileLocation(),
                  innerLoopEdge.getPredecessor(),
                  innerLoopEdge.getSuccessor(),
                  ""));
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

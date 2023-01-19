// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.SubstitutingCAstNodeVisitor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.graph.FlexCfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.LoopStructurePostProcessor;
import org.sosy_lab.cpachecker.cfa.postprocessing.function.ReversePostorderPostProcessor;
import org.sosy_lab.cpachecker.cfa.postprocessing.global.VariableClassificationPostProcessor;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactory;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.c.CCfaFactory;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/**
 * A {@link CfaTransformer} that creates CFAs that have only a single array operation per CFA edge.
 */
final class SingleArrayOperationSimplifier implements CfaTransformer {

  private final Configuration config;
  private final VariableGenerator variableGenerator;

  SingleArrayOperationSimplifier(Configuration pConfig, VariableGenerator pVariableGenerator) {
    config = pConfig;
    variableGenerator = pVariableGenerator;
  }

  @Override
  public CFA transform(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {
    FlexCfaNetwork graph = FlexCfaNetwork.copy(pCfaNetwork);
    Map<CFAEdge, Map<ArrayAccess, CAstNode>> substitution = new HashMap<>();

    // copy of edges to prevent concurrent modification of graph
    for (CFAEdge edge : ImmutableSet.copyOf(graph.edges())) {

      // skip function summary edges here, modify them when the corresponding call edge is modified
      // (to prevent doing the same modification twice)
      if (edge instanceof FunctionSummaryEdge) {
        continue;
      }

      Set<ArrayAccess> remainingArrayAccesses =
          new LinkedHashSet<>(ArrayAccess.findArrayAccesses(edge));

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
              String newVarName = variableGenerator.createNewVariableName();
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
                  new CFANode(predecessor.getFunction()), newDeclarationEdge, predecessor);

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
            if (statement instanceof CExpressionAssignmentStatement assignStatement) {
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

    CfaFactory cfaFactory =
        CCfaFactory.toUnconnectedFunctions()
            .transformEdges(CCfaEdgeTransformer.forSubstitutions(substitutionFunction::apply))
            .executePostProcessor(new ReversePostorderPostProcessor())
            .executePostProcessor(new LoopStructurePostProcessor())
            .toSupergraph()
            .executePostProcessor(new VariableClassificationPostProcessor(config));

    return cfaFactory.createCfa(graph, pCfaMetadata, pLogger, pShutdownNotifier);
  }
}

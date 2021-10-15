// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

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

      Set<ArrayAccess> remainingArrayAccesses = new HashSet<>(ArrayAccess.getArrayAccesses(edge));

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
              ArrayAccess.getArrayAccesses(current.getExpression());

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
}

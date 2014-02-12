/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cfa.simplification;

import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;


public class ExpressionSimplifier implements CFAVisitor {

  private static final String ONLY_C_SUPPORTED =
      "only simplification of C-code is supported"; // TODO support Java

  private final MachineModel machineModel;
  private final LogManager logger;


  // Map of oldEdge to newEdge,
  // List is enough, because we only add edges and at last we iterate over them.
  private final List<Pair<CFAEdge, CFAEdge>> e2e = new ArrayList<>();



  public ExpressionSimplifier(final MachineModel mm, final LogManager pLogger) {
    this.machineModel = mm;
    this.logger = pLogger;
  }


  @Override
  public TraversalProcess visitEdge(final CFAEdge edge) {
    replaceEdgeWithSimplifiedEdge(edge);
    return TraversalProcess.CONTINUE;
  }


  @Override
  public TraversalProcess visitNode(final CFANode node) {
    return TraversalProcess.CONTINUE;
  }


  /** This method analyses the edge for expressions and evaluates them.
   * If possible, the edge is replaced with a new edge with the simplified expression. */
  private void replaceEdgeWithSimplifiedEdge(final CFAEdge oldEdge) {
    // this info is needed for all types of edges
    final CFANode start = oldEdge.getPredecessor();
    final CFANode end = oldEdge.getSuccessor();
    final int line = oldEdge.getLineNumber();
    final String rawStatement = oldEdge.getRawStatement();

    final CFAEdge newEdge = getEdgeWithSimplifiedExpression(
        oldEdge, start, end, line, rawStatement);

    if (oldEdge != newEdge) {
      // something has changed, so we replace the edge with the new one.
      // we cannot replace it now, because we iterate over the CFA.
      // so we store the new edge and call "replaceEdges()" later.

      e2e.add(Pair.of(oldEdge, newEdge));
    }
  }

  /** This method replaces some edges with their simplified versions.
   * This method should be called after the traversal of the CFA. */
  public void replaceEdges() {
    for (Pair<CFAEdge, CFAEdge> pair : e2e) {

      final CFAEdge oldEdge = pair.getFirst();
      final CFAEdge newEdge = pair.getSecond();

      final CFANode start = oldEdge.getPredecessor();
      final CFANode end = oldEdge.getSuccessor();

      // replace old edge with new edge, link nodes with edge
      start.removeLeavingEdge(oldEdge);
      end.removeEnteringEdge(oldEdge);
      start.addLeavingEdge(newEdge);
      end.addEnteringEdge(newEdge);
    }
  }


  /** This method switches between all types of edges and searches for expressions.
   * If an expression is found, it is evaluated and the result is packed into a new edge.
   * If nothing is changed (when nothing can be simplified),
   * the original edge-object is returned. */
  private CFAEdge getEdgeWithSimplifiedExpression(final CFAEdge edge,
      final CFANode start, final CFANode end, final int line, final String raw) {

    // clone correct type of edge
    switch (edge.getEdgeType()) {

    case AssumeEdge: {
      if (edge instanceof CAssumeEdge) {
        final CAssumeEdge e = (CAssumeEdge) edge;
        final CExpression exp = e.getExpression();
        final CExpression newExp = simplify(exp);

        if (newExp == exp) { return edge; }

        return new CAssumeEdge(raw, line, start, end, newExp, e.getTruthAssumption());

      } else {
        throw new AssertionError(ONLY_C_SUPPORTED);
      }
    }

    case StatementEdge: {
      if (edge instanceof CFunctionSummaryStatementEdge) {
        return edge; // nothing to do
      } else if (edge instanceof CStatementEdge) {
        final CStatementEdge e = (CStatementEdge) edge;
        final CStatement statement = e.getStatement();
        final CStatement newStatement;

        if (statement instanceof CAssignment) {
          final CAssignment assignment = (CAssignment) statement;
          final CRightHandSide rhs = assignment.getRightHandSide();

          if (rhs instanceof CExpression) {
            final CExpression exp = (CExpression) rhs;
            final CExpression newExpr = simplify(exp);

            if (newExpr == exp) { return edge; }

            newStatement = new CExpressionAssignmentStatement(
                statement.getFileLocation(), assignment.getLeftHandSide(), newExpr);

          } else if (rhs instanceof CFunctionCallExpression) {
            // handle params of functionCall, maybe there is a sideeffect
            CFunctionCallExpression rhsExp = (CFunctionCallExpression) rhs;

            final List<CExpression> list = rhsExp.getParameterExpressions();
            final List<CExpression> newList = simplify(list);

            if (list == newList) { return edge; }

            final CFunctionCallExpression newRhsExp = new CFunctionCallExpression(
                rhsExp.getFileLocation(), rhsExp.getExpressionType(),
                rhsExp.getFunctionNameExpression(), newList, rhsExp.getDeclaration());
            newStatement = new CFunctionCallAssignmentStatement(
                statement.getFileLocation(), assignment.getLeftHandSide(), newRhsExp);
          } else {
            throw new AssertionError("unknown rhs");
          }

          // call of external function, "scanf(...)" without assignment
          // internal functioncalls are handled as FunctionCallEdges
        } else if (statement instanceof CFunctionCallStatement) {
          final CFunctionCallStatement fcStatement = (CFunctionCallStatement) statement;
          final CFunctionCallExpression fcExp = fcStatement.getFunctionCallExpression();
          final List<CExpression> list = fcExp.getParameterExpressions();
          final List<CExpression> newList = simplify(list);

          if (list == newList) { return edge; }

          final CFunctionCallExpression newRhsExp = new CFunctionCallExpression(
              fcExp.getFileLocation(), fcExp.getExpressionType(),
              fcExp.getFunctionNameExpression(), newList, fcExp.getDeclaration());
          newStatement = new CFunctionCallStatement(statement.getFileLocation(), newRhsExp);

        } else if (statement instanceof CExpressionStatement) {
          final CExpressionStatement cest = (CExpressionStatement) statement;
          final CExpression exp = cest.getExpression();
          final CExpression newExpr = simplify(exp);

          if (newExpr == exp) { return edge; }

          newStatement = new CExpressionStatement(statement.getFileLocation(), newExpr);

        } else {
          throw new AssertionError("unknown statement");
        }

        return new CStatementEdge(raw, newStatement, line, start, end);

      } else {
        throw new AssertionError(ONLY_C_SUPPORTED);
      }
    }

    case DeclarationEdge: {
      if (!(edge instanceof CDeclarationEdge)) { return edge; }

      final CDeclarationEdge e = (CDeclarationEdge) edge;
      final CDeclaration decl = e.getDeclaration();

      if (!(decl instanceof CVariableDeclaration)) { return edge; }

      final CVariableDeclaration vdecl = (CVariableDeclaration) decl;
      final CInitializer initializer = vdecl.getInitializer();

      if (!(initializer instanceof CInitializerExpression)) { return edge; }

      final CExpression exp = ((CInitializerExpression) initializer).getExpression();
      final CExpression newExp = simplify(exp);
      if (exp == newExp) { return edge; }
      final CInitializer newInit = new CInitializerExpression(
          initializer.getFileLocation(), newExp);
      final CDeclaration newDecl = new CVariableDeclaration(vdecl.getFileLocation(),
          vdecl.isGlobal(), vdecl.getCStorageClass(), vdecl.getType(),
          vdecl.getName(), vdecl.getOrigName(), vdecl.getQualifiedName(), newInit);

      return new CDeclarationEdge(raw, line, start, end, newDecl);
    }

    case ReturnStatementEdge: {
      if (edge instanceof CReturnStatementEdge) {
        final CReturnStatementEdge e = (CReturnStatementEdge) edge;

        if (!e.getRawAST().isPresent()) { return edge; }

        final CReturnStatement retStatement = e.getRawAST().get();
        final CExpression exp = retStatement.getReturnValue();

        if (exp == null) { return edge; }

        final CExpression newExp = simplify(exp);

        if (exp == newExp) { return edge; }

        final CReturnStatement newRetStatement = new CReturnStatement(
            retStatement.getFileLocation(), newExp);

        return new CReturnStatementEdge(raw, newRetStatement,
            line, start, (FunctionExitNode) end);

      } else {
        throw new AssertionError(ONLY_C_SUPPORTED);
      }
    }

    case FunctionCallEdge: {
      if (edge instanceof CFunctionCallEdge) {
        final CFunctionCallEdge e = (CFunctionCallEdge) edge;
        final CFunctionSummaryEdge sumEdge = e.getSummaryEdge();
        final CFunctionCall fc = sumEdge.getExpression();
        final CFunctionCallExpression fcExp = fc.getFunctionCallExpression();
        final FileLocation loc = fc.getFileLocation();

        final List<CExpression> list = fcExp.getParameterExpressions();
        final List<CExpression> newList = simplify(list);
        if (list == newList) { return edge; }
        final CFunctionCallExpression newFcExp = new CFunctionCallExpression(
            loc, fcExp.getExpressionType(), fcExp.getFunctionNameExpression(),
            list, fcExp.getDeclaration());
        final CFunctionCall newFc = new CFunctionCallStatement(loc, newFcExp);

        return new CFunctionCallEdge(raw, line, start, (CFunctionEntryNode) end,
            newFc, e.getSummaryEdge());

      } else {
        throw new AssertionError(ONLY_C_SUPPORTED);
      }
    }

    case FunctionReturnEdge:
    case BlankEdge:
    case CallToReturnEdge:
      // nothing to do, because there is no expression
      return edge;

    case MultiEdge: {
      throw new AssertionError("MultiEdge not supported"); // TODO necessary?
    }

    default:
      throw new AssertionError("unknown edge");
    }
  }


  /** Simplifiy all elements in the list.
   *  If no expression is changed, the original list-object is returned. */
  private List<CExpression> simplify(final List<CExpression> exprs) {
    final List<CExpression> result = new ArrayList<>(exprs.size());
    boolean hasChanged = false;
    for (CExpression expr : exprs) {
      CExpression newExp = simplify(expr);
      result.add(newExp);
      if (newExp != expr) {
        hasChanged = true;
      }
    }
    return hasChanged ? result : exprs;
  }


  /** This method evaluates an expression and returns the simplified expression.
   * If no simplification is possible, the original expression-object is returned.*/
  private CExpression simplify(final CExpression expr) {
    final ExpressionSimplificationVisitor v = new ExpressionSimplificationVisitor(machineModel, logger);
    final Pair<CExpression, Number> eval = expr.accept(v);
    return eval.getFirst();
  }
}

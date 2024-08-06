// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration.FunctionAttribute;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

@SuppressWarnings("unused")
public class AtExitTransformer {
  private final MutableCFA cfa;

  private final LogManager logger;
  private final Configuration config;

  private static int variableCounter = 0;

  public AtExitTransformer(MutableCFA pCFA, LogManager pLogger, Configuration pConfiguration) {
    cfa = pCFA;
    logger = pLogger;
    config = pConfiguration;
  }

  /** Check if the atexit() function is used by the program */
  private boolean usesAtExit() {
    for (CFAEdge edge : ImmutableList.copyOf(cfa.edges())) {
      if (edge instanceof CStatementEdge stmtEdge
          && stmtEdge.getStatement() instanceof CFunctionCallStatement callStmt
          && callStmt.getFunctionCallExpression().getFunctionNameExpression()
              instanceof CIdExpression nameExpr
          && nameExpr.getName().equals("atexit")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if it's a "return" edge and get the expression that will be returned
   *
   * <p>Returns {@link Optional#empty()} for all other edges
   */
  private Optional<CExpression> tryReturnEdge(CFAEdge pEdge) {
    if (pEdge instanceof CReturnStatementEdge returnEdge) {
      // The default value for "return" is actually undefined.
      // Since this method is only called for the main() function the actual value does not matter,
      // and we simply return 0 as a placeholder.
      return Optional.of(returnEdge.getExpression().orElse(CIntegerLiteralExpression.ZERO));
    } else if (pEdge instanceof BlankEdge && pEdge.getDescription().equals("default return")) {
      return Optional.of(CIntegerLiteralExpression.ZERO);
    } else {
      return Optional.empty();
    }
  }

  /**
   * Replace all "return" statements in main() with exit() calls
   *
   * <p>Meant to be used as a first step before calling {@link AtExitTransformer#addExitHandlers()}
   */
  public void replaceReturns() {
    for (CFAEdge edge : ImmutableList.copyOf(cfa.edges())) {
      // Match all "return" statements in the main function
      Optional<CExpression> maybeReturnValue = tryReturnEdge(edge);
      if (edge.getPredecessor().getFunctionName().equals("main") && maybeReturnValue.isPresent()) {
        FileLocation loc = edge.getFileLocation();
        CExpression value = maybeReturnValue.orElseThrow();

        // Convert the "return" statement to a call to exit()
        CType intType =
            new CSimpleType(
                false, false, CBasicType.INT, false, false, false, false, false, false, false);
        ImmutableList<CParameterDeclaration> exitArgs =
            ImmutableList.of(new CParameterDeclaration(loc, intType, "status"));
        CFunctionType exitType = new CFunctionTypeWithNames(CVoidType.VOID, exitArgs, false);
        CFunctionDeclaration exitDecl =
            new CFunctionDeclaration(
                loc, exitType, "exit", exitArgs, ImmutableSet.of(FunctionAttribute.NO_RETURN));
        CFunctionCallExpression callExpr =
            new CFunctionCallExpression(
                loc,
                CVoidType.VOID,
                new CIdExpression(loc, exitDecl),
                ImmutableList.of(value),
                exitDecl);
        CFunctionCallStatement callStmt = new CFunctionCallStatement(loc, callExpr);
        CStatementEdge exitEdge =
            new CStatementEdge("", callStmt, loc, edge.getPredecessor(), edge.getSuccessor());

        // Remove the "return" edge
        CFACreationUtils.removeEdgeFromNodes(edge);

        // Add the new edge to call exit() instead
        CFACreationUtils.addEdgeUnconditionallyToCFA(exitEdge);
      }
    }
  }

  /** Add a new node to the CFA */
  private CFANode mkNode(CFunctionDeclaration pDeclaration) {
    CFANode nextNode = new CFANode(pDeclaration);
    cfa.addNode(nextNode);
    return nextNode;
  }

  /** Generate a new variable name */
  private String mkTmpVariable() {
    return "__atexit_TMP_" + variableCounter++;
  }

  /**
   * Add runtime support for atexit handlers
   *
   * <p>Whenever exit() is called and the program shuts down the runtime first needs to execute any
   * handlers that have been registered with atexit(). This method adds additional edges before any
   * call to exit() to handle these calls. It is meant to be called after {@link
   * AtExitTransformer#replaceReturns()} has already been executed and all `returns` in the main()
   * function have been convert to exit() calls.
   */
  public void addExitHandlers() {
    for (CFAEdge edge : ImmutableList.copyOf(cfa.edges())) {
      // Match all calls to "exit"
      if (edge instanceof CStatementEdge stmtEdge
          && stmtEdge.getStatement() instanceof CFunctionCallStatement callStmt
          && callStmt.getFunctionCallExpression().getFunctionNameExpression()
              instanceof CIdExpression nameExpr
          && nameExpr.getName().equals("exit")) {
        CFANode n0 = edge.getPredecessor();
        CFunctionDeclaration scope = (CFunctionDeclaration) n0.getFunction();
        FileLocation loc = edge.getFileLocation();

        // Remove the old edge from the predecessor
        CFACreationUtils.removeEdgeFromNodes(edge);

        // Add a blank edge for the while loop
        CFANode n1 = mkNode(scope);
        CFAEdge e1 = new BlankEdge("", loc, n0, n1, "while");
        CFACreationUtils.addEdgeUnconditionallyToCFA(e1);

        // Mark the node as a loop head
        n1.setLoopStart();

        // Add an edge to declare a tmp variable for the __CPACHECKER_atexit_next() call
        CFANode n2 = mkNode(scope);
        CType functionType = new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);
        CType fpointerType = new CPointerType(false, false, functionType);
        String var2 = mkTmpVariable();
        CVariableDeclaration declVar2 =
            new CVariableDeclaration(
                loc,
                false,
                CStorageClass.AUTO,
                fpointerType,
                var2,
                var2,
                scope.getQualifiedName() + "::" + var2,
                null);
        CFAEdge e2 = new CDeclarationEdge("", loc, n1, n2, declVar2);
        CFACreationUtils.addEdgeUnconditionallyToCFA(e2);

        // Add an edge for the function call to __CPACHECKER_atexit_next()
        CFANode n3 = mkNode(scope);
        CFunctionDeclaration declNext =
            new CFunctionDeclaration(
                loc,
                new CFunctionType(fpointerType, ImmutableList.of(), false),
                "__CPACHECKER_atexit_next",
                ImmutableList.of(),
                ImmutableSet.of());
        CFunctionCallExpression callNext =
            new CFunctionCallExpression(
                loc, fpointerType, new CIdExpression(loc, declNext), ImmutableList.of(), declNext);
        CFunctionCallAssignmentStatement stmtNext =
            new CFunctionCallAssignmentStatement(loc, new CIdExpression(loc, declVar2), callNext);
        CFAEdge e3 = new CStatementEdge("", stmtNext, loc, n2, n3);
        CFACreationUtils.addEdgeUnconditionallyToCFA(e3);

        // Add two assumption edge to branch on the result of __CPACHECKER_atexit_next():
        CType intType =
            new CSimpleType(
                false, false, CBasicType.INT, false, false, false, false, false, false, false);
        CBinaryExpression isEmpty =
            new CBinaryExpression(
                loc,
                fpointerType,
                intType,
                new CIdExpression(loc, fpointerType, var2, declVar2),
                CIntegerLiteralExpression.ZERO,
                BinaryOperator.EQUALS);
        // "then"
        CFANode n4 = mkNode(scope);
        CAssumeEdge e4 = new CAssumeEdge("", loc, n3, n4, isEmpty, true);
        CFACreationUtils.addEdgeUnconditionallyToCFA(e4);
        // "else"
        CFANode n5 = mkNode(scope);
        CAssumeEdge e5 = new CAssumeEdge("", loc, n3, n5, isEmpty, false);
        CFACreationUtils.addEdgeUnconditionallyToCFA(e5);

        // Exit the loop if __CPACHECKER_atexit_next() has returned a null pointer and the stack is
        // empty
        // Add an edge that calls exit and returns to the rest of the graph
        CFunctionCallStatement stmtExit =
            new CFunctionCallStatement(loc, callStmt.getFunctionCallExpression());
        CFAEdge e0 = new CStatementEdge("", stmtExit, loc, n4, edge.getSuccessor());
        CFACreationUtils.addEdgeUnconditionallyToCFA(e0);

        // Otherwise continue with the body of the loop:
        // Add an edge for the call to the function pointer
        CPointerExpression expFPointer =
            new CPointerExpression(loc, functionType, new CIdExpression(loc, declVar2));
        CFunctionCallStatement callFPointer =
            new CFunctionCallStatement(
                loc,
                new CFunctionCallExpression(
                    loc, CVoidType.VOID, expFPointer, ImmutableList.of(), null));
        CFAEdge e6 = new CStatementEdge("", callFPointer, loc, n5, n1);
        CFACreationUtils.addEdgeUnconditionallyToCFA(e6);
      }
    }
  }

  /**
   * Rewrite return statements and add atexit handlers to the CFA if the atexit() is used by the
   * program.
   */
  public void transformIfNeeded() {
    if (usesAtExit()) {
      // TODO: These two transformation could be merged
      replaceReturns();
      addExitHandlers();
    }
  }
}

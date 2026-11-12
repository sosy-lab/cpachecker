// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.ADeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;

/**
 * Models setjmp and longjmp for programs with exactly one call of setjmp, which is in a function F
 * that is not recursive.
 *
 * <p>The call of setjmp returns 0. A call longjmp(env, val) stores val (or 1 if val is 0) in a
 * global variable and returns from all functions until F is reached, where the call of setjmp then
 * returns this value. After each call of a function that can call longjmp, the global variable is
 * checked. The local variables of F keep their values (C11 7.13.2.1 makes those that are not
 * volatile and were changed after setjmp indeterminate). A longjmp while F is not active has
 * undefined behavior and ends the program path.
 *
 * <p>The CFA of all other programs is not changed, so their calls of longjmp stay unsupported.
 */
public final class SetjmpLongjmpTransformer {

  private static final ImmutableSet<String> SETJMP =
      ImmutableSet.of("setjmp", "_setjmp", "sigsetjmp", "__sigsetjmp");
  private static final ImmutableSet<String> LONGJMP =
      ImmutableSet.of("longjmp", "_longjmp", "siglongjmp");
  private static final ImmutableSet<String> THREAD_CREATE =
      ImmutableSet.of("pthread_create", "pthread_once");
  private static final String VALUE_VAR = "__CPAchecker_longjmp_value";
  private static final String TMP_VAR = "__CPAchecker_longjmp_TMP_";

  private final MutableCFA cfa;
  private final LogManager logger;
  private final CBinaryExpressionBuilder binExprBuilder;
  private final CVariableDeclaration valueDecl;
  private int tmpCounter = 0;

  public SetjmpLongjmpTransformer(MutableCFA pCfa, LogManager pLogger) {
    cfa = pCfa;
    logger = pLogger;
    binExprBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);
    valueDecl =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            CNumericTypes.INT,
            VALUE_VAR,
            VALUE_VAR,
            VALUE_VAR,
            new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO),
            false);
  }

  /** Returns the name of the directly called function, or empty for a call via a pointer. */
  private static Optional<String> getCalledFunction(CFunctionCall pCall) {
    if (pCall.getFunctionCallExpression().getFunctionNameExpression() instanceof CIdExpression id
        && (id.getDeclaration() == null || id.getDeclaration() instanceof CFunctionDeclaration)) {
      return Optional.of(id.getName());
    }
    return Optional.empty();
  }

  private void logUnsupported(String pReason) {
    logger.log(Level.INFO, "Cannot model setjmp and longjmp in this program:", pReason);
  }

  /**
   * Transforms the CFA if the program calls longjmp and fulfills the conditions given above.
   *
   * @param pGlobalDeclarations the global declarations, the declaration of the new global variable
   *     is added to it
   */
  public void transformIfNeeded(List<Pair<ADeclaration, String>> pGlobalDeclarations) {
    List<CStatementEdge> setjmpCalls = new ArrayList<>();
    List<CStatementEdge> longjmpCalls = new ArrayList<>();
    // calls of functions with a body and calls via pointers
    List<CStatementEdge> calls = new ArrayList<>();
    SetMultimap<String, String> callees = HashMultimap.create();
    Set<String> functionsWithPointerCalls = new HashSet<>();
    Set<String> functionsWithLongjmp = new HashSet<>();
    boolean createsThreads = false;

    for (CFAEdge edge : cfa.edges()) {
      if (edge instanceof CStatementEdge stmtEdge
          && stmtEdge.getStatement() instanceof CFunctionCall call) {
        String caller = edge.getPredecessor().getFunctionName();
        Optional<String> callee = getCalledFunction(call);
        if (callee.isEmpty()) {
          functionsWithPointerCalls.add(caller);
          calls.add(stmtEdge);
        } else if (SETJMP.contains(callee.orElseThrow())) {
          setjmpCalls.add(stmtEdge);
        } else if (LONGJMP.contains(callee.orElseThrow())) {
          longjmpCalls.add(stmtEdge);
          functionsWithLongjmp.add(caller);
        } else if (THREAD_CREATE.contains(callee.orElseThrow())) {
          createsThreads = true;
        } else if (cfa.getAllFunctionNames().contains(callee.orElseThrow())) {
          callees.put(caller, callee.orElseThrow());
          calls.add(stmtEdge);
        }
      }
    }

    if (longjmpCalls.isEmpty()) {
      return;
    }
    if (setjmpCalls.size() != 1) {
      logUnsupported(setjmpCalls.size() + " calls of setjmp");
      return;
    }
    if (createsThreads) {
      logUnsupported("threads");
      return;
    }
    if (Iterables.any(cfa.getAllFunctionNames(), f -> SETJMP.contains(f) || LONGJMP.contains(f))) {
      logUnsupported("program defines setjmp or longjmp");
      return;
    }
    if (Iterables.any(
        longjmpCalls,
        e ->
            ((CFunctionCall) e.getStatement())
                    .getFunctionCallExpression()
                    .getParameterExpressions()
                    .size()
                != 2)) {
      logUnsupported("call of longjmp without two arguments");
      return;
    }
    CReferencedFunctionsCollector referencedFunctions = new CReferencedFunctionsCollector();
    cfa.edges().forEach(referencedFunctions::visitEdge);
    for (Pair<ADeclaration, String> decl : pGlobalDeclarations) {
      if (decl.getFirst() instanceof CVariableDeclaration varDecl) {
        referencedFunctions.visitDeclaration(varDecl);
      }
    }
    if (Iterables.any(
        referencedFunctions.getCollectedFunctions(),
        f -> SETJMP.contains(f) || LONGJMP.contains(f))) {
      logUnsupported("address of setjmp or longjmp is used");
      return;
    }

    CStatementEdge setjmpCall = setjmpCalls.getFirst();
    String setjmpFunction = setjmpCall.getPredecessor().getFunctionName();
    String mainFunction = cfa.getMainFunction().getFunctionName();

    // a call via a pointer can call each function whose address is used
    Set<String> pointerTargets =
        Sets.intersection(referencedFunctions.getCollectedFunctions(), cfa.getAllFunctionNames());
    for (String caller : functionsWithPointerCalls) {
      callees.putAll(caller, pointerTargets);
    }
    if (reachableFunctions(callees, callees.get(setjmpFunction)).contains(setjmpFunction)) {
      logUnsupported("function " + setjmpFunction + " with setjmp may be recursive");
      return;
    }

    // functions that may return because of a longjmp; F catches all longjmps
    Set<String> mayLongjmp =
        reachableFunctions(invert(callees, setjmpFunction), functionsWithLongjmp);
    for (String function : mayLongjmp) {
      if (!function.equals(setjmpFunction)
          && !function.equals(mainFunction)
          && cfa.getFunctionHead(function).getExitNode().isEmpty()) {
        logUnsupported("function " + function + " never returns");
        return;
      }
    }

    boolean pointerCallsMayLongjmp =
        Iterables.any(pointerTargets, f -> mayLongjmp.contains(f) && !f.equals(setjmpFunction));

    pGlobalDeclarations.add(Pair.of(valueDecl, "int " + VALUE_VAR + " = 0;"));
    Set<String> changedFunctions = new HashSet<>();

    CFANode longjmpTarget = transformSetjmp(setjmpCall);
    for (CStatementEdge call : calls) {
      Optional<String> callee = getCalledFunction((CFunctionCall) call.getStatement());
      if (callee.isEmpty()
          ? pointerCallsMayLongjmp
          : (mayLongjmp.contains(callee.orElseThrow())
              && !callee.orElseThrow().equals(setjmpFunction))) {
        transformCall(call, setjmpFunction, mainFunction, longjmpTarget);
        changedFunctions.add(call.getPredecessor().getFunctionName());
      }
    }
    for (CStatementEdge longjmpCall : longjmpCalls) {
      transformLongjmp(longjmpCall, setjmpFunction, mainFunction, longjmpTarget);
      changedFunctions.add(longjmpCall.getPredecessor().getFunctionName());
    }

    changedFunctions.forEach(this::removeUnreachableNodes);
    logger.log(
        Level.INFO,
        "Modeling setjmp and longjmp with",
        longjmpCalls.size(),
        "calls of longjmp and setjmp in function",
        setjmpFunction);
  }

  private static SetMultimap<String, String> invert(
      SetMultimap<String, String> pCallees, String pExcludedCallee) {
    SetMultimap<String, String> callers = HashMultimap.create();
    pCallees.forEach(
        (caller, callee) -> {
          if (!callee.equals(pExcludedCallee)) {
            callers.put(callee, caller);
          }
        });
    return callers;
  }

  /** Returns the functions reachable from the given ones, including them. */
  private static Set<String> reachableFunctions(
      SetMultimap<String, String> pCallGraph, Iterable<String> pStart) {
    Set<String> reached = new HashSet<>();
    Deque<String> waitlist = new ArrayDeque<>();
    Iterables.addAll(waitlist, pStart);
    while (!waitlist.isEmpty()) {
      String function = waitlist.pop();
      if (reached.add(function)) {
        waitlist.addAll(pCallGraph.get(function));
      }
    }
    return reached;
  }

  private CFANode newNode(CFANode pNodeInFunction) {
    CFANode node = new CFANode(pNodeInFunction.getFunction());
    cfa.addNode(node);
    return node;
  }

  private static void addEdge(CFAEdge pEdge) {
    CFACreationUtils.addEdgeUnconditionallyToCFA(pEdge);
  }

  private CIdExpression valueVar(FileLocation pLoc) {
    return new CIdExpression(pLoc, valueDecl);
  }

  private CStatementEdge assignmentEdge(
      CLeftHandSide pLhs, CExpression pRhs, CStatementEdge pOrig, CFANode pFrom, CFANode pTo) {
    CStatement stmt = new CExpressionAssignmentStatement(pOrig.getFileLocation(), pLhs, pRhs);
    return new CStatementEdge(stmt.toASTString(), stmt, pOrig.getFileLocation(), pFrom, pTo);
  }

  /**
   * Replaces the call of setjmp by its direct return with value 0 and returns the node where
   * longjmp continues.
   */
  private CFANode transformSetjmp(CStatementEdge pSetjmp) {
    CFANode pred = pSetjmp.getPredecessor();
    CFANode succ = pSetjmp.getSuccessor();
    FileLocation loc = pSetjmp.getFileLocation();
    CFANode target = newNode(pred);
    CFACreationUtils.removeEdgeFromNodes(pSetjmp);
    // longjmp creates a loop, e.g., k-induction needs its head
    succ.setLoopStart();

    if (pSetjmp.getStatement() instanceof CFunctionCallAssignmentStatement assignment) {
      CLeftHandSide lhs = assignment.getLeftHandSide();
      addEdge(assignmentEdge(lhs, CIntegerLiteralExpression.ZERO, pSetjmp, pred, succ));
      CFANode node = newNode(pred);
      addEdge(assignmentEdge(lhs, valueVar(loc), pSetjmp, target, node));
      addEdge(assignmentEdge(valueVar(loc), CIntegerLiteralExpression.ZERO, pSetjmp, node, succ));
    } else {
      addEdge(new BlankEdge(pSetjmp.getRawStatement(), loc, pred, succ, "setjmp returns 0"));
      addEdge(assignmentEdge(valueVar(loc), CIntegerLiteralExpression.ZERO, pSetjmp, target, succ));
    }
    return target;
  }

  /** Adds an edge from the given node that leaves the function because of a longjmp. */
  private void addLongjmpExit(
      CFANode pFrom,
      CStatementEdge pOrig,
      String pSetjmpFunction,
      String pMainFunction,
      CFANode pLongjmpTarget) {
    String function = pFrom.getFunctionName();
    final CFANode target;
    final String description;
    if (function.equals(pSetjmpFunction)) {
      target = pLongjmpTarget;
      description = "continue after setjmp";
    } else if (function.equals(pMainFunction)) {
      // longjmp to a function that is not active
      target = new CFATerminationNode(pFrom.getFunction());
      cfa.addNode(target);
      description = "longjmp without active setjmp";
    } else {
      FunctionEntryNode entry = cfa.getFunctionHead(function);
      target = entry.getExitNode().orElseThrow();
      description = "return because of longjmp";
    }
    addEdge(new BlankEdge("", pOrig.getFileLocation(), pFrom, target, description));
  }

  /** Inserts a check for a longjmp after a call. */
  private void transformCall(
      CStatementEdge pCall, String pSetjmpFunction, String pMainFunction, CFANode pLongjmpTarget) {
    CFANode pred = pCall.getPredecessor();
    CFANode succ = pCall.getSuccessor();
    FileLocation loc = pCall.getFileLocation();
    CFANode afterCall = newNode(pred);
    CFACreationUtils.removeEdgeFromNodes(pCall);

    CFANode noLongjmp;
    if (pCall.getStatement() instanceof CFunctionCallAssignmentStatement assignment) {
      // assign the result only if the call returned normally
      String name = TMP_VAR + tmpCounter++;
      CVariableDeclaration tmpDecl =
          new CVariableDeclaration(
              loc,
              false,
              CStorageClass.AUTO,
              assignment.getRightHandSide().getExpressionType(),
              name,
              name,
              pred.getFunctionName() + "::" + name,
              null,
              false);
      CFANode declared = newNode(pred);
      addEdge(new CDeclarationEdge(tmpDecl.toASTString(), loc, pred, declared, tmpDecl));
      CStatement tmpCall =
          new CFunctionCallAssignmentStatement(
              loc, new CIdExpression(loc, tmpDecl), assignment.getRightHandSide());
      addEdge(new CStatementEdge(tmpCall.toASTString(), tmpCall, loc, declared, afterCall));
      noLongjmp = newNode(pred);
      addEdge(
          assignmentEdge(
              assignment.getLeftHandSide(),
              new CIdExpression(loc, tmpDecl),
              pCall,
              noLongjmp,
              succ));
    } else {
      addEdge(
          new CStatementEdge(pCall.getRawStatement(), pCall.getStatement(), loc, pred, afterCall));
      noLongjmp = succ;
    }

    CBinaryExpression isLongjmp =
        binExprBuilder.buildBinaryExpressionUnchecked(
            valueVar(loc), CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);
    String raw = isLongjmp.toASTString();
    CFANode longjmp = newNode(pred);
    addEdge(new CAssumeEdge(raw, loc, afterCall, longjmp, isLongjmp, true));
    addEdge(new CAssumeEdge(raw, loc, afterCall, noLongjmp, isLongjmp, false));
    addLongjmpExit(longjmp, pCall, pSetjmpFunction, pMainFunction, pLongjmpTarget);
  }

  /** Replaces a call of longjmp by setting the global variable and leaving the function. */
  private void transformLongjmp(
      CStatementEdge pLongjmp,
      String pSetjmpFunction,
      String pMainFunction,
      CFANode pLongjmpTarget) {
    CFANode pred = pLongjmp.getPredecessor();
    FileLocation loc = pLongjmp.getFileLocation();
    CExpression value =
        ((CFunctionCall) pLongjmp.getStatement())
            .getFunctionCallExpression()
            .getParameterExpressions()
            .get(1);
    CFACreationUtils.removeEdgeFromNodes(pLongjmp);

    CFANode valueSet = newNode(pred);
    if (value instanceof CIntegerLiteralExpression literal
        && literal.getValue().signum() != 0
        && literal.getValue().bitLength() < 32) {
      addEdge(assignmentEdge(valueVar(loc), value, pLongjmp, pred, valueSet));
    } else {
      // longjmp(env, 0) makes setjmp return 1
      CFANode assigned = newNode(pred);
      CFANode isZero = newNode(pred);
      addEdge(assignmentEdge(valueVar(loc), value, pLongjmp, pred, assigned));
      CBinaryExpression notZero =
          binExprBuilder.buildBinaryExpressionUnchecked(
              valueVar(loc), CIntegerLiteralExpression.ZERO, BinaryOperator.NOT_EQUALS);
      String raw = notZero.toASTString();
      addEdge(new CAssumeEdge(raw, loc, assigned, valueSet, notZero, true));
      addEdge(new CAssumeEdge(raw, loc, assigned, isZero, notZero, false));
      addEdge(
          assignmentEdge(valueVar(loc), CIntegerLiteralExpression.ONE, pLongjmp, isZero, valueSet));
    }
    addLongjmpExit(valueSet, pLongjmp, pSetjmpFunction, pMainFunction, pLongjmpTarget);
  }

  /** Removes the nodes that are not reachable anymore, e.g., the code after longjmp. */
  private void removeUnreachableNodes(String pFunction) {
    FunctionEntryNode entry = cfa.getFunctionHead(pFunction);
    Set<CFANode> reached = new HashSet<>();
    Deque<CFANode> waitlist = new ArrayDeque<>();
    waitlist.add(entry);
    while (!waitlist.isEmpty()) {
      CFANode node = waitlist.pop();
      if (reached.add(node)) {
        CFAUtils.successorsOf(node).forEach(waitlist::push);
      }
    }
    for (CFANode node : ImmutableList.copyOf(cfa.getFunctionNodes(pFunction))) {
      if (!reached.contains(node)) {
        for (CFAEdge edge : node.getAllLeavingEdges().toList()) {
          CFACreationUtils.removeEdgeFromNodes(edge);
        }
        for (CFAEdge edge : node.getAllEnteringEdges().toList()) {
          CFACreationUtils.removeEdgeFromNodes(edge);
        }
        if (node instanceof FunctionExitNode) {
          entry.removeExitNode();
        }
        cfa.removeNode(node);
      }
    }
  }
}

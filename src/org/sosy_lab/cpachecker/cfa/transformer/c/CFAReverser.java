// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.CFAReversePostorder;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProvider;
import org.sosy_lab.cpachecker.util.automaton.TargetLocationProviderImpl;

/** Transformer to reverse the CFA */
public class CFAReverser {
  /**
   * Returns a new CFA that represents the reverse CFA
   *
   * <p>The original CFA is not modified.
   *
   * @param pConfiguration the configuration that was used to create the original CFA
   * @param pSpecification the specification
   * @param pLogger the logger to use
   * @param pCfa the original CFA
   * @return a new CFA that represents the reverse CFA
   * @throws NullPointerException if any of the parameters is {@code null}
   */
  public static CFA reverseCfa(
      Configuration pConfiguration,
      Specification pSpecification,
      LogManager pLogger,
      CFA pCfa,
      ShutdownNotifier shutdownNotifier) {

    checkNotNull(pConfiguration);
    checkNotNull(pSpecification);
    checkNotNull(pLogger);
    checkNotNull(pCfa);
    CfaBuilder cfabBuilder =
        new CfaBuilder(pConfiguration, pSpecification, pLogger, pCfa, shutdownNotifier);

    return cfabBuilder.createCfa().immutableCopy();
  }

  /** The reverse CFA builder */
  private static final class CfaBuilder {
    private final CFA pCfa;
    private final Specification pSpec;
    private final LogManager pLog;
    private final TargetLocationProvider targetFinder;
    private final NavigableMap<String, FunctionEntryNode> functions;
    private final TreeMultimap<String, CFANode> nodes;
    private final Map<CFunctionDeclaration, CFunctionDeclaration> funcDeclMap;
    private final Map<CFANode, CFANode> nodeMap;
    private final ImmutableSet<CFANode> targets;
    private final Map<String, CFunctionDeclaration> funcDecls;
    private static CType intType =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, false, false, false, false);

    private CfaBuilder(
        Configuration pConfiguration,
        Specification pSpecification,
        LogManager pLogger,
        CFA pCfa,
        ShutdownNotifier shutdownNotifier) {
      checkNotNull(pConfiguration);
      this.pSpec = pSpecification;
      this.pLog = pLogger;
      this.pCfa = pCfa;
      this.targetFinder = new TargetLocationProviderImpl(shutdownNotifier, pLogger, pCfa);
      this.functions = new TreeMap<>();
      this.nodes = TreeMultimap.create();
      this.funcDeclMap = new HashMap<>();
      this.nodeMap = new HashMap<>();
      this.funcDecls = new HashMap<>();
      // Search for the target in the original CFA
      this.targets = targetFinder.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpec);
    }

    private CFA createCfa() {

      // create a dummy main
      CFunctionEntryNode reverseMainEntry = null; // = newDummyMain();

      // Reverse each Function's CFA
      for (Map.Entry<String, FunctionEntryNode> function : pCfa.getAllFunctions().entrySet()) {
        String name = function.getKey();
        CFunctionEntryNode entryNode = (CFunctionEntryNode) function.getValue();
        CfaFunctionBuilder cfaFuncBuilder = new CfaFunctionBuilder(entryNode);
        CFunctionEntryNode reverseEntryNode = cfaFuncBuilder.reverse();
        functions.put(name, reverseEntryNode);
        reverseMainEntry = reverseEntryNode;
      }

      checkNotNull(reverseMainEntry);

      MutableCFA newMutableCfa =
          new MutableCFA(
              functions, nodes, pCfa.getMetadata().withMainFunctionEntry(reverseMainEntry));

      newMutableCfa.entryNodes().forEach(CFAReversePostorder::assignIds);

      LoopStructure loopStructure = null;
      try {
        loopStructure = LoopStructure.getLoopStructure(newMutableCfa);
        pLog.log(Level.INFO, "found loop count: " + loopStructure.getCount());
      } catch (Exception e) {
        pLog.log(Level.WARNING, "failed to get the loop structure" + e);
      }
      newMutableCfa.setLoopStructure(loopStructure);

      return newMutableCfa;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Function Reverser
    /////////////////////////////////////////////////////////////////////////////
    private final class CfaFunctionBuilder {

      private Map<String, CVariableDeclaration> variables; // function scope
      private CFunctionEntryNode oldEntryNode;

      private CfaFunctionBuilder(CFunctionEntryNode oldEntryNode) {
        this.variables = new HashMap<>();
        this.oldEntryNode = oldEntryNode;
      }

      private CFunctionEntryNode reverse() {
        String funcName = oldEntryNode.getFunctionName();

        CFunctionDeclaration oldDecl = oldEntryNode.getFunctionDefinition();
        CFunctionDeclaration newDecl = reverseFunctionDeclaration(oldDecl);
        funcDeclMap.put(oldDecl, newDecl);

        FunctionExitNode oldExitNode = oldEntryNode.getExitNode().orElseThrow();

        CFunctionEntryNode newEntry =
            new CFunctionEntryNode(FileLocation.DUMMY, newDecl, null, Optional.empty());
        nodes.put(funcName, newEntry);

        CFANode initStartNode = new CFANode(newDecl); // node before variable initialization
        nodes.put(funcName, initStartNode);

        // initStartNode --> int i --> initDoneNode --> target
        BlankEdge initStartEdge =
            new BlankEdge("", FileLocation.DUMMY, newEntry, initStartNode, "INIT GLOBAL VARS");

        addToCFA(initStartEdge);

        CFANode varInitStartNode = new CFANode(newDecl);
        nodes.put(varInitStartNode.getFunctionName(), varInitStartNode);

        // node after function initialization.
        // real function body begin here
        CFANode initDoneNode = new CFANode(newDecl);
        nodeMap.put(oldExitNode, initDoneNode);
        nodes.put(funcName, initDoneNode);

        int branchCnt = 0; // how many branching in this function

        // =============================================================================
        // BFS the old CFA and create the new CFA
        Set<CFANode> visited = new HashSet<>();
        Deque<CFANode> waitList = new ArrayDeque<>();
        waitList.add(oldExitNode);
        visited.add(oldExitNode);

        Set<CFANode> localTargets = new HashSet<>();

        while (!waitList.isEmpty()) {
          CFANode oldhead = waitList.remove();
          CFANode newhead = nodeMap.get(oldhead);

          if (oldhead instanceof CFunctionEntryNode) {
            ((FunctionExitNode) newhead).setEntryNode(newEntry);
          }

          if (targets.contains(oldhead)) {
            localTargets.add(newhead);
          }

          if (oldhead.isLoopStart()) {
            newhead.setLoopStart();
          }

          checkNotNull(newhead);

          nodes.put(funcName, newhead);

          Boolean usingBranch = false;
          CVariableDeclaration ndetBranchVarDecl = null;
          CIdExpression ndetBranchVarExpr = null;
          CFANode branchNode = null;

          // Create Branching
          // newhead -> branchID1 -> newNext1
          //         -> branchID2 -> newNext2

          if (CFAUtils.allEnteringEdges(oldhead).size() > 1) {
            branchCnt += 1;
            usingBranch = true;
            // Create a ndet variable
            ndetBranchVarDecl =
                new CVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    CStorageClass.AUTO,
                    intType,
                    "branch_" + branchCnt,
                    "branch_" + branchCnt,
                    newDecl.getName() + "::" + "branch_" + branchCnt,
                    null);

            variables.put("branch_", ndetBranchVarDecl);
            ndetBranchVarExpr = new CIdExpression(FileLocation.DUMMY, ndetBranchVarDecl);
            branchNode = createNoDetAssign(ndetBranchVarExpr, newhead);
          }

          int branchid = 0;

          for (CFAEdge oldEdge : CFAUtils.allEnteringEdges(oldhead)) {
            CFANode oldNext = oldEdge.getPredecessor(); // forward edge

            if (oldNext instanceof CFunctionEntryNode
                && oldEntryNode.equals(pCfa.getMainFunction())) {
              // insert the error label
              CFALabelNode errorLabelNode = new CFALabelNode(newDecl, "ERROR");
              nodes.put(funcName, errorLabelNode);

              BlankEdge errorLabelEdge =
                  new BlankEdge("ERROR", FileLocation.DUMMY, newhead, errorLabelNode, "ERROR");
              addToCFA(errorLabelEdge);

              FunctionExitNode newExit = new FunctionExitNode(newDecl);

              BlankEdge exitEdge =
                  new BlankEdge("", FileLocation.DUMMY, errorLabelNode, newExit, "return");

              addToCFA(exitEdge);
              nodeMap.put(oldNext, newExit);
              nodes.put(funcName, newExit);

            } else {
              // branch = 0
              if (usingBranch) {
                pLog.log(Level.INFO, "OLDHEAD TYPE: " + (oldhead instanceof CFATerminationNode));
                CIntegerLiteralExpression ndetBranchIdExpr =
                    new CIntegerLiteralExpression(
                        FileLocation.DUMMY, intType, BigInteger.valueOf(branchid));
                branchid += 1;
                CFANode branchIdHead =
                    createAssumeEdge(ndetBranchVarExpr, ndetBranchIdExpr, branchNode, true);

                branchNode =
                    createAssumeEdge(ndetBranchVarExpr, ndetBranchIdExpr, branchNode, false);

                newhead = branchIdHead;
              }

              CFANode newNext = reverseEdge(oldEdge, newhead);

              checkNotNull(newNext);
              nodes.put(funcName, newNext);
              nodeMap.put(oldNext, newNext);
            }

            if (visited.add(oldNext)) {
              waitList.add(oldNext);
            }
          }
        }

        // =============================================================================
        // Declare function
        CFANode curr = initStartNode;
        CFANode next = null;

        for (CFunctionDeclaration decl : funcDecls.values()) {
          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CDeclarationEdge declEdge =
              new CDeclarationEdge("", FileLocation.DUMMY, curr, next, decl);
          addToCFA(declEdge);
          curr = next;
        }

        next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);
        CDeclarationEdge funcDeclEdge =
            new CDeclarationEdge("", newDecl.getFileLocation(), curr, next, newDecl);
        pLog.log(Level.INFO, "func decl: " + funcDeclEdge);
        addToCFA(funcDeclEdge);
        curr = next;

        BlankEdge functionStartDummyEdge =
            new BlankEdge(
                "", FileLocation.DUMMY, curr, varInitStartNode, "Function start dummy edge");
        addToCFA(functionStartDummyEdge);
        curr = varInitStartNode;
        // =============================================================================
        // Create a ndet variable for target branching
        CVariableDeclaration targetBranchVarDecl =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                false,
                CStorageClass.AUTO,
                intType,
                "target__" + branchCnt,
                "target__" + branchCnt,
                newDecl.getName() + "::" + "target__" + branchCnt,
                null);
        variables.put("target__", targetBranchVarDecl);
        CIdExpression targetBranchVarExpr =
            new CIdExpression(FileLocation.DUMMY, targetBranchVarDecl);
        // =============================================================================
        // Declare variable
        for (CVariableDeclaration decl : variables.values()) {
          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CDeclarationEdge ndeclEdge =
              new CDeclarationEdge("", decl.getFileLocation(), curr, next, decl);
          addToCFA(ndeclEdge);
          curr = next;
        }

        // =============================================================================
        // Create non det branch for each target
        CFANode targetBranchNode = curr;

        int targetCnt = 0;
        CIntegerLiteralExpression targetBranchID =
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, intType, BigInteger.valueOf(targetCnt));
        curr = createAssumeEdge(targetBranchVarExpr, targetBranchID, curr, true);
        curr = createAssumeEdge(targetBranchVarExpr, targetBranchVarExpr, curr, false);
        BlankEdge initDoneEdge =
            new BlankEdge("init done", FileLocation.DUMMY, curr, initDoneNode, "INIT DONE");
        addToCFA(initDoneEdge);

        curr = targetBranchNode;
        if (!localTargets.isEmpty()) {
          for (CFANode localTarget : localTargets) {
            CIntegerLiteralExpression lastBranchID =
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, intType, BigInteger.valueOf(targetCnt));
            curr = createAssumeEdge(targetBranchVarExpr, lastBranchID, curr, false);
            targetCnt += 1;
            CIntegerLiteralExpression currBranchID =
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, intType, BigInteger.valueOf(targetCnt));
            curr = createAssumeEdge(targetBranchVarExpr, currBranchID, curr, true);
            BlankEdge jumpTargetEdge =
                new BlankEdge(
                    "",
                    FileLocation.DUMMY,
                    curr,
                    localTarget,
                    "jump to target [" + targetCnt + "]");
            addToCFA(jumpTargetEdge);
            curr = targetBranchNode;
          }
        }

        return newEntry;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Edge Reverser
      /////////////////////////////////////////////////////////////////////////////
      private CFANode reverseEdge(CFAEdge edge, CFANode from) {

        CFANode to;
        if (nodeMap.containsKey(edge.getPredecessor())) {
          to = nodeMap.get(edge.getPredecessor());
        } else {
          to = new CFANode(from.getFunction());
        }

        pLog.log(Level.INFO, "Reversing: " + edge + " " + edge.getClass());
        if (edge instanceof BlankEdge) {
          reverseBlankEdge((BlankEdge) edge, from, to);
        } else if (edge instanceof CDeclarationEdge) {
          reverseDeclEdge((CDeclarationEdge) edge, from, to);
        } else if (edge instanceof CAssumeEdge) {
          reverseAssumeEdge((CAssumeEdge) edge, from, to);
        } else if (edge instanceof CFunctionCallEdge) {
          pLog.log(Level.INFO, "CFunctionCallEdge: " + edge);
        } else if (edge instanceof CFunctionReturnEdge) {
          pLog.log(Level.INFO, "CFunctionReturnEdge: " + edge);
        } else if (edge instanceof CFunctionSummaryEdge) {
          pLog.log(Level.INFO, "CFunctionSummaryEdge: " + edge);
        } else if (edge instanceof CReturnStatementEdge) {
          pLog.log(Level.INFO, "CReturnStatementEdge: " + edge);
        } else if (edge instanceof CFunctionSummaryStatementEdge) {
          pLog.log(Level.INFO, "CFunctionSummaryStatementEdge: " + edge);
        } else {
          pLog.log(Level.INFO, "CStatementEdge: " + edge);
          reverseStmtEdge((CStatementEdge) edge, from, to);
        }

        return to;
      }

      /////////////////////////////////////////////////////////////////////////////
      // New Node Creator
      /////////////////////////////////////////////////////////////////////////////
      private CFunctionDeclaration reverseFunctionDeclaration(CFunctionDeclaration fdef) {
        return new CFunctionDeclaration(
            fdef.getFileLocation(),
            fdef.getType(),
            fdef.getName(),
            fdef.getOrigName(),
            fdef.getParameters(),
            fdef.getAttributes());
      }

      /////////////////////////////////////////////////////////////////////////////
      // Assume Edge
      /////////////////////////////////////////////////////////////////////////////
      private void reverseAssumeEdge(CAssumeEdge edge, CFANode from, CFANode to) {
        pLog.log(Level.INFO, "assume edge: " + edge);

        CExpression expr = edge.getExpression();
        ExprVariableFinder finder = new ExprVariableFinder(new HashSet<>());
        expr.accept(finder);

        CExpression assumeExpr = expr.accept(finder);

        CAssumeEdge assumeEdge =
            new CAssumeEdge(
                "",
                FileLocation.DUMMY,
                from,
                to,
                assumeExpr,
                edge.getTruthAssumption(),
                edge.isSwapped(),
                edge.isArtificialIntermediate());
        addToCFA(assumeEdge);
      }

      private void reverseBlankEdge(BlankEdge edge, CFANode from, CFANode to) {
        BlankEdge blankEdge =
            new BlankEdge(
                "REV_" + edge.getRawStatement(),
                FileLocation.DUMMY,
                from,
                to,
                "REV_" + edge.getDescription());
        addToCFA(blankEdge);
      }

      private void reverseDeclEdge(CDeclarationEdge edge, CFANode from, CFANode to) {
        CDeclaration decl = edge.getDeclaration();
        if (decl instanceof CTypeDeclaration) {
          pLog.log(Level.INFO, "TYPE DECL not support" + decl);
        } else if (decl instanceof CFunctionDeclaration) {
          reverseFuncDeclEdge(edge, from, to);
        } else {
          reverseVarDeclEdge((CVariableDeclaration) decl, from, to);
        }
      }

      private void reverseFuncDeclEdge(CDeclarationEdge edge, CFANode from, CFANode to) {
        BlankEdge blankEdge =
            new BlankEdge("", FileLocation.DUMMY, from, to, edge.getDescription());
        addToCFA(blankEdge);
      }

      private void reverseVarDeclEdge(CVariableDeclaration decl, CFANode from, CFANode to) {

        String var = decl.getName();

        if (!variables.containsKey(var)) {
          String name = decl.getName();
          CVariableDeclaration newdecl =
              new CVariableDeclaration(
                  decl.getFileLocation(),
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  decl.getType(),
                  name,
                  name,
                  name,
                  null);
          variables.put(name, newdecl);
        }

        // Creating the Assume Edge
        CFANode curr = from;

        CInitializer init = decl.getInitializer();

        if (init instanceof CInitializerExpression) {
          ExprVariableFinder rfinder = new ExprVariableFinder(new HashSet<>());
          CIntegerLiteralExpression rvalue =
              (CIntegerLiteralExpression) ((CInitializerExpression) init).getExpression();

          CExpression rhs = rvalue.accept(rfinder);

          CLeftHandSide lhs = new CIdExpression(FileLocation.DUMMY, variables.get(var));
          curr = createAssumeEdge(lhs, rhs, curr, true);

          BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
          addToCFA(blankEdge);
        }
      }

      private void reverseStmtEdge(CStatementEdge edge, CFANode from, CFANode to) {

        CStatement stmt = edge.getStatement();
        if (stmt instanceof CExpressionAssignmentStatement) {
          handleExprAssignStmt((CExpressionAssignmentStatement) stmt, from, to);
        } else if (stmt instanceof CExpressionStatement) {
          handleExprStmt((CExpressionStatement) stmt, from, to);
        } else if (stmt instanceof CFunctionCallAssignmentStatement) {
          handleCallAssignStmt((CFunctionCallAssignmentStatement) stmt, from);
        } else if (stmt instanceof CFunctionCallStatement) {
          handleCallStmt((CFunctionCallStatement) stmt, from);
        }
      }

      /////////////////////////////////////////////////////////////////////////////
      // Statement Handlers
      /////////////////////////////////////////////////////////////////////////////
      private CFANode handleExprAssignStmt(
          CExpressionAssignmentStatement stmt, CFANode from, CFANode to) {
        checkNotNull(from, to);
        CLeftHandSide lvalue = stmt.getLeftHandSide();
        CExpression rvalue = stmt.getRightHandSide();

        // left hand side
        LeftVariableFinder lfinder = new LeftVariableFinder();
        CIdExpression lhs = (CIdExpression) lvalue.accept(lfinder);
        pLog.log(Level.INFO, "LFINDER VARS: " + lfinder.vars);
        // right hand side
        ExprVariableFinder rfinder = new ExprVariableFinder(lfinder.vars);
        CExpression rhs = rvalue.accept(rfinder);

        CFANode curr = from;

        curr = createAssumeEdge(lhs, rhs, curr, true);

        // reset the left side
        if (rfinder.tmpVarMap.size() == 0) {
          curr = createNoDetAssign(lhs, curr);
        } else { // i <- tmp_i;
          curr = rfinder.createTmpAssignEdge(curr);
          curr = rfinder.resetTmpVar(curr);
        }

        // exit this edge
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(dummyEdge);

        return to;
      }

      private CFANode handleExprStmt(CExpressionStatement stmt, CFANode from, CFANode to) {
        checkNotNull(from, to);
        ExprVariableFinder finder = new ExprVariableFinder(new HashSet<>());
        CExpression expr = stmt.getExpression().accept(finder);

        CExpressionStatement exprStatement = new CExpressionStatement(FileLocation.DUMMY, expr);

        CStatementEdge stmtEdge =
            new CStatementEdge("", exprStatement, FileLocation.DUMMY, from, to);
        addToCFA(stmtEdge);

        return to;
      }

      private CFANode handleCallAssignStmt(CFunctionCallAssignmentStatement stmt, CFANode from) {
        checkNotNull(from);
        CExpression lvalue = stmt.getLeftHandSide();
        CFunctionCallExpression rvalue = stmt.getRightHandSide();
        pLog.log(Level.INFO, "call assign stmt: " + stmt);
        pLog.log(Level.INFO, "lvalue:" + lvalue + " " + lvalue.getClass());
        pLog.log(Level.INFO, "rvalue:" + rvalue + " " + rvalue.getClass());

        return null;
      }

      private CFANode handleCallStmt(CFunctionCallStatement stmt, CFANode from) {
        checkNotNull(stmt, from);
        return null;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Left Side Visitor
      /////////////////////////////////////////////////////////////////////////////
      private final class LeftVariableFinder
          implements CLeftHandSideVisitor<CLeftHandSide, NoException> {
        private Set<String> vars;

        private LeftVariableFinder() {
          this.vars = new HashSet<>();
        }

        private CVariableDeclaration createNewVar(CVariableDeclaration decl) {
          String name = decl.getName();
          assert !variables.containsKey(name);

          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  decl.getFileLocation(),
                  false,
                  decl.getCStorageClass(),
                  decl.getType(),
                  name,
                  name,
                  name,
                  null);
          variables.put(name, newDecl);
          return newDecl;
        }

        private CIdExpression handleVarDecl(CVariableDeclaration decl) {
          String name = decl.getName();
          vars.add(name);

          CVariableDeclaration newDecl;

          if (!variables.containsKey(name)) {
            newDecl = createNewVar(decl);
          } else {
            newDecl = variables.get(name);
          }

          checkNotNull(newDecl);

          return new CIdExpression(FileLocation.DUMMY, newDecl);
        }

        @Override
        public CLeftHandSide visit(CIdExpression pIastIdExpression) throws NoException {
          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            return handleVarDecl((CVariableDeclaration) decl);
          }

          return null;
        }

        @Override
        public CLeftHandSide visit(CPointerExpression pPointerExpression) throws NoException {
          // TODO CPointerExpression
          return null;
        }

        @Override
        public CLeftHandSide visit(CFieldReference pIastFieldReference) throws NoException {
          // TODO CFieldReference
          return null;
        }

        @Override
        public CLeftHandSide visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws NoException {
          // TODO CArraySubscriptExpression
          return null;
        }

        @Override
        public CLeftHandSide visit(CComplexCastExpression pComplexCastExpression)
            throws NoException {
          // TODO CComplexCastExpressio
          return null;
        }
      }

      /////////////////////////////////////////////////////////////////////////////
      // Expression Visitor
      /////////////////////////////////////////////////////////////////////////////
      private final class ExprVariableFinder
          implements CExpressionVisitor<CExpression, NoException> {

        private final Set<String> leftVars;
        private final Map<String, String> tmpVarMap;

        private ExprVariableFinder(Set<String> leftVars) {
          this.leftVars = leftVars;
          this.tmpVarMap = new HashMap<>();
        }

        // Create `i = tmp_i`
        private CFANode createTmpAssignEdge(CFANode from) {
          CFANode curr = from;

          for (Map.Entry<String, String> e : tmpVarMap.entrySet()) {
            String lVar = e.getKey();
            String rVar = e.getValue();
            CIdExpression lExpr = new CIdExpression(FileLocation.DUMMY, variables.get(lVar));
            CIdExpression rExpr = new CIdExpression(FileLocation.DUMMY, variables.get(rVar));
            curr = createAssignEdge(lExpr, rExpr, curr);
          }
          return curr;
        }

        private CFANode resetTmpVar(CFANode from) {
          CFANode curr = from;
          for (String tmpVar : tmpVarMap.values()) {
            CIdExpression tmpExpr = new CIdExpression(FileLocation.DUMMY, variables.get(tmpVar));
            curr = createNoDetAssign(tmpExpr, curr);
          }
          return curr;
        }

        private CVariableDeclaration createTmpVar(CVariableDeclaration decl) {
          String varName = decl.getName();
          String tmpName = "tmp__" + varName;
          CVariableDeclaration tmpDecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  decl.getType(),
                  tmpName,
                  "tmp__" + decl.getOrigName(),
                  "tmp__" + decl.getQualifiedName(),
                  null);
          variables.put(tmpName, tmpDecl);
          tmpVarMap.put(varName, tmpName);
          return tmpDecl;
        }

        private CVariableDeclaration createNewVar(CVariableDeclaration decl) {
          String name = decl.getName();
          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  decl.getFileLocation(),
                  false,
                  decl.getCStorageClass(),
                  decl.getType(),
                  name,
                  name,
                  name,
                  null);
          variables.put(name, newDecl);
          return newDecl;
        }

        private CExpression handleVarDecl(CIdExpression expr, CVariableDeclaration decl) {
          String name = decl.getName();

          CVariableDeclaration newDecl = variables.get(name);
          if (!variables.containsKey(name)) {
            newDecl = createNewVar(decl);
          }

          if (leftVars.contains(name)) {
            newDecl = createTmpVar(decl);
          }

          return new CIdExpression(expr.getFileLocation(), newDecl);
        }

        @Override
        public CExpression visit(CIdExpression pIastIdExpression) throws NoException {

          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();

          if (decl instanceof CVariableDeclaration) {
            return handleVarDecl(pIastIdExpression, (CVariableDeclaration) decl);
          }

          return null;
        }

        @Override
        public CExpression visit(CBinaryExpression pIastBinaryExpression) throws NoException {
          CExpression op1 = pIastBinaryExpression.getOperand1().accept(this);
          CExpression op2 = pIastBinaryExpression.getOperand2().accept(this);
          return new CBinaryExpression(
              FileLocation.DUMMY,
              pIastBinaryExpression.getExpressionType(),
              pIastBinaryExpression.getCalculationType(),
              op1,
              op2,
              pIastBinaryExpression.getOperator());
        }

        @Override
        public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
            throws NoException {
          // TODO CAddressOfLabelExpression
          return null;
        }

        @Override
        public CExpression visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws NoException {
          // TODO CArraySubscriptExpression
          return null;
        }

        @Override
        public CExpression visit(CCastExpression pIastCastExpression) throws NoException {
          // TODO CCastExpression
          return null;
        }

        @Override
        public CExpression visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
            throws NoException {
          return new CIntegerLiteralExpression(
              FileLocation.DUMMY,
              pIastIntegerLiteralExpression.getExpressionType(),
              pIastIntegerLiteralExpression.getValue());
        }

        @Override
        public CExpression visit(CCharLiteralExpression pIastCharLiteralExpression)
            throws NoException {
          return new CCharLiteralExpression(
              FileLocation.DUMMY,
              pIastCharLiteralExpression.getExpressionType(),
              pIastCharLiteralExpression.getCharacter());
        }

        @Override
        public CExpression visit(CFloatLiteralExpression pIastFloatLiteralExpression)
            throws NoException {
          return new CFloatLiteralExpression(
              FileLocation.DUMMY,
              pIastFloatLiteralExpression.getExpressionType(),
              pIastFloatLiteralExpression.getValue());
        }

        @Override
        public CExpression visit(CImaginaryLiteralExpression PIastLiteralExpression)
            throws NoException {
          return new CImaginaryLiteralExpression(
              FileLocation.DUMMY,
              PIastLiteralExpression.getExpressionType(),
              PIastLiteralExpression.getValue());
        }

        @Override
        public CExpression visit(CStringLiteralExpression pIastStringLiteralExpression)
            throws NoException {
          return new CStringLiteralExpression(
              FileLocation.DUMMY, pIastStringLiteralExpression.getContentWithNullTerminator());
        }

        @Override
        public CExpression visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO : CComplexCastExpression
          return null;
        }

        @Override
        public CExpression visit(CFieldReference pIastFieldReference) throws NoException {
          // TODO CFieldReference
          return null;
        }

        @Override
        public CExpression visit(CPointerExpression pPointerExpression) throws NoException {
          // TODO CPointerExpression
          return null;
        }

        @Override
        public CExpression visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
          // TODO CTypeIdExpressio
          return null;
        }

        @Override
        public CExpression visit(CUnaryExpression pIastUnaryExpression) throws NoException {
          // TODO CUnaryExpression
          return null;
        }
      } // CfaFunctionReverser
    }

    /////////////////////////////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////////////////////////////
    /** Add edge to the leaving and entering edges of its predecessor and successor. */
    private void addToCFA(CFAEdge edge) {
      CFACreationUtils.addEdgeUnconditionallyToCFA(edge);
    }

    private CFunctionCallExpression createNoDetCallExpr(CType type) {
      String funcName = "__VERIFIER_nondet_" + type;
      CFunctionDeclaration decl = funcDecls.get(funcName);
      if (decl == null) {
        CFunctionType functype = new CFunctionType(type, ImmutableList.of(), false);
        decl =
            new CFunctionDeclaration(
                FileLocation.DUMMY, functype, funcName, ImmutableList.of(), ImmutableSet.of());
        funcDecls.put(funcName, decl);
      }

      checkNotNull(decl);

      CExpression funcexpr = new CIdExpression(FileLocation.DUMMY, decl);

      return new CFunctionCallExpression(
          FileLocation.DUMMY, type, funcexpr, ImmutableList.of(), decl);
    }

    private CFANode createNoDetAssign(CLeftHandSide lhs, CFANode curr) {
      CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(lhs.getExpressionType());
      CFunctionCallAssignmentStatement ndetAssign =
          new CFunctionCallAssignmentStatement(FileLocation.DUMMY, lhs, ndetCallExpr);
      CFANode next = new CFANode(curr.getFunction());
      nodes.put(next.getFunctionName(), next);
      CStatementEdge assignEdge =
          new CStatementEdge("", ndetAssign, FileLocation.DUMMY, curr, next);

      addToCFA(assignEdge);
      pLog.log(Level.INFO, "NEW NONDET: " + assignEdge);
      return next;
    }

    private CFANode createAssignEdge(CLeftHandSide lExpr, CExpression rExpr, CFANode curr) {

      CFANode next = new CFANode(curr.getFunction());
      nodes.put(next.getFunctionName(), next);

      CExpressionAssignmentStatement assignExpr =
          new CExpressionAssignmentStatement(FileLocation.DUMMY, lExpr, rExpr);
      CStatementEdge assignEdge =
          new CStatementEdge("", assignExpr, FileLocation.DUMMY, curr, next);

      addToCFA(assignEdge);
      return next;
    }

    private CFANode createAssumeEdge(
        CExpression lExpr, CExpression rExpr, CFANode curr, boolean assume) {

      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLog);
      CExpression assumeExpr =
          builder.buildBinaryExpressionUnchecked(lExpr, rExpr, BinaryOperator.EQUALS);

      CFANode next = new CFANode(curr.getFunction());
      nodes.put(next.getFunctionName(), next);

      CAssumeEdge assumeEdge =
          new CAssumeEdge("", FileLocation.DUMMY, curr, next, assumeExpr, assume, false, false);

      addToCFA(assumeEdge);
      curr = next;

      return curr;
    }
  }
}

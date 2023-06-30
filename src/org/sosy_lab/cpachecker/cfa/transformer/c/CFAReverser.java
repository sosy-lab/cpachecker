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
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
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
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFAUtils;
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
    private final HashMap<CFunctionDeclaration, CFunctionDeclaration> funcDeclMap;
    private final HashMap<CFANode, CFANode> nodeMap;
    private final ImmutableSet<CFANode> targets;
    private final Set<CFANode> newTargets;
    private static CType intType =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, false, false, false, false);
    private static CType boolType =
        new CSimpleType(
            false, false, CBasicType.BOOL, false, false, false, false, false, false, false);
    private static CType voidType = CVoidType.VOID;

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
      // Search for the target in the original CFA
      this.targets = targetFinder.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpec);
      this.newTargets = new HashSet<>();
    }

    private CFA createCfa() {

      // create a dummy main
      CFunctionEntryNode dummyReverseMainEntry = newDummyMain();

      // Reverse each Function's CFA
      for (Map.Entry<String, FunctionEntryNode> function : pCfa.getAllFunctions().entrySet()) {
        String name = function.getKey();
        FunctionEntryNode entryNode = function.getValue();
        CfaFunctionBuilder cfaFuncBuilder = new CfaFunctionBuilder(entryNode);
        FunctionEntryNode reverseEntryNode = cfaFuncBuilder.reverse();
        functions.put(name, reverseEntryNode);
      }

      // Connect main entry to each target
      for (CFANode newtarget : newTargets) {
        BlankEdge dummyEdge =
            new BlankEdge(
                "", FileLocation.DUMMY, dummyReverseMainEntry, newtarget, "Target Dummy Edge");
        addToCFA(dummyEdge);
      }

      return new MutableCFA(
          functions, nodes, pCfa.getMetadata().withMainFunctionEntry(dummyReverseMainEntry));
    }

    private CFunctionEntryNode newDummyMain() {
      CFunctionType type = new CFunctionType(CVoidType.VOID, new ArrayList<>(0), false);

      CFunctionDeclaration dummyMain =
          new CFunctionDeclaration(
              FileLocation.DUMMY,
              type,
              "dummy_main",
              "dummy_main",
              new ArrayList<>(0),
              ImmutableSet.of());

      CFunctionEntryNode dummyEntryNode =
          new CFunctionEntryNode(FileLocation.DUMMY, dummyMain, null, Optional.empty());

      functions.put("dummy_main", dummyEntryNode);
      nodes.put("dummy_main", dummyEntryNode);

      return dummyEntryNode;
    }

    /////////////////////////////////////////////////////////////////////////////
    // Function Reverser
    /////////////////////////////////////////////////////////////////////////////
    private final class CfaFunctionBuilder {

      private HashMap<String, CVariableDeclaration> variables; // function scope
      private FunctionEntryNode oldEntryNode;

      private CfaFunctionBuilder(FunctionEntryNode oldEntryNode) {
        this.variables = new HashMap<>();
        this.oldEntryNode = oldEntryNode;
      }

      private FunctionEntryNode reverse() {
        String funcName = oldEntryNode.getFunctionName();

        CFunctionDeclaration oldDecl = (CFunctionDeclaration) oldEntryNode.getFunctionDefinition();
        CFunctionDeclaration newDecl = newDeclaration(oldDecl);

        FunctionExitNode oldExitNode = oldEntryNode.getExitNode().get();

        FunctionEntryNode newEntry =
            new CFunctionEntryNode(FileLocation.DUMMY, newDecl, null, Optional.empty());

        CFANode newDummyNode = new CFANode(newDecl);

        nodeMap.put(oldExitNode, newDummyNode);
        nodes.put(funcName, newEntry);
        nodes.put(funcName, newDummyNode);
        funcDeclMap.put(oldDecl, newDecl);
        BlankEdge dummyEdge =
            new BlankEdge(
                "", FileLocation.DUMMY, newEntry, newDummyNode, "variable initialization start");
        addToCFA(dummyEdge);

        CFANode initNode = new CFANode(newDecl);
        nodeMap.put(oldExitNode, initNode);
        nodes.put(funcName, initNode);

        // BFS the old CFA and create the new CFA
        Set<CFANode> visited = new HashSet<>();
        Deque<CFANode> waitList = new ArrayDeque<>();
        waitList.add(oldExitNode);
        visited.add(oldExitNode);
        pLog.log(Level.INFO, "TRACE:");

        HashSet<CFANode> localTargets = new HashSet<>();

        while (!waitList.isEmpty()) {
          pLog.log(Level.INFO, "//======================================================");
          CFANode oldhead = waitList.remove();
          CFANode newhead = nodeMap.get(oldhead);

          if (oldhead instanceof CFunctionEntryNode) {
            ((FunctionExitNode) newhead).setEntryNode(newEntry);
            break;
          }

          if (targets.contains(oldhead)) {
            localTargets.add(newhead);
          }

          pLog.log(
              Level.INFO,
              "OLD HEAD: "
                  + oldhead.toString()
                  + oldhead.describeFileLocation()
                  + oldhead.getClass());
          pLog.log(
              Level.INFO,
              "NEW HEAD: "
                  + newhead.toString()
                  + newhead.describeFileLocation()
                  + newhead.getClass());
          checkNotNull(newhead);

          nodes.put(funcName, newhead);

          Boolean usingBranch = false;
          CVariableDeclaration ndetDecl = null;

          // Create Branching
          if (CFAUtils.allEnteringEdges(oldhead).size() > 1) {
            usingBranch = true;
            // Create a ndet variable
            ndetDecl =
                new CVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    CStorageClass.AUTO,
                    intType,
                    "TMP_BRANCHING_REVERSE_CFA",
                    "TMP_BRANCHING_REVERSE_CFA",
                    "TMP_BRANCHING_REVERSE_CFA",
                    null);

            CFANode branchNode = new CFANode(newhead.getFunction());
            nodes.put(funcName, branchNode);

            CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(intType);

            CIdExpression varid = new CIdExpression(FileLocation.DUMMY, ndetDecl);
            CFunctionCallAssignmentStatement ndetAssign =
                new CFunctionCallAssignmentStatement(FileLocation.DUMMY, varid, ndetCallExpr);

            CStatementEdge branchEdge =
                new CStatementEdge(
                    "BRANCHING", ndetAssign, FileLocation.DUMMY, newhead, branchNode);
            addToCFA(branchEdge);
            pLog.log(Level.INFO, "BRANCHING: " + branchEdge.toString());
            newhead = branchNode;
          }

          int branchid = 0;

          for (CFAEdge oldEdge : CFAUtils.allEnteringEdges(oldhead)) {
            CFANode oldNext = oldEdge.getPredecessor(); // forward edge

            if (oldNext instanceof CFunctionEntryNode
                && oldEntryNode.equals(pCfa.getMainFunction())) {
              // insert the error label

              CFALabelNode label = new CFALabelNode(newDecl, "ERROR");
              nodes.put(funcName, label);
              BlankEdge newedge1 =
                  new BlankEdge("ERROR", FileLocation.DUMMY, newhead, label, "ERROR");
              addToCFA(newedge1);
              CFANode newNext = newNode(oldNext);
              BlankEdge newedge2 =
                  new BlankEdge("", FileLocation.DUMMY, label, newNext, "AFTER ERROR");
              addToCFA(newedge2);
              nodeMap.put(oldNext, newNext);
              nodes.put(funcName, newNext);
            } else {
              if (usingBranch) {
                CFANode assumeNode = new CFANode(newhead.getFunction());
                nodes.put(funcName, assumeNode);
                CExpression lvalue = new CIdExpression(FileLocation.DUMMY, ndetDecl);
                CIntegerLiteralExpression rvalue =
                    new CIntegerLiteralExpression(
                        FileLocation.DUMMY, intType, BigInteger.valueOf(branchid++));

                CBinaryExpression assumeExpr =
                    new CBinaryExpression(
                        FileLocation.DUMMY,
                        boolType,
                        boolType,
                        lvalue,
                        rvalue,
                        BinaryOperator.EQUALS);
                CAssumeEdge assumeEdge =
                    new CAssumeEdge(
                        "",
                        FileLocation.DUMMY,
                        newhead,
                        assumeNode,
                        assumeExpr,
                        true,
                        false,
                        false);
                pLog.log(Level.INFO, "ASSUMMING: " + assumeEdge.toString());
                addToCFA(assumeEdge);
                nodeMap.put(oldhead, assumeNode);
              }

              CFANode newNext = reverseEdge((CCfaEdge) oldEdge);

              nodes.put(funcName, newNext);
              nodeMap.put(oldNext, newNext);
              pLog.log(Level.INFO, "oldNext: " + oldNext.toString() + oldNext.getClass());
              pLog.log(Level.INFO, "newNext: " + newNext.toString() + newNext.getClass());
            }

            if (visited.add(oldNext)) {
              waitList.add(oldNext);
            }
          }
        }

        // Initialize all variables after entering the function.
        CFANode curr = newDummyNode;
        CFANode next = null;

        for (String newvar : variables.keySet()) {
          CVariableDeclaration decl = variables.get(newvar);
          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CDeclarationEdge ndeclEdge =
              new CDeclarationEdge("", FileLocation.DUMMY, curr, next, decl);
          addToCFA(ndeclEdge);
          curr = next;

          CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(decl.getType());
          CLeftHandSide varid = new CIdExpression(FileLocation.DUMMY, decl);
          CFunctionCallAssignmentStatement ndetAssign =
              new CFunctionCallAssignmentStatement(FileLocation.DUMMY, varid, ndetCallExpr);

          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CStatementEdge ndetEdge =
              new CStatementEdge("", ndetAssign, FileLocation.DUMMY, curr, next);

          addToCFA(ndetEdge);
          curr = next;
        }

        BlankEdge initEdge =
            new BlankEdge("", FileLocation.DUMMY, curr, initNode, "variable initialization done");
        addToCFA(initEdge);

        // Initialize all variables after entering the target.
        for (CFANode localTarget : localTargets) {
          CFANode dummyTarget = new CFANode(newDecl);
          nodes.put(funcName, dummyTarget);
          curr = dummyTarget;
          next = null;
          for (String newvar : variables.keySet()) {
            CVariableDeclaration decl = variables.get(newvar);
            next = new CFANode(curr.getFunction());
            nodes.put(next.getFunctionName(), next);
            CDeclarationEdge ndeclEdge =
                new CDeclarationEdge("", FileLocation.DUMMY, curr, next, decl);
            addToCFA(ndeclEdge);
            curr = next;

            CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(decl.getType());
            CLeftHandSide varid = new CIdExpression(FileLocation.DUMMY, decl);
            CFunctionCallAssignmentStatement ndetAssign =
                new CFunctionCallAssignmentStatement(FileLocation.DUMMY, varid, ndetCallExpr);

            next = new CFANode(curr.getFunction());
            nodes.put(next.getFunctionName(), next);
            CStatementEdge ndetEdge =
                new CStatementEdge("", ndetAssign, FileLocation.DUMMY, curr, next);

            addToCFA(ndetEdge);
            curr = next;
          }

          BlankEdge targetEdge =
              new BlankEdge(
                  "", FileLocation.DUMMY, curr, localTarget, "variable initialization done");

          addToCFA(targetEdge);

          newTargets.add(dummyTarget);
        }

        return newEntry;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Edge Reverser
      /////////////////////////////////////////////////////////////////////////////
      private CFANode reverseEdge(CCfaEdge edge) {

        pLog.log(Level.INFO, "Reversing: " + edge.toString() + " " + edge.getClass());
        if (edge instanceof BlankEdge) {
          return reverseBlankEdge((BlankEdge) edge);
        } else if (edge instanceof CDeclarationEdge) {
          return reverseDeclEdge((CDeclarationEdge) edge);
        } else if (edge instanceof CAssumeEdge) {
          return reverseAssumeEdge((CAssumeEdge) edge);
        } else if (edge instanceof CFunctionCallEdge) {
          pLog.log(Level.INFO, "CFunctionCallEdge: " + edge.toString());
        } else if (edge instanceof CFunctionReturnEdge) {
          pLog.log(Level.INFO, "CFunctionReturnEdge: " + edge.toString());
        } else if (edge instanceof CFunctionSummaryEdge) {
          pLog.log(Level.INFO, "CFunctionSummaryEdge: " + edge.toString());
        } else if (edge instanceof CReturnStatementEdge) {
          pLog.log(Level.INFO, "CReturnStatementEdge: " + edge.toString());
        } else if (edge instanceof CFunctionSummaryStatementEdge) {
          pLog.log(Level.INFO, "CFunctionSummaryStatementEdge: " + edge.toString());
        } else {
          pLog.log(Level.INFO, "CStatementEdge: " + edge.toString());
          return reverseStmtEdge((CStatementEdge) edge);
        }

        return null;
      }

      /////////////////////////////////////////////////////////////////////////////
      // New Node Creator
      /////////////////////////////////////////////////////////////////////////////
      private CFunctionDeclaration newDeclaration(CFunctionDeclaration fdef) {
        return new CFunctionDeclaration(
            FileLocation.DUMMY,
            fdef.getType(),
            fdef.getName(),
            fdef.getOrigName(),
            fdef.getParameters(),
            fdef.getAttributes());
      }

      private CFANode newNode(CFANode node) {
        if (node instanceof CFALabelNode) {
          String labelName = ((CFALabelNode) node).getLabel() + "_";
          AFunctionDeclaration funcDecl = funcDeclMap.get(((CFALabelNode) node).getFunction());
          return new CFALabelNode(funcDecl, labelName);
        } else if (node instanceof FunctionEntryNode) {
          AFunctionDeclaration funcDecl = funcDeclMap.get(node.getFunction());
          return new FunctionExitNode(funcDecl);
        } else if (node instanceof FunctionExitNode) {
          CFANode exitNode = nodeMap.get(((FunctionExitNode) node).getEntryNode());
          assert (exitNode instanceof FunctionExitNode);
          CFunctionDeclaration fdef = (CFunctionDeclaration) node.getFunction();
          return new CFunctionEntryNode(
              FileLocation.DUMMY, fdef, (FunctionExitNode) exitNode, Optional.empty());
        } else {
          AFunctionDeclaration funcDecl = funcDeclMap.get(node.getFunction());
          return new CFANode(funcDecl);
        }
      }

      private CFANode reverseAssumeEdge(CAssumeEdge edge) {
        pLog.log(Level.INFO, "HANDLING: " + edge.toString());
        CFANode from = nodeMap.get(edge.getSuccessor());
        CFANode to;

        CExpression expr = edge.getExpression();
        ExprVariableFinder finder = new ExprVariableFinder();
        expr.accept(finder);
        // CFANode curr = finder.initNewVars(from);

        if (nodeMap.containsKey(edge.getPredecessor())) {
          to = nodeMap.get(edge.getPredecessor());
        } else {
          to = new CFANode(from.getFunction());
        }

        CFANode curr = from;
        CFANode next = new CFANode(curr.getFunction());
        nodes.put(curr.getFunctionName(), next);

        CExpression assumeExpr = null;

        CLeftHandSide newOp1 = null;
        if (expr instanceof CBinaryExpression) {

          CExpression op1 = ((CBinaryExpression) expr).getOperand1();
          CExpression op2 = ((CBinaryExpression) expr).getOperand2();
          BinaryOperator operator = ((CBinaryExpression) expr).getOperator();
          CExpression newOp2;
          if (op1 instanceof CIdExpression) {
            String varid = ((CIdExpression) op1).getName();
            newOp1 = new CIdExpression(FileLocation.DUMMY, variables.get(varid));
            pLog.log(
                Level.INFO,
                "OP1: "
                    + newOp1.toASTString()
                    + "!"
                    + variables.get(varid)
                    + "VAR: "
                    + varid.toString());
            newOp2 = op2;
            assumeExpr =
                new CBinaryExpression(
                    FileLocation.DUMMY, boolType, boolType, newOp1, newOp2, operator);

            CAssumeEdge assumeEdge =
                new CAssumeEdge(
                    edge.getRawStatement(),
                    edge.getFileLocation(),
                    curr,
                    next,
                    assumeExpr,
                    edge.getTruthAssumption(),
                    edge.isSwapped(),
                    edge.isArtificialIntermediate());

            addToCFA(assumeEdge);
            curr = next;

            pLog.log(Level.INFO, "NEWEDGE: " + assumeEdge.toString());

            if ((operator.equals(BinaryOperator.EQUALS) && edge.getTruthAssumption())
                || (operator.equals(BinaryOperator.NOT_EQUALS) && !edge.getTruthAssumption())) {
              next = new CFANode(curr.getFunction());
              nodes.put(next.getFunctionName(), next);
              CExpressionAssignmentStatement assignStmt =
                  new CExpressionAssignmentStatement(FileLocation.DUMMY, newOp1, newOp2);
              CStatementEdge assignmentEdge =
                  new CStatementEdge("", assignStmt, FileLocation.DUMMY, curr, next);
              addToCFA(assignmentEdge);
              pLog.log(Level.INFO, "NEWEDGE: " + assignmentEdge.toString());
            }
          }
        }

        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, next, to, "AFTER ASSUME");
        addToCFA(blankEdge);

        return to;
      }

      private CFANode reverseBlankEdge(BlankEdge edge) {
        CFANode from = nodeMap.get(edge.getSuccessor());
        CFANode to;

        if (nodeMap.containsKey(edge.getPredecessor())) {
          to = nodeMap.get(edge.getPredecessor());
        } else {
          to = new CFANode(from.getFunction());
        }

        BlankEdge newedge =
            new BlankEdge(
                edge.getRawStatement() + "_",
                edge.getFileLocation(),
                from,
                to,
                edge.getDescription() + "_");
        addToCFA(newedge);

        pLog.log(Level.INFO, "NEWEDGE: " + newedge.toString());
        return to;
      }

      private CFANode reverseDeclEdge(CDeclarationEdge edge) {
        CDeclaration decl = edge.getDeclaration();

        if (decl instanceof CTypeDeclaration) {
          return null;
        } else if (decl instanceof CFunctionDeclaration) {
          return reverseFuncDeclEdge(edge, (CFunctionDeclaration) decl);
        } else {
          return reverseVarDeclEdge(edge, (CVariableDeclaration) decl);
        }
      }

      private CFANode reverseFuncDeclEdge(CDeclarationEdge edge, CFunctionDeclaration decl) {
        CFANode from = nodeMap.get(edge.getSuccessor());
        CFANode to = newNode(edge.getPredecessor());
        CDeclarationEdge newedge =
            new CDeclarationEdge(edge.getRawStatement(), edge.getFileLocation(), from, to, decl);
        addToCFA(newedge);
        return to;
      }

      private CFANode reverseVarDeclEdge(CDeclarationEdge edge, CVariableDeclaration decl) {
        CFANode from = nodeMap.get(edge.getSuccessor());
        pLog.log(
            Level.INFO,
            "HANDLING: "
                + decl.toASTString()
                + " "
                + decl.getInitializer().toASTString()
                + decl.getName());

        String var = decl.getName();

        if (!variables.containsKey(var)) {
          String name = decl.getName();
          CType ty = decl.getType();
          CVariableDeclaration newdecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  ty,
                  decl.getName(),
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(name, newdecl);
        }

        // Creating the Assume Edge
        CFANode curr = from;

        CInitializer init = decl.getInitializer();

        if (init instanceof CInitializerExpression) {
          ExprVariableFinder rfinder = new ExprVariableFinder();
          // TODO
          CIntegerLiteralExpression rvalue =
              (CIntegerLiteralExpression) ((CInitializerExpression) init).getExpression();
          rvalue.accept(rfinder);

          CIntegerLiteralExpression op2 =
              new CIntegerLiteralExpression(FileLocation.DUMMY, intType, rvalue.getValue());

          // curr = rfinder.initNewVars(from);

          CFANode next;
          if (nodeMap.containsKey(edge.getPredecessor())) {
            next = nodeMap.get(edge.getPredecessor());
          } else {
            next = new CFANode(curr.getFunction());
          }

          CLeftHandSide lvalue = new CIdExpression(FileLocation.DUMMY, variables.get(var));
          pLog.log(
              Level.INFO,
              "LVALUE: "
                  + lvalue.toASTString()
                  + "!"
                  + variables.get(var)
                  + "VAR: "
                  + var.toString());

          CBinaryExpression assumeExpr =
              new CBinaryExpression(
                  FileLocation.DUMMY, boolType, boolType, lvalue, op2, BinaryOperator.NOT_EQUALS);
          CAssumeEdge trueEdge =
              new CAssumeEdge("", FileLocation.DUMMY, curr, next, assumeExpr, false, false, true);

          addToCFA(trueEdge);

          pLog.log(Level.INFO, "NEWEDGE: " + trueEdge.toString());
          createAbortCall(curr, trueEdge); // false then abort

          return next;
        }

        return null;
      }

      private CFANode reverseStmtEdge(CStatementEdge edge) {
        CFANode from = nodeMap.get(edge.getSuccessor());
        CFANode to = nodeMap.get(edge.getPredecessor());
        CStatement stmt = edge.getStatement();
        if (stmt instanceof CExpressionAssignmentStatement) {
          return handleExprAssignStmt((CExpressionAssignmentStatement) stmt, from, to);
        } else if (stmt instanceof CExpressionStatement) {
          return handleExprStmt((CExpressionStatement) stmt, from);
        } else if (stmt instanceof CFunctionCallAssignmentStatement) {
          return handleCallAssignStmt((CFunctionCallAssignmentStatement) stmt, from);
        } else if (stmt instanceof CFunctionCallStatement) {
          return handleCallStmt((CFunctionCallStatement) stmt, from);
        }
        return null;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Statement Handlers
      /////////////////////////////////////////////////////////////////////////////
      private CFANode handleExprAssignStmt(
          CExpressionAssignmentStatement stmt, CFANode from, CFANode to) {

        CLeftHandSide lvalue = stmt.getLeftHandSide();
        CExpression rvalue = stmt.getRightHandSide();
        pLog.log(Level.INFO, "lvalue:" + lvalue.toASTString());
        pLog.log(Level.INFO, "rvalue:" + rvalue.toASTString() + rvalue.getExpressionType());

        ExprVariableFinder rfinder = new ExprVariableFinder();

        CFANode curr = from;

        // right hand side
        rvalue.accept(rfinder);
        // curr = rfinder.initNewVars(curr);

        if (to == null) {
          to = new CFANode(curr.getFunction());
        }

        LeftVariableFinder lfinder = new LeftVariableFinder(rfinder.newVars);

        // left hand side
        lvalue.accept(lfinder);
        // curr = lfinder.initNewVar(curr);

        CBinaryExpression assumeExpr =
            new CBinaryExpression(
                FileLocation.DUMMY, boolType, boolType, lvalue, rvalue, BinaryOperator.EQUALS);

        CFANode next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);

        CAssumeEdge trueEdge =
            new CAssumeEdge("", FileLocation.DUMMY, curr, next, assumeExpr, true, false, false);

        addToCFA(trueEdge);
        pLog.log(Level.INFO, "TRUEEDGE: " + trueEdge.toString());

        createAbortCall(curr, trueEdge); // false then abort

        curr = next;

        // reset the left side
        CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(lvalue.getExpressionType());
        CFunctionCallAssignmentStatement ndetAssign =
            new CFunctionCallAssignmentStatement(FileLocation.DUMMY, lvalue, ndetCallExpr);

        CStatementEdge resetEdge = new CStatementEdge("", ndetAssign, FileLocation.DUMMY, curr, to);

        addToCFA(resetEdge);

        return to;
      }

      private CFANode handleExprStmt(CExpressionStatement stmt, CFANode from) {
        return null;
      }

      private CFANode handleCallAssignStmt(CFunctionCallAssignmentStatement stmt, CFANode from) {
        CExpression lvalue = stmt.getLeftHandSide();
        CFunctionCallExpression rvalue = stmt.getRightHandSide();
        pLog.log(Level.INFO, "lvalue:" + lvalue.toASTString());
        pLog.log(Level.INFO, "rvalue:" + rvalue.toASTString());
        pLog.log(Level.INFO, "rvalue:" + rvalue.getClass());

        return null;
      }

      private CFANode handleCallStmt(CFunctionCallStatement stmt, CFANode from) {
        return null;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Left Side Visitor
      /////////////////////////////////////////////////////////////////////////////
      private final class LeftVariableFinder implements CLeftHandSideVisitor<Void, NoException> {
        private HashSet<String> rightVars;
        private HashSet<String> newVars;

        private LeftVariableFinder(HashSet<String> rightVars) {
          this.rightVars = rightVars;
          this.newVars = new HashSet<>();
        }

        private CFANode _initNewVar(CFANode from) {

          CFANode curr = from;
          CFANode next = null;
          for (String newvar : newVars) {
            CVariableDeclaration decl = variables.get(newvar);
            next = new CFANode(curr.getFunction());
            nodes.put(next.getFunctionName(), next);
            CDeclarationEdge ndeclEdge =
                new CDeclarationEdge("", FileLocation.DUMMY, curr, next, decl);
            addToCFA(ndeclEdge);
            curr = next;

            CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(decl.getType());
            CLeftHandSide varid = new CIdExpression(FileLocation.DUMMY, decl);
            CFunctionCallAssignmentStatement ndetAssign =
                new CFunctionCallAssignmentStatement(FileLocation.DUMMY, varid, ndetCallExpr);

            next = new CFANode(curr.getFunction());
            nodes.put(next.getFunctionName(), next);
            CStatementEdge ndetEdge =
                new CStatementEdge("", ndetAssign, FileLocation.DUMMY, curr, next);

            addToCFA(ndetEdge);
            curr = next;
          }

          return curr;
        }

        private void createNewVar(CVariableDeclaration decl) {
          String name = decl.getName();
          CType ty = decl.getType();
          newVars.add(name);
          CVariableDeclaration newdecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  ty,
                  name,
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(name, newdecl);
        }

        private void createTmpVar(CVariableDeclaration decl) {
          String name = decl.getName();
          String tmp_name = "TMP__" + name;
          CType ty = decl.getType();
          newVars.add(tmp_name);
          CVariableDeclaration tmp_decl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  ty,
                  tmp_name,
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(tmp_name, tmp_decl);
        }

        private void handleVarDecl(CVariableDeclaration decl) {
          String name = decl.getName();
          if (!variables.containsKey(name)) {
            createNewVar(decl);
            return;
          }

          // like { i = i - 1; }
          if (rightVars.contains(name)) {
            createTmpVar(decl);
            pLog.log(Level.WARNING, "Assignment like {i -= 1} not work yet!");
          }

          return;
        }

        @Override
        public Void visit(CIdExpression pIastIdExpression) throws NoException {
          pLog.log(Level.INFO, "HANDLING: " + pIastIdExpression.toASTString());
          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            handleVarDecl((CVariableDeclaration) decl);
          }
          return null;
        }

        @Override
        public Void visit(CPointerExpression pPointerExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CFieldReference pIastFieldReference) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }
      }

      /////////////////////////////////////////////////////////////////////////////
      // Expression Visitor
      /////////////////////////////////////////////////////////////////////////////
      private final class ExprVariableFinder implements CExpressionVisitor<Void, NoException> {

        private HashSet<String> newVars;

        private ExprVariableFinder() {
          this.newVars = new HashSet<>();
        }

        private CFANode _initNewVars(CFANode from) {
          CFANode curr = from;
          CFANode next = null;
          for (String newvar : newVars) {
            CVariableDeclaration decl = variables.get(newvar);

            next = new CFANode(curr.getFunction());
            nodes.put(next.getFunctionName(), next);
            CDeclarationEdge ndeclEdge =
                new CDeclarationEdge("", FileLocation.DUMMY, curr, next, decl);
            addToCFA(ndeclEdge);
            curr = next;

            CFunctionCallExpression ndetCallExpr = createNoDetCallExpr(decl.getType());
            CLeftHandSide varid = new CIdExpression(FileLocation.DUMMY, decl);
            CFunctionCallAssignmentStatement ndetAssign =
                new CFunctionCallAssignmentStatement(FileLocation.DUMMY, varid, ndetCallExpr);

            next = new CFANode(curr.getFunction());
            nodes.put(next.getFunctionName(), next);
            CStatementEdge ndetEdge =
                new CStatementEdge("", ndetAssign, FileLocation.DUMMY, curr, next);
            addToCFA(ndetEdge);
            curr = next;
          }
          return curr;
        }

        private void createNewVar(CVariableDeclaration decl) {
          String name = decl.getName();
          CType ty = decl.getType();
          newVars.add(name);
          CVariableDeclaration newdecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  ty,
                  name,
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(name, newdecl);
        }

        private void handleVarDecl(CVariableDeclaration decl) {
          String name = decl.getName();
          if (!variables.containsKey(name)) {
            createNewVar(decl);
          }
          return;
        }

        @Override
        public Void visit(CIdExpression pIastIdExpression) throws NoException {
          pLog.log(Level.INFO, "HANDLING: " + pIastIdExpression.toASTString());
          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();
          if (decl instanceof CVariableDeclaration) {
            handleVarDecl((CVariableDeclaration) decl);
          }

          return null;
        }

        @Override
        public Void visit(CBinaryExpression pIastBinaryExpression) throws NoException {
          pLog.log(Level.INFO, "HANDLING: " + pIastBinaryExpression.toASTString());
          CExpression op1 = pIastBinaryExpression.getOperand1();
          CExpression op2 = pIastBinaryExpression.getOperand2();
          op1.accept(this);
          op2.accept(this);
          return null;
        }

        @Override
        public Void visit(CAddressOfLabelExpression pAddressOfLabelExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CCastExpression pIastCastExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CIntegerLiteralExpression pIastIntegerLiteralExpression)
            throws NoException {
          pLog.log(Level.INFO, "HANDLING: " + pIastIntegerLiteralExpression.toASTString());
          return null;
        }

        @Override
        public Void visit(CCharLiteralExpression pIastCharLiteralExpression) throws NoException {
          pLog.log(Level.INFO, "HANDLING: " + pIastCharLiteralExpression.toASTString());
          return null;
        }

        @Override
        public Void visit(CFloatLiteralExpression pIastFloatLiteralExpression) throws NoException {
          pLog.log(Level.INFO, "HANDLING: " + pIastFloatLiteralExpression.toASTString());
          return null;
        }

        @Override
        public Void visit(CImaginaryLiteralExpression PIastLiteralExpression) throws NoException {
          return null;
        }

        @Override
        public Void visit(CStringLiteralExpression pIastStringLiteralExpression)
            throws NoException {
          return null;
        }

        @Override
        public Void visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CFieldReference pIastFieldReference) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CPointerExpression pPointerExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public Void visit(CUnaryExpression pIastUnaryExpression) throws NoException {
          // TODO Auto-generated method stub
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
      String funcName = "__VERIFIER_nondet_" + type.toString();
      CFunctionType functype = new CFunctionType(type, ImmutableList.of(), false);

      CFunctionDeclaration decl =
          new CFunctionDeclaration(
              FileLocation.DUMMY, functype, funcName, ImmutableList.of(), ImmutableSet.of());
      CExpression funcexpr = new CIdExpression(FileLocation.DUMMY, decl);

      return new CFunctionCallExpression(
          FileLocation.DUMMY, type, funcexpr, ImmutableList.of(), decl);
    }

    private void createAbortCall(CFANode curr, CAssumeEdge trueEdge) {
      CFANode falseNode = new CFANode(curr.getFunction());
      nodes.put(curr.getFunctionName(), falseNode);

      CAssumeEdge falseEdge =
          new CAssumeEdge(
              "",
              FileLocation.DUMMY,
              curr,
              falseNode,
              trueEdge.getExpression(),
              !trueEdge.getTruthAssumption(),
              trueEdge.isSwapped(),
              trueEdge.isArtificialIntermediate());

      addToCFA(falseEdge);
      pLog.log(Level.INFO, "FALEEDGE: " + falseEdge.toString());

      CFunctionType functype = new CFunctionType(voidType, ImmutableList.of(), false);
      CFunctionDeclaration decl =
          new CFunctionDeclaration(
              FileLocation.DUMMY, functype, "abort", ImmutableList.of(), ImmutableSet.of());
      CExpression funcexpr = new CIdExpression(FileLocation.DUMMY, decl);
      CFunctionCallExpression callExpr =
          new CFunctionCallExpression(
              FileLocation.DUMMY, voidType, funcexpr, ImmutableList.of(), decl);

      CFunctionCallStatement callStmt = new CFunctionCallStatement(FileLocation.DUMMY, callExpr);

      CFANode callNode = new CFANode(curr.getFunction());
      nodes.put(curr.getFunctionName(), callNode);

      CStatementEdge callEdge =
          new CStatementEdge("abort", callStmt, FileLocation.DUMMY, falseNode, callNode);

      addToCFA(callEdge);
    }
  }
}

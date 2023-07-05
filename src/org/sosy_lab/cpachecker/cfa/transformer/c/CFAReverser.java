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
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
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
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
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
    private final Map<CFunctionDeclaration, CFunctionDeclaration> funcDeclMap;
    private final Map<CFANode, CFANode> nodeMap;
    private final ImmutableSet<CFANode> targets;
    private final Map<String, CFunctionDeclaration> funcDecls;
    private static CType intType =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, false, false, false, false);
    private static CType boolType =
        new CSimpleType(
            false, false, CBasicType.BOOL, false, false, false, false, false, false, false);

    // private static CType voidType = CVoidType.VOID;

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
        CFunctionDeclaration newDecl = newDeclaration(oldDecl);

        FunctionExitNode oldExitNode = oldEntryNode.getExitNode().orElseThrow();

        CFunctionEntryNode newEntry =
            new CFunctionEntryNode(FileLocation.DUMMY, newDecl, null, Optional.empty());

        CFANode initStartNode = new CFANode(newDecl); // node before variable initialization

        nodes.put(funcName, newEntry);
        nodes.put(funcName, initStartNode);

        funcDeclMap.put(oldDecl, newDecl);

        BlankEdge initStartEdge =
            new BlankEdge(
                "", FileLocation.DUMMY, newEntry, initStartNode, "variable initialization start");

        addToCFA(initStartEdge);

        // node after function initialization.
        // real function body begin here
        CFANode initDoneNode = new CFANode(newDecl);
        nodeMap.put(oldExitNode, initDoneNode);
        nodes.put(funcName, initDoneNode);

        // BFS the old CFA and create the new CFA
        Set<CFANode> visited = new HashSet<>();
        Deque<CFANode> waitList = new ArrayDeque<>();
        waitList.add(oldExitNode);
        visited.add(oldExitNode);
        pLog.log(Level.INFO, "TRACE:");

        Set<CFANode> localTargets = new HashSet<>();

        while (!waitList.isEmpty()) {
          CFANode oldhead = waitList.remove();
          CFANode newhead = nodeMap.get(oldhead);

          if (oldhead instanceof CFunctionEntryNode) {
            ((FunctionExitNode) newhead).setEntryNode(newEntry);
            break;
          }

          if (targets.contains(oldhead)) {
            localTargets.add(newhead);
          }

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
                        intType,
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
                pLog.log(Level.INFO, "ASSUMMING: " + assumeEdge);
                addToCFA(assumeEdge);
                nodeMap.put(oldhead, assumeNode);
              }

              CFANode newNext = reverseEdge((CCfaEdge) oldEdge);

              nodes.put(funcName, newNext);
              nodeMap.put(oldNext, newNext);
            }

            if (visited.add(oldNext)) {
              waitList.add(oldNext);
            }
          }
        }

        // Initialize all variables and function after entering the function.
        CFANode curr = initStartNode;
        CFANode next = null;

        for (Map.Entry<String, CFunctionDeclaration> e : funcDecls.entrySet()) {
          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CDeclarationEdge declEdge =
              new CDeclarationEdge("", FileLocation.DUMMY, curr, next, e.getValue());
          addToCFA(declEdge);
          curr = next;
        }

        next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);
        CDeclarationEdge funcDeclEdge =
            new CDeclarationEdge("", FileLocation.DUMMY, curr, next, newDecl);
        addToCFA(funcDeclEdge);
        curr = next;

        for (CVariableDeclaration decl : variables.values()) {
          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CDeclarationEdge ndeclEdge =
              new CDeclarationEdge("", FileLocation.DUMMY, curr, next, decl);
          addToCFA(ndeclEdge);
          curr = next;
        }

        // TODO: create ndet branching for target
        if (localTargets.size() != 0) {
          for (CFANode localTarget : localTargets) {
            BlankEdge jumpTargetEdge =
                new BlankEdge("", FileLocation.DUMMY, curr, localTarget, "jump to target");
            addToCFA(jumpTargetEdge);
          }
        }

        return newEntry;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Edge Reverser
      /////////////////////////////////////////////////////////////////////////////
      private CFANode reverseEdge(CCfaEdge edge) {

        pLog.log(Level.INFO, "Reversing: " + edge + " " + edge.getClass());
        if (edge instanceof BlankEdge) {
          return reverseBlankEdge((BlankEdge) edge);
        } else if (edge instanceof CDeclarationEdge) {
          return reverseDeclEdge((CDeclarationEdge) edge);
        } else if (edge instanceof CAssumeEdge) {
          return reverseAssumeEdge((CAssumeEdge) edge);
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

      /////////////////////////////////////////////////////////////////////////////
      // Assume Edge
      /////////////////////////////////////////////////////////////////////////////
      private CFANode reverseAssumeEdge(CAssumeEdge edge) {
        pLog.log(Level.INFO, "HANDLING: " + edge);
        CFANode from = nodeMap.get(edge.getSuccessor());
        CFANode to;

        CExpression expr = edge.getExpression();

        ExprVariableFinder finder = new ExprVariableFinder(new HashSet<>());
        expr.accept(finder);

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
            newOp2 = op2;
            assumeExpr =
                new CBinaryExpression(
                    FileLocation.DUMMY, boolType, intType, newOp1, newOp2, operator);

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
          }
        }

        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "AFTER ASSUME");
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

        pLog.log(Level.INFO, "NEWEDGE: " + newedge);
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

        String var = decl.getName();

        if (!variables.containsKey(var)) {
          String name = decl.getName();
          CVariableDeclaration newdecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
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

        CFANode to;
        if (nodeMap.containsKey(edge.getPredecessor())) {
          to = nodeMap.get(edge.getPredecessor());
        } else {
          to = new CFANode(from.getFunction());
        }

        CInitializer init = decl.getInitializer();

        if (init instanceof CInitializerExpression) {
          ExprVariableFinder rfinder = new ExprVariableFinder(new HashSet<>());
          CIntegerLiteralExpression rvalue =
              (CIntegerLiteralExpression) ((CInitializerExpression) init).getExpression();

          CExpression rhs = rvalue.accept(rfinder);

          CLeftHandSide lhs = new CIdExpression(FileLocation.DUMMY, variables.get(var));

          curr = createAssumeEqualEdge(lhs, rhs, curr);

          BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
          addToCFA(blankEdge);

          return to;
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

        CIdExpression lvalue = (CIdExpression) stmt.getLeftHandSide();
        CExpression rvalue = stmt.getRightHandSide();

        // left hand side
        LeftVariableFinder lfinder = new LeftVariableFinder();
        CIdExpression lhs = (CIdExpression) lvalue.accept(lfinder);
        pLog.log(Level.INFO, "LFINDER VARS: " + lfinder.vars);
        // right hand side
        ExprVariableFinder rfinder = new ExprVariableFinder(lfinder.vars);
        CExpression rhs = rvalue.accept(rfinder);

        CFANode curr = from;
        if (to == null) {
          to = new CFANode(curr.getFunction());
        }

        curr = createAssumeEqualEdge(lhs, rhs, curr);

        // reset the left side
        if (rfinder.tmpVarMap.size() == 0) {
          curr = createNoDetAssign(lhs, curr);
        } else { // i <- tmp_i;
          curr = rfinder.createTmpAssignEdge(curr);
        }

        // exit this edge
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(dummyEdge);

        return to;
      }

      private CFANode handleExprStmt(CExpressionStatement stmt, CFANode from) {
        checkNotNull(stmt, from);
        return null;
      }

      private CFANode handleCallAssignStmt(CFunctionCallAssignmentStatement stmt, CFANode from) {
        checkNotNull(from);
        CExpression lvalue = stmt.getLeftHandSide();
        CFunctionCallExpression rvalue = stmt.getRightHandSide();
        pLog.log(Level.INFO, "lvalue:" + lvalue.toASTString());
        pLog.log(Level.INFO, "rvalue:" + rvalue.toASTString());
        pLog.log(
            Level.INFO,
            rvalue.getDeclaration() + "FUNC NAME: " + rvalue.getFunctionNameExpression());
        pLog.log(
            Level.INFO, rvalue.getExpressionType() + " PARAS: " + rvalue.getParameterExpressions());
        pLog.log(
            Level.INFO,
            rvalue.getDeclaration().getAttributes() + rvalue.getDeclaration().getName());
        pLog.log(
            Level.INFO,
            rvalue.getDeclaration().getOrigName() + rvalue.getDeclaration().getQualifiedName());
        pLog.log(Level.INFO, "rvalue:" + rvalue.getClass());

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

          CIntegerLiteralExpression zero =
              new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(0));

          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  decl.getType(),
                  name,
                  name,
                  name,
                  new CInitializerExpression(FileLocation.DUMMY, zero));
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

        private CVariableDeclaration createTmpVar(CVariableDeclaration decl) {
          String name = decl.getName();
          String tmpName = "TMP__" + name;
          CType ty = decl.getType();
          CVariableDeclaration tmpDecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  ty,
                  tmpName,
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(tmpName, tmpDecl);
          tmpVarMap.put(name, tmpName);
          return tmpDecl;
        }

        private CVariableDeclaration createNewVar(CVariableDeclaration decl) {
          String name = decl.getName();
          CIntegerLiteralExpression zero =
              new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(0));
          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  decl.getType(),
                  name,
                  name,
                  name,
                  new CInitializerExpression(FileLocation.DUMMY, zero));
          variables.put(name, newDecl);
          return newDecl;
        }

        private CExpression handleVarDecl(CVariableDeclaration decl) {
          String name = decl.getName();

          CVariableDeclaration newDecl = variables.get(name);
          if (!variables.containsKey(name)) {
            newDecl = createNewVar(decl);
          }

          if (leftVars.contains(name)) {
            newDecl = createTmpVar(decl);
          }

          return new CIdExpression(FileLocation.DUMMY, newDecl);
        }

        @Override
        public CExpression visit(CIdExpression pIastIdExpression) throws NoException {

          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();

          if (decl instanceof CVariableDeclaration) {
            return handleVarDecl((CVariableDeclaration) decl);
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

    private CFANode createAssumeEqualEdge(CExpression lExpr, CExpression rExpr, CFANode curr) {

      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLog);
      CExpression assumeExpr =
          builder.buildBinaryExpressionUnchecked(lExpr, rExpr, BinaryOperator.EQUALS);

      CFANode next = new CFANode(curr.getFunction());
      nodes.put(next.getFunctionName(), next);

      CAssumeEdge assumeEdge =
          new CAssumeEdge("", FileLocation.DUMMY, curr, next, assumeExpr, true, false, true);

      addToCFA(assumeEdge);
      curr = next;

      return curr;
    }
  }
}

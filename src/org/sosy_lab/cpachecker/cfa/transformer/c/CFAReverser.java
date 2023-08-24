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
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
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
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
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

  /** Add sv-comp-errorlabel to the original specification */
  public static Specification updateSpecForReverseCFA(
      Iterable<Path> pSpecFiles,
      CFA cfa,
      Configuration config,
      LogManager logger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, InterruptedException {
    checkNotNull(pSpecFiles);
    Path path =
        Classes.getCodeLocation(CFAReverser.class)
            .resolveSibling("config")
            .resolve("properties")
            .resolve("unreach-label.prp");
    return Specification.fromFiles(ImmutableSet.of(path), cfa, config, logger, pShutdownNotifier);
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
      this.funcDecls = new HashMap<>();
      // Search for the target in the original CFA
      this.targets = targetFinder.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpec);
      // Should contain at least target in the original CFA
      if (this.targets.size() < 1) {
        throw new AssertionError("Not found any target in this CFA");
      }
    }

    /**
     * @return the main entry node for the new CFA
     */
    private FunctionEntryNode findMainEntry() {
      String oldMainName = pCfa.getMainFunction().getFunctionName();
      return functions.get(oldMainName);
    }

    /**
     * Insert the error label in main function
     *
     * @param mainEntryNode the main entry node for the new CFA
     */
    private void insertErrorLabel(FunctionEntryNode mainEntryNode) {

      // exitNode
      FunctionExitNode exitNode = mainEntryNode.getExitNode().orElseThrow();
      assert exitNode.getNumEnteringEdges() == 1;
      // exitEdge
      CFAEdge exitEdge = exitNode.getEnteringEdge(0);
      // TODO: possible to be return edge
      assert exitEdge instanceof BlankEdge;
      CFANode predeccsorNode = exitEdge.getPredecessor();
      CFACreationUtils.removeEdgeFromNodes(exitEdge);

      CFALabelNode errorLabelNode = new CFALabelNode(mainEntryNode.getFunction(), "ERROR");
      nodes.put(mainEntryNode.getFunctionName(), errorLabelNode);

      BlankEdge errorLabelEdge =
          new BlankEdge("", FileLocation.DUMMY, predeccsorNode, errorLabelNode, "ERROR");

      addToCFA(errorLabelEdge);

      exitEdge = new BlankEdge("", FileLocation.DUMMY, errorLabelNode, exitNode, "default return");

      addToCFA(exitEdge);
    }

    private CFA createCfa() {
      // Reverse each Function's CFA
      for (Map.Entry<String, FunctionEntryNode> function : pCfa.getAllFunctions().entrySet()) {
        String name = function.getKey();
        CFunctionEntryNode entryNode = (CFunctionEntryNode) function.getValue();
        CfaFunctionBuilder cfaFuncBuilder = new CfaFunctionBuilder(entryNode);
        CFunctionEntryNode reverseEntryNode = cfaFuncBuilder.reverse();
        functions.put(name, reverseEntryNode);
      }

      // Get the main entry node for the new CFA
      FunctionEntryNode reverseMainEntry = findMainEntry();

      checkNotNull(reverseMainEntry);

      // Insert Error State
      insertErrorLabel(reverseMainEntry);

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

      private Map<String, CVariableDeclaration> variables; // Function scope
      private CFunctionEntryNode oldEntryNode; // Entry node for the old Function CFA
      private int branchCnt; // How many branch in this function
      private Set<CFANode> localTargets;
      private final Map<CFANode, CFANode> nodeMap;

      private CfaFunctionBuilder(CFunctionEntryNode oldEntryNode) {
        this.variables = new HashMap<>();
        this.oldEntryNode = oldEntryNode;
        this.branchCnt = 0;
        this.localTargets = new HashSet<>();
        this.nodeMap = new HashMap<>();
      }

      /**
       * @return main entry node of the reverse CFA
       */
      private CFunctionEntryNode reverse() {
        String funcName = oldEntryNode.getFunctionName();

        // get the new function declaration
        CFunctionDeclaration oldFuncDecl = oldEntryNode.getFunctionDefinition();
        CFunctionDeclaration newFuncDecl = reverseFunctionDeclaration(oldFuncDecl);
        funcDeclMap.put(oldFuncDecl, newFuncDecl);

        // old entry <-> new exit
        // old exit  <-> new entry
        FunctionExitNode newExitNode = new FunctionExitNode(newFuncDecl);
        nodeMap.put(oldEntryNode, newExitNode);
        nodes.put(newExitNode.getFunctionName(), newExitNode);

        FunctionExitNode oldExitNode = oldEntryNode.getExitNode().orElseThrow();
        CFunctionEntryNode newEntryNode =
            new CFunctionEntryNode(
                FileLocation.DUMMY, newFuncDecl, newExitNode, Optional.ofNullable(null));
        nodes.put(funcName, newEntryNode);

        newExitNode.setEntryNode(newEntryNode);

        // create two dummy node, in order to insert initialization edges between those
        // initStartNode --> int i --> initDoneNode --> target
        CFANode initStartNode = new CFANode(newFuncDecl); // node before variable initialization
        nodes.put(funcName, initStartNode);
        BlankEdge initStartEdge =
            new BlankEdge("", FileLocation.DUMMY, newEntryNode, initStartNode, "INIT VARS");
        addToCFA(initStartEdge);

        // node after function initialization.
        // real function body begin here
        CFANode initDoneNode = new CFANode(newFuncDecl);
        nodeMap.put(oldExitNode, initDoneNode);
        nodes.put(funcName, initDoneNode);

        // BFS the CFA
        newEntryNode = bfs(funcName, oldExitNode, newEntryNode, newFuncDecl);

        // Create a ndet variable for target branching
        CVariableDeclaration targetBranchVarDecl =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                false,
                CStorageClass.AUTO,
                intType,
                "TARGET__" + branchCnt,
                "TARGET__" + branchCnt,
                newFuncDecl.getName() + "::" + "TARGET__" + branchCnt,
                null);
        variables.put("TARGET__", targetBranchVarDecl);
        CIdExpression targetBranchVarExpr =
            new CIdExpression(FileLocation.DUMMY, targetBranchVarDecl);

        // Declare variable
        CFANode curr = initStartNode;
        curr = createVariableDeclaration(curr);

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
                    "Jump to target [" + targetCnt + "]");
            addToCFA(jumpTargetEdge);
            curr = targetBranchNode;
          }
        }

        return newEntryNode;
      }

      // BFS the old CFA to create the new CFA
      private CFunctionEntryNode bfs(
          String funcName,
          FunctionExitNode oldExitNode,
          CFunctionEntryNode newEntryNode,
          CFunctionDeclaration newFuncDecl) {

        checkNotNull(oldExitNode, newEntryNode);

        Set<CFANode> visited = new HashSet<>();
        Deque<CFANode> waitList = new ArrayDeque<>();
        waitList.add(oldExitNode);
        visited.add(oldExitNode);

        while (!waitList.isEmpty()) {
          CFANode oldhead = waitList.remove();
          CFANode newhead = nodeMap.get(oldhead);

          if (oldhead instanceof CFunctionEntryNode) {
            assert newhead instanceof FunctionExitNode;
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
            String branchVarname = newFuncDecl.getName() + "::" + "BRANCH__" + branchCnt;

            ndetBranchVarDecl =
                new CVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    CStorageClass.AUTO,
                    intType,
                    branchVarname,
                    branchVarname,
                    branchVarname,
                    null);

            variables.put(branchVarname, ndetBranchVarDecl);
            ndetBranchVarExpr = new CIdExpression(FileLocation.DUMMY, ndetBranchVarDecl);
            branchNode = createNoDetAssign(ndetBranchVarExpr, newhead);
          }

          int branchid = 0;
          CIntegerLiteralExpression ndetBranchIdExpr = null;

          for (CFAEdge oldEdge : CFAUtils.allEnteringEdges(oldhead)) {
            CFANode oldNext = oldEdge.getPredecessor();

            if (usingBranch) {

              if (ndetBranchIdExpr != null) {
                branchNode =
                    createAssumeEdge(ndetBranchVarExpr, ndetBranchIdExpr, branchNode, false);
              }

              ndetBranchIdExpr =
                  new CIntegerLiteralExpression(
                      FileLocation.DUMMY, intType, BigInteger.valueOf(branchid));

              branchid += 1;

              CFANode branchIdHead =
                  createAssumeEdge(ndetBranchVarExpr, ndetBranchIdExpr, branchNode, true);

              newhead = branchIdHead;
            }

            CFANode newNext = reverseEdge(oldEdge, newhead);

            checkNotNull(newNext);
            nodes.put(funcName, newNext);
            nodeMap.put(oldNext, newNext);

            if (visited.add(oldNext)) {
              waitList.add(oldNext);
            }
          }
        }

        return newEntryNode;
      }

      private CFANode createVariableDeclaration(CFANode curr) {
        CFANode next = null;

        for (CVariableDeclaration decl : variables.values()) {
          next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CDeclarationEdge ndeclEdge =
              new CDeclarationEdge("", decl.getFileLocation(), curr, next, decl);
          addToCFA(ndeclEdge);
          curr = next;
        }
        return curr;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Edge Reversing
      /////////////////////////////////////////////////////////////////////////////
      private CFANode reverseEdge(CFAEdge edge, CFANode from) {

        CFANode to = reverseCFANode(edge.getPredecessor());

        pLog.log(Level.INFO, "Reversing Edge: " + edge + " " + edge.getClass());

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
          reverseStmtEdge((CStatementEdge) edge, from, to);
        }

        return to;
      }

      /**
       * @param oldNode node in the original CFA
       * @return the corresponding node in the reverse CFA
       */
      private CFANode reverseCFANode(CFANode oldNode) {
        // already created
        if (nodeMap.containsKey(oldNode)) {
          return nodeMap.get(oldNode);
        }

        assert !(oldNode instanceof CFunctionEntryNode);

        CFunctionDeclaration newFuncDecl = funcDeclMap.get(oldNode.getFunction());

        checkNotNull(newFuncDecl);

        CFANode newNode = new CFANode(newFuncDecl);

        nodeMap.put(oldNode, newNode);
        nodes.put(newNode.getFunctionName(), newNode);
        return newNode;
      }

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
        CExpression expr = edge.getExpression();
        RightSideVariableFinder finder = new RightSideVariableFinder(new HashSet<>());
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
          reverseTypeDeclEdge(edge, from, to);
        } else if (decl instanceof CFunctionDeclaration) {
          reverseFuncDeclEdge(edge, from, to);
        } else {
          reverseVarDeclEdge(edge, from, to);
        }
      }

      private void reverseTypeDeclEdge(CDeclarationEdge edge, CFANode from, CFANode to) {
        BlankEdge blankEdge =
            new BlankEdge(
                edge.getRawStatement(), FileLocation.DUMMY, from, to, edge.getDescription());
        addToCFA(blankEdge);
      }

      private void reverseFuncDeclEdge(CDeclarationEdge edge, CFANode from, CFANode to) {
        BlankEdge blankEdge =
            new BlankEdge(
                edge.getRawStatement(), FileLocation.DUMMY, from, to, edge.getDescription());
        addToCFA(blankEdge);
      }

      private void reverseVarDeclEdge(CDeclarationEdge edge, CFANode from, CFANode to) {

        CVariableDeclaration decl = (CVariableDeclaration) edge.getDeclaration();
        String var = decl.getQualifiedName();

        // Create new variable
        if (!variables.containsKey(var)) {
          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  decl.getFileLocation(),
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  decl.getType(),
                  decl.getName(),
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(decl.getQualifiedName(), newDecl);
        }

        CInitializer init = decl.getInitializer();

        if (init == null) {
          BlankEdge blankEdge =
              new BlankEdge(
                  edge.getRawStatement(), FileLocation.DUMMY, from, to, edge.getDescription());
          addToCFA(blankEdge);
        }

        if (init instanceof CInitializerExpression) {
          RightSideVariableFinder rfinder = new RightSideVariableFinder(new HashSet<>());
          CExpression rvalue = ((CInitializerExpression) init).getExpression();

          CExpression rExpr = rvalue.accept(rfinder);
          CLeftHandSide lExpr = new CIdExpression(FileLocation.DUMMY, variables.get(var));

          CBinaryExpression assumeExpr = createAssumeExpr(lExpr, rExpr);
          CAssumeEdge assumeEdge =
              new CAssumeEdge("", FileLocation.DUMMY, from, to, assumeExpr, true, false, false);

          addToCFA(assumeEdge);
        }

        // Array initialization
        if (init instanceof CInitializerList) {

          List<CInitializer> initializerList = ((CInitializerList) init).getInitializers();
          assert decl.getType() instanceof CArrayType;
          CArrayType arrayType = (CArrayType) decl.getType();
          int arrayLength = arrayType.getLengthAsInt().orElseThrow();
          CType elemType = arrayType.getType();

          CFANode curr = from;
          CIdExpression arrayExpr = new CIdExpression(FileLocation.DUMMY, variables.get(var));

          for (int i = 0; i < arrayLength; i++) {

            CIntegerLiteralExpression subscriptExpr =
                new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(i));
            CArraySubscriptExpression lExpr =
                new CArraySubscriptExpression(
                    FileLocation.DUMMY, elemType, arrayExpr, subscriptExpr);
            CExpression rvalue = ((CInitializerExpression) initializerList.get(i)).getExpression();
            RightSideVariableFinder rfinder = new RightSideVariableFinder(new HashSet<>());
            CExpression rExpr = rvalue.accept(rfinder);

            if (i != arrayLength - 1) {
              curr = createAssumeEdge(lExpr, rExpr, curr, true);
            } else {
              CBinaryExpression assumeExpr = createAssumeExpr(lExpr, rExpr);
              CAssumeEdge assumeEdge =
                  new CAssumeEdge("", FileLocation.DUMMY, curr, to, assumeExpr, true, false, false);
              addToCFA(assumeEdge);
            }
          }
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
          handleCallStmt((CFunctionCallStatement) stmt, from, to);
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
        LeftSideVariableFinder lfinder = new LeftSideVariableFinder();
        CLeftHandSide lhs = (CLeftHandSide) lvalue.accept(lfinder);

        // right hand side
        RightSideVariableFinder rfinder = new RightSideVariableFinder(lfinder.lvalues);
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
        RightSideVariableFinder finder = new RightSideVariableFinder(new HashSet<>());
        CExpression expr = stmt.getExpression().accept(finder);

        CExpressionStatement exprStatement = new CExpressionStatement(FileLocation.DUMMY, expr);

        CStatementEdge stmtEdge =
            new CStatementEdge("", exprStatement, FileLocation.DUMMY, from, to);
        addToCFA(stmtEdge);

        return to;
      }

      private CFANode handleCallAssignStmt(CFunctionCallAssignmentStatement stmt, CFANode from) {
        // TODO: handleCallAssignStmt

        checkNotNull(from);
        CExpression lvalue = stmt.getLeftHandSide();
        CFunctionCallExpression rvalue = stmt.getRightHandSide();
        pLog.log(Level.INFO, "call assign stmt: " + stmt);
        pLog.log(Level.INFO, "lvalue:" + lvalue + " " + lvalue.getClass());
        pLog.log(Level.INFO, "rvalue:" + rvalue + " " + rvalue.getClass());

        return null;
      }

      private CFANode handleCallStmt(CFunctionCallStatement stmt, CFANode from, CFANode to) {
        // TODO: handleCallAssignStmt
        BlankEdge blankEdge =
            new BlankEdge(stmt.toString(), FileLocation.DUMMY, from, to, stmt.toString());
        addToCFA(blankEdge);
        return null;
      }

      /////////////////////////////////////////////////////////////////////////////
      // Left Side Expression Visitor
      /////////////////////////////////////////////////////////////////////////////
      private final class LeftSideVariableFinder
          implements CExpressionVisitor<CExpression, NoException> {

        private Set<CLeftHandSide> lvalues;

        private LeftSideVariableFinder() {
          this.lvalues = new HashSet<>();
        }

        private CVariableDeclaration createNewVar(CVariableDeclaration decl) {
          assert !variables.containsKey(decl.getQualifiedName());

          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  decl.getFileLocation(),
                  decl.isGlobal(),
                  decl.getCStorageClass(),
                  decl.getType(),
                  decl.getName(),
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);

          variables.put(decl.getQualifiedName(), newDecl);
          return newDecl;
        }

        private CIdExpression handleVarDeclExpr(CIdExpression pIastIdExpression) {
          CVariableDeclaration decl = (CVariableDeclaration) pIastIdExpression.getDeclaration();

          CVariableDeclaration newDecl;

          if (!variables.containsKey(decl.getQualifiedName())) {
            newDecl = createNewVar(decl);
          } else {
            newDecl = variables.get(decl.getQualifiedName());
          }

          checkNotNull(newDecl);

          CIdExpression newExpr = new CIdExpression(FileLocation.DUMMY, decl);
          lvalues.add(newExpr);
          return newExpr;
        }

        @Override
        public CExpression visit(CIdExpression pIastIdExpression) throws NoException {

          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();

          if (decl instanceof CVariableDeclaration) {
            return handleVarDeclExpr(pIastIdExpression);
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
        public CExpression visit(CPointerExpression pPointerExpression) throws NoException {
          CExpression operand = pPointerExpression.getOperand().accept(this);
          CPointerExpression newExpr =
              new CPointerExpression(
                  FileLocation.DUMMY, pPointerExpression.getExpressionType(), operand);
          lvalues.add(newExpr);
          return newExpr;
        }

        @Override
        public CExpression visit(CUnaryExpression pIastUnaryExpression) throws NoException {
          CExpression operand = pIastUnaryExpression.getOperand().accept(this);
          UnaryOperator operator = pIastUnaryExpression.getOperator();

          return new CUnaryExpression(
              FileLocation.DUMMY, pIastUnaryExpression.getExpressionType(), operand, operator);
        }

        @Override
        public CExpression visit(CFieldReference pIastFieldReference) throws NoException {
          CExpression owner = pIastFieldReference.getFieldOwner().accept(this);
          CFieldReference expr =
              new CFieldReference(
                  FileLocation.DUMMY,
                  pIastFieldReference.getExpressionType(),
                  pIastFieldReference.getFieldName(),
                  owner,
                  pIastFieldReference.isPointerDereference());
          lvalues.add(expr);
          return expr;
        }

        @Override
        public CExpression visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws NoException {
          CExpression array = pIastArraySubscriptExpression.getArrayExpression().accept(this);
          CExpression subscript =
              pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
          CType type = pIastArraySubscriptExpression.getExpressionType();

          CArraySubscriptExpression expr =
              new CArraySubscriptExpression(FileLocation.DUMMY, type, array, subscript);
          lvalues.add(expr);

          return expr;
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
        public CExpression visit(CCastExpression pIastCastExpression) throws NoException {
          // TODO CCastExpression
          return null;
        }

        @Override
        public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
            throws NoException {
          // TODO CAddressOfLabelExpression
          return null;
        }

        @Override
        public CExpression visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO : CComplexCastExpression
          return null;
        }

        @Override
        public CExpression visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
          // TODO CTypeIdExpressio
          return null;
        }
      }

      /////////////////////////////////////////////////////////////////////////////
      // Right Side Expression Visitor
      /////////////////////////////////////////////////////////////////////////////
      private final class RightSideVariableFinder
          implements CExpressionVisitor<CExpression, NoException> {

        private final Set<CLeftHandSide> lvalues; // left values in left side

        private final Map<CLeftHandSide, CIdExpression> tmpVarMap;

        private RightSideVariableFinder(Set<CLeftHandSide> lvalues) {
          this.lvalues = lvalues;
          this.tmpVarMap = new HashMap<>();
        }

        // Create `i = tmp_i`
        private CFANode createTmpAssignEdge(CFANode from) {
          CFANode curr = from;

          for (Map.Entry<CLeftHandSide, CIdExpression> e : tmpVarMap.entrySet()) {
            CLeftHandSide lExpr = e.getKey();
            CExpression rExpr = e.getValue();
            curr = createAssignEdge(lExpr, rExpr, curr);
          }
          return curr;
        }

        private CFANode resetTmpVar(CFANode from) {
          CFANode curr = from;
          for (CIdExpression tmpExpr : tmpVarMap.values()) {
            curr = createNoDetAssign(tmpExpr, curr);
          }
          return curr;
        }

        private CIdExpression createTmpValue(CLeftHandSide expr) {

          String tmpName = "tmp__" + expr.toQualifiedASTString();
          CVariableDeclaration tmpDecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  false,
                  CStorageClass.AUTO,
                  expr.getExpressionType(),
                  tmpName,
                  tmpName,
                  tmpName,
                  null);
          CIdExpression tmpExpr = new CIdExpression(FileLocation.DUMMY, tmpDecl);
          variables.put(tmpName, tmpDecl);
          tmpVarMap.put(expr, tmpExpr);
          return tmpExpr;
        }

        private CVariableDeclaration createNewVar(CVariableDeclaration decl) {

          CVariableDeclaration newDecl =
              new CVariableDeclaration(
                  decl.getFileLocation(),
                  false,
                  decl.getCStorageClass(),
                  decl.getType(),
                  decl.getName(),
                  decl.getOrigName(),
                  decl.getQualifiedName(),
                  null);
          variables.put(decl.getQualifiedName(), newDecl);
          return newDecl;
        }

        private CExpression handleVarDecl(CIdExpression pIastCIdExpression) {

          if (lvalues.contains(pIastCIdExpression)) {
            return createTmpValue(pIastCIdExpression);
          }

          CVariableDeclaration decl = (CVariableDeclaration) pIastCIdExpression.getDeclaration();

          CVariableDeclaration newDecl = variables.get(decl.getQualifiedName());

          if (newDecl == null) {
            newDecl = createNewVar(decl);
          }

          return new CIdExpression(pIastCIdExpression.getFileLocation(), newDecl);
        }

        @Override
        public CExpression visit(CIdExpression pIastIdExpression) throws NoException {

          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();

          if (decl instanceof CVariableDeclaration) {
            return handleVarDecl(pIastIdExpression);
          }

          return null;
        }

        @Override
        public CExpression visit(CPointerExpression pPointerExpression) throws NoException {

          if (lvalues.contains(pPointerExpression)) {
            return createTmpValue(pPointerExpression);
          }

          CExpression operand = pPointerExpression.getOperand().accept(this);

          return new CPointerExpression(
              FileLocation.DUMMY, pPointerExpression.getExpressionType(), operand);
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
        public CExpression visit(CUnaryExpression pIastUnaryExpression) throws NoException {
          CExpression operand = pIastUnaryExpression.getOperand().accept(this);
          UnaryOperator operator = pIastUnaryExpression.getOperator();

          return new CUnaryExpression(
              FileLocation.DUMMY, pIastUnaryExpression.getExpressionType(), operand, operator);
        }

        @Override
        public CExpression visit(CFieldReference pIastFieldReference) throws NoException {
          if (lvalues.contains(pIastFieldReference)) {
            return createTmpValue(pIastFieldReference);
          }

          CExpression owner = pIastFieldReference.getFieldOwner().accept(this);

          return new CFieldReference(
              FileLocation.DUMMY,
              pIastFieldReference.getExpressionType(),
              pIastFieldReference.getFieldName(),
              owner,
              pIastFieldReference.isPointerDereference());
        }

        @Override
        public CExpression visit(CArraySubscriptExpression pIastArraySubscriptExpression)
            throws NoException {

          if (lvalues.contains(pIastArraySubscriptExpression)) {
            return createTmpValue(pIastArraySubscriptExpression);
          }

          CExpression array = pIastArraySubscriptExpression.getArrayExpression().accept(this);
          CExpression subscript =
              pIastArraySubscriptExpression.getSubscriptExpression().accept(this);
          CType type = pIastArraySubscriptExpression.getExpressionType();

          return new CArraySubscriptExpression(FileLocation.DUMMY, type, array, subscript);
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
        public CExpression visit(CCastExpression pIastCastExpression) throws NoException {
          // TODO CCastExpression
          return null;
        }

        @Override
        public CExpression visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO : CComplexCastExpression
          return null;
        }

        @Override
        public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
            throws NoException {
          // TODO CAddressOfLabelExpression
          return null;
        }

        @Override
        public CExpression visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
          // TODO CTypeIdExpressio
          return null;
        }
      }
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
      return next;
    }

    /**
     * Helper method to create an Assignment edge
     *
     * @param lExpr expression at the left side
     * @param rExpr expression at the right side
     * @param curr current CFA node
     * @return the new CFA node
     */
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

    private CBinaryExpression createAssumeExpr(CExpression lExpr, CExpression rExpr) {
      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLog);
      return builder.buildBinaryExpressionUnchecked(lExpr, rExpr, BinaryOperator.EQUALS);
    }

    /**
     * Helper method to create an Assume edge
     *
     * @param lExpr expression at the left side
     * @param rExpr expression at the right side
     * @param curr the current CFA node
     * @param assume the assumption towards the expression
     * @return the new CFA node
     */
    private CFANode createAssumeEdge(
        CExpression lExpr, CExpression rExpr, CFANode curr, boolean assume) {

      CBinaryExpression assumeExpr = createAssumeExpr(lExpr, rExpr);

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

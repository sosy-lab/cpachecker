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
import com.google.common.collect.Iterables;
import com.google.common.collect.TreeMultimap;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
import org.sosy_lab.cpachecker.cfa.CFASecondPassBuilder;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.Language;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
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
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
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
            .resolve("specification")
            .resolve("ErrorLabel.spc");
    return Specification.fromFiles(ImmutableSet.of(path), cfa, config, logger, pShutdownNotifier);
  }

  /** The reverse CFA builder */
  private static final class CfaBuilder {
    private final CFA pCfa;
    private final Configuration pConfig;
    private final Specification pSpec;
    private final LogManager pLog;

    private final TargetLocationProvider targetFinder;
    private final NavigableMap<String, FunctionEntryNode> functions;
    private final TreeMultimap<String, CFANode> nodes;

    private final Set<CFANode> targets;
    private final Map<String, CFunctionDeclaration> funcDecls;
    private final Map<CFANode, CFANode> nodeMap;
    private Map<String, CVariableDeclaration> variables; // variables
    private int tmpCnt;

    private static final String parameterSuffix = "__REV_PARAMETER";
    private static final String tmpPrefix = "__REV__TMP__";
    private static final String targetPrefix = "__REV__TARGET__";
    private static final String branchPrefix = "__REV__BRANCH__";
    private static final String tmpSuffix = "__REV_TMP";

    private static CType intType =
        new CSimpleType(
            false, false, CBasicType.INT, false, false, false, false, false, false, false);

    private CfaBuilder(
        Configuration pConfiguration,
        Specification pSpecification,
        LogManager pLogger,
        CFA pCfa,
        ShutdownNotifier shutdownNotifier) {
      this.pConfig = pConfiguration;
      this.pSpec = pSpecification;
      this.pLog = pLogger;
      this.pCfa = pCfa;
      this.targetFinder = new TargetLocationProviderImpl(shutdownNotifier, pLogger, pCfa);
      this.functions = new TreeMap<>();
      this.nodes = TreeMultimap.create();
      this.funcDecls = new HashMap<>();
      this.nodeMap = new HashMap<>();
      this.variables = new HashMap<>();
      this.tmpCnt = 0;
      // Search for the target in the original CFA
      this.targets =
          new HashSet<>(targetFinder.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpec));
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

    /**
     * build the reverse CFA
     *
     * @return the reverse CFA
     */
    private CFA createCfa() {

      // preprocess targets
      Set<CFANode> entryTargets =
          new HashSet<>(
              targets.stream().filter(node -> node instanceof CFunctionEntryNode).toList());

      for (CFANode entryTarget : entryTargets) {
        for (CFAEdge edge : CFAUtils.allEnteringEdges(entryTarget)) {
          CFANode caller = edge.getPredecessor();
          targets.add(caller);
          String callerFunc = caller.getFunctionName();
          if (pCfa.getAllFunctions().get(callerFunc).getExitNode().isPresent()) {
            FunctionExitNode callerExitNode =
                pCfa.getAllFunctions().get(callerFunc).getExitNode().orElseThrow();
            for (CFAEdge returnEdge : CFAUtils.leavingEdges(callerExitNode)) {
              CFANode newTarget = returnEdge.getSuccessor();
              targets.add(newTarget);
            }
          }
        }
        targets.remove(entryTarget);
      }

      // reverse each function's CFA
      for (Map.Entry<String, FunctionEntryNode> function : pCfa.getAllFunctions().entrySet()) {
        String name = function.getKey();
        CFunctionEntryNode entryNode = (CFunctionEntryNode) function.getValue();
        CfaFunctionBuilder cfaFuncBuilder = new CfaFunctionBuilder(entryNode);
        Optional<CFunctionEntryNode> reverseEntryNode = cfaFuncBuilder.reverse();
        if (reverseEntryNode.isPresent()) {
          functions.put(name, reverseEntryNode.orElseThrow());
        }
      }

      // get the main entry node for the new CFA
      FunctionEntryNode reverseMainEntry = findMainEntry();

      checkNotNull(reverseMainEntry);

      // insert Error State
      insertErrorLabel(reverseMainEntry);

      MutableCFA newMutableCfa =
          new MutableCFA(
              functions, nodes, pCfa.getMetadata().withMainFunctionEntry(reverseMainEntry));

      newMutableCfa.entryNodes().forEach(CFAReversePostorder::assignIds);

      // get the loop structure
      LoopStructure loopStructure = null;
      try {
        loopStructure = LoopStructure.getLoopStructure(newMutableCfa);
      } catch (Exception e) {
        throw new AssertionError("failed to get the loop structure due to " + e);
      }
      newMutableCfa.setLoopStructure(loopStructure);

      try {
        CFASecondPassBuilder spbuilder =
            new CFASecondPassBuilder(newMutableCfa, Language.C, pLog, pConfig);
        spbuilder.insertCallEdgesRecursively();
        newMutableCfa.setMetadata(
            newMutableCfa.getMetadata().withConnectedness(CfaConnectedness.SUPERGRAPH));
      } catch (Exception e) {
        throw new AssertionError("failed to connect cfas due to " + e);
      }

      return newMutableCfa;
    }

    /** A Builder to create a reverse CFA corresponding the original CFA */
    private final class CfaFunctionBuilder {

      // private Map<String, CVariableDeclaration> variables; // Function scope variables
      private CFunctionEntryNode oldEntryNode; // Entry node for the old Function CFA
      private int branchCnt; // How many branch in this function
      private Set<CFANode> localTargets;
      private Map<CParameterDeclaration, CVariableDeclaration> parameters; // Function parameters

      private CfaFunctionBuilder(CFunctionEntryNode oldEntryNode) {
        // this.variables = new HashMap<>();
        this.parameters = new HashMap<>();
        this.oldEntryNode = oldEntryNode;
        this.branchCnt = 0;
        this.localTargets = new HashSet<>();
      }

      /**
       * @return main entry node of the reverse CFA
       */
      private Optional<CFunctionEntryNode> reverse() {
        String funcName = oldEntryNode.getFunctionName();

        Set<CFANode> oldLocalTargets =
            new HashSet<>(
                targets.stream().filter(node -> node.getFunctionName().equals(funcName)).toList());

        if (oldLocalTargets.isEmpty() && oldEntryNode.getExitNode().isEmpty()) {
          return Optional.empty();
        }

        // get the new function declaration
        CFunctionDeclaration oldFuncDecl = oldEntryNode.getFunctionDefinition();
        CFunctionDeclaration newFuncDecl = reverseFunctionDeclaration(oldFuncDecl);

        // old entry <-> new exit
        // old exit  <-> new entry
        FunctionExitNode newExitNode = new FunctionExitNode(newFuncDecl);
        nodeMap.put(oldEntryNode, newExitNode);
        nodes.put(newExitNode.getFunctionName(), newExitNode);

        FunctionExitNode oldExitNode = oldEntryNode.getExitNode().orElseThrow();

        checkNotNull(oldExitNode);
        CFunctionEntryNode newEntryNode =
            new CFunctionEntryNode(FileLocation.DUMMY, newFuncDecl, newExitNode, Optional.empty());

        nodes.put(funcName, newEntryNode);

        newExitNode.setEntryNode(newEntryNode);

        // create two dummy node, in order to insert initialization edges between those
        // initStartNode --> int i; --> initDoneNode --> target
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

        // bfs the CFA
        newEntryNode = bfs(funcName, oldExitNode, newEntryNode, newFuncDecl);

        String targetBranchName = newFuncDecl.getName() + "::" + targetPrefix + branchCnt;

        // create a ndet variable for target branching
        CVariableDeclaration targetBranchVarDecl =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                false,
                CStorageClass.AUTO,
                intType,
                targetPrefix + branchCnt,
                targetPrefix + branchCnt,
                targetBranchName,
                null);

        variables.put(targetBranchName, targetBranchVarDecl);
        CIdExpression targetBranchVarExpr =
            new CIdExpression(FileLocation.DUMMY, targetBranchVarDecl);

        // declare local variables
        CFANode curr = initStartNode;
        curr = createVariableDeclaration(curr);

        // create non det branch for each target
        jumpToTargets(targetBranchVarExpr, curr, initDoneNode);

        // insert function epilogue
        insertFuncEpilogue(newEntryNode);

        return Optional.of(newEntryNode);
      }

      /**
       * insert assumption edges for function parameters
       *
       * @param entryNode Function CFA entry node
       */
      private void insertFuncEpilogue(CFunctionEntryNode entryNode) {
        // exitNode
        FunctionExitNode exitNode = entryNode.getExitNode().orElseThrow();
        assert exitNode.getNumEnteringEdges() == 1;

        // exitEdge
        CFAEdge exitEdge = exitNode.getEnteringEdge(0);

        // TODO: possible to be return edge in the future
        assert exitEdge instanceof BlankEdge;

        CFACreationUtils.removeEdgeFromNodes(exitEdge);

        CFANode curr = exitEdge.getPredecessor();

        for (Map.Entry<CParameterDeclaration, CVariableDeclaration> e : parameters.entrySet()) {
          CIdExpression para = new CIdExpression(FileLocation.DUMMY, e.getKey());
          CIdExpression vara = new CIdExpression(FileLocation.DUMMY, e.getValue());
          curr = appendAssumeEdge(para, vara, curr, true);
        }

        exitEdge = new BlankEdge("", FileLocation.DUMMY, curr, exitNode, "default return");
        addToCFA(exitEdge);
      }

      private CFANode jumpToTargets(
          CIdExpression targetBranchVarExpr, CFANode curr, CFANode initDoneNode) {
        if (localTargets.isEmpty()) {
          BlankEdge initDoneEdge =
              new BlankEdge("init done", FileLocation.DUMMY, curr, initDoneNode, "INIT DONE");
          addToCFA(initDoneEdge);
          return initDoneNode;
        }

        CFANode targetBranchNode = curr;

        int targetCnt = 0;
        CIntegerLiteralExpression targetBranchID =
            new CIntegerLiteralExpression(
                FileLocation.DUMMY, intType, BigInteger.valueOf(targetCnt));
        curr = appendAssumeEdge(targetBranchVarExpr, targetBranchID, curr, true);
        curr = appendAssumeEdge(targetBranchVarExpr, targetBranchVarExpr, curr, false);
        BlankEdge initDoneEdge =
            new BlankEdge("init done", FileLocation.DUMMY, curr, initDoneNode, "INIT DONE");

        addToCFA(initDoneEdge);

        curr = targetBranchNode;
        if (!localTargets.isEmpty()) {
          for (CFANode localTarget : localTargets) {
            CIntegerLiteralExpression lastBranchID =
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, intType, BigInteger.valueOf(targetCnt));
            curr = appendAssumeEdge(targetBranchVarExpr, lastBranchID, curr, false);
            targetCnt += 1;
            CIntegerLiteralExpression currBranchID =
                new CIntegerLiteralExpression(
                    FileLocation.DUMMY, intType, BigInteger.valueOf(targetCnt));
            curr = appendAssumeEdge(targetBranchVarExpr, currBranchID, curr, true);
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
        return curr;
      }

      /**
       * bfs the function CFA to create the reverse function CFA
       *
       * @param funcName currnet function name
       * @param oldExitNode exit node for the original function CFA
       * @param newEntryNode entry node for the reverse function CFA
       * @param newFuncDecl reverse function declaration
       * @return reverse CFA entry node
       */
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

        Set<CFANode> oldLocalTargets =
            new HashSet<>(
                targets.stream().filter(node -> node.getFunctionName().equals(funcName)).toList());

        for (CFANode oldLocalTarget : oldLocalTargets) {
          CFANode newLocalTarget = reverseCFANode(oldLocalTarget);
          nodes.put(funcName, newLocalTarget);
          nodeMap.put(oldLocalTarget, newLocalTarget);
          waitList.add(oldLocalTarget);
          visited.add(oldLocalTarget);
        }

        while (!waitList.isEmpty()) {
          CFANode oldhead = waitList.remove();
          CFANode newhead = nodeMap.get(oldhead);

          if (targets.contains(oldhead)) {
            localTargets.add(newhead);
          }

          if (oldhead.isLoopStart()) {
            newhead.setLoopStart();
          }

          checkNotNull(newhead);

          nodes.put(funcName, newhead);

          // =================================================
          // handle branching
          Boolean usingBranch = false;
          CVariableDeclaration ndetBranchVarDecl = null;
          CIdExpression ndetBranchVarExpr = null;
          CFANode branchNode = null;

          // Create Branching
          // newhead -> branchID1 -> newNext1
          //         -> branchID2 -> newNext2
          if (oldhead.getNumEnteringEdges() > 1 && !(newhead instanceof FunctionExitNode)) {
            branchCnt += 1;
            usingBranch = true;

            // Create a ndet variable
            String branchVarname = newFuncDecl.getName() + "::" + branchPrefix + branchCnt;

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
            branchNode = appendNonDetAssignEdge(ndetBranchVarExpr, newhead);
          }
          // =================================================

          int branchid = 0;
          CIntegerLiteralExpression ndetBranchIdExpr = null;

          for (CFAEdge oldEdge : CFAUtils.allEnteringEdges(oldhead)) {

            CFANode oldNext = oldEdge.getPredecessor();

            if (!oldNext.getFunctionName().equals(oldhead.getFunctionName())) {
              continue;
            }

            if (usingBranch) {

              if (ndetBranchIdExpr != null) {
                branchNode =
                    appendAssumeEdge(ndetBranchVarExpr, ndetBranchIdExpr, branchNode, false);
              }

              ndetBranchIdExpr =
                  new CIntegerLiteralExpression(
                      FileLocation.DUMMY, intType, BigInteger.valueOf(branchid));

              branchid += 1;

              CFANode branchIdHead =
                  appendAssumeEdge(ndetBranchVarExpr, ndetBranchIdExpr, branchNode, true);

              newhead = branchIdHead;
            }

            CFANode newNext = reverseEdge(oldEdge, newhead);

            checkNotNull(newNext);
            nodes.put(funcName, newNext);
            nodeMap.put(oldNext, newNext);

            // do not enter in other functions
            if (visited.add(oldNext)) {
              waitList.add(oldNext);
            }
          }
        }

        return newEntryNode;
      }

      /**
       * append the variable declaration edge chain to the head node In some cases, this method
       * should also be responsible for initializing the variables.
       *
       * @param curr head node of the edge chain
       * @return tail node of the edge chain
       */
      private CFANode createVariableDeclaration(CFANode curr) {
        for (CVariableDeclaration decl : variables.values()) {
          CIdExpression lhs = new CIdExpression(FileLocation.DUMMY, decl);
          CType realType = getRealType(lhs.getExpressionType());
          if (realType instanceof CCompositeType compositeType) {
            List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
            for (CCompositeTypeMemberDeclaration member : members) {
              CFieldReference fieldReference =
                  new CFieldReference(
                      FileLocation.DUMMY, member.getType(), member.getName(), lhs, false);
              curr = appendNonDetAssignEdge(fieldReference, curr);
            }
          }
        }
        return curr;
      }

      /**
       * create the reverse edge chain for the original edge
       *
       * @param edge the original edge
       * @param from head node of the reverse edge chain
       * @return tail node of the reverse edge chain
       */
      private CFANode reverseEdge(CFAEdge edge, CFANode from) {

        CFANode to = reverseCFANode(edge.getPredecessor());

        assert !(edge instanceof CFunctionCallEdge || edge instanceof CFunctionReturnEdge);

        if (edge instanceof BlankEdge blankEdge) {
          reverseBlankEdge(blankEdge, from, to);
        } else if (edge instanceof CDeclarationEdge declEdge) {
          reverseDeclEdge(declEdge, from, to);
        } else if (edge instanceof CAssumeEdge assumeEdge) {
          reverseAssumeEdge(assumeEdge, from, to);
        } else if (edge instanceof CFunctionSummaryEdge funcSummaryEdge) {
          reverseFunctionSummaryEdge(funcSummaryEdge, from, to);
        } else if (edge instanceof CReturnStatementEdge returnStmtEdge) {
          reverseReturnStmtEdge(returnStmtEdge, from, to);
        } else if (edge instanceof CFunctionSummaryStatementEdge) {
          pLog.log(Level.INFO, "CFunctionSummaryStatementEdge: " + edge);
        } else if (edge instanceof CStatementEdge stmtEdge) {
          reverseStmtEdge(stmtEdge, from, to);
        } else {
          throw new AssertionError("Illegal edge: " + edge);
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

        CFunctionDeclaration oldFuncDecl = (CFunctionDeclaration) oldNode.getFunction();
        String funcName = oldFuncDecl.getName();

        CFunctionDeclaration newFuncDecl = funcDecls.get(funcName);

        if (newFuncDecl == null) {
          newFuncDecl = reverseFunctionDeclaration(oldFuncDecl);
          funcDecls.put(funcName, newFuncDecl);
        }

        CFANode newNode = null;

        if (oldNode instanceof CFunctionEntryNode) {
          newNode = new FunctionExitNode(newFuncDecl);
        } else if (oldNode instanceof FunctionExitNode) {
          newNode =
              new CFunctionEntryNode(
                  FileLocation.DUMMY, newFuncDecl, null, Optional.ofNullable(null));
        } else {
          newNode = new CFANode(newFuncDecl);
        }

        checkNotNull(newFuncDecl, newNode);

        nodeMap.put(oldNode, newNode);
        nodes.put(newNode.getFunctionName(), newNode);
        return newNode;
      }

      private CFunctionDeclaration reverseFunctionDeclaration(CFunctionDeclaration funcDecl) {
        if (funcDecls.get(funcDecl.getName()) != null) {
          return funcDecls.get(funcDecl.getName());
        }
        String funcName = funcDecl.getName();

        if (funcName.equals("main")) {
          return new CFunctionDeclaration(
              FileLocation.DUMMY,
              funcDecl.getType(),
              funcDecl.getName(),
              funcDecl.getParameters(),
              funcDecl.getAttributes());
        }

        if (!pCfa.getAllFunctions().containsKey(funcName)) {
          return new CFunctionDeclaration(
              FileLocation.DUMMY,
              funcDecl.getType(),
              funcDecl.getName(),
              funcDecl.getParameters(),
              funcDecl.getAttributes());
        }

        List<CParameterDeclaration> paras = new ArrayList<>();
        Iterables.addAll(paras, funcDecl.getParameters());

        CType returnType = funcDecl.getType().getReturnType();
        CParameterDeclaration returnVarDecl =
            new CParameterDeclaration(FileLocation.DUMMY, returnType, "__rev__retval__");
        returnVarDecl.setQualifiedName(funcDecl.getName() + "__rev__retval__");

        List<CType> paraTypes = new ArrayList<>();
        Iterables.addAll(paraTypes, funcDecl.getType().getParameters());

        if (returnType != CVoidType.VOID) {
          paras.add(returnVarDecl);
          paraTypes.add(returnType);
        }
        CFunctionType functionType = new CFunctionType(CVoidType.VOID, paraTypes, false);

        return new CFunctionDeclaration(
            funcDecl.getFileLocation(),
            functionType,
            funcDecl.getName(),
            funcDecl.getOrigName(),
            paras,
            funcDecl.getAttributes());
      }

      private void reverseFunctionSummaryEdge(CFunctionSummaryEdge edge, CFANode from, CFANode to) {
        CFunctionCall oldCall = edge.getExpression();
        if (oldCall instanceof CFunctionCallAssignmentStatement funcCallAssignStmt) {
          handleCallAssignStmt(funcCallAssignStmt, from, to);
        } else if (oldCall instanceof CFunctionCallStatement funcCallStmt) {
          handleCallStmt(funcCallStmt, from, to);
        } else {
          throw new AssertionError("Illegal function summary edge: " + edge);
        }
      }

      private void handleBuiltInFunc(
          CFunctionCallAssignmentStatement stmt, CFANode from, CFANode to) {
        CFANode curr = from;
        LeftSideVariableFinder lfinder = new LeftSideVariableFinder();
        CLeftHandSide lvalue = (CLeftHandSide) stmt.getLeftHandSide().accept(lfinder);
        CFunctionCallAssignmentStatement assignmentStatement =
            new CFunctionCallAssignmentStatement(
                FileLocation.DUMMY, lvalue, stmt.getFunctionCallExpression());
        CFANode next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);
        CStatementEdge statementEdge =
            new CStatementEdge("", assignmentStatement, FileLocation.DUMMY, curr, next);
        addToCFA(statementEdge);
        curr = next;
        // reset
        curr = appendNonDetAssignEdge(lvalue, curr);
        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(blankEdge);
      }

      private void handleExternFunc(
          CFunctionCallAssignmentStatement stmt, CFANode from, CFANode to) {

        String tmpName = oldEntryNode.getFunctionName() + "::" + tmpPrefix + tmpCnt;
        CType type = stmt.getLeftHandSide().getExpressionType();
        tmpCnt += 1;
        // TODO: nodet assign tmp var
        CVariableDeclaration tmpDecl =
            new CVariableDeclaration(
                FileLocation.DUMMY,
                false,
                CStorageClass.AUTO,
                type,
                tmpName,
                tmpName,
                tmpName,
                null);
        CIdExpression tmpExpr = new CIdExpression(FileLocation.DUMMY, tmpDecl);
        variables.put(tmpName, tmpDecl);

        CFunctionCallAssignmentStatement callAssignStmt =
            new CFunctionCallAssignmentStatement(
                FileLocation.DUMMY, tmpExpr, stmt.getFunctionCallExpression());

        CFANode curr = from;
        CFANode next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);

        CStatementEdge callAssignStatementEdge =
            new CStatementEdge("", callAssignStmt, FileLocation.DUMMY, curr, next);
        addToCFA(callAssignStatementEdge);
        curr = next;

        next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);
        CExpression assumeExpr = createAssumeExpr(stmt.getLeftHandSide(), tmpExpr);
        CAssumeEdge assumeEdge =
            new CAssumeEdge("", FileLocation.DUMMY, curr, next, assumeExpr, true, false, false);
        addToCFA(assumeEdge);
        curr = next;
        LeftSideVariableFinder lfinder = new LeftSideVariableFinder();
        CLeftHandSide lvalue = (CLeftHandSide) stmt.getLeftHandSide().accept(lfinder);
        curr = appendNonDetAssignEdge(lvalue, curr);
        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(blankEdge);
      }

      /**
       * handle call assignment statement edge like `x = foo();`
       *
       * @param stmt the original function call assignment statement
       * @param from head node of the edge chain
       * @param to tail node of the edge chain
       */
      private void handleCallAssignStmt(
          CFunctionCallAssignmentStatement stmt, CFANode from, CFANode to) {

        CFANode curr = from;

        String funcName = stmt.getFunctionCallExpression().getDeclaration().getName();

        // builtin function
        if (stmt.getFunctionCallExpression().getDeclaration() == null) {
          handleBuiltInFunc(stmt, from, to);
          return;
        }

        // extern function
        if (!pCfa.getAllFunctionNames().contains(funcName)) {
          handleExternFunc(stmt, from, to);
          return;
        }

        CFunctionCallExpression callExpr = stmt.getFunctionCallExpression();
        CFunctionDeclaration oldDecl = callExpr.getDeclaration();
        CFunctionDeclaration newDecl = funcDecls.get(funcName);

        // add new function declaration
        if (newDecl == null) {
          newDecl = reverseFunctionDeclaration(oldDecl);
          funcDecls.put(funcName, newDecl);
        }

        checkNotNull(newDecl);

        LeftSideVariableFinder lfinder = new LeftSideVariableFinder();
        CLeftHandSide lvalue = (CLeftHandSide) stmt.getLeftHandSide().accept(lfinder);

        RightSideVariableFinder rfinder = new RightSideVariableFinder(lfinder.lvalues);

        List<CExpression> newArgsList = new ArrayList<>();

        for (CExpression arg : callExpr.getParameterExpressions()) {
          CExpression newArg = arg.accept(rfinder);
          newArgsList.add(newArg);
        }
        newArgsList.add(lvalue);

        CIdExpression funcExpr = new CIdExpression(FileLocation.DUMMY, newDecl);

        CFunctionCallExpression newCallExpr =
            new CFunctionCallExpression(
                FileLocation.DUMMY, callExpr.getExpressionType(), funcExpr, newArgsList, newDecl);

        CFunctionCallStatement newFuncCallStmt =
            new CFunctionCallStatement(FileLocation.DUMMY, newCallExpr);

        CFANode next = new CFANode(curr.getFunction());
        nodes.put(next.getFunctionName(), next);

        CStatementEdge newStmtEdge =
            new CStatementEdge("", newFuncCallStmt, FileLocation.DUMMY, curr, next);
        addToCFA(newStmtEdge);

        curr = next;

        if (rfinder.tmpVarMap.size() == 0 && !lvalue.getExpressionType().isConst()) {
          curr = appendNonDetAssignEdge(lvalue, curr);
        } else { // i <- tmp_i;
          curr = rfinder.createTmpAssignEdge(curr);
          curr = rfinder.resetTmpVar(curr);
        }

        BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(blankEdge);
      }

      /**
       * handle call statement edge like `foo();`
       *
       * @param stmt the original function call statement
       * @param from head node of edge chain
       * @param to tail node of edge chain
       */
      private void handleCallStmt(CFunctionCallStatement stmt, CFANode from, CFANode to) {

        CFunctionDeclaration oldDecl = stmt.getFunctionCallExpression().getDeclaration();
        String funcName = oldDecl.getName();
        CFunctionDeclaration newDecl = funcDecls.get(funcName);

        // add new function declaration
        if (newDecl == null) {
          newDecl = reverseFunctionDeclaration(oldDecl);
          funcDecls.put(funcName, newDecl);
        }

        checkNotNull(newDecl);

        List<CExpression> newArgsList = new ArrayList<>();

        RightSideVariableFinder rfinder = new RightSideVariableFinder(new HashSet<>());

        for (CExpression arg : stmt.getFunctionCallExpression().getParameterExpressions()) {
          CExpression newArg = arg.accept(rfinder);
          newArgsList.add(newArg);
        }

        CType retType = oldDecl.getType().getReturnType();
        if (retType != CVoidType.VOID) {
          String tmpName = oldEntryNode.getFunctionName() + "::" + tmpPrefix + tmpCnt;
          tmpCnt += 1;
          CVariableDeclaration tmpDecl =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  false,
                  CStorageClass.AUTO,
                  retType,
                  tmpName,
                  tmpName,
                  tmpName,
                  null);
          CIdExpression tmpExpr = new CIdExpression(FileLocation.DUMMY, tmpDecl);
          variables.put(tmpName, tmpDecl);
          newArgsList.add(tmpExpr);
        }

        CIdExpression newFuncExpr = new CIdExpression(FileLocation.DUMMY, newDecl);

        CFunctionCallExpression newCallExpr =
            new CFunctionCallExpression(
                FileLocation.DUMMY,
                stmt.getFunctionCallExpression().getExpressionType(),
                newFuncExpr,
                newArgsList,
                newDecl);

        CFunctionCallStatement newCallStmt =
            new CFunctionCallStatement(FileLocation.DUMMY, newCallExpr);

        CStatementEdge newCallStmtEdge =
            new CStatementEdge("", newCallStmt, FileLocation.DUMMY, from, to);

        addToCFA(newCallStmtEdge);
      }

      private void reverseReturnStmtEdge(CReturnStatementEdge edge, CFANode from, CFANode to) {
        CReturnStatement stmt = edge.getReturnStatement();
        // `return;`
        if (stmt.getReturnValue().isEmpty()) {
          BlankEdge blankEdge = new BlankEdge("", FileLocation.DUMMY, from, to, "");
          addToCFA(blankEdge);
          return;
        }

        LeftSideVariableFinder retValfinder = new LeftSideVariableFinder();
        CExpression retVal = stmt.getReturnValue().orElseThrow().accept(retValfinder);

        CParameterDeclaration retVarDecl =
            handleReturnVariable(
                (CIdExpression) stmt.asAssignment().orElseThrow().getLeftHandSide(),
                from.getFunctionName());
        CIdExpression retVar = new CIdExpression(FileLocation.DUMMY, retVarDecl);

        CFANode curr = appendAssumeEdge(retVal, retVar, from, true);
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(dummyEdge);
      }

      private CParameterDeclaration handleReturnVariable(CIdExpression retVar, String funcName) {
        String retVarName = "__rev__retval__";
        CParameterDeclaration retVarDecl =
            new CParameterDeclaration(
                FileLocation.DUMMY, retVar.getExpressionType(), "__rev__retval__");
        retVarDecl.setQualifiedName(funcName + retVarName);
        return retVarDecl;
      }

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
                  FileLocation.DUMMY,
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

          CExpression assumeExpr = createAssumeExpr(lExpr, rExpr);

          CFANode curr = from;
          CFANode next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);

          CAssumeEdge assumeEdge =
              new CAssumeEdge("", FileLocation.DUMMY, curr, next, assumeExpr, true, false, false);
          addToCFA(assumeEdge);

          curr = next;
          curr = appendNonDetAssignEdge(lExpr, curr);

          BlankEdge dummyEdge =
              new BlankEdge("", FileLocation.DUMMY, curr, to, edge.getDescription());
          addToCFA(dummyEdge);
        }

        // Array initialization
        if (init instanceof CInitializerList) {

          List<CInitializer> initializerList = ((CInitializerList) init).getInitializers();
          assert decl.getType() instanceof CArrayType;
          CArrayType arrayType = (CArrayType) decl.getType();

          CFANode curr = from;
          CIdExpression arrayExpr = new CIdExpression(FileLocation.DUMMY, variables.get(var));

          curr = handleInitializerList(initializerList, arrayExpr, arrayType, curr);

          BlankEdge dummyEdge =
              new BlankEdge("", FileLocation.DUMMY, curr, to, edge.getDescription());
          addToCFA(dummyEdge);
        }
      }

      // recursive handling the initializer list
      private CFANode handleInitializerList(
          List<CInitializer> initializerList,
          CExpression arrayExpr,
          CArrayType arrayType,
          CFANode curr) {
        int arrayLength = initializerList.size();

        // reversely create the assume edge
        for (int i = arrayLength - 1; i >= 0; i--) {
          CInitializer initializer = initializerList.get(i);
          if (initializer instanceof CInitializerList) {
            CType elemType = arrayType.getType();
            assert elemType instanceof CArrayType;
            CIntegerLiteralExpression subscriptExpr =
                new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(i));

            CExpression elemExpr =
                new CArraySubscriptExpression(
                    FileLocation.DUMMY, elemType, arrayExpr, subscriptExpr);
            curr =
                handleInitializerList(
                    ((CInitializerList) initializer).getInitializers(),
                    elemExpr,
                    (CArrayType) elemType,
                    curr);
            continue;
          }

          assert initializer instanceof CInitializerExpression;

          // Bottom Case
          CType elemType = arrayType.getType();
          assert !(elemType instanceof CArrayType);
          CIntegerLiteralExpression subscriptExpr =
              new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(i));

          CArraySubscriptExpression lExpr =
              new CArraySubscriptExpression(FileLocation.DUMMY, elemType, arrayExpr, subscriptExpr);

          CExpression rvalue = ((CInitializerExpression) initializer).getExpression();
          RightSideVariableFinder rfinder = new RightSideVariableFinder(new HashSet<>());
          CExpression rExpr = rvalue.accept(rfinder);

          curr = appendAssumeEdge(lExpr, rExpr, curr, true);
        }

        return curr;
      }

      private void reverseStmtEdge(CStatementEdge edge, CFANode from, CFANode to) {
        CStatement stmt = edge.getStatement();
        if (stmt instanceof CExpressionAssignmentStatement exprAssignStmt) {
          handleExprAssignStmt(exprAssignStmt, from, to);
        } else if (stmt instanceof CExpressionStatement exprStmt) {
          handleExprStmt(exprStmt, from, to);
        } else if (stmt instanceof CFunctionCallAssignmentStatement funcCallAssignStmt) {
          handleCallAssignStmt(funcCallAssignStmt, from, to);
        } else if (stmt instanceof CFunctionCallStatement funcCallStmt) {
          handleCallStmt(funcCallStmt, from, to);
        }
      }

      private void handleExprAssignStmt(
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

        curr = appendAssumeEdge(lhs, rhs, curr, true);

        // the left side
        if (rfinder.tmpVarMap.size() == 0 && !lvalue.getExpressionType().isConst()) {
          curr = appendNonDetAssignEdge(lhs, curr);
        } else { // i <- tmp_i;
          curr = rfinder.createTmpAssignEdge(curr);
          curr = rfinder.resetTmpVar(curr);
        }

        // exit this edge
        BlankEdge dummyEdge = new BlankEdge("", FileLocation.DUMMY, curr, to, "");
        addToCFA(dummyEdge);
      }

      private void handleExprStmt(CExpressionStatement stmt, CFANode from, CFANode to) {
        checkNotNull(from, to);
        RightSideVariableFinder finder = new RightSideVariableFinder(new HashSet<>());
        CExpression expr = stmt.getExpression().accept(finder);

        CExpressionStatement exprStmt = new CExpressionStatement(FileLocation.DUMMY, expr);

        CStatementEdge stmtEdge = new CStatementEdge("", exprStmt, FileLocation.DUMMY, from, to);
        addToCFA(stmtEdge);
      }

      /** Visitor to create a new expression for a left side expression */
      private final class LeftSideVariableFinder
          implements CExpressionVisitor<CExpression, NoException> {

        // subexpressions
        private Set<CLeftHandSide> lvalues;

        private LeftSideVariableFinder() {
          this.lvalues = new HashSet<>();
        }

        /**
         * get the variable declaration corresponding to the original declaration. If uncreated,
         * then create a new variable declaration
         *
         * @param decl the original declaration
         * @return the declaration in the reverse CFA
         */
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

        /**
         * get the variable expression corresponding to the original variable expression. If
         * uncreated, then create a new variable expression
         *
         * @param expr the variable expression in the original CFA
         * @return the variable expression in the reverse CFA
         */
        private CIdExpression handleVarDeclExpr(CIdExpression expr) {
          CVariableDeclaration decl = (CVariableDeclaration) expr.getDeclaration();

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

        /**
         * get the variable expression corresponding to the original parameter expression. If
         * uncreated, then create a new variable expressions
         *
         * @param expr the parameter expression in the original CFA
         * @return the variable expression in the reverse CFA
         */
        private CIdExpression handleParaDeclExpr(CIdExpression expr) {

          CParameterDeclaration paraDecl = (CParameterDeclaration) expr.getDeclaration();
          CVariableDeclaration varaDecl = variables.get(paraDecl.getQualifiedName());

          // handle new parameter
          if (varaDecl == null) {
            varaDecl =
                new CVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    CStorageClass.AUTO,
                    paraDecl.getType(),
                    paraDecl.getName() + parameterSuffix,
                    paraDecl.getOrigName() + parameterSuffix,
                    paraDecl.getQualifiedName() + parameterSuffix,
                    null);
            CParameterDeclaration newParaDecl =
                new CParameterDeclaration(
                    FileLocation.DUMMY, paraDecl.getType(), paraDecl.getName());
            newParaDecl.setQualifiedName(paraDecl.getQualifiedName());
            parameters.put(newParaDecl, varaDecl);
          }

          return new CIdExpression(FileLocation.DUMMY, varaDecl);
        }

        @Override
        public CExpression visit(CIdExpression pIastIdExpression) throws NoException {

          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();

          if (decl instanceof CVariableDeclaration) {
            return handleVarDeclExpr(pIastIdExpression);
          } else if (decl instanceof CParameterDeclaration) {
            return handleParaDeclExpr(pIastIdExpression);
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
          // index do not need to be dumped
          RightSideVariableFinder rfinder = new RightSideVariableFinder(new HashSet<>());
          CExpression subscript =
              pIastArraySubscriptExpression.getSubscriptExpression().accept(rfinder);
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
          CExpression expr = pIastCastExpression.getOperand().accept(this);
          CType type = pIastCastExpression.getExpressionType();
          return new CCastExpression(FileLocation.DUMMY, type, expr);
        }

        @Override
        public CExpression visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO : CComplexCastExpression
          throw new AssertionError(pComplexCastExpression.toString());
        }

        @Override
        public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
            throws NoException {
          // TODO CAddressOfLabelExpression
          throw new AssertionError(pAddressOfLabelExpression.toString());
        }

        @Override
        public CExpression visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
          // TODO CTypeIdExpressio
          throw new AssertionError(pIastTypeIdExpression.toString());
        }
      }

      /** Visitor to create a new expression for a right side expression */
      private final class RightSideVariableFinder
          implements CExpressionVisitor<CExpression, NoException> {

        // left values in left side
        private final ImmutableSet<CLeftHandSide> lvalues;

        // temporary variables for left values
        private final Map<CLeftHandSide, CIdExpression> tmpVarMap;

        private RightSideVariableFinder(Set<CLeftHandSide> lvalues) {
          this.lvalues = ImmutableSet.copyOf(lvalues);
          this.tmpVarMap = new HashMap<>();
        }

        /**
         * create `i = tmp_i` edges for each temporary variables
         *
         * @param from head node of the edge chain
         * @return tail node of the edge chain
         */
        private CFANode createTmpAssignEdge(CFANode from) {
          CFANode curr = from;

          for (Map.Entry<CLeftHandSide, CIdExpression> e : tmpVarMap.entrySet()) {
            CLeftHandSide lExpr = e.getKey();
            CExpression rExpr = e.getValue();
            curr = appendAssignEdge(lExpr, rExpr, curr);
          }
          return curr;
        }

        /**
         * create non-det assignment for each temporary variables
         *
         * @param from head node of the edge chain
         * @return tail node of the edge chain
         */
        private CFANode resetTmpVar(CFANode from) {
          CFANode curr = from;
          for (CIdExpression tmpExpr : tmpVarMap.values()) {
            curr = appendNonDetAssignEdge(tmpExpr, curr);
          }
          return curr;
        }

        /**
         * create the temporary variable expression, corresponding to the left side subexpression
         *
         * @param expr the left side subexpression
         * @return the temporary variable expression
         */
        private CIdExpression createTmpValue(CLeftHandSide expr) {

          String tmpName = expr.toQualifiedASTString() + tmpSuffix;
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

        private CExpression handleVarDecl(CIdExpression expr) {

          if (lvalues.contains(expr)) {
            return createTmpValue(expr);
          }

          CVariableDeclaration decl = (CVariableDeclaration) expr.getDeclaration();

          CVariableDeclaration newDecl = variables.get(decl.getQualifiedName());

          if (newDecl == null) {
            newDecl = createNewVar(decl);
          }

          return new CIdExpression(expr.getFileLocation(), newDecl);
        }

        private CIdExpression handleParaDeclExpr(CIdExpression expr) {

          CParameterDeclaration paraDecl = (CParameterDeclaration) expr.getDeclaration();
          CVariableDeclaration varaDecl = variables.get(paraDecl.getQualifiedName());

          // handle new parameter
          if (varaDecl == null) {
            varaDecl =
                new CVariableDeclaration(
                    FileLocation.DUMMY,
                    false,
                    CStorageClass.AUTO,
                    paraDecl.getType(),
                    paraDecl.getName() + parameterSuffix,
                    paraDecl.getOrigName() + parameterSuffix,
                    paraDecl.getQualifiedName() + parameterSuffix,
                    null);
            CParameterDeclaration newParaDecl =
                new CParameterDeclaration(
                    FileLocation.DUMMY, paraDecl.getType(), paraDecl.getName());
            newParaDecl.setQualifiedName(paraDecl.getQualifiedName());
            parameters.put(newParaDecl, varaDecl);
          }

          return new CIdExpression(FileLocation.DUMMY, varaDecl);
        }

        @Override
        public CExpression visit(CIdExpression pIastIdExpression) throws NoException {

          CSimpleDeclaration decl = pIastIdExpression.getDeclaration();

          if (decl instanceof CVariableDeclaration) {
            return handleVarDecl(pIastIdExpression);
          } else if (decl instanceof CParameterDeclaration) {
            return handleParaDeclExpr(pIastIdExpression);
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
          CExpression expr = pIastCastExpression.getOperand().accept(this);
          CType type = pIastCastExpression.getExpressionType();
          return new CCastExpression(FileLocation.DUMMY, type, expr);
        }

        @Override
        public CExpression visit(CComplexCastExpression pComplexCastExpression) throws NoException {
          // TODO : CComplexCastExpression
          throw new AssertionError(pComplexCastExpression.toString());
        }

        @Override
        public CExpression visit(CAddressOfLabelExpression pAddressOfLabelExpression)
            throws NoException {
          // TODO CAddressOfLabelExpression
          throw new AssertionError(pAddressOfLabelExpression.toString());
        }

        @Override
        public CExpression visit(CTypeIdExpression pIastTypeIdExpression) throws NoException {
          // TODO CTypeIdExpressio
          throw new AssertionError(pIastTypeIdExpression.toString());
        }
      }
    }

    /////////////////////////////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////////////////////////////

    /**
     * add the egde to the CFA
     *
     * @param edge CFA edge
     */
    private void addToCFA(CFAEdge edge) {
      // avoid the assertion during reversing the CFAs
      CFACreationUtils.addEdgeUnconditionallyToCFA(edge);
    }

    /**
     * get the nondet function name for a type
     *
     * @param type function 's return type
     * @return function name
     */
    private String getNonDetName(CType type) {
      String prefix = "__VERIFIER_nondet_";
      CType realType = getRealType(type);
      if (realType instanceof CSimpleType simpleType) {
        CBasicType basicType = simpleType.getType();

        String typeName = basicType.toASTString();
        if (simpleType.isUnsigned()) {
          typeName = "u" + typeName;
        }
        return prefix + typeName;
      } else {
        throw new AssertionError("There is no non det function for " + type);
      }
    }

    /**
     * create a nondet function call for a type
     *
     * @param type function call's return type
     * @return the function call
     */
    private CFunctionCallExpression createNoDetCallExpr(CType type) {
      String funcName = getNonDetName(type);
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

    /**
     * Append an ndet assignment edge
     *
     * @param lhs expression at the left side
     * @param curr curr CFA node
     * @return the next CFA node
     */
    private CFANode appendNonDetAssignEdge(CLeftHandSide lhs, CFANode curr) {
      CType realType = getRealType(lhs.getExpressionType());
      if (realType instanceof CCompositeType compositeType) {
        List<CCompositeTypeMemberDeclaration> members = compositeType.getMembers();
        for (CCompositeTypeMemberDeclaration member : members) {
          CFieldReference fieldReference =
              new CFieldReference(
                  FileLocation.DUMMY, member.getType(), member.getName(), lhs, false);
          curr = appendNonDetAssignEdge(fieldReference, curr);
        }
        return curr;
      }

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
     * Append an assignment edge
     *
     * @param lExpr expression at the left side
     * @param rExpr expression at the right side
     * @param curr current CFA node
     * @return the next CFA node
     */
    private CFANode appendAssignEdge(CLeftHandSide lExpr, CExpression rExpr, CFANode curr) {

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

    private CBinaryExpression createBinaryExpr(
        CExpression lExpr, CExpression rExpr, BinaryOperator op) {
      CBinaryExpressionBuilder builder = new CBinaryExpressionBuilder(MachineModel.LINUX64, pLog);
      return builder.buildBinaryExpressionUnchecked(lExpr, rExpr, op);
    }

    private CType getRealType(CType type) {
      CType realType = type;
      if (type instanceof CTypedefType) {
        realType = ((CTypedefType) type).getRealType();
      }
      if (realType instanceof CElaboratedType) {
        realType = ((CElaboratedType) realType).getRealType();
      }
      return realType;
    }

    /**
     * Append an Assume edge
     *
     * @param lExpr expression at the left side
     * @param rExpr expression at the right side
     * @param curr the current CFA node
     * @param assume the assumption towards the expression
     * @return the new CFA node
     */
    private CFANode appendAssumeEdge(
        CExpression lExpr, CExpression rExpr, CFANode curr, boolean assume) {

      CType type = lExpr.getExpressionType();

      if (type instanceof CArrayType) {

        if (((CArrayType) type).getLengthAsInt().isEmpty()) {
          CExpression length = ((CArrayType) type).getLength();

          String indexName = curr.getFunctionName() + "::" + lExpr + "__index";
          assert length.getExpressionType().equals(intType);
          CVariableDeclaration index =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  false,
                  CStorageClass.AUTO,
                  length.getExpressionType(),
                  indexName,
                  indexName,
                  indexName,
                  null);
          CIdExpression indexExpr = new CIdExpression(FileLocation.DUMMY, index);
          curr = appendAssignEdge(indexExpr, length, curr);
          CFANode loopHead = curr;
          CFANode loopTail = new CFANode(loopHead.getFunction());
          nodes.put(loopTail.getFunctionName(), loopTail);
          curr.setLoopStart();
          // i >= 0
          CExpression contCondtion =
              createBinaryExpr(
                  indexExpr, CIntegerLiteralExpression.ZERO, BinaryOperator.GREATER_EQUAL);
          CFANode next = new CFANode(curr.getFunction());
          nodes.put(next.getFunctionName(), next);
          CAssumeEdge contEdge =
              new CAssumeEdge(
                  "", FileLocation.DUMMY, loopHead, next, contCondtion, true, false, false);
          addToCFA(contEdge);
          curr = next;
          CAssumeEdge breakEdge =
              new CAssumeEdge(
                  "", FileLocation.DUMMY, loopHead, loopTail, contCondtion, false, false, false);
          addToCFA(breakEdge);

          CType elemType = ((CArrayType) type).getType();
          CExpression lElem =
              new CArraySubscriptExpression(FileLocation.DUMMY, elemType, lExpr, indexExpr);
          CExpression rElem =
              new CArraySubscriptExpression(FileLocation.DUMMY, elemType, rExpr, indexExpr);
          curr = appendAssumeEdge(lElem, rElem, curr, true);
          // tmp = i;
          String tmpName = indexName + "__TMP";
          CVariableDeclaration tmp =
              new CVariableDeclaration(
                  FileLocation.DUMMY,
                  false,
                  CStorageClass.AUTO,
                  intType,
                  tmpName,
                  tmpName,
                  tmpName,
                  null);
          CIdExpression tmpExpr = new CIdExpression(FileLocation.DUMMY, tmp);
          curr = appendAssignEdge(tmpExpr, indexExpr, curr);
          // i = tmp - 1;
          CExpression rvalue =
              createBinaryExpr(tmpExpr, CIntegerLiteralExpression.ONE, BinaryOperator.MINUS);
          curr = appendAssignEdge(indexExpr, rvalue, curr);

          BlankEdge cont = new BlankEdge("", FileLocation.DUMMY, curr, loopHead, "continue;");
          addToCFA(cont);
          return loopTail;
        }

        int length = ((CArrayType) type).getLengthAsInt().orElseThrow();
        CType elemType = ((CArrayType) type).getType();
        for (int i = length; i >= 0; i--) {
          CIntegerLiteralExpression subscriptExpr =
              new CIntegerLiteralExpression(FileLocation.DUMMY, intType, BigInteger.valueOf(i));
          CExpression lElem =
              new CArraySubscriptExpression(FileLocation.DUMMY, elemType, lExpr, subscriptExpr);
          CExpression rElem =
              new CArraySubscriptExpression(FileLocation.DUMMY, elemType, rExpr, subscriptExpr);
          curr = appendAssumeEdge(lElem, rElem, curr, true);
        }
        return curr;
      }

      CType realType = getRealType(type);

      if (realType instanceof CCompositeType) {
        List<CCompositeTypeMemberDeclaration> members = ((CCompositeType) realType).getMembers();
        for (CCompositeTypeMemberDeclaration member : members) {
          CFieldReference lMember =
              new CFieldReference(
                  FileLocation.DUMMY, member.getType(), member.getName(), lExpr, false);
          CFieldReference rMember =
              new CFieldReference(
                  FileLocation.DUMMY, member.getType(), member.getName(), rExpr, false);
          curr = appendAssumeEdge(lMember, rMember, curr, true);
        }
        return curr;
      }

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

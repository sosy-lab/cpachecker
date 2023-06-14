// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.TreeMultimap;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFALabelNode;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CCfaEdgeVisitor;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
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
    private final EdgeReverseVistor edgeReverseVistor;
    private final Deque<CFANode> locstack;
    private final NavigableMap<String, FunctionEntryNode> functions;
    private final TreeMultimap<String, CFANode> nodes;
    private final HashMap<CFunctionDeclaration, CFunctionDeclaration> funcDeclMap;
    private final HashMap<CFANode, CFANode> nodeMap;

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
      this.edgeReverseVistor = new EdgeReverseVistor();
      this.locstack = new ArrayDeque<>();
      this.functions = new TreeMap<>();
      this.nodes = TreeMultimap.create();
      this.funcDeclMap = new HashMap<>();
      this.nodeMap = new HashMap<>();
    }

    private CFA createCfa() {

      // create a dummy main
      CFunctionEntryNode dummyReverseMainEntry = newDummyMain();

      // Reverse each Function's CFA
      for (Map.Entry<String, FunctionEntryNode> function : pCfa.getAllFunctions().entrySet()) {
        String name = function.getKey();
        FunctionEntryNode entryNode = function.getValue();
        FunctionEntryNode reverseEntryNode = reverseFunction(entryNode);
        functions.put(name, reverseEntryNode);
      }

      // Search for the target in the original CFA
      ImmutableSet<CFANode> targets =
          targetFinder.tryGetAutomatonTargetLocations(pCfa.getMainFunction(), pSpec);

      // Connect main entry to each target
      for (CFANode oldtarget : targets) {
        CFANode newtarget = nodeMap.get(oldtarget);
        BlankEdge dummyEdge =
            new BlankEdge(
                "", FileLocation.DUMMY, dummyReverseMainEntry, newtarget, "Target Dummy Edge");
        addToCFA(dummyEdge);
      }

      return new MutableCFA(
          functions, nodes, pCfa.getMetadata().withMainFunctionEntry(dummyReverseMainEntry));
    }

    // Reverse a single function's CFA.
    private FunctionEntryNode reverseFunction(FunctionEntryNode oldEntryNode) {
      String funcName = oldEntryNode.getFunctionName();

      CFunctionDeclaration oldDecl = (CFunctionDeclaration) oldEntryNode.getFunctionDefinition();
      CFunctionDeclaration newDecl = newDeclaration(oldDecl);

      FunctionExitNode oldExitNode = oldEntryNode.getExitNode().get();

      FunctionEntryNode newEntry =
          new CFunctionEntryNode(FileLocation.DUMMY, newDecl, null, Optional.empty());

      nodeMap.put(oldExitNode, newEntry);
      nodes.put(funcName, newEntry);
      funcDeclMap.put(oldDecl, newDecl);

      // BFS the old CFA, starting with the target.
      Set<CFANode> visited = new HashSet<>();
      Deque<CFANode> waitList = new ArrayDeque<>();
      waitList.add(oldExitNode);
      visited.add(oldExitNode);
      pLog.log(Level.INFO, "TRACE:");

      while (!waitList.isEmpty()) {
        pLog.log(Level.INFO, "//======================================================");
        CFANode oldhead = waitList.remove();
        CFANode newhead = nodeMap.get(oldhead);

        if (oldhead instanceof CFunctionEntryNode) {
          ((FunctionExitNode) newhead).setEntryNode(newEntry);
          break;
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

        for (CFAEdge oldEdge : CFAUtils.allEnteringEdges(oldhead)) {
          CFANode oldNext = oldEdge.getPredecessor(); // forward edge

          if (oldNext instanceof CFunctionEntryNode
              && oldEntryNode.equals(pCfa.getMainFunction())) {

            CFALabelNode label = new CFALabelNode(newDecl, "ERROR");
            nodes.put(funcName, label);
            BlankEdge newedge1 = new BlankEdge("", FileLocation.DUMMY, newhead, label, "");
            addToCFA(newedge1);
            CFANode newNext = newNode(oldNext);
            BlankEdge newedge2 = new BlankEdge("", FileLocation.DUMMY, label, newNext, "");
            addToCFA(newedge2);
            nodeMap.put(oldNext, newNext);
            nodes.put(funcName, newNext);
          } else {

            locstack.push(newhead);

            CFAEdge newedge = ((CCfaEdge) oldEdge).accept(edgeReverseVistor);
            pLog.log(Level.INFO, "OLD EDGE: " + oldEdge.toString() + " " + oldEdge.getEdgeType());
            pLog.log(Level.INFO, "NEW EDGE: " + newedge.toString() + " " + newedge.getEdgeType());

            CFANode newNext = newedge.getSuccessor(); // reverse edge

            nodes.put(funcName, newNext);

            nodeMap.put(oldNext, newNext);
            pLog.log(Level.INFO, "oldNext: " + oldNext.toString() + oldNext.getClass());
            pLog.log(Level.INFO, "newNext: " + newNext.toString() + newNext.getClass());
            addToCFA(newedge);
          }

          if (visited.add(oldNext)) {
            waitList.add(oldNext);
          }
        }
      }

      return newEntry;
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
    // Edge Visitor
    /////////////////////////////////////////////////////////////////////////////
    private final class EdgeReverseVistor implements CCfaEdgeVisitor<CFAEdge, NoException> {

      @Override
      public CFAEdge visit(BlankEdge pBlankEdge) throws NoException {
        CFANode from = locstack.pop();
        CFANode to = newNode(pBlankEdge.getPredecessor());
        return new BlankEdge("", FileLocation.DUMMY, from, to, pBlankEdge.getDescription());
      }

      @Override
      public CFAEdge visit(CStatementEdge pCStatementEdge) throws NoException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CFAEdge visit(CAssumeEdge pCAssumeEdge) throws NoException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CFAEdge visit(CDeclarationEdge pCDeclarationEdge) throws NoException {
        // TODO Auto-generated method stub
        CFANode from = locstack.pop();
        CFANode to = newNode(pCDeclarationEdge.getPredecessor());
        CDeclaration decl = pCDeclarationEdge.getDeclaration();
        return new CDeclarationEdge("", FileLocation.DUMMY, from, to, decl);
      }

      @Override
      public CFAEdge visit(CFunctionCallEdge pCFunctionCallEdge) throws NoException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CFAEdge visit(CFunctionReturnEdge pCFunctionReturnEdge) throws NoException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CFAEdge visit(CFunctionSummaryEdge pCFunctionSummaryEdge) throws NoException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CFAEdge visit(CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge)
          throws NoException {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public CFAEdge visit(CReturnStatementEdge pCReturnStatementEdge) throws NoException {
        // TODO Auto-generated method stub
        return null;
      }
    }

    /////////////////////////////////////////////////////////////////////////////
    // AST Visitor
    /////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////////////////////////////////////

    /** Add edge to the leaving and entering edges of its predecessor and successor. */
    private void addToCFA(CFAEdge edge) {
      CFACreationUtils.addEdgeToCFA(edge, pLog, false);
    }
  }
}

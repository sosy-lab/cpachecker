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
    private final Configuration pConfig;
    private final Specification pSpec;
    private final LogManager pLog;
    private final TargetLocationProvider targetFinder;
    private final EdgeReverseVistor edgeReverseVistor;
    private final Deque<CFANode> locstack;
    private final NavigableMap<String, FunctionEntryNode> functions;
    private final TreeMultimap<String, CFANode> nodes;
    private final HashMap<CFunctionDeclaration, CFunctionDeclaration> funcDeclMap;

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
      this.edgeReverseVistor = new EdgeReverseVistor();
      this.locstack = new ArrayDeque<>();
      this.functions = new TreeMap<>();
      this.nodes = TreeMultimap.create();
      this.funcDeclMap = new HashMap<>();
    }

    private CFA createCfa() {

      FunctionEntryNode dummyMain = null;
      /* Reverse each Function's CFA */
      for (Map.Entry<String, FunctionEntryNode> function : pCfa.getAllFunctions().entrySet()) {
        String name = function.getKey();
        FunctionEntryNode entryNode = function.getValue();
        FunctionEntryNode reverseEntryNode = reverseFunction(entryNode);
        dummyMain = reverseEntryNode;
        functions.put(name, reverseEntryNode);
      }
      /* second pass */

      // FIXME : metadata for new CFA
      return new MutableCFA(functions, nodes, pCfa.getMetadata().withMainFunctionEntry(dummyMain));
    }

    /** Reverse a single function's CFA. */
    private FunctionEntryNode reverseFunction(FunctionEntryNode originalEntryNode) {
      String funcName = originalEntryNode.getFunctionName();
      CFunctionDeclaration oldDecl =
          (CFunctionDeclaration) originalEntryNode.getFunctionDefinition();
      // pLog.log(Level.INFO, "oldfdef: " +  oldfdef.toString());
      CFunctionDeclaration newDecl = newDeclaration(oldDecl);
      // pLog.log(Level.INFO, "oldfdef: " +  oldfdef.toString());
      FunctionExitNode dummyExit = new FunctionExitNode(newDecl);
      // pLog.log(Level.INFO, "dummyExit: " +  dummyExit.toString());
      FunctionEntryNode dummyEntry =
          new CFunctionEntryNode(FileLocation.DUMMY, newDecl, dummyExit, Optional.empty());
      // pLog.log(Level.INFO, "dummyEntry: " +  dummyEntry.toString());
      nodes.put(funcName, dummyEntry);
      nodes.put(funcName, dummyExit);

      funcDeclMap.put(oldDecl, newDecl);

      ImmutableSet<CFANode> targets =
          targetFinder.tryGetAutomatonTargetLocations(originalEntryNode, pSpec);
      // Find if there is any target in this CFA, and connect them to the dummy entry.
      for (CFANode oldtarget : targets) {
        pLog.log(Level.INFO, "TARGET: " + oldtarget.toString());
        CFANode newtarget = newNode(oldtarget);
        BlankEdge dummyEdge =
            new BlankEdge("", FileLocation.DUMMY, dummyEntry, newtarget, "Target Dummy Edge");
        addToCFA(dummyEdge);
        reverseEachTarget(funcName, oldtarget, newtarget);
      }

      return dummyEntry;
    }

    /** For each new "entry", create a path which connect it with the original entry node */
    private void reverseEachTarget(String funcName, CFANode oldtarget, CFANode newtarget) {
      // BFS the old CFA, starting with the target.
      Set<CFANode> visited = new HashSet<>();
      Deque<CFANode> waitList = new ArrayDeque<>();
      TreeMap<CFANode, CFANode> nodeMap = new TreeMap<>();
      waitList.add(oldtarget);
      visited.add(oldtarget);
      nodeMap.put(oldtarget, newtarget);
      pLog.log(Level.INFO, "TRACE:");
      while (!waitList.isEmpty()) {

        CFANode oldhead = waitList.remove();
        pLog.log(Level.INFO, oldhead.toString());
        if (oldhead instanceof FunctionEntryNode) {
          continue;
        }
        CFANode newhead = nodeMap.get(oldhead);
        nodes.put(funcName, newhead);

        for (CFAEdge oldEdge : CFAUtils.allEnteringEdges(oldhead)) {
          pLog.log(Level.INFO, oldEdge.toString() + " " + oldEdge.getEdgeType());
          locstack.push(newhead);
          CFAEdge newedge = ((CCfaEdge) oldEdge).accept(edgeReverseVistor);

          CFANode oldNext = oldEdge.getPredecessor(); // forward edge
          CFANode newNext = newedge.getSuccessor(); // reverse edge

          nodeMap.put(oldNext, newNext);
          addToCFA(newedge);

          if (visited.add(oldNext)) {
            waitList.add(oldNext);
          }
        }
      }
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
        String labelName = ((CFALabelNode) node).getLabel();
        AFunctionDeclaration funcDecl = funcDeclMap.get(((CFALabelNode) node).getFunction());
        return new CFALabelNode(funcDecl, labelName);
      } else if (node instanceof FunctionEntryNode) {
        AFunctionDeclaration funcDecl = funcDeclMap.get(node.getFunction());
        return new CFANode(funcDecl);
      } else if (node instanceof FunctionExitNode) {
        throw new IllegalStateException(
            "FunctionExitNode should not be processed now." + node.toString());
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
    // Helper methods
    /////////////////////////////////////////////////////////////////////////////

    /** Add edge to the leaving and entering edges of its predecessor and successor. */
    private void addToCFA(CFAEdge edge) {
      CFACreationUtils.addEdgeToCFA(edge, pLog, false);
    }
  }
}

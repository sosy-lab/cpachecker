// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

final class SliceToCfaConverter {

  private final Slice slice;

  private final Map<AFunctionDeclaration, CDeclarationEdge> declarationEdges;
  private final Map<AFunctionDeclaration, FunctionEntryNode> entryNodes;
  private final Map<CFAEdge, Set<MemoryLocation>> relevantMemoryLocations;

  private SliceToCfaConverter(Slice pSlice) {

    slice = pSlice;

    declarationEdges = new HashMap<>();
    entryNodes = new HashMap<>();
    relevantMemoryLocations = new HashMap<>();
  }

  public static CFA convert(Slice pSlice) {
    return new SliceToCfaConverter(pSlice).run();
  }

  /**
   * Returns true if the node hast at least one leaving edge and only leaving edges that are assume
   * edges and not contained in pRelevantEdges.
   */
  private boolean skipNextAssumeBranching(CFANode pNode, Set<CFAEdge> pRelevantEdges) {

    for (CFAEdge succEdge : CFAUtils.leavingEdges(pNode)) {
      if (succEdge.getEdgeType() != CFAEdgeType.AssumeEdge || pRelevantEdges.contains(succEdge)) {
        return false;
      }
    }

    return pNode.getNumLeavingEdges() > 0;
  }

  private <T> List<T> filterByRelevantParams(Iterable<T> pInput, AFunctionDeclaration pFunction) {

    CFunctionDeclaration functionDeclaration = (CFunctionDeclaration) pFunction;
    CDeclarationEdge declarationEdge = declarationEdges.get(pFunction);

    List<T> result = new ArrayList<>();
    Iterator<T> iterator = pInput.iterator();

    for (CParameterDeclaration parameter : functionDeclaration.getParameters()) {

      assert iterator.hasNext() : "pInput must have one element for every parameter";
      T element = iterator.next();

      MemoryLocation memoryLocation = MemoryLocation.valueOf(parameter.getQualifiedName());

      if (relevantMemoryLocations.get(declarationEdge).contains(memoryLocation)) {
        result.add(element);
      }
    }

    assert !iterator.hasNext() : "pInput must have one element for every parameter";

    return result;
  }

  private CFunctionDeclaration cloneFunctionDeclaration(AFunctionDeclaration pFunction) {

    CFunctionDeclaration originalFunctionDeclaration = (CFunctionDeclaration) pFunction;
    CDeclarationEdge originalDeclarationEdge = declarationEdges.get(pFunction);
    FunctionEntryNode originalFunctionEntryNode = entryNodes.get(pFunction);

    CFunctionType originalFunctionType = originalFunctionDeclaration.getType();
    List<CParameterDeclaration> relevantParameters =
        filterByRelevantParams(originalFunctionDeclaration.getParameters(), pFunction);

    CType relevantReturnType = originalFunctionType.getReturnType();
    Optional<? extends AVariableDeclaration> optRetVar =
        originalFunctionEntryNode.getReturnVariable();

    if (optRetVar.isPresent()) {
      MemoryLocation memoryLocation = MemoryLocation.valueOf(optRetVar.get().getQualifiedName());
      if (!relevantMemoryLocations.get(originalDeclarationEdge).contains(memoryLocation)) {
        relevantReturnType = CVoidType.VOID;
      }
    }

    CFunctionType relevantFunctionType =
        new CFunctionTypeWithNames(
            relevantReturnType, relevantParameters, originalFunctionType.takesVarArgs());

    return new CFunctionDeclaration(
        originalDeclarationEdge.getFileLocation(),
        relevantFunctionType,
        originalFunctionDeclaration.getQualifiedName(),
        relevantParameters);
  }

  /**
   * Returns a node with content copied from the specified pNode.
   *
   * <p>If pNodeMap already contains a node for the specified pNode (only one clone per node
   * number), the cached node from pNodeMap is returned; otherwise a new node with content copied
   * from specified pNode is created, stored in pNodeMap, and returned.
   */
  private CFANode cloneNode(CFANode pNode, Map<Integer, CFANode> pNodeMap) {

    CFANode newNode = pNodeMap.get(pNode.getNodeNumber());
    if (newNode != null) {
      return newNode;
    }

    CFunctionDeclaration functionDeclaration = (CFunctionDeclaration) pNode.getFunction();
    CDeclarationEdge declarationEdge = declarationEdges.get(functionDeclaration);

    if (declarationEdge != null) {
      functionDeclaration = cloneFunctionDeclaration(functionDeclaration);
    }

    if (pNode instanceof CLabelNode) {

      CLabelNode labelNode = (CLabelNode) pNode;
      newNode = new CLabelNode(functionDeclaration, labelNode.getLabel());

    } else if (pNode instanceof CFATerminationNode) {

      newNode = new CFATerminationNode(functionDeclaration);

    } else if (pNode instanceof FunctionExitNode) {

      newNode = new FunctionExitNode(functionDeclaration);

    } else if (pNode instanceof CFunctionEntryNode) {

      CFunctionEntryNode originalFunctionEntryNode = (CFunctionEntryNode) pNode;

      FunctionExitNode relevantFunctionExitNode =
          (FunctionExitNode) cloneNode(originalFunctionEntryNode.getExitNode(), pNodeMap);

      Optional<CVariableDeclaration> relevantReturnVariable;

      if (functionDeclaration.getType().getReturnType().equals(CVoidType.VOID)) {
        relevantReturnVariable = Optional.absent();
      } else {
        relevantReturnVariable = originalFunctionEntryNode.getReturnVariable();
      }

      newNode =
          new CFunctionEntryNode(
              originalFunctionEntryNode.getFileLocation(),
              functionDeclaration,
              relevantFunctionExitNode,
              relevantReturnVariable);

      relevantFunctionExitNode.setEntryNode((CFunctionEntryNode) newNode);

    } else {

      newNode = new CFANode(functionDeclaration);
    }

    newNode.setReversePostorderId(pNode.getReversePostorderId());

    if (pNode.isLoopStart()) {
      newNode.setLoopStart();
    }

    pNodeMap.put(pNode.getNodeNumber(), newNode);

    return newNode;
  }

  /**
   * Creates a new CFunctionCallEdge with the following connections:
   *
   * <p><code>{@code
   *                                                           (dummy nodes)
   * [pPredecessor] --- CFunctionCallEdge ---> new CFunctionEntryNode(newFunctionExitNode())
   *        |                    |
   *        ----------- CFunctionSummaryEdge ---> [pSuccessor]
   * }</code>
   */
  private CFunctionCallEdge cloneFunctionCall(
      CFunctionCallEdge pEdge, CFANode pPredecessor, CFANode pSuccessor) {

    CFunctionSummaryEdge originalFunctionSummaryEdge = pEdge.getSummaryEdge();
    CFunctionEntryNode originalFunctionEntryNode = pEdge.getSuccessor();

    CFunctionReturnEdge originalFunctionReturnEdge = null;
    for (CFAEdge edge : CFAUtils.enteringEdges(originalFunctionSummaryEdge.getSuccessor())) {
      if (edge instanceof CFunctionReturnEdge) {
        originalFunctionReturnEdge = (CFunctionReturnEdge) edge;
      }
    }

    CFunctionDeclaration relevantFunctionDeclaration =
        cloneFunctionDeclaration(originalFunctionEntryNode.getFunctionDefinition());
    Optional<CVariableDeclaration> optionalReturnVariable =
        originalFunctionEntryNode.getReturnVariable();

    if (relevantFunctionDeclaration.getType().getReturnType().equals(CVoidType.VOID)) {
      optionalReturnVariable = Optional.absent();
    }

    FunctionExitNode relevantFunctionExitNode = new FunctionExitNode(relevantFunctionDeclaration);
    CFunctionEntryNode relevantFunctionEntryNode =
        new CFunctionEntryNode(
            pEdge.getFileLocation(),
            relevantFunctionDeclaration,
            relevantFunctionExitNode,
            optionalReturnVariable);

    CFunctionCall originalFunctionCall = originalFunctionSummaryEdge.getExpression();
    CFunctionCallExpression originalFunctionCallExpression =
        originalFunctionCall.getFunctionCallExpression();
    List<CExpression> relevantParameterExpressions =
        filterByRelevantParams(
            originalFunctionCallExpression.getParameterExpressions(),
            originalFunctionEntryNode.getFunctionDefinition());

    CFunctionCallExpression relevantFunctionCallExpression =
        new CFunctionCallExpression(
            pEdge.getFileLocation(),
            relevantFunctionDeclaration.getType().getReturnType(),
            originalFunctionCallExpression.getFunctionNameExpression(),
            relevantParameterExpressions,
            relevantFunctionDeclaration);

    CFunctionCall relevantFunctionCall;

    if (originalFunctionCall instanceof CFunctionCallStatement) {

      CFunctionCallStatement originalStatement = (CFunctionCallStatement) originalFunctionCall;
      relevantFunctionCall =
          new CFunctionCallStatement(
              originalStatement.getFileLocation(), relevantFunctionCallExpression);

    } else if (originalFunctionCall instanceof CFunctionCallAssignmentStatement) {

      CFunctionCallAssignmentStatement originalStatement =
          (CFunctionCallAssignmentStatement) originalFunctionCall;

      if (optionalReturnVariable.isPresent()) {

        String returnVariableName = optionalReturnVariable.get().getQualifiedName();
        Set<MemoryLocation> memoryLocations =
            relevantMemoryLocations.get(originalFunctionReturnEdge);

        if (memoryLocations != null
            && memoryLocations.contains(MemoryLocation.valueOf(returnVariableName))) {

          relevantFunctionCall =
              new CFunctionCallAssignmentStatement(
                  originalStatement.getFileLocation(),
                  originalStatement.getLeftHandSide(),
                  relevantFunctionCallExpression);
        } else {

          relevantFunctionCall =
              new CFunctionCallStatement(
                  originalStatement.getFileLocation(), relevantFunctionCallExpression);
        }

      } else {

        relevantFunctionCall =
            new CFunctionCallStatement(
                originalStatement.getFileLocation(), relevantFunctionCallExpression);
      }

    } else {
      throw new AssertionError("Unknown function call type: " + originalFunctionCall);
    }

    CFunctionSummaryEdge relevantFunctionSummaryEdge =
        new CFunctionSummaryEdge(
            originalFunctionSummaryEdge.getRawStatement(),
            originalFunctionSummaryEdge.getFileLocation(),
            pPredecessor,
            pSuccessor,
            relevantFunctionCall,
            relevantFunctionEntryNode);

    pPredecessor.addLeavingSummaryEdge(relevantFunctionSummaryEdge);
    pSuccessor.addEnteringSummaryEdge(relevantFunctionSummaryEdge);

    return new CFunctionCallEdge(
        pEdge.getRawStatement(),
        pEdge.getFileLocation(),
        pPredecessor,
        relevantFunctionEntryNode,
        relevantFunctionCall,
        relevantFunctionSummaryEdge);
  }

  /**
   * Returns a new edge with content copied from specified pEdge and with specified predecessor and
   * successor.
   *
   * <p>Treatment of CFunctionCallEdges is special and handled in {@link #cloneFunctionCall}.
   */
  private CFAEdge cloneEdge(CFAEdge pEdge, CFANode pPredecessor, CFANode pSuccessor) {

    CFAEdgeType type = pEdge.getEdgeType();
    String rawStatement = pEdge.getRawStatement();
    FileLocation fileLocation = pEdge.getFileLocation();
    String description = pEdge.getDescription();

    if (type == CFAEdgeType.BlankEdge) {

      return new BlankEdge(rawStatement, fileLocation, pPredecessor, pSuccessor, description);

    } else if (type == CFAEdgeType.AssumeEdge && pEdge instanceof CAssumeEdge) {

      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      return new CAssumeEdge(
          rawStatement,
          fileLocation,
          pPredecessor,
          pSuccessor,
          assumeEdge.getRawAST().get(),
          assumeEdge.getTruthAssumption(),
          assumeEdge.isSwapped(),
          assumeEdge.isArtificialIntermediate());

    } else if (pEdge instanceof CFunctionSummaryStatementEdge) {

      CFunctionSummaryStatementEdge statementEdge = (CFunctionSummaryStatementEdge) pEdge;
      return new CFunctionSummaryStatementEdge(
          rawStatement,
          statementEdge.getStatement(),
          fileLocation,
          pPredecessor,
          pSuccessor,
          statementEdge.getFunctionCall(),
          statementEdge.getFunctionName());

    } else if (type == CFAEdgeType.StatementEdge && pEdge instanceof CStatementEdge) {

      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      return new CStatementEdge(
          rawStatement, statementEdge.getStatement(), fileLocation, pPredecessor, pSuccessor);

    } else if (type == CFAEdgeType.DeclarationEdge && pEdge instanceof CDeclarationEdge) {

      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      CDeclaration originalDeclaration = declarationEdge.getDeclaration();
      CDeclaration relevantDeclaration = originalDeclaration;

      if (originalDeclaration instanceof CFunctionDeclaration) {
        relevantDeclaration = cloneFunctionDeclaration((CFunctionDeclaration) originalDeclaration);
      }

      return new CDeclarationEdge(
          rawStatement, fileLocation, pPredecessor, pSuccessor, relevantDeclaration);

    } else if (type == CFAEdgeType.ReturnStatementEdge && pEdge instanceof CReturnStatementEdge) {

      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pEdge;
      return new CReturnStatementEdge(
          rawStatement,
          returnStatementEdge.getRawAST().get(),
          fileLocation,
          pPredecessor,
          (FunctionExitNode) pSuccessor);

    } else if (type == CFAEdgeType.FunctionCallEdge && pEdge instanceof CFunctionCallEdge) {

      return cloneFunctionCall((CFunctionCallEdge) pEdge, pPredecessor, pSuccessor);

    } else {
      throw new AssertionError(
          String.format(
              "Cannot clone edge: type=%s, class=%s", type.toString(), pEdge.getClass().getName()));
    }
  }

  /**
   * Creates a new function that resembles the sliced function (as specified by pRelevantEdges) as
   * closely as possible.
   *
   * <p>Nodes of the created function are added to pNewNodes.
   *
   * @param pEntryNode the entry node for the function.
   * @param pNewNodes the mapping of function names to function nodes (multimap).
   * @return the function entry node of the created function.
   */
  private FunctionEntryNode createRelevantFunction(
      CFunctionEntryNode pEntryNode, Multimap<String, CFANode> pNewNodes) {

    ImmutableSet<CFAEdge> relevantEdges = slice.getRelevantEdges();

    Map<Integer, CFANode> nodeMap =
        new HashMap<>(); // mapping of old-node-number --> new-node-clone
    Queue<CFAEdge> waitlist = new ArrayDeque<>(); // edges to be processed (cloned or replaced)
    Set<CFANode> visited =
        new HashSet<>(); // a node is visited when all its leaving edges were added to the waitlist

    FunctionEntryNode newEntryNode = (FunctionEntryNode) cloneNode(pEntryNode, nodeMap);
    CFAUtils.leavingEdges(pEntryNode).copyInto(waitlist);
    visited.add(pEntryNode);

    while (!waitlist.isEmpty()) {

      CFAEdge edge = waitlist.remove();
      CFANode pred = edge.getPredecessor();
      CFANode succ = edge.getSuccessor();

      CFAEdge newEdge;
      CFANode newPred = cloneNode(pred, nodeMap);
      CFANode newSucc;

      // step over function
      if (edge instanceof CFunctionCallEdge) {
        succ = ((CFunctionCallEdge) edge).getSummaryEdge().getSuccessor();
      }

      newSucc = cloneNode(succ, nodeMap);

      if (relevantEdges.contains(edge)
          || edge.getEdgeType() == CFAEdgeType.BlankEdge
          || edge.getEdgeType() == CFAEdgeType.AssumeEdge
          || edge instanceof CFunctionSummaryStatementEdge) {

        newEdge = cloneEdge(edge, newPred, newSucc);

      } else {
        newEdge = new BlankEdge("", edge.getFileLocation(), newPred, newSucc, "slice-irrelevant");
      }

      newPred.addLeavingEdge(newEdge);

      // newSucc is only the successor of all non-function-call edges. function call handling
      // takes care of special node-edge relations during node creation
      if (!(newEdge instanceof CFunctionCallEdge)) {
        newSucc.addEnteringEdge(newEdge);
      }

      // don't visit a node twice and stay in function
      if (!visited.contains(succ)
          && !(succ instanceof FunctionExitNode)
          && !(succ instanceof FunctionEntryNode)) {
        // If all leaving edges of the current successors
        // are irrelevant assume edges, one branch is chosen (arbitrarily)
        // and used as the only leaving branch.
        // This makes it possible to replace all of the assume edges by a single blank edge.
        // WARNING: It is assumed that there are no relevant edges in all the other branches.
        //          Edges of those branches (even relevant edges) will not be cloned!
        //          This code *must* be updated if any slicing method is used that creates
        //          irrelevant branching conditions, but at the same time allows nested relevant
        //          statements in those branches.
        if (skipNextAssumeBranching(succ, relevantEdges)) {

          CFAEdge assumeEdge = succ.getLeavingEdge(0);
          waitlist.add(
              new BlankEdge(
                  "",
                  assumeEdge.getFileLocation(),
                  succ,
                  assumeEdge.getSuccessor(),
                  "slice-irrelevant"));

        } else {
          CFAUtils.leavingEdges(succ).copyInto(waitlist);
        }

        visited.add(succ);
      }
    }

    pNewNodes.putAll(pEntryNode.getFunctionName(), nodeMap.values());

    return newEntryNode;
  }

  private void initRelevantMemoryLocations() {

    Map<CDeclarationEdge, Set<MemoryLocation>> originalMemoryLocations = new HashMap<>();

    // populate originalMemoryLocations with the original memory locations for function declarations
    // i.e. with memory locations for parameters and return values
    for (FunctionEntryNode entryNode : entryNodes.values()) {

      CDeclarationEdge declarationEdge = declarationEdges.get(entryNode.getFunctionDefinition());

      if (declarationEdge != null) {

        Set<MemoryLocation> memoryLocations =
            originalMemoryLocations.computeIfAbsent(declarationEdge, key -> new HashSet<>());

        for (AParameterDeclaration parameter : entryNode.getFunctionParameters()) {
          String qualifiedName = parameter.getQualifiedName();
          memoryLocations.add(MemoryLocation.valueOf(qualifiedName));
        }

        Optional<? extends AVariableDeclaration> optionalReturnVariable =
            entryNode.getReturnVariable();
        if (optionalReturnVariable.isPresent()) {
          String qualifiedName = optionalReturnVariable.get().getQualifiedName();
          memoryLocations.add(MemoryLocation.valueOf(qualifiedName));
        }
      }
    }

    // add relevant memory locations for:
    //   - function declaration edges
    //   - function call edges
    //   - function return edges
    // to relevantMemoryLocations
    for (CFAEdge edge : slice.getRelevantEdges()) {

      AFunctionDeclaration function = null;

      if (edge instanceof CFunctionCallEdge) {
        function = edge.getSuccessor().getFunction();
      } else if (edge instanceof CFunctionReturnEdge) {
        function = edge.getPredecessor().getFunction();
      }

      CDeclarationEdge declarationEdge = declarationEdges.get(function);

      if (declarationEdge != null) {

        Set<MemoryLocation> relevantDeclarationMemoryLocations =
            relevantMemoryLocations.computeIfAbsent(declarationEdge, key -> new HashSet<>());
        Set<MemoryLocation> relevantEdgeMemoryLocations =
            relevantMemoryLocations.computeIfAbsent(edge, key -> new HashSet<>());

        for (MemoryLocation memoryLocation : originalMemoryLocations.get(declarationEdge)) {
          if (slice.isRelevantDef(edge, memoryLocation)) {
            relevantDeclarationMemoryLocations.add(memoryLocation);
            relevantEdgeMemoryLocations.add(memoryLocation);
          }
        }
      }
    }
  }

  private CFA run() {

    final CFA originalCfa = slice.getOriginalCfa();
    final ImmutableSet<CFAEdge> relevantEdges = slice.getRelevantEdges();

    // init declarationEdges (AFunctionDeclaration -> CDeclarationEdge)
    for (CFAEdge edge : relevantEdges) {
      if (edge instanceof CDeclarationEdge) {
        CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        CDeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CFunctionDeclaration) {
          declarationEdges.put((CFunctionDeclaration) declaration, declarationEdge);
        }
      }
    }

    // init entryNodes (AFunctionDeclaration -> FunctionEntryNode)
    for (FunctionEntryNode entryNode : originalCfa.getAllFunctionHeads()) {
      AFunctionDeclaration function = entryNode.getFunction();
      CDeclarationEdge declarationEdge = declarationEdges.get(function);
      if (declarationEdge != null) {
        entryNodes.put(function, entryNode);
      }
    }

    // the main function is always required
    AFunctionDeclaration originalMainFunction = originalCfa.getMainFunction().getFunction();
    if (!entryNodes.containsKey(originalMainFunction)) {
      FunctionEntryNode entryNode = originalCfa.getMainFunction();
      entryNodes.put(entryNode.getFunction(), entryNode);
    }

    initRelevantMemoryLocations();

    NavigableMap<String, FunctionEntryNode> relevantFunctions = new TreeMap<>();
    TreeMultimap<String, CFANode> relevantNodes = TreeMultimap.create();
    FunctionEntryNode relevantMainEntryNode = null;
    for (Map.Entry<AFunctionDeclaration, FunctionEntryNode> entry : entryNodes.entrySet()) {
      AFunctionDeclaration originalFunction = entry.getKey();
      FunctionEntryNode originalEntryNode = entry.getValue();
      FunctionEntryNode relevantEntryNode =
          createRelevantFunction((CFunctionEntryNode) originalEntryNode, relevantNodes);
      relevantFunctions.put(originalFunction.getQualifiedName(), relevantEntryNode);
      if (originalFunction.equals(originalMainFunction)) {
        Preconditions.checkState(
            relevantMainEntryNode == null,
            "Trying to set entry node of main function, but one already exists: %s",
            relevantMainEntryNode);
        relevantMainEntryNode = relevantEntryNode;
      }
    }

    assert relevantMainEntryNode != null : "Entry node of main function is missing";

    return new MutableCFA(
        originalCfa.getMachineModel(),
        relevantFunctions,
        relevantNodes,
        relevantMainEntryNode,
        originalCfa.getFileNames(),
        originalCfa.getLanguage());
  }
}

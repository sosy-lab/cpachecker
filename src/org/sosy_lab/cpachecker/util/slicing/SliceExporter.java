// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.slicing;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
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
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.cwriter.CFAToCTranslator;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

@Options(prefix = "slicing")
public class SliceExporter {

  @Option(
      secure = true,
      name = "exportToC.enable",
      description = "Whether to export slices as C program files")
  private boolean exportToC = false;

  @Option(
      secure = true,
      name = "exportToC.file",
      description = "File template for exported C program slices")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportToCFile = PathTemplate.ofFormatString("programSlice.%d.c");

  @Option(
      secure = true,
      name = "exportCriteria.enable",
      description = "Export the used slicing criteria to file")
  private boolean exportCriteria = false;

  @Option(
      secure = true,
      name = "exportCriteria.file",
      description = "File template for export of used slicing criteria")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate exportCriteriaFile =
      PathTemplate.ofFormatString("programSlice.%d.criteria.txt");

  private final LogManager logger;
  private int exportCount = -1;
  private final CFAToCTranslator translator;

  public SliceExporter(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    translator = new CFAToCTranslator(pConfig);
    logger = pLogger;
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

  private <T> List<T> filterParams(
      Iterable<T> pInput, AFunctionDeclaration pFunction, FunctionManager pFunctionManager) {

    CFunctionDeclaration functionDeclaration = (CFunctionDeclaration) pFunction;
    CDeclarationEdge declarationEdge = pFunctionManager.getDeclarationEdge(pFunction);
    Slice slice = pFunctionManager.getSlice();

    List<T> result = new ArrayList<>();
    Iterator<T> iterator = pInput.iterator();

    for (CParameterDeclaration parameter : functionDeclaration.getParameters()) {

      assert iterator.hasNext() : "pInput must have one element for every parameter";
      T element = iterator.next();

      MemoryLocation memoryLocation = MemoryLocation.valueOf(parameter.getQualifiedName());

      if (slice.isRelevantDef(declarationEdge, memoryLocation)) {
        result.add(element);
      }
    }

    assert !iterator.hasNext() : "pInput must have one element for every parameter";

    return result;
  }

  private CFunctionDeclaration cloneFunctionDeclaration(
      AFunctionDeclaration pFunction, FunctionManager pFunctionManager) {

    CFunctionDeclaration originalFunctionDeclaration = (CFunctionDeclaration) pFunction;
    CDeclarationEdge originalDeclarationEdge = pFunctionManager.getDeclarationEdge(pFunction);
    FunctionEntryNode originalFunctionEntryNode = pFunctionManager.getEntryNode(pFunction);
    Slice slice = pFunctionManager.getSlice();

    CFunctionType originalFunctionType = originalFunctionDeclaration.getType();
    List<CParameterDeclaration> relevantParameters =
        filterParams(originalFunctionDeclaration.getParameters(), pFunction, pFunctionManager);

    CType relevantReturnType = originalFunctionType.getReturnType();
    Optional<? extends AVariableDeclaration> optRetVar =
        originalFunctionEntryNode.getReturnVariable().toJavaUtil();

    if (optRetVar.isPresent()) {
      MemoryLocation memoryLocation =
          MemoryLocation.valueOf(optRetVar.orElseThrow().getQualifiedName());
      if (!slice.isRelevantDef(originalDeclarationEdge, memoryLocation)) {
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
   * Returns a node with content copied from specified pNode.
   *
   * <p>If pNodeMap already contains a node for the specified pNode (only one clone per node
   * number), the cached node from pNodeMap is returned; otherwise a new node with content copied
   * from specified pNode is created, stored in pNodeMap, and returned.
   */
  private CFANode cloneNode(
      CFANode pNode, Map<Integer, CFANode> pNodeMap, FunctionManager pFunctionManager) {

    CFANode newNode = pNodeMap.get(pNode.getNodeNumber());
    if (newNode != null) {
      return newNode;
    }

    var functionDeclaration = (CFunctionDeclaration) pNode.getFunction();
    var declarationEdge = pFunctionManager.getDeclarationEdge(functionDeclaration);

    if (declarationEdge != null) {
      functionDeclaration = cloneFunctionDeclaration(functionDeclaration, pFunctionManager);
    }

    if (pNode instanceof CLabelNode) {

      CLabelNode labelNode = (CLabelNode) pNode;
      newNode = new CLabelNode(functionDeclaration, labelNode.getLabel());

    } else if (pNode instanceof CFATerminationNode) {

      newNode = new CFATerminationNode(functionDeclaration);

    } else if (pNode instanceof FunctionExitNode) {

      newNode = new FunctionExitNode(functionDeclaration);

    } else if (pNode instanceof CFunctionEntryNode) {

      var originalFunctionEntryNode = (CFunctionEntryNode) pNode;

      var relevantFunctionExitNode =
          (FunctionExitNode)
              cloneNode(originalFunctionEntryNode.getExitNode(), pNodeMap, pFunctionManager);

      if (functionDeclaration.getType().getReturnType().equals(CVoidType.VOID)) {
        newNode =
            new CFunctionEntryNode(
                originalFunctionEntryNode.getFileLocation(),
                functionDeclaration,
                relevantFunctionExitNode,
                com.google.common.base.Optional.absent());
      } else {
        newNode =
            new CFunctionEntryNode(
                originalFunctionEntryNode.getFileLocation(),
                functionDeclaration,
                relevantFunctionExitNode,
                originalFunctionEntryNode.getReturnVariable());
      }

      newNode =
          new CFunctionEntryNode(
              originalFunctionEntryNode.getFileLocation(),
              functionDeclaration,
              relevantFunctionExitNode,
              originalFunctionEntryNode.getReturnVariable());
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
      CFunctionCallEdge pEdge,
      CFANode pPredecessor,
      CFANode pSuccessor,
      FunctionManager pFunctionManager) {

    var originalFunctionSummaryEdge = pEdge.getSummaryEdge();
    var originalFunctionEntryNode = pEdge.getSuccessor();

    CFunctionReturnEdge originalFunctionReturnEdge = null;
    for (CFAEdge edge : CFAUtils.enteringEdges(originalFunctionSummaryEdge.getSuccessor())) {
      if (edge instanceof CFunctionReturnEdge) {
        originalFunctionReturnEdge = (CFunctionReturnEdge) edge;
      }
    }

    var relevantFunctionDeclaration =
        cloneFunctionDeclaration(
            originalFunctionEntryNode.getFunctionDefinition(), pFunctionManager);
    Optional<CVariableDeclaration> optionalReturnVariable =
        originalFunctionEntryNode.getReturnVariable().toJavaUtil();

    if (relevantFunctionDeclaration.getType().getReturnType().equals(CVoidType.VOID)) {
      optionalReturnVariable = Optional.empty();
    }

    var relevantFunctionExitNode = new FunctionExitNode(relevantFunctionDeclaration);
    var relevantFunctionEntryNode =
        new CFunctionEntryNode(
            pEdge.getFileLocation(),
            relevantFunctionDeclaration,
            relevantFunctionExitNode,
            com.google.common.base.Optional.fromJavaUtil(optionalReturnVariable));

    var originalFunctionCall = originalFunctionSummaryEdge.getExpression();
    var originalFunctionCallExpression = originalFunctionCall.getFunctionCallExpression();
    List<CExpression> relevantParameterExpressions =
        filterParams(
            originalFunctionCallExpression.getParameterExpressions(),
            originalFunctionEntryNode.getFunctionDefinition(),
            pFunctionManager);

    var relevantFunctionCallExpression =
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

        String returnVariableName = optionalReturnVariable.orElseThrow().getQualifiedName();

        if (pFunctionManager
            .getSlice()
            .isRelevantDef(
                originalFunctionReturnEdge, MemoryLocation.valueOf(returnVariableName))) {

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

    var relevantFunctionSummaryEdge =
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
  private CFAEdge cloneEdge(
      CFAEdge pEdge, CFANode pPredecessor, CFANode pSuccessor, FunctionManager pFunctionManager) {

    CFAEdgeType type = pEdge.getEdgeType();

    if (type == CFAEdgeType.BlankEdge) {

      return new BlankEdge(
          pEdge.getRawStatement(),
          pEdge.getFileLocation(),
          pPredecessor,
          pSuccessor,
          pEdge.getDescription());

    } else if (type == CFAEdgeType.AssumeEdge && pEdge instanceof CAssumeEdge) {

      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      return new CAssumeEdge(
          assumeEdge.getRawStatement(),
          assumeEdge.getFileLocation(),
          pPredecessor,
          pSuccessor,
          assumeEdge.getRawAST().get(),
          assumeEdge.getTruthAssumption(),
          assumeEdge.isSwapped(),
          assumeEdge.isArtificialIntermediate());

    } else if (pEdge instanceof CFunctionSummaryStatementEdge) {

      CFunctionSummaryStatementEdge statementEdge = (CFunctionSummaryStatementEdge) pEdge;
      return new CFunctionSummaryStatementEdge(
          statementEdge.getRawStatement(),
          statementEdge.getStatement(),
          statementEdge.getFileLocation(),
          pPredecessor,
          pSuccessor,
          statementEdge.getFunctionCall(),
          statementEdge.getFunctionName());

    } else if (type == CFAEdgeType.StatementEdge && pEdge instanceof CStatementEdge) {

      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      return new CStatementEdge(
          statementEdge.getRawStatement(),
          statementEdge.getStatement(),
          statementEdge.getFileLocation(),
          pPredecessor,
          pSuccessor);

    } else if (type == CFAEdgeType.DeclarationEdge && pEdge instanceof CDeclarationEdge) {
      
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      CDeclaration originalDeclaration = declarationEdge.getDeclaration();
      CDeclaration relevantDeclaration = originalDeclaration;

      if (originalDeclaration instanceof CFunctionDeclaration) {
        relevantDeclaration =
            cloneFunctionDeclaration((CFunctionDeclaration) originalDeclaration, pFunctionManager);
      }

      return new CDeclarationEdge(
          declarationEdge.getRawStatement(),
          declarationEdge.getFileLocation(),
          pPredecessor,
          pSuccessor,
          relevantDeclaration);

    } else if (type == CFAEdgeType.ReturnStatementEdge && pEdge instanceof CReturnStatementEdge) {

      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pEdge;
      return new CReturnStatementEdge(
          returnStatementEdge.getRawStatement(),
          returnStatementEdge.getRawAST().get(),
          returnStatementEdge.getFileLocation(),
          pPredecessor,
          (FunctionExitNode) pSuccessor);

    } else if (type == CFAEdgeType.FunctionCallEdge && pEdge instanceof CFunctionCallEdge) {

      return cloneFunctionCall(
          (CFunctionCallEdge) pEdge, pPredecessor, pSuccessor, pFunctionManager);

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
   * @param pFunctionManager the function manger for the original functions.
   * @param pNewNodes the mapping of function names to function nodes (multimap).
   * @return the function entry node of the created function.
   */
  private FunctionEntryNode createRelevantFunction(
      CFunctionEntryNode pEntryNode,
      FunctionManager pFunctionManager,
      Multimap<String, CFANode> pNewNodes) {

    ImmutableSet<CFAEdge> relevantEdges = pFunctionManager.getRelevantEdges();

    Map<Integer, CFANode> nodeMap =
        new HashMap<>(); // mapping of old-node-number --> new-node-clone
    Queue<CFAEdge> waitlist = new ArrayDeque<>(); // edges to be processed (cloned or replaced)
    Set<CFANode> visited =
        new HashSet<>(); // a node is visited when all its leaving edges were added to the waitlist

    FunctionEntryNode newEntryNode =
        (FunctionEntryNode) cloneNode(pEntryNode, nodeMap, pFunctionManager);
    CFAUtils.leavingEdges(pEntryNode).copyInto(waitlist);
    visited.add(pEntryNode);

    while (!waitlist.isEmpty()) {

      CFAEdge edge = waitlist.remove();
      CFANode pred = edge.getPredecessor();
      CFANode succ = edge.getSuccessor();

      CFAEdge newEdge;
      CFANode newPred = cloneNode(pred, nodeMap, pFunctionManager);
      CFANode newSucc;

      // step over function
      if (edge instanceof CFunctionCallEdge) {
        succ = ((CFunctionCallEdge) edge).getSummaryEdge().getSuccessor();
      }

      newSucc = cloneNode(succ, nodeMap, pFunctionManager);

      if (relevantEdges.contains(edge)
          || edge.getEdgeType() == CFAEdgeType.BlankEdge
          || edge.getEdgeType() == CFAEdgeType.AssumeEdge
          || edge instanceof CFunctionSummaryStatementEdge) {

        newEdge = cloneEdge(edge, newPred, newSucc, pFunctionManager);

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

  /** Creates a new CFA that resembles the program slice as closely as possible. */
  private CFA createRelevantCfa(final Slice pSlice) {

    final CFA originalCfa = pSlice.getOriginalCfa();
    final ImmutableSet<CFAEdge> relevantEdges = pSlice.getRelevantEdges();

    FunctionManager functionManager = new FunctionManager(pSlice);

    Map<AFunctionDeclaration, CDeclarationEdge> functionDeclarations = new HashMap<>();
    for (CFAEdge edge : relevantEdges) {
      if (edge instanceof CDeclarationEdge) {
        CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        CDeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CFunctionDeclaration) {
          functionDeclarations.put((CFunctionDeclaration) declaration, declarationEdge);
        }
      }
    }

    for (FunctionEntryNode entryNode : originalCfa.getAllFunctionHeads()) {
      AFunctionDeclaration function = entryNode.getFunction();
      CDeclarationEdge declarationEdge = functionDeclarations.get(function);
      if (declarationEdge != null) {
        functionManager.insert(function, entryNode, declarationEdge);
      }
    }

    AFunctionDeclaration originalMainFunction = originalCfa.getMainFunction().getFunction();
    if (!functionManager.isRelevant(originalMainFunction)) {
      FunctionEntryNode entryNode = originalCfa.getMainFunction();
      functionManager.insert(entryNode.getFunction(), entryNode, null);
    }

    NavigableMap<String, FunctionEntryNode> relevantFunctions = new TreeMap<>();
    TreeMultimap<String, CFANode> relevantNodes = TreeMultimap.create();
    FunctionEntryNode relevantMainEntryNode = null;
    for (AFunctionDeclaration originalFunction : functionManager.getFunctions()) {

      FunctionEntryNode originalEntryNode = functionManager.getEntryNode(originalFunction);
      FunctionEntryNode relevantEntryNode =
          createRelevantFunction(
              (CFunctionEntryNode) originalEntryNode, functionManager, relevantNodes);
      relevantFunctions.put(originalFunction.getQualifiedName(), relevantEntryNode);

      if (originalFunction.equals(originalMainFunction)) {
        checkState(
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

  /**
   * Executes the slice-exporter, which (depending on the configuration) exports program slices to C
   * program files.
   *
   * @param pSlice program slice to export
   */
  public void execute(Slice pSlice) {
    exportCount++;
    if (exportCriteria && exportCriteriaFile != null) {
      Concurrency.newThread(
              "Slice-criteria-Exporter",
              () -> {
                Path path = exportCriteriaFile.getPath(exportCount);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {
                  StringBuilder output = new StringBuilder();
                  for (CFAEdge e : pSlice.getUsedCriteria()) {
                    FileLocation loc = e.getFileLocation();
                    output
                        .append(loc.getFileName())
                        .append(":")
                        .append(loc.getStartingLineNumber())
                        .append(":")
                        .append(e.getCode())
                        .append("\n");
                  }
                  writer.write(output.toString());

                } catch (IOException e) {
                  logger.logUserException(
                      Level.WARNING, e, "Could not write slicing criteria to file " + path);
                }
              })
          .start();
    }

    if (exportToC && exportToCFile != null) {
      Concurrency.newThread(
              "Slice-Exporter",
              () -> {
                CFA sliceCfa = createRelevantCfa(pSlice);

                Path path = exportToCFile.getPath(exportCount);

                try (Writer writer = IO.openOutputFile(path, Charset.defaultCharset())) {

                  assert translator != null;
                  String code = translator.translateCfa(sliceCfa);
                  writer.write(code);

                } catch (CPAException | IOException | InvalidConfigurationException e) {
                  logger.logUserException(Level.WARNING, e, "Could not write CFA to C file.");
                }
              })
          .start();
    }
  }

  private static final class FunctionManager {

    private final Slice slice;
    private final Map<AFunctionDeclaration, FunctionManager.Value> functions;

    private FunctionManager(Slice pSlice) {

      slice = pSlice;
      functions = new HashMap<>();
    }

    private Slice getSlice() {
      return slice;
    }

    private Set<AFunctionDeclaration> getFunctions() {
      return functions.keySet();
    }

    private ImmutableSet<CFAEdge> getRelevantEdges() {
      return slice.getRelevantEdges();
    }

    private void insert(
        AFunctionDeclaration pFunction,
        FunctionEntryNode pEntryNode,
        CDeclarationEdge pDeclarationEdge) {
      functions.put(pFunction, new FunctionManager.Value(pEntryNode, pDeclarationEdge));
    }

    private boolean isRelevant(AFunctionDeclaration pFunction) {
      return functions.containsKey(pFunction);
    }

    private CDeclarationEdge getDeclarationEdge(AFunctionDeclaration pFunction) {
      return functions.get(pFunction).declarationEdge;
    }

    private FunctionEntryNode getEntryNode(AFunctionDeclaration pFunction) {
      return functions.get(pFunction).entryNode;
    }

    private static final class Value {

      private final FunctionEntryNode entryNode;
      private final CDeclarationEdge declarationEdge;

      private Value(FunctionEntryNode pEntryNode, CDeclarationEdge pDeclarationEdge) {

        entryNode = pEntryNode;
        declarationEdge = pDeclarationEdge;
      }
    }
  }
}

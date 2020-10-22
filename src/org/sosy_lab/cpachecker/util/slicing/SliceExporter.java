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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
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
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
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
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFATraversal;
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

  /** Returns true if any leaving edge of any node is contained in pRelevantEdges. */
  private boolean containsRelevantEdge(Collection<CFANode> pNodes, Set<CFAEdge> pRelevantEdges) {

    for (CFANode node : pNodes) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (pRelevantEdges.contains(edge)) {
          return true;
        }
      }
    }

    return false;
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

  private <T> List<T> filterParams(Iterable<T> pInput, Slice pSlice, CDeclarationEdge pEdge) {

    CDeclaration declaration = pEdge.getDeclaration();

    if (!(declaration instanceof CFunctionDeclaration)) {
      throw new IllegalArgumentException("pEdge must be a function declaration edge");
    }

    CFunctionDeclaration functionDeclaration = (CFunctionDeclaration) declaration;

    List<T> result = new ArrayList<>();
    Iterator<T> iterator = pInput.iterator();

    for (CParameterDeclaration parameter : functionDeclaration.getParameters()) {

      assert iterator.hasNext() : "pInput must have one element for every parameter";
      T element = iterator.next();

      MemoryLocation memoryLocation = MemoryLocation.valueOf(parameter.getQualifiedName());

      if (pSlice.isRelevantDef(pEdge, memoryLocation)) {
        result.add(element);
      }
    }

    assert !iterator.hasNext() : "pInput must have one element for every parameter";

    return result;
  }

  private CFunctionDeclaration cloneFunctionDeclaration(Slice pSlice, CDeclarationEdge pEdge) {

    CDeclaration declaration = pEdge.getDeclaration();

    if (!(declaration instanceof CFunctionDeclaration)) {
      throw new IllegalArgumentException("pEdge must be a function declaration edge");
    }

    CFunctionDeclaration originalFunctionDeclaration = (CFunctionDeclaration) declaration;

    CFunctionType originalFunctionType = originalFunctionDeclaration.getType();
    List<CParameterDeclaration> relevantParameters =
        filterParams(originalFunctionDeclaration.getParameters(), pSlice, pEdge);

    CFunctionType relevantFunctionType =
        new CFunctionTypeWithNames(
            originalFunctionType.getReturnType(),
            relevantParameters,
            originalFunctionType.takesVarArgs());

    return new CFunctionDeclaration(
        pEdge.getFileLocation(),
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
      CFANode pNode,
      Map<Integer, CFANode> pNodeMap,
      Slice pSlice,
      Map<AFunctionDeclaration, CDeclarationEdge> pFunctionDeclarations) {

    CFANode newNode = pNodeMap.get(pNode.getNodeNumber());
    if (newNode != null) {
      return newNode;
    }

    var functionDeclaration = (CFunctionDeclaration) pNode.getFunction();
    var declarationEdge = pFunctionDeclarations.get(functionDeclaration);

    if (declarationEdge != null) {
      functionDeclaration = cloneFunctionDeclaration(pSlice, declarationEdge);
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
              cloneNode(
                  originalFunctionEntryNode.getExitNode(), pNodeMap, pSlice, pFunctionDeclarations);

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
      Slice pSlice,
      CDeclarationEdge pDeclarationEdge) {

    var originalFunctionSummaryEdge = pEdge.getSummaryEdge();
    var originalFunctionEntryNode = pEdge.getSuccessor();

    var relevantFunctionDeclaration = cloneFunctionDeclaration(pSlice, pDeclarationEdge);
    var relevantFunctionExitNode = new FunctionExitNode(relevantFunctionDeclaration);
    var relevantFunctionEntryNode =
        new CFunctionEntryNode(
            pEdge.getFileLocation(),
            relevantFunctionDeclaration,
            relevantFunctionExitNode,
            originalFunctionEntryNode.getReturnVariable());

    var originalFunctionCall = originalFunctionSummaryEdge.getExpression();
    var originalFunctionCallExpression = originalFunctionCall.getFunctionCallExpression();
    List<CExpression> relevantParameterExpressions =
        filterParams(
            originalFunctionCallExpression.getParameterExpressions(), pSlice, pDeclarationEdge);

    var relevantFunctionCallExpression =
        new CFunctionCallExpression(
            pEdge.getFileLocation(),
            originalFunctionCallExpression.getExpressionType(),
            originalFunctionCallExpression.getFunctionNameExpression(),
            relevantParameterExpressions,
            relevantFunctionDeclaration);

    CFunctionCall relevantFunctionCall;

    if (originalFunctionCall instanceof CFunctionCallStatement) {

      var originalFunctionCallStatement = (CFunctionCallStatement) originalFunctionCall;
      relevantFunctionCall =
          new CFunctionCallStatement(
              originalFunctionCallStatement.getFileLocation(), relevantFunctionCallExpression);

    } else if (originalFunctionCall instanceof CFunctionCallAssignmentStatement) {

      var originalFunctionCallAssignmentStatement =
          (CFunctionCallAssignmentStatement) originalFunctionCall;
      relevantFunctionCall =
          new CFunctionCallAssignmentStatement(
              originalFunctionCallAssignmentStatement.getFileLocation(),
              originalFunctionCallAssignmentStatement.getLeftHandSide(),
              relevantFunctionCallExpression);

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
      CFAEdge pEdge,
      CFANode pPredecessor,
      CFANode pSuccessor,
      Slice pSlice,
      Map<AFunctionDeclaration, CDeclarationEdge> pFunctionDeclarations) {

    FileLocation loc = pEdge.getFileLocation();
    String raw = pEdge.getRawStatement();
    String desc = pEdge.getDescription();
    CFAEdgeType type = pEdge.getEdgeType();

    if (type == CFAEdgeType.BlankEdge) {

      return new BlankEdge(raw, loc, pPredecessor, pSuccessor, desc);

    } else if (type == CFAEdgeType.AssumeEdge && pEdge instanceof CAssumeEdge) {

      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      return new CAssumeEdge(
          raw,
          loc,
          pPredecessor,
          pSuccessor,
          assumeEdge.getRawAST().get(),
          assumeEdge.getTruthAssumption(),
          assumeEdge.isSwapped(),
          assumeEdge.isArtificialIntermediate());

    } else if (pEdge instanceof CFunctionSummaryStatementEdge) {

      CFunctionSummaryStatementEdge statementEdge = (CFunctionSummaryStatementEdge) pEdge;
      return new CFunctionSummaryStatementEdge(
          raw,
          statementEdge.getStatement(),
          loc,
          pPredecessor,
          pSuccessor,
          statementEdge.getFunctionCall(),
          statementEdge.getFunctionName());

    } else if (type == CFAEdgeType.StatementEdge && pEdge instanceof CStatementEdge) {

      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      return new CStatementEdge(raw, statementEdge.getStatement(), loc, pPredecessor, pSuccessor);

    } else if (type == CFAEdgeType.DeclarationEdge && pEdge instanceof CDeclarationEdge) {
      
      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      CDeclaration originalDeclaration = declarationEdge.getDeclaration();
      CDeclaration relevantDeclaration = originalDeclaration;

      if (originalDeclaration instanceof CFunctionDeclaration) {
        relevantDeclaration = cloneFunctionDeclaration(pSlice, declarationEdge);
      }

      return new CDeclarationEdge(raw, loc, pPredecessor, pSuccessor, relevantDeclaration);

    } else if (type == CFAEdgeType.ReturnStatementEdge && pEdge instanceof CReturnStatementEdge) {

      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge) pEdge;
      return new CReturnStatementEdge(
          raw,
          returnStatementEdge.getRawAST().get(),
          loc,
          pPredecessor,
          (FunctionExitNode) pSuccessor);

    } else if (type == CFAEdgeType.FunctionCallEdge && pEdge instanceof CFunctionCallEdge) {

      CDeclarationEdge declarationEdge =
          pFunctionDeclarations.get(pEdge.getSuccessor().getFunction());

      return cloneFunctionCall(
          (CFunctionCallEdge) pEdge, pPredecessor, pSuccessor, pSlice, declarationEdge);

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
   * @param pSlice the program slice.
   * @param pFunctionDeclarations a map from function declarations to the corresponding function
   *     declaration edges.
   * @param pNewNodes the mapping of function names to function nodes (multimap).
   * @return the function entry node of the created function.
   */
  private FunctionEntryNode createRelevantFunction(
      CFunctionEntryNode pEntryNode,
      Slice pSlice,
      Map<AFunctionDeclaration, CDeclarationEdge> pFunctionDeclarations,
      Multimap<String, CFANode> pNewNodes) {

    Map<Integer, CFANode> nodeMap =
        new HashMap<>(); // mapping of old-node-number --> new-node-clone
    Queue<CFAEdge> waitlist = new ArrayDeque<>(); // edges to be processed (cloned or replaced)
    Set<CFANode> visited =
        new HashSet<>(); // a node is visited when all its leaving edges were added to the waitlist

    FunctionEntryNode newEntryNode =
        (FunctionEntryNode) cloneNode(pEntryNode, nodeMap, pSlice, pFunctionDeclarations);
    CFAUtils.leavingEdges(pEntryNode).copyInto(waitlist);
    visited.add(pEntryNode);

    while (!waitlist.isEmpty()) {

      CFAEdge edge = waitlist.remove();
      CFANode pred = edge.getPredecessor();
      CFANode succ = edge.getSuccessor();

      CFAEdge newEdge;
      CFANode newPred = cloneNode(pred, nodeMap, pSlice, pFunctionDeclarations);
      CFANode newSucc;

      // step over function
      if (edge instanceof CFunctionCallEdge) {
        succ = ((CFunctionCallEdge) edge).getSummaryEdge().getSuccessor();
      }

      newSucc = cloneNode(succ, nodeMap, pSlice, pFunctionDeclarations);

      if (pSlice.getRelevantEdges().contains(edge)
          || edge.getEdgeType() == CFAEdgeType.BlankEdge
          || edge.getEdgeType() == CFAEdgeType.AssumeEdge
          || edge instanceof CFunctionSummaryStatementEdge) {

        newEdge = cloneEdge(edge, newPred, newSucc, pSlice, pFunctionDeclarations);

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
        if (skipNextAssumeBranching(succ, pSlice.getRelevantEdges())) {

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

    NavigableMap<String, FunctionEntryNode> newFunctions = new TreeMap<>();
    TreeMultimap<String, CFANode> newNodes = TreeMultimap.create();
    Map<AFunctionDeclaration, CDeclarationEdge> functionDeclarations = new HashMap<>();
    FunctionEntryNode newMainEntryNode = null;

    for (CFAEdge edge : relevantEdges) {
      if (edge instanceof CDeclarationEdge) {
        CDeclarationEdge declarationEdge = (CDeclarationEdge) edge;
        CDeclaration declaration = declarationEdge.getDeclaration();
        if (declaration instanceof CFunctionDeclaration) {
          functionDeclarations.put((CFunctionDeclaration) declaration, declarationEdge);
        }
      }
    }

    for (String functionName : originalCfa.getAllFunctionNames()) {
      final boolean isMainFunction =
          functionName.equals(originalCfa.getMainFunction().getFunctionName());

      FunctionEntryNode entryNode = originalCfa.getFunctionHead(functionName);

      Collection<CFANode> functionNodes =
          CFATraversal.dfs().collectNodesReachableFromTo(entryNode, entryNode.getExitNode());

      if (isMainFunction || containsRelevantEdge(functionNodes, relevantEdges)) {
        final FunctionEntryNode newEntryNode =
            createRelevantFunction(
                (CFunctionEntryNode) entryNode, pSlice, functionDeclarations, newNodes);
        newFunctions.put(functionName, newEntryNode);

        if (isMainFunction) {
          checkState(
              newMainEntryNode == null,
              "Trying to set entry node of main function, but one already exists: %s",
              newMainEntryNode);
          newMainEntryNode = newEntryNode;
        }
      }
    }
    assert newMainEntryNode != null;

    return new MutableCFA(
        originalCfa.getMachineModel(),
        newFunctions,
        newNodes,
        newMainEntryNode,
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
}

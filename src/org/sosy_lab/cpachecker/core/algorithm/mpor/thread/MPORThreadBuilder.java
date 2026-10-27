// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.program_counter.SeqProgramCounterVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.CFANodeForThread.BlockType;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCodeException;

public record MPORThreadBuilder(MPOROptions options, CFA cfa) {

  @VisibleForTesting public static final int MAIN_THREAD_ID = 0;

  private static int currentThreadId = MAIN_THREAD_ID;

  public static void resetThreadId() {
    currentThreadId = MAIN_THREAD_ID;
  }

  /** Track the currentPc, static so that it is consistent across recursive function calls. */
  private static int currentPc = SeqProgramCounterVariables.INIT_PC;

  public static void resetPc() {
    currentPc = SeqProgramCounterVariables.INIT_PC;
  }

  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   */
  public ImmutableList<MPORThread> extractThreadsFromCfa() throws UnsupportedCodeException {
    ImmutableList.Builder<MPORThread> rThreads = ImmutableList.builder();
    // add the main thread
    FunctionEntryNode mainEntryNode = cfa.getMainFunction();
    MPORThread mainThread = extractThreadFromCfa(Optional.empty(), Optional.empty(), mainEntryNode);
    rThreads.add(mainThread);
    // recursively search the thread CFAs for pthread_create calls, and store in rThreads
    List<MPORThread> createdThreads = new ArrayList<>();
    recursivelyFindThreadCreations(mainThread, createdThreads);
    // sort threads by ID, otherwise the program could have backward jumps
    createdThreads.sort(Comparator.comparingInt(MPORThread::id));
    rThreads.addAll(createdThreads);
    return rThreads.build();
  }

  private void recursivelyFindThreadCreations(
      MPORThread pCurrentThread, List<MPORThread> pFoundThreads) throws UnsupportedCodeException {

    for (CFAEdgeForThread threadEdge : pCurrentThread.cfa().threadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      Optional<CFunctionCall> optionalFunctionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(cfaEdge);
      if (optionalFunctionCall.isPresent()) {
        CFunctionCall functionCall = optionalFunctionCall.orElseThrow();
        if (PthreadUtil.isCallToPthreadFunction(functionCall, PthreadFunctionType.PTHREAD_CREATE)) {
          assert cfaEdge instanceof CStatementEdge : "pthread_create must be CStatementEdge";
          // extract the first parameter of pthread_create, i.e. the pthread_t value
          CIdExpression pthreadT =
              PthreadUtil.extractPthreadObject(functionCall, PthreadObjectType.PTHREAD_T);
          // extract the 3rd parameter of pthread_create which points to the start_routine function
          CFunctionDeclaration startRoutineDeclaration =
              PthreadUtil.extractStartRoutineDeclaration(functionCall);
          FunctionEntryNode entryNode =
              getStartRoutineFunctionEntryNode(startRoutineDeclaration, cfaEdge);
          MPORThread newThread =
              extractThreadFromCfa(Optional.of(pthreadT), Optional.of(threadEdge), entryNode);
          recursivelyFindThreadCreations(newThread, pFoundThreads);
          pFoundThreads.add(newThread);
        }
      }
    }
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   */
  private MPORThread extractThreadFromCfa(
      Optional<CIdExpression> pThreadObject,
      Optional<CFAEdgeForThread> pStartRoutineCall,
      FunctionEntryNode pEntryNode)
      throws UnsupportedCodeException {

    resetPc(); // reset pc for every thread created
    int newThreadId = currentThreadId++;
    CFAForThread threadCfa = buildThreadCfa(newThreadId, pEntryNode, pStartRoutineCall);
    Optional<CIdExpression> startRoutineExitVariable = tryCreateStartRoutineExitVariable(threadCfa);
    ImmutableListMultimap<CVariableDeclaration, SeqCallContext> localVariables =
        getLocalVariableDeclarations(threadCfa.threadEdges);
    return new MPORThread(
        newThreadId,
        pThreadObject,
        (CFunctionDeclaration) pEntryNode.getFunction(),
        pStartRoutineCall,
        startRoutineExitVariable,
        localVariables,
        threadCfa);
  }

  private CFAForThread buildThreadCfa(
      int pThreadId, FunctionEntryNode pEntryNode, Optional<CFAEdgeForThread> pStartRoutineCall)
      throws UnsupportedCodeException {

    // check if node was visited already in a specific calling context
    Multimap<CFANode, SeqCallContext> visitedNodes = ArrayListMultimap.create();
    Map<SeqCallContext, CFunctionSummaryEdge> visitedInitRoutines = new HashMap<>();
    ImmutableList.Builder<CFANodeForThread> threadNodes = ImmutableList.builder();
    // we use an immutable map to preserve ordering (important when declaring types)
    ImmutableMap.Builder<CFAEdgeForThread, CFAEdge> threadEdges = ImmutableMap.builder();

    // recursively build thread CFA, starting in pEntryNode
    buildThreadCfaVariables(
        pThreadId,
        visitedNodes,
        visitedInitRoutines,
        threadNodes,
        threadEdges,
        pEntryNode,
        // at start, there is never any block
        ImmutableTable.of(),
        // at start, use an empty call context
        SeqCallContext.EMPTY,
        pStartRoutineCall);

    ImmutableList<CFANodeForThread> finalThreadNodes = threadNodes.build();

    ImmutableSet<CFANode> allLoopHeads = cfa.getLoopStructure().orElseThrow().getAllLoopHeads();
    ImmutableSet<CFANodeForThread> loopHeads =
        finalThreadNodes.stream()
            .filter(n -> allLoopHeads.contains(n.getCfaNode()))
            .collect(ImmutableSet.toImmutableSet());

    return new CFAForThread(
        pThreadId,
        pEntryNode,
        finalThreadNodes,
        loopHeads,
        ImmutableList.copyOf(threadEdges.buildOrThrow().keySet()),
        visitedInitRoutines);
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   */
  private void buildThreadCfaVariables(
      final int pThreadId,
      Multimap<CFANode, SeqCallContext> pVisitedCfaNodes,
      Map<SeqCallContext, CFunctionSummaryEdge> pVisitedInitRoutines,
      ImmutableList.Builder<CFANodeForThread> pThreadNodes,
      ImmutableMap.Builder<CFAEdgeForThread, CFAEdge> pThreadEdges,
      CFANode pCurrentNode,
      ImmutableTable<SeqCallContext, BlockType, CFunctionCall> pBlockTypes,
      SeqCallContext pCallContext,
      final Optional<CFAEdgeForThread> pStartRoutineCall)
      throws UnsupportedCodeException {

    SeqCallContext callContext =
        MPORThreadUtil.getCallContextOrStartRoutineCall(pCallContext, pStartRoutineCall);
    // if node was visited in this context already, return
    if (pVisitedCfaNodes.containsKey(pCurrentNode)) {
      if (pVisitedCfaNodes.get(pCurrentNode).contains(callContext)) {
        return;
      }
    }
    pVisitedCfaNodes.put(pCurrentNode, callContext);

    // all leaving edges of a node are in the atomic block
    ImmutableMultimap<CFAEdgeForThread, CFAEdge> threadEdges =
        buildThreadEdgesFromCfaEdges(pThreadId, pCurrentNode, callContext, pVisitedInitRoutines);
    pThreadEdges.putAll(threadEdges.entries());
    List<CFAEdgeForThread> edgeList = new ArrayList<>(threadEdges.keySet());

    // recursively build cfa nodes and edges
    if (threadEdges.isEmpty()) {
      pThreadNodes.add(
          new CFANodeForThread(
              pThreadId,
              pCurrentNode,
              SeqProgramCounterVariables.EXIT_PC,
              callContext,
              edgeList,
              // thread exits are never in any block
              ImmutableSet.of()));
    } else {
      pThreadNodes.add(
          new CFANodeForThread(
              pThreadId,
              pCurrentNode,
              currentPc++,
              callContext,
              edgeList,
              pBlockTypes.columnKeySet()));
      for (CFAEdge cfaEdge : threadEdges.values()) {
        // exclude CFunctionReturnEdges in the search, their successors may be in other threads.
        // the original, same-thread successor is included due to the CFunctionSummaryEdge.
        if (!(cfaEdge instanceof CFunctionReturnEdge)) {
          // update the calling context, if a function call is encountered
          if (cfaEdge instanceof CFunctionCallEdge) {
            Optional<CFAEdgeForThread> callContextEdge =
                Optional.ofNullable(
                    Iterables.getOnlyElement(
                        threadEdges.entries().stream()
                            .filter(entry -> entry.getValue().equals(cfaEdge))
                            .map(Map.Entry::getKey)
                            .toList()));
            callContext = new SeqCallContext(callContextEdge);
          }
          buildThreadCfaVariables(
              pThreadId,
              pVisitedCfaNodes,
              pVisitedInitRoutines,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              updateBlockTypes(cfaEdge, callContext, pBlockTypes),
              callContext,
              pStartRoutineCall);
        }
      }
    }
  }

  /**
   * Updates the {@link BlockType}s so that they are still valid after the given {@link CFAEdge} is
   * executed. Returns the table mapping call contexts and block types to the {@link CFunctionCall}
   * that triggered the block. Since the {@link CFunctionCall} may be injected and not present in
   * the input program, the {@link SeqCallContext} and {@link CFunctionCall} may differ.
   */
  private ImmutableTable<SeqCallContext, BlockType, CFunctionCall> updateBlockTypes(
      CFAEdge pCfaEdge,
      SeqCallContext pCallContext,
      ImmutableTable<SeqCallContext, BlockType, CFunctionCall> pCurrentBlockTypes)
      throws UnsupportedCodeException {

    // Use a mutable table so that elements can be removed.
    Table<SeqCallContext, BlockType, CFunctionCall> blockTypes =
        HashBasedTable.create(pCurrentBlockTypes);

    if (blockTypes.contains(pCallContext, BlockType.PTHREAD_ONCE_ROUTINE)) {
      CFunctionDeclaration initRoutineDeclaration =
          Objects.requireNonNull(blockTypes.get(pCallContext, BlockType.PTHREAD_ONCE_ROUTINE))
              .getFunctionCallExpression()
              .getDeclaration();

      // The pthread_once init_routine is left only if the predecessor equals the declaration.
      // This excludes inner functions called inside init_routine from triggering the condition.
      if (pCfaEdge.getPredecessor().getFunction().equals(initRoutineDeclaration)
          && pCfaEdge.getSuccessor() instanceof FunctionExitNode
          // Since the init_routine call was not present in the input, there are no leaving edges.
          && pCfaEdge.getSuccessor().getLeavingEdges().isEmpty()) {
        blockTypes.remove(pCallContext, BlockType.PTHREAD_ONCE_ROUTINE);
      }
    }

    Optional<CFunctionCall> functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(pCfaEdge);

    if (functionCall.isPresent()) {
      CFunctionCall call = functionCall.orElseThrow();
      if (PthreadUtil.isCallToPthreadFunction(call, PthreadFunctionType.VERIFIER_ATOMIC_BEGIN)) {
        blockTypes.put(pCallContext, BlockType.ATOMIC, call);
      }
      if (PthreadUtil.isCallToPthreadFunction(call, PthreadFunctionType.VERIFIER_ATOMIC_END)) {
        blockTypes.remove(pCallContext, BlockType.ATOMIC);
      }
      if (PthreadUtil.isCallToPthreadFunction(call, PthreadFunctionType.PTHREAD_ONCE)) {
        CFunctionDeclaration initRoutineDeclaration =
            PthreadUtil.extractStartRoutineDeclaration(call);
        CFunctionCall initRoutineCall =
            PthreadUtil.createStartRoutineFunctionCall(initRoutineDeclaration, Optional.empty());
        blockTypes.put(pCallContext, BlockType.PTHREAD_ONCE_ROUTINE, initRoutineCall);
      }
    }

    return ImmutableTable.copyOf(blockTypes);
  }

  /**
   * Returns the intermediate exit variable that stores the {@code retval} given to {@code
   * pthread_exit}, or {@link Optional#empty()} if the thread does not call {@code pthread_exit} at
   * all.
   */
  private Optional<CIdExpression> tryCreateStartRoutineExitVariable(CFAForThread pThreadCFA) {
    for (CFAEdgeForThread threadEdge : pThreadCFA.threadEdges) {
      Optional<CFunctionCall> functionCall =
          PthreadUtil.tryGetFunctionCallFromCfaEdge(threadEdge.cfaEdge);
      if (functionCall.isPresent()) {
        if (PthreadUtil.isCallToPthreadExitWithReturnValue(functionCall.orElseThrow())) {
          String name = SeqNameUtil.buildStartRoutineExitVariableName(options, pThreadCFA.threadId);
          CVariableDeclaration declaration =
              SeqDeclarationBuilder.buildVariableDeclaration(
                  true, CPointerType.POINTER_TO_VOID, name, null);
          CIdExpression startRoutineExitVariable =
              SeqExpressionBuilder.buildIdExpression(declaration);
          return Optional.of(startRoutineExitVariable);
        }
      }
    }
    return Optional.empty();
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private ImmutableListMultimap<CVariableDeclaration, SeqCallContext> getLocalVariableDeclarations(
      ImmutableList<CFAEdgeForThread> pThreadEdges) {

    ImmutableListMultimap.Builder<CVariableDeclaration, SeqCallContext> rLocalVars =
        ImmutableListMultimap.builder();
    for (CFAEdgeForThread threadEdge : pThreadEdges) {
      CFAEdge edge = threadEdge.cfaEdge;
      if (edge instanceof CDeclarationEdge declarationEdge) {
        if (!declarationEdge.getDeclaration().isGlobal()) {
          AAstNode aAstNode = declarationEdge.getRawAST().orElseThrow();
          // exclude FunctionDeclarations
          if (aAstNode instanceof CVariableDeclaration variableDeclaration) {
            rLocalVars.put(variableDeclaration, threadEdge.callContext);
          }
        }
      }
    }
    return rLocalVars.build();
  }

  // (Private) Helpers =============================================================================

  private ImmutableListMultimap<CFAEdgeForThread, CFAEdge> buildThreadEdgesFromCfaEdges(
      int pThreadId,
      CFANode pCfaNode,
      SeqCallContext pCallContext,
      Map<SeqCallContext, CFunctionSummaryEdge> pVisitedInitRoutines)
      throws UnsupportedCodeException {

    // use ImmutableMap to retain insertion order
    ImmutableListMultimap.Builder<CFAEdgeForThread, CFAEdge> rThreadEdges =
        ImmutableListMultimap.builder();

    // if present, replace the call to pthread_once with a call to the init_routine
    for (CFAEdge edge : pCfaNode.getAllLeavingEdges()) {
      if (PthreadUtil.isCallToPthreadFunction(edge, PthreadFunctionType.PTHREAD_ONCE)) {
        CFunctionCall functionCall = PthreadUtil.tryGetFunctionCallFromCfaEdge(edge).orElseThrow();
        CFunctionDeclaration routineDeclaration =
            PthreadUtil.extractStartRoutineDeclaration(functionCall);
        CFunctionEntryNode functionEntryNode =
            (CFunctionEntryNode) cfa.getFunctionHead(routineDeclaration.getName());
        CFunctionCall routineCall =
            PthreadUtil.createStartRoutineFunctionCall(routineDeclaration, Optional.empty());

        // create the injected function edges
        CFunctionSummaryEdge functionSummaryEdge =
            new CFunctionSummaryEdge(
                routineCall.toASTString(),
                edge.getFileLocation(),
                pCfaNode,
                // the summary edge contains the original call context
                edge.getSuccessor(),
                routineCall,
                functionEntryNode);
        CFunctionCallEdge functionCallEdge =
            new CFunctionCallEdge(
                routineCall.toASTString(),
                edge.getFileLocation(),
                pCfaNode,
                functionEntryNode,
                routineCall,
                functionSummaryEdge);

        rThreadEdges.put(
            new CFAEdgeForThread(pThreadId, functionSummaryEdge, pCallContext),
            functionSummaryEdge);
        CFAEdgeForThread functionCallThreadEdge =
            new CFAEdgeForThread(pThreadId, functionCallEdge, pCallContext);
        rThreadEdges.put(functionCallThreadEdge, functionCallEdge);
        pVisitedInitRoutines.put(
            new SeqCallContext(Optional.of(functionCallThreadEdge)), functionSummaryEdge);
      } else {
        rThreadEdges.put(new CFAEdgeForThread(pThreadId, edge, pCallContext), edge);
      }
    }
    return rThreadEdges.build();
  }

  private FunctionEntryNode getStartRoutineFunctionEntryNode(
      CFunctionDeclaration pStartRoutineDeclaration, CFAEdge pPthreadCreateEdge)
      throws UnsupportedCodeException {

    FluentIterable<FunctionEntryNode> functionEntryNodes =
        FluentIterable.from(cfa.getAllFunctions().values())
            .filter(
                functionEntryNode ->
                    Objects.requireNonNull(functionEntryNode)
                        .getFunctionDefinition()
                        .equals(pStartRoutineDeclaration));
    if (functionEntryNodes.isEmpty()) {
      throw new UnsupportedCodeException(
          "Could not find a FunctionEntryNode for start_routine "
              + pStartRoutineDeclaration.getName(),
          pPthreadCreateEdge);
    }
    if (functionEntryNodes.size() > 1) {
      throw new UnsupportedCodeException(
          "Multiple FunctionEntryNodes found for start_routine "
              + pStartRoutineDeclaration.getName(),
          pPthreadCreateEdge);
    }
    return Objects.requireNonNull(Iterables.getOnlyElement(functionEntryNodes));
  }
}

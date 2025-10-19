// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.thread;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadObjectType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqDeclarationBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder.SeqExpressionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.goto_labels.SeqThreadLabelStatement;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.util.CFAUtils;

public class ThreadBuilder {

  private static int currentThreadId = Sequentialization.MAIN_THREAD_ID;

  public static void resetThreadId() {
    currentThreadId = Sequentialization.MAIN_THREAD_ID;
  }

  /** Track the currentPc, static so that it is consistent across recursive function calls. */
  private static int currentPc = Sequentialization.INIT_PC;

  public static void resetPc() {
    currentPc = Sequentialization.INIT_PC;
  }

  // TODO pthread_create calls in loops can be considered by loop unrolling
  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   */
  public static ImmutableList<MPORThread> createThreads(MPOROptions pOptions, CFA pCfa) {
    ImmutableList.Builder<MPORThread> rThreads = ImmutableList.builder();
    // add the main thread
    FunctionEntryNode mainEntryNode = pCfa.getMainFunction();
    MPORThread mainThread =
        createThread(pOptions, Optional.empty(), Optional.empty(), mainEntryNode);
    rThreads.add(mainThread);
    // recursively search the thread CFAs for pthread_create calls, and store in rThreads
    List<MPORThread> createdThreads = new ArrayList<>();
    recursivelyFindThreadCreations(pOptions, pCfa, mainThread, createdThreads);
    // sort threads by ID, otherwise the program could have backward jumps
    createdThreads.sort(Comparator.comparingInt(MPORThread::getId));
    rThreads.addAll(createdThreads);
    return rThreads.build();
  }

  private static void recursivelyFindThreadCreations(
      MPOROptions pOptions, CFA pCfa, MPORThread pCurrentThread, List<MPORThread> pFoundThreads) {

    for (ThreadEdge threadEdge : pCurrentThread.cfa.threadEdges) {
      CFAEdge cfaEdge = threadEdge.cfaEdge;
      if (PthreadUtil.isCallToPthreadFunction(cfaEdge, PthreadFunctionType.PTHREAD_CREATE)) {
        assert cfaEdge instanceof CStatementEdge : "pthread_create must be CStatementEdge";
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CIdExpression pthreadT =
            PthreadUtil.extractPthreadObject(cfaEdge, PthreadObjectType.PTHREAD_T);
        // extract the third parameter of pthread_create which points to the start_routine function
        CFunctionType startRoutine = PthreadUtil.extractStartRoutineType(cfaEdge);
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        MPORThread newThread =
            createThread(
                pOptions, Optional.ofNullable(pthreadT), Optional.of(threadEdge), entryNode);
        recursivelyFindThreadCreations(pOptions, pCfa, newThread, pFoundThreads);
        pFoundThreads.add(newThread);
      }
    }
  }

  /**
   * Initializes a MPORThread object with the corresponding CFANodes and CFAEdges and returns it.
   */
  private static MPORThread createThread(
      MPOROptions pOptions,
      Optional<CIdExpression> pThreadObject,
      Optional<ThreadEdge> pStartRoutineCall,
      FunctionEntryNode pEntryNode) {

    // ensure so that we can cast to CFunctionType
    checkArgument(
        pEntryNode.getFunction().getType() instanceof CFunctionType,
        "pEntryNode function must be CFunctionType");
    resetPc(); // reset pc for every thread created
    int newThreadId = currentThreadId++;
    ThreadCFA threadCfa = buildThreadCfa(newThreadId, pEntryNode, pStartRoutineCall);
    Optional<CIdExpression> startRoutineExitVariable =
        tryCreateStartRoutineExitVariable(pOptions, threadCfa);
    ImmutableListMultimap<CVariableDeclaration, Optional<ThreadEdge>> localVariables =
        getLocalVariableDeclarations(threadCfa.threadEdges);
    return new MPORThread(
        newThreadId,
        pThreadObject,
        (CFunctionDeclaration) pEntryNode.getFunction(),
        pStartRoutineCall,
        startRoutineExitVariable,
        localVariables,
        threadCfa,
        tryBuildKVariable(pOptions, newThreadId),
        tryBuildLabel(pOptions, newThreadId));
  }

  private static ThreadCFA buildThreadCfa(
      int pThreadId, FunctionEntryNode pEntryNode, Optional<ThreadEdge> pStartRoutineCall) {

    // check if node is present already in a specific calling context
    Multimap<CFANode, Optional<ThreadEdge>> visitedNodes = ArrayListMultimap.create();
    ImmutableList.Builder<ThreadNode> threadNodes = ImmutableList.builder();
    // we use an immutable map to preserve ordering (important when declaring types)
    ImmutableMap.Builder<ThreadEdge, CFAEdge> threadEdges = ImmutableMap.builder();

    // recursively build thread CFA, starting in pEntryNode
    buildThreadCfaVariables(
        pThreadId,
        visitedNodes,
        threadNodes,
        threadEdges,
        pEntryNode,
        false, // at start, no atomic block
        Optional.empty(),
        pStartRoutineCall);

    return new ThreadCFA(
        pThreadId,
        pEntryNode,
        threadNodes.build(),
        ImmutableList.copyOf(threadEdges.buildOrThrow().keySet()));
  }

  /**
   * Recursively searches the CFA of a thread specified by its entry node (the first pCurrentNode)
   * and pExitNode.
   */
  private static void buildThreadCfaVariables(
      final int pThreadId,
      Multimap<CFANode, Optional<ThreadEdge>> pVisitedCfaNodes,
      ImmutableList.Builder<ThreadNode> pThreadNodes,
      ImmutableMap.Builder<ThreadEdge, CFAEdge> pThreadEdges,
      CFANode pCurrentNode,
      boolean pIsInAtomicBlock,
      Optional<ThreadEdge> pCallContext,
      final Optional<ThreadEdge> pStartRoutineCall) {

    Optional<ThreadEdge> callContext =
        ThreadUtil.getCallContextOrStartRoutineCall(pCallContext, pStartRoutineCall);
    // if node was visited in this context already, return
    if (pVisitedCfaNodes.containsKey(pCurrentNode)) {
      if (pVisitedCfaNodes.get(pCurrentNode).contains(callContext)) {
        return;
      }
    }
    pVisitedCfaNodes.put(pCurrentNode, callContext);

    FluentIterable<CFAEdge> leavingCfaEdges = pCurrentNode.getAllLeavingEdges();
    // all leaving edges of a node are in the atomic block
    ImmutableBiMap<ThreadEdge, CFAEdge> threadEdges =
        buildThreadEdgesFromCfaEdges(pThreadId, leavingCfaEdges, callContext);
    pThreadEdges.putAll(threadEdges);
    List<ThreadEdge> edgeList = new ArrayList<>(threadEdges.keySet());

    // recursively build cfa nodes and edges
    if (leavingCfaEdges.isEmpty()) {
      pThreadNodes.add(
          new ThreadNode(
              // thread exits are never in atomic blocks
              pThreadId, pCurrentNode, Sequentialization.EXIT_PC, callContext, edgeList, false));
    } else {
      pThreadNodes.add(
          new ThreadNode(
              pThreadId, pCurrentNode, currentPc++, callContext, edgeList, pIsInAtomicBlock));
      for (CFAEdge cfaEdge : leavingCfaEdges) {
        // exclude function returns, their successors may be in other threads.
        // the original, same-thread successor is included due to the function summary edge.
        if (!(cfaEdge instanceof CFunctionReturnEdge)) {
          // update the calling context, if a function call is encountered
          if (cfaEdge instanceof CFunctionCallEdge) {
            callContext = Optional.ofNullable(threadEdges.inverse().get(cfaEdge));
          }
          buildThreadCfaVariables(
              pThreadId,
              pVisitedCfaNodes,
              pThreadNodes,
              pThreadEdges,
              cfaEdge.getSuccessor(),
              updateIsInAtomicBlock(cfaEdge, pIsInAtomicBlock),
              callContext,
              pStartRoutineCall);
        }
      }
    }
  }

  /** Checks if, after calling {@code pCfaEdge}, the thread is still/yet in an atomic block. */
  private static boolean updateIsInAtomicBlock(CFAEdge pCfaEdge, boolean pPreviousIsInAtomicBlock) {
    if (PthreadUtil.isCallToPthreadFunction(
        pCfaEdge, PthreadFunctionType.__VERIFIER_ATOMIC_BEGIN)) {
      return true;
    } else if (PthreadUtil.isCallToPthreadFunction(
        pCfaEdge, PthreadFunctionType.__VERIFIER_ATOMIC_END)) {
      return false;
    }
    return pPreviousIsInAtomicBlock;
  }

  /**
   * Returns the intermediate exit variable that stores the {@code retval} given to {@code
   * pthread_exit}, or {@link Optional#empty()} if the thread does not call {@code pthread_exit} at
   * all.
   */
  private static Optional<CIdExpression> tryCreateStartRoutineExitVariable(
      MPOROptions pOptions, ThreadCFA pThreadCFA) {

    for (ThreadEdge threadEdge : pThreadCFA.threadEdges) {
      if (PthreadUtil.isCallToPthreadFunction(
          threadEdge.cfaEdge, PthreadFunctionType.PTHREAD_EXIT)) {
        String name = SeqNameUtil.buildStartRoutineExitVariableName(pOptions, pThreadCFA.threadId);
        CVariableDeclaration declaration =
            SeqDeclarationBuilder.buildVariableDeclaration(
                true, CPointerType.POINTER_TO_VOID, name, null);
        CIdExpression startRoutineExitVariable =
            SeqExpressionBuilder.buildIdExpression(declaration);
        return Optional.of(startRoutineExitVariable);
      }
    }
    return Optional.empty();
  }

  /** Extracts all local variable declarations from pThreadEdges. */
  private static ImmutableListMultimap<CVariableDeclaration, Optional<ThreadEdge>>
      getLocalVariableDeclarations(ImmutableList<ThreadEdge> pThreadEdges) {

    ImmutableListMultimap.Builder<CVariableDeclaration, Optional<ThreadEdge>> rLocalVars =
        ImmutableListMultimap.builder();
    for (ThreadEdge threadEdge : pThreadEdges) {
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

  // TODO barriers see pthread-divine for examples
  //  SV benchmarks use their custom barrier objects and functions, e.g. pthread-divine/barrier_2t.i

  // (Private) Helpers =============================================================================

  private static ImmutableBiMap<ThreadEdge, CFAEdge> buildThreadEdgesFromCfaEdges(
      int pThreadId, FluentIterable<CFAEdge> pCfaEdges, Optional<ThreadEdge> pCallContext) {

    // use ImmutableBiMap to retain insertion order (HashBiMap does not)
    ImmutableBiMap.Builder<ThreadEdge, CFAEdge> rThreadEdges = ImmutableBiMap.builder();
    for (CFAEdge cfaEdge : pCfaEdges) {
      rThreadEdges.put(new ThreadEdge(pThreadId, cfaEdge, pCallContext), cfaEdge);
    }
    return rThreadEdges.buildOrThrow();
  }

  private static Optional<CIdExpression> tryBuildKVariable(MPOROptions pOptions, int pThreadId) {
    if (pOptions.nondeterminismSource.equals(NondeterminismSource.NUM_STATEMENTS)) {
      String variableName = SeqNameUtil.buildThreadKVariable(pThreadId);
      CVariableDeclaration declaration =
          SeqDeclarationBuilder.buildVariableDeclaration(
              false,
              pOptions.nondeterminismSigned ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
              variableName,
              SeqInitializer.INT_0);
      CIdExpression KVariable = SeqExpressionBuilder.buildIdExpression(declaration);
      return Optional.of(KVariable);
    }
    return Optional.empty();
  }

  private static Optional<SeqThreadLabelStatement> tryBuildLabel(
      MPOROptions pOptions, int pThreadId) {

    if (ThreadUtil.isThreadLabelRequired(pOptions)) {
      String name = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId);
      return Optional.of(new SeqThreadLabelStatement(name));
    }
    return Optional.empty();
  }
}

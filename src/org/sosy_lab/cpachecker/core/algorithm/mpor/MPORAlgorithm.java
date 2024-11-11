// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.Language;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cmdline.Output;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqDeclarations.SeqFunctionDeclaration;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIntegerLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqStringLiteralExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqTypes.SeqVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output.SequentializationWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.state.StateBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadBuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateRefiner;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateTransferRelation;
import org.sosy_lab.cpachecker.cpa.threading.GlobalAccessChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CPAs;

/**
 * This is an implementation of a Partial Order Reduction (POR) algorithm, presented in the 2022
 * paper "Sound Sequentialization for Concurrent Program Verification". This algorithm aims at
 * producing a reduced sequentialization of a parallel C program. The reduced sequentialization can
 * be given to an existing verifier capable of verifying sequential C programs. The POR and the
 * verifier serve as modules, hence Modular POR (MPOR).
 */
@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // TODO remove all @SuppressWarnings once finished

  // TODO (not sure if important for our algorithm) PredicateAbstractState.abstractLocations
  //  contains all CFANodes visited so far

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    outputSequentialization();
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  @CanIgnoreReturnValue
  public String outputSequentialization() throws UnrecognizedCodeException {
    Path inputFilePath = inputCfa.getFileNames().get(0);
    SequentializationWriter writer = new SequentializationWriter(logger, inputFilePath);
    CFunctionCallExpression seqErrorCall = getSeqErrorCall(writer.outputFileName, -1);
    if (!MPORStatics.isSeqErrorSet()) {
      MPORStatics.setSeqError(seqErrorCall.toASTString());
    }
    return writer.write(seq.generateProgram(substitutions));
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final Configuration config;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA inputCfa;

  private final GlobalAccessChecker gac;

  private final PredicateTransferRelation ptr;

  private final Sequentialization seq;

  /**
   * A map from {@link CFunctionCallEdge} Predecessors to Return Nodes. Needs to be initialized
   * before {@link MPORAlgorithm#threads}.
   */
  private final ImmutableMap<CFANode, CFANode> funcCallMap;

  private final StateBuilder stateBuilder;

  private final ThreadBuilder threadBuilder;

  /**
   * The set of threads in the program, including the main thread and all pthreads. Needs to be
   * initialized after {@link MPORAlgorithm#funcCallMap}.
   */
  private final ImmutableSet<MPORThread> threads;

  /**
   * The map of thread specific variable declaration substitutions. The main thread (0) handles
   * global variables.
   */
  private final ImmutableMap<MPORThread, CSimpleDeclarationSubstitution> substitutions;

  public MPORAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa)
      throws InvalidConfigurationException, UnrecognizedCodeException {

    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    inputCfa = pInputCfa;

    handleInitialInputProgramRejections(inputCfa);

    gac = new GlobalAccessChecker();
    PredicateCPA predicateCpa =
        CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, PredicateRefiner.class);
    ptr = predicateCpa.getTransferRelation();

    funcCallMap = getFunctionCallMap(inputCfa);

    threadBuilder = new ThreadBuilder(funcCallMap);
    stateBuilder = new StateBuilder(ptr, funcCallMap);

    threads = getThreads(inputCfa, funcCallMap);

    ImmutableSet<CVariableDeclaration> globalVars = getGlobalVars(inputCfa);
    MPORStatics.setBinExprBuilder(new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger));
    substitutions = SubstituteBuilder.buildSubstitutions(globalVars, threads);

    seq = new Sequentialization(threads.size());
  }

  public static MPORAlgorithm testInstance(LogManager pLogManager, CFA pInputCfa) {
    return new MPORAlgorithm(pLogManager, pInputCfa);
  }

  /** Use this constructor only for test purposes. */
  private MPORAlgorithm(LogManager pLogManager, CFA pInputCfa) {

    cpa = null;
    config = null;
    logger = pLogManager;
    shutdownNotifier = null;
    inputCfa = pInputCfa;

    handleInitialInputProgramRejections(inputCfa);

    gac = new GlobalAccessChecker();
    ptr = null;

    funcCallMap = getFunctionCallMap(inputCfa);

    threadBuilder = new ThreadBuilder(funcCallMap);
    stateBuilder = null;

    threads = getThreads(inputCfa, funcCallMap);

    ImmutableSet<CVariableDeclaration> globalVars = getGlobalVars(inputCfa);
    // in tests, we may use the same CPAchecker instance -> builder is init already
    if (!MPORStatics.isBinExprBuilderSet()) {
      MPORStatics.setBinExprBuilder(
          new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger));
    }
    substitutions = SubstituteBuilder.buildSubstitutions(globalVars, threads);

    seq = new Sequentialization(threads.size());
  }

  // Input program rejections ====================================================================

  /**
   * Handles initial (i.e. more may come at later stages of the MPOR transformation) input program
   * rejections and throws an {@link IllegalArgumentException} if the input program...
   *
   * <ul>
   *   <li>is not in C
   *   <li>contains multiple files
   *   <li>has no call to {@code pthread_create} i.e. is not parallel
   *   <li>uses arrays for {@code pthread_t} or {@code pthread_mutex_t} identifiers
   *   <li>stores the return value of any pthread method call
   *   <li>contains any unsupported {@code pthread} function, see {@link PthreadFuncType}
   *   <li>contains a {@code pthread_create} call in a loop
   *   <li>contains a recursive function call (both direct and indirect)
   * </ul>
   */
  private void handleInitialInputProgramRejections(CFA pInputCfa) {
    // TODO check for preprocessed files (all files must have .i ending)
    checkLanguageC(pInputCfa);
    checkOneInputFile(pInputCfa);
    checkIsParallelProgram(pInputCfa);
    checkUnsupportedFunctions(pInputCfa);
    checkPthreadArrayIdentifiers(pInputCfa);
    checkPthreadFunctionReturnValues(pInputCfa);
    // these are recursive and can be expensive, so they are last
    checkPthreadCreateLoops(pInputCfa);
    checkRecursiveFunctions(pInputCfa);
  }

  private void checkLanguageC(CFA pInputCfa) {
    Language language = pInputCfa.getMetadata().getInputLanguage();
    if (!language.equals(Language.C)) {
      throw Output.fatalError("MPOR does not support the language %s", language);
    }
  }

  private void checkOneInputFile(CFA pInputCfa) {
    if (pInputCfa.getFileNames().size() != 1) {
      throw Output.fatalError("MPOR expects exactly one input file");
    }
  }

  private void checkIsParallelProgram(CFA pInputCfa) {
    boolean isParallel = false;
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
        isParallel = true;
        break;
      }
    }
    if (!isParallel) {
      throw Output.fatalError(
          "MPOR expects parallel C program with at least one pthread_create call");
    }
  }

  private void checkPthreadArrayIdentifiers(CFA pInputCfa) {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      for (PthreadFuncType funcType : PthreadFuncType.values()) {
        if (funcType.isSupported && PthreadFuncType.callsPthreadFunc(cfaEdge, funcType)) {
          if (funcType.hasPthreadTIndex()) {
            int pthreadTIndex = funcType.getPthreadTIndex();
            CExpression parameter = CFAUtils.getParameterAtIndex(cfaEdge, pthreadTIndex);
            if (isArraySubscriptExpression(parameter)) {
              throw Output.fatalError(
                  "MPOR does not support arrays as pthread_t parameters in line %s: %s",
                  cfaEdge.getLineNumber(), cfaEdge.getCode());
            }
          }
          if (funcType.hasPthreadMutexTIndex()) {
            int pthreadMutexTIndex = funcType.getPthreadMutexTIndex();
            CExpression parameter = CFAUtils.getParameterAtIndex(cfaEdge, pthreadMutexTIndex);
            if (isArraySubscriptExpression(parameter)) {
              throw Output.fatalError(
                  "MPOR does not support arrays as pthread_mutex_t parameters in line %s: %s",
                  cfaEdge.getLineNumber(), cfaEdge.getCode());
            }
          }
        }
      }
    }
  }

  private void checkUnsupportedFunctions(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      for (PthreadFuncType funcType : PthreadFuncType.values()) {
        if (!funcType.isSupported) {
          if (PthreadFuncType.callsPthreadFunc(edge, funcType)) {
            throw Output.fatalError(
                "MPOR does not support the function in line %s: %s",
                edge.getLineNumber(), edge.getCode());
          }
        }
      }
    }
  }

  private void checkPthreadFunctionReturnValues(CFA pInputCfa) {
    for (CFAEdge edge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsAnyPthreadFunc(edge)) {
        if (edge.getRawAST().orElseThrow() instanceof CFunctionCallAssignmentStatement) {
          throw Output.fatalError(
              "MPOR does not support pthread method return value assignments, see line %s: %s",
              edge.getLineNumber(), edge.getCode());
        }
      }
    }
  }

  /**
   * Recursively checks if any {@code pthread_create} call in pInputCfa can be reached from itself,
   * i.e. if it is in a loop (or in a recursive call).
   */
  private void checkPthreadCreateLoops(CFA pInputCfa) {
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pInputCfa)) {
      if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
        if (MPORUtil.isSelfReachable(cfaEdge, Optional.empty(), new ArrayList<>(), cfaEdge)) {
          throw Output.fatalError("MPOR does not support pthread_create calls in loops");
        }
      }
    }
  }

  private void checkRecursiveFunctions(CFA pInputCfa) {
    for (FunctionEntryNode entry : pInputCfa.entryNodes()) {
      Optional<FunctionExitNode> exit = entry.getExitNode();
      // "upcasting" exit from FunctionExitNode to CFANode is necessary here...
      if (MPORUtil.isSelfReachable(entry, exit.map(node -> node), new ArrayList<>(), entry)) {
        throw Output.fatalError(
            "MPOR does not support the (in)direct recursive function %s in line %s",
            entry.getFunctionName(), entry.getFunction().getFileLocation().getStartingLineNumber());
      }
    }
  }

  /**
   * Tries to extract the FunctionExitNode from the given FunctionEntryNode and throws an {@link
   * IllegalArgumentException} if there is none.
   */
  public static FunctionExitNode getFunctionExitNode(FunctionEntryNode pFunctionEntryNode) {
    if (pFunctionEntryNode.getExitNode().isEmpty()) {
      throw Output.fatalError(
          "MPOR expects the main function and all start routines to contain a FunctionExitNode");
    }
    return pFunctionEntryNode.getExitNode().orElseThrow();
  }

  // Variable Initializers =======================================================================

  /**
   * Searches all CFAEdges in pCfa for {@link CFunctionCallEdge} and maps the predecessor CFANodes
   * to their ReturnNodes so that context-sensitive algorithms can be performed on the CFA.
   *
   * <p>E.g. a FunctionExitNode may have several leaving Edges, one for each time the function is
   * called. With the Map, extracting only the leaving Edge resulting in the ReturnNode is possible.
   * Using FunctionEntryNodes is not possible because the calling context (the node before the
   * function call) is lost, which is why keys are not FunctionEntryNodes.
   *
   * @param pCfa the CFA to be analyzed
   * @return A Map of CFANodes before a {@link CFunctionCallEdge} (keys) to the CFANodes where a
   *     function continues (values, i.e. the ReturnNode) after going through the CFA of the
   *     function called.
   */
  private ImmutableMap<CFANode, CFANode> getFunctionCallMap(CFA pCfa) {
    ImmutableMap.Builder<CFANode, CFANode> rFunctionCallMap = ImmutableMap.builder();
    for (CFAEdge cfaEdge : CFAUtils.allEdges(pCfa)) {
      if (cfaEdge instanceof CFunctionCallEdge functionCallEdge) {
        rFunctionCallMap.put(functionCallEdge.getPredecessor(), functionCallEdge.getReturnNode());
      }
    }
    return rFunctionCallMap.buildOrThrow();
  }

  /** Extracts all global variable declarations from pCfa. */
  private ImmutableSet<CVariableDeclaration> getGlobalVars(CFA pCfa) {
    ImmutableSet.Builder<CVariableDeclaration> rGlobalVars = ImmutableSet.builder();
    for (CFAEdge edge : CFAUtils.allEdges(pCfa)) {
      if (edge instanceof CDeclarationEdge declarationEdge) {
        if (gac.hasGlobalAccess(edge) && declarationEdge.getDeclaration().isGlobal()) {
          AAstNode aAstNode = declarationEdge.getRawAST().orElseThrow();
          // exclude FunctionDeclarations
          if (aAstNode instanceof CVariableDeclaration cVariableDeclaration) {
            rGlobalVars.add(cVariableDeclaration);
          }
        }
      }
    }
    return rGlobalVars.build();
  }

  // TODO pthread_create calls in loops can be considered by loop unrolling
  /**
   * Extracts all threads (main and pthreads) and the FunctionEntry / ExitNodes of their start
   * routines from the given CFA.
   *
   * <p>This functions needs to be called after functionCallMap was initialized so that we can track
   * the calling context of each thread.
   *
   * @param pCfa the CFA to be analyzed
   * @param pFunctionCallMap map from CFANodes before {@link CFunctionCallEdge} to
   *     FunctionReturnNodes
   * @return the set of threads
   */
  private ImmutableSet<MPORThread> getThreads(
      CFA pCfa, ImmutableMap<CFANode, CFANode> pFunctionCallMap) {

    ImmutableSet.Builder<MPORThread> rThreads = ImmutableSet.builder();

    // add the main thread
    FunctionEntryNode mainEntryNode = pCfa.getMainFunction();
    FunctionExitNode mainExitNode = getFunctionExitNode(mainEntryNode);
    assert threadBuilder != null;
    rThreads.add(threadBuilder.createThread(Optional.empty(), mainEntryNode, mainExitNode));

    // search the CFA for pthread_create calls
    for (CFAEdge cfaEdge : CFAUtils.allUniqueEdges(pCfa)) {
      if (PthreadFuncType.callsPthreadFunc(cfaEdge, PthreadFuncType.PTHREAD_CREATE)) {
        // extract the first parameter of pthread_create, i.e. the pthread_t value
        CIdExpression pthreadT = PthreadUtil.extractPthreadT(cfaEdge);
        // extract the third parameter of pthread_create which points to the start routine function
        CFunctionType startRoutine = PthreadUtil.extractStartRoutine(cfaEdge);
        FunctionEntryNode entryNode =
            CFAUtils.getFunctionEntryNodeFromCFunctionType(pCfa, startRoutine);
        FunctionExitNode exitNode = getFunctionExitNode(entryNode);
        rThreads.add(
            threadBuilder.createThread(Optional.ofNullable(pthreadT), entryNode, exitNode));
      }
    }
    return rThreads.build();
  }

  // (Private) Helpers ===========================================================================

  private boolean isArraySubscriptExpression(CExpression pExpression) {
    if (pExpression instanceof CArraySubscriptExpression) {
      return true;
    } else if (pExpression instanceof CUnaryExpression unary) {
      if (unary.getOperand() instanceof CArraySubscriptExpression) {
        return true;
      }
    }
    return false;
  }

  /**
   * Returns the {@link CFunctionCallExpression} of {@code __assert_fail("0", "{pFileName}",
   * {pLine}, "__SEQUENTIALIZATION_ERROR__");}
   */
  public static CFunctionCallExpression getSeqErrorCall(String pFileName, int pLine) {
    CStringLiteralExpression seqFileName =
        SeqStringLiteralExpression.buildStringLiteralExpr(SeqUtil.wrapInQuotationMarks(pFileName));
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        SeqVoidType.VOID,
        SeqIdExpression.ASSERT_FAIL,
        ImmutableList.of(
            SeqStringLiteralExpression.STRING_0,
            seqFileName,
            SeqIntegerLiteralExpression.buildIntLiteralExpr(pLine),
            SeqStringLiteralExpression.SEQUENTIALIZATION_ERROR),
        SeqFunctionDeclaration.ASSERT_FAIL);
  }

  // (Public) Helpers ===========================================================================

  /** Returns the MPORThread in pThreads whose pthread_t object is empty. */
  public static MPORThread getMainThread(ImmutableSet<MPORThread> pThreads) {
    for (MPORThread thread : pThreads) {
      if (thread.isMain()) {
        return thread;
      }
    }
    throw new IllegalArgumentException("pThreads does not contain the main thread");
  }
}

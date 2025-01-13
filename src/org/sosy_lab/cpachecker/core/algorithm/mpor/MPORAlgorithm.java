// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import java.time.Year;
import java.time.ZoneId;
import java.util.Optional;
import java.util.logging.Level;
import javax.xml.parsers.DocumentBuilderFactory;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadFuncType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads.PthreadUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqArraySubscriptExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output.SequentializationWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.output.SequentializationWriter.FileExtension;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqSyntax;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.string.SeqToken;
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
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The sequentialization contains reductions in the state space via assumptions over
 * allowed transitions between thread simulations. Sequentializations can be given to any verifier
 * capable of verifying sequential C programs, hence modular.
 */
@Options(prefix = "analysis.algorithm.MPOR")
@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // TODO remove all @SuppressWarnings once finished

  // TODO (not sure if important for our algorithm) PredicateAbstractState.abstractLocations
  //  contains all CFANodes visited so far

  @Option(
      secure = true,
      description =
          "whether to add assertions over thread simulation variables at the loop head. slows"
              + " down verification but improves sequentialization correctness guarantees")
  private boolean addLoopInvariants = false;

  @Option(
      secure = true,
      description =
          "whether to add partial order reduction assumptions in the sequentialization"
              + " to reduce the state space")
  private boolean addPOR = false;

  @Option(
      secure = true,
      description =
          "whether to use separate int values (scalars) for tracking thread pcs instead of"
              + " int arrays. may slow down or improve verification depending on the verifier")
  private boolean scalarPc = false;

  private static final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

  /** Matches both Windows (\r\n) and Unix-like (\n) newline conventions. */
  private static final Splitter newlineSplitter = Splitter.onPattern("\\r?\\n");

  private static final String licenseHeader =
      "// This file is part of CPAchecker,\n"
          + "// a tool for configurable software verification:\n"
          + "// https://cpachecker.sosy-lab.org\n"
          + "//\n"
          + "// SPDX-"
          + "FileCopyrightText: "
          + Year.now(ZoneId.systemDefault()).getValue()
          + " Dirk Beyer <https://www.sosy-lab.org>\n"
          + "//\n"
          + "// SPDX-License-Identifier:"
          + "Apache-2.0\n\n";

  private static final String seqHeader =
      "// This sequentialization (transformation of a concurrent program into an equivalent \n"
          + "// sequential program) was created by the MPORAlgorithm implemented in CPAchecker. \n"
          + "// \n"
          + "// Assertion fails from the function "
          + SeqToken.__SEQUENTIALIZATION_ERROR__
          + " mark faulty sequentializations. \n"
          + "// All other assertion fails are induced by faulty input programs.\n\n";

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException {
    Path inputFilePath = inputCfa.getFileNames().get(0);
    String seqName = createSeqName(inputFilePath);
    SequentializationWriter writer = new SequentializationWriter(logger, seqName, inputFilePath);
    String initSeq = buildInitSeq();
    String finalSeq =
        buildFinalSeq(
            inputFilePath.getFileName().toString(), seqName + FileExtension.I.suffix, initSeq);
    writer.write(finalSeq);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /** Returns the initial sequentialization, i.e. we adjust it in later stages */
  public String buildInitSeq() throws UnrecognizedCodeException {
    return seq.generateProgram(substitutions, addLoopInvariants, addPOR, scalarPc, logger);
  }

  /**
   * Adds the license and sequentialization comments at the top of pInitProgram and replaces the
   * file name and line in {@code reach_error("__FILE_NAME_PLACEHOLDER__", -1,
   * "__SEQUENTIALIZATION_ERROR__");} with pOutputFileName and the actual line.
   */
  public String buildFinalSeq(String pInputFileName, String pOutputFileName, String pInitProgram) {
    // consider license and seq comment header for line numbers
    String header = licenseHeader + seqHeader;
    int currentLine = countLines(header);
    StringBuilder rFinal = new StringBuilder();
    rFinal.append(header);
    // replace dummy line numbers (-1) with actual line numbers in the seq
    for (String line : newlineSplitter.split(pInitProgram)) {
      if (line.contains(Sequentialization.inputReachErrorDummy)) {
        CFunctionCallExpression reachErrorCall =
            Sequentialization.buildReachErrorCall(
                pInputFileName, currentLine, SeqToken.__PRETTY_FUNCTION__);
        rFinal.append(
            line.replace(
                Sequentialization.inputReachErrorDummy,
                reachErrorCall.toASTString() + SeqSyntax.SEMICOLON));
      } else if (line.contains(Sequentialization.outputReachErrorDummy)) {
        CFunctionCallExpression reachErrorCall =
            Sequentialization.buildReachErrorCall(
                pOutputFileName, currentLine, SeqToken.__SEQUENTIALIZATION_ERROR__);
        rFinal.append(
            line.replace(
                Sequentialization.outputReachErrorDummy,
                reachErrorCall.toASTString() + SeqSyntax.SEMICOLON));
      } else {
        rFinal.append(line);
      }
      rFinal.append(SeqSyntax.NEWLINE);
      currentLine++;
    }
    return rFinal.toString();
  }

  /** Returns the number of lines, i.e. the amount of \n + 1 in pString. */
  private int countLines(String pString) {
    if (isNullOrEmpty(pString)) {
      return 0;
    }
    return newlineSplitter.splitToList(pString).size();
  }

  private String createSeqName(Path pInputFilePath) {
    return SeqToken.__MPOR_SEQ__ + getFileNameWithoutExtension(pInputFilePath);
  }

  private String getFileNameWithoutExtension(Path pInputFilePath) {
    String fileName = pInputFilePath.getFileName().toString();
    return fileName.contains(".") ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;
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
      throws InvalidConfigurationException {

    pConfiguration.inject(this);

    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    inputCfa = pInputCfa;

    InputRejection.handleInitialRejections(logger, inputCfa);

    gac = new GlobalAccessChecker();

    try {
      PredicateCPA predicateCpa =
          CPAs.retrieveCPAOrFail(cpa, PredicateCPA.class, PredicateRefiner.class);
      ptr = predicateCpa.getTransferRelation();
    } catch (Exception e) {
      logger.logException(Level.SEVERE, e, e.getMessage());
      throw new AssertionError();
    }

    funcCallMap = getFunctionCallMap(inputCfa);

    threadBuilder = new ThreadBuilder(funcCallMap);
    stateBuilder = new StateBuilder(ptr, funcCallMap);

    threads = getThreads(inputCfa, funcCallMap);

    initStaticVariables(inputCfa, logger, scalarPc, threads.size());

    ImmutableSet<CVariableDeclaration> globalVars = getGlobalVars(inputCfa);
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

    InputRejection.handleInitialRejections(logger, inputCfa);

    gac = new GlobalAccessChecker();
    ptr = null;

    funcCallMap = getFunctionCallMap(inputCfa);

    threadBuilder = new ThreadBuilder(funcCallMap);
    stateBuilder = null;

    threads = getThreads(inputCfa, funcCallMap);

    initStaticVariables(inputCfa, logger, scalarPc, threads.size());

    ImmutableSet<CVariableDeclaration> globalVars = getGlobalVars(inputCfa);
    substitutions = SubstituteBuilder.buildSubstitutions(globalVars, threads);

    seq = new Sequentialization(threads.size());
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
    FunctionExitNode mainExitNode = InputRejection.getFunctionExitNode(mainEntryNode);
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
        FunctionExitNode exitNode = InputRejection.getFunctionExitNode(entryNode);
        rThreads.add(
            threadBuilder.createThread(Optional.ofNullable(pthreadT), entryNode, exitNode));
      }
    }
    return rThreads.build();
  }

  private void initStaticVariables(
      CFA pInputCfa, LogManager pLogger, boolean pScalarPc, int pNumThreads) {
    // in tests, we may use the same CPAchecker instance -> builder may be init already
    if (!MPORStatics.isBinExprBuilderSet()) {
      MPORStatics.setBinExprBuilder(
          new CBinaryExpressionBuilder(pInputCfa.getMachineModel(), pLogger));
    }
    if (pScalarPc) {
      if (SeqIdExpression.areScalarPcSet()) {
        SeqIdExpression.resetScalarPc();
      }
      SeqIdExpression.initScalarPc(pNumThreads);
    } else {
      if (SeqArraySubscriptExpression.areArrayPcSet()) {
        SeqArraySubscriptExpression.resetArrayPc();
      }
      SeqArraySubscriptExpression.initArrayPcExpr(pNumThreads);
    }
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

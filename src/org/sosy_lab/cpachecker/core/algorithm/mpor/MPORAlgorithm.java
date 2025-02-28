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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.nio.file.Path;
import org.checkerframework.dataflow.qual.TerminatesExecution;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.CSimpleDeclarationSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.SubstituteBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadBuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CFAUtils;

/**
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The sequentialization contains reductions in the state space by grouping commuting
 * statements together. Sequentializations can be given to any verifier capable of verifying
 * sequential C programs, hence modular.
 */
@Options(prefix = "analysis.algorithm.MPOR")
@SuppressWarnings("unused")
@SuppressFBWarnings({"UUF_UNUSED_FIELD", "URF_UNREAD_FIELD"})
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // TODO remove all @SuppressWarnings once finished

  @Option(
      secure = true,
      description =
          "include comments with trivia and additional newlines in the sequentialization? true ->"
              + " bigger file size")
  private boolean comments = false;

  @Option(
      secure = true,
      description =
          "include original function declarations from input file? true -> bigger file size")
  private boolean inputFunctionDeclarations = false;

  @Option(
      secure = true,
      description =
          "add an additional .yml file with metadata such as algorithm options and input file(s)?")
  private boolean outputMetadata = true;

  @Option(
      secure = true,
      description =
          "the path to output the sequentialization and metadata to. (\"output/\" by default)")
  private String outputPath = SeqWriter.DEFAULT_OUTPUT_PATH;

  @Option(
      secure = true,
      description = "overwrite files in the ./output directory when creating sequentializations?")
  private boolean overwriteFiles = true;

  @Option(
      secure = true,
      description =
          "add partial order reduction (grouping commuting statements) in the sequentialization"
              + " to reduce the state space?")
  private boolean partialOrderReduction = false;

  @Option(
      secure = true,
      description =
          "use separate int values (scalars) for tracking thread pcs instead of"
              + " int arrays? may slow down or improve verification depending on the verifier and"
              + " input program")
  private boolean scalarPc = false;

  @Option(
      secure = true,
      description =
          "use signed __VERIFIER_nondet_int() instead of unsigned __VERIFIER_nondet_uint() when"
              + " assigning next_thread at the loop head. may slow down or improve verification"
              + " depending on the verifier and input program")
  private boolean signedNextThread = false;

  private final MPOROptions options;

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException {
    // just use the first input file name for naming purposes
    Path firstInputFilePath = inputCfa.getFileNames().get(0);
    String inputFileName = firstInputFilePath.toString();
    String outputFileName = SeqNameUtil.buildOutputFileName(firstInputFilePath);
    Sequentialization sequentialization = buildSequentialization(inputFileName, outputFileName);
    String outputProgram = sequentialization.toString();
    SeqWriter seqWriter = new SeqWriter(logger, outputFileName, inputCfa.getFileNames(), options);
    seqWriter.write(outputProgram);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /** Creates a {@link Sequentialization} based on this instance, necessary for test purposes. */
  public Sequentialization buildSequentialization(String pInputFileName, String pOutputFileName) {
    return new Sequentialization(
        substitutions, options, pInputFileName, pOutputFileName, binaryExpressionBuilder, logger);
  }

  private static final String INTERNAL_ERROR =
      "MPOR FAIL. Sequentialization could not be created due to an internal error: ";

  /** Stops the algorithm by throwing an {@link AssertionError}. */
  @TerminatesExecution
  public static void fail(String pMessage) {
    throw new AssertionError(INTERNAL_ERROR + pMessage);
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final Configuration config;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA inputCfa;

  private final CBinaryExpressionBuilder binaryExpressionBuilder;

  /**
   * A map from {@link CFunctionCallEdge} predecessors to return nodes, used to perform calling
   * context-sensitive searches.
   */
  private final ImmutableMap<CFANode, CFANode> functionCallMap;

  /** The list of threads in the program, including the main thread and all pthreads. */
  private final ImmutableList<MPORThread> threads;

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
      throws InvalidConfigurationException, CPAException {

    pConfiguration.inject(this);

    options =
        new MPOROptions(
            comments,
            inputFunctionDeclarations,
            outputMetadata,
            outputPath,
            overwriteFiles,
            partialOrderReduction,
            scalarPc,
            signedNextThread);
    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    inputCfa = pInputCfa;

    InputRejection.handleRejections(logger, inputCfa);

    binaryExpressionBuilder = new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger);

    functionCallMap = CFAUtils.getFunctionCallMap(inputCfa);
    threads = ThreadBuilder.createThreads(inputCfa, functionCallMap);

    ImmutableSet<CVariableDeclaration> globalVars =
        CFAUtils.getGlobalVariableDeclarations(inputCfa);
    substitutions =
        SubstituteBuilder.buildSubstitutions(globalVars, threads, binaryExpressionBuilder);
  }

  /** Use this constructor only for test purposes. */
  private MPORAlgorithm(MPOROptions pOptions, LogManager pLogManager, CFA pInputCfa) {
    options = pOptions;
    cpa = null;
    config = null;
    logger = pLogManager;
    shutdownNotifier = null;
    inputCfa = pInputCfa;

    InputRejection.handleRejections(logger, inputCfa);

    binaryExpressionBuilder = new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger);

    functionCallMap = CFAUtils.getFunctionCallMap(inputCfa);
    threads = ThreadBuilder.createThreads(inputCfa, functionCallMap);

    ImmutableSet<CVariableDeclaration> globalVariableDeclarations =
        CFAUtils.getGlobalVariableDeclarations(inputCfa);
    substitutions =
        SubstituteBuilder.buildSubstitutions(
            globalVariableDeclarations, threads, binaryExpressionBuilder);
  }

  public static MPORAlgorithm testInstance(
      MPOROptions pOptions, LogManager pLogManager, CFA pInputCfa) {

    return new MPORAlgorithm(pOptions, pLogManager, pInputCfa);
  }
}

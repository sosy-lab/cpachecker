// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
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
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionBuilder;
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
@SuppressWarnings("unused") // this is necessary because we don't use the cpa and config
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // TODO with more benchmarks, find out which settings work best for which verifier
  //  then create an enum with preferred verifier, overriding all options, but output a warning

  // TODO add shortFunctions Option (e.g. assume instead of __MPOR_SEQ__assume)

  @Option(
      secure = true,
      description =
          "include comments with explaining trivia in the sequentialization? true ->"
              + " bigger file size")
  private boolean comments = false;

  @Option(
      secure = true,
      description =
          "make labels for thread statements consecutive? i.e. 0 to n - 1 where n is the"
              + " number of statements")
  private boolean consecutiveLabels = true;

  @Option(
      secure = true,
      description =
          "include original function declarations from input file? true -> bigger file size")
  private boolean inputFunctionDeclarations = false;

  // TODO make this secure by checking if all types for all variables are included
  @Option(
      description = "include original type declarations from input file? true -> bigger file size")
  private boolean inputTypeDeclarations = true;

  @Option(
      secure = true,
      description =
          "include CPAchecker license header in the sequentialization? true -> bigger file size")
  private boolean license = false;

  @Option(
      secure = true,
      description =
          "add an additional .yml file with metadata such as input file(s) and algorithm options?")
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

  // TODO POR is currently not secure because we assume pointer parameters that are assigned global
  //  variable addresses to commute
  @Option(
      description =
          "add partial order reduction (grouping commuting statements) in the sequentialization"
              + " to reduce the state space?")
  private boolean porConcat = true;

  @Option(
      description =
          "add partial order reduction (bit vectors storing global variable) in the"
              + " sequentialization to reduce the state space? distinguishing between global"
              + " variable reads and writes, not just accesses, reduces the state space more.")
  private BitVectorReduction porBitVectorReduction = BitVectorReduction.NONE;

  @Option(
      description =
          "the encoding (binary, hex, scalar) of the partial order reduction bit vectors.")
  // using optional for @Options is not allowed, unfortunately...
  private BitVectorEncoding porBitVectorEncoding = BitVectorEncoding.NONE;

  @Option(
      secure = true,
      description =
          "prune and simplify bit vector evaluation expressions based on the previous "
              + " bit vector assignments? true -> (should) improve verification performance")
  private boolean pruneBitVectorEvaluation = false;

  @Option(
      secure = true,
      description = "prune empty statements (with only pc writes) from the sequentialization?")
  private boolean pruneEmptyStatements = true;

  @Option(
      secure = true,
      description =
          "use separate int values (scalars) for tracking thread pcs instead of"
              + " int arrays? may slow down or improve verification depending on the verifier and"
              + " input program")
  private boolean scalarPc = true;

  @Option(
      description =
          "include additional reach_error marking sequentialization locations only reachable when"
              + " transformation is erroneous?")
  private boolean sequentializationErrors = false;

  @Option(secure = true, description = "use shortened variable names? e.g. THREAD0 -> T0")
  private boolean shortVariables = true;

  @Option(
      secure = true,
      description =
          "use signed __VERIFIER_nondet_int() instead of unsigned __VERIFIER_nondet_uint()?"
              + " in tests, signed generally slowed down verification performance.")
  private boolean signedNondet = false;

  @Option(
      secure = true,
      description =
          "use thread modular loops to reduce the amount of evaluated assume expressions?"
              + " may slow down or improve verification"
              + " depending on the verifier and input program")
  private boolean threadLoops = false;

  @Option(
      secure = true,
      description =
          "use the next_thread variable when choosing the thread loop to execute? may slow down or"
              + " improve verification  depending on the verifier")
  private boolean threadLoopsNext = false;

  @Option(
      description =
          "test if CPAchecker can parse sequentialization? true -> less efficient, but more"
              + " correctness guarantees")
  private boolean validateParse = true;

  @Option(
      description =
          "test if all label pc (except 0) are target pc and all target pc (except -1) are label pc"
              + " within a thread switch? true -> less efficient, but more correctness guarantees")
  private boolean validatePc = true;

  private final MPOROptions options;

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException {
    // just use the first input file name for naming purposes
    Path firstInputFilePath = inputCfa.getFileNames().get(0);
    String inputFileName = firstInputFilePath.toString();
    String outputFileName = SeqNameUtil.buildOutputFileName(firstInputFilePath);
    Sequentialization sequentialization = buildSequentialization(inputFileName, outputFileName);
    String outputProgram = sequentialization.toString();
    SeqWriter seqWriter =
        new SeqWriter(shutdownNotifier, logger, outputFileName, inputCfa.getFileNames(), options);
    seqWriter.write(outputProgram);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /** Creates a {@link Sequentialization} based on this instance, necessary for test purposes. */
  public Sequentialization buildSequentialization(String pInputFileName, String pOutputFileName) {
    return new Sequentialization(
        substitutions,
        options,
        pInputFileName,
        pOutputFileName,
        binaryExpressionBuilder,
        shutdownNotifier,
        logger);
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

  /** The list of threads in the program, including the main thread and all pthreads. */
  private final ImmutableList<MPORThread> threads;

  /**
   * The list of thread specific variable declaration substitutions. The substitution for the main
   * thread (0) handles global variables.
   */
  private final ImmutableList<MPORSubstitution> substitutions;

  public MPORAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa)
      throws InvalidConfigurationException {

    pConfiguration.inject(this);

    options =
        new MPOROptions(
            comments,
            consecutiveLabels,
            inputFunctionDeclarations,
            inputTypeDeclarations,
            license,
            outputMetadata,
            outputPath,
            overwriteFiles,
            porConcat,
            porBitVectorReduction,
            porBitVectorEncoding,
            pruneBitVectorEvaluation,
            pruneEmptyStatements,
            scalarPc,
            sequentializationErrors,
            shortVariables,
            signedNondet,
            threadLoops,
            threadLoopsNext,
            validateParse,
            validatePc);
    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    inputCfa = pInputCfa;

    InputRejection.handleRejections(logger, options, inputCfa);
    options.handleOptionWarnings(logger);

    binaryExpressionBuilder = new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger);

    threads = ThreadBuilder.createThreads(options, inputCfa);
    ImmutableSet<CVariableDeclaration> globalVars =
        CFAUtils.getGlobalVariableDeclarations(inputCfa);
    substitutions =
        MPORSubstitutionBuilder.buildSubstitutions(
            options, globalVars, threads, binaryExpressionBuilder);
  }

  /** Use this constructor only for test purposes. */
  private MPORAlgorithm(MPOROptions pOptions, LogManager pLogManager, CFA pInputCfa) {
    options = pOptions;
    cpa = null;
    config = null;
    logger = pLogManager;
    shutdownNotifier = null;
    inputCfa = pInputCfa;

    InputRejection.handleRejections(logger, options, inputCfa);

    binaryExpressionBuilder = new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger);

    threads = ThreadBuilder.createThreads(options, inputCfa);
    ImmutableSet<CVariableDeclaration> globalVariableDeclarations =
        CFAUtils.getGlobalVariableDeclarations(inputCfa);
    substitutions =
        MPORSubstitutionBuilder.buildSubstitutions(
            options, globalVariableDeclarations, threads, binaryExpressionBuilder);
  }

  public static MPORAlgorithm testInstance(
      MPOROptions pOptions, LogManager pLogManager, CFA pInputCfa) {

    return new MPORAlgorithm(pOptions, pLogManager, pInputCfa);
  }
}

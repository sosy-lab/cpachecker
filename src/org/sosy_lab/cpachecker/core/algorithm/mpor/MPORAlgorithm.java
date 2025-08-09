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
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.formatting.ClangFormatStyle;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.formatting.ClangFormatter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitution;
import org.sosy_lab.cpachecker.core.algorithm.mpor.substitution.MPORSubstitutionBuilder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.MPORThread;
import org.sosy_lab.cpachecker.core.algorithm.mpor.thread.ThreadBuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
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
      description = "allow writing pointer variables? false may help memory safety tasks.")
  private boolean allowPointerWrites = true;

  @Option(
      description =
          "merge statements between __VERIFIER_atomic_begin and __VERIFIER_atomic_end via gotos?"
              + " setting this to false does not model the input programs behavior correctly.")
  private boolean atomicBlockMerge = true;

  @Option(secure = true, description = "the encoding of the partial order reduction bit vectors.")
  // using optional for @Options is not allowed, unfortunately...
  private BitVectorEncoding bitVectorEncoding = BitVectorEncoding.NONE;

  @Option(
      secure = true,
      description =
          "prune and simplify bit vector evaluation expressions based on the previous bit vector "
              + " assignments? true -> (should) improve verification performance")
  private boolean bitVectorEvaluationPrune = false;

  @Option(
      secure = true,
      description =
          "add partial order reduction (bit vectors storing global variable) in the"
              + " sequentialization to reduce the state space?")
  // using optional for @Options is not allowed, unfortunately...
  private boolean bitVectorReduction = false;

  @Option(
      secure = true,
      description =
          "include comments with explaining trivia in the sequentialization? true ->"
              + " bigger file size")
  private boolean comments = false;

  @Option(
      secure = true,
      description =
          "adds execution orders via assumptions for non-conflicting threads to reduce the state"
              + " space. it takes the previous and current thread and checks if they commute from"
              + " the current location")
  private boolean conflictReduction = false;

  @Option(
      description =
          "make labels for thread statements consecutive? i.e. 0 to n - 1 where n is the"
              + " number of statements. disabling may result in first statements being "
              + " unreachable.")
  private boolean consecutiveLabels = true;

  @Option(
      secure = true,
      description =
          "defines the syntax in which the last_thread is chosen to check for conflicts."
              + " may slow down or improve performance, depending on the verifier.")
  private MultiControlStatementEncoding controlEncodingConflict =
      MultiControlStatementEncoding.NONE;

  @Option(
      secure = true,
      description =
          "defines the syntax in which the next statement of a thread simulation is chosen."
              + " may slow down or improve performance, depending on the verifier.")
  private MultiControlStatementEncoding controlEncodingStatement =
      MultiControlStatementEncoding.SWITCH_CASE;

  @Option(
      secure = true,
      description =
          "defines the syntax in which the next thread executing a statement is chosen."
              + " may slow down or improve performance, depending on the verifier.")
  private MultiControlStatementEncoding controlEncodingThread = MultiControlStatementEncoding.NONE;

  @Option(
      secure = true,
      description = "format the output sequentialized C program using clang-format?")
  private boolean formatCode = true;

  @Option(
      secure = true,
      description = "set the C formatting style preset used by clang-format, if enabled.")
  private ClangFormatStyle formatStyle = ClangFormatStyle.WEBKIT;

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
          "assign K only when the respective thread is active i.e. lazily?"
              + " may slow down or improve performance, depending on the verifier.")
  private boolean kAssignLazy;

  @Option(
      secure = true,
      description =
          "bound K to the number of statements in a thread simulation via assumptions?"
              + " may slow down or improve performance, depending on the verifier.")
  private boolean kBound;

  @Option(
      secure = true,
      description =
          "ignore K == 0 if the current thread is not in a conflict with any other thread?"
              + " reduces the amount of interleavings.")
  private boolean kIgnoreZeroReduction;

  // TODO kThreadSeparate
  //  use separate k for each thread (with k sum > 0), or one for each thread (should be used when
  //  loopIterations > 0 to prevent infinite k = 0 interleaving)

  @Option(
      secure = true,
      description =
          "include CPAchecker license header in the sequentialization? true -> bigger file size")
  private boolean license = false;

  // TODO POR is currently not secure because we assume pointer parameters that are assigned global
  //  variable addresses to commute
  @Option(
      description =
          "add partial order reduction (linking commuting statements via goto) in the "
              + "sequentialization to reduce the state space?")
  private boolean linkReduction = true;

  @Option(
      description =
          "when the loopIterations are finite and the number of statements as the sole "
              + " nondeterminismSource, the last loop iteration only executes the main thread.")
  private boolean loopFiniteMainThreadEnd = false;

  @Option(
      description =
          "the number of loop iterations to perform thread simulations. use 0 for an infinite loop"
              + " (while (1)). any number other than 0 is unsound, because the entire state space"
              + " is not searched. when finite with the number of statements as the "
              + " nondeterminismSource, the search always ends on the main thread.")
  private int loopIterations = 0;

  @Option(
      secure = true,
      description =
          "the source(s) of nondeterminism in the sequentialization. may slow down or improve"
              + " performance, depending on the verifier.")
  private NondeterminismSource nondeterminismSource = NondeterminismSource.NUM_STATEMENTS;

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

  @Option(
      secure = true,
      description = "prune empty statements (with only pc writes) from the sequentialization?")
  private boolean pruneEmptyStatements = true;

  @Option(
      secure = true,
      description =
          "how to determine if statements commute. distinguishing between global "
              + " variable reads and writes, not just accesses, reduces the state space more but"
              + "  may not be faster due to evaluation overhead.")
  private ReductionMode reductionMode = ReductionMode.NONE;

  // TODO also add option for scalar / array bit vectors
  @Option(
      secure = true,
      description =
          "use separate int values (scalars) for tracking thread pcs instead of"
              + " int arrays? may slow down or improve verification depending on the verifier and"
              + " input program")
  private boolean scalarPc = true;

  @Option(
      secure = true,
      description =
          "include additional reach_error marking sequentialization locations only reachable when"
              + " transformation is erroneous?")
  private boolean sequentializationErrors = false;

  @Option(secure = true, description = "use shortened variable names, e.g. THREAD0 -> T0")
  private boolean shortVariableNames = true;

  @Option(
      secure = true,
      description =
          "use signed __VERIFIER_nondet_int() instead of unsigned __VERIFIER_nondet_uint()?"
              + " in tests, signed generally slowed down verification performance.")
  private boolean signedNondet = false;

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
    String program = sequentialization.toString();
    String formattedProgram =
        options.formatCode ? ClangFormatter.format(program, options.formatStyle, logger) : program;
    SeqWriter seqWriter =
        new SeqWriter(shutdownNotifier, logger, outputFileName, inputCfa.getFileNames(), options);
    seqWriter.write(formattedProgram);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  /** Creates a {@link Sequentialization} based on this instance, necessary for test purposes. */
  public Sequentialization buildSequentialization(String pInputFileName, String pOutputFileName)
      throws UnrecognizedCodeException {

    return new Sequentialization(
        substitutions,
        options,
        pInputFileName,
        pOutputFileName,
        binaryExpressionBuilder,
        shutdownNotifier,
        logger);
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
      CFA pInputCfa,
      MPOROptions pOptions)
      throws InvalidConfigurationException {

    // the config can be null when unit testing
    if (pConfiguration != null) {
      pConfiguration.inject(this);
    }

    // the options are not null when unit testing
    if (pOptions != null) {
      options = pOptions;
    } else {
      options =
          new MPOROptions(
              allowPointerWrites,
              atomicBlockMerge,
              bitVectorEncoding,
              bitVectorEvaluationPrune,
              bitVectorReduction,
              comments,
              conflictReduction,
              consecutiveLabels,
              controlEncodingConflict,
              controlEncodingStatement,
              controlEncodingThread,
              formatCode,
              formatStyle,
              inputFunctionDeclarations,
              inputTypeDeclarations,
              kAssignLazy,
              kBound,
              kIgnoreZeroReduction,
              license,
              linkReduction,
              loopFiniteMainThreadEnd,
              loopIterations,
              nondeterminismSource,
              outputMetadata,
              outputPath,
              overwriteFiles,
              pruneEmptyStatements,
              reductionMode,
              scalarPc,
              sequentializationErrors,
              shortVariableNames,
              signedNondet,
              validateParse,
              validatePc);
    }

    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    inputCfa = pInputCfa;

    options.handleOptionRejections(logger);
    options.handleOptionWarnings(logger);
    InputRejection.handleRejections(logger, inputCfa);

    binaryExpressionBuilder = new CBinaryExpressionBuilder(inputCfa.getMachineModel(), logger);

    threads = ThreadBuilder.createThreads(options, inputCfa);
    ImmutableSet<CVariableDeclaration> globalVariableDeclarations =
        CFAUtils.getGlobalVariableDeclarations(inputCfa);
    substitutions =
        MPORSubstitutionBuilder.buildSubstitutions(
            options, globalVariableDeclarations, threads, binaryExpressionBuilder, logger);
  }

  public static MPORAlgorithm testInstance(
      LogManager pLogManager, CFA pInputCfa, MPOROptions pOptions)
      throws InvalidConfigurationException {

    return new MPORAlgorithm(null, null, pLogManager, null, pInputCfa, pOptions);
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.nio.file.Path;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.mpor.input_rejection.InputRejection;
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.MPORWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.cwriter.ClangFormatStyle;

/**
 * The Modular Partial Order Reduction (MPOR) algorithm produces a sequentialization of a concurrent
 * C program. The algorithm contains options that allow both static and dynamic reductions in the
 * state space. Sequentializations can be given to any verifier capable of verifying sequential C
 * programs, hence modular.
 */
@Options(prefix = "analysis.algorithm.MPOR")
@SuppressWarnings("unused") // this is necessary because we don't use the cpa and config
public class MPORAlgorithm implements Algorithm /* TODO statistics? */ {

  // using Optional for @Option is not allowed, so we use 'NONE' for enums that can be disabled.

  @Option(secure = true, description = "allow input programs that write pointer variables?")
  private boolean allowPointerWrites = true;

  @Option(
      description =
          "merge statements between __VERIFIER_atomic_begin() and __VERIFIER_atomic_end() via goto?"
              + " setting this to false does not model the input programs behavior correctly.")
  private boolean atomicBlockMerge = true;

  @Option(secure = true, description = "the encoding of the partial order reduction bit vectors.")
  private BitVectorEncoding bitVectorEncoding = BitVectorEncoding.NONE;

  @Option(
      secure = true,
      description =
          "the style preset used by clang-format to format the output program. use NONE to disable"
              + " formatting.")
  private ClangFormatStyle clangFormatStyle = ClangFormatStyle.WEBKIT;

  @Option(
      secure = true,
      description = "include comments with trivia explaining the sequentialization?")
  private boolean comments = false;

  @Option(
      description =
          "make labels for thread statements consecutive? i.e. 0 to n - 1 where n is the number of"
              + " statements. disabling may result in first statement being unreachable.")
  private boolean consecutiveLabels = true;

  @Option(
      secure = true,
      description =
          "defines the syntax in which the next statement of a thread simulation is chosen.")
  private MultiControlStatementEncoding controlEncodingStatement =
      MultiControlStatementEncoding.SWITCH_CASE;

  @Option(
      secure = true,
      description = "defines the syntax in which the next thread executing a statement is chosen.")
  private MultiControlStatementEncoding controlEncodingThread = MultiControlStatementEncoding.NONE;

  @Option(
      description =
          "include original function declarations from input file? including them may result in"
              + " unsound analysis (e.g. false alarms for CBMC + ignored function calls through"
              + " function pointers for CPAchecker)")
  private boolean inputFunctionDeclarations = false;

  // TODO make this secure by checking if all types for all variables are included.
  //  note that this is probably a lot of work (recursive type checking, and the only benefit would
  //  be that the output programs are smaller in size.
  @Option(description = "include original type declarations from input file?")
  private boolean inputTypeDeclarations = true;

  @Option(
      secure = true,
      description = "include CPAchecker license header in the sequentialization?")
  private boolean license = false;

  @Option(
      secure = true,
      description = "link commuting statements via goto to reduce the state space?")
  private boolean linkReduction = true;

  @Option(
      description =
          "the number of loop iterations to perform thread simulations. use 0 for an infinite loop"
              + " (while (1)). any number other than 0 is unsound, because the entire state space"
              + " is not searched.")
  private int loopIterations = 0;

  @Option(
      secure = true,
      description =
          "unroll the loop used for thread simulation? not unrolling loops can be unsound, e.g. for"
              + " CBMC. only use with loopIterations > 0.")
  private boolean loopUnrolling = false;

  @Option(
      secure = true,
      description =
          "removes backward goto, i.e. jumping to a line higher up in the program, by reordering"
              + " statements. only works for if-else constructs, not loops. use noBackwardLoopGoto"
              + " to ensure that backward loop goto are removed.")
  private boolean noBackwardGoto = true;

  @Option(
      secure = true,
      description =
          "removes backward goto from loops. this option is independent from noBackwardGoto.")
  private boolean noBackwardLoopGoto = true;

  @Option(
      secure = true,
      description =
          "use signed __VERIFIER_nondet_int() instead of unsigned __VERIFIER_nondet_uint()?")
  private boolean nondeterminismSigned = false;

  @Option(secure = true, description = "the source(s) of nondeterminism in the sequentialization.")
  private NondeterminismSource nondeterminismSource = NondeterminismSource.NUM_STATEMENTS;

  @Option(
      secure = true,
      description =
          "create additional output file with metadata such as input file(s) and algorithm"
              + " options?")
  private boolean outputMetadata = true;

  @Option(
      secure = true,
      description =
          "the file name for the sequentialization and metadata. uses the first input file name as"
              + " default.")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate outputPath = PathTemplate.ofFormatString(SeqToken.MPOR_PREFIX + "%s");

  @Option(secure = true, description = "export the sequentialized program in a .i file?")
  private boolean outputProgram = true;

  @Option(
      secure = true,
      description = "overwrite files in the ./output directory when creating sequentializations?")
  private boolean overwriteFiles = true;

  @Option(
      secure = true,
      description =
          "prune and simplify bit vector evaluation expressions based on perfect knowledge? e.g."
              + " if it is known that the left hand side in an & expression is 0, then the entire"
              + " evaluation can be pruned.")
  private boolean pruneBitVectorEvaluations = false;

  @Option(
      secure = true,
      description = "prune empty statements (with only pc writes) from the sequentialization?")
  private boolean pruneEmptyStatements = true;

  @Option(
      secure = true,
      description =
          "only bit vectors for memory locations that are reachable for a thread are included,"
              + " reducing the amount of variables, evaluations, and writes in the output program.")
  private boolean pruneSparseBitVectors = false;

  @Option(
      secure = true,
      description =
          "bit vectors are only written to 0 if the corresponding memory location is not reachable"
              + " anymore, removing all unnecessary writes to 1 in the output program.")
  private boolean pruneSparseBitVectorWrites = false;

  // TODO not sound, resulted in wrong proofs
  @Option(
      description =
          "ignore that the current thread should not execute if it is not in conflict with any"
              + " other thread? only works when nondeterminismSource contains NUM_STATEMENTS.")
  private boolean reduceIgnoreSleep = false;

  @Option(
      secure = true,
      description = "enforce an execution order when the current and previous thread commute?")
  private boolean reduceLastThreadOrder = false;

  @Option(
      secure = true,
      description =
          "continue executing the current thread until it is in conflict with at least another"
              + " thread?")
  private boolean reduceUntilConflict = false;

  @Option(
      secure = true,
      description =
          "how to determine if two threads commute from a location. READ_AND_WRITE reduces the"
              + " state space more than ACCESS_ONLY, but introduces additional overhead (i.e."
              + " number of variables, assignments, and expression evaluations)")
  private ReductionMode reductionMode = ReductionMode.NONE;

  @Option(
      description =
          "if both reduceLastThreadOrder and reduceUntilConflict are enabled, define the order"
              + " in which their statements are placed in the output program.")
  private ReductionOrder reductionOrder = ReductionOrder.NONE;

  @Option(
      secure = true,
      description =
          "use separate int values (scalars) for tracking thread pcs instead of int arrays?")
  private boolean scalarPc = true;

  @Option(secure = true, description = "use shortened variable names, e.g. THREAD0 -> T0")
  private boolean shortVariableNames = true;

  @Option(
      secure = true,
      description =
          "check if all goto statements jump forward, i.e. to a higher line of code in the"
              + " program?")
  private boolean validateNoBackwardGoto = true;

  @Option(
      description =
          "check if CPAchecker can parse sequentialization? note that it may take several seconds"
              + " to parse a program")
  private boolean validateParse = true;

  @Option(
      description =
          "check if all label pc (except initial) are target pc and all target pc (except "
              + " termination) are label pc within a thread simulation?")
  private boolean validatePc = true;

  private final MPOROptions options;

  @CanIgnoreReturnValue
  @Override
  public AlgorithmStatus run(@Nullable ReachedSet pReachedSet) throws CPAException {
    String sequentializedProgram = buildSequentializedProgram();
    MPORWriter.write(options, sequentializedProgram, cfa.getFileNames(), logger);
    return AlgorithmStatus.NO_PROPERTY_CHECKED;
  }

  public String buildSequentializedProgram() {
    // just use the first input file name for naming purposes
    Path firstInputFilePath = cfa.getFileNames().getFirst();
    String inputFileName = firstInputFilePath.toString();
    return Sequentialization.tryBuildProgramString(
        options, cfa, inputFileName, logger, shutdownNotifier);
  }

  private final ConfigurableProgramAnalysis cpa;

  private final LogManager logger;

  private final Configuration config;

  private final ShutdownNotifier shutdownNotifier;

  private final CFA cfa;

  public MPORAlgorithm(
      @Nullable ConfigurableProgramAnalysis pCpa,
      @Nullable Configuration pConfiguration,
      LogManager pLogManager,
      ShutdownNotifier pShutdownNotifier,
      CFA pInputCfa,
      @Nullable MPOROptions pOptions)
      throws InvalidConfigurationException {

    // the config can be null when unit testing
    if (pConfiguration != null) {
      pConfiguration.inject(this);
    }

    // the options are not null when unit testing
    options =
        Objects.requireNonNullElseGet(
            pOptions,
            () ->
                new MPOROptions(
                    allowPointerWrites,
                    atomicBlockMerge,
                    bitVectorEncoding,
                    clangFormatStyle,
                    comments,
                    consecutiveLabels,
                    controlEncodingStatement,
                    controlEncodingThread,
                    inputFunctionDeclarations,
                    inputTypeDeclarations,
                    license,
                    linkReduction,
                    loopIterations,
                    loopUnrolling,
                    noBackwardGoto,
                    noBackwardLoopGoto,
                    nondeterminismSigned,
                    nondeterminismSource,
                    outputMetadata,
                    outputPath,
                    outputProgram,
                    overwriteFiles,
                    pruneBitVectorEvaluations,
                    pruneEmptyStatements,
                    pruneSparseBitVectors,
                    pruneSparseBitVectorWrites,
                    reduceIgnoreSleep,
                    reduceLastThreadOrder,
                    reduceUntilConflict,
                    reductionMode,
                    reductionOrder,
                    scalarPc,
                    shortVariableNames,
                    validateNoBackwardGoto,
                    validateParse,
                    validatePc));

    cpa = pCpa;
    config = pConfiguration;
    logger = pLogManager;
    shutdownNotifier = pShutdownNotifier;
    cfa = pInputCfa;

    options.handleOptionRejections(logger);
    InputRejection.handleRejections(logger, cfa);
  }

  public static MPORAlgorithm testInstance(
      LogManager pLogManager, CFA pInputCfa, MPOROptions pOptions)
      throws InvalidConfigurationException {

    return new MPORAlgorithm(null, null, pLogManager, null, pInputCfa, pOptions);
  }
}

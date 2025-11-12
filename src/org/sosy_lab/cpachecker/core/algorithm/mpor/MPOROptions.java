// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import com.google.common.collect.ImmutableMap;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;
import org.sosy_lab.cpachecker.util.cwriter.ClangFormatStyle;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Contains all {@link Option} fields used to adjust {@link MPORAlgorithm}. */
@Options(prefix = "analysis.algorithm.MPOR")
public class MPOROptions {

  // using Optional for @Option is not allowed, so we use 'NONE' for enums that can be disabled.

  @Option(secure = true, description = "allow input programs that write pointer variables?")
  private boolean allowPointerWrites = true;

  @Option(
      secure = true,
      description =
          "merge statements between __VERIFIER_atomic_begin() and __VERIFIER_atomic_end() via goto?"
              + " setting this to false does not model the input programs behavior correctly.")
  private boolean atomicBlockMerge = true;

  @Option(secure = true, description = "the encoding of the partial order reduction bit vectors.")
  private BitVectorEncoding bitVectorEncoding = BitVectorEncoding.NONE;

  @Option(
      secure = false,
      description =
          "the style preset used by clang-format to format the output program. use NONE to disable"
              + " formatting.")
  private ClangFormatStyle clangFormatStyle = ClangFormatStyle.WEBKIT;

  @Option(
      secure = true,
      description = "include comments with trivia explaining the sequentialization?")
  private boolean comments = false;

  @Option(
      secure = true,
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
      secure = true,
      description =
          "include original function declarations from input file? including them may result in"
              + " unsound analysis (e.g. false alarms for CBMC + ignored function calls through"
              + " function pointers for CPAchecker)")
  private boolean inputFunctionDeclarations = false;

  @Option(
      secure = true,
      description =
          "include original type declarations from input file? disabling may result in parse errors"
              + " for the output program.")
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
      secure = true,
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

  @Option(
      secure = true,
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
      secure = true,
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
      secure = true,
      description =
          "check if CPAchecker can parse sequentialization? note that it may take several seconds"
              + " to parse a program")
  private boolean validateParse = true;

  @Option(
      secure = true,
      description =
          "check if all label pc (except initial) are target pc and all target pc (except "
              + " termination) are label pc within a thread simulation?")
  private boolean validatePc = true;

  /**
   * Returns an instance of {@link MPOROptions} with the {@link Option}s set based on {@code
   * pConfig}.
   */
  public MPOROptions(Configuration pConfig, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    handleOptionRejections(pLogger);
  }

  /** Returns an instance of {@link MPOROptions} with all standard {@link Option}s. */
  public static MPOROptions getDefaultTestInstance() throws InvalidConfigurationException {
    return new MPOROptions(
        TestDataTools.configurationForTest().build(), LogManager.createTestLogManager());
  }

  /**
   * Maps all {@link Option} field names in this class to their actual values. Can be used to export
   * to {@code .yml}.
   */
  public ImmutableMap<String, Object> buildAlgorithmOptionMap(MPOROptions pOptions)
      throws IllegalAccessException {

    ImmutableMap.Builder<String, Object> rMap = ImmutableMap.builder();
    for (Field field : pOptions.getClass().getDeclaredFields()) {
      rMap.put(field.getName(), field.get(pOptions));
    }
    return rMap.buildOrThrow();
  }

  // Rejection =====================================================================================

  /**
   * Rejects specific option values or combinations of option values that are not allowed. Throws an
   * {@link AssertionError} if a rejection occurs.
   */
  private void handleOptionRejections(LogManager pLogger) {
    if (controlEncodingStatement.equals(MultiControlStatementEncoding.NONE)) {
      handleOptionRejection(
          pLogger, "controlEncodingStatement cannot be %s", MultiControlStatementEncoding.NONE);
    }
    if (!linkReduction) {
      if (bitVectorEncoding.isEnabled()) {
        handleOptionRejection(
            pLogger, "bitVectorEncoding cannot be set when linkReduction is disabled.");
      }
      if (reduceLastThreadOrder) {
        handleOptionRejection(
            pLogger, "reduceLastThreadOrder cannot be enabled when linkReduction is disabled");
      }
      if (reduceUntilConflict) {
        handleOptionRejection(
            pLogger, "reduceUntilConflict cannot be enabled when linkReduction is disabled.");
      }
    }
    if (loopIterations < 0) {
      handleOptionRejection(
          pLogger, "loopIterations must be 0 or greater, cannot be %s", loopIterations);
    }
    if (loopIterations == 0) {
      if (loopUnrolling) {
        handleOptionRejection(pLogger, "loopUnrolling can only be enabled when loopIterations > 0");
      }
    }
    if (reduceLastThreadOrder && reduceUntilConflict) {
      if (!reductionOrder.isEnabled()) {
        handleOptionRejection(
            pLogger,
            "both reduceLastThreadOrder and reduceUntilConflict are enabled, but no reductionOrder"
                + " is specified.");
      }
    }
    if (!noBackwardGoto) {
      if (validateNoBackwardGoto) {
        handleOptionRejection(
            pLogger, "validateNoBackwardGoto is enabled, but noBackwardGoto is disabled.");
      }
    }
    if (!nondeterminismSource.isNextThreadNondeterministic()) {
      if (controlEncodingThread.isEnabled()) {
        handleOptionRejection(
            pLogger,
            "controlEncodingThread is set, but nondeterminismSource does not contain NEXT_THREAD.");
      }
    }
    if (!nondeterminismSource.isNumStatementsNondeterministic()) {
      if (reduceIgnoreSleep) {
        handleOptionRejection(
            pLogger,
            "reduceIgnoreSleep cannot be enabled when nondeterminismSource does not contain"
                + " NUM_STATEMENTS");
      }
    }
    if (pruneBitVectorEvaluations) {
      if (!isAnyReductionEnabled()) {
        handleOptionRejection(
            pLogger, "pruneBitVectorEvaluations is enabled, but no reduce* option is enabled.");
      }
      if (!bitVectorEncoding.isEnabled()) {
        handleOptionRejection(
            pLogger, "pruneBitVectorEvaluations is enabled, but no bitVectorEncoding is set.");
      }
    }
    if (pruneSparseBitVectors) {
      if (!bitVectorEncoding.isSparse) {
        handleOptionRejection(
            pLogger, "pruneSparseBitVectors is enabled, but bitVectorEncoding is not sparse.");
      }
      if (reduceIgnoreSleep) {
        handleOptionRejection(
            pLogger, "pruneSparseBitVectors cannot be enabled when reduceIgnoreSleep is enabled.");
      }
      if (reduceLastThreadOrder) {
        handleOptionRejection(
            pLogger,
            "pruneSparseBitVectors cannot be enabled when reduceLastThreadOrder is enabled.");
      }
    }
    if (pruneSparseBitVectorWrites) {
      if (!bitVectorEncoding.isSparse) {
        handleOptionRejection(
            pLogger, "pruneSparseBitVectorWrites is enabled, but bitVectorEncoding is not SPARSE.");
      }
    }
    if (isAnyReductionEnabled()) {
      if (!reductionMode.isEnabled()) {
        handleOptionRejection(
            pLogger, "a reduce* option is enabled, but reductionMode is not set.");
      }
      if (!bitVectorEncoding.isEnabled()) {
        handleOptionRejection(
            pLogger, "a reduce* option is enabled, but bitVectorEncoding is not set.");
      }
    } else {
      if (reductionMode.isEnabled()) {
        handleOptionRejection(pLogger, "reductionMode is set, but no reduce* option is enabled");
      }
      if (bitVectorEncoding.isEnabled()) {
        handleOptionRejection(
            pLogger, "bitVectorEncoding is set, but no reduce* option is enabled");
      }
    }
  }

  private void handleOptionRejection(LogManager pLogger, Object... pMessage) {
    pLogger.log(Level.SEVERE, pMessage);
    throw new AssertionError(Arrays.toString(pMessage));
  }

  // boolean helpers ===============================================================================

  public boolean isAnyReductionEnabled() {
    return reduceIgnoreSleep || reduceLastThreadOrder || reduceUntilConflict;
  }

  public boolean isThreadCountRequired() {
    return nondeterminismSource.equals(NondeterminismSource.NUM_STATEMENTS) && loopIterations == 0;
  }

  public boolean isThreadLabelRequired() {
    // only needed if the loop is finite i.e. not 0, otherwise just use continue;
    if (loopIterations > 0 && !loopUnrolling) {
      // only use with NUM_STATEMENTS nondeterminism, for NEXT_THREAD, just continue;
      if (!nondeterminismSource.isNextThreadNondeterministic()) {
        // in switch case, just use break; instead of continue;
        if (!controlEncodingStatement.equals(MultiControlStatementEncoding.SWITCH_CASE)) {
          return true;
        }
      }
    }
    return false;
  }

  // public getters ================================================================================

  public boolean allowPointerWrites() {
    return allowPointerWrites;
  }

  public boolean atomicBlockMerge() {
    return atomicBlockMerge;
  }

  public BitVectorEncoding bitVectorEncoding() {
    return bitVectorEncoding;
  }

  public ClangFormatStyle clangFormatStyle() {
    return clangFormatStyle;
  }

  public boolean comments() {
    return comments;
  }

  public boolean consecutiveLabels() {
    return consecutiveLabels;
  }

  public MultiControlStatementEncoding controlEncodingStatement() {
    return controlEncodingStatement;
  }

  public MultiControlStatementEncoding controlEncodingThread() {
    return controlEncodingThread;
  }

  public boolean inputFunctionDeclarations() {
    return inputFunctionDeclarations;
  }

  public boolean inputTypeDeclarations() {
    return inputTypeDeclarations;
  }

  public boolean license() {
    return license;
  }

  public boolean linkReduction() {
    return linkReduction;
  }

  public int loopIterations() {
    return loopIterations;
  }

  public boolean loopUnrolling() {
    return loopUnrolling;
  }

  public boolean noBackwardGoto() {
    return noBackwardGoto;
  }

  public boolean noBackwardLoopGoto() {
    return noBackwardLoopGoto;
  }

  public boolean nondeterminismSigned() {
    return nondeterminismSigned;
  }

  public NondeterminismSource nondeterminismSource() {
    return nondeterminismSource;
  }

  public boolean outputMetadata() {
    return outputMetadata;
  }

  public PathTemplate outputPath() {
    return outputPath;
  }

  public boolean outputProgram() {
    return outputProgram;
  }

  public boolean pruneBitVectorEvaluations() {
    return pruneBitVectorEvaluations;
  }

  public boolean pruneEmptyStatements() {
    return pruneEmptyStatements;
  }

  public boolean pruneSparseBitVectors() {
    return pruneSparseBitVectors;
  }

  public boolean pruneSparseBitVectorWrites() {
    return pruneSparseBitVectorWrites;
  }

  public boolean reduceIgnoreSleep() {
    return reduceIgnoreSleep;
  }

  public boolean reduceLastThreadOrder() {
    return reduceLastThreadOrder;
  }

  public boolean reduceUntilConflict() {
    return reduceUntilConflict;
  }

  public ReductionMode reductionMode() {
    return reductionMode;
  }

  public ReductionOrder reductionOrder() {
    return reductionOrder;
  }

  public boolean scalarPc() {
    return scalarPc;
  }

  public boolean shortVariableNames() {
    return shortVariableNames;
  }

  public boolean validateNoBackwardGoto() {
    return noBackwardGoto;
  }

  public boolean validateParse() {
    return validateParse;
  }

  public boolean validatePc() {
    return validatePc;
  }
}

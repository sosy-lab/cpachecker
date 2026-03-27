// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.MultiSelectionStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.SeqBitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.util.cwriter.ClangFormatStyle;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

/** Contains all {@link Option} fields used to adjust {@link MporPreprocessingAlgorithm}. */
@Options(prefix = "analysis.algorithm.MPOR")
public class MPOROptions {

  // using Optional for @Option is not allowed, so we use 'NONE' for enums that can be disabled.

  @Option(secure = true, description = "Allow input programs that write pointer variables?")
  private boolean allowPointerWrites = true;

  @Option(
      secure = true,
      description =
          "Merge statements between __VERIFIER_atomic_begin() and __VERIFIER_atomic_end() with goto"
              + " statements? Setting this to false does not model the input programs behavior"
              + " correctly.")
  private boolean atomicBlockMerge = true;

  @Option(
      secure = true,
      description =
          "The encoding of bit vectors that are used to instrument the output program with partial"
              + " order reduction.")
  private SeqBitVectorEncoding bitVectorEncoding = SeqBitVectorEncoding.NONE;

  @Option(
      secure = false,
      description =
          "The style preset used by clang-format to format the output program. Use NONE to disable"
              + " formatting.")
  private ClangFormatStyle clangFormatStyle = ClangFormatStyle.WEBKIT;

  @Option(
      secure = true,
      description =
          "Include comments in the output program that explain the sequentialization and its"
              + " components?")
  private boolean comments = false;

  @Option(
      secure = true,
      description =
          "Make labels for thread statements consecutive, i.e., in [1; N] where N is the number of"
              + " statements? Disabling this option may result in the first statement being"
              + " unreachable, but can be useful for debugging.")
  private boolean consecutiveLabels = true;

  @Option(
      secure = true,
      description =
          "Defines the program syntax in which the next statement(s) of a thread simulation is"
              + " selected.")
  private MultiSelectionStatementEncoding controlEncodingStatement =
      MultiSelectionStatementEncoding.SWITCH_CASE;

  @Option(
      secure = true,
      description =
          "Defines the program syntax in which the next thread executing a statement (or multiple"
              + " statements) is selected.")
  private MultiSelectionStatementEncoding controlEncodingThread =
      MultiSelectionStatementEncoding.NONE;

  @Option(
      secure = true,
      description =
          "Include original function declarations from the input program? Including them may result"
              + " in unsound analysis (e.g. false alarms for CBMC, or ignored function calls"
              + " through function pointers for CPAchecker).")
  private boolean inputFunctionDeclarations = false;

  @Option(
      secure = true,
      description =
          "Include original type declarations from the input file? Disabling may result in parse"
              + " errors for the output program.")
  private boolean inputTypeDeclarations = true;

  @Option(
      secure = true,
      description = "Link commuting statements with goto statements to reduce the state space?")
  private boolean linkReduction = true;

  @Option(
      secure = true,
      description =
          "The number of loop iterations to perform thread simulations. Use 0 for an infinite loop"
              + " (while (1)). Any number other than 0 is unsound, because the entire state space"
              + " is not searched.")
  private int loopIterations = 0;

  @Option(
      secure = true,
      description =
          "Unroll the loop used for the thread simulation? Disabling this option can be unsound for"
              + " CBMC. Enabling this option is only possible with loopIterations > 0.")
  private boolean loopUnrolling = false;

  @Option(
      secure = true,
      description =
          "Removes backward goto, i.e., jumping to a line higher up in the program, by reordering"
              + " statements. Only works for if-else constructs, not loops. Use noBackwardLoopGoto"
              + " to ensure that backward goto from loops are removed too.")
  private boolean noBackwardGoto = true;

  @Option(
      secure = true,
      description =
          "Removes backward goto from loops. This option is independent from noBackwardGoto.")
  private boolean noBackwardLoopGoto = true;

  @Option(
      secure = true,
      description =
          "Use signed __VERIFIER_nondet_int() instead of unsigned __VERIFIER_nondet_uint()?")
  private boolean nondeterminismSigned = false;

  @Option(secure = true, description = "The source(s) of nondeterminism in the sequentialization.")
  private NondeterminismSource nondeterminismSource = NondeterminismSource.NUM_STATEMENTS;

  @Option(
      secure = true,
      description =
          "Prune and simplify bit vector evaluation expressions based on perfect knowledge? E.g.,"
              + " if it is known that the left hand side in an & expression is 0, then the entire"
              + " evaluation can be pruned.")
  private boolean pruneBitVectorEvaluations = false;

  @Option(
      secure = true,
      description =
          "Prune empty statements with no semantics from the input program from the"
              + " sequentialization?")
  private boolean pruneEmptyStatements = true;

  @Option(
      secure = true,
      description =
          "Prune the sparse bit vectors so that the scalar bool variables for the bit vector are"
              + " only included if their respective memory location is reachable for their"
              + " corresponding thread. Reduces the amount of variables, evaluations, and writes in"
              + " the output program.")
  private boolean pruneSparseBitVectors = false;

  @Option(
      secure = true,
      description =
          "Prune unnecessary sparse bit vector writes to 1 in the output program? Sparse bit"
              + " vectors for the reachability of a memory location are initialized to 1, and only"
              + " written to 0 if the corresponding memory location is not reachable anymore from a"
              + " given location, so any additional write to 1 is unnecessary and can be pruned"
              + " with this option.")
  private boolean pruneSparseBitVectorWrites = false;

  @Option(
      secure = true,
      description =
          "Ignore that the current thread should not execute if it is not in conflict with any"
              + " other thread?")
  private boolean reduceIgnoreSleep = false;

  @Option(
      secure = true,
      description = "Enforce an execution order if the current and previous thread commute?")
  private boolean reduceLastThreadOrder = false;

  @Option(
      secure = true,
      description =
          "Continue executing the current thread if it is the only active thread. This option"
              + " utilizes a thread_count ghost variable which is initialized to 1 and incremented"
              + " for each created thread and decremented for each terminated thread. These"
              + " increments and decrement are placed inside a possibly infinite loop, so when"
              + " analyzing for overflows, it may be more efficient to disable this option.")
  private boolean reduceSingleActiveThread = true;

  @Option(
      secure = true,
      description =
          "Continue executing the current thread until it is in conflict with at least another"
              + " thread?")
  private boolean reduceUntilConflict = false;

  @Option(
      secure = true,
      description =
          "How to determine if two threads commute from a location. READ_AND_WRITE reduces the"
              + " state space more than ACCESS_ONLY, but introduces additional overhead (i.e.,"
              + " number of variables, assignments, and expression evaluations)")
  private ReductionMode reductionMode = ReductionMode.NONE;

  @Option(
      secure = true,
      description =
          "Use separate int values (scalars) for tracking thread program counters instead of a"
              + " single int array?")
  private boolean scalarPc = true;

  @Option(secure = true, description = "Use shortened variable names, e.g., THREAD0 -> T0.")
  private boolean shortVariableNames = true;

  @Option(
      secure = true,
      description =
          "Check if all goto statements jump forward, i.e., to a higher line of code in the"
              + " program?")
  private boolean validateNoBackwardGoto = true;

  @Option(
      secure = true,
      description =
          "Check if CPAchecker can parse output program? Note that it may take several seconds"
              + " to parse a program.")
  private boolean validateParse = true;

  @Option(
      secure = true,
      description =
          "Check (1) if all label program counter (except initial program counters) are also"
              + " targeted and (2) that all target program counter (except terminating program"
              + " counters) are also labels within a thread simulation?")
  private boolean validatePc = true;

  /**
   * Returns an instance of {@link MPOROptions} with the {@link Option}s set based on {@code
   * pConfig}.
   */
  public MPOROptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    handleOptionRejections();
  }

  /** Returns an instance of {@link MPOROptions} with all standard {@link Option}s. */
  public static MPOROptions getDefaultTestInstance() throws InvalidConfigurationException {
    return new MPOROptions(TestDataTools.configurationForTest().build());
  }

  // Rejection =====================================================================================

  /**
   * Rejects specific option values or combinations of option values that are not allowed. Throws an
   * {@link AssertionError} if a rejection occurs.
   */
  private void handleOptionRejections() throws InvalidConfigurationException {
    if (controlEncodingStatement.equals(MultiSelectionStatementEncoding.NONE)) {
      throw new InvalidConfigurationException(
          String.format("controlEncodingStatement cannot be %s", controlEncodingStatement));
    }
    if (nondeterminismSource.isNextThreadNondeterministic()) {
      if (!controlEncodingThread.isEnabled()) {
        // if loopUnrolling is enabled, then choosing controlEncodingThread=NONE is allowed even
        // when nondeterminismSource contains NEXT_THREAD, because then there is no multi control
        // statement for next_thread in the main() function anyway
        if (!loopUnrolling) {
          throw new InvalidConfigurationException(
              String.format(
                  "controlEncodingThread cannot be %s when nondeterminismSource contains"
                      + " NEXT_THREAD",
                  controlEncodingThread));
        }
      }
    }
    if (controlEncodingThread.isEnabled()) {
      if (loopUnrolling) {
        throw new InvalidConfigurationException(
            String.format(
                "controlEncodingThread cannot be %s when loopUnrolling is enabled, because the"
                    + " controlEncodingThread is only used when all thread simulations are placed"
                    + " inside the main() function.",
                controlEncodingThread));
      }
    }
    if (!linkReduction) {
      if (bitVectorEncoding.isEnabled()) {
        throw new InvalidConfigurationException(
            "bitVectorEncoding cannot be set when linkReduction is disabled.");
      }
      if (reduceLastThreadOrder) {
        throw new InvalidConfigurationException(
            "reduceLastThreadOrder cannot be enabled when linkReduction is disabled");
      }
      if (reduceUntilConflict) {
        throw new InvalidConfigurationException(
            "reduceUntilConflict cannot be enabled when linkReduction is disabled.");
      }
    }
    if (loopIterations < 0) {
      throw new InvalidConfigurationException(
          String.format("loopIterations must be 0 or greater, cannot be %s", loopIterations));
    }
    if (loopIterations == 0) {
      if (loopUnrolling) {
        throw new InvalidConfigurationException(
            "loopUnrolling can only be enabled when loopIterations > 0");
      }
    }
    if (!noBackwardGoto) {
      if (validateNoBackwardGoto) {
        throw new InvalidConfigurationException(
            "validateNoBackwardGoto is enabled, but noBackwardGoto is disabled.");
      }
    }
    if (!nondeterminismSource.isNextThreadNondeterministic()) {
      if (controlEncodingThread.isEnabled()) {
        throw new InvalidConfigurationException(
            "controlEncodingThread is set, but nondeterminismSource does not contain NEXT_THREAD.");
      }
    }
    if (pruneBitVectorEvaluations) {
      if (!isAnyBitVectorReductionEnabled()) {
        throw new InvalidConfigurationException(
            "pruneBitVectorEvaluations is enabled, but no reduce* option is enabled.");
      }
      if (!bitVectorEncoding.isEnabled()) {
        throw new InvalidConfigurationException(
            "pruneBitVectorEvaluations is enabled, but no bitVectorEncoding is set.");
      }
    }
    if (pruneSparseBitVectors) {
      if (!bitVectorEncoding.isSparse) {
        throw new InvalidConfigurationException(
            "pruneSparseBitVectors is enabled, but bitVectorEncoding is not sparse.");
      }
      if (reduceIgnoreSleep) {
        throw new InvalidConfigurationException(
            "pruneSparseBitVectors cannot be enabled when reduceIgnoreSleep is enabled.");
      }
      if (reduceLastThreadOrder) {
        throw new InvalidConfigurationException(
            "pruneSparseBitVectors cannot be enabled when reduceLastThreadOrder is enabled.");
      }
    }
    if (pruneSparseBitVectorWrites) {
      if (!bitVectorEncoding.isSparse) {
        throw new InvalidConfigurationException(
            "pruneSparseBitVectorWrites is enabled, but bitVectorEncoding is not SPARSE.");
      }
    }
    if (isAnyBitVectorReductionEnabled()) {
      if (!reductionMode.isEnabled()) {
        throw new InvalidConfigurationException(
            "a reduce* option is enabled, but reductionMode is not set.");
      }
      if (!bitVectorEncoding.isEnabled()) {
        throw new InvalidConfigurationException(
            "a reduce* option is enabled, but bitVectorEncoding is not set.");
      }
    } else {
      if (reductionMode.isEnabled()) {
        throw new InvalidConfigurationException(
            "reductionMode is set, but no reduce* option is enabled");
      }
      if (bitVectorEncoding.isEnabled()) {
        throw new InvalidConfigurationException(
            "bitVectorEncoding is set, but no reduce* option is enabled");
      }
    }
  }

  // boolean helpers ===============================================================================

  public boolean isAnyBitVectorReductionEnabled() {
    return reduceIgnoreSleep || reduceLastThreadOrder || reduceUntilConflict;
  }

  public boolean isThreadLabelRequired() {
    // only needed if the loop is finite i.e. not 0, otherwise just use continue;
    if (loopIterations > 0 && !loopUnrolling) {
      // only use with NUM_STATEMENTS nondeterminism, for NEXT_THREAD, just continue;
      if (!nondeterminismSource.isNextThreadNondeterministic()) {
        // in switch case, just use break; instead of continue;
        if (!controlEncodingStatement.equals(MultiSelectionStatementEncoding.SWITCH_CASE)) {
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

  public SeqBitVectorEncoding bitVectorEncoding() {
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

  public MultiSelectionStatementEncoding controlEncodingStatement() {
    return controlEncodingStatement;
  }

  public MultiSelectionStatementEncoding controlEncodingThread() {
    return controlEncodingThread;
  }

  public boolean inputFunctionDeclarations() {
    return inputFunctionDeclarations;
  }

  public boolean inputTypeDeclarations() {
    return inputTypeDeclarations;
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

  public boolean reduceSingleActiveThread() {
    return reduceSingleActiveThread;
  }

  public boolean reduceUntilConflict() {
    return reduceUntilConflict;
  }

  public ReductionMode reductionMode() {
    return reductionMode;
  }

  public boolean scalarPc() {
    return scalarPc;
  }

  public boolean shortVariableNames() {
    return shortVariableNames;
  }

  public boolean validateNoBackwardGoto() {
    return validateNoBackwardGoto;
  }

  public boolean validateParse() {
    return validateParse;
  }

  public boolean validatePc() {
    return validatePc;
  }
}

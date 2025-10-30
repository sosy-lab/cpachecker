// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.ClangFormatStyle;
import org.sosy_lab.cpachecker.core.algorithm.mpor.output.MPORWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.custom_statements.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;

/**
 * For better overview so that {@link Option}s do not have to be accessed through {@link
 * MPORAlgorithm}.
 */
public record MPOROptions(
    boolean allowPointerWrites,
    boolean atomicBlockMerge,
    BitVectorEncoding bitVectorEncoding,
    ClangFormatStyle clangFormatStyle,
    boolean comments,
    boolean consecutiveLabels,
    MultiControlStatementEncoding controlEncodingStatement,
    MultiControlStatementEncoding controlEncodingThread,
    boolean inputFunctionDeclarations,
    boolean inputTypeDeclarations,
    boolean license,
    boolean linkReduction,
    int loopIterations,
    boolean loopUnrolling,
    boolean noBackwardGoto,
    boolean noBackwardLoopGoto,
    boolean nondeterminismSigned,
    NondeterminismSource nondeterminismSource,
    boolean outputMetadata,
    String outputPath,
    boolean outputProgram,
    boolean overwriteFiles,
    boolean pruneBitVectorEvaluations,
    boolean pruneEmptyStatements,
    boolean pruneSparseBitVectors,
    boolean pruneSparseBitVectorWrites,
    boolean reduceIgnoreSleep,
    boolean reduceLastThreadOrder,
    boolean reduceUntilConflict,
    ReductionMode reductionMode,
    ReductionOrder reductionOrder,
    boolean scalarPc,
    boolean shortVariableNames,
    boolean validateNoBackwardGoto,
    boolean validateParse,
    boolean validatePc) {

  public MPOROptions {

    checkCorrectParameterCount();
    checkEqualFieldNames();
  }

  public static MPOROptions getDefaultTestInstance() {
    // we don't want this to be a constant field so that we can compare all fields with @Options
    return new MPOROptions(
        true,
        true,
        BitVectorEncoding.NONE,
        ClangFormatStyle.WEBKIT,
        false,
        true,
        MultiControlStatementEncoding.SWITCH_CASE,
        MultiControlStatementEncoding.NONE,
        false,
        true,
        false,
        // linkReduction = true so that MemoryModel is created
        true,
        0,
        false,
        true,
        true,
        false,
        NondeterminismSource.NUM_STATEMENTS,
        true,
        MPORWriter.DEFAULT_OUTPUT_PATH,
        true,
        true,
        false,
        true,
        false,
        false,
        false,
        false,
        false,
        ReductionMode.NONE,
        ReductionOrder.NONE,
        true,
        true,
        true,
        true,
        true);
  }

  /** Returns a test instance where only the program customization, not output, can be specified. */
  public static MPOROptions testInstance(
      boolean pAllowPointerWrites,
      BitVectorEncoding pBitVectorEncoding,
      boolean pComments,
      MultiControlStatementEncoding pControlEncodingStatement,
      MultiControlStatementEncoding pControlEncodingThread,
      boolean pInputFunctionDeclarations,
      boolean pLicense,
      boolean pLinkReduction,
      int pLoopIterations,
      boolean pLoopUnrolling,
      boolean pNoBackwardGoto,
      boolean pNoBackwardLoopGoto,
      boolean pNondeterminismSigned,
      NondeterminismSource pNondeterminismSource,
      boolean pPruneBitVectorEvaluations,
      boolean pPruneSparseBitVectors,
      boolean pPruneSparseBitVectorWrites,
      boolean pReduceIgnoreSleep,
      boolean pReduceLastThreadOrder,
      boolean pReduceUntilConflict,
      ReductionMode pReductionMode,
      ReductionOrder pReductionOrder,
      boolean pScalarPc,
      boolean pShortVariableNames,
      boolean pValidateNoBackwardGoto) {

    return new MPOROptions(
        pAllowPointerWrites,
        // always merge atomic blocks because not doing so may add additional interleavings
        true,
        pBitVectorEncoding,
        // never format output code so that unit test is independent of clang-format
        ClangFormatStyle.NONE,
        pComments,
        // always use consecutive labels, disabling is only for debugging, not for release
        true,
        pControlEncodingStatement,
        pControlEncodingThread,
        pInputFunctionDeclarations,
        // always include type declarations at the moment, excluding them is unsafe
        true,
        pLicense,
        pLinkReduction,
        pLoopIterations,
        pLoopUnrolling,
        pNoBackwardGoto,
        pNoBackwardLoopGoto,
        pNondeterminismSigned,
        pNondeterminismSource,
        // never output for unit tests
        false,
        MPORWriter.DEFAULT_OUTPUT_PATH,
        false,
        false,
        pPruneBitVectorEvaluations,
        // always prune empty, disabling is only for debugging, not for release
        true,
        pPruneSparseBitVectors,
        pPruneSparseBitVectorWrites,
        pReduceIgnoreSleep,
        pReduceLastThreadOrder,
        pReduceUntilConflict,
        pReductionMode,
        pReductionOrder,
        pScalarPc,
        pShortVariableNames,
        pValidateNoBackwardGoto,
        // no parse validation is done separately in unit tests
        false,
        true);
  }

  private void checkCorrectParameterCount() {
    // extract amount of MPOROptions constructor parameters
    Constructor<?>[] constructors = MPOROptions.class.getDeclaredConstructors();
    checkArgument(constructors.length == 1, "MPOROptions can have one constructor only");
    int parameterCount = constructors[0].getParameterCount();
    // extract amount of fields marked as @Option in MPORAlgorithm
    int optionCount =
        (int)
            Arrays.stream(MPORAlgorithm.class.getDeclaredFields())
                .filter(pField -> pField.isAnnotationPresent(Option.class))
                .count();
    checkArgument(
        parameterCount == optionCount,
        "the amount of constructor parameters must match the amount of @Option fields in"
            + " MPORAlgorithm");
  }

  private void checkEqualFieldNames() {
    // extract string of all fields in MPOROptions
    ImmutableSet<String> optionsFieldNames =
        Arrays.stream(MPOROptions.class.getDeclaredFields())
            .map(Field::getName)
            .collect(ImmutableSet.toImmutableSet());
    // check if fields from MPORAlgorithm with @Option have a field with same name in this class
    for (Field algorithmField : MPORAlgorithm.class.getDeclaredFields()) {
      if (algorithmField.isAnnotationPresent(Option.class)) {
        if (!optionsFieldNames.contains(algorithmField.getName())) {
          throw new IllegalArgumentException(
              String.format(
                  "MPOROptions fields does not contain field from MPORAlgorithm annotated as"
                      + " @Option: %s",
                  algorithmField.getName()));
        }
      }
    }
  }

  void handleOptionRejections(LogManager pLogger) {
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
}

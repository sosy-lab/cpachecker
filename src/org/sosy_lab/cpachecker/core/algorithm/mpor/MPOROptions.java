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
import org.sosy_lab.cpachecker.core.algorithm.mpor.nondeterminism.NondeterminismSource;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.multi_control.MultiControlStatementEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.formatting.ClangFormatStyle;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_elements.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionOrder;

/**
 * For better overview so that not all {@link Option}s passed to {@code analysis.algorithm.MPOR}
 * have to be passed as parameters.
 */
public class MPOROptions {

  public final boolean allowPointerWrites;

  public final boolean atomicBlockMerge;

  public final BitVectorEncoding bitVectorEncoding;

  public final boolean bitVectorReduction;

  public final boolean comments;

  public final boolean conflictReduction;

  public final boolean consecutiveLabels;

  public final MultiControlStatementEncoding controlEncodingStatement;

  public final MultiControlStatementEncoding controlEncodingThread;

  public final boolean formatCode;

  public final ClangFormatStyle formatStyle;

  public final boolean inputFunctionDeclarations;

  public final boolean inputTypeDeclarations;

  public final boolean kAssignLazy;

  public final boolean kBound;

  public final boolean kIgnoreZeroReduction;

  public final boolean license;

  public final boolean linkReduction;

  public final int loopIterations;

  public final boolean loopUnrolling;

  public final boolean noBackwardGoto;

  public final boolean noBackwardLoopGoto;

  public final boolean nondeterminismSigned;

  public final NondeterminismSource nondeterminismSource;

  public final boolean outputMetadata;

  public final String outputPath;

  public final boolean overwriteFiles;

  public final boolean pruneEmptyStatements;

  public final boolean pruneBitVectorEvaluation;

  public final boolean pruneBitVectorWrite;

  public final ReductionMode reductionMode;

  public final ReductionOrder reductionOrder;

  public final boolean scalarPc;

  public final boolean sequentializationErrors;

  public final boolean shortVariableNames;

  public final boolean validateNoBackwardGoto;

  public final boolean validateParse;

  public final boolean validatePc;

  public MPOROptions(
      boolean pAllowPointerWrites,
      boolean pAtomicBlockMerge,
      BitVectorEncoding pBitVectorEncoding,
      boolean pBitVectorReduction,
      boolean pComments,
      boolean pConflictReduction,
      boolean pConsecutiveLabels,
      MultiControlStatementEncoding pControlEncodingStatement,
      MultiControlStatementEncoding pControlEncodingThread,
      boolean pFormatCode,
      ClangFormatStyle pFormatStyle,
      boolean pInputFunctionDeclarations,
      boolean pInputTypeDeclarations,
      boolean pKAssignLazy,
      boolean pKBound,
      boolean pKIgnoreZeroReduction,
      boolean pLicense,
      boolean pLinkReduction,
      int pLoopIterations,
      boolean pLoopUnrolling,
      boolean pNoBackwardGoto,
      boolean pNoBackwardLoopGoto,
      boolean pNondeterminismSigned,
      NondeterminismSource pNondeterminismSource,
      boolean pOutputMetadata,
      String pOutputPath,
      boolean pOverwriteFiles,
      boolean pPruneEmptyStatements,
      boolean pPruneBitVectorEvaluation,
      boolean pPruneBitVectorWrite,
      ReductionMode pReductionMode,
      ReductionOrder pReductionOrder,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariableNames,
      boolean pValidateNoBackwardGoto,
      boolean pValidateParse,
      boolean pValidatePc) {

    checkArgument(
        correctParameterAmount(),
        "the amount of constructor parameters must match the amount of @Option fields in"
            + " MPORAlgorithm");
    checkArgument(
        equalFieldNames(),
        "all @Option fields in MPORAlgorithm must have a MPOROptions field with the same name");

    allowPointerWrites = pAllowPointerWrites;
    atomicBlockMerge = pAtomicBlockMerge;
    bitVectorEncoding = pBitVectorEncoding;
    bitVectorReduction = pBitVectorReduction;
    comments = pComments;
    conflictReduction = pConflictReduction;
    consecutiveLabels = pConsecutiveLabels;
    controlEncodingStatement = pControlEncodingStatement;
    controlEncodingThread = pControlEncodingThread;
    formatCode = pFormatCode;
    formatStyle = pFormatStyle;
    inputFunctionDeclarations = pInputFunctionDeclarations;
    inputTypeDeclarations = pInputTypeDeclarations;
    kAssignLazy = pKAssignLazy;
    kBound = pKBound;
    kIgnoreZeroReduction = pKIgnoreZeroReduction;
    license = pLicense;
    linkReduction = pLinkReduction;
    loopIterations = pLoopIterations;
    loopUnrolling = pLoopUnrolling;
    noBackwardGoto = pNoBackwardGoto;
    noBackwardLoopGoto = pNoBackwardLoopGoto;
    nondeterminismSigned = pNondeterminismSigned;
    nondeterminismSource = pNondeterminismSource;
    outputMetadata = pOutputMetadata;
    outputPath = pOutputPath;
    overwriteFiles = pOverwriteFiles;
    pruneEmptyStatements = pPruneEmptyStatements;
    pruneBitVectorEvaluation = pPruneBitVectorEvaluation;
    pruneBitVectorWrite = pPruneBitVectorWrite;
    reductionMode = pReductionMode;
    reductionOrder = pReductionOrder;
    scalarPc = pScalarPc;
    sequentializationErrors = pSequentializationErrors;
    shortVariableNames = pShortVariableNames;
    validateNoBackwardGoto = pValidateNoBackwardGoto;
    validateParse = pValidateParse;
    validatePc = pValidatePc;
  }

  public static MPOROptions defaultTestInstance() {
    return new MPOROptions(
        true,
        true,
        BitVectorEncoding.NONE,
        false,
        false,
        false,
        true,
        MultiControlStatementEncoding.SWITCH_CASE,
        MultiControlStatementEncoding.NONE,
        true,
        ClangFormatStyle.WEBKIT,
        true,
        true,
        true,
        false,
        false,
        false,
        // true so that MemoryModel is created
        true,
        0,
        false,
        true,
        true,
        false,
        NondeterminismSource.NUM_STATEMENTS,
        true,
        SeqWriter.DEFAULT_OUTPUT_PATH,
        true,
        true,
        false,
        false,
        ReductionMode.NONE,
        ReductionOrder.NONE,
        true,
        false,
        true,
        true,
        true,
        true);
  }

  /** Returns a test instance where only the program customization, not output, can be specified. */
  public static MPOROptions testInstance(
      boolean pAllowPointerWrites,
      BitVectorEncoding pBitVectorEncoding,
      boolean pBitVectorReduction,
      boolean pComments,
      boolean pConflictReduction,
      MultiControlStatementEncoding pControlEncodingStatement,
      MultiControlStatementEncoding pControlEncodingThread,
      boolean pInputFunctionDeclarations,
      boolean pKAssignLazy,
      boolean pKBound,
      boolean pKIgnoreZeroReduction,
      boolean pLicense,
      boolean pLinkReduction,
      int pLoopIterations,
      boolean pLoopUnrolling,
      boolean pNoBackwardGoto,
      boolean pNoBackwardLoopGoto,
      boolean pNondeterminismSigned,
      NondeterminismSource pNondeterminismSource,
      boolean pPruneBitVectorEvaluation,
      boolean pPruneBitVectorWrite,
      ReductionMode pReductionMode,
      ReductionOrder pReductionOrder,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariableNames,
      boolean pValidateNoBackwardGoto) {

    return new MPOROptions(
        pAllowPointerWrites,
        // always merge atomic blocks because not doing so may add additional interleavings
        true,
        pBitVectorEncoding,
        pBitVectorReduction,
        pComments,
        pConflictReduction,
        // always use consecutive labels, disabling is only for debugging, not for release
        true,
        pControlEncodingStatement,
        pControlEncodingThread,
        // never format output code so that unit test is independent of clang-format
        false,
        ClangFormatStyle.GNU,
        pInputFunctionDeclarations,
        // always include type declarations at the moment, excluding them is unsafe
        true,
        pKAssignLazy,
        pKBound,
        pKIgnoreZeroReduction,
        pLicense,
        pLinkReduction,
        pLoopIterations,
        pLoopUnrolling,
        pNoBackwardGoto,
        pNoBackwardLoopGoto,
        pNondeterminismSigned,
        pNondeterminismSource,
        false,
        SeqWriter.DEFAULT_OUTPUT_PATH,
        false,
        // always prune empty, disabling is only for debugging, not for release
        true,
        pPruneBitVectorEvaluation,
        pPruneBitVectorWrite,
        pReductionMode,
        pReductionOrder,
        pScalarPc,
        pSequentializationErrors,
        pShortVariableNames,
        pValidateNoBackwardGoto,
        // no parse validation is done separately in unit tests
        false,
        true);
  }

  private boolean correctParameterAmount() {
    // extract amount of MPOROptions constructor parameters
    Constructor<?>[] constructors = MPOROptions.class.getDeclaredConstructors();
    checkArgument(constructors.length == 1, "MPOROptions can have one constructor only");
    int paramAmount = constructors[0].getParameterCount();
    // extract amount of fields marked as @Option in MPORAlgorithm
    int optionAmount =
        (int)
            Arrays.stream(MPORAlgorithm.class.getDeclaredFields())
                .filter(pField -> pField.isAnnotationPresent(Option.class))
                .count();
    return paramAmount == optionAmount;
  }

  private boolean equalFieldNames() {
    // extract string of all fields in MPOROptions
    ImmutableSet<String> optionsFieldNames =
        Arrays.stream(MPOROptions.class.getDeclaredFields())
            .map(Field::getName)
            .collect(ImmutableSet.toImmutableSet());
    // check if fields from MPORAlgorithm with @Option have a field with same name in this class
    for (Field algorithmField : MPORAlgorithm.class.getDeclaredFields()) {
      if (algorithmField.isAnnotationPresent(Option.class)) {
        if (!optionsFieldNames.contains(algorithmField.getName())) {
          return false;
        }
      }
    }
    return true;
  }

  // TODO best move to input_rejection
  void handleOptionRejections(LogManager pLogger) {
    if (loopIterations < 0) {
      pLogger.logfUserException(
          Level.SEVERE,
          new RuntimeException(),
          "loopIterations must be 0 or greater, cannot be %s",
          loopIterations);
      throw new AssertionError();
    }
    if (loopIterations == 0) {
      if (loopUnrolling) {
        pLogger.logfUserException(
            Level.SEVERE,
            new RuntimeException(),
            "loopUnrolling can only be enabled when loopIterations > 0");
        throw new AssertionError();
      }
    }
    if (controlEncodingStatement.equals(MultiControlStatementEncoding.NONE)) {
      pLogger.log(
          Level.SEVERE,
          "controlEncodingStatement cannot be %s",
          MultiControlStatementEncoding.NONE);
      throw new AssertionError();
    }
    if (areBitVectorsEnabled()) {
      if (!reductionMode.isEnabled()) {
        pLogger.log(
            Level.SEVERE,
            "conflictReduction or bitVectorReduction is enabled, but reductionMode is not set.");
        throw new AssertionError();
      }
      if (!bitVectorEncoding.isEnabled()) {
        pLogger.log(
            Level.SEVERE,
            "conflictReduction or bitVectorReduction is enabled, but bitVectorEncoding is not"
                + " set.");
        throw new AssertionError();
      }
    }
    if (bitVectorReduction && conflictReduction) {
      if (!reductionOrder.isEnabled()) {
        pLogger.log(
            Level.SEVERE,
            "both bitVectorReduction and conflictReduction are enabled, but no reductionOrder is"
                + " specified.");
        throw new AssertionError();
      }
    }
    if (!noBackwardGoto && validateNoBackwardGoto) {
      pLogger.log(
          Level.SEVERE, "validateNoBackwardGoto is enabled, but noBackwardGoto is disabled.");
      throw new AssertionError();
    }
  }

  // TODO need more warnings here
  /** Logs all warnings regarding unused, overwritten, conflicting, ... options. */
  void handleOptionWarnings(LogManager pLogger) {
    if (!linkReduction && bitVectorReduction) {
      pLogger.log(
          Level.WARNING,
          "bitVectorReduction is only considered with linkReduction"
              + " enabled. Either enable linkReduction or disable bitVectorReduction.");
    }
    if (!linkReduction && conflictReduction) {
      pLogger.log(
          Level.WARNING,
          "conflictReduction is only considered with linkReduction"
              + " enabled. Either enable linkReduction or disable conflictReduction.");
    }
    if (!linkReduction && bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "bitVectorEncoding is only considered with linkReduction"
              + " enabled. Either enable linkReduction or set bitVectorEncoding to NONE.");
    }
    if (pruneBitVectorEvaluation && !areBitVectorsEnabled()) {
      pLogger.log(
          Level.WARNING,
          "pruneBitVectorEvaluation is only considered when bitVectorReduction /"
              + " conflictReduction is enabled. Either disable pruneBitVectorEvaluation or enable"
              + " bitVectorReduction / conflictReduction.");
    }
    if (pruneBitVectorEvaluation && !bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "pruneBitVectorEvaluation is only considered when bitVectorEncoding is not"
              + " NONE. Either disable pruneBitVectorEvaluation or set bitVectorEncoding.");
    }
    if (pruneBitVectorWrite && !areBitVectorsEnabled()) {
      pLogger.log(
          Level.WARNING,
          "pruneBitVectorWrite is only considered when bitVectorReduction /"
              + " conflictReduction is enabled. Either disable pruneBitVectorWrite or enable"
              + " bitVectorReduction / conflictReduction.");
    }
    if (pruneBitVectorWrite && !bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "pruneBitVectorWrite is only considered when bitVectorEncoding is not"
              + " NONE. Either disable pruneBitVectorWrite or set bitVectorEncoding.");
    }
    if (pruneBitVectorWrite && !bitVectorEncoding.isSparse) {
      pLogger.log(
          Level.WARNING,
          "pruneBitVectorWrite only has an effect when bitVectorEncoding is SPARSE.");
    }
    if (!nondeterminismSource.isNextThreadNondeterministic()) {
      if (!controlEncodingThread.equals(MultiControlStatementEncoding.NONE)) {
        pLogger.log(
            Level.WARNING,
            "controlEncodingThread is not NONE, but the next thread is not chosen"
                + " non-deterministically. Either set controlEncodingThread to NONE or choose a"
                + " nondeterminismSource that makes the next thread non-deterministic.");
      }
    }
    if (!nondeterminismSource.isNumStatementsNondeterministic()) {
      if (kBound) {
        pLogger.log(
            Level.WARNING,
            "kBound is enabled, but the number of statements is not chosen"
                + " non-deterministically. Either disable kBound or choose a"
                + " nondeterminismSource that makes the number of statements non-deterministic.");
      }
      if (kIgnoreZeroReduction) {
        pLogger.log(
            Level.WARNING,
            "kIgnoreZeroReduction is enabled, but the number of statements is not chosen"
                + " non-deterministically. Either disable kIgnoreZeroReduction or choose a"
                + " nondeterminismSource that makes the number of statements non-deterministic.");
      }
    }
    if (reductionMode.isEnabled()) {
      if (!areBitVectorsEnabled()) {
        pLogger.log(
            Level.WARNING,
            "reductionMode is set, but both conflictReduction and "
                + " bitVectorReduction are disabled.");
      }
    }
    if (bitVectorEncoding.isEnabled()) {
      if (!areBitVectorsEnabled()) {
        pLogger.log(
            Level.WARNING,
            "bitVectorEncoding is set, but both conflictReduction and "
                + " bitVectorReduction are disabled.");
      }
    }
  }

  public boolean areBitVectorsEnabled() {
    return bitVectorReduction || conflictReduction || kIgnoreZeroReduction;
  }
}

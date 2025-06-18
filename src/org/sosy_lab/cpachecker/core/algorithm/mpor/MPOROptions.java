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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.partial_order_reduction.ReductionMode;

/**
 * For better overview so that not all {@link Option}s passed to {@code analysis.algorithm.MPOR}
 * have to be passed as parameters.
 */
public class MPOROptions {

  public final boolean atomicBlockMerge;

  public final BitVectorEncoding bitVectorEncoding;

  public final boolean bitVectorEvaluationPrune;

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

  public final boolean license;

  public final boolean linkReduction;

  public final int loopIterations;

  public final NondeterminismSource nondeterminismSource;

  public final boolean outputMetadata;

  public final String outputPath;

  public final boolean overwriteFiles;

  public final boolean pruneEmptyStatements;

  public final ReductionMode reductionMode;

  public final boolean scalarPc;

  public final boolean sequentializationErrors;

  public final boolean shortVariableNames;

  public final boolean signedNondet;

  public final boolean validateParse;

  public final boolean validatePc;

  public MPOROptions(
      boolean pAtomicBlockMerge,
      BitVectorEncoding pBitVectorEncoding,
      boolean pBitVectorEvaluationPrune,
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
      boolean pLicense,
      boolean pLinkReduction,
      int pLoopIterations,
      NondeterminismSource pNondeterminismSource,
      boolean pOutputMetadata,
      String pOutputPath,
      boolean pOverwriteFiles,
      boolean pPruneEmptyStatements,
      ReductionMode pReductionMode,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariableNames,
      boolean pSignedNondet,
      boolean pValidateParse,
      boolean pValidatePc) {

    checkArgument(
        correctParameterAmount(),
        "the amount of constructor parameters must match the amount of @Option fields in"
            + " MPORAlgorithm");
    checkArgument(
        equalFieldNames(),
        "all @Option fields in MPORAlgorithm must have a MPOROptions field with the same name");

    atomicBlockMerge = pAtomicBlockMerge;
    bitVectorEncoding = pBitVectorEncoding;
    bitVectorEvaluationPrune = pBitVectorEvaluationPrune;
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
    license = pLicense;
    linkReduction = pLinkReduction;
    loopIterations = pLoopIterations;
    nondeterminismSource = pNondeterminismSource;
    outputMetadata = pOutputMetadata;
    outputPath = pOutputPath;
    overwriteFiles = pOverwriteFiles;
    pruneEmptyStatements = pPruneEmptyStatements;
    reductionMode = pReductionMode;
    scalarPc = pScalarPc;
    sequentializationErrors = pSequentializationErrors;
    shortVariableNames = pShortVariableNames;
    signedNondet = pSignedNondet;
    validateParse = pValidateParse;
    validatePc = pValidatePc;
  }

  /** Returns a test instance where only the program customization, not output, can be specified. */
  public static MPOROptions testInstance(
      BitVectorEncoding pBitVectorEncoding,
      boolean pPruneBitVectorEvaluation,
      boolean pBitVectorReduction,
      boolean pComments,
      boolean pConflictReduction,
      MultiControlStatementEncoding pControlEncodingStatement,
      MultiControlStatementEncoding pControlEncodingThread,
      boolean pInputFunctionDeclarations,
      boolean pLicense,
      boolean pLinkReduction,
      int pLoopIterations,
      NondeterminismSource pNondeterminismSource,
      ReductionMode pReductionMode,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariableNames,
      boolean pSignedNondet) {

    return new MPOROptions(
        // always merge atomic blocks because not doing so may add additional interleavings
        true,
        pBitVectorEncoding,
        pPruneBitVectorEvaluation,
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
        pLicense,
        pLinkReduction,
        pLoopIterations,
        pNondeterminismSource,
        false,
        SeqWriter.DEFAULT_OUTPUT_PATH,
        false,
        // always prune empty, disabling is only for debugging, not for release
        true,
        pReductionMode,
        pScalarPc,
        pSequentializationErrors,
        pShortVariableNames,
        pSignedNondet,
        // no parse validation in unit tests -> tests are independent of implementation
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

  void handleOptionRejections(LogManager pLogger) {
    if (loopIterations < 0) {
      pLogger.logfUserException(
          Level.SEVERE,
          new RuntimeException(),
          "loopIterations must be 0 or greater, cannot be %s",
          loopIterations);
      throw new AssertionError();
    }
    if (controlEncodingStatement.equals(MultiControlStatementEncoding.NONE)) {
      pLogger.log(
          Level.SEVERE,
          "controlEncodingStatement cannot be %s",
          MultiControlStatementEncoding.NONE);
      throw new AssertionError();
    }
    if (conflictReduction || bitVectorReduction) {
      if (!reductionMode.isEnabled()) {
        pLogger.log(
            Level.SEVERE,
            "conflictReduction or bitVectorReduction are enabled, but reductionMode is not set.");
        throw new AssertionError();
      }
      if (!bitVectorEncoding.isEnabled()) {
        pLogger.log(
            Level.SEVERE,
            "conflictReduction or bitVectorReduction are enabled, but bitVectorEncoding is not"
                + " set.");
        throw new AssertionError();
      }
    }
  }

  /** Logs all warnings regarding unused, overwritten, conflicting, ... options. */
  void handleOptionWarnings(LogManager pLogger) {
    if (!linkReduction && bitVectorReduction) {
      pLogger.log(
          Level.WARNING,
          "WARNING: bitVectorReduction is only considered with linkReduction"
              + " enabled. Either enable linkReduction or disable bitVectorReduction.");
    }
    if (!linkReduction && conflictReduction) {
      pLogger.log(
          Level.WARNING,
          "WARNING: conflictReduction is only considered with linkReduction"
              + " enabled. Either enable linkReduction or disable conflictReduction.");
    }
    if (!linkReduction && bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "WARNING: bitVectorEncoding is only considered with linkReduction"
              + " enabled. Either enable linkReduction or set bitVectorEncoding to NONE.");
    }
    if (bitVectorEvaluationPrune && !(bitVectorReduction || conflictReduction)) {
      pLogger.log(
          Level.WARNING,
          "WARNING: pruneBitVectorEvaluation is only considered when bitVectorReduction /"
              + " conflictReduction is enabled. . Either disable pruneBitVectorEvaluation or enable"
              + " bitVectorReduction / conflictReduction.");
    }
    if (bitVectorEvaluationPrune && !bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "WARNING: pruneBitVectorEvaluation is only considered when bitVectorEncoding is not"
              + " NONE. Either disable pruneBitVectorEvaluation or set bitVectorEncoding.");
    }
    if (!nondeterminismSource.isNextThreadNondeterministic()) {
      if (!controlEncodingThread.equals(MultiControlStatementEncoding.NONE)) {
        pLogger.log(
            Level.WARNING,
            "WARNING: controlEncodingThread is not NONE, but the next thread is not chosen"
                + " non-deterministically. Either set controlEncodingThread to NONE or choose a"
                + " nondeterminismSource that makes the next thread non-deterministic.");
      }
    }
    if (reductionMode.isEnabled()) {
      if (!(conflictReduction || bitVectorReduction)) {
        pLogger.log(
            Level.WARNING,
            "WARNING: reductionMode is set, but both conflictReduction and "
                + " bitVectorReduction are disabled.");
      }
    }
    if (bitVectorEncoding.isEnabled()) {
      if (!(conflictReduction || bitVectorReduction)) {
        pLogger.log(
            Level.WARNING,
            "WARNING: bitVectorEncoding is set, but both conflictReduction and "
                + " bitVectorReduction are disabled.");
      }
    }
  }
}

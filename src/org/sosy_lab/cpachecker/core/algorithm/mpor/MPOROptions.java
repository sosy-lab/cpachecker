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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.seq_custom.statement.control_flow.multi.MultiControlEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;

/**
 * For better overview so that not all {@link Option}s passed to {@code analysis.algorithm.MPOR}
 * have to be passed as parameters.
 */
public class MPOROptions {

  public final boolean atomicBlockMerge;

  public final BitVectorEncoding bitVectorEncoding;

  public final boolean bitVectorEvaluationPrune;

  public final BitVectorReduction bitVectorReduction;

  public final boolean comments;

  public final boolean consecutiveLabels;

  public final MultiControlEncoding controlEncodingThread;

  public final MultiControlEncoding controlEncodingStatement;

  public final boolean inputFunctionDeclarations;

  public final boolean inputTypeDeclarations;

  public final boolean license;

  public final boolean linkReduction;

  public final NondeterminismSource nondeterminismSource;

  public final boolean outputMetadata;

  public final String outputPath;

  public final boolean overwriteFiles;

  public final boolean pruneEmptyStatements;

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
      BitVectorReduction pBitVectorReduction,
      boolean pComments,
      boolean pConsecutiveLabels,
      MultiControlEncoding pControlEncodingThread,
      MultiControlEncoding pControlEncodingStatement,
      boolean pInputFunctionDeclarations,
      boolean pInputTypeDeclarations,
      boolean pLicense,
      boolean pLinkReduction,
      NondeterminismSource pNondeterminismSource,
      boolean pOutputMetadata,
      String pOutputPath,
      boolean pOverwriteFiles,
      boolean pPruneEmptyStatements,
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
    consecutiveLabels = pConsecutiveLabels;
    controlEncodingThread = pControlEncodingThread;
    controlEncodingStatement = pControlEncodingStatement;
    inputFunctionDeclarations = pInputFunctionDeclarations;
    inputTypeDeclarations = pInputTypeDeclarations;
    license = pLicense;
    linkReduction = pLinkReduction;
    nondeterminismSource = pNondeterminismSource;
    outputMetadata = pOutputMetadata;
    outputPath = pOutputPath;
    overwriteFiles = pOverwriteFiles;
    pruneEmptyStatements = pPruneEmptyStatements;
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
      BitVectorReduction pBitVectorReduction,
      boolean pComments,
      MultiControlEncoding pControlEncodingThread,
      MultiControlEncoding pControlEncodingStatement,
      boolean pInputFunctionDeclarations,
      boolean pLicense,
      boolean pLinkReduction,
      NondeterminismSource pNondeterminismSource,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariables,
      boolean pSignedNondet) {

    return new MPOROptions(
        // always merge atomic blocks because not doing so may add additional interleavings
        true,
        pBitVectorEncoding,
        pPruneBitVectorEvaluation,
        pBitVectorReduction,
        pComments,
        // always use consecutive labels, disabling is only for debugging, not for release
        true,
        pControlEncodingThread,
        pControlEncodingStatement,
        pInputFunctionDeclarations,
        // always include type declarations at the moment, excluding them is unsafe
        true,
        pLicense,
        pLinkReduction,
        pNondeterminismSource,
        false,
        SeqWriter.DEFAULT_OUTPUT_PATH,
        false,
        // always prune empty, disabling is only for debugging, not for release
        true,
        pScalarPc,
        pSequentializationErrors,
        pShortVariables,
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

  /** Logs all warnings regarding unused, overwritten, conflicting, ... options. */
  protected void handleOptionWarnings(LogManager pLogger) {
    if (!linkReduction && bitVectorReduction.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "WARNING: bitVectorReduction is only considered with linkReduction"
              + " enabled. Either enable linkReduction or set bitVectorReduction to NONE.");
    }
    if (!linkReduction && bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "WARNING: bitVectorEncoding is only considered with linkReduction"
              + " enabled. Either enable linkReduction or set bitVectorEncoding to NONE.");
    }
    if (bitVectorEvaluationPrune && !bitVectorReduction.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "WARNING: pruneBitVectorEvaluation is only considered when bitVectorReduction is not"
              + " NONE. Either disable pruneBitVectorEvaluation or set bitVectorReduction.");
    }
    if (bitVectorEvaluationPrune && !bitVectorEncoding.isEnabled()) {
      pLogger.log(
          Level.WARNING,
          "WARNING: pruneBitVectorEvaluation is only considered when bitVectorEncoding is not"
              + " NONE. Either disable pruneBitVectorEvaluation or set bitVectorEncoding.");
    }
  }
}

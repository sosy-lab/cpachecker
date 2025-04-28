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
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SeqWriter;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorEncoding;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.bit_vector.BitVectorReduction;

/**
 * For better overview so that not all {@link Option}s passed to {@code analysis.algorithm.MPOR}
 * have to be passed as parameters.
 */
public class MPOROptions {

  public final boolean comments;

  public final boolean consecutiveLabels;

  public final boolean inputFunctionDeclarations;

  public final boolean inputTypeDeclarations;

  public final boolean license;

  public final boolean outputMetadata;

  public final String outputPath;

  public final boolean overwriteFiles;

  public final boolean porConcat;

  public final BitVectorReduction porBitVectorReduction;

  public final BitVectorEncoding porBitVectorEncoding;

  public final boolean pruneEmpty;

  public final boolean scalarPc;

  public final boolean sequentializationErrors;

  public final boolean shortVariables;

  public final boolean signedNondet;

  public final boolean threadLoops;

  public final boolean threadLoopsNext;

  public final boolean validateParse;

  public final boolean validatePc;

  public MPOROptions(
      boolean pComments,
      boolean pConsecutiveLabels,
      boolean pInputFunctionDeclarations,
      boolean pInputTypeDeclarations,
      boolean pLicense,
      boolean pOutputMetadata,
      String pOutputPath,
      boolean pOverwriteFiles,
      boolean pPorConcat,
      BitVectorReduction pPorBitVectorReduction,
      BitVectorEncoding pPorBitVectorEncoding,
      boolean pPruneEmpty,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariables,
      boolean pSignedNondet,
      boolean pThreadLoops,
      boolean pThreadLoopsNext,
      boolean pValidateParse,
      boolean pValidatePc) {

    checkArgument(
        correctParameterAmount(),
        "the amount of constructor parameters must match the amount of @Option fields in"
            + " MPORAlgorithm");
    checkArgument(
        equalFieldNames(),
        "all @Option fields in MPORAlgorithm must have a MPOROptions field with the same name");

    comments = pComments;
    consecutiveLabels = pConsecutiveLabels;
    inputFunctionDeclarations = pInputFunctionDeclarations;
    inputTypeDeclarations = pInputTypeDeclarations;
    license = pLicense;
    outputMetadata = pOutputMetadata;
    outputPath = pOutputPath;
    overwriteFiles = pOverwriteFiles;
    porConcat = pPorConcat;
    porBitVectorReduction = pPorBitVectorReduction;
    porBitVectorEncoding = pPorBitVectorEncoding;
    pruneEmpty = pPruneEmpty;
    scalarPc = pScalarPc;
    sequentializationErrors = pSequentializationErrors;
    shortVariables = pShortVariables;
    signedNondet = pSignedNondet;
    threadLoops = pThreadLoops;
    threadLoopsNext = pThreadLoopsNext;
    validateParse = pValidateParse;
    validatePc = pValidatePc;
  }

  /** Returns a test instance where only the program customization, not output, can be specified. */
  public static MPOROptions testInstance(
      boolean pComments,
      boolean pInputFunctionDeclarations,
      boolean pLicense,
      boolean pPorConcat,
      BitVectorReduction pPorBitVectorReduction,
      BitVectorEncoding pPorBitVectorEncoding,
      boolean pScalarPc,
      boolean pSequentializationErrors,
      boolean pShortVariables,
      boolean pSignedNondet,
      boolean pThreadLoops,
      boolean pThreadLoopsNext) {

    return new MPOROptions(
        pComments,
        // always use consecutive labels, disabling is only for debugging, not for release
        true,
        pInputFunctionDeclarations,
        // always include type declarations at the moment, excluding them is unsafe
        true,
        pLicense,
        false,
        SeqWriter.DEFAULT_OUTPUT_PATH,
        false,
        pPorConcat,
        pPorBitVectorReduction,
        pPorBitVectorEncoding,
        // always prune empty, disabling is only for debugging, not for release
        true,
        pScalarPc,
        pSequentializationErrors,
        pShortVariables,
        pSignedNondet,
        pThreadLoops,
        pThreadLoopsNext,
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
    for (Field algoField : MPORAlgorithm.class.getDeclaredFields()) {
      if (algoField.isAnnotationPresent(Option.class)) {
        if (!optionsFieldNames.contains(algoField.getName())) {
          return false;
        }
      }
    }
    return true;
  }

  /** Logs all warnings regarding unused, overwritten, conflicting, ... options. */
  protected void handleOptionWarnings(LogManager pLogger) {
    if (!porConcat && !porBitVectorReduction.equals(BitVectorReduction.NONE)) {
      pLogger.log(
          Level.WARNING,
          "WARNING: porBitVectorReduction is only considered with porConcat "
              + " enabled. Either enable porConcat or set porBitVectorReduction to NONE.");
    }
    if (porBitVectorReduction.equals(BitVectorReduction.NONE)
        && !porBitVectorEncoding.equals(BitVectorEncoding.NONE)) {
      pLogger.log(
          Level.WARNING,
          "WARNING: porBitVectorEncoding is only considered when porBitVectorReduction is not NONE."
              + " Either set porBitVectorEncoding to NONE or set porBitVectorReduction.");
    }
    if (!threadLoops && threadLoopsNext) {
      pLogger.log(
          Level.WARNING,
          "WARNING: threadLoopsNext is only considered with threadLoops enabled. Either enable"
              + "threadLoops or disable threadLoopsNext.");
    }
  }
}

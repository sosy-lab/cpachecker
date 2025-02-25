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
import org.sosy_lab.common.configuration.Option;

/**
 * For better overview so that not all {@link Option}s passed to {@code analysis.algorithm.MPOR}
 * have to be passed as parameters.
 */
public class MPOROptions {

  public final boolean outputMetadata;

  public final boolean overwriteFiles;

  public final boolean partialOrderReduction;

  public final boolean scalarPc;

  public MPOROptions(
      boolean pOutputMetadata,
      boolean pOverwriteFiles,
      boolean pPartialOrderReduction,
      boolean pScalarPc) {

    checkArgument(
        correctParamAmount(),
        "the amount of constructor parameters must match the amount of @Option fields in"
            + " MPORAlgorithm");
    checkArgument(
        equalFieldNames(),
        "all @Option fields in MPORAlgorithm must have a MPOROptions field with the same name");
    outputMetadata = pOutputMetadata;
    overwriteFiles = pOverwriteFiles;
    partialOrderReduction = pPartialOrderReduction;
    scalarPc = pScalarPc;
  }

  private boolean correctParamAmount() {
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
}

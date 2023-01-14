// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;

/**
 * A {@link CfaValidator} determines whether a given CFA is valid (i.e., whether a CFA is correct
 * with respect to certain criteria checked by the validator).
 */
@FunctionalInterface
public interface CfaValidator {

  /**
   * Returns a new {@link CfaValidator} that is the combination of the specified validators.
   *
   * <p>The returned validator only considers a given CFA valid, if all specified validators
   * consider it valid.
   *
   * @param pValidators the validators to combine
   * @return a new {@link CfaValidator} that is the combination of the specified validators
   * @throws NullPointerException if {@code pValidators == null} or {@code pValidators} contains an
   *     element that is {@code null}
   */
  public static CfaValidator combine(CfaValidator... pValidators) {
    ImmutableList<CfaValidator> validators = ImmutableList.copyOf(pValidators);
    return new CfaValidator() {

      @Override
      public CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
        CfaValidationResult validationResult = CfaValidationResult.VALID;
        for (CfaValidator validator : validators) {
          validationResult = validationResult.combine(validator.check(pCfaNetwork, pCfaMetadata));
        }
        return validationResult;
      }
    };
  }

  /**
   * Returns a {@link CfaValidationResult} that indicates whether the specified CFA is valid.
   *
   * <p>The pair {@code (pCfaNetwork, pCfaMetadata)} represents the CFA to validate.
   *
   * @param pCfaNetwork the graph representation of the CFA to validate
   * @param pCfaMetadata the metadata of the CFA to validate
   * @return a {@link CfaValidationResult} that indicates whether the specified CFA is valid
   * @throws NullPointerException if any parameter is {@code null}
   */
  CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata);

  /**
   * Returns a {@link CfaValidationResult} that indicates whether the specified {@link CFA}
   * represents a valid CFA.
   *
   * @param pCfa the {@link CFA} to validate
   * @return a {@link CfaValidationResult} that indicates whether the specified {@link CFA}
   *     represents a valid CFA
   * @throws NullPointerException if {@code pCfa == null}
   */
  default CfaValidationResult check(CFA pCfa) {
    return check(CfaNetwork.wrap(pCfa), pCfa.getMetadata());
  }
}

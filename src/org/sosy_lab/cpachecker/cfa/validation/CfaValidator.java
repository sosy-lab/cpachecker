// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;

/**
 * A {@link CfaValidator} determines whether a given CFA is valid (i.e., whether a CFA is correct
 * with respect to certain criteria checked by the validator).
 *
 * <p>Typically, a {@link CfaValidator} implementation checks a specific aspect of a CFA. To check
 * multiple aspects, validators can be combined using {@link #combine(CfaValidator)}.
 */
public interface CfaValidator {

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
  CfaValidationResult check(CFA pCfa);

  /**
   * Returns a new {@link CfaValidator} that combines this validator and {@code pOther}.
   *
   * <p>The returned validator only considers a given CFA valid, if both validators (this validator
   * and {@code pOther}) consider it valid.
   *
   * @param pOther the validator to combine with this validator
   * @return a new {@link CfaValidator} that combines this validator and {@code pOther}
   * @throws NullPointerException if {@code pOther == null}
   */
  CfaValidator combine(CfaValidator pOther);
}

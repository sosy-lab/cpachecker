// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.validation;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

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
   * Returns a new {@link CfaValidator} that validates all nodes in a CFA using the specified node
   * validator.
   *
   * @param pNodeValidator the node validator (CFA node -> a {@link CfaValidationResult} that
   *     indicates whether the node is valid) that is called for every node in a CFA
   * @return a new {@link CfaValidator} that validates all nodes in a CFA using the specified node
   *     validator
   * @throws NullPointerException if {@code pNodeValidator == null} or {@code pNodeValidator}
   *     returns {@code null}
   */
  public static CfaValidator createNodeValidator(
      Function<CFANode, CfaValidationResult> pNodeValidator) {
    checkNotNull(pNodeValidator);
    return (cfaNetwork, cfaMetadata) -> {
      CfaValidationResult validationResult = CfaValidationResult.VALID;
      for (CFANode node : cfaNetwork.nodes()) {
        validationResult = validationResult.combine(pNodeValidator.apply(node));
      }
      return validationResult;
    };
  }

  /**
   * Returns a new {@link CfaValidator} that validates all edges in a CFA using the specified edge
   * validator.
   *
   * @param pEdgeValidator the edge validator (CFA edge -> a {@link CfaValidationResult} that
   *     indicates whether the edge is valid) that is called for every edge in a CFA
   * @return a new {@link CfaValidator} that validates all edges in a CFA using the specified edge
   *     validator
   * @throws NullPointerException if {@code pEdgeValidator == null} or {@code pEdgeValidator}
   *     returns {@code null}
   */
  public static CfaValidator createEdgeValidator(
      Function<CFAEdge, CfaValidationResult> pEdgeValidator) {
    checkNotNull(pEdgeValidator);
    return (cfaNetwork, cfaMetadata) -> {
      CfaValidationResult validationResult = CfaValidationResult.VALID;
      for (CFAEdge edge : cfaNetwork.edges()) {
        validationResult = validationResult.combine(pEdgeValidator.apply(edge));
      }
      return validationResult;
    };
  }

  /**
   * Returns a new {@link CfaValidator} that validates all elements in a CFA (i.e., its nodes and
   * edges) using the specified node and edge validators.
   *
   * @param pNodeValidator the node validator (CFA node -> a {@link CfaValidationResult} that
   *     indicates whether the node is valid) that is called for every node in a CFA
   * @param pEdgeValidator the edge validator (CFA edge -> a {@link CfaValidationResult} that
   *     indicates whether the edge is valid) that is called for every edge in a CFA
   * @return a new {@link CfaValidator} that validates all elements in a CFA (i.e., its nodes and
   *     edges) using the specified node and edge validators
   * @throws NullPointerException if any parameter is {@code null} or any of the specified
   *     validators returns {@code null}
   */
  public static CfaValidator createElementValidator(
      Function<CFANode, CfaValidationResult> pNodeValidator,
      Function<CFAEdge, CfaValidationResult> pEdgeValidator) {
    checkNotNull(pNodeValidator);
    checkNotNull(pEdgeValidator);
    return (cfaNetwork, cfaMetadata) -> {
      CfaValidationResult validationResult = CfaValidationResult.VALID;
      for (CFANode node : cfaNetwork.nodes()) {
        validationResult = validationResult.combine(pNodeValidator.apply(node));
        for (CFAEdge edge : cfaNetwork.outEdges(node)) {
          validationResult = validationResult.combine(pEdgeValidator.apply(edge));
        }
      }
      return validationResult;
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

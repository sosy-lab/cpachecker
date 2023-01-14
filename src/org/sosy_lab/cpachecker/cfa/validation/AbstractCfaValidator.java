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
import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * A class that makes implementing {@link CfaValidator} easier, especially if checking individual
 * elements of a CFA (i.e., its nodes and edges) is required.
 *
 * <p>For every CFA validation, a new {@link AbstractCfaValidator} instance is created using the
 * specified validator factory. This means that a {@link AbstractCfaValidator} can have object
 * attributes which store data relevant for a single CFA validation.
 */
public abstract class AbstractCfaValidator implements CfaValidator {

  private final BiFunction<CfaNetwork, CfaMetadata, AbstractCfaValidator> validatorFactory;

  /**
   * Creates a new {@link AbstractCfaValidator} with the specified validator factory.
   *
   * <p>For every CFA validation, a new {@link AbstractCfaValidator} instance is created using the
   * specified validator factory.
   *
   * @param pValidatorFactory CFA to validate -> new validator instance
   * @throws NullPointerException if {@code pValidatorFactory == null}
   */
  protected AbstractCfaValidator(
      BiFunction<CfaNetwork, CfaMetadata, AbstractCfaValidator> pValidatorFactory) {
    validatorFactory = checkNotNull(pValidatorFactory);
  }

  /**
   * Creates a new {@link AbstractCfaValidator} with the specified validator factory.
   *
   * <p>For every CFA validation, a new {@link AbstractCfaValidator} instance is created using the
   * specified validator factory.
   *
   * @param pValidatorFactory supplier for new validator instances
   * @throws NullPointerException if {@code pValidatorFactory == null}
   */
  protected AbstractCfaValidator(Supplier<AbstractCfaValidator> pValidatorFactory) {
    checkNotNull(pValidatorFactory);
    validatorFactory = (cfaNetwork, cfaMetadata) -> pValidatorFactory.get();
  }

  @Override
  public final CfaValidationResult check(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    CfaValidationResult validationResult = CfaValidationResult.VALID;
    AbstractCfaValidator validator = validatorFactory.apply(pCfaNetwork, pCfaMetadata);
    for (CFANode node : pCfaNetwork.nodes()) {
      validationResult = validationResult.combine(validator.checkNode(node));
    }
    for (CFAEdge edge : pCfaNetwork.edges()) {
      validationResult = validationResult.combine(validator.checkEdge(edge));
    }
    validationResult = validationResult.combine(validator.checkComplete(pCfaNetwork, pCfaMetadata));
    return validationResult;
  }

  @Override
  public final CfaValidationResult check(CFA pCfa) {
    return check(CfaNetwork.wrap(pCfa), pCfa.getMetadata());
  }

  @Override
  public final CfaValidator combine(CfaValidator pOther) {
    return new CompositeCfaValidator(this, pOther);
  }

  /**
   * Returns a {@link CfaValidationResult} that indicates that the current check passed (i.e., the
   * current check didn't fail).
   *
   * @return a {@link CfaValidationResult} that indicates that the current check passed
   */
  protected final CfaValidationResult pass() {
    return CfaValidationResult.VALID;
  }

  /**
   * Creates a new {@link CfaValidationResult} that indicates that the check failed (i.e., the CFA
   * is not valid).
   *
   * @param pMessage a string that describes the cause of the failure, can be a format string that
   *     {@link String#format(String, Object...)} accepts
   * @param pArgs arguments referenced by the format specifiers in {@code pMessage}, if it's a
   *     format string
   * @return a new {@link CfaValidationResult} that indicates that the check failed
   * @throws NullPointerException if {@code pMessage == null}
   */
  protected final CfaValidationResult fail(String pMessage, Object... pArgs) {
    String message = String.format(pMessage, pArgs);
    return CfaValidationResult.error(String.format("[%s] %s", getClass().getSimpleName(), message));
  }

  /**
   * Called for every node of the CFA to validate.
   *
   * @param pNode the node to check
   * @return a {@link CfaValidationResult} that indicates whether the node check passed or failed
   * @throws NullPointerException if {@code pNode == null}
   */
  protected CfaValidationResult checkNode(CFANode pNode) {
    return pass();
  }

  /**
   * Called for every edge of the CFA to validate.
   *
   * @param pEdge the edge to check
   * @return a {@link CfaValidationResult} that indicates whether the edge check passed or failed
   * @throws NullPointerException if {@code pEdge == null}
   */
  protected CfaValidationResult checkEdge(CFAEdge pEdge) {
    return pass();
  }

  /**
   * Called after all individual elements of the CFA (i.e., its nodes and edges) have been checked.
   *
   * <p>The pair {@code (pCfaNetwork, pCfaMetadata)} represents the CFA to validate.
   *
   * @param pCfaNetwork the graph representation of the CFA to validate
   * @param pCfaMetadata the metadata of the CFA to validate
   * @return a {@link CfaValidationResult} that indicates whether the check passed or failed (only
   *     report new fails here, i.e., fails that weren't already discoverd during node/edge checks)
   * @throws NullPointerException if any parameter is {@code null}
   */
  protected CfaValidationResult checkComplete(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
    return pass();
  }

  /** A {@link AbstractCfaValidator} for combining multiple validators. */
  private static final class CompositeCfaValidator extends AbstractCfaValidator {

    private final CfaValidator fst;
    private final CfaValidator snd;

    private CompositeCfaValidator(CfaValidator pFst, CfaValidator pSnd) {
      super(createValidatorFactory(pFst, pSnd));
      fst = pFst;
      snd = pSnd;
    }

    private static BiFunction<CfaNetwork, CfaMetadata, AbstractCfaValidator> createValidatorFactory(
        CfaValidator pFst, CfaValidator pSnd) {
      return (cfaNetwork, cfaMetadata) -> {
        CfaValidator newFst =
            pFst instanceof AbstractCfaValidator
                ? ((AbstractCfaValidator) pFst).validatorFactory.apply(cfaNetwork, cfaMetadata)
                : pFst;
        CfaValidator newSnd =
            pSnd instanceof AbstractCfaValidator
                ? ((AbstractCfaValidator) pSnd).validatorFactory.apply(cfaNetwork, cfaMetadata)
                : pSnd;
        return new CompositeCfaValidator(newFst, newSnd);
      };
    }

    @Override
    protected CfaValidationResult checkNode(CFANode pNode) {
      CfaValidationResult validationResult = CfaValidationResult.VALID;
      for (CfaValidator validator : ImmutableList.of(fst, snd)) {
        if (validator instanceof AbstractCfaValidator) {
          validationResult =
              validationResult.combine(((AbstractCfaValidator) validator).checkNode(pNode));
        }
      }
      return validationResult;
    }

    @Override
    protected CfaValidationResult checkEdge(CFAEdge pEdge) {
      CfaValidationResult validationResult = CfaValidationResult.VALID;
      for (CfaValidator validator : ImmutableList.of(fst, snd)) {
        if (validator instanceof AbstractCfaValidator) {
          validationResult =
              validationResult.combine(((AbstractCfaValidator) validator).checkEdge(pEdge));
        }
      }
      return validationResult;
    }

    @Override
    protected CfaValidationResult checkComplete(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata) {
      CfaValidationResult validationResult = CfaValidationResult.VALID;
      for (CfaValidator validator : ImmutableList.of(fst, snd)) {
        if (validator instanceof AbstractCfaValidator) {
          validationResult =
              validationResult.combine(
                  ((AbstractCfaValidator) validator).checkComplete(pCfaNetwork, pCfaMetadata));
        } else {
          validator.check(pCfaNetwork, pCfaMetadata);
        }
      }
      return validationResult;
    }
  }
}

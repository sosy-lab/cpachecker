// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;

/**
 * A CFA transformer takes a CFA, creates a copy of the CFA, applies some modifications to the copy,
 * and returns the modified copy as the transformed CFA.
 *
 * <p>To implement a CFA transformer, an implementation for {@link
 * CfaTransformer#transform(CfaNetwork, CfaMetadata, LogManager, ShutdownNotifier)} must be
 * provided. The implementation must guarantee that every time the method is called, a new
 * transformed CFA instance is created.
 */
@FunctionalInterface
public interface CfaTransformer {

  /**
   * Returns a new CFA transformer that represents the combination of the specified transformers.
   *
   * <p>The transformers are executed in the order they are specified. The output of a transformer
   * is the input of the next transformer. The input of the combined transformer is the input of the
   * first specified transformer and the output of the combined transformer is the output of the
   * last specified transformer.
   *
   * @param pTransformer the first CFA transformer
   * @param pTransformers additional CFA transformers executed after the first CFA transformer
   * @return a new CFA transformer that represents the combination of the specified transformers
   * @throws NullPointerException if any parameter is {@code null}
   */
  public static CfaTransformer of(CfaTransformer pTransformer, CfaTransformer... pTransformers) {

    checkNotNull(pTransformer);

    ImmutableList<CfaTransformer> transformers = ImmutableList.copyOf(pTransformers);

    return new CfaTransformer() {

      @Override
      public CFA transform(
          CfaNetwork pCfaNetwork,
          CfaMetadata pCfaMetadata,
          LogManager pLogger,
          ShutdownNotifier pShutdownNotifier) {

        CFA transformedCfa =
            pTransformer.transform(pCfaNetwork, pCfaMetadata, pLogger, pShutdownNotifier);
        for (CfaTransformer transformer : transformers) {
          transformedCfa = transformer.transform(transformedCfa, pLogger, pShutdownNotifier);
        }

        return transformedCfa;
      }
    };
  }

  /**
   * Returns a new transformed CFA for the specified CFA.
   *
   * <p>Every time this method is called, a new transformed CFA instance is created.
   *
   * @param pCfa the CFA (represented as a {@link CfaNetwork}) to create a transformed CFA for
   * @param pCfaMetadata the metadata of the specified CFA
   * @param pLogger the logger to use during CFA transformation
   * @return a new transformed CFA for the specified CFA
   * @throws NullPointerException if any parameter is {@code null}
   */
  CFA transform(
      CfaNetwork pCfa,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier);

  /**
   * Returns a new transformed CFA for the specified CFA.
   *
   * <p>Every time this method is called, a new transformed CFA instance is created.
   *
   * @param pCfa the CFA to create a transformed CFA for
   * @param pLogger the logger to use during CFA transformation
   * @return a new transformed CFA for the specified CFA
   * @throws NullPointerException if any parameter is {@code null}
   */
  default CFA transform(CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier) {
    return transform(CfaNetwork.wrap(pCfa), pCfa.getMetadata(), pLogger, pShutdownNotifier);
  }
}

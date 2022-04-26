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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;

@FunctionalInterface
public interface CfaTransformer {

  public static CfaTransformer of(CfaTransformer pTransformer, CfaTransformer... pTransformers) {

    checkNotNull(pTransformer);

    ImmutableList<CfaTransformer> transformers = ImmutableList.copyOf(pTransformers);

    return new CfaTransformer() {

      @Override
      public CFA transform(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger) {

        CFA transformedCfa = pTransformer.transform(pCfaNetwork, pCfaMetadata, pLogger);
        for (CfaTransformer transformer : transformers) {
          transformedCfa = transformer.transform(transformedCfa, pCfaMetadata, pLogger);
        }

        return transformedCfa;
      }
    };
  }

  CFA transform(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger);

  default CFA transform(CFA pCfa, CfaMetadata pCfaMetadata, LogManager pLogger) {
    return transform(CfaNetwork.of(pCfa), pCfaMetadata, pLogger);
  }
}

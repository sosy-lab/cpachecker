// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaProcessor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.transformer.CfaCreator;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeConverter;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeConverter;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;

public final class CCfaTransformer implements CfaTransformer {

  private final ImmutableList<CfaProcessor> cfaProcessors;

  private final CfaNodeConverter nodeConverter;
  private final CfaEdgeConverter edgeConverter;

  private CCfaTransformer(
      ImmutableList<CfaProcessor> pCfaProcessors,
      ImmutableList<CCfaNodeAstSubstitution> pNodeAstSubstitutions,
      ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions) {

    cfaProcessors = pCfaProcessors;

    nodeConverter = CCfaNodeConverter.forSubstitutions(pNodeAstSubstitutions);
    edgeConverter = CCfaEdgeConverter.forSubstitutions(pEdgeAstSubstitutions);
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public CFA transform(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger) {
    return CfaCreator.createCfa(
        cfaProcessors, pCfaNetwork, nodeConverter, edgeConverter, pCfaMetadata, pLogger);
  }

  public static final class Builder {

    private final ImmutableList.Builder<CfaProcessor> cfaProcessors;

    private final ImmutableList.Builder<CCfaNodeAstSubstitution> nodeAstSubstitutions;
    private final ImmutableList.Builder<CCfaEdgeAstSubstitution> edgeAstSubstitutions;

    private Builder() {

      cfaProcessors = ImmutableList.builder();

      nodeAstSubstitutions = ImmutableList.builder();
      edgeAstSubstitutions = ImmutableList.builder();
    }

    public Builder addCfaProcessor(CfaProcessor pCfaProcessor) {

      cfaProcessors.add(pCfaProcessor);

      return this;
    }

    public Builder addNodeAstSubstitution(CCfaNodeAstSubstitution pNodeAstSubstitution) {

      nodeAstSubstitutions.add(pNodeAstSubstitution);

      return this;
    }

    public Builder addEdgeAstSubstitution(CCfaEdgeAstSubstitution pEdgeAstSubstitution) {

      edgeAstSubstitutions.add(pEdgeAstSubstitution);

      return this;
    }

    public CfaTransformer build() {
      return new CCfaTransformer(
          cfaProcessors.build(), nodeAstSubstitutions.build(), edgeAstSubstitutions.build());
    }
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.transformer.CfaCreator;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;

/** A {@link CfaTransformer} for transforming CFAs of C programs. */
public final class CCfaTransformer implements CfaTransformer {

  private final ImmutableList<CfaPostProcessor> cfaPostProcessors;

  private final CfaNodeTransformer nodeTransformer;
  private final CfaEdgeTransformer edgeTransformer;

  private final Configuration config;

  private CCfaTransformer(
      ImmutableList<CfaPostProcessor> pCfaPostProcessors,
      ImmutableList<CCfaNodeAstSubstitution> pNodeAstSubstitutions,
      ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions,
      Configuration pConfig) {

    cfaPostProcessors = pCfaPostProcessors;

    nodeTransformer = CCfaNodeTransformer.withSubstitutions(pNodeAstSubstitutions);
    edgeTransformer = CCfaEdgeTransformer.withSubstitutions(pEdgeAstSubstitutions);

    config = pConfig;
  }

  /**
   * Returns a new {@link CCfaTransformer.Builder} instance.
   *
   * @return a new {@link CCfaTransformer.Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public CFA transform(
      CfaNetwork pCfaNetwork,
      CfaMetadata pCfaMetadata,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier) {

    CfaNetwork unconnectedFunctionCfa =
        CfaCreator.toUnconnectedFunctionCfaNetwork(
            pCfaNetwork, CCfaEdgeTransformer.SUMMARY_TO_STATEMENT_EDGE_TRANSFORMER);
    CfaMetadata unconnectedFunctionCfaMetadata =
        pCfaMetadata.withConnectedness(CfaConnectedness.UNCONNECTED_FUNCTIONS);

    return CfaCreator.createCfa(
        cfaPostProcessors,
        nodeTransformer,
        edgeTransformer,
        unconnectedFunctionCfa,
        unconnectedFunctionCfaMetadata,
        config,
        pLogger);
  }

  public static final class Builder {

    private final ImmutableList.Builder<CfaPostProcessor> cfaPostProcessors;

    private final ImmutableList.Builder<CCfaNodeAstSubstitution> nodeAstSubstitutions;
    private final ImmutableList.Builder<CCfaEdgeAstSubstitution> edgeAstSubstitutions;

    private Builder() {

      cfaPostProcessors = ImmutableList.builder();

      nodeAstSubstitutions = ImmutableList.builder();
      edgeAstSubstitutions = ImmutableList.builder();
    }

    /**
     * Adds a {@link CfaPostProcessor} that is executed during CFA construction.
     *
     * <p>Different kinds of CFA post-processors are executed in the order defined in {@link
     * CfaPostProcessor}, even if CFA post-processors are added in a different order. CFA
     * post-processors that are executed in the same step are executed in the order they are added
     * to this builder.
     *
     * @param pCfaPostProcessor the CFA post-processor to add for CFA construction
     * @return this builder instance
     */
    public Builder addPostProcessor(CfaPostProcessor pCfaPostProcessor) {

      cfaPostProcessors.add(pCfaPostProcessor);

      return this;
    }

    /**
     * Adds a {@link CCfaNodeAstSubstitution} that is executed during CFA creation of s {@link
     * CfaTransformer} created by this builder.
     *
     * <p>AST substitutions are applied in the order they are added to this builder.
     *
     * @param pNodeAstSubstitution the AST substitution to add
     * @return this builder instance
     */
    public Builder addNodeAstSubstitution(CCfaNodeAstSubstitution pNodeAstSubstitution) {

      nodeAstSubstitutions.add(pNodeAstSubstitution);

      return this;
    }

    /**
     * Adds a {@link CCfaEdgeAstSubstitution} that is executed during CFA creation of a {@link
     * CfaTransformer} created by this builder.
     *
     * <p>AST substitutions are applied in the order they are added to this builder.
     *
     * @param pEdgeAstSubstitution the AST substitution to add
     * @return this builder instance
     */
    public Builder addEdgeAstSubstitution(CCfaEdgeAstSubstitution pEdgeAstSubstitution) {

      edgeAstSubstitutions.add(pEdgeAstSubstitution);

      return this;
    }

    /**
     * Returns a new {@link CfaTransformer} instance created from the current state of this builder.
     *
     * @param pConfig the configuration to use during CFA creation
     * @return a new {@link CfaTransformer} instance created from the current state of this builder
     */
    public CfaTransformer build(Configuration pConfig) {
      return new CCfaTransformer(
          cfaPostProcessors.build(),
          nodeAstSubstitutions.build(),
          edgeAstSubstitutions.build(),
          checkNotNull(pConfig));
    }
  }
}

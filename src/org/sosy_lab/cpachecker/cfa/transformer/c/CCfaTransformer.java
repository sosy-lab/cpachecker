// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.transformer.CfaBuilder;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeSubstitution;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;

/** A {@link CfaTransformer} for transforming CFAs of C programs. */
public final class CCfaTransformer implements CfaTransformer {

  private final ImmutableList<CfaPostProcessor> functionPostProcessors;
  private final ImmutableList<CfaPostProcessor> supergraphPostProcessors;

  private final CfaNodeTransformer nodeTransformer;
  private final CfaEdgeTransformer edgeTransformer;

  private CCfaTransformer(
      ImmutableList<CfaPostProcessor> pFunctionPostProcessors,
      ImmutableList<CfaPostProcessor> pSupergraphPostProcessors,
      ImmutableList<CCfaNodeAstSubstitution> pNodeAstSubstitutions,
      ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions) {

    functionPostProcessors = pFunctionPostProcessors;
    supergraphPostProcessors = pSupergraphPostProcessors;

    nodeTransformer = CCfaNodeTransformer.withSubstitutions(pNodeAstSubstitutions);
    edgeTransformer = CCfaEdgeTransformer.withSubstitutions(pEdgeAstSubstitutions);
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

    CfaNetwork cfaWithoutSuperEdges = pCfaNetwork.withoutSuperEdges();
    CfaNetwork unconnectedFunctionCfa =
        cfaWithoutSuperEdges.transformEdges(
            edge -> {
              if (edge instanceof FunctionSummaryEdge) {
                return CCfaEdgeTransformer.SUMMARY_TO_STATEMENT_EDGE_TRANSFORMER.transform(
                    edge, cfaWithoutSuperEdges, node -> node, CfaEdgeSubstitution.UNSUPPORTED);
              }

              return edge;
            });
    CfaMetadata unconnectedFunctionCfaMetadata =
        pCfaMetadata.withConnectedness(CfaConnectedness.UNCONNECTED_FUNCTIONS);

    CfaBuilder builder =
        CfaBuilder.builder(
            pLogger,
            pShutdownNotifier,
            nodeTransformer,
            edgeTransformer,
            unconnectedFunctionCfa,
            unconnectedFunctionCfaMetadata);
    functionPostProcessors.forEach(builder::runPostProcessor);
    builder.toSupergraph();
    supergraphPostProcessors.forEach(builder::runPostProcessor);

    return builder.createCfa();
  }

  public static final class Builder {

    private final ImmutableList.Builder<CfaPostProcessor> functionPostProcessors;
    private final ImmutableList.Builder<CfaPostProcessor> supergraphPostProcessors;

    private final ImmutableList.Builder<CCfaNodeAstSubstitution> nodeAstSubstitutions;
    private final ImmutableList.Builder<CCfaEdgeAstSubstitution> edgeAstSubstitutions;

    private Builder() {

      functionPostProcessors = ImmutableList.builder();
      supergraphPostProcessors = ImmutableList.builder();

      nodeAstSubstitutions = ImmutableList.builder();
      edgeAstSubstitutions = ImmutableList.builder();
    }

    /**
     * Adds a {@link CfaPostProcessor} that is executed during CFA construction on unconnected
     * function CFAs.
     *
     * @param pCfaPostProcessor the CFA post-processor to add for CFA construction
     * @return this builder instance
     */
    public Builder addFunctionPostProcessor(CfaPostProcessor pCfaPostProcessor) {

      functionPostProcessors.add(pCfaPostProcessor);

      return this;
    }

    /**
     * Adds a {@link CfaPostProcessor} that is executed during CFA construction on the supergraph
     * CFA.
     *
     * @param pCfaPostProcessor the CFA post-processor to add for CFA construction
     * @return this builder instance
     */
    public Builder addSupergraphPostProcessor(CfaPostProcessor pCfaPostProcessor) {

      supergraphPostProcessors.add(pCfaPostProcessor);

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
     * @return a new {@link CfaTransformer} instance created from the current state of this builder
     */
    public CfaTransformer build() {
      return new CCfaTransformer(
          functionPostProcessors.build(),
          supergraphPostProcessors.build(),
          nodeAstSubstitutions.build(),
          edgeAstSubstitutions.build());
    }
  }
}

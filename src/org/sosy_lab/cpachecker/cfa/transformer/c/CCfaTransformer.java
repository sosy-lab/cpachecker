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
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaNodeTransformer;
import org.sosy_lab.cpachecker.cfa.transformer.CfaTransformer;

/** A {@link CfaTransformer} for transforming CFAs whose language is C. */
public final class CCfaTransformer implements CfaTransformer {

  private final ImmutableList<CfaProcessor> cfaProcessors;

  private final CfaNodeTransformer nodeTransformer;
  private final CfaEdgeTransformer edgeTransformer;

  private CCfaTransformer(
      ImmutableList<CfaProcessor> pCfaProcessors,
      ImmutableList<CCfaNodeAstSubstitution> pNodeAstSubstitutions,
      ImmutableList<CCfaEdgeAstSubstitution> pEdgeAstSubstitutions) {

    cfaProcessors = pCfaProcessors;

    nodeTransformer = CCfaNodeTransformer.forSubstitutions(pNodeAstSubstitutions);
    edgeTransformer = CCfaEdgeTransformer.forSubstitutions(pEdgeAstSubstitutions);
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
  public CFA transform(CfaNetwork pCfaNetwork, CfaMetadata pCfaMetadata, LogManager pLogger) {
    return CfaCreator.createCfa(
        cfaProcessors, pCfaNetwork, nodeTransformer, edgeTransformer, pCfaMetadata, pLogger);
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

    /**
     * Adds a {@link CfaProcessor} that is executed during CFA creation of a {@link CfaTransformer}
     * created by this builder.
     *
     * <p>Different kinds of CFA processors are executed in the order defined in {@link
     * CfaProcessor}, even if CFA processors are added in a different order. CFA processors that are
     * executed in the same step are executed in the order they are added to this builder.
     *
     * @param pCfaProcessor the CFA processor to add for CFA creation
     * @return this builder instance
     */
    public Builder addCfaProcessor(CfaProcessor pCfaProcessor) {

      cfaProcessors.add(pCfaProcessor);

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
          cfaProcessors.build(), nodeAstSubstitutions.build(), edgeAstSubstitutions.build());
    }
  }
}

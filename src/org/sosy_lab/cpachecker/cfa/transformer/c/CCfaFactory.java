// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.transformer.c;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.CfaConnectedness;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.transformer.CfaEdgeProvider;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactories;
import org.sosy_lab.cpachecker.cfa.transformer.CfaFactory;

/**
 * A utility class for creating {@link CfaFactory} instances that create C program CFAs for CFAs
 * represented by ({@link CfaNetwork}, {@link CfaMetadata}) pairs.
 *
 * <p>The recommended way of creating {@link CfaFactory} instances is by chaining method calls.
 * After every method call, the {@link CfaFactory} can be used directly, or a {@link CfaFactory} can
 * be created that differs from the previous {@link CfaFactory} (e.g., it executes an additional CFA
 * post-processor). During CFA construction, the order of method chain calls is taken into account
 * (e.g, the first {@code executePostProcessor(...)} call defines the first CFA post-processor to
 * execute during CFA construction).
 *
 * <p>Example usages:
 *
 * <pre>{@code
 * CfaFactory cfaFactory =
 *     CCfaFactory
 *         .toUnconnectedFunctions()
 *         .transformNodes(...)
 *         .transformEdges(...)
 *         .executePostProcessor(...) // first CFA post-processor executed during CFA construction
 *         .executePostProcessors(...)
 *         .executePostProcessor(...) // last CFA post-processor executed before supergraph construction
 *         .toSupergraph()
 *         .executePostProcessor(...); // last CFA post-processor executed during CFA construction
 * }</pre>
 *
 * <pre>{@code
 * CfaFactory cfaFactory =
 *     CCfaFactory
 *         .transformNodes(...)
 *         .executePostProcessor(...);
 * }</pre>
 *
 * <pre>{@code
 * CfaFactory cfaFactory = CCfaFactory.transformEdges(...);
 * }</pre>
 */
public final class CCfaFactory {

  /**
   * A {@link CfaFactory} that creates CFAs that resemble the CFA represented by the given ({@link
   * CfaNetwork}, {@link CfaMetadata}) pair instances as closely as possible. No CFA post-processors
   * are executed.
   */
  public static final CfaFactory CLONER =
      CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of());

  private CCfaFactory() {}

  private static CfaFactories.State<CCfaNodeTransformer, CCfaEdgeTransformer> initialState() {
    return CfaFactories.createInitialState(CCfaNodeTransformer.CLONER, CCfaEdgeTransformer.CLONER);
  }

  private static CFAEdge summaryEdgeToStatementEdge(CFAEdge pEdge, CfaNetwork pCfaNetwork) {
    if (pEdge instanceof FunctionSummaryEdge) {
      return CCfaEdgeTransformer.SUMMARY_TO_STATEMENT_EDGE_TRANSFORMER.transform(
          pEdge, pCfaNetwork, node -> node, CfaEdgeProvider.UNSUPPORTED);
    } else {
      return pEdge;
    }
  }

  /**
   * Returns a {@link CfaFactory} that transforms a {@link CfaNetwork} of any connectedness to a CFA
   * consisting of unconnected functions (similar to {@link CfaConnectedness#UNCONNECTED_FUNCTIONS
   * unconnected function CFAs}, just as a single CFA that contains multiple unconnected function
   * CFAs).
   *
   * @return a {@link CfaFactory} that transforms a {@link CfaNetwork} of any connectedness to a CFA
   *     consisting of unconnected functions
   */
  public static CfaFactories.InitialCfaFactory<CCfaNodeTransformer, CCfaEdgeTransformer>
      toUnconnectedFunctions() {
    CfaFactories.Step<CCfaNodeTransformer, CCfaEdgeTransformer> step =
        (state, logger, shutdownNotifier) -> {
          CfaNetwork cfaWithoutSuperEdges = state.getCfaNetwork().withoutSuperEdges();
          CfaNetwork unconnectedFunctionCfaNetwork =
              cfaWithoutSuperEdges.transformEdges(
                  edge -> summaryEdgeToStatementEdge(edge, cfaWithoutSuperEdges));

          CfaMetadata unconnectedFunctionCfaMetadata =
              state.getCfaMetadata().withConnectedness(CfaConnectedness.UNCONNECTED_FUNCTIONS);

          return state
              .withCfaNetwork(unconnectedFunctionCfaNetwork)
              .withCfaMetadata(unconnectedFunctionCfaMetadata);
        };

    return CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of(step));
  }

  /**
   * Returns a {@link CfaFactory} that uses the specified CFA node transformer for node creation.
   *
   * <p>The specified transformer is used to create all nodes in CFAs created by the returned {@link
   * CfaFactory}.
   *
   * @param pCfaNodeTransformer the CFA node transformer to use during CFA construction
   * @return a {@link CfaFactory} that uses the specified CFA node transformer for node creation
   * @throws NullPointerException if {@code pCfaNodeTransformer == null}
   */
  public static CfaFactories.NodeTransformingCfaFactory<CCfaNodeTransformer, CCfaEdgeTransformer>
      transformNodes(CCfaNodeTransformer pCfaNodeTransformer) {
    return CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of())
        .transformNodes(pCfaNodeTransformer);
  }

  /**
   * Returns a {@link CfaFactory} that uses the specified CFA edge transformer for edge creation.
   *
   * <p>The specified transformer is used to create all edges in CFAs created by the returned {@link
   * CfaFactory}.
   *
   * @param pCfaEdgeTransformer the CFA edge transformer to use during CFA construction
   * @return a {@link CfaFactory} that uses the specified CFA edge transformer for edge creation
   * @throws NullPointerException if {@code pCfaEdgeTransformer == null}
   */
  public static CfaFactories.AnyConnectednessCfaFactory<CCfaNodeTransformer, CCfaEdgeTransformer>
      transformEdges(CCfaEdgeTransformer pCfaEdgeTransformer) {
    return CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of())
        .transformEdges(pCfaEdgeTransformer);
  }

  /**
   * Returns a {@link CfaFactory} that executes the specified CFA post-processor.
   *
   * <p>The execution order of CFA post-processors is the order in which they are specified. If
   * multiple {@code executePostProcessor} calls are chained, the CFA post-processors are executed
   * in the order they appear in the chain.
   *
   * @param pCfaPostProcessor the CFA post-processor to execute during CFA construction
   * @return a {@link CfaFactory} that executes the specified CFA post-processor
   * @throws NullPointerException if {@code pCfaPostProcessor == null}
   */
  public static CfaFactories.AnyConnectednessCfaFactory<CCfaNodeTransformer, CCfaEdgeTransformer>
      executePostProcessor(CfaPostProcessor pCfaPostProcessor) {
    return CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of())
        .executePostProcessor(pCfaPostProcessor);
  }

  /**
   * Returns a {@link CfaFactory} that executes the specified CFA post-processors.
   *
   * <p>The execution order of CFA post-processors is the order in which they are specified. If
   * multiple {@code executePostProcessor} calls are chained, the CFA post-processors are executed
   * in the order they appear in the chain. Additionally, the order of {@code pCfaPostProcessors} is
   * taken into account.
   *
   * @param pCfaPostProcessors the CFA post-processors to execute during CFA construction
   * @return a {@link CfaFactory} that executes the specified CFA post-processors
   * @throws NullPointerException if {@code pCfaPostProcessors == null} or any of its elements is
   *     {@code null}
   */
  public static CfaFactories.AnyConnectednessCfaFactory<CCfaNodeTransformer, CCfaEdgeTransformer>
      executePostProcessors(Iterable<CfaPostProcessor> pCfaPostProcessors) {
    return CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of())
        .executePostProcessors(pCfaPostProcessors);
  }

  /**
   * Returns a {@link CfaFactory} that builds the supergraph.
   *
   * @return a {@link CfaFactory} that builds the supergraph
   * @throws IllegalStateException if the CFA during the current construction step is already a
   *     supergraph
   */
  public static CfaFactories.SupergraphCfaFactory<CCfaNodeTransformer, CCfaEdgeTransformer>
      toSupergraph() {
    return CfaFactories.InitialCfaFactory.create(initialState(), ImmutableList.of()).toSupergraph();
  }
}

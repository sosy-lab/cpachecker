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
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CfaMetadata;
import org.sosy_lab.cpachecker.cfa.CfaPostProcessor;
import org.sosy_lab.cpachecker.cfa.graph.CfaNetwork;

/**
 * Contains classes that make building {@link CfaFactory} instances easier.
 *
 * <p>The recommended way of creating {@link CfaFactory} instances is by chaining method calls.
 * After every method call, the {@link CfaFactory} can be used directly, or a {@link CfaFactory} can
 * be created that differs from the previous {@link CfaFactory} (e.g., it executes one more CFA
 * post-processor). During CFA construction, the order of method chain calls is taken into account
 * (e.g, the first {@code executePostProcessor(...)} call defines the first CFA post-processor to
 * execute during CFA construction).
 *
 * <p>Example usages (if the factories in this class are used as intended for building {@link
 * CfaFactory} instances of {@code Some} language):
 *
 * <pre>{@code
 * CfaFactory cfaFactory =
 *     SomeCfaFactory
 *        .toUnconnectedFunctions()
 *        .transformNodes(...)
 *        .transformEdges(...)
 *        .executePostProcessor(...) // first CFA post-processor executed during CFA construction
 *        .executePostProcessors(...)
 *        .executePostProcessor(...) // last CFA post-processor executed before supergraph construction
 *        .toSupergraph()
 *        .executePostProcessor(...); // last CFA post-processor executed during CFA construction
 * }</pre>
 *
 * <pre>{@code
 * CfaFactory cfaFactory =
 *     SomeCfaFactory
 *        .transformNodes(...)
 *        .executePostProcessor(...);
 * }</pre>
 *
 * <pre>{@code
 * CfaFactory cfaFactory = SomeCfaFactory.transformEdges(...);
 * }</pre>
 */
public final class CfaFactories {

  private CfaFactories() {}

  private static <E> ImmutableList<E> combine(List<E> pFstList, List<E> pSndList) {
    return ImmutableList.<E>builderWithExpectedSize(pFstList.size() + pSndList.size())
        .addAll(pFstList)
        .addAll(pSndList)
        .build();
  }

  /**
   * Creates the initial {@link State}.
   *
   * @param pDefaultCfaNodeTransformer the default {@link CfaNodeTransformer} that is used if no
   *     other {@link CfaNodeTransformer} is later specified
   * @param pDefaultCfaEdgeTransformer the default {@link CfaEdgeTransformer} that is used if no
   *     other {@link CfaEdgeTransformer} is later specified
   * @throws NullPointerException if any parameter is {@code null}
   */
  public static <N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
      State<N, E> createInitialState(N pDefaultCfaNodeTransformer, E pDefaultCfaEdgeTransformer) {
    return new State<>(
        null,
        null,
        checkNotNull(pDefaultCfaNodeTransformer),
        checkNotNull(pDefaultCfaEdgeTransformer),
        null);
  }

  /**
   * Defines a single step executed during CFA construction (e.g., executing a CFA post-processor).
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  public static interface Step<N extends CfaNodeTransformer, E extends CfaEdgeTransformer> {

    /**
     * Executes a single step during CFA construction (e.g., running a CFA post-processor).
     *
     * @param pState the state before this step is executed
     * @param pLogger the logger to use during step execution
     * @param pShutdownNotifier the shutdown notifier to use during step execution
     * @return the state after executing this step
     */
    State<N, E> execute(State<N, E> pState, LogManager pLogger, ShutdownNotifier pShutdownNotifier);
  }

  /**
   * The state that is passed from {@link Step step} to {@link Step step}.
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  public static final class State<N extends CfaNodeTransformer, E extends CfaEdgeTransformer> {

    // `null` before we know what `CfaNetwork` and `CfaMetadata` to use for CFA construction.
    // Sometimes we want to use a modified `CfaNetwork` view, so we have to create that in a prior
    // step and than set these fields.
    private final @Nullable CfaNetwork cfaNetwork;
    private final @Nullable CfaMetadata cfaMetadata;

    private final N cfaNodeTransformer;
    private final E cfaEdgeTransformer;

    // `null` before we known the `CfaNodeTransformer`, `CfaEdgeTransformer`, `CfaNetwork`, and
    // `CfaMetadata` to use for CFA construction.
    private final @Nullable CfaBuilder cfaBuilder;

    private State(
        @Nullable CfaNetwork pCfaNetwork,
        @Nullable CfaMetadata pCfaMetadata,
        N pCfaNodeTransformer,
        E pCfaEdgeTransformer,
        @Nullable CfaBuilder pCfaBuilder) {

      cfaNetwork = pCfaNetwork;
      cfaMetadata = pCfaMetadata;

      cfaNodeTransformer = checkNotNull(pCfaNodeTransformer);
      cfaEdgeTransformer = checkNotNull(pCfaEdgeTransformer);

      cfaBuilder = pCfaBuilder;
    }

    public CfaNetwork getCfaNetwork() {
      return checkNotNull(cfaNetwork);
    }

    public CfaMetadata getCfaMetadata() {
      return checkNotNull(cfaMetadata);
    }

    private CfaBuilder getCfaBuilderOrCreate(
        LogManager pLogger, ShutdownNotifier pShutdownNotifier) {

      if (cfaBuilder != null) {
        return cfaBuilder;
      }

      return CfaBuilder.builder(
          pLogger,
          pShutdownNotifier,
          cfaNodeTransformer,
          cfaEdgeTransformer,
          cfaNetwork,
          cfaMetadata);
    }

    public State<N, E> withCfaNetwork(CfaNetwork pCfaNetwork) {
      return new State<>(
          pCfaNetwork, cfaMetadata, cfaNodeTransformer, cfaEdgeTransformer, cfaBuilder);
    }

    public State<N, E> withCfaMetadata(CfaMetadata pCfaMetadata) {
      return new State<>(
          cfaNetwork, pCfaMetadata, cfaNodeTransformer, cfaEdgeTransformer, cfaBuilder);
    }

    private State<N, E> withCfaNodeTransformer(N pCfaNodeTransformer) {
      return new State<>(
          cfaNetwork, cfaMetadata, pCfaNodeTransformer, cfaEdgeTransformer, cfaBuilder);
    }

    private State<N, E> withCfaEdgeTransformer(E pCfaEdgeTransformer) {
      return new State<>(
          cfaNetwork, cfaMetadata, cfaNodeTransformer, pCfaEdgeTransformer, cfaBuilder);
    }

    private State<N, E> withCfaBuilder(CfaBuilder pCfaBuilder) {
      return new State<>(
          cfaNetwork, cfaMetadata, cfaNodeTransformer, cfaEdgeTransformer, pCfaBuilder);
    }
  }

  /**
   * Superclass for all other chainable {@link CfaFactory} implementations. Implements how {@link
   * Step steps} are executes to create the final CFA.
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  private abstract static class AbstractCfaFactory<
          N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
      implements CfaFactory {

    private final State<N, E> initialState;
    private final ImmutableList<Step<N, E>> steps;

    protected AbstractCfaFactory(State<N, E> pInitialState, List<Step<N, E>> pSteps) {
      initialState = pInitialState;
      steps = ImmutableList.copyOf(pSteps);
    }

    protected final State<N, E> getInitialState() {
      return initialState;
    }

    protected final ImmutableList<Step<N, E>> getSteps() {
      return steps;
    }

    @Override
    public final CFA createCfa(
        CfaNetwork pCfaNetwork,
        CfaMetadata pCfaMetadata,
        LogManager pLogger,
        ShutdownNotifier pShutdownNotifier) {

      State<N, E> state = initialState.withCfaNetwork(pCfaNetwork).withCfaMetadata(pCfaMetadata);
      for (Step<N, E> step : steps) {
        state = step.execute(state, pLogger, pShutdownNotifier);
      }

      return state.getCfaBuilderOrCreate(pLogger, pShutdownNotifier).createCfa();
    }
  }

  /**
   * Class to use after the supergraph CFA has been built. We are only allowed to execute CFA
   * post-processors (on the supergraph).
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  public static class SupergraphCfaFactory<
          N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
      extends AbstractCfaFactory<N, E> {

    private SupergraphCfaFactory(State<N, E> pInitialState, List<Step<N, E>> pSteps) {
      super(pInitialState, pSteps);
    }

    private Step<N, E> executeCfaPostProcessorStep(CfaPostProcessor pCfaPostProcessor) {
      return (state, logger, shutdownNotifier) -> {
        CfaBuilder cfaBuilder = state.getCfaBuilderOrCreate(logger, shutdownNotifier);
        cfaBuilder.runPostProcessor(pCfaPostProcessor);

        return state.withCfaBuilder(cfaBuilder);
      };
    }

    /**
     * Returns a {@link CfaFactory} that does what this {@link CfaFactory} does and additionally
     * executes the specified CFA post-processor.
     *
     * <p>The execution order of CFA post-processors is the order in which they are specified. If
     * multiple {@code executePostProcessor} calls are chained, the CFA post-processors are executed
     * in the order they appear in the chain.
     *
     * @param pCfaPostProcessor the CFA post-processor to execute during CFA construction
     * @return a {@link CfaFactory} that does what this {@link CfaFactory} does and additionally
     *     executes the specified CFA post-processor
     * @throws NullPointerException if {@code pCfaPostProcessor == null}
     */
    public SupergraphCfaFactory<N, E> executePostProcessor(CfaPostProcessor pCfaPostProcessor) {

      Step<N, E> step = executeCfaPostProcessorStep(pCfaPostProcessor);

      return new SupergraphCfaFactory<>(
          getInitialState(), combine(getSteps(), ImmutableList.of(step)));
    }

    /**
     * Returns a {@link CfaFactory} that does what this {@link CfaFactory} does and additionally
     * executes the specified CFA post-processor.
     *
     * <p>The execution order of CFA post-processors is the order in which they are specified. If
     * multiple {@code executePostProcessors} calls are chained, the CFA post-processors are
     * executed in the order they appear in the chain. Additionally, the order of {@code
     * pCfaPostProcessors} is taken into account.
     *
     * @param pCfaPostProcessors the CFA post-processors to execute during CFA construction
     * @return a {@link CfaFactory} that does what this {@link CfaFactory} does and additionally
     *     executes the specified CFA post-processors
     * @throws NullPointerException if {@code pCfaPostProcessors == null} or any of its elements is
     *     {@code null}
     */
    public SupergraphCfaFactory<N, E> executePostProcessors(
        Iterable<CfaPostProcessor> pCfaPostProcessors) {

      checkNotNull(pCfaPostProcessors);

      ImmutableList.Builder<Step<N, E>> steps = ImmutableList.builder();
      for (CfaPostProcessor cfaPostProcessor : pCfaPostProcessors) {
        steps.add(executeCfaPostProcessorStep(checkNotNull(cfaPostProcessor)));
      }

      return new SupergraphCfaFactory<>(getInitialState(), combine(getSteps(), steps.build()));
    }
  }

  /**
   * Class to use after CFA node/edge transformers have been chosen. We are only allowed to execute
   * CFA post-processors and build the supergraph (we don't assume any connectedness).
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  public static class AnyConnectednessCfaFactory<
          N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
      extends SupergraphCfaFactory<N, E> {

    private AnyConnectednessCfaFactory(State<N, E> pInitialState, List<Step<N, E>> pSteps) {
      super(pInitialState, pSteps);
    }

    @Override
    public AnyConnectednessCfaFactory<N, E> executePostProcessor(
        CfaPostProcessor pCfaPostProcessor) {
      return new AnyConnectednessCfaFactory<>(
          getInitialState(), super.executePostProcessor(pCfaPostProcessor).getSteps());
    }

    @Override
    public AnyConnectednessCfaFactory<N, E> executePostProcessors(
        Iterable<CfaPostProcessor> pCfaPostProcessors) {
      return new AnyConnectednessCfaFactory<>(
          getInitialState(), super.executePostProcessors(pCfaPostProcessors).getSteps());
    }

    /**
     * Returns a {@link CfaFactory} that does what this {@link CfaFactory} does and additionally
     * builds the supergraph CFA.
     *
     * @return a {@link CfaFactory} that does what this {@link CfaFactory} does and additionally
     *     builds the supergraph CFA
     */
    public SupergraphCfaFactory<N, E> toSupergraph() {

      Step<N, E> step =
          (state, logger, shutdownNotifier) -> {
            CfaBuilder cfaBuilder = state.getCfaBuilderOrCreate(logger, shutdownNotifier);
            cfaBuilder.toSupergraph();

            return state.withCfaBuilder(cfaBuilder);
          };

      return new SupergraphCfaFactory<>(
          getInitialState(), combine(getSteps(), ImmutableList.of(step)));
    }
  }

  /**
   * Class to use after a {@link CfaNodeTransformer} has been chosen.
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  public static class NodeTransformingCfaFactory<
          N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
      extends AnyConnectednessCfaFactory<N, E> {

    private NodeTransformingCfaFactory(State<N, E> pInitialState, List<Step<N, E>> pSteps) {
      super(pInitialState, pSteps);
    }

    /**
     * Returns a {@link CfaFactory} that does what this {@link CfaFactory} does, but uses the
     * specified {@link CfaEdgeTransformer}.
     *
     * <p>The specified transformer is used to create all edges in CFAs created by the returned
     * {@link CfaFactory}.
     *
     * @param pCfaEdgeTransformer the {@link CfaEdgeTransformer} to use during CFA construction
     * @return a {@link CfaFactory} that does what this {@link CfaFactory} does, but uses the
     *     specified {@link CfaEdgeTransformer}
     * @throws NullPointerException if {@code pCfaEdgeTransformer == null}
     */
    public AnyConnectednessCfaFactory<N, E> transformEdges(E pCfaEdgeTransformer) {

      checkNotNull(pCfaEdgeTransformer);

      Step<N, E> step =
          (state, logger, shutdownNotifier) -> state.withCfaEdgeTransformer(pCfaEdgeTransformer);

      return new AnyConnectednessCfaFactory<>(
          getInitialState(), combine(getSteps(), ImmutableList.of(step)));
    }
  }

  /**
   * {@link InitialCfaFactory} has the most methods for creating new {@link CfaFactory} instances,
   * because we haven't done anything that limits our choices.
   *
   * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
   * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
   */
  public static final class InitialCfaFactory<
          N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
      extends NodeTransformingCfaFactory<N, E> {

    private InitialCfaFactory(State<N, E> pInitialState, List<Step<N, E>> pSteps) {
      super(pInitialState, pSteps);
    }

    /**
     * Creates a {@link InitialCfaFactory} for the specified initial state and prior steps.
     *
     * @param <N> the type of {@code CfaNodeTransformer} used during CFA construction
     * @param <E> the type of {@code CfaEdgeTransformer} used during CFA construction
     * @param pInitialState the {@link CfaFactories#createInitialState(CfaNodeTransformer,
     *     CfaEdgeTransformer) initial state} to use for CFA construction
     * @param pPriorSteps any prior steps (e.g, the creation of a modified `CfaNetwork` view)
     * @return a {@link InitialCfaFactory} for the specified initial state and prior steps
     * @throws NullPointerException if any parameter is {@code null}
     */
    public static <N extends CfaNodeTransformer, E extends CfaEdgeTransformer>
        InitialCfaFactory<N, E> create(State<N, E> pInitialState, List<Step<N, E>> pPriorSteps) {
      return new InitialCfaFactory<>(checkNotNull(pInitialState), checkNotNull(pPriorSteps));
    }

    /**
     * Returns a {@link CfaFactory} that does what this {@link CfaFactory} does, but uses the
     * specified {@link CfaNodeTransformer}.
     *
     * <p>The specified transformer is used to create all nodes in CFAs created by the returned
     * {@link CfaFactory}.
     *
     * @param pCfaNodeTransformer the {@link CfaNodeTransformer} to use during CFA construction
     * @return a {@link CfaFactory} that does what this {@link CfaFactory} does, but uses the
     *     specified {@link CfaNodeTransformer}
     * @throws NullPointerException if {@code pCfaNodeTransformer == null}
     */
    public NodeTransformingCfaFactory<N, E> transformNodes(N pCfaNodeTransformer) {

      checkNotNull(pCfaNodeTransformer);

      Step<N, E> step =
          (state, logger, shutdownNotifier) -> state.withCfaNodeTransformer(pCfaNodeTransformer);

      return new NodeTransformingCfaFactory<>(
          getInitialState(), combine(getSteps(), ImmutableList.of(step)));
    }
  }
}

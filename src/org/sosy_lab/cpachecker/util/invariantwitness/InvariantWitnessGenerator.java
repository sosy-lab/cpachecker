// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.util.invariantwitness;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.invariants.AbstractInvariantGenerator;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.DownwardCastingVisitor;
import org.sosy_lab.cpachecker.util.expressions.DownwardCastingVisitor.IncompatibleLeafTypesException;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTreeFactory;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.invariantwitness.exchange.entryimport.InvariantWitnessProvider;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/**
 * Wraps an invariant witness provider as invariant generator.
 *
 * <p>The underlying provider may access external resources. The provider (and its resource) is
 * managed (and thus closed) by this class.
 */
public class InvariantWitnessGenerator extends AbstractInvariantGenerator implements AutoCloseable {
  private final InvariantWitnessProvider provider;
  private final LogManager logger;

  private InvariantWitnessGenerator(LogManager pLogger, InvariantWitnessProvider pProvider) {
    provider = pProvider;
    logger = pLogger;
  }

  /**
   * Returns a new instance of this class. This generator returns unmodifiable suppliers. That is,
   * the suppliers only represent a snapshot and are not updated automatically.
   *
   * <p>The generator supports {@link InvariantSupplier}s and {@link ExpressionTreeSupplier}s.
   *
   * <p>The generator produces invariants only on-demand (i.e. when a supplier is requested) and in
   * the calling thread.
   *
   * <p>The underlying WitnessProvider is created with {@link
   * InvariantWitnessProvider#getNewFromDiskWitnessProvider(Configuration, CFA, LogManager,
   * ShutdownNotifier)}. Refer to the documentation of that method for more information.
   *
   * @param pConfig Configuration with which the instance shall be created
   * @param pCFA CFA representing the program of the invariants that the instance loads
   * @param pLogger Logger
   * @param pShutdownNotifier ShutdownNotifier
   * @return generator that gets invariants from the underlying provider
   * @throws InvalidConfigurationException if the configuration is (semantically) invalid
   * @throws IOException if the program files can not be accessed (access is required to translate
   *     the location mapping)
   */
  public static InvariantWitnessGenerator getNewFromDiskInvariantGenerator(
      Configuration pConfig, CFA pCFA, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, IOException {
    InvariantWitnessProvider provider =
        InvariantWitnessProvider.getNewFromDiskWitnessProvider(
            pConfig, pCFA, pLogger, pShutdownNotifier);

    // Note that the generator actually takes ownership of the witnessProvider and is responsible
    // for closing it. Consequently, never make the witnessProvider visible to the outside (e.g. by
    // adding it as an
    // argument to other static methods).
    return new InvariantWitnessGenerator(pLogger, provider);
  }

  @Override
  public InvariantSupplier getSupplier() throws CPAException, InterruptedException {
    try {
      return new InvariantSupplier() {
        private final Map<CFANode, ExpressionTree<Object>> witnessesByNode =
            getCurrentWitnessesByNodes();
        private final DownwardCastingVisitor<Object, AExpression> caster =
            new DownwardCastingVisitor<>(AExpression.class);

        @Override
        public BooleanFormula getInvariantFor(
            CFANode pNode,
            Optional<CallstackStateEqualsWrapper> pCallstackInformation,
            FormulaManagerView pFmgr,
            PathFormulaManager pPfmgr,
            @Nullable PathFormula pContext)
            throws InterruptedException {
          ExpressionTree<Object> invariant =
              witnessesByNode.getOrDefault(pNode, ExpressionTrees.getTrue());

          ToFormulaVisitor visitor = new ToFormulaVisitor(pFmgr, pPfmgr, pContext);

          try {
            return invariant.accept(caster).accept(visitor);
          } catch (ToFormulaException e) {
            logger.logDebugException(e);
          } catch (IncompatibleLeafTypesException e) {
            // This is an unexpected programming error.
            // We should never see an ExpressionTree that is not ExprTree<AExpression>
            throw new AssertionError(e);
          }

          return pFmgr.getBooleanFormulaManager().makeTrue();
        }
      };
    } catch (IOException e) {
      throw new CPAException("Could not load invariants from disk", e);
    }
  }

  @Override
  public ExpressionTreeSupplier getExpressionTreeSupplier()
      throws CPAException, InterruptedException {

    try {
      return new ExpressionTreeSupplier() {
        private final Map<CFANode, ExpressionTree<Object>> witnessesByNode =
            getCurrentWitnessesByNodes();

        @Override
        public ExpressionTree<Object> getInvariantFor(CFANode pNode) {
          return ExpressionTrees.cast(
              witnessesByNode.getOrDefault(pNode, ExpressionTrees.getTrue()));
        }
      };
    } catch (IOException e) {
      throw new CPAException("Could not load invariants from disk", e);
    }
  }

  @Override
  public void close() throws IOException {
    provider.close();
  }

  private Map<CFANode, ExpressionTree<Object>> getCurrentWitnessesByNodes()
      throws InterruptedException, IOException {
    ExpressionTreeFactory<Object> factory = ExpressionTrees.newFactory();
    Collection<InvariantWitness> witnesses = provider.getCurrentWitnesses();

    return witnesses.stream()
        .collect(
            // This well-named collector produces a disjunction of all invariant witness formulas
            // that hold at the same node.
            ImmutableMap.toImmutableMap(
                InvariantWitness::getNode, InvariantWitness::getFormula, factory::or));
  }

  @Override
  protected void startImpl(CFANode pInitialLocation) {
    // Nothing to do
  }

  @Override
  public void cancel() {
    // Nothing to do
  }

  @Override
  public boolean isProgramSafe() {
    return false;
  }
}

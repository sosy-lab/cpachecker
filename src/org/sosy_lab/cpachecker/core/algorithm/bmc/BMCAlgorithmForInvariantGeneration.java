// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import com.google.common.base.Verify;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.CandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeCandidateInvariant;
import org.sosy_lab.cpachecker.core.algorithm.invariants.ExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.InvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.callstack.CallstackStateEqualsWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.expressions.And;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTree;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;

public class BMCAlgorithmForInvariantGeneration extends AbstractBMCAlgorithm {

  private final CandidateGenerator candidateGenerator;

  private InvariantSupplier locationInvariantsProvider =
      InvariantSupplier.TrivialInvariantSupplier.INSTANCE;

  private ExpressionTreeSupplier locationInvariantExpressionTreeProvider =
      ExpressionTreeSupplier.TrivialInvariantSupplier.INSTANCE;

  public BMCAlgorithmForInvariantGeneration(
      Algorithm pAlgorithm,
      ConfigurableProgramAnalysis pCPA,
      Configuration pConfig,
      LogManager pLogger,
      ReachedSetFactory pReachedSetFactory,
      ShutdownManager pShutdownManager,
      CFA pCFA,
      final Specification specification,
      BMCStatistics pBMCStatistics,
      CandidateGenerator pCandidateGenerator,
      AggregatedReachedSets pAggregatedReachedSets)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    super(
        pAlgorithm,
        pCPA,
        pConfig,
        pLogger,
        pReachedSetFactory,
        pShutdownManager,
        pCFA,
        specification,
        pBMCStatistics,
        true /* invariant generator */,
        pAggregatedReachedSets);
    Verify.verify(checkIfInductionIsPossible(pCFA, pLogger));
    candidateGenerator = Objects.requireNonNull(pCandidateGenerator);
  }

  public InvariantSupplier getCurrentInvariants() {
    return locationInvariantsProvider;
  }

  public ExpressionTreeSupplier getCurrentInvariantsAsExpressionTree() {
    return locationInvariantExpressionTreeProvider;
  }

  public boolean isProgramSafe() {
    return invariantGenerator.isProgramSafe();
  }

  @Override
  protected CandidateGenerator getCandidateInvariants() {
    return candidateGenerator;
  }

  @Override
  protected KInductionProver createInductionProver() {
    final KInductionProver prover = super.createInductionProver();

    if (prover != null) {
      locationInvariantsProvider =
          new InvariantSupplier() {

            @Override
            public BooleanFormula getInvariantFor(
                CFANode pLocation,
                Optional<CallstackStateEqualsWrapper> pCallstackInformation,
                FormulaManagerView pFMGR,
                PathFormulaManager pPFMGR,
                PathFormula pContext) {
              try {
                BooleanFormulaManager booleanFormulaManager = pFMGR.getBooleanFormulaManager();
                BooleanFormula invariant = booleanFormulaManager.makeTrue();

                for (CandidateInvariant candidateInvariant : getConfirmedCandidates(pLocation)) {
                  invariant =
                      booleanFormulaManager.and(
                          invariant, candidateInvariant.getFormula(pFMGR, pPFMGR, pContext));
                }
                return booleanFormulaManager.and(
                    invariant,
                    prover.getCurrentLocationInvariants(pLocation, pFMGR, pPFMGR, pContext));
              } catch (InterruptedException | CPAException e) {
                return pFMGR.getBooleanFormulaManager().makeTrue();
              }
            }
          };
      locationInvariantExpressionTreeProvider =
          new ExpressionTreeSupplier() {

            @Override
            public ExpressionTree<Object> getInvariantFor(CFANode pLocation) {
              try {

                ExpressionTree<Object> invariant = ExpressionTrees.getTrue();

                for (ExpressionTreeCandidateInvariant expressionTreeCandidateInvariant :
                    getConfirmedCandidates(pLocation)
                        .filter(ExpressionTreeCandidateInvariant.class)) {
                  invariant =
                      And.of(invariant, expressionTreeCandidateInvariant.asExpressionTree());
                  if (ExpressionTrees.getFalse().equals(invariant)) {
                    break;
                  }
                }

                return And.of(invariant, prover.getCurrentLocationInvariants(pLocation));
              } catch (InterruptedException e) {
                return ExpressionTrees.getTrue();
              }
            }
          };
    }

    return prover;
  }
}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import de.uni_freiburg.informatik.ultimate.util.datastructures.HashDeque;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Set;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.IMCAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants.ExpressionTreeLocationInvariant;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor;
import org.sosy_lab.cpachecker.util.WitnessInvariantsExtractor.InvalidWitnessException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

@Options(prefix = "witness.validation.transitionInvariants")
public class TerminationWitnessValidator implements Algorithm {

  @Option(
      secure = true,
      required = true,
      name = "terminatingStatements",
      description =
          "Path to automaton specification describing which statements let the program terminate.")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path terminatingStatementsAutomaton =
      Classes.getCodeLocation(NonTerminationWitnessValidator.class)
          .resolveSibling("config/specification/TerminatingStatements.spc");

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSimpleTargetInformation("termination");

  private final Path witnessPath;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Configuration config;
  private final PathFormulaManager pfmgr;
  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;

  public TerminationWitnessValidator(
      final CFA pCfa,
      final ConfigurableProgramAnalysis pCPA,
      final Configuration pConfig,
      final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier,
      final ImmutableSet<Path> pWitnessPath)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    @SuppressWarnings("resource")
    PredicateCPA predCpa = CPAs.retrieveCPAOrFail(pCPA, PredicateCPA.class, IMCAlgorithm.class);
    solver = predCpa.getSolver();
    pfmgr = predCpa.getPathFormulaManager();
    fmgr = predCpa.getSolver().getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();

    if (pWitnessPath.size() < 1) {
      throw new InvalidConfigurationException("Witness file is missing in specification.");
    }
    if (pWitnessPath.size() != 1) {
      throw new InvalidConfigurationException(
          "Expect that only violation witness is part of the specification.");
    }

    witnessPath = pWitnessPath.stream().findAny().orElseThrow();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Set<ExpressionTreeLocationInvariant> invariants;
    ImmutableCollection<LoopStructure.Loop> loops =
        cfa.getLoopStructure().orElseThrow().getAllLoops();
    try {
      WitnessInvariantsExtractor invariantsExtractor =
          new WitnessInvariantsExtractor(config, logger, cfa, shutdownNotifier, witnessPath);
      invariants = invariantsExtractor.extractInvariantsFromReachedSet();
    } catch (InvalidConfigurationException e) {
      throw new CPAException(
          "Invalid Configuration while analyzing witness:\n" + e.getMessage(), e);
    } catch (InvalidWitnessException e) {
      throw new CPAException("Invalid witness:\n" + e.getMessage(), e);
    }

    ImmutableMap<LoopStructure.Loop, Formula> loopsToTransitionInvariants =
        mapTransitionInvariantsToLoops(loops, invariants);

    // Check that every transition invariant is disjunctively well-founded
    for (LoopStructure.Loop loop : loops) {
      Deque<Formula> waitlist = new HashDeque<>();
      waitlist.add(loopsToTransitionInvariants.get(loop));

      while (!waitlist.isEmpty()) {
        Formula invariant = waitlist.remove();
        // Check the well-foundedness of the subformula
        if (!isTheFormulaWellFounded(loop, loopsToTransitionInvariants.get(loop))
            && !canBeDecomposedByDNFRules(invariant, waitlist)) {
          // The invariant is not disjunctively well-founded
          pReachedSet.add(new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
          return AlgorithmStatus.SOUND_AND_PRECISE;
        }
      }
    }

    // Check that every invariant is actually a transition invariant, i.e. R^+ => T
    // TODO: Consider, implementing these checks in two different configurations and run them in
    // parallel.
    for (LoopStructure.Loop loop : loops) {
      if (!isTheInvariantTransitionInvariant(loop, loopsToTransitionInvariants.get(loop))) {
        pReachedSet.add(new ARGState(DUMMY_TARGET_STATE, null), SingletonPrecision.getInstance());
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
    }
    pReachedSet.clear();
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  private ImmutableMap<LoopStructure.Loop, Formula> mapTransitionInvariantsToLoops(
      ImmutableCollection<LoopStructure.Loop> pLoops,
      Set<ExpressionTreeLocationInvariant> pInvariants)
      throws InterruptedException {
    ImmutableMap.Builder<LoopStructure.Loop, Formula> builder = new Builder<>();

    for (LoopStructure.Loop loop : pLoops) {
      BooleanFormula invariantForTheLoop = bfmgr.makeFalse();
      for (ExpressionTreeLocationInvariant invariant : pInvariants) {
        if (!invariant.isTransitionInvariant()) {
          // The check for supporting invariants is not yet supported
          // TODO: Implement checking that the invariants are really invariants
          continue;
        }

        if (isTheInvariantLocationInLoop(loop, invariant.getLocation())) {
          BooleanFormula invariantFormula;
          try {
            invariantFormula = invariant.getFormula(fmgr, pfmgr, pfmgr.makeEmptyPathFormula());
          } catch (CPATransferException e) {
            invariantFormula = bfmgr.makeFalse();
          }
          invariantForTheLoop = bfmgr.or(invariantForTheLoop, invariantFormula);
        }
      }
      builder.put(loop, invariantForTheLoop);
    }
    return builder.build();
  }

  private boolean isTheInvariantLocationInLoop(
      LoopStructure.Loop pLoop, CFANode pInvariantLocation) {
    for (CFANode loopNode : pLoop.getLoopNodes()) {
      if (loopNode.equals(pInvariantLocation)) {
        return true;
      }
    }
    return false;
  }

  // TODO: Write this method
  private boolean isTheFormulaWellFounded(LoopStructure.Loop pLoop, Formula pFormula) {
    return true;
  }

  // TODO: Write the method that decomposes the formula with rewrite rules and puts the formulas in
  // the waitlist.
  private boolean canBeDecomposedByDNFRules(Formula pFormula, Deque<Formula> pWaitlist) {
    return false;
  }

  // TODO: Write this method
  private boolean isTheInvariantTransitionInvariant(LoopStructure.Loop pLoop, Formula pInvariant) {
    return true;
  }
}

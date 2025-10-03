// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.ImmutableList;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.cwriter.FormulaToCExpressionConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

public class ImplicitRankingChecker implements WellFoundednessChecker {

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CFACreator cfaCreator;
  private final CFA cfa;
  private final Configuration config;
  private final Scope scope;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final Specification specification;

  public ImplicitRankingChecker(
      final FormulaManagerView pFmgr,
      final BooleanFormulaManagerView pBfmgr,
      final LogManager pLogger,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final Specification pSpecification,
      final Scope pScope,
      final CFA pCFA)
      throws InvalidConfigurationException {
    fmgr = pFmgr;
    bfmgr = pBfmgr;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    scope = pScope;
    cfaCreator = new CFACreator(config, pLogger, pShutdownNotifier);
    specification = pSpecification;
    cfa = pCFA;
  }

  /**
   * This method checks whether one concrete subformula from transition invariant is well-founded.
   * It tries to compute a ranking function for the transition invariant.
   *
   * @param pFormula representing the transition invariant
   * @param pSupportingInvariants that can strengthen the transition invariant
   * @return true if the formula really is well-founded, false otherwise
   */
  @Override
  public boolean isWellFounded(
      BooleanFormula pFormula, ImmutableList<BooleanFormula> pSupportingInvariants, Loop pLoop)
      throws InterruptedException, CPAException {
    Map<String, Formula> mapNamesToVariables = fmgr.extractVariables(pFormula);
    StringBuilder builder = new StringBuilder();
    builder.append("int main() { \n");

    // Initialize the variables from the transition invariant
    Set<String> alreadyDeclaredVars = new HashSet<>();
    String varDeclaration;
    for (String variable : cfa.getVarClassification().orElseThrow().getRelevantVariables()) {
      varDeclaration =
          TransitionInvariantUtils.removeFunctionFromVarsName(
              scope.lookupVariable(variable).toString());
      alreadyDeclaredVars.add(varDeclaration);
      if (!varDeclaration.contains(";")) {
        varDeclaration = varDeclaration + ";";
      }
      builder.append(varDeclaration + "\n");
    }
    for (String variable : mapNamesToVariables.keySet()) {
      varDeclaration =
          TransitionInvariantUtils.removeFunctionFromVarsName(
              scope.lookupVariable(variable).toString());
      if (!alreadyDeclaredVars.contains(varDeclaration)) {
        builder.append(varDeclaration + "\n");
      }
    }

    // Build the loop
    FormulaToCExpressionConverter converter = new FormulaToCExpressionConverter(fmgr);
    String loopCondition = converter.formulaToCExpression(pFormula);
    List<String> exitConditions =
        pLoop.getOutgoingEdges().stream()
            .filter(x -> x instanceof CAssumeEdge)
            .map(x -> ((CAssumeEdge) x).getExpression().toParenthesizedASTString())
            .toList();
    loopCondition = loopCondition + " && " + String.join(" && ", exitConditions);
    for (BooleanFormula invariant : pSupportingInvariants) {
      loopCondition = loopCondition + " && " + converter.formulaToCExpression(invariant);
      for (String variable : fmgr.extractVariables(invariant).keySet()) {
        varDeclaration =
            TransitionInvariantUtils.removeFunctionFromVarsName(
                scope.lookupVariable(variable).toString());
        if (!alreadyDeclaredVars.contains(varDeclaration)) {
          builder.append(varDeclaration + "\n");
        }
      }
    }
    loopCondition = loopCondition.replace("true", "1");
    loopCondition = loopCondition.replace("false", "0");
    builder.append("while(" + loopCondition + ") {\n");
    // Initialize the variables from the transition invariant
    for (String variable : mapNamesToVariables.keySet()) {
      if (variable.contains("__PREV")) {
        builder.append(
            TransitionInvariantUtils.removeFunctionFromVarsName(variable)
                + " = "
                + TransitionInvariantUtils.removeFunctionFromVarsName(
                    variable.replace("__PREV", ""))
                + ";\n");
      }
    }
    // Reset the original variables
    for (String variable : mapNamesToVariables.keySet()) {
      if (!variable.contains("__PREV")) {
        builder.append(
            TransitionInvariantUtils.removeFunctionFromVarsName(variable)
                + " = "
                + "__VERIFIER_nondet_"
                + scope.lookupVariable(variable).getType()
                + "();\n");
      }
    }
    builder.append("}}");
    String overapproximatingProgam = builder.toString();

    try {
      // Initialization:
      // CFA
      CFA overapproximatingCFA = cfaCreator.parseSourceAndCreateCFA(overapproximatingProgam);
      CFANode mainEntryNode = overapproximatingCFA.getMainFunction();
      // CPA
      Path lassoRankerConfigPath =
          Classes.getCodeLocation(ImplicitRankingChecker.class)
              .resolveSibling("config/lassoRankerAnalysis.properties");
      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              Configuration.builder().loadFromFile(lassoRankerConfigPath).build(),
              logger,
              shutdownNotifier,
              AggregatedReachedSets.empty());
      ConfigurableProgramAnalysis terminationCpa =
          coreComponents.createCPA(overapproximatingCFA, specification);
      // Reached Set
      ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logger);
      ReachedSet reachedSet = reachedSetFactory.create(terminationCpa);
      AbstractState initialState =
          terminationCpa.getInitialState(mainEntryNode, StateSpacePartition.getDefaultPartition());
      Precision initialPrecision =
          terminationCpa.getInitialPrecision(
              mainEntryNode, StateSpacePartition.getDefaultPartition());
      reachedSet.add(initialState, initialPrecision);

      // Running the algorithm
      Algorithm terminationAlgorithm =
          coreComponents.createAlgorithm(terminationCpa, overapproximatingCFA, specification);
      terminationAlgorithm.run(reachedSet);

      if (reachedSet.wasTargetReached()) {
        return false;
      }
    } catch (Exception e) {
      throw new CPAException(e.toString());
    }
    return true;
  }

  /**
   * Checks whether the formula can be divided into disjunction of formulas expressing relations
   * that are well-founded. We do it by transformation into DNF and then checking each respective
   * subformula.
   *
   * @param pFormula that is to be checked for disjunctive well-foundedness.
   * @param pSupportingInvariants that can strengthen the transition invariant
   * @return true if the formula is disjunctively well-founded, false otherwise.
   */
  @Override
  public boolean isDisjunctivelyWellFounded(
      BooleanFormula pFormula, ImmutableList<BooleanFormula> pSupportingInvariants, Loop pLoop)
      throws InterruptedException, CPAException {
    Set<BooleanFormula> invariantInDNF = bfmgr.toDisjunctionArgs(pFormula, true);

    for (BooleanFormula candidateInvariant : invariantInDNF) {
      if (!isWellFounded(candidateInvariant, pSupportingInvariants, pLoop)) {
        return false;
      }
    }
    return true;
  }
}

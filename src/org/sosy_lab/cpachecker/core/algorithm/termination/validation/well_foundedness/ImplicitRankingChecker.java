// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property.CommonVerificationProperty;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.VerifierNondetFunctions;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

@Options(prefix = "termination.validation")
public class ImplicitRankingChecker implements WellFoundednessChecker {

  @Option(
      secure = true,
      required = true,
      description =
          "configuration of the termination analysis that proves the well-foundedness of the"
              + " transition invariants by proving the termination of an overapproximating"
              + " program")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private @Nullable Path wellFoundednessConfig = null;

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CFA cfa;
  private final Configuration config;
  private final Scope scope;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public ImplicitRankingChecker(
      final FormulaManagerView pFmgr,
      final BooleanFormulaManagerView pBfmgr,
      final LogManager pLogger,
      final Configuration pConfig,
      final ShutdownNotifier pShutdownNotifier,
      final Scope pScope,
      final CFA pCFA)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    fmgr = pFmgr;
    bfmgr = pBfmgr;
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    scope = pScope;
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
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      Loop pLoop,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars)
      throws InterruptedException, CPAException {
    Map<String, Formula> mapNamesToVariables = fmgr.extractVariables(pFormula);
    StringJoiner builder = new StringJoiner(System.lineSeparator());
    // The declarations of the functions returning nondeterministic values that are used in the
    // generated program, see nondetCall
    Map<String, String> nondetDeclarations = new LinkedHashMap<>();
    builder.add("int main() {");
    CFANode loopHead = pLoop.getLoopHeads().asList().getFirst();

    // This could easily happen (and it is completely fine and expected) since there could be some
    // variables shared across for example supporting invariants and the loop condition or
    // transition invariant and the supporting invariants.
    // That is why they are collected to not be initialized it twice.
    Set<String> alreadyDeclaredVars = new HashSet<>();

    // Initialize the variables from the transition invariant
    initializeVariables(
        loopHead, alreadyDeclaredVars, builder, mapNamesToVariables, nondetDeclarations);

    // Build the loop
    buildTheLoop(loopHead, pFormula, builder, alreadyDeclaredVars, pSupportingInvariants);

    // Initialize the variables from the transition invariant
    resetVariablesFromTransitionInvariant(builder, mapCurrVarsToPrevVars, mapNamesToVariables);

    // Reset the original variables
    resetVariablesFromProgram(
        builder, mapCurrVarsToPrevVars, mapNamesToVariables, nondetDeclarations);

    builder.add("}}");
    String overapproximatingProgam = declareNondetfunctions(builder.toString(), nondetDeclarations);

    try {
      // Initialization:
      // CFA

      // CFA creation with preprocessing currently do not support building from string
      // Therefore, we have to remove the --preprocess from the config
      Configuration cfaParsingConfig =
          Configuration.builder().copyFrom(config).clearOption("parser.usePreprocessor").build();
      CFACreator cfaCreator = new CFACreator(cfaParsingConfig, logger, shutdownNotifier);

      CFA overapproximatingCFA = cfaCreator.parseSourceAndCreateCFA(overapproximatingProgam);
      CFANode mainEntryNode = overapproximatingCFA.getMainFunction();

      // CPA
      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              Configuration.builder().loadFromFile(wellFoundednessConfig).build(),
              logger,
              shutdownNotifier,
              AggregatedReachedSets.empty(),
              overapproximatingCFA);
      // Only the termination of the generated program is checked, the witness does not belong
      // to the generated program
      Specification terminationSpecification =
          Specification.alwaysSatisfied()
              .withAdditionalProperties(ImmutableSet.of(CommonVerificationProperty.TERMINATION));
      ConfigurableProgramAnalysis terminationCpa =
          coreComponents.createCPA(terminationSpecification);
      // Reached Set
      ReachedSet reachedSet =
          coreComponents.createInitializedReachedSet(terminationCpa, mainEntryNode);

      // Running the algorithm
      Algorithm terminationAlgorithm =
          coreComponents.createAlgorithm(terminationCpa, terminationSpecification);
      AlgorithmStatus status = terminationAlgorithm.run(reachedSet);
      shutdownNotifier.shutdownIfNecessary();

      // The formula is only well-founded if the termination of the program is proven, i.e., if
      // there is no non-terminating loop and the analysis is sound
      if (reachedSet.wasTargetReached() || !status.isSound()) {
        return false;
      }
    } catch (InvalidConfigurationException | IOException | ParserException e) {
      throw new CPAException(
          "The termination algorithm failed to verify the overapproximating program reducing"
              + " well-foundedness!");
    }
    return true;
  }

  /**
   * Returns a call of a function that returns a nondeterministic value of the given type, and
   * records the declaration of the function, see {@link #declareNondetfunctions}.
   */
  private static String nondetCall(CSimpleType pType, Map<String, String> pNondetDeclarations) {
    String functionName = VerifierNondetFunctions.getFunctionName(pType);
    pNondetDeclarations.putIfAbsent(
        functionName, VerifierNondetFunctions.getFunctionDeclaration(pType));
    return functionName + "()";
  }

  /**
   * Finishes the generated program: the functions returning nondeterministic values are declared,
   * since otherwise their return types would be unknown, and the variables for the previous values
   * are renamed, since the termination analyses use the keywords of these names internally.
   */
  private static String declareNondetfunctions(
      String pProgram, Map<String, String> pNondetDeclarations) {
    StringJoiner program = new StringJoiner(System.lineSeparator());
    pNondetDeclarations.values().forEach(program::add);
    program.add(pProgram.replace(TransitionInvariantUtils.PREV_KEYWORD, "__validation_previous"));
    return program.toString();
  }

  private void resetVariablesFromProgram(
      StringJoiner builder,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars,
      Map<String, Formula> mapNamesToVariables,
      Map<String, String> pNondetDeclarations)
      throws CPAException {
    for (String variable : mapNamesToVariables.keySet()) {
      if (!TransitionInvariantUtils.isPrevVariable(variable, mapCurrVarsToPrevVars)) {
        String nondetVerifierCall;
        // The formula has the qualified name of the variable, e.g., main::i__1 if CPAchecker
        // renamed it, but the scope knows it only as main::i or i__1. The name without the
        // function is unique, since the scope has only the variables in scope at the loop.
        CSimpleDeclaration declaration =
            scope.lookupVariable(TransitionInvariantUtils.removeFunctionFromVarsName(variable));
        if (declaration.getType().getCanonicalType() instanceof CSimpleType simpleType) {
          nondetVerifierCall = nondetCall(simpleType, pNondetDeclarations) + ";";
        } else {
          throw new CPAException(
              "We currently do not support nondeterministic initialization of complex types.");
        }
        builder.add(
            TransitionInvariantUtils.removeFunctionFromVarsName(variable)
                + " = "
                + nondetVerifierCall);
      }
    }
  }

  private void resetVariablesFromTransitionInvariant(
      StringJoiner builder,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars,
      Map<String, Formula> mapNamesToVariables) {
    for (String variable : mapNamesToVariables.keySet()) {
      if (TransitionInvariantUtils.isPrevVariable(variable, mapCurrVarsToPrevVars)) {
        CSimpleDeclaration prevDeclaration =
            TransitionInvariantUtils.getPrevDeclaration(variable, mapCurrVarsToPrevVars);
        CExpressionAssignmentStatement assignment =
            new CExpressionAssignmentStatement(
                FileLocation.DUMMY,
                new CIdExpression(FileLocation.DUMMY, prevDeclaration),
                new CIdExpression(FileLocation.DUMMY, mapCurrVarsToPrevVars.get(prevDeclaration)));
        builder.add(assignment.toASTString());
      }
    }
  }

  private void buildTheLoop(
      CFANode loopHead,
      BooleanFormula pFormula,
      StringJoiner builder,
      Set<String> alreadyDeclaredVars,
      ImmutableList<BooleanFormula> pSupportingInvariants)
      throws CPAException {
    String varDeclaration;
    String loopCondition =
        TransitionInvariantUtils.transformFormulaToStringWithReplacement(
            pFormula, bfmgr, fmgr, scope);
    // If the condition of the loop cannot be computed, the condition true overapproximates it
    String exitCondition =
        CFAUtils.computeLoopCondition(loopHead, cfa.getAstCfaRelation()).orElse("1");
    loopCondition = loopCondition + " && " + exitCondition;
    for (BooleanFormula invariant : pSupportingInvariants) {
      loopCondition =
          loopCondition
              + " && "
              + TransitionInvariantUtils.transformFormulaToStringWithReplacement(
                  invariant, bfmgr, fmgr, scope);
      for (String variable : fmgr.extractVariables(invariant).keySet()) {
        String variableName = TransitionInvariantUtils.removeFunctionFromVarsName(variable);
        varDeclaration =
            TransitionInvariantUtils.removeFunctionFromVarsName(
                scope.lookupVariable(variableName).toString());
        if (alreadyDeclaredVars.add(variableName)) {
          builder.add(varDeclaration);
        }
      }
    }
    builder.add("while(" + loopCondition + ") {");
  }

  private void initializeVariables(
      CFANode loopHead,
      Set<String> alreadyDeclaredVars,
      StringJoiner builder,
      Map<String, Formula> mapNamesToVariables,
      Map<String, String> pNondetDeclarations) {
    String varDeclaration;
    FluentIterable<AbstractSimpleDeclaration> variablesInScope =
        cfa.getAstCfaRelation().getVariablesAndParametersInScope(loopHead).orElseThrow();
    for (AbstractSimpleDeclaration variable : variablesInScope) {
      CType type = (CType) variable.getType();
      // Only variables of simple types are declared, since the generated program does not contain
      // the declarations of the types, e.g., of the structs that pointers point to
      if (!(type.getCanonicalType() instanceof CSimpleType simpleType)
          || isGlobalVariableOverwrittenByLocal(variable, variablesInScope)) {
        continue;
      }
      if (alreadyDeclaredVars.add(variable.getName())) {
        // The variables have arbitrary initial values, the termination analyses may treat
        // uninitialized variables imprecisely
        builder.add(
            type.toASTString(variable.getName())
                + " = "
                + nondetCall(simpleType, pNondetDeclarations)
                + ";");
      }
    }
    for (String variable : mapNamesToVariables.keySet()) {
      String variableName = TransitionInvariantUtils.removeFunctionFromVarsName(variable);
      CSimpleDeclaration declaration = scope.lookupVariable(variableName);
      if (declaration.getType().getCanonicalType() instanceof CSimpleType simpleType) {
        varDeclaration =
            declaration.getType().toASTString(variableName)
                + " = "
                + nondetCall(simpleType, pNondetDeclarations)
                + ";";
      } else {
        varDeclaration =
            TransitionInvariantUtils.removeFunctionFromVarsName(declaration.toString());
      }
      if (alreadyDeclaredVars.add(variableName)) {
        builder.add(varDeclaration);
      }
    }
  }

  private boolean isGlobalVariableOverwrittenByLocal(
      AbstractSimpleDeclaration variable,
      FluentIterable<AbstractSimpleDeclaration> variablesInScope) {
    if (cfa.getMetadata().getAstCfaRelation().getGlobalVariables().isEmpty()) {
      return false;
    }
    ImmutableSet<AVariableDeclaration> globalVars =
        cfa.getMetadata().getAstCfaRelation().getGlobalVariables().orElseThrow();

    return FluentIterable.from(globalVars).anyMatch(globalVar -> globalVar.equals(variable))
        && variablesInScope.anyMatch(
            localVar ->
                !localVar.equals(variable) && localVar.getName().equals(variable.getName()));
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
      BooleanFormula pFormula,
      ImmutableList<BooleanFormula> pSupportingInvariants,
      Loop pLoop,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars)
      throws InterruptedException, CPAException {
    Set<BooleanFormula> invariantInDNF = bfmgr.toDisjunctionArgs(pFormula, true);

    for (BooleanFormula candidateInvariant : invariantInDNF) {
      if (!isWellFounded(candidateInvariant, pSupportingInvariants, pLoop, mapCurrVarsToPrevVars)) {
        return false;
      }
    }
    return true;
  }
}

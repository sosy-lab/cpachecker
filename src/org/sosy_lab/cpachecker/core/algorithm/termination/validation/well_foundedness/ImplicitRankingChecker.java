// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
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
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.AstUtils.BoundaryNodesComputationFailed;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;

@Options(prefix = "termination.validation")
public class ImplicitRankingChecker implements WellFoundednessChecker {

  @Option(
      secure = true,
      description =
          "configuration of the termination analysis that proves the well-foundedness of the"
              + " transition invariants by proving the termination of an overapproximating program")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private Path wellFoundednessConfig =
      Classes.getCodeLocation(ImplicitRankingChecker.class)
          .resolveSibling("config/terminationToSafety.properties");

  @Option(
      secure = true,
      description =
          "wall-time limit for proving the well-foundedness of one transition invariant (0 means"
              + " no limit)")
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
  private TimeSpan wellFoundednessCheckTimeLimit = TimeSpan.ofSeconds(60);

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final CFA cfa;
  private final Configuration config;
  private final Scope scope;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  // The declarations of the functions returning nondeterministic values that are used in the
  // generated program, see nondetCall
  private final Map<String, String> nondetDeclarations = new LinkedHashMap<>();

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
    nondetDeclarations.clear();
    builder.add("int main() {");
    CFANode loopHead = pLoop.getLoopHeads().asList().getFirst();

    // This could easily happen (and it is completely fine and expected) since there could be some
    // variables shared across for example supporting invariants and the loop condition or
    // transition invariant and the supporting invariants.
    // That is why they are collected to not be initialized it twice.
    Set<String> alreadyDeclaredVars = new HashSet<>();

    // Initialize the variables from the transition invariant
    initializeVariables(loopHead, alreadyDeclaredVars, builder, mapNamesToVariables);

    // Build the loop
    buildTheLoop(loopHead, pFormula, builder, alreadyDeclaredVars, pSupportingInvariants);

    // Initialize the variables from the transition invariant
    resetVariablesFromTransitionInvariant(builder, mapCurrVarsToPrevVars, mapNamesToVariables);

    // Reset the original variables
    resetVariablesFromProgram(builder, mapCurrVarsToPrevVars, mapNamesToVariables);

    builder.add("}}");
    String overapproximatingProgam = finishProgram(builder.toString());

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

      // The termination analysis may not terminate, so it gets its own time limit
      ShutdownManager checkShutdownManager = ShutdownManager.createWithParent(shutdownNotifier);
      ResourceLimitChecker limitChecker =
          ResourceLimitChecker.createWallTimeLimitChecker(
              checkShutdownManager, wellFoundednessCheckTimeLimit);
      limitChecker.start();
      try {
        // CPA
        CoreComponentsFactory coreComponents =
            new CoreComponentsFactory(
                Configuration.builder().loadFromFile(wellFoundednessConfig).build(),
                logger,
                checkShutdownManager.getNotifier(),
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
        checkShutdownManager.getNotifier().shutdownIfNecessary();

        // The formula is only well-founded if the termination of the program is proven, i.e., if
        // there is no non-terminating loop and the analysis is sound
        if (reachedSet.wasTargetReached() || !status.isSound()) {
          return false;
        }
      } catch (InterruptedException e) {
        // Only the time limit of the check stops the check without stopping the validation
        shutdownNotifier.shutdownIfNecessary();
        logger.log(Level.FINE, "Proving the well-foundedness reached the time limit.");
        return false;
      } finally {
        limitChecker.cancel();
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
   * records the declaration of the function, see {@link #finishProgram}.
   */
  private String nondetCall(CType pType) {
    String typeName = pType.getCanonicalType().toASTString("").trim();
    String functionName = "__VERIFIER_nondet_" + typeName.replaceAll("\\W+", "_");
    nondetDeclarations.putIfAbsent(
        functionName, "extern " + typeName + " " + functionName + "(void);");
    return functionName + "()";
  }

  /**
   * Finishes the generated program: the functions returning nondeterministic values are declared,
   * since otherwise their return types would be unknown, and the variables for the previous values
   * are renamed, since the termination analyses use the keywords of these names internally.
   */
  private String finishProgram(String pProgram) {
    StringJoiner program = new StringJoiner(System.lineSeparator());
    nondetDeclarations.values().forEach(program::add);
    program.add(pProgram.replace(TransitionInvariantUtils.PREV_KEYWORD, "__validation_previous"));
    return program.toString();
  }

  /**
   * Computes the condition under which the body of the loop with the given loop head is entered, as
   * a C expression. It is the disjunction over all paths through the controlling expression of the
   * loop from the loop head to the body of the conjunction of the assumptions on the path. If the
   * condition cannot be expressed, e.g., for loops like while(1) or conditions with side effects,
   * the condition true ("1") is returned, which overapproximates the loop.
   */
  private String computeLoopCondition(CFANode pLoopHead) {
    Optional<IterationElement> iteration =
        cfa.getAstCfaRelation().getTightestIterationStructureForNode(pLoopHead);
    if (iteration.isEmpty() || iteration.orElseThrow().getControllingExpression().isEmpty()) {
      return "1";
    }
    ImmutableSet<CFAEdge> conditionEdges =
        iteration.orElseThrow().getControllingExpression().orElseThrow().edges();
    ImmutableSet<CFANode> bodyEntries;
    try {
      bodyEntries = iteration.orElseThrow().getNodesBetweenConditionAndBody();
    } catch (BoundaryNodesComputationFailed e) {
      logger.logDebugException(e, "Could not compute the condition of the loop");
      return "1";
    }
    ImmutableSet<CFANode> successorsInCondition =
        FluentIterable.from(conditionEdges).transform(CFAEdge::getSuccessor).toSet();
    ImmutableSet<CFANode> conditionEntries =
        FluentIterable.from(conditionEdges)
            .transform(CFAEdge::getPredecessor)
            .filter(node -> !successorsInCondition.contains(node))
            .toSet();

    List<String> pathConditions = new ArrayList<>();
    for (CFANode entry : conditionEntries) {
      if (!collectPathConditions(
          entry, conditionEdges, bodyEntries, ImmutableList.of(), pathConditions)) {
        return "1";
      }
    }
    if (pathConditions.isEmpty()) {
      return "1";
    }
    return FluentIterable.from(pathConditions)
        .transform(condition -> "(" + condition + ")")
        .join(Joiner.on(" || "));
  }

  /**
   * Collects the conjunctions of the assumptions on all paths from the given node through the
   * condition edges to a node of the body.
   *
   * @return false if the condition of a path cannot be expressed as a C expression
   */
  private static boolean collectPathConditions(
      CFANode pNode,
      ImmutableSet<CFAEdge> pConditionEdges,
      ImmutableSet<CFANode> pBodyEntries,
      ImmutableList<String> pPath,
      List<String> pPathConditions) {
    if (pBodyEntries.contains(pNode)) {
      pPathConditions.add(pPath.isEmpty() ? "1" : Joiner.on(" && ").join(pPath));
      return true;
    }
    if (pPath.size() > pConditionEdges.size()) {
      // The condition edges contain a cycle
      return false;
    }
    for (CFAEdge edge : pNode.getLeavingEdges()) {
      if (!pConditionEdges.contains(edge)) {
        continue;
      }
      ImmutableList<String> path = pPath;
      if (edge instanceof CAssumeEdge assumeEdge) {
        String expression = assumeEdge.getExpression().toASTString();
        if (expression.contains("__CPAchecker_TMP")) {
          // The condition has side effects, which are not part of the generated program
          return false;
        }
        path =
            ImmutableList.<String>builder()
                .addAll(pPath)
                .add(
                    assumeEdge.getTruthAssumption()
                        ? "(" + expression + ")"
                        : "!(" + expression + ")")
                .build();
      } else if (!(edge instanceof BlankEdge)) {
        return false;
      }
      if (!collectPathConditions(
          edge.getSuccessor(), pConditionEdges, pBodyEntries, path, pPathConditions)) {
        return false;
      }
    }
    return true;
  }

  private void resetVariablesFromProgram(
      StringJoiner builder,
      ImmutableMap<CSimpleDeclaration, CSimpleDeclaration> mapCurrVarsToPrevVars,
      Map<String, Formula> mapNamesToVariables)
      throws CPAException {
    for (String variable : mapNamesToVariables.keySet()) {
      if (!TransitionInvariantUtils.isPrevVariable(variable, mapCurrVarsToPrevVars)) {
        String nondetVerifierCall;
        if (scope.lookupVariable(variable).getType() instanceof CSimpleType) {
          nondetVerifierCall = nondetCall(scope.lookupVariable(variable).getType()) + ";";
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
        TransitionInvariantUtils.transformFormulaToStringWithTrivialReplacement(
            pFormula, bfmgr, fmgr, scope);
    String exitCondition = computeLoopCondition(loopHead);
    loopCondition = loopCondition + " && " + exitCondition;
    for (BooleanFormula invariant : pSupportingInvariants) {
      loopCondition =
          loopCondition
              + " && "
              + TransitionInvariantUtils.transformFormulaToStringWithTrivialReplacement(
                  invariant, bfmgr, fmgr, scope);
      for (String variable : fmgr.extractVariables(invariant).keySet()) {
        String variableName = TransitionInvariantUtils.removeFunctionFromVarsName(variable);
        varDeclaration =
            TransitionInvariantUtils.removeFunctionFromVarsName(
                scope.lookupVariable(variable).toString());
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
      Map<String, Formula> mapNamesToVariables) {
    String varDeclaration;
    FluentIterable<AbstractSimpleDeclaration> variablesInScope =
        cfa.getAstCfaRelation().getVariablesAndParametersInScope(loopHead).orElseThrow();
    for (AbstractSimpleDeclaration variable : variablesInScope) {
      CType type = (CType) variable.getType();
      // Only variables of simple types are declared, since the generated program does not contain
      // the declarations of the types, e.g., of the structs that pointers point to
      if (!(type.getCanonicalType() instanceof CSimpleType)
          || isGlobalVariableOverwrittenByLocal(variable, variablesInScope)) {
        continue;
      }
      if (alreadyDeclaredVars.add(variable.getName())) {
        // The variables have arbitrary initial values, the termination analyses may treat
        // uninitialized variables imprecisely
        builder.add(type.toASTString(variable.getName()) + " = " + nondetCall(type) + ";");
      }
    }
    for (String variable : mapNamesToVariables.keySet()) {
      String variableName = TransitionInvariantUtils.removeFunctionFromVarsName(variable);
      CSimpleDeclaration declaration = scope.lookupVariable(variable);
      if (declaration.getType().getCanonicalType() instanceof CSimpleType) {
        varDeclaration =
            declaration.getType().toASTString(variableName)
                + " = "
                + nondetCall(declaration.getType())
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

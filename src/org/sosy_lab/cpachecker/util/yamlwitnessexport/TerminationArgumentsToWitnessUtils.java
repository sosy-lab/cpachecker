// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.ANYPREV_SUFFIX;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.AT_PREFIX;
import static org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils.AT_PREFIX_NON_C;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.AffineFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.SupportingInvariant;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.TerminationArgument;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.LexicographicRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.LinearRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.NestedRankingFunction;
import de.uni_freiburg.informatik.ultimate.lassoranker.termination.rankingfunctions.RankingFunction;
import de.uni_freiburg.informatik.ultimate.lib.modelcheckerutils.cfg.variables.IProgramVar;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.well_foundedness.TransitionInvariantUtils;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.PartitionedRelationFormula;
import org.sosy_lab.cpachecker.cpa.terminationviamemory.TerminationToReachState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;
import org.sosy_lab.cpachecker.util.ast.IterationElement;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.LocationRecord;

public class TerminationArgumentsToWitnessUtils {

  /**
   * Returns the scope of the variables in scope at the given node, in which a name of the input
   * program refers to the variable in scope, as in the invariants of a witness, and the names of
   * the variables in CPAchecker without the function are distinct, e.g., i__1.
   */
  public static CProgramScope getScopeAtNode(
      CProgramScope pProgramScope, CFANode pNode, AstCfaRelation pAstCfaRelation) {
    return pAstCfaRelation
        .getVariablesAndParametersInScope(pNode)
        .map(
            variables ->
                pProgramScope.withVariablesInScope(
                    variables.filter(CSimpleDeclaration.class).toSet()))
        .orElse(pProgramScope)
        .withFunctionScope(pNode.getFunctionName());
  }

  /**
   * Returns the qualified name with the name in the input program of the variable with the given
   * qualified name in CPAchecker, e.g., main::i for main::i__1. Other names are returned unchanged.
   */
  private static String toNameInInputProgram(String pVariable, Scope pScope) {
    CSimpleDeclaration variable =
        pScope.lookupVariable(TransitionInvariantUtils.removeFunctionFromVarsName(pVariable));
    return variable == null ? pVariable : pScope.createScopedNameOf(variable.getOrigName());
  }

  /**
   * Returns whether the given C expression of a variable of a termination argument has only
   * variables of the input program, i.e., no temporary variable of CPAchecker.
   */
  private static boolean isInInputProgram(CExpression pExpression) {
    return !CFAUtils.getIdExpressionsOfExpression(pExpression)
        .anyMatch(variable -> variable.getName().startsWith(CParserUtils.CPACHECKER_TMP_PREFIX));
  }

  /** Returns whether the given integer can be written as integer constant of C (long long). */
  private static boolean isIntegerConstantOfC(BigInteger pValue) {
    return pValue.abs().bitLength() < Long.SIZE;
  }

  /**
   * Returns the given affine function of a termination argument as C expression, e.g., {@code
   * 2*((__int128)x) - 1*((__int128)(*p)) + 3}, with the summands in the order of {@link
   * AffineFunction#toString}. Each variable is written by the given function from its C expression,
   * e.g., as its value in the previous state. It is empty if the affine function cannot be
   * expressed in C, i.e., if a variable has no C expression with only variables of the input
   * program, e.g., a heap cell at an offset of a pointer, or if a coefficient or the constant does
   * not fit into long long.
   */
  private static Optional<String> toCExpression(
      AffineFunction pFunction,
      Map<IProgramVar, CExpression> pCExpressionsOfVariables,
      Function<String, String> pWriteVariable) {
    StringBuilder expression = new StringBuilder();
    for (IProgramVar variable : pFunction.getVariables()) {
      CExpression cExpression = pCExpressionsOfVariables.get(variable);
      BigInteger coefficient = pFunction.get(variable);
      if (cExpression == null
          || !isInInputProgram(cExpression)
          || !isIntegerConstantOfC(coefficient)) {
        return Optional.empty();
      }
      if (!expression.isEmpty()) {
        expression.append(coefficient.signum() < 0 ? " - " : " + ");
      } else if (coefficient.signum() < 0) {
        expression.append('-');
      }
      expression
          .append(coefficient.abs())
          .append('*')
          .append(
              pWriteVariable.apply(
                  cExpression.toParenthesizedASTString(AAstNodeRepresentation.ORIGINAL_NAMES)));
    }
    BigInteger constant = pFunction.getConstant();
    if (!isIntegerConstantOfC(constant)) {
      return Optional.empty();
    }
    if (expression.isEmpty()) {
      expression.append(constant);
    } else if (constant.signum() != 0) {
      expression.append(constant.signum() < 0 ? " - " : " + ").append(constant.abs());
    }
    return Optional.of(expression.toString());
  }

  // The variables are cast to __int128 to prevent overflows in the witness
  private static String castToInt128(String pVariable) {
    return "((__int128)" + pVariable + ")";
  }

  // The variables of the previous state are wrapped into \at(x, AnyPrev)
  private static String castToInt128AtAnyPrev(String pVariable) {
    return castToInt128(AT_PREFIX + pVariable + ANYPREV_SUFFIX);
  }

  /**
   * Returns the name in the input program of a variable of a transition invariant of
   * TerminationToSafety, whose variables of the previous state are wrapped into \at(x, AnyPrev).
   */
  private static String toNameInTransitionInvariant(String pVariable, Scope pScope) {
    if (pVariable.startsWith(AT_PREFIX_NON_C) && pVariable.endsWith(ANYPREV_SUFFIX)) {
      String previousVariable =
          pVariable.substring(
              AT_PREFIX_NON_C.length(), pVariable.length() - ANYPREV_SUFFIX.length());
      return AT_PREFIX_NON_C + toNameInInputProgram(previousVariable, pScope) + ANYPREV_SUFFIX;
    }
    return toNameInInputProgram(pVariable, pScope);
  }

  /**
   * Converts supporting invariant into an invariant entry, or returns empty if it cannot be
   * expressed in C.
   */
  public static Optional<InvariantEntry> convertSupportingInvariantToInvariantEntry(
      SupportingInvariant pSupportingInvariant,
      CFANode pLoopHead,
      CFAEdge pIncomingLoopEdge,
      Map<IProgramVar, CExpression> pCExpressionsOfVariables) {
    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    // This is planned in https://gitlab.com/sosy-lab/software/cpachecker/-/work_items/1483
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();

    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    return toCExpression(
            pSupportingInvariant,
            pCExpressionsOfVariables,
            TerminationArgumentsToWitnessUtils::castToInt128)
        .map(function -> function + (pSupportingInvariant.strict ? " > 0" : " >= 0"))
        .map(
            invariant ->
                new InvariantEntry(
                    invariant,
                    InvariantRecordType.LOOP_INVARIANT.getKeyword(),
                    YAMLWitnessExpressionType.C,
                    locationRecord));
  }

  /**
   * Iterates through all the termination arguments in form of ranking functions and converts them
   * to invariant entry expressed with transition invariant.
   */
  public static InvariantEntry convertRankgingFunctionsToTransitionInvariants(
      Collection<TerminationArgument> pArguments,
      CFANode pLoopHead,
      CFAEdge pIncomingLoopEdge,
      Map<IProgramVar, CExpression> pCExpressionsOfVariables) {
    ImmutableList.Builder<Optional<String>> transitionInvariants = ImmutableList.builder();

    // Ideally, this should be done via AstToCFARelation, however, this breaks due to copying of CFA
    // This is planned in https://gitlab.com/sosy-lab/software/cpachecker/-/work_items/1483
    FileLocation fileLocation = pIncomingLoopEdge.getFileLocation();
    LocationRecord locationRecord =
        LocationRecord.createLocationRecordAtStart(
            fileLocation,
            pLoopHead.getFunction().getFileLocation().getFileName().toString(),
            pLoopHead.getFunctionName());
    for (TerminationArgument argument : pArguments) {
      RankingFunction rankingFunction = argument.getRankingFunction();
      if (rankingFunction instanceof LexicographicRankingFunction pLexicographicRankingFunction) {
        for (int i = 0; i < pLexicographicRankingFunction.getComponents().length; i++) {
          Optional<String> componentConjunction =
              constructTransitionInvariantRelation(
                  pLexicographicRankingFunction.getComponents()[i], true, pCExpressionsOfVariables);
          for (int j = 0; j < pLexicographicRankingFunction.getComponents().length; j++) {
            Optional<String> relation =
                constructTransitionInvariantRelation(
                    pLexicographicRankingFunction.getComponents()[j],
                    false,
                    pCExpressionsOfVariables);
            componentConjunction =
                componentConjunction.flatMap(
                    conjunction -> relation.map(r -> conjunction + "&&" + r));
          }
          transitionInvariants.add(componentConjunction);
        }
      } else if (rankingFunction instanceof NestedRankingFunction pNestedRankingFunction) {
        for (AffineFunction nestedRankingFunction : pNestedRankingFunction.getComponents()) {
          transitionInvariants.add(
              constructTransitionInvariantRelation(
                  nestedRankingFunction, true, pCExpressionsOfVariables));
        }
      } else {
        transitionInvariants.add(
            constructTransitionInvariantRelation(rankingFunction, true, pCExpressionsOfVariables));
      }
    }
    ImmutableList<Optional<String>> disjuncts = transitionInvariants.build();
    // Weakening a transition invariant to true keeps it a transition invariant
    String transitionInvariant =
        disjuncts.stream().allMatch(Optional::isPresent)
            ? FluentIterable.from(disjuncts)
                .transform(disjunct -> "(" + disjunct.orElseThrow() + ")")
                .join(Joiner.on(" || "))
            : "1";
    return new InvariantEntry(
        transitionInvariant,
        InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
        YAMLWitnessExpressionType.EXT_C,
        locationRecord);
  }

  /**
   * Returns the relation between the values of the ranking function in the previous and the current
   * state as C expression, or empty if it cannot be expressed in C.
   */
  private static Optional<String> constructTransitionInvariantRelation(
      RankingFunction pRankingFunction,
      boolean pStrictRelation,
      Map<IProgramVar, CExpression> pCExpressionsOfVariables) {
    if (pRankingFunction instanceof LinearRankingFunction linearRankingFunction) {
      return constructTransitionInvariantRelation(
          linearRankingFunction.getComponent(), pStrictRelation, pCExpressionsOfVariables);
    }
    // LassoAnalysis synthesizes only linear and nested ranking functions
    return Optional.empty();
  }

  /**
   * Returns the relation between the values of the ranking function in the previous and the current
   * state as C expression, or empty if it cannot be expressed in C.
   */
  private static Optional<String> constructTransitionInvariantRelation(
      AffineFunction pRankingFunction,
      boolean pStrictRelation,
      Map<IProgramVar, CExpression> pCExpressionsOfVariables) {
    Optional<String> prevRank =
        toCExpression(
            pRankingFunction,
            pCExpressionsOfVariables,
            TerminationArgumentsToWitnessUtils::castToInt128AtAnyPrev);
    Optional<String> currentRank =
        toCExpression(
            pRankingFunction,
            pCExpressionsOfVariables,
            TerminationArgumentsToWitnessUtils::castToInt128);
    String relation = pStrictRelation ? " > " : " >= ";
    return prevRank.flatMap(prev -> currentRank.map(current -> prev + relation + current));
  }

  public static Set<TerminationArgument> collectArgumentsForNestedLoops(
      Loop pLoop, Set<Loop> pAllLoops, Multimap<Loop, TerminationArgument> pTerminationArguments) {
    Set<TerminationArgument> argumentsForNestedLoops = new HashSet<>();
    for (Loop loop : pAllLoops) {
      for (CFAEdge innerEdge : loop.getInnerLoopEdges()) {
        if (pLoop.getLoopHeads().contains(innerEdge.getPredecessor())) {
          argumentsForNestedLoops.addAll(pTerminationArguments.get(loop));
        }
      }
    }
    return argumentsForNestedLoops;
  }

  public static ImmutableList<AbstractInvariantEntry> getTransitionInvariantsFromARG(
      UnmodifiableReachedSet pReached,
      CFA pCFA,
      FormulaManagerView fmgr,
      BooleanFormulaManagerView bfmgr,
      CProgramScope scope) {
    Map<FileLocation, InvariantEntry> transitionInvariants = new HashMap<>();
    AstCfaRelation astCfaRelation = pCFA.getAstCfaRelation();
    for (AbstractState state :
        pReached.stream()
            .filter(
                state ->
                    !AbstractStates.extractStateByType(state, TerminationToReachState.class)
                        .getTransitionInvariants()
                        .isEmpty())
            .collect(ImmutableSet.toImmutableSet())) {
      TerminationToReachState terminationState =
          AbstractStates.extractStateByType(state, TerminationToReachState.class);
      CFANode location =
          AbstractStates.extractStateByType(state, LocationState.class).getLocationNode();
      String transitionInvariantAsC;

      for (PartitionedRelationFormula formula : terminationState.getTransitionInvariants()) {
        PartitionedRelationFormula wrappedFormula;
        wrappedFormula = formula.withPrevVarsWrapped(AT_PREFIX_NON_C, ANYPREV_SUFFIX);
        wrappedFormula = wrappedFormula.withCurrVarsWrapped("", "");
        // Transforming the candidate transition invariant from formula to C expression
        // and wrapping the previous variables into \\at(x, AnyPrev)
        try {
          transitionInvariantAsC =
              TransitionInvariantUtils.transformFormulaToStringWithReplacement(
                      wrappedFormula.getFormula(),
                      bfmgr,
                      fmgr,
                      scope,
                      variable ->
                          toNameInTransitionInvariant(
                              variable, getScopeAtNode(scope, location, astCfaRelation)))
                  .replace(AT_PREFIX_NON_C, AT_PREFIX);
          transitionInvariantAsC =
              TransitionInvariantUtils.removeFunctionFromVarsName(transitionInvariantAsC);
        } catch (CPAException e) {
          transitionInvariantAsC = "true";
        }

        Optional<IterationElement> iterationElement =
            astCfaRelation.getTightestIterationStructureForNode(location);
        if (iterationElement.isPresent()) {
          FileLocation fileLocation =
              iterationElement.orElseThrow().getCompleteElement().location();
          if (transitionInvariants.containsKey(fileLocation)) {
            if (!transitionInvariants
                .get(fileLocation)
                .getValue()
                .contains(transitionInvariantAsC)) {
              transitionInvariantAsC =
                  transitionInvariantAsC
                      + " || "
                      + transitionInvariants.get(fileLocation).getValue();
              transitionInvariants.remove(fileLocation, transitionInvariants.get(fileLocation));
              LocationRecord locationEntry =
                  LocationRecord.createLocationRecordAtStart(
                      iterationElement.orElseThrow().getCompleteElement().location(),
                      location.getFunction().getFileLocation().getFileName().toString(),
                      location.getFunctionName());

              transitionInvariants.put(
                  fileLocation,
                  new InvariantEntry(
                      transitionInvariantAsC,
                      InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
                      YAMLWitnessExpressionType.EXT_C,
                      locationEntry));
            }
          } else {
            LocationRecord locationEntry =
                LocationRecord.createLocationRecordAtStart(
                    iterationElement.orElseThrow().getCompleteElement().location(),
                    location.getFunction().getFileLocation().getFileName().toString(),
                    location.getFunctionName());

            transitionInvariants.put(
                fileLocation,
                new InvariantEntry(
                    transitionInvariantAsC,
                    InvariantRecordType.TRANSITION_LOOP_INVARIANT.getKeyword(),
                    YAMLWitnessExpressionType.EXT_C,
                    locationEntry));
          }
        }
      }
    }
    return transitionInvariants.values().stream().collect(ImmutableList.toImmutableList());
  }
}

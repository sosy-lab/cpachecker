/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.matching;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.QuantifiedFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelection.LogicalConnection;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtQuantificationPattern.QuantifierType;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public class SmtAstMatcherImpl implements SmtAstMatcher {

  private final UnsafeFormulaManager ufmgr;
  private final BooleanFormulaManager bfmgr;
  private final QuantifiedFormulaManager qfmgr;
  private final FormulaManager fm;

  protected final Multimap<Comparable<?>, Comparable<?>> functionAliases = HashMultimap.create();
  protected final Multimap<Comparable<?>, Comparable<?>> functionImpliedBy = HashMultimap.create();
  protected final Map<Comparable<?>, Comparable<?>> functionRotations = Maps.newHashMap();
  protected final Set<Comparable<?>> commutativeFunctions = Sets.newTreeSet();

  public SmtAstMatcherImpl(
      UnsafeFormulaManager pUfmgr, BooleanFormulaManager pBfmgr,
      QuantifiedFormulaManager pQfmgr, FormulaManager pFm) {
    ufmgr = pUfmgr;
    bfmgr = pBfmgr;
    qfmgr = pQfmgr;
    fm = pFm;

    defineRotations(">=", "<="); // IMPORTANT: This should NOT define a NEGATION!
    defineRotations(">", "<");

    defineCommutative("=");
    defineCommutative("+");
    defineCommutative("*");
    defineCommutative("and"); // used in the arguments of a quantified predicate

    defineFunctionAliases("*", Sets.newHashSet("Integer__*_", "Real__*_"));
    defineFunctionAliases("-", Sets.newHashSet("Integer__-_", "Real__-_"));
    defineFunctionAliases("+", Sets.newHashSet("Integer__+_", "Real__+_"));
  }

  protected SmtAstMatchResult newMatchFailedResult(Object... pDescription) {
    return SmtAstMatchResult.NOMATCH_RESULT;
  }

  protected SmtAstMatchResult wrapPositiveMatchResult(SmtAstMatchResult pResult, Object... pDescription) {
    //debugLogFromUnitTesting(ObjectArrays.concat("MATCH SUCCESS", pDescription));
    return pResult;
  }

  @Override
  public SmtAstMatchResult perform(
      SmtAstPatternSelection pPatternSelection,
      @Nullable Formula pParent,
      Formula pF,
      Optional<Multimap<String, Formula>> bBindingRestrictions) {

    return matchSelectionOnOneFormula(pParent, pF,
        pPatternSelection, bBindingRestrictions);
  }

  private SmtAstMatchResult matchSelectionOnOneFormula(
      final Formula pParentFormula,
      final Formula pF,
      final SmtAstPatternSelection pPatternSelection,
      final Optional<Multimap<String, Formula>> pBindingRestrictions) {
    // TODO: Cache the match result

    int matches = 0;
    SmtAstMatchResultImpl aggregatedResult = new SmtAstMatchResultImpl();

    for (SmtAstPatternSelectionElement p: pPatternSelection) {
      final SmtAstMatchResult r;
      if (p instanceof SmtAstPattern) {
        SmtAstPattern asp = (SmtAstPattern) p;
        r = internalPerform(pParentFormula, pF,
            asp, pBindingRestrictions);
      } else if (p instanceof SmtAstPatternSelection){
        r = matchSelectionOnOneFormula(pParentFormula, pF,
            (SmtAstPatternSelection) p, pBindingRestrictions);
      } else {
        throw new UnsupportedOperationException("Unknown Ast Pattern Type!");
      }

      matches = matches + (r.matches() ? 1 : 0);

      if (r.matches() && pPatternSelection.getRelationship().isNone()) {
        return newMatchFailedResult("Match but NONE should!");
      }

      if (!r.matches() && pPatternSelection.getRelationship().isAnd()) {
        return newMatchFailedResult("No match but ALL should!");
      }

      if (r.matches() && pPatternSelection.getRelationship().isOr()) {
        return wrapPositiveMatchResult(r, "OR logic");
      }

      if (r.matches()) {
        aggregatedResult.setMatchingRootFormula(pF);
        aggregatedResult.putMatchingArgumentFormula(p, pF); // TODO: Refactor/code duplication
        for (String boundVar: r.getBoundVariables()) {
          for (Formula varBinding : r.getVariableBindings(boundVar)) {
            aggregatedResult.putBoundVaribale(boundVar, varBinding);
          }
        }
      }
    }

    if (matches == 0 && pPatternSelection.getRelationship().isOr()) {
      return newMatchFailedResult("No match but ANY should!");
    }

    return wrapPositiveMatchResult(aggregatedResult, "Late return");
  }

  protected SmtAstMatchResult handleFunctionApplication(
      final Formula pFunctionRootFormula,
      final SmtAstMatchResultImpl result,
      final SmtFunctionApplicationPattern fp,
      final String functionSymbol,
      final ArrayList<Formula> pFunctionArguments,
      final Optional<Multimap<String, Formula>> pBindingRestrictions) {

    // ---------------------------------------------
    // We might consider the reversion version of the function
    boolean considerArgumentsInReverse = false; // TODO: Add support for cases where NOT fp.function.isPresent()
    if (fp.function.isPresent()) {
      boolean isExpectedFunctApp = isExpectedFunctionSymbol(fp.function.get(), functionSymbol);

      if (!isExpectedFunctApp) {
        Comparable<?> functionSymbolRotated = functionRotations.get(functionSymbol);
        if (functionSymbolRotated != null) {
          if (isExpectedFunctionSymbol(fp.function.get(), functionSymbolRotated)) {
            isExpectedFunctApp = true;
            considerArgumentsInReverse = true;
          }
        }
      }

      if (!isExpectedFunctApp) {
        return newMatchFailedResult("Function missmatch!", fp.function.get(), functionSymbol);
      }
    }

    // ---------------------------------------------
    // in case of AND, the number of arguments must match
    if (fp.getArgumentsLogic().isAnd()) {
      if (fp.getArgumentPatternCount() != pFunctionArguments.size()) {
        return SmtAstMatchResult.NOMATCH_RESULT;
      }
    }

    boolean initialReverseMatching = considerArgumentsInReverse;

    SmtAstMatchResult argumentMatchingResult = matchFormulaChildrenInSequence(
        pFunctionRootFormula,
        pFunctionArguments, fp.argumentPatterns,
        pBindingRestrictions, initialReverseMatching);

    if (!argumentMatchingResult.matches() && isCommutative(functionSymbol)) {
      argumentMatchingResult = matchFormulaChildrenInSequence(
          pFunctionRootFormula,
          pFunctionArguments, fp.argumentPatterns,
          pBindingRestrictions, !initialReverseMatching);
    }

    if (argumentMatchingResult.matches()) {
      result.addSubResults(argumentMatchingResult);
      return result;
    } else {
      // Encodes the reason for the failure
      return argumentMatchingResult;
    }
  }

  protected SmtAstMatchResult matchFormulaChildrenInSequence(
      final Formula pRootFormula,
      final List<? extends Formula> pChildFormulas,
      final SmtAstPatternSelection pChildPatterns,
      final Optional<Multimap<String, Formula>> pBindingRestrictions,
      boolean pConsiderPatternsInReverse) {

    final LogicalConnection logic = pChildPatterns.getRelationship();
    final Iterator<SmtAstPatternSelectionElement> pItPatternsInSequence;
    if (pConsiderPatternsInReverse) {
      pItPatternsInSequence = Lists.reverse(pChildPatterns.getPatterns()).iterator();
    } else {
      pItPatternsInSequence = pChildPatterns.iterator();
    }

    SmtAstMatchResultImpl result = new SmtAstMatchResultImpl();
    result.setMatchingRootFormula(pRootFormula);

    if (logic.isDontCare()) {
      return wrapPositiveMatchResult(result, "Don't care");
    }

    // Perform the matching recursively on the arguments
    Set<SmtAstPatternSelectionElement> argPatternsMatched = Sets.newHashSet();

    for (Formula childFormula: pChildFormulas) {
      if (!pItPatternsInSequence.hasNext()) {
        break;
      }

      final SmtAstPatternSelectionElement argPattern = pItPatternsInSequence.next();
      final SmtAstMatchResult functionArgumentResult;

      if (argPattern instanceof SmtAstPattern) {
        functionArgumentResult = internalPerform(
            pRootFormula,
            childFormula,
            (SmtAstPattern) argPattern,
            pBindingRestrictions);
      } else {
        functionArgumentResult = matchSelectionOnOneFormula(
            pRootFormula,
            childFormula,
            (SmtAstPatternSelection) argPattern,
            pBindingRestrictions);
      }

      if (functionArgumentResult.matches()) {
        argPatternsMatched.add(argPattern);
        result.putMatchingArgumentFormula(argPattern, childFormula);
        for (String boundVar: functionArgumentResult.getBoundVariables()) {
          for (Formula varBinding : functionArgumentResult.getVariableBindings(boundVar)) {
            result.putBoundVaribale(boundVar, varBinding);
          }
        }

        if (logic.isNone()) {
          return newMatchFailedResult("Match but NONE should!");
        }

      } else if (logic.isAnd()) {
        return newMatchFailedResult("No match but ALL should!");
      }
    }

    if (argPatternsMatched.isEmpty()
        && logic.isOr()) {
      return newMatchFailedResult("No match but ANY should!");
    }

    if (argPatternsMatched.size() != pChildPatterns.getPatterns().size()
        && logic.isAnd()) {
      // assert false; // might be dead code
      return newMatchFailedResult("No match but ALL should!");
    }

    return wrapPositiveMatchResult(result, "Last in matchFormulaChildrenInSequence");
  }

  protected boolean isExpectedFunctionSymbol(Comparable<?> pExpectedSymbol, Comparable<?> pFound) {
    // Either it is equivalent...
    boolean result = pExpectedSymbol.equals(pFound);

    // Or there is a symmetric alias...
    if (!result) {
      Collection<Comparable<?>> definedAliase = functionAliases.get(pExpectedSymbol);
      for (Comparable<?> alias: definedAliase) {
        if (alias.equals(pFound)) {
          return true;
        }
      }
    }

    // Or the operator implies (or is implied by) another operator...
    if (!result) {
      // Example:
      //  pExpectedSymbol == "="
      //      pFound == ">="
      //  or  pFound == "<="
      //
      if (functionImpliedBy.get(pExpectedSymbol).contains(pFound)) {
        return true;
      }
    }

    return result;
  }

  protected boolean isCommutative(final String pFunctionName) {
    return commutativeFunctions.contains(pFunctionName);
  }

  @Override
  public void defineCommutative(String pFunctionName) {
    commutativeFunctions.add(pFunctionName);
  }

  @Override
  public void defineRotations(String pFunctionName, String pRotationFunctionName) {
    functionRotations.put(pFunctionName, pRotationFunctionName);
    functionRotations.put(pRotationFunctionName, pFunctionName);
  }

  @Override
  public void defineFunctionAliases(String pFunctionName, Set<String> pAliases) {
    functionAliases.putAll(pFunctionName, pAliases);
  }


  @Override
  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF) {
    return perform(pPatternSelection, null, pF, Optional.<Multimap<String, Formula>>absent());
  }

  protected SmtAstMatchResult internalPerform(
      final Formula pParentFormula,
      final Formula pRootFormula,
      final SmtAstPattern pP, Optional<Multimap<String, Formula>> pBindingRestrictions) {

    final SmtAstMatchResultImpl result = new SmtAstMatchResultImpl();
    result.setMatchingRootFormula(pRootFormula);

    if (pP.getBindMatchTo().isPresent()) {
      final String bindMatchTo = pP.getBindMatchTo().get();
      result.putBoundVaribale(bindMatchTo, pRootFormula);
      result.putBoundVaribale(bindMatchTo + ".parent" , pParentFormula);

      if (!bindMatchTo.contains("?")) {
        if (pBindingRestrictions.isPresent()) {
          Collection<Formula> variableAlreadyBoundTo = pBindingRestrictions.get().get(bindMatchTo);
          assert variableAlreadyBoundTo.size() <= 1;
          if (variableAlreadyBoundTo.size() > 0) {
            if (!variableAlreadyBoundTo.contains(pRootFormula)) {
              return newMatchFailedResult(String.format("Binding of variable %s does not match!", bindMatchTo));
            }
          }
        }
      }
    }

    if (pRootFormula instanceof BooleanFormula
        && qfmgr.isQuantifier((BooleanFormula)pRootFormula)) {
      BooleanFormula booleanFormula = (BooleanFormula) pRootFormula;

      if (!(pP instanceof SmtQuantificationPattern)) {
        return newMatchFailedResult("No function application!");
      }
      SmtQuantificationPattern qp = (SmtQuantificationPattern) pP;

      SmtQuantificationPattern.QuantifierType quantifierType
          = qfmgr.isForall(booleanFormula)
          ? QuantifierType.FORALL
          : QuantifierType.EXISTS;

      if (qp.matchQuantificationWithType.isPresent()) {
        if (!qp.matchQuantificationWithType.get().equals(quantifierType)) {
          return newMatchFailedResult("Different quantifier!");
        }
      }

      BooleanFormula bodyFormula = qfmgr.getQuantifierBody(booleanFormula);

      return handleQuantification(
          pRootFormula, result, qp, bodyFormula, pBindingRestrictions);

    } else {
      if (!(pP instanceof SmtFunctionApplicationPattern)) {
        return newMatchFailedResult("Got unexpected function application!");
      }

      // todo: check that it is always applicable.
      SmtFunctionApplicationPattern fp = (SmtFunctionApplicationPattern) pP;

      if (fp.customFormulaMatcher.isPresent()) {
        if (!fp.customFormulaMatcher.get().formulaMatches(fm, pRootFormula)) {
          return newMatchFailedResult("Custom matcher not matched!");
        }
      }

      final String functionSymbol;
      final int functionParameterCount;

      functionParameterCount = ufmgr.getArity(pRootFormula);
      functionSymbol = ufmgr.getName(pRootFormula);

      if (functionParameterCount == 0) {
        if (pP.getBindMatchTo().isPresent()) {
          final String bindMatchTo = pP.getBindMatchTo().get();
          if (bindMatchTo.startsWith(".") &&
              !qfmgr.isBoundByQuantifier(pRootFormula)) {

            // No match if we are on a variable, that was not bound by a
            // quantifier, but it is expected to be so.
            return newMatchFailedResult("Variable not quantified, "
                + "quantification expected");
          }
        }
      }

      final ArrayList<Formula> functionArguments = Lists.newArrayList();
      for (int i=0; i<functionParameterCount; i++) {
        final Formula argFormula = ufmgr.getArg(pRootFormula, i);
        functionArguments.add(argFormula);
      }

      return handleFunctionApplication(
          pRootFormula, result, fp, functionSymbol, functionArguments,
          pBindingRestrictions);
    }
  }

  private SmtAstMatchResult handleQuantification(
      final Formula pRootFormula,
      final SmtAstMatchResultImpl pResult,
      final SmtQuantificationPattern pQp,
      final BooleanFormula pBodyFormula,
      final Optional<Multimap<String, Formula>> bBindingRestrictions) {

    final List<BooleanFormula> bodyConjuncts = extractConjuncts(pBodyFormula, false);

    SmtAstMatchResult bodyMatchingResult = matchFormulaChildrenInSequence(
        pRootFormula,
        bodyConjuncts, pQp.quantorBodyMatchers, bBindingRestrictions, false);

    if (!bodyMatchingResult.matches() && bodyConjuncts.size() > 0) {
      bodyMatchingResult = matchFormulaChildrenInSequence(
          pRootFormula,
          bodyConjuncts, pQp.quantorBodyMatchers, bBindingRestrictions, true);
    }

    if (bodyMatchingResult.matches()) {
      pResult.addSubResults(bodyMatchingResult);
      return pResult;
    } else {
      // Encodes the reason for the failure
      return bodyMatchingResult;
    }
  }

  private List<BooleanFormula> extractConjuncts(BooleanFormula pFormula,
      boolean pRecursive) {
    List<BooleanFormula> result = Lists.newArrayList();
    if (bfmgr.isAnd(pFormula)) {
      for (int i=0; i<ufmgr.getArity(pFormula); i++) {
        BooleanFormula child = (BooleanFormula) ufmgr.getArg(pFormula, i);
        if (pRecursive) {
          result.addAll(extractConjuncts(child, true));
        } else {
          result.add(child);
        }
      }
    }
    return result;
  }

  @Override
  public <T1 extends Formula, T2 extends Formula> T1 substitute(T1 f,
      Map<T2, T2> fromToMapping) {
    return ufmgr.substitute(f, fromToMapping);
  }

}

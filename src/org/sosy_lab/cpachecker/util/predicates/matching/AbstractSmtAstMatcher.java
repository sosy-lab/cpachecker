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
import java.util.Stack;

import javax.annotation.Nullable;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelection.LogicalConnection;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;


public abstract class AbstractSmtAstMatcher implements SmtAstMatcher {

  protected final Multimap<Comparable<?>, Comparable<?>> functionAliases = HashMultimap.create();
  protected final Multimap<Comparable<?>, Comparable<?>> functionImpliedBy = HashMultimap.create();
  protected final Map<Comparable<?>, Comparable<?>> functionRotations = Maps.newHashMap();
  protected final Set<Comparable<?>> commutativeFunctions = Sets.newTreeSet();

  public AbstractSmtAstMatcher() {

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

  //private void debugLogFromUnitTesting(Object... pDescription) {
  //for (Object o : pDescription) {
  //  out.print(o.toString());
  //  out.print(" ");
  //}
  //out.println();
  //}

  protected SmtAstMatchResult newMatchFailedResult(Object... pDescription) {
    //debugLogFromUnitTesting(ObjectArrays.concat("FAILED MATCH", pDescription));
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

    return matchSelectionOnOneFormula(pParent, pF, new Stack<String>(), pPatternSelection, bBindingRestrictions);
  }

  protected abstract SmtAstMatchResult internalPerform(
      final Formula pParentFormula,
      final Formula pRootFormula,
      final Stack<String> pQuantifiedVariables,
      final SmtAstPattern pP, Optional<Multimap<String, Formula>> pBindingRestrictions);

  private SmtAstMatchResult matchSelectionOnOneFormula(
      final Formula pParentFormula,
      final Formula pF,
      final Stack<String> pQuantifiedVariables,
      final SmtAstPatternSelection pPatternSelection,
      final Optional<Multimap<String, Formula>> pBindingRestrictions) {
    // TODO: Cache the match result

    int matches = 0;
    SmtAstMatchResultImpl aggregatedResult = new SmtAstMatchResultImpl();

    for (SmtAstPatternSelectionElement p: pPatternSelection) {
      final SmtAstMatchResult r;
      if (p instanceof SmtAstPattern) {
        SmtAstPattern asp = (SmtAstPattern) p;
        r = internalPerform(pParentFormula, pF, pQuantifiedVariables, asp, pBindingRestrictions);
      } else if (p instanceof SmtAstPatternSelection){
        r = matchSelectionOnOneFormula(pParentFormula, pF, pQuantifiedVariables, (SmtAstPatternSelection) p, pBindingRestrictions);
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
      final Stack<String> pBoundQuantifiedVariables,
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

    SmtAstMatchResult argumentMatchingResult = matchFormulaChildsInSequence(
        pFunctionRootFormula, pBoundQuantifiedVariables,
        pFunctionArguments, fp.argumentPatterns,
        pBindingRestrictions, initialReverseMatching);

    if (!argumentMatchingResult.matches() && isCommutative(functionSymbol)) {
      argumentMatchingResult = matchFormulaChildsInSequence(
          pFunctionRootFormula, pBoundQuantifiedVariables,
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

  protected SmtAstMatchResult matchFormulaChildsInSequence(
      final Formula pRootFormula,
      final Stack<String> pBoundQuantifiedVariables,
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
      return wrapPositiveMatchResult(result, "Dont care");
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
            pBoundQuantifiedVariables,
            (SmtAstPattern) argPattern,
            pBindingRestrictions);
      } else {
        functionArgumentResult = matchSelectionOnOneFormula(
            pRootFormula,
            childFormula,
            pBoundQuantifiedVariables,
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

    return wrapPositiveMatchResult(result, "Last in matchFormulaChildsInSequence");
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

}

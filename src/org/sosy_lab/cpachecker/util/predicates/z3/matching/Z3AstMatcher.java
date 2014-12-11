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
package org.sosy_lab.cpachecker.util.predicates.z3.matching;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;
import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.FormulaCreator;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaCreator;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiHelpers;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtAstPatternSelection.LogicalConnection;
import org.sosy_lab.cpachecker.util.predicates.z3.matching.SmtQuantificationPattern.QuantifierType;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Sets;

public class Z3AstMatcher implements SmtAstMatcher {

  private final long ctx;
  private final Z3FormulaManager fm;
  private final FormulaCreator<Long, Long, Long> fmc;
  private final LogManager logger;
  private final FormulaManagerView fmv;

  private Multimap<Comparable<?>, Comparable<?>> functionAliases = HashMultimap.create();
  private Map<Comparable<?>, Comparable<?>> functionRotations = Maps.newHashMap();
  private Set<Comparable<?>> commutativeFunctions = Sets.newTreeSet();

  protected static class BoundVariable {
    final FormulaType<?> variableType;
    final String variableName;

    public BoundVariable(FormulaType<?> pVariableType, String pVariableName) {
      variableType = pVariableType;
      variableName = pVariableName;
    }
  }

  public Z3AstMatcher(LogManager pLogger, FormulaManager pFm, FormulaManagerView pFmv) {
    this.logger = pLogger;
    this.fm = (Z3FormulaManager) pFm;
    this.fmv = pFmv;
    this.ctx = fm.getEnvironment();
    this.fmc = fm.getFormulaCreator();

    defineRotations(">=", "<="); // IMPORTANT: This should NOT define a NEGATION!
    defineRotations(">", "<");

    defineCommutative("=");
    defineCommutative("and"); // used in the arguments of a quantified predicate

    defineFunctionAliases("*", Sets.newHashSet("Integer__*_", "Real__*_"));
  }

  @Override
  public SmtAstMatchResult perform(SmtAstPattern pP, Formula pF, Optional<Multimap<String, Formula>> bBindingRestrictions) {
    final SmtAstPatternSelection sel = SmtAstPatternBuilder.and(pP);
    return perform(sel, pF, bBindingRestrictions);
  }

  @Override
  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF, Optional<Multimap<String, Formula>> bBindingRestrictions) {
    return matchSelectionOnOneFormula(pPatternSelection, pF);
  }

  private SmtAstMatchResult newMatchFailedResult(Object... pDescription) {
    logger.log(Level.ALL, ObjectArrays.concat("FAILED MATCH", pDescription));
    return SmtAstMatchResult.NOMATCH_RESULT;
  }

  private SmtAstMatchResult matchSelectionOnOneFormula(final SmtAstPatternSelection pPatternSelection, final Formula pF) {
    // TODO: Cache the match result

    int matches = 0;
    SmtAstMatchResultImpl aggregatedResult = new SmtAstMatchResultImpl();

    for (SmtAstPattern p: pPatternSelection) {
      SmtAstMatchResult r = internalPerform(p, pF);
      matches = matches + (r.matches() ? 1 : 0);

      if (r.matches() && pPatternSelection.getRelationship().isNone()) {
        return newMatchFailedResult("Match but NONE should!");
      }

      if (!r.matches() && pPatternSelection.getRelationship().isAnd()) {
        return newMatchFailedResult("No match but ALL should!");
      }

      if (r.matches() && pPatternSelection.getRelationship().isOr()) {
        return r;
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

    return aggregatedResult;
  }

  private SmtAstMatchResult internalPerform(final SmtAstPattern pP, final Formula pRootFormula) {

    final SmtAstMatchResultImpl result = new SmtAstMatchResultImpl();
    result.setMatchingRootFormula(pRootFormula);

    if (pP.getBindMatchTo().isPresent()) {
      result.putBoundVaribale(pP.getBindMatchTo().get(), pRootFormula);
    }

    final long ast = fm.extractInfo(pRootFormula);
    final int astKind = get_ast_kind(ctx, ast);

    switch (astKind) {
    case Z3_NUMERAL_AST: // handle this as an unary function
    case Z3_APP_AST:  // k-nary function // -------------------------------------------------
      if (!(pP instanceof SmtFunctionApplicationPattern)) {
        return newMatchFailedResult("No function application!");
      }
      SmtFunctionApplicationPattern fp = (SmtFunctionApplicationPattern) pP;

      final String functionSymbol;
      final int functionParameterCount;

      if (astKind == Z3_NUMERAL_AST) {
        functionSymbol = ast_to_string(ctx, ast);
        functionParameterCount = 0;
      } else {
        functionSymbol = Z3NativeApiHelpers.getDeclarationSymbolText(ctx, get_app_decl(ctx, ast));
        functionParameterCount = get_app_num_args(ctx, ast);
      }

      final ArrayList<Formula> functionArguments = Lists.newArrayList();
      for (int i=0; i<functionParameterCount; i++) {
        final long argAst = get_app_arg(ctx, ast, i);
        final Formula argFormula = encapsulateAstAsFormula(argAst);
        functionArguments.add(argFormula);
      }

      return handleFunctionApplication(pRootFormula, result, fp, functionSymbol, functionArguments);

    case Z3_QUANTIFIER_AST: // -------------------------------------------------
      if (!(pP instanceof SmtQuantificationPattern)) {
        return newMatchFailedResult("No function application!");
      }
      SmtQuantificationPattern qp = (SmtQuantificationPattern) pP;

      SmtQuantificationPattern.QuantifierType quantifierType
        = is_quantifier_forall(ctx, ast)
          ? QuantifierType.FORALL
          : QuantifierType.EXISTS;

      BooleanFormula bodyFormula = (BooleanFormula) encapsulateAstAsFormula(get_quantifier_body(ctx, ast));
      ArrayList<BoundVariable> boundVariables = getBoundVariables(ast);

      return handleQuantification(pRootFormula, result, qp, quantifierType, boundVariables, bodyFormula);

    case Z3_VAR_AST: // -------------------------------------------------
    case Z3_SORT_AST:
    case Z3_FUNC_DECL_AST:
    default:
      break;
    }

    return SmtAstMatchResult.NOMATCH_RESULT;
  }

  private ArrayList<BoundVariable> getBoundVariables(final long ast) {
    final ArrayList<BoundVariable> boundVariables = Lists.newArrayList();
    final int boundCount = get_quantifier_num_bound(ctx, ast);
    for (int b=0; b<boundCount; b++) {
      long boundVariableSort = get_quantifier_bound_sort(ctx, ast, b);
      FormulaType<?> boundVariableType = ((Z3FormulaCreator) fmc).getFormulaTypeFromSort(boundVariableSort);
      String boundVariableName = get_symbol_string(ctx, get_quantifier_bound_name(ctx, ast, b));

      boundVariables.add(new BoundVariable(
          boundVariableType,
          boundVariableName));
    }
    return boundVariables;
  }

  private Formula encapsulateAstAsFormula(final long ast) {
    inc_ref(ctx, ast); // TODO: This should be done within 'FormulaCreator.encapsulate'
    FormulaType<?> formulaType = fmc.getFormulaType(ast);
    Formula f = fmc.encapsulate(formulaType, ast);
    return f;
  }

  private SmtAstMatchResult handleQuantification(Formula pRootFormula, SmtAstMatchResultImpl pResult,
      SmtQuantificationPattern pQp, QuantifierType pQuantifierType, ArrayList<BoundVariable> pBoundVariables,
      BooleanFormula pBodyFormula) {

    final List<BooleanFormula> bodyConjuncts = Lists.newArrayList(fmv.extractAtoms(pBodyFormula, false, true));

    SmtAstMatchResult bodyMatchingResult = matchFormulaChildsInSequence(pRootFormula, bodyConjuncts, pQp.quantorBodyMatchers, false);

    if (bodyMatchingResult.matches()) {
      pResult.addSubResults(bodyMatchingResult);
      return pResult;
    } else {
      // Encodes the reason for the failure
      return bodyMatchingResult;
    }
  }

  private SmtAstMatchResult handleFunctionApplication(
      final Formula pFunctionRootFormula,
      final SmtAstMatchResultImpl result,
      final SmtFunctionApplicationPattern fp,
      final String functionSymbol,
      final ArrayList<Formula> pFunctionArguments) {

    // ---------------------------------------------
    // We might consider the reversion version of the function
    boolean considerArgumentsInReverse = false; // TODO: Add support for cases where NOT fp.function.isPresent()
    if (fp.function.isPresent()) {
      boolean isExpectedFunction = isExpectedFunctionSumbol(fp.function.get(), functionSymbol);

      if (!isExpectedFunction) {
        Comparable<?> functionSymbolRotated = functionRotations.get(functionSymbol);
        if (functionSymbolRotated != null) {
          if (isExpectedFunctionSumbol(fp.function.get(), functionSymbolRotated)) {
            isExpectedFunction = true;
            considerArgumentsInReverse = true;
          }
        }
      }

      if (!isExpectedFunction) {
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
        pFunctionRootFormula, pFunctionArguments, fp.argumentPatterns, initialReverseMatching);

    if (!argumentMatchingResult.matches() && isCommutative(functionSymbol)) {
      argumentMatchingResult = matchFormulaChildsInSequence(
          pFunctionRootFormula, pFunctionArguments, fp.argumentPatterns, !initialReverseMatching);
    }

    if (argumentMatchingResult.matches()) {
      result.addSubResults(argumentMatchingResult);
      return result;
    } else {
      // Encodes the reason for the failure
      return argumentMatchingResult;
    }
  }

  private SmtAstMatchResult matchFormulaChildsInSequence(
      final Formula pRootFormula,
      final List<? extends Formula> pChildFormulas,
      final SmtAstPatternSelection pChildPatterns,
      boolean pConsiderPatternsInReverse) {

    final LogicalConnection logic = pChildPatterns.getRelationship();
    final Iterator<SmtAstPattern> pItPatternsInSequence;
    if (pConsiderPatternsInReverse) {
      pItPatternsInSequence = Lists.reverse(pChildPatterns.getPatterns()).iterator();
    } else {
      pItPatternsInSequence = pChildPatterns.iterator();
    }

    SmtAstMatchResultImpl result = new SmtAstMatchResultImpl();
    result.setMatchingRootFormula(pRootFormula);

    if (logic.isDontCare()) {
      return result;
    }

    // Perform the matching recursively on the arguments
    Set<SmtAstPattern> argPatternsMatched = Sets.newHashSet();

    for (Formula childFormula: pChildFormulas) {
      if (!pItPatternsInSequence.hasNext()) {
        break;
      }
      final SmtAstPattern argPattern = pItPatternsInSequence.next();
      final SmtAstMatchResult functionArgumentResult = internalPerform(
            argPattern,
            childFormula);

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

      if (argPatternsMatched.isEmpty()
          && logic.isOr()) {
        return newMatchFailedResult("No match but ANY should!");
      }
    }

    if (argPatternsMatched.size() != pChildPatterns.getPatterns().size()
        && logic.isAnd()) {
      assert false; // might be dead code
      return newMatchFailedResult("No match but ALL should!");
    }

    return result;
  }

  private boolean isExpectedFunctionSumbol(Comparable<?> pExpectedSymbol, Comparable<?> pFound) {
    boolean result = pExpectedSymbol.equals(pFound);
    if (!result) {
      Collection<Comparable<?>> definedAliase = functionAliases.get(pExpectedSymbol);
      for (Comparable<?> alias: definedAliase) {
        if (alias.equals(pFound)) {
          return true;
        }
      }
    }
    return result;
  }

  private boolean isCommutative(final String pFunctionName) {
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
  public SmtAstMatchResult perform(SmtAstPattern pPattern, Formula pF) {
    return perform(pPattern, pF, Optional.<Multimap<String, Formula>>absent());
  }

  @Override
  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF) {
    return perform(pPatternSelection, pF, Optional.<Multimap<String, Formula>>absent());
  }

}

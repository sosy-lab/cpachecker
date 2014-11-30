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

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.basicimpl.FormulaCreator;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiHelpers;
import org.sosy_lab.cpachecker.util.test.DebugOutput;

import com.google.common.collect.Sets;

public class Z3AstMatcher implements SmtAstMatcher {

  private final long ctx;
  private final Z3FormulaManager fm;

  private FormulaCreator<Long, Long, Long> fmc;

  public Z3AstMatcher(Z3FormulaManager pFm, FormulaCreator<Long, Long, Long> pFormulaCreator) {
    this.ctx = pFm.getEnvironment();
    this.fm = pFm;
    this.fmc = pFormulaCreator;
  }

  @Override
  public SmtAstMatchResult perform(SmtAstPattern pP, Formula pF) {
    SmtAstPatternSelection sel = SmtAstPatternBuilder.and(pP);
    return perform(sel, pF);
  }

  @Override
  public SmtAstMatchResult perform(SmtAstPatternSelection pPatternSelection, Formula pF) {
    return internalPerform(pPatternSelection, pF, Sets.<Long>newTreeSet());
  }

  private SmtAstMatchResult internalPerform(final SmtAstPatternSelection pPatternSelection, final Formula pF, final Set<Long> visited) {
    // TODO: Cache the match result

    int matches = 0;
    Z3AstMatchResult aggregatedResult = new Z3AstMatchResult();

    for (SmtAstPattern p: pPatternSelection) {
      SmtAstMatchResult r = internalPerform(p, pF, Sets.newHashSet(visited));
      matches = matches + (r.matches() ? 1 : 0);

      if (r.matches() && pPatternSelection.getRelationship().isNone()) {
        return SmtAstMatchResult.NOMATCH_RESULT;
      }

      if (!r.matches() && pPatternSelection.getRelationship().isAnd()) {
        return SmtAstMatchResult.NOMATCH_RESULT;
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
      return SmtAstMatchResult.NOMATCH_RESULT;
    }

    return aggregatedResult;
  }

  private SmtAstMatchResult internalPerform(final SmtAstPattern pP, final Formula pF, final Set<Long> visited) {
    assert (visited.size() == 0);

    final long ast = fm.extractInfo(pF);
    final Z3AstMatchResult result = new Z3AstMatchResult();
    result.setMatchingRootFormula(pF);
    if (pP.getBindMatchTo().isPresent()) {
      result.putBoundVaribale(pP.getBindMatchTo().get(), pF);
    }

    // Z3_get_ast_id  (unique identifier)
    // Z3_is_eq_ast

    long astId = get_ast_id(ctx, ast);
    if (!visited.add(astId)) {
      return SmtAstMatchResult.NOMATCH_RESULT;
    }

    final int astKind = get_ast_kind(ctx, ast);
    switch (astKind) {
    case Z3_NUMERAL_AST: // handle this as an unary function
    case Z3_APP_AST:  // k-nary function
      if (!(pP instanceof SmtFunctionApplicationPattern)) {
        return SmtAstMatchResult.NOMATCH_RESULT;
      }

      SmtFunctionApplicationPattern fp = (SmtFunctionApplicationPattern) pP;

      final String functionSymbol;
      final int functionParameterCount;

      if (astKind == Z3_NUMERAL_AST) {
        functionSymbol = ast_to_string(ctx, ast);
        functionParameterCount = 0;
      } else {
        final long functionDeclaration = get_app_decl(ctx, ast);
        functionSymbol = Z3NativeApiHelpers.getDeclarationSymbolText(ctx, functionDeclaration);
        functionParameterCount = get_app_num_args(ctx, ast);
      }

      if (fp.function.isPresent()) {
        if (!fp.function.get().equals(functionSymbol)) {
          DebugOutput.debugMsg("Difference:", fp.function.get(), functionSymbol);
          return SmtAstMatchResult.NOMATCH_RESULT;
        }
      }

      if (fp.getArgumentsLogic().isAnd()) {
        if (fp.getArgumentPatternCount() != functionParameterCount) {
          return SmtAstMatchResult.NOMATCH_RESULT;
        }
      }

      // Perform the matching recursively on the arguments
      Iterator<SmtAstPattern> itPatternsInSequence = fp.getArgumentPatternIterator();
      for (int i=0; i<functionParameterCount; i++) {
        final long argAst = get_app_arg(ctx, ast, i);
        final FormulaType<?> argFormulaType = fmc.getFormulaType(argAst);
        final Formula argFormula = fmc.encapsulate(argFormulaType, argAst);
        inc_ref(ctx, argAst); // TODO: This should be done within 'FormulaCreator.encapsulate'

        Queue<SmtAstPattern> patternInSequence = new ArrayDeque<>();
        if (isCommutative(functionSymbol)) {
          if (itPatternsInSequence.hasNext()) {
            patternInSequence.add(itPatternsInSequence.next());
          } else {
            assert false;
          }
        } else {
          patternInSequence.addAll(fp.getArgumentPatterns());
        }

        int differentArgPatternsMatched = 0;

        while (!patternInSequence.isEmpty()) {
          final SmtAstPattern argPattern = patternInSequence.poll();
          final SmtAstMatchResult argMatchingResult = internalPerform(
                argPattern,
                argFormula,
                Sets.newHashSet(visited) // TODO: This might be not correct or not optimal. We have a DAG, i.e. we would not run into a problem
                );

          if (argMatchingResult.matches()) {
            differentArgPatternsMatched++;
            result.putMatchingArgumentFormula(argPattern, argFormula);
            for (String boundVar: argMatchingResult.getBoundVariables()) {
              for (Formula varBinding : argMatchingResult.getVariableBindings(boundVar)) {
                result.putBoundVaribale(boundVar, varBinding);
              }
            }

            if (fp.getArgumentsLogic().isNone()) {
              return SmtAstMatchResult.NOMATCH_RESULT;
            }

          } else if (fp.getArgumentsLogic().isAnd()) {
            return SmtAstMatchResult.NOMATCH_RESULT;
          }
        }

        if (differentArgPatternsMatched == 0
            && fp.getArgumentsLogic().isOr()) {
          return SmtAstMatchResult.NOMATCH_RESULT;
        }

        if (differentArgPatternsMatched != fp.getArgumentPatternCount()
            && fp.getArgumentsLogic().isAnd()) {
          return SmtAstMatchResult.NOMATCH_RESULT;
        }
      }

      return result;
    case Z3_VAR_AST:
    break;
    case Z3_QUANTIFIER_AST:
      // Z3_is_quantifier_forall
      // Z3_get_quantifier_weight
      // Z3_get_quantifier_body
      // Z3_get_quantifier_num_bound
      // Z3_get_quantifier_bound_name
      // Z3_get_quantifier_bound_sort
      // Z3_get_quantifier_body
      int boundCount = get_quantifier_num_bound(ctx, ast);
      for (int b=0; b<boundCount; b++) {

      }
      long bodyAst = get_quantifier_body(ctx, ast);

      // TODO

      break;
    case Z3_SORT_AST:
      // Z3_get_sort_kind
      // ...
      break;
    case Z3_FUNC_DECL_AST:  // search for Z3_func_decl in the API doc
      // Z3_get_decl_num_parameters
      // Z3_get_decl_parameter_kind
      // Z3_get_domain
      // Z3_get_range
      // Z3_get_arity
      // ...
      break;

    default:
      // Unknown AST (Z3_UNKNOWN_AST)
      break;
    }

    return SmtAstMatchResult.NOMATCH_RESULT;
  }

  private boolean isCommutative(final String pFunctionName) {
    return false;
  }


  @Override
  public void defineCommutative(String pFunctionName) {
    // TODO Auto-generated method stub

  }

  @Override
  public void defineRotations(String pFunctionName, String pRotationFunctionName) {
    // TODO Auto-generated method stub

  }

  @Override
  public void defineFunctionAliases(String pFunctionName, Set<String> pAliases) {
    // TODO Auto-generated method stub

  }

}

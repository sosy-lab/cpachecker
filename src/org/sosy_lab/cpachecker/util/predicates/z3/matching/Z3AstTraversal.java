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

import java.io.PrintStream;
import java.util.Set;

import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3FormulaManager;

import com.google.common.collect.Sets;

public class Z3AstTraversal {

  private Long ctx;
  private Z3FormulaManager fm;

  private Set<Integer> visited = Sets.newTreeSet();

  public Z3AstTraversal(Z3FormulaManager pFm) {
    this.ctx = pFm.getEnvironment();
    this.fm = pFm;
  }

  public boolean isIte(Formula f) {
    return false;
  }

  public boolean isTrue(Formula f) {
    return false;
  }

  public boolean isFalse(Formula f) {
    return false;
  }

  public boolean isEqivalence(Formula f) {
    return false;
  }

  public boolean isDistinct(Formula f) {
    return false;
  }

  public boolean isNot(Formula f) {
    return false;
  }

  public boolean isIff(Formula f) {
    return false;
  }

  public boolean isImplies(Formula f) {
    return false;
  }

  public boolean isXor(Formula f) {
    return false;
  }

  public boolean isAnd(Formula f) {
    return false;
  }

  public boolean isOr(Formula f) {
    return false;
  }

  public void traverse(Formula pFormula) {
    assert (visited.size() == 0);
    traverse(fm.extractInfo(pFormula), 0);
  }

  private void traverse(long ast, int depth) {
    final PrintStream x = System.out;

    // Z3_get_ast_id  (unique identifier)
    // Z3_is_eq_ast

    int astId = get_ast_id(ctx, ast);
    if (!visited.add(astId)) {
      return;
    }

    String text = ast_to_string(ctx, ast);
    x.println(text);

    final int astKind = get_ast_kind(ctx, ast);
    switch (astKind) {
    case Z3_NUMERAL_AST:
      break;
    case Z3_APP_AST:
      // Entry point for traversing childs!

      // Z3_get_app_num_args
      //    returns 0 for constants (unary function)
      // Z3_get_app_arg
      //    Return the i-th argument of the given application.
      // Z3_get_app_decl

      final long decl = get_app_decl(ctx, ast);
      x.println(ast_to_string(ctx, decl));

      final int argCount = get_app_num_args(ctx, ast);
      for (int i=0; i<argCount; i++) {
        final long argAst = get_app_arg(ctx, ast, i);

        traverse(argAst, depth+1);
      }

      break;
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
      traverse(bodyAst, depth+1);

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
  }

}

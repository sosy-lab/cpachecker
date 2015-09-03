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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.Formula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType.NumeralType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPattern.SmtAstMatchFlag;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtAstPatternSelection.LogicalConnection;
import org.sosy_lab.cpachecker.util.predicates.matching.SmtQuantificationPattern.QuantifierType;

import com.google.common.base.Optional;
import com.google.common.base.Verify;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;


public class SmtAstPatternBuilder {

  // Idea: Large parts of SMT formulas are described using functions.
  //  --> We can simplify the matching problem!!
  //
  //  Considering let-statements is not necessary because the are "only"
  //    used for representing common sub formulas, i.e., with multiple entry-edges (DAG)
  //
  //  We operate on the DAG

  /**
   * Match a specific function. This might be either a
   *  ... nullary (constant, variable)
   *  ... unary ('boolean not', ...)
   *  ... binary (equality, implication, different arithmetic operations, ...
   *  ... ternary (ITE, ...)
   *  ... n-ary (and, or, ... user-defined functions, ...)
   *
   * @param pFunction         String symbol that represents the function (as declared)
   * @param argumentMatchers  Matchers for the arguments of the function
   *
   * @return  A description of the pattern that matches the specified sub-formula within the AST
   */
  public static SmtAstPattern match(Comparable<?> pFunction, SmtAstPattern... argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        and(argumentMatchers));
  }

  /**
   * Matches only if all argument patterns of an arbitrary function match.
   *
   * The root node of the AST is also considered as a function with childs!!
   *
   * @param argumentMatchers  The child patterns.
   */
  public static SmtAstPattern match(SmtAstPattern... argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        and(argumentMatchers));
  }

  public static SmtAstPattern match(Comparable<?> pFunction, SmtAstPatternSelection pArgumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        pArgumentMatchers);
  }

  /**
   * The same as described in {@link #match}, but binds the matching formula to a variable.
   */
  public static SmtAstPattern matchBind(Comparable<?> pFunction, String pBindMatchTo, SmtAstPattern... argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.of(pBindMatchTo),
        and(argumentMatchers));
  }

  public static SmtAstPattern matchBind(Comparable<?> pFunction, String pBindMatchTo, SmtAstPatternSelection argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.of(pBindMatchTo),
        argumentMatchers);
  }


  /**
   * Match any function (and bind its formula to a variable), but with specific arguments.
   */
  public static SmtAstPattern matchAnyBind(String pBindMatchTo, SmtAstPattern... argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.of(pBindMatchTo),
        and(argumentMatchers));
  }

  public static SmtAstPattern matchAnyBind(String pBindMatchTo, SmtAstPatternSelection argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.of(pBindMatchTo),
        argumentMatchers);
  }

  /**
   * Match any function, but with specific arguments.
   */
  public static SmtAstPattern matchAny(SmtAstPattern... argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        and(argumentMatchers));
  }

  public static SmtAstPattern matchAnyWithArgs(SmtAstPatternSelection argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        argumentMatchers);
  }

  /**
   * Matches any function application, with any arguments.
   *
   * @return  Pattern.
   */
  public static SmtAstPattern matchAnyWithAnyArgs() {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        dontcare());
  }

  /**
   * The same as described in {@link #matchAny}, but binds the matching formula to a variable.
   */
  public static SmtAstPattern matchAnyWithAnyArgsBind(String pBindMatchTo) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>of(pBindMatchTo),
        dontcare());
  }

  /**
   * Matches only if NONE of the patterns matches on the arguments of the specific function application.
   *
   * @param pFunction
   * @param argumentMatchers
   * @return
   */
  public static SmtAstPattern matchIfNot(Comparable<?> pFunction, SmtAstPattern... argumentMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pFunction),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        none(argumentMatchers));
  }

  /**
   * Matches only if NONE of the patterns matches an arbitrary function application.
   *
   * @param quantorBodyMatchers
   * @return
   */
  public static SmtAstPattern matchIfNot(SmtAstPattern... pMatchers) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        none(pMatchers));
  }

  public static SmtAstPattern matchNullaryBind(String pBindMatchTo) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>of(pBindMatchTo),
        and());
  }

  public static SmtAstPattern matchNumeralExpressionBind(String pBindMatchTo) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>of(new SmtFormulaMatcher() {
          @Override
          public boolean formulaMatches(FormulaManager pMgr, Formula pF) {
            return pMgr.getFormulaType(pF) instanceof NumeralType;
          }
        }),
        Optional.<String>of(pBindMatchTo),
        dontcare());
  }

  public static SmtAstPattern matchNumeralVariableBind(String pBindMatchTo) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>of(new SmtFormulaMatcher() {
          @Override
          public boolean formulaMatches(FormulaManager pMgr, Formula pF) {
            return pMgr.getUnsafeFormulaManager().isVariable(pF)
                && pMgr.getFormulaType(pF) instanceof NumeralType;
          }
        }),
        Optional.<String>of(pBindMatchTo),
        and());
  }

  public static SmtAstPattern matchFreeVariableBind(String pBindMatchTo) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>of(new SmtFormulaMatcher() {
          @Override
          public boolean formulaMatches(FormulaManager pMgr, Formula pF) {
            return pMgr.getUnsafeFormulaManager().isFreeVariable(pF);
          }
        }),
        Optional.<String>of(pBindMatchTo),
        and());
  }

  public static SmtAstPattern matchNullaryBind(Comparable<?> pSymbol, String pBindMatchTo) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pSymbol),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>of(pBindMatchTo),
        and());
  }

  public static SmtAstPattern matchNullary(Comparable<?> pSymbol) {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>of(pSymbol),
        Optional.<SmtFormulaMatcher>absent(),
        Optional.<String>absent(),
        and());
  }

  public static SmtAstPattern matchNotLiteral() {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>of(new SmtFormulaMatcher() {
          @Override
          public boolean formulaMatches(FormulaManager pMgr, Formula pF) {
            // TODO: Switch from FormulaManager to FormulaManagerView
            //return !pMgr.getUnsafeFormulaManager().isLiteral(pF);
            throw new UnsupportedOperationException();
          }
        }),
        Optional.<String>absent(),
        and());
  }

  public static SmtAstPattern matchNegativeNumber() {
    return new SmtFunctionApplicationPattern(
        Optional.<Comparable<?>>absent(),
        Optional.<SmtFormulaMatcher>of(new SmtFormulaMatcher() {
          @Override
          public boolean formulaMatches(FormulaManager pMgr, Formula pF) {
            if (!(pF instanceof NumeralFormula)) {
              return false;
            }

            IntegerFormula minusOne = pMgr.getIntegerFormulaManager().makeNumber(-1);
            if (pF.equals(minusOne)) {
              return true;
            }

            try (ProverEnvironment prover = pMgr.newProverEnvironment(false, false)) {
              BooleanFormula largerEqualZero = pMgr.getIntegerFormulaManager()
                  .greaterOrEquals((IntegerFormula) pF, pMgr.getIntegerFormulaManager().makeNumber(0));
              prover.push(largerEqualZero);
              return prover.isUnsat();

            } catch (SolverException | InterruptedException e) {
              return false;
            }
          }
        }),
        Optional.<String>absent(),
        and());
  }


  public static SmtAstPatternSelection or(SmtAstPatternSelectionElement... pDisjuncts) {
    return new SmtAstPatternSelectionImpl(
        LogicalConnection.OR,
        Arrays.asList(pDisjuncts),
        Collections.<String,Formula>emptyMap());
  }

  public static SmtAstPatternSelection and(SmtAstPatternSelectionElement... pDisjuncts) {
    return new SmtAstPatternSelectionImpl(
        LogicalConnection.AND,
        Arrays.asList(pDisjuncts),
        Collections.<String,Formula>emptyMap());
  }

  public static SmtAstPatternSelection dontcare() {
    return new SmtAstPatternSelectionImpl(
        LogicalConnection.DONTCARE,
        Collections.<SmtAstPatternSelectionElement>emptyList(),
        Collections.<String,Formula>emptyMap());
  }


  public static SmtAstPatternSelection none(SmtAstPatternSelectionElement... pDisjuncts) {
    return new SmtAstPatternSelectionImpl(
        LogicalConnection.NONE,
        Arrays.asList(pDisjuncts),
        Collections.<String,Formula>emptyMap());
  }

  public static SmtAstPatternSelection withDefaultBinding(String pVariableName, Formula pDefaultBinding, SmtAstPatternSelection pSelection) {
    Map<String,Formula> defaultBindings = Maps.newHashMap(pSelection.getDefaultBindings());
    defaultBindings.put(pVariableName, pDefaultBinding);

    return new SmtAstPatternSelectionImpl(
        pSelection.getRelationship(),
        pSelection.getPatterns(),
        defaultBindings);
  }

  public static SmtAstPatternSelection concat(SmtAstPatternSelection... pSelections) {

    Map<String,Formula> defaultBindings = Maps.newHashMap();
    List<SmtAstPatternSelectionElement> patterns = Lists.newArrayList();

    LogicalConnection logicRelation = null;

    for (SmtAstPatternSelection sel: pSelections) {
      if (logicRelation == null) {
        logicRelation = sel.getRelationship();
      }

      Verify.verify(logicRelation == sel.getRelationship(), "Logic relations must match!");

      for (SmtAstPatternSelectionElement p: sel.getPatterns()) {
        patterns.add(p);
      }
      defaultBindings.putAll(sel.getDefaultBindings());
    }

    return new SmtAstPatternSelectionImpl(
        logicRelation,
        patterns,
        defaultBindings);
  }

  public static SmtAstPattern matchExistsQuant(SmtAstPatternSelection pBodyMatchers) {
    return new SmtQuantificationPattern(
        Optional.of(QuantifierType.EXISTS),
        Optional.<String>absent(),
        pBodyMatchers);
  }

  public static SmtAstPattern matchExistsQuantBind(String pBindMatchTo, SmtAstPatternSelection pBodyMatchers) {
    return new SmtQuantificationPattern(
        Optional.of(QuantifierType.EXISTS),
        Optional.<String>of(pBindMatchTo),
        pBodyMatchers);
  }

  public static SmtAstPattern matchForallQuant(SmtAstPatternSelection pBodyMatchers) {
    return new SmtQuantificationPattern(
        Optional.of(QuantifierType.FORALL),
        Optional.<String>absent(),
        pBodyMatchers);
  }

  public static SmtAstPattern matchForallQuantBind(String pBindMatchTo, SmtAstPatternSelection pBodyMatchers) {
    return new SmtQuantificationPattern(
        Optional.of(QuantifierType.FORALL),
        Optional.<String>of(pBindMatchTo),
        pBodyMatchers);
  }

  public static SmtAstPattern matchInSubtree(SmtAstPattern pPattern) {
    if (pPattern instanceof SmtFunctionApplicationPattern) {
      SmtFunctionApplicationPattern ap = (SmtFunctionApplicationPattern) pPattern;

      Set<SmtAstMatchFlag> newFlags = Sets.newHashSet(ap.flags);
      newFlags.add(SmtAstMatchFlag.IN_SUBTREE_RECURSIVE);

      final SmtFunctionApplicationPattern patternWithRecursionFlag = new SmtFunctionApplicationPattern(
          ap.function,
          ap.customFormulaMatcher,
          ap.bindMatchTo,
          ap.argumentPatterns,
          newFlags.toArray(new SmtAstMatchFlag[newFlags.size()]));

      return patternWithRecursionFlag;
    }

    throw new UnsupportedOperationException("Subtree matching not (yet) available for the requested pattern class!");
  }

  public static SmtAstPatternSelection matchInSubtreeBoundedDepth(int pMaxDepth, SmtAstPattern pPattern) {
    if (pMaxDepth <= 0) {
      return and();
    }

    return or(
        pPattern,
        matchAnyWithArgs(
            matchInSubtreeBoundedDepth(pMaxDepth-1, pPattern)
            )
        );
  }

  public static String quantified(String pVariableName) {
    return "." + pVariableName;
  }

  public static String parentOf(String pVariableName) {
    return pVariableName + ".parent";
  }

}

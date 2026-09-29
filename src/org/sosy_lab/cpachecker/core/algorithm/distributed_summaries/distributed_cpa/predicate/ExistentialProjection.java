// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FunctionDeclaration;
import org.sosy_lab.java_smt.api.SolverException;
import org.sosy_lab.java_smt.api.visitors.DefaultFormulaVisitor;

/**
 * Removes existentially quantified variables from a formula without changing its meaning.
 *
 * <p>The formula is read as {@code ∃ E. c_1 ∧ ... ∧ c_n}, where {@code E} are the variables the
 * caller declares existential and the {@code c_i} are the top-level conjuncts. Two rules are
 * applied, both of which are equivalences:
 *
 * <ol>
 *   <li><b>One-point rule:</b> {@code ∃ v. v = t ∧ R} is {@code R[v := t]} if {@code v} does not
 *       occur in {@code t}. A conjunct that is an existential Boolean variable (or its negation)
 *       counts as the definition {@code v = true} ({@code v = false}). In the default mode, the
 *       rule is not applied to variables that occur in a disjunctive conjunct: substituting into it
 *       would copy it once per disjunct of the enclosing formula instead of sharing it.
 *   <li><b>Independent parts:</b> the conjuncts are grouped into components that share existential
 *       variables. A component without any other variable is independent of the rest, so it can be
 *       replaced by {@code true} if it is satisfiable. All such components are checked with a
 *       single query, because they share no variable with each other either.
 * </ol>
 *
 * <p>Existential quantification distributes over disjunction, so a top-level disjunction is
 * projected disjunct by disjunct, and disjuncts that become equal are kept once. This matters for
 * violation conditions, which are disjunctions of paths: to align the SSA indices of the paths,
 * {@code makeOr} adds equalities between intermediate variables that the one-point rule then
 * removes again. The default mode leaves nested disjunctions shared. The bounded nested mode also
 * substitutes into them and projects their private existential variables, which is useful for
 * conditions constructed directly from a shared ARG graph. Quantifiers of variables shared with
 * other conjuncts stay outside, preserving correlations.
 *
 * <p>Existential variables that no rule removes stay in the formula, where they remain implicitly
 * existential. Conjuncts and disjuncts are sorted, so that equal sets of them yield equal formulas.
 *
 * <p>The one-point rule only recognizes an equality whose one side is the variable itself. That is
 * how path formulas look with the bitvector encoding DSS uses. Some solvers normalize linear
 * integer equalities, e.g., MathSAT turns {@code v = y + 1} into a sum, which this class does not
 * solve for {@code v}; the result is then still equivalent, just less small.
 */
final class ExistentialProjection {

  /** A definition {@code variable = term} found among the conjuncts. */
  private record Definition(
      int conjunct, String name, Formula variable, Formula term, Set<String> termVariables) {}

  private record Conjunct(BooleanFormula formula, Set<String> variables, boolean isDisjunction) {}

  private final FormulaManagerView fmgr;
  private final BooleanFormulaManagerView bfmgr;
  private final Solver solver;
  private final boolean nested;

  ExistentialProjection(Solver pSolver) {
    this(pSolver, false);
  }

  ExistentialProjection(Solver pSolver, boolean pNested) {
    nested = pNested;
    solver = pSolver;
    fmgr = pSolver.getFormulaManager();
    bfmgr = fmgr.getBooleanFormulaManager();
  }

  /**
   * Projects the local variables out of a violation condition, i.e., all variables except the ones
   * at their latest SSA index, see {@link PredicateOperatorUtil#isLocalVariable}. The SSA map stays
   * as it is, so the interface of the condition does not change.
   */
  PathFormula projectLocalVariables(PathFormula pCondition)
      throws InterruptedException, SolverException {
    SSAMap ssa = pCondition.getSsa();
    return pCondition.withFormula(
        project(
            pCondition.getFormula(),
            name -> PredicateOperatorUtil.isLocalVariable(name, ssa, fmgr)));
  }

  /**
   * Projects the existential variables out of the given formula as far as the rules allow.
   *
   * @param pFormula the formula to project
   * @param pIsExistential decides by name which variables are existentially quantified
   * @return a formula equivalent to {@code pFormula} under the existential reading; a part that
   *     turns out to be unsatisfiable is retained in default mode and replaced by false in nested
   *     mode
   */
  BooleanFormula project(BooleanFormula pFormula, Predicate<String> pIsExistential)
      throws InterruptedException, SolverException {
    return project(pFormula, pIsExistential, new int[] {2000});
  }

  private BooleanFormula project(
      BooleanFormula pFormula, Predicate<String> pIsExistential, int[] budget)
      throws InterruptedException, SolverException {
    if (--budget[0] < 0) {
      return pFormula;
    }
    Set<BooleanFormula> disjuncts = bfmgr.toDisjunctionArgs(pFormula, true);
    if (disjuncts.size() <= 1) {
      return projectConjunction(pFormula, pIsExistential, budget);
    }
    return projectDisjunction(disjuncts, pIsExistential, budget);
  }

  private BooleanFormula projectDisjunction(
      Set<BooleanFormula> pDisjuncts, Predicate<String> pIsExistential, int[] budget)
      throws InterruptedException, SolverException {
    Set<BooleanFormula> projected = new LinkedHashSet<>();
    for (BooleanFormula disjunct : pDisjuncts) {
      BooleanFormula projectedDisjunct = projectConjunction(disjunct, pIsExistential, budget);
      if (bfmgr.isTrue(projectedDisjunct)) {
        return projectedDisjunct;
      }
      if (!bfmgr.isFalse(projectedDisjunct)) {
        projected.add(projectedDisjunct);
      }
    }
    return bfmgr.or(sorted(projected));
  }

  private BooleanFormula projectConjunction(
      BooleanFormula pFormula, Predicate<String> pIsExistential, int[] budget)
      throws InterruptedException, SolverException {
    List<Conjunct> conjuncts = new ArrayList<>();
    if (!addConjuncts(pFormula, conjuncts)) {
      return pFormula;
    }

    for (List<Conjunct> substituted = eliminateDefinitions(conjuncts, pIsExistential);
        substituted != conjuncts;
        substituted = eliminateDefinitions(conjuncts, pIsExistential)) {
      if (substituted == null) {
        return nested ? bfmgr.makeFalse() : pFormula;
      }
      conjuncts = substituted;
    }

    ImmutableList<Conjunct> independent = independentConjuncts(conjuncts, pIsExistential);
    if (!independent.isEmpty()) {
      if (solver.isUnsat(bfmgr.and(Lists.transform(independent, Conjunct::formula)))) {
        return nested ? bfmgr.makeFalse() : pFormula;
      }
      conjuncts.removeAll(independent);
    }

    BooleanFormula result = bfmgr.and(sorted(Lists.transform(conjuncts, Conjunct::formula)));
    if (nested && budget[0] > 0) {
      if (!result.equals(pFormula)) {
        return project(result, pIsExistential, budget);
      }
      Map<String, Integer> occurrences = new HashMap<>();
      for (Conjunct conjunct : conjuncts) {
        conjunct.variables().forEach(name -> occurrences.merge(name, 1, Integer::sum));
      }
      List<BooleanFormula> smaller = new ArrayList<>();
      for (Conjunct conjunct : conjuncts) {
        // Only move an existential quantifier into a conjunct when its variable occurs nowhere
        // else.
        smaller.add(
            conjunct.isDisjunction()
                ? project(
                    conjunct.formula(),
                    name -> pIsExistential.test(name) && occurrences.getOrDefault(name, 0) == 1,
                    budget)
                : conjunct.formula());
      }
      BooleanFormula projected = bfmgr.and(sorted(smaller));
      if (!projected.equals(result)) {
        return project(projected, pIsExistential, budget);
      }
    }
    return result;
  }

  /**
   * Sorts formulas by their hash code, which is cheap and, within one solver context, the same for
   * equal formulas. Sorting by the representation would be canonical across contexts, too, but
   * printing large formulas costs more than everything else this class does.
   */
  private static ImmutableList<BooleanFormula> sorted(Collection<BooleanFormula> pFormulas) {
    return ImmutableList.sortedCopyOf(Comparator.comparingInt(BooleanFormula::hashCode), pFormulas);
  }

  /**
   * Adds the flattened conjuncts of {@code pFormula} to {@code pConjuncts}, skipping {@code true}.
   *
   * @return {@code false} if one of the conjuncts is {@code false}
   */
  private boolean addConjuncts(BooleanFormula pFormula, List<Conjunct> pConjuncts) {
    for (BooleanFormula conjunct : bfmgr.toConjunctionArgs(pFormula, true)) {
      if (bfmgr.isFalse(conjunct)) {
        return false;
      }
      if (!bfmgr.isTrue(conjunct)) {
        pConjuncts.add(
            new Conjunct(
                conjunct,
                ImmutableSet.copyOf(fmgr.extractVariableNames(conjunct)),
                bfmgr.toDisjunctionArgs(conjunct, false).size() > 1));
      }
    }
    return true;
  }

  /**
   * Applies the one-point rule to all definitions among the conjuncts at once.
   *
   * <p>Removing one definition at a time and substituting it into every other conjunct costs a
   * quadratic number of substitutions along the chains of definitions a path formula consists of.
   * Instead, the first definition of every variable is collected, the definitions are resolved in
   * the order of their dependencies, and the remaining conjuncts are substituted once. A definition
   * that would close a cycle is not used; its conjunct stays as a constraint.
   *
   * @return the conjuncts after substitution, {@code pConjuncts} itself if no definition could be
   *     used, or {@code null} if a conjunct became {@code false}
   */
  private @Nullable List<Conjunct> eliminateDefinitions(
      List<Conjunct> pConjuncts, Predicate<String> pIsExistential) throws InterruptedException {
    // Substituting into a disjunctive conjunct would copy it: in a violation condition, such a
    // conjunct is the condition of the successor, which every path shares through the equalities
    // that connect it to the path. Variables occurring in one therefore stay defined by equalities.
    Set<String> inDisjunctions = new HashSet<>();
    for (Conjunct conjunct : pConjuncts) {
      if (conjunct.isDisjunction()) {
        inDisjunctions.addAll(conjunct.variables());
      }
    }
    Predicate<String> isEliminable =
        name -> pIsExistential.test(name) && (nested || !inDisjunctions.contains(name));

    Map<String, Definition> definitions = new LinkedHashMap<>();
    for (int i = 0; i < pConjuncts.size(); i++) {
      Conjunct conjunct = pConjuncts.get(i);
      if (conjunct.variables().stream().noneMatch(isEliminable)) {
        continue;
      }
      Definition definition =
          fmgr.visit(conjunct.formula(), new DefinitionExtractor(i, isEliminable));
      if (definition != null) {
        definitions.putIfAbsent(definition.name(), definition);
      }
    }
    if (definitions.isEmpty()) {
      return pConjuncts;
    }

    // resolve every definition after the definitions its term depends on
    Map<Formula, Formula> resolved = new LinkedHashMap<>();
    Set<String> resolvedNames = new HashSet<>();
    Set<Integer> usedConjuncts = new HashSet<>();
    for (String name : dependencyOrder(definitions)) {
      Definition definition = definitions.get(name);
      Formula term = definition.term();
      Map<Formula, Formula> dependencies = new HashMap<>();
      for (String dependency : definition.termVariables()) {
        if (resolvedNames.contains(dependency)) {
          Definition other = definitions.get(dependency);
          dependencies.put(other.variable(), resolved.get(other.variable()));
        }
      }
      if (!dependencies.isEmpty()) {
        // FormulaManagerView substitutes only into Boolean formulas, so substitute into the
        // defining equality and read the resolved term back from it
        term =
            otherSide(
                fmgr.substitute(pConjuncts.get(definition.conjunct()).formula(), dependencies),
                definition.variable());
        if (term == null) {
          // the solver rewrote the equality; keep it as a constraint
          continue;
        }
      }
      resolved.put(definition.variable(), term);
      resolvedNames.add(name);
      usedConjuncts.add(definition.conjunct());
    }
    if (resolvedNames.isEmpty()) {
      // every definition closes a cycle or could not be resolved: there is nothing to substitute
      return pConjuncts;
    }

    List<Conjunct> result = new ArrayList<>(pConjuncts.size());
    List<BooleanFormula> affected = new ArrayList<>();
    for (int i = 0; i < pConjuncts.size(); i++) {
      Conjunct conjunct = pConjuncts.get(i);
      if (usedConjuncts.contains(i)) {
        continue;
      }
      if (conjunct.variables().stream().anyMatch(resolvedNames::contains)) {
        affected.add(conjunct.formula());
      } else {
        result.add(conjunct);
      }
    }
    if (!affected.isEmpty()
        && !addConjuncts(fmgr.substitute(bfmgr.and(affected), resolved), result)) {
      return null;
    }
    return result;
  }

  /** The other side of an equality {@code pVariable = t} or {@code t = pVariable}, if it is one. */
  private @Nullable Formula otherSide(BooleanFormula pEquality, Formula pVariable) {
    return fmgr.visit(
        pEquality,
        new DefaultFormulaVisitor<@Nullable Formula>() {
          @Override
          protected @Nullable Formula visitDefault(Formula pF) {
            return null;
          }

          @Override
          public @Nullable Formula visitFunction(
              Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
            return switch (pFunctionDeclaration.getKind()) {
              case EQ, BV_EQ, IFF -> {
                if (pArgs.size() != 2) {
                  yield null;
                }
                if (pArgs.get(0).equals(pVariable)) {
                  yield pArgs.get(1);
                }
                yield pArgs.get(1).equals(pVariable) ? pArgs.get(0) : null;
              }
              default -> null;
            };
          }
        });
  }

  /**
   * The defined variables in an order in which every definition comes after the definitions its
   * term depends on. A definition that depends on itself, directly or through others, is left out.
   */
  private static ImmutableList<String> dependencyOrder(Map<String, Definition> pDefinitions) {
    ImmutableList.Builder<String> order = ImmutableList.builder();
    Set<String> finished = new HashSet<>();
    Set<String> onStack = new HashSet<>();
    Set<String> excluded = new HashSet<>();
    // iterative depth-first search: path formulas can have long chains of definitions
    Deque<Iterator<String>> stack = new ArrayDeque<>();
    Deque<String> names = new ArrayDeque<>();
    for (String root : pDefinitions.keySet()) {
      if (finished.contains(root)) {
        continue;
      }
      names.push(root);
      onStack.add(root);
      stack.push(pDefinitions.get(root).termVariables().iterator());
      while (!stack.isEmpty()) {
        Iterator<String> dependencies = stack.peek();
        String current = names.peek();
        if (dependencies.hasNext()) {
          String dependency = dependencies.next();
          if (!pDefinitions.containsKey(dependency) || finished.contains(dependency)) {
            continue;
          }
          if (onStack.contains(dependency)) {
            // a cycle: the current definition cannot be resolved
            excluded.add(current);
            continue;
          }
          names.push(dependency);
          onStack.add(dependency);
          stack.push(pDefinitions.get(dependency).termVariables().iterator());
        } else {
          stack.pop();
          names.pop();
          onStack.remove(current);
          finished.add(current);
          if (!excluded.contains(current)) {
            order.add(current);
          }
        }
      }
    }
    return order.build();
  }

  /**
   * The conjuncts of all components that contain existential variables only, where two conjuncts
   * are in the same component if they share an existential variable.
   */
  private static ImmutableList<Conjunct> independentConjuncts(
      List<Conjunct> pConjuncts, Predicate<String> pIsExistential) {
    int[] parent = new int[pConjuncts.size()];
    for (int i = 0; i < parent.length; i++) {
      parent[i] = i;
    }
    Map<String, Integer> firstOccurrence = new HashMap<>();
    for (int i = 0; i < pConjuncts.size(); i++) {
      for (String variable : pConjuncts.get(i).variables()) {
        if (pIsExistential.test(variable)) {
          Integer other = firstOccurrence.putIfAbsent(variable, i);
          if (other != null) {
            parent[find(parent, i)] = find(parent, other);
          }
        }
      }
    }
    boolean[] dependent = new boolean[pConjuncts.size()];
    for (int i = 0; i < pConjuncts.size(); i++) {
      if (!pConjuncts.get(i).variables().stream().allMatch(pIsExistential)) {
        dependent[find(parent, i)] = true;
      }
    }
    ImmutableList.Builder<Conjunct> independent = ImmutableList.builder();
    for (int i = 0; i < pConjuncts.size(); i++) {
      if (!dependent[find(parent, i)]) {
        independent.add(pConjuncts.get(i));
      }
    }
    return independent.build();
  }

  private static int find(int[] pParent, int pElement) {
    int root = pElement;
    while (pParent[root] != root) {
      root = pParent[root];
    }
    // path compression
    for (int current = pElement; pParent[current] != root; ) {
      int next = pParent[current];
      pParent[current] = root;
      current = next;
    }
    return root;
  }

  /** Recognizes a conjunct that defines an existential variable. */
  private final class DefinitionExtractor extends DefaultFormulaVisitor<@Nullable Definition> {

    private final int conjunct;
    private final Predicate<String> isExistential;

    private DefinitionExtractor(int pConjunct, Predicate<String> pIsExistential) {
      conjunct = pConjunct;
      isExistential = pIsExistential;
    }

    @Override
    protected @Nullable Definition visitDefault(Formula pF) {
      return null;
    }

    @Override
    public @Nullable Definition visitFreeVariable(Formula pF, String pName) {
      // a conjunct that is a Boolean variable defines it as true
      return isExistential.test(pName)
          ? new Definition(conjunct, pName, pF, bfmgr.makeTrue(), ImmutableSet.of())
          : null;
    }

    @Override
    public @Nullable Definition visitFunction(
        Formula pF, List<Formula> pArgs, FunctionDeclaration<?> pFunctionDeclaration) {
      return switch (pFunctionDeclaration.getKind()) {
        // FP_EQ is deliberately missing: it is not substitutive because of NaN and signed zeros
        case EQ, BV_EQ, IFF -> {
          if (pArgs.size() != 2) {
            yield null;
          }
          Definition definition = definitionOf(pArgs.get(0), pArgs.get(1));
          yield definition != null ? definition : definitionOf(pArgs.get(1), pArgs.get(0));
        }
        case NOT -> {
          String name = variableName(pArgs.getFirst());
          yield name != null && isExistential.test(name)
              ? new Definition(
                  conjunct, name, pArgs.getFirst(), bfmgr.makeFalse(), ImmutableSet.of())
              : null;
        }
        default -> null;
      };
    }

    private @Nullable Definition definitionOf(Formula pVariable, Formula pTerm) {
      String name = variableName(pVariable);
      if (name == null || !isExistential.test(name)) {
        return null;
      }
      Set<String> termVariables = ImmutableSet.copyOf(fmgr.extractVariableNames(pTerm));
      if (termVariables.contains(name)) {
        return null;
      }
      return new Definition(conjunct, name, pVariable, pTerm, termVariables);
    }

    private @Nullable String variableName(Formula pFormula) {
      return fmgr.visit(
          pFormula,
          new DefaultFormulaVisitor<@Nullable String>() {
            @Override
            protected @Nullable String visitDefault(Formula pF) {
              return null;
            }

            @Override
            public String visitFreeVariable(Formula pF, String pName) {
              return pName;
            }
          });
    }
  }
}

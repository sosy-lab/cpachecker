// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static com.google.common.base.Preconditions.checkArgument;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AbstractSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.ast.AstCfaRelation;

/**
 * The variables of the input program that the scopes of a precision witness can name, by their
 * names in the input program. Such a name denotes the same variable everywhere in its scope.
 */
public final class PrecisionScopeVariables {

  private final AstCfaRelation astCfaRelation;
  private final Supplier<ImmutableMap<String, AbstractSimpleDeclaration>> globalVariables;
  private final Supplier<ImmutableSetMultimap<String, AbstractSimpleDeclaration>>
      declaredVariablesPerFunction;

  /** Requires the relation of the CFA to the AST of the program, i.e., a program in C. */
  public PrecisionScopeVariables(CFA pCfa) {
    checkArgument(pCfa.getAstCfaRelation() != null, "No relation of the CFA to the AST");
    astCfaRelation = pCfa.getAstCfaRelation();
    globalVariables =
        Suppliers.memoize(() ->
            byUniqueName(
                FluentIterable.from(astCfaRelation.getGlobalVariables().orElseThrow())
                    // A static local variable or a static one of a file has another name
                    .filter(variable -> variable.getName().equals(variable.getOrigName()))));
    declaredVariablesPerFunction =
        Suppliers.memoize(() -> declaredVariablesPerFunction(pCfa, astCfaRelation));
  }

  private static ImmutableSetMultimap<String, AbstractSimpleDeclaration>
      declaredVariablesPerFunction(CFA pCfa, AstCfaRelation pAstCfaRelation) {
    ImmutableSetMultimap.Builder<String, AbstractSimpleDeclaration> variables =
        ImmutableSetMultimap.builder();
    for (CFANode node : pCfa.nodes()) {
      variables.putAll(
          node.getFunctionName(), pAstCfaRelation.getDeclaredLocalVariablesByCfaNode(node));
      variables.putAll(
          node.getFunctionName(), pAstCfaRelation.getDeclaredParametersByCfaNode(node));
    }
    return variables.build();
  }

  /**
   * Maps the names in the input program to the variables, but leaves out a name of several
   * variables and the variables CPAchecker introduced.
   */
  private static ImmutableMap<String, AbstractSimpleDeclaration> byUniqueName(
      FluentIterable<? extends AbstractSimpleDeclaration> pVariables) {
    ImmutableListMultimap<String, AbstractSimpleDeclaration> variablesByName =
        Multimaps.index(
            pVariables
                .filter(variable -> !variable.getName().contains("__CPAchecker_"))
                // The same variable can be declared by several objects
                .stream()
                .collect(
                    ImmutableMap.toImmutableMap(
                        AbstractSimpleDeclaration::getQualifiedName,
                        variable -> (AbstractSimpleDeclaration) variable,
                        (first, _) -> first))
                .values(),
            AbstractSimpleDeclaration::getOrigName);
    return FluentIterable.from(variablesByName.keySet())
        .filter(name -> variablesByName.get(name).size() == 1)
        .toMap(name -> variablesByName.get(name).getFirst());
  }

  /** The global variables, which the global scope names. */
  public ImmutableMap<String, AbstractSimpleDeclaration> globalVariables() {
    return globalVariables.get();
  }

  /**
   * The variables the scope of the given function names: its parameters and local variables, if no
   * other one of them has the same name, and the global variables none of them hides.
   */
  public ImmutableMap<String, AbstractSimpleDeclaration> variablesIn(String pFunction) {
    Set<AbstractSimpleDeclaration> declared = declaredVariablesPerFunction.get().get(pFunction);
    ImmutableSet<String> declaredNames =
        transformedImmutableSetCopy(declared, AbstractSimpleDeclaration::getOrigName);
    return ImmutableMap.<String, AbstractSimpleDeclaration>builder()
        .putAll(byUniqueName(FluentIterable.from(declared)))
        .putAll(Maps.filterKeys(globalVariables(), name -> !declaredNames.contains(name)))
        .buildOrThrow();
  }

  /**
   * The variables in scope at the given node, which the scope of its location names, or nothing if
   * they are unknown.
   */
  public Optional<ImmutableMap<String, AbstractSimpleDeclaration>> variablesAt(CFANode pNode) {
    ImmutableSet<String> declaredInFunction =
        transformedImmutableSetCopy(
            declaredVariablesPerFunction.get().get(pNode.getFunctionName()),
            AbstractSimpleDeclaration::getQualifiedName);
    return astCfaRelation
        .getVariablesAndParametersInScope(pNode)
        .map(variables ->
            byUniqueName(
                // The global variables of the relation include the static local variables of all
                // functions, which CPAchecker renamed
                variables.filter(variable ->
                    variable.getName().equals(variable.getOrigName())
                        || declaredInFunction.contains(variable.getQualifiedName()))));
  }
}

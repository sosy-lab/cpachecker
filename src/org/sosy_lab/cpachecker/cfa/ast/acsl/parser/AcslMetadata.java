// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopAnnotation;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This record represents the Acsl Declarations and Acsl Annotations that are associated with a Cfa.
 *
 * <p>An Acsl Annotation can be: - A single assertion (§2.4.1 of the Acsl Standard v. 1.23) - A loop
 * annotation consisting of loop invariants and loop assigns (§ 2.4.2 of the Acsl Standard v. 1.23)
 * - A function contract consisting of ensures, assigns and requires clauses (§ 2.3 of the Acsl
 * Standard v.1.23)
 *
 * @param globalAcslDeclarations A set of global Acsl Declarations
 * @param assertions A mapping of Cfa Nodes to Acsl assertion_clauses
 * @param loopAnnotations A mapping of Cfa Nodes to Acsl loop_invariants
 * @param functionContracts A mapping of Cfa Nodes to Acsl function_contracts. "A C function can be
 *     defined only once but declared several times. It is allowed to annotate each of these
 *     declarations with contracts. Those contracts are seen as a single contract with the union of
 *     the requires clauses and behaviors." (see ANSI/ISO C Specification Language Version 1.23
 *     §2.3.5)
 */
public record AcslMetadata(
    ImmutableSet<AcslDeclaration> globalAcslDeclarations,
    ImmutableSetMultimap<CFANode, AcslAssertion> assertions,
    ImmutableSetMultimap<CFANode, AcslLoopAnnotation> loopAnnotations,
    ImmutableSetMultimap<CFANode, AcslFunctionContract> functionContracts) {

  public static AcslMetadata empty() {
    return new AcslMetadata(
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of());
  }

  public int numOfAnnotaniots() {
    return assertions.size() + loopAnnotations.size() + functionContracts.size();
  }
}

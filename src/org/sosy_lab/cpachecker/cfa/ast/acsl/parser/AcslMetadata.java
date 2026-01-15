// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AAcslAnnotation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssertion;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssigns;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

/**
 * This record represents the Acsl Declarations and Acsl Annotations that are associated with a Cfa.
 *
 * @param globalAcslDeclarations A set of global Acsl Declarations
 * @param assertions A mapping of Cfa Nodes to Acsl assertion_clauses
 * @param invariants A mapping of Cfa Nodes to Acsl loop_invariants
 * @param functionContracts A mapping of Cfa Nodes to Acsl function_contracts
 * @param modifiedMemoryLocations A mapping of Cfa Nodes to Acsl assigns_clauses
 */
public record AcslMetadata(
    ImmutableList<AcslComment> pAcslComments,
    ImmutableList<AAcslAnnotation> genericAnnotations,
    ImmutableSet<AcslDeclaration> globalAcslDeclarations,
    ImmutableSetMultimap<CFANode, AcslAssertion> assertions,
    ImmutableSetMultimap<CFANode, AcslLoopInvariant> invariants,
    ImmutableSetMultimap<CFANode, AcslFunctionContract> functionContracts,
    ImmutableSetMultimap<CFANode, AcslAssigns> modifiedMemoryLocations) {

  public static AcslMetadata empty() {
    return new AcslMetadata(
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of());
  }

  public static AcslMetadata withGenericAnnotations(List<AAcslAnnotation> pAnnotations) {
    return new AcslMetadata(
        ImmutableList.of(),
        ImmutableList.copyOf(pAnnotations),
        ImmutableSet.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of(),
        ImmutableSetMultimap.of());
  }
}

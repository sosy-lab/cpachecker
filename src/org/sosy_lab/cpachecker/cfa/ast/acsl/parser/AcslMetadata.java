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
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslAssigns;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslFunctionContract;
import org.sosy_lab.cpachecker.cfa.ast.acsl.annotations.AcslLoopInvariant;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record AcslMetadata(
    ImmutableSet<AcslDeclaration> globalAcslDeclarations,
    ImmutableSetMultimap<CFANode, AcslAssertion> assertions,
    ImmutableSetMultimap<CFANode, AcslLoopInvariant> invariants,
    ImmutableSetMultimap<CFANode, AcslFunctionContract> functionContracts,
    ImmutableSetMultimap<CFANode, AcslAssigns> modifiedMemoryLocations) {}

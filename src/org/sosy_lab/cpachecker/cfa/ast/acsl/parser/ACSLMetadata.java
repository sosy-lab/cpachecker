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
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslPredicate;
import org.sosy_lab.cpachecker.cfa.model.CFANode;

public record ACSLMetadata(
    ImmutableSet<AcslDeclaration> globalAcslDeclarations,
    ImmutableSetMultimap<CFANode, AcslPredicate> assertions,
    ImmutableSetMultimap<CFANode, AcslPredicate> invariants,
    ImmutableSetMultimap<CFANode, AcslPredicate> functionContracts,
    ImmutableSetMultimap<CFANode, AcslPredicate> modifiedMemoryLocations) {}

// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

/**
 * Sealed interface for nodes in a parsed DSL pattern tree. A {@link DelegatingRefinerPatternNode}
 * can either be a wildcard leaf {@link DelegatingRefinerDelegatingRefinerPatternAtom} or a compound
 * node {@link DelegatingRefinerPatternOperator}.Produced by the {@link DelegatingRefinerParser} and
 * consumed by the {@link DelegatingRefinerMatchingVisitor}.
 */
public sealed interface DelegatingRefinerPatternNode
    permits DelegatingRefinerDelegatingRefinerPatternAtom, DelegatingRefinerPatternOperator {}

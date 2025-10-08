// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

/**
 * Leaf node in a parsed DSL pattern tree representing a wildcard placeholder. Produced by the
 * {@link DelegatingRefinerParser} for tokens such as {@code <var>} in the {@code patternMatch}
 * string of a {@link DelegatingRefinerPatternRule}. Consumed by the {@link
 * DelegatingRefinerMatchingVisitor} during recursive matching.
 */
public record DelegatingRefinerDelegatingRefinerPatternAtom(String name)
    implements DelegatingRefinerPatternNode {}

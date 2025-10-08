// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.delegatingRefinerUtils;

import com.google.common.collect.ImmutableList;

/**
 * Compound node in a parsed DSL pattern tree. Represents an operator and its child nodes. A DSL
 * String such as {@code (= <var> <term>)} is represented by a {@link
 * DelegatingRefinerPatternOperator} with the operator {@code "="} and two children of the type
 * {@link DelegatingRefinerPatternNode}. Produced by the {@link DelegatingRefinerParser} and
 * consumed by the {@link DelegatingRefinerMatchingVisitor} during recursive matching.
 */
public record DelegatingRefinerPatternOperator(
    String operator, ImmutableList<DelegatingRefinerPatternNode> sExpressionList)
    implements DelegatingRefinerPatternNode {}

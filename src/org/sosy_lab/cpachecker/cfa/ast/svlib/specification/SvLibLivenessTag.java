// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

/** A liveness property of a loop, which no analysis of CPAchecker checks yet. */
public sealed interface SvLibLivenessTag extends SvLibTagProperty
    permits SvLibNotRecurringTag, SvLibRecurringTag {}

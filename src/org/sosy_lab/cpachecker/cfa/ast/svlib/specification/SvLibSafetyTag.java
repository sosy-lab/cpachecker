// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

/** A safety property, which a single state can violate. */
public sealed interface SvLibSafetyTag extends SvLibTagProperty
    permits SvLibCheckTrueTag, SvLibEnsuresTag, SvLibInvariantTag, SvLibRequiresTag {}

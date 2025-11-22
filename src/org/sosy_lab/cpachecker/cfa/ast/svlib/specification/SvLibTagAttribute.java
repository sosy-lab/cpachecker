// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNode;

/**
 * A tag attribute in an SV-LIB specification, which can either be a property (like `:assert`,
 * `:invariant`, etc.) or a named reference which can then be used to annotate the statement tagged
 * with that reference with further attributes.
 */
public sealed interface SvLibTagAttribute extends SvLibAstNode
    permits SvLibTagProperty, SvLibTagReference {}

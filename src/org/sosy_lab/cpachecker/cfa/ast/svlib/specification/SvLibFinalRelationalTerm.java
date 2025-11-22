// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibAstNode;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibExpression;
import org.sosy_lab.cpachecker.cfa.ast.svlib.SvLibTermVisitor;

// TODO: seal once we have modules
public interface SvLibFinalRelationalTerm extends SvLibExpression, SvLibAstNode {

  <R, X extends Exception> R accept(SvLibTermVisitor<R, X> v) throws X;
}

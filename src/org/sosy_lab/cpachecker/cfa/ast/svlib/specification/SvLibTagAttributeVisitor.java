// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib.specification;

public interface SvLibTagAttributeVisitor<R, X extends Exception> {

  R accept(SvLibTagReference pSvLibTagReference) throws X;

  R accept(SvLibCheckTrueTag pSvLibCheckTrueTag) throws X;

  R accept(SvLibRequiresTag pSvLibRequiresTag) throws X;

  R accept(SvLibEnsuresTag pSvLibEnsuresTag) throws X;

  R accept(SvLibInvariantTag pSvLibInvariantTag) throws X;
}

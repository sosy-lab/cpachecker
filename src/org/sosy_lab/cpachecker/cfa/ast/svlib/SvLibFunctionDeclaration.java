// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibAnyType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibFunctionType;
import org.sosy_lab.cpachecker.cfa.types.svlib.SvLibType;

public final class SvLibFunctionDeclaration extends AFunctionDeclaration
    implements SvLibDeclaration {
  @Serial private static final long serialVersionUID = -7637229289026207373L;

  public static SvLibFunctionDeclaration nondetFunctionWithReturnType(SvLibType pType) {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(ImmutableList.of(), pType),
        "#VERIFIER_nondet_" + pType,
        "#VERIFIER_nondet_" + pType,
        ImmutableList.of());
  }

  public static SvLibFunctionDeclaration mainFunctionDeclaration() {
    return new SvLibFunctionDeclaration(
        FileLocation.DUMMY,
        new SvLibFunctionType(ImmutableList.of(), new SvLibAnyType()),
        "#VERIFIER_MAIN",
        "#VERIFIER_MAIN",
        ImmutableList.of());
  }

  public SvLibFunctionDeclaration(
      FileLocation pFileLocation,
      SvLibFunctionType pType,
      String pName,
      String pOrigName,
      List<SvLibParameterDeclaration> pParameters) {
    super(pFileLocation, pType, pName, pOrigName, pParameters);
  }

  @SuppressWarnings("unchecked")
  @Override
  public ImmutableList<SvLibParameterDeclaration> getParameters() {
    return (ImmutableList<SvLibParameterDeclaration>) super.getParameters();
  }

  @Override
  public SvLibFunctionType getType() {
    return (SvLibFunctionType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.accept(this);
  }
}

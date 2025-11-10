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

/**
 * Corresponds to a mathematical function declaration i.e. as given in SMT-LIB. It has neither
 * parameters, nor a return variable, since this is handled by assumptions in SMT-LIB
 */
public final class SvLibFunctionDeclaration extends AFunctionDeclaration
    implements SvLibDeclaration {

  @Serial private static final long serialVersionUID = 5745608767872283746L;

  public SvLibFunctionDeclaration(
      FileLocation pFileLocation,
      String pName,
      List<SvLibType> pInputTypes,
      SvLibType pReturnType) {
    // The type of the procedure declaration can be inferred from the parameters, since there is no
    // anonymous parameters and no function declaration using only the types
    super(
        pFileLocation,
        new SvLibFunctionType(pFileLocation, pInputTypes, pReturnType),
        pName,
        pName,
        // Functions are declared anonymously in SMT-LIB, so there are no parameters
        // only from the function definition do we get the parameters
        ImmutableList.of());
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public SvLibFunctionType getType() {
    return (SvLibFunctionType) super.getType();
  }

  @Override
  public <R, X extends Exception> R accept(SvLibAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public String toASTString() {
    return getType().toASTString(getName());
  }

  @Override
  public ImmutableList<SvLibParameterDeclaration> getParameters() {
    throw new UnsupportedOperationException(
        "SvLibFunctionDeclaration has no parameters, since it is a "
            + "mathematical function declaration.");
  }
}

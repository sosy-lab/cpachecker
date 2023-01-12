// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.collect.Iterables.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CInitializerList extends AbstractInitializer implements CInitializer, CAstNode {

  private static final long serialVersionUID = 6601820489208683306L;
  private final List<CInitializer> initializerList;

  public CInitializerList(
      final FileLocation pFileLocation, final List<CInitializer> pInitializerList) {
    super(pFileLocation);
    initializerList = ImmutableList.copyOf(pInitializerList);
  }

  public List<CInitializer> getInitializers() {
    return initializerList;
  }

  @Override
  public String toASTString(boolean pQualified) {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append("{ ");
    Joiner.on(", ")
        .appendTo(lASTString, transform(initializerList, cinit -> cinit.toASTString(pQualified)));
    lASTString.append(" }");

    return lASTString.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializerList);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CInitializerList) || !super.equals(obj)) {
      return false;
    }

    CInitializerList other = (CInitializerList) obj;

    return Objects.equals(other.initializerList, initializerList);
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }
}

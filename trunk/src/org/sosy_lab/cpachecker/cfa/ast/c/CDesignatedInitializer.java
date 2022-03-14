// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.collect.Lists.transform;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AbstractInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

public class CDesignatedInitializer extends AbstractInitializer implements CInitializer {

  private static final long serialVersionUID = -2567254248669651550L;
  private final List<CDesignator> designators;
  private final CInitializer right;

  public CDesignatedInitializer(
      FileLocation pFileLocation, final List<CDesignator> pLeft, final CInitializer pRight) {
    super(pFileLocation);
    designators = ImmutableList.copyOf(pLeft);
    right = pRight;
  }

  @Override
  public String toASTString(boolean pQualified) {
    StringBuilder sb = new StringBuilder();
    Joiner.on("")
        .appendTo(sb, transform(designators, cdesignator -> cdesignator.toASTString(pQualified)));
    sb.append(" = ");
    sb.append(right.toASTString(pQualified));
    return sb.toString();
  }

  public List<CDesignator> getDesignators() {
    return designators;
  }

  public CInitializer getRightHandSide() {
    return right;
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(CAstNodeVisitor<R, X> pV) throws X {
    return pV.visit(this);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(designators);
    result = prime * result + Objects.hashCode(right);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CDesignatedInitializer) || !super.equals(obj)) {
      return false;
    }

    CDesignatedInitializer other = (CDesignatedInitializer) obj;

    return Objects.equals(other.designators, designators) && Objects.equals(other.right, right);
  }
}

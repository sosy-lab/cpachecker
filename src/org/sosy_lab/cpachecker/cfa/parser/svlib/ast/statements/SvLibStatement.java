// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.statements;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import java.io.Serial;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagAttribute;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagProperty;
import org.sosy_lab.cpachecker.cfa.ast.svlib.specification.SvLibTagReference;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNode;

public abstract sealed class SvLibStatement implements SvLibParsingAstNode
    permits SvLibAssignmentStatement,
        SvLibControlFlowStatement,
        SvLibHavocStatement,
        SvLibProcedureCallStatement {

  @Serial private static final long serialVersionUID = -2682818218051235918L;
  private final FileLocation fileLocation;
  private final ImmutableList<SvLibTagProperty> tagAttributes;
  private final ImmutableList<SvLibTagReference> tagReferences;

  protected SvLibStatement(
      FileLocation pFileLocation,
      List<SvLibTagProperty> pTagAttributes,
      List<SvLibTagReference> pTagReferences) {
    fileLocation = pFileLocation;
    tagAttributes = ImmutableList.copyOf(pTagAttributes);
    tagReferences = ImmutableList.copyOf(pTagReferences);
  }

  public ImmutableList<SvLibTagProperty> getTagAttributes() {
    return tagAttributes;
  }

  public @NonNull ImmutableList<@NonNull SvLibTagReference> getTagReferences() {
    return tagReferences;
  }

  public abstract <R, X extends Exception> R accept(SvLibStatementVisitor<R, X> v) throws X;

  /**
   * Returns a string representation of the SvLibStatement. If annotations exist for the
   * SvLibStatement, then these annotations, which may be tags or properties, are added to the
   * String. The generated string representation corresponds to the format for SvLibStatements in
   * the SvLib language.
   *
   * @return the SvLibStatement with annotations as String
   */
  @Override
  public String toASTString() {
    if (getTagReferences().isEmpty() && getTagAttributes().isEmpty()) {
      return toASTStringWithoutTags();
    } else {
      return "(! "
          + toASTStringWithoutTags()
          + Joiner.on(" ")
              .join(
                  FluentIterable.concat(tagReferences, tagAttributes)
                      .transform(SvLibTagAttribute::toASTString))
          + ")";
    }
  }

  /**
   * Returns a string representation of the SvLibStatement without adding any annotations that might
   * exist for the statement.
   *
   * @return the SvLibStatement as String
   */
  public abstract String toASTStringWithoutTags();

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }

    return pOther instanceof SvLibStatement other
        && tagAttributes.equals(other.tagAttributes)
        && tagReferences.equals(other.tagReferences);
  }

  @Override
  public int hashCode() {
    return 31 * tagAttributes.hashCode() + tagReferences.hashCode();
  }
}

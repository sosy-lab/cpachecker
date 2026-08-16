// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast.commands;

import com.google.common.base.Preconditions;
import java.io.Serial;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibSetInfoCommand implements SmtLibCommand, SvLibCommand {

  @Serial private static final long serialVersionUID = 5563954583806046500L;
  private final String option;
  private final @Nullable String value;
  private final FileLocation fileLocation;

  public SvLibSetInfoCommand(String pOption, @Nullable String pValue, FileLocation pFileLocation) {
    Preconditions.checkArgument(pOption != null);
    option = pOption;
    value = pValue;
    fileLocation = pFileLocation;
  }

  @NonNull
  public String getOption() {
    return option;
  }

  @Nullable
  public String getValue() {
    return value;
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(set-info " + option + " " + value + ")";
  }

  @Override
  public int hashCode() {
    return option.hashCode() * 31 + (value != null ? value.hashCode() : 0);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibSetInfoCommand other
        && option.equals(other.option)
        && value.equals(other.value);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibCommandVisitor<R, X> v) throws X {
    return v.visit(this);
  }

  @Override
  public <R, X extends Exception> R accept(SvLibParsingAstNodeVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}

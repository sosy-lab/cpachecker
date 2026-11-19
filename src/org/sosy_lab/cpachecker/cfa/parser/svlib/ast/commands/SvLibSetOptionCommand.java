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
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.parser.svlib.ast.SvLibParsingAstNodeVisitor;

public final class SvLibSetOptionCommand implements SmtLibCommand, SvLibCommand {
  @Serial private static final long serialVersionUID = 5643151014736552178L;

  private final String option;
  private final String value;
  private final FileLocation fileLocation;

  // Some constants to identify common options
  public static final String OPTION_WITNESS_OUTPUT_CHANNEL = ":witness-output-channel";

  public static final String OPTION_PRODUCE_CORRECTNESS = ":produce-correctness-witnesses";

  public static final String OPTION_PRODUCE_VIOLATION = ":produce-violation-witnesses";

  public SvLibSetOptionCommand(String pOption, String pValue, FileLocation pFileLocation) {
    Preconditions.checkArgument(pOption != null);
    Preconditions.checkArgument(pValue != null);
    option = pOption;
    value = pValue;
    fileLocation = pFileLocation;
  }

  @NonNull
  public String getOption() {
    return option;
  }

  @NonNull
  public String getValue() {
    return value;
  }

  public Optional<Boolean> getBooleanValue() {
    if (value.equalsIgnoreCase("true")) {
      return Optional.of(true);
    } else if (value.equalsIgnoreCase("false")) {
      return Optional.of(false);
    } else {
      return Optional.empty();
    }
  }

  @Override
  public FileLocation getFileLocation() {
    return fileLocation;
  }

  @Override
  public String toASTString() {
    return "(set-option "
        + option
        + " "
        // Add quotation marks to the String if value does not represent a boolean value,
        // but a String, i.e. a witness-output-channel.
        // The quotation marks are needed so that toASTString conforms to the expected format
        // for options with a value of type String and serialization of Sv-Lib programs works
        // correctly.
        + (getBooleanValue().equals(Optional.empty()) ? "\"" + value + "\"" : value)
        + ")";
  }

  @Override
  public int hashCode() {
    return option.hashCode() * 31 + value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    return obj instanceof SvLibSetOptionCommand other
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

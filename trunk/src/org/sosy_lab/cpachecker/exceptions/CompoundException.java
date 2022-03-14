// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.sosy_lab.common.annotations.SuppressForbidden;

public class CompoundException extends CPAException {

  private static final long serialVersionUID = -8880889342586540115L;

  private final List<CPAException> exceptions;

  public CompoundException(List<CPAException> pExceptions) {
    super(getMessage(pExceptions));
    exceptions = Collections.unmodifiableList(pExceptions);
  }

  public List<CPAException> getExceptions() {
    return exceptions;
  }

  @SuppressForbidden(value = "result of Collectors.toList() is only read, needs to support null")
  private static String getMessage(List<CPAException> pExceptions) {
    Preconditions.checkArgument(
        pExceptions.size() > 1,
        "Use a CompoundException only if there actually are multiple exceptions.");
    List<String> messages =
        pExceptions.stream().map(Throwable::getMessage).distinct().collect(Collectors.toList());
    if (messages.size() == 1) {
      return messages.get(0);
    }
    return "Several exceptions occured during the analysis:\n -> "
        + Joiner.on("\n -> ").join(messages);
  }
}

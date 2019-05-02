/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.exceptions;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
        + Joiner.on("\n -> ")
            .join(Lists.transform(pExceptions, e -> e.getMessage()).stream().distinct().iterator());
  }
}
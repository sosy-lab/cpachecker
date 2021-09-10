// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.string.domains;

import java.util.Arrays;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cpa.string.StringOptions;
import org.sosy_lab.cpachecker.cpa.string.utils.Aspect;

/*
 * Tracks the characters in the string
 */
public class CharSetDomain implements AbstractStringDomain {

  private static final DomainType TYPE = DomainType.CHAR_SET;
  // private final StringOptions options;

  private CharSetDomain(StringOptions pOptions) {
    // options = pOptions;
  }

  @Override
  public Aspect toAdd(String pVariable) {
    String noDuplicates =
        Arrays.asList(pVariable.split("")).stream().distinct().collect(Collectors.joining());
    return new Aspect(TYPE, noDuplicates);
  }

  @Override
  public DomainType getType() {
    return TYPE;
  }

  @Override
  public AbstractStringDomain createInstance(StringOptions pOptions) {
    return new CharSetDomain(pOptions);
  }

}

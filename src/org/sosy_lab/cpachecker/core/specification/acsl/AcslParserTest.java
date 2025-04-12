// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;

public class AcslParserTest {

  @Test
  public void parseSimpleStrings() throws Exception {
    List<String> inputs = ImmutableList.of("x == 0", "\\old{x} < 0", "\\result > 0");

    AcslParser parser = new AcslParser();

    for (String input : inputs) {
      parser.parsePredicate(input);
    }
  }
}

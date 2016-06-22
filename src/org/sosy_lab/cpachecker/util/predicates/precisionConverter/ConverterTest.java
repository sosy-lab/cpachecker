/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.precisionConverter;

import static com.google.common.truth.Truth.assert_;

import org.junit.Test;
import org.sosy_lab.common.log.LogManagerWithoutDuplicates;
import org.sosy_lab.common.log.LogManager;


public class ConverterTest {

  /** just some useless formulas */
  private final String[] LINES = {
      "(declare-fun |__VERIFIER_assert::cond@1| () Int)",
      "(define-fun .def_110 () Bool (= |__VERIFIER_assert::cond@1| 0))",
      "(define-fun .def_113 () Bool (<= 1 |main::p@4|))",
      "(define-fun .def_176 () Int (+ .def_75 .def_65))",
      "(define-fun .def_285 () Int (* (- 2) |main::i@3|))",
      "(assert .def_110)",
      "(assert false)",
      "(assert (((a))))",
      "(assert (t t t t t))",
      "(assert (t (t t t) (t t)))",
      "(assert ((t (t t t)) (t t) (t t) t))",
      "(assert (3))",
      "(define-fun .def_178 () Bool (4))",
      "(define-fun .def_178 (t t t) Bool (4))",
      };

  private final LogManagerWithoutDuplicates logger = new LogManagerWithoutDuplicates(LogManager.createTestLogManager());

  @Test
  public void test1() throws Exception {
    Converter converter = new Converter();
    for (String line : LINES) {
      String converted = FormulaParser.convertFormula(converter, line, logger);
      assert_().that(converted).isEqualTo(line);
    }
  }
}

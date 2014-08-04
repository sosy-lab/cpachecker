/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates.mathsat5;

import static org.junit.Assert.assertEquals;
import static org.sosy_lab.cpachecker.util.predicates.mathsat5.Mathsat5NativeApi.*;

import org.junit.Test;


/**
 * This class is not part of the normal CPAchecker test suite because
 * the java.library.path needs to be defined in order to execute the tests.
 */
public class Mathsat5NativeApiTest0 {

  @Test
  public void bvSize() {
    long cfg = msat_create_config();
    long env = msat_create_env(cfg);
    msat_destroy_config(cfg);

    long number = msat_make_bv_number(env, "42", 32, 10);
    long type = msat_term_get_type(number);

    assertEquals(true, msat_is_bv_type(env, type));
    assertEquals(32, msat_get_bv_type_size(env, type));

    long funcDecl = msat_declare_function(env, "testVar", type);
    long var = msat_make_constant(env, funcDecl);
    type = msat_term_get_type(var);

    assertEquals(true, msat_is_bv_type(env, type));
    assertEquals(32, msat_get_bv_type_size(env, type));


    msat_destroy_env(env);
  }
}

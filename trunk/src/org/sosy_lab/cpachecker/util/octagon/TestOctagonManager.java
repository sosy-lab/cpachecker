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
package org.sosy_lab.cpachecker.util.octagon;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class TestOctagonManager {

  static OctagonManager manager;

  @BeforeClass
  public static void setUpBeforeClass() {
    manager = new OctagonFloatManager();
  }

  @Test
  public void testNum_Int() {
    NumArray num = manager.init_num_t(1);
    manager.num_set_int(num, 0, 3);
    Assert.assertFalse(manager.num_infty(num, 0));
    Assert.assertEquals(3, manager.num_get_int(num, 0));
    Assert.assertEquals(3, manager.num_get_float(num, 0), 0);
  }

  @Test
  public void testNum_Float() {
    NumArray num = manager.init_num_t(1);
    manager.num_set_float(num, 0, 3.3);
    Assert.assertFalse(manager.num_infty(num, 0));
    Assert.assertEquals(3, manager.num_get_int(num, 0));
    Assert.assertEquals(3.3, manager.num_get_float(num, 0), 0);
  }

}

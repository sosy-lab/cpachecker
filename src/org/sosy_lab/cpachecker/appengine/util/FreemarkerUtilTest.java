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
package org.sosy_lab.cpachecker.appengine.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.restlet.Context;


public class FreemarkerUtilTest {

  @Test
  public void contextIsNull() throws Exception {
    Throwable exc = null;
    try {
      FreemarkerUtil.templateBuilder().build();
    } catch (IllegalStateException e) {
      exc = e;
    }
    assertEquals("The context must not be null.", exc.getMessage());
  }

  @Test
  public void templateNameIsNull() throws Exception {
    Throwable exc = null;
    try {
      FreemarkerUtil.templateBuilder().context(new Context()).templateName(null).build();
    } catch (IllegalStateException e) {
      exc = e;
    }
    assertEquals("The template name must not be null or empty.", exc.getMessage());
  }

  @Test
  public void labelsKeyIsNull() throws Exception {
    Throwable exc = null;
    try {
      FreemarkerUtil.templateBuilder().context(new Context()).templateName("foo").labelsKey(null).build();
    } catch (IllegalStateException e) {
      exc = e;
    }
    assertEquals("The labels key must not be null or empty.", exc.getMessage());
  }

  @Test
  public void resourceBundleNameIsNull() throws Exception {
    Throwable exc = null;
    try {
      FreemarkerUtil.templateBuilder().context(new Context()).templateName("foo").resourceBundleName(null).build();
    } catch (IllegalStateException e) {
      exc = e;
    }
    assertEquals("The resource bundle name must not be null or empty.", exc.getMessage());
  }

}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.pcc.strategy;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

import org.eclipse.jdt.core.dom.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnsuitedClassException;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RunWith(Parameterized.class)
public class PCCStrategyBuilderTest {

  @Parameters(name = "{0}")
  public static List<Class<?>> getPCCStrategies() throws IOException {
    return ClassPath.from(Thread.currentThread().getContextClassLoader())
        .getTopLevelClassesRecursive("org.sosy_lab.cpachecker.pcc.strategy")
        .stream()
        .map(ClassInfo::load)
        .filter(PCCStrategy.class::isAssignableFrom)
        .filter(cls -> !Modifier.isAbstract(cls.getModifiers()))
        .collect(Collectors.toList());
  }

  @Parameter(0)
  public Class<?> pccStrategyClass;

  @Test
  @SuppressWarnings("CheckReturnValue")
  public void testStrategyHasMatchingConstructorForFactory() throws UnsuitedClassException {
    Classes.createFactory(PCCStrategy.Factory.class, pccStrategyClass);
  }
}

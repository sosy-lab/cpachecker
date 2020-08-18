// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.jdt.core.dom.Modifier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.Classes.UnsuitedClassException;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;

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

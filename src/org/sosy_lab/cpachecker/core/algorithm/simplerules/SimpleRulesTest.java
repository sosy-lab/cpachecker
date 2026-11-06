// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.simplerules;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.Test;

/** Tests for {@link SimpleRules}, the registry that decides which rules the analysis runs. */
public class SimpleRulesTest {

  /**
   * Every rule that some class of this package provides, found by the convention that such a class
   * declares a static method {@code rules()}.
   */
  private static ImmutableList<SimpleRule> providedRules() throws Exception {
    ImmutableList.Builder<SimpleRule> rules = ImmutableList.builder();
    for (ClassInfo classInfo :
        ClassPath.from(Thread.currentThread().getContextClassLoader())
            .getTopLevelClasses(SimpleRulesTest.class.getPackage().getName())) {
      Method method;
      try {
        method = classInfo.load().getDeclaredMethod("rules");
      } catch (NoSuchMethodException e) {
        continue;
      }
      if (!Modifier.isStatic(method.getModifiers())) {
        continue;
      }
      for (Object rule : (Iterable<?>) method.invoke(null)) {
        rules.add((SimpleRule) rule);
      }
    }
    return rules.build();
  }

  private static ImmutableList<String> namesOf(ImmutableList<SimpleRule> pRules) {
    return pRules.stream().map(SimpleRule::name).collect(toImmutableList());
  }

  /**
   * Guards against the most likely mistake when a rule is added: writing it, but not listing it in
   * {@link SimpleRules#all()}, where it would silently never be used.
   */
  @Test
  public void testEveryRuleOfThePackageIsRegistered() throws Exception {
    ImmutableList<SimpleRule> provided = providedRules();
    assertWithMessage(
            "no class with a static method rules() was found, so this test checks nothing")
        .that(provided)
        .isNotEmpty();
    assertThat(namesOf(SimpleRules.all())).containsAtLeastElementsIn(namesOf(provided));
  }

  /**
   * The name of a rule selects it in the option {@code simplerules.rules}, so it must be unique.
   */
  @Test
  public void testRuleNamesAreUnique() {
    assertThat(namesOf(SimpleRules.all())).containsNoDuplicates();
  }
}

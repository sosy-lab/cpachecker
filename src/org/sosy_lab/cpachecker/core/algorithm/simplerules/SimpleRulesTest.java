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
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

  private static final Path ALGORITHM_SOURCE =
      Path.of("src/org/sosy_lab/cpachecker/core/algorithm/simplerules/SimpleRulesAlgorithm.java");

  private static final Pattern DEFAULT_OF_RULES_OPTION =
      Pattern.compile("private Set<String> rules =(.*?);", Pattern.DOTALL);

  /**
   * The default of {@code simplerules.rules} lists every rule, so that {@code
   * doc/ConfigurationOptions.txt} shows which rules exist. That list has to be written out in the
   * source, because OptionCollector documents the source text of the initializer, and this test is
   * what keeps it in sync with {@link SimpleRules#all()}.
   */
  @Test
  public void testDefaultOfTheRulesOptionListsEveryRule() throws IOException {
    String source = Files.readString(ALGORITHM_SOURCE, StandardCharsets.UTF_8);
    Matcher declaration = DEFAULT_OF_RULES_OPTION.matcher(source);
    assertWithMessage(
            "the option simplerules.rules is not declared as expected in %s", ALGORITHM_SOURCE)
        .that(declaration.find())
        .isTrue();

    ImmutableList.Builder<String> names = ImmutableList.builder();
    Matcher literal = Pattern.compile("\"([^\"]*)\"").matcher(declaration.group(1));
    while (literal.find()) {
      names.add(literal.group(1));
    }
    assertThat(names.build()).containsExactlyElementsIn(namesOf(SimpleRules.all()));
  }
}

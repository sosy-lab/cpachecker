// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.Invokable;
import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.Test;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.annotations.SuppressForbidden;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

public class PredicateCPATest {

  private static final Pattern PREDICATECPA_CLASSES =
      Pattern.compile(
          "org\\.sosy_lab\\.cpachecker\\..*(predicate|bdd|BDD|FormulaReportingState|InvariantSupplier).*");
  private static final Pattern BDD_CLASS_PATTERN = Pattern.compile("(BDD|bdd)");

  /**
   * This tests that the BDD library is NOT loaded by PredicateCPA if it is not necessary (loading
   * the library might occupy a lot of memory).
   */
  @Test
  public void dontLoadBDDLibraryIfNotNecessary() throws Exception {
    Configuration config =
        TestDataTools.configurationForTest()
            .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
            .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
            .build();

    FluentIterable<String> loadedClasses = loadPredicateCPA(config);
    assertThat(loadedClasses.filter(Predicates.contains(BDD_CLASS_PATTERN))).isEmpty();
  }

  /**
   * This tests that the BDD library is loaded by PredicateCPA if it is necessary (if this test
   * fails, the {@link #loadBDDLibraryIfNecessary} test won't work).
   */
  @Test
  public void loadBDDLibraryIfNecessary() throws Exception {
    Configuration config = TestDataTools.configurationForTest().build();

    FluentIterable<String> loadedClasses = loadPredicateCPA(config);
    assertThat(loadedClasses.filter(Predicates.contains(BDD_CLASS_PATTERN))).isNotEmpty();
  }

  @SuppressForbidden("reflection only in test")
  private FluentIterable<String> loadPredicateCPA(Configuration config) throws Exception {
    ClassLoader myClassLoader = PredicateCPATest.class.getClassLoader();
    assume().that(myClassLoader).isInstanceOf(URLClassLoader.class);
    LogManager logger = LogManager.createTestLogManager();

    try (URLClassLoader cl =
        Classes.makeExtendedURLClassLoader()
            .setParent(myClassLoader)
            .setUrls(((URLClassLoader) myClassLoader).getURLs())
            .setDirectLoadClasses(PREDICATECPA_CLASSES)
            .build()) {
      Class<?> cpaClass =
          cl.loadClass(PredicateCPATest.class.getPackage().getName() + ".PredicateCPA");
      Invokable<?, CPAFactory> factoryMethod =
          Invokable.from(cpaClass.getDeclaredMethod("factory")).returning(CPAFactory.class);
      CPAFactory factory = factoryMethod.invoke(null);

      factory.setConfiguration(config);
      factory.setLogger(logger);
      factory.setShutdownNotifier(ShutdownNotifier.createDummy());
      factory.set(AggregatedReachedSets.empty(), AggregatedReachedSets.class);
      factory.set(TestDataTools.makeCFA(config, "void main() { }"), CFA.class);
      factory.set(new ReachedSetFactory(config, logger), ReachedSetFactory.class);
      factory.set(Specification.alwaysSatisfied(), Specification.class);

      ConfigurableProgramAnalysis cpa = factory.createInstance();
      if (cpa instanceof AutoCloseable) {
        ((AutoCloseable) cpa).close();
      }

      Field classesField = ClassLoader.class.getDeclaredField("classes");
      classesField.setAccessible(true);
      @SuppressWarnings("unchecked")
      List<Class<?>> classes = (List<Class<?>>) classesField.get(cl);
      return FluentIterable.from(classes).transform(Class::getName);
    }
  }
}

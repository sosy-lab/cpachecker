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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.reflect.Invokable;

import org.junit.Test;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import java.lang.reflect.Field;
import java.net.URLClassLoader;
import java.util.Vector;
import java.util.regex.Pattern;

public class PredicateCPATest {

  private static final Pattern PREDICATECPA_CLASSES =
      Pattern.compile(
          "org\\.sosy_lab\\.cpachecker\\..*(predicate|bdd|BDD|FormulaReportingState|InvariantSupplier).*");
  private static final Pattern BDD_CLASS_PATTERN = Pattern.compile("(BDD|bdd)");

  /**
   * This tests that the BDD library is NOT loaded by PredicateCPA if it is not necessary
   * (loading the library might occupy a lot of memory).
   */
  @Test
  public void dontLoadBDDLibraryIfNotNecessary() throws Exception {
    Configuration config = TestDataTools.configurationForTest()
        .setOption("cpa.predicate.blk.alwaysAtFunctions", "false")
        .setOption("cpa.predicate.blk.alwaysAtLoops", "false")
        .build();

    FluentIterable<String> loadedClasses = loadPredicateCPA(config);
    assertThat(loadedClasses.filter(Predicates.contains(BDD_CLASS_PATTERN))).isEmpty();
  }

  /**
   * This tests that the BDD library is loaded by PredicateCPA if it is necessary
   * (if this test fails, the {@link #loadBDDLibraryIfNecessary} test
   * won't work).
   */
  @Test
  public void loadBDDLibraryIfNecessary() throws Exception {
    Configuration config = TestDataTools.configurationForTest().build();

    FluentIterable<String> loadedClasses = loadPredicateCPA(config);
    assertThat(loadedClasses.filter(Predicates.contains(BDD_CLASS_PATTERN))).isNotEmpty();
  }

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
      Class<?> cpaClass = cl.loadClass(PredicateCPATest.class.getPackage().getName() + ".PredicateCPA");
      Invokable<?, CPAFactory> factoryMethod = Invokable.from(cpaClass.getDeclaredMethod("factory")).returning(CPAFactory.class);
      CPAFactory factory = factoryMethod.invoke(null);

      factory.setConfiguration(config);
      factory.setLogger(logger);
      factory.setShutdownNotifier(ShutdownNotifier.createDummy());
      factory.set(new AggregatedReachedSets(), AggregatedReachedSets.class);
      factory.set(TestDataTools.makeCFA(config, "void main() { }"), CFA.class);
      factory.set(new ReachedSetFactory(config), ReachedSetFactory.class);
      factory.set(Specification.alwaysSatisfied(), Specification.class);

      ConfigurableProgramAnalysis cpa = factory.createInstance();
      if (cpa instanceof AutoCloseable) {
        ((AutoCloseable)cpa).close();
      }

      Field classesField = ClassLoader.class.getDeclaredField("classes");
      classesField.setAccessible(true);
      @SuppressWarnings("unchecked")
      Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(cl);
      return FluentIterable.from(classes).transform(Class::getName);
    }
  }
}

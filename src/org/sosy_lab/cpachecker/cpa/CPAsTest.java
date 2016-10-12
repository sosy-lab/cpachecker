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
package org.sosy_lab.cpachecker.cpa;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;

import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.annotations.Unmaintained;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.abe.ABECPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.argReplay.ARGReplayCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.cache.CacheCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.monitor.MonitorCPA;
import org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor.SingleSuccessorCompactorCPA;
import org.sosy_lab.cpachecker.cpa.termination.TerminationCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

@RunWith(Parameterized.class)
public class CPAsTest {

  @Parameters(name = "{0}")
  public static Iterable<Class<?>> getCPAs() throws IOException {
    Set<Class<?>> cpas =
        ClassPath.from(Thread.currentThread().getContextClassLoader())
            .getTopLevelClassesRecursive(CPAsTest.class.getPackage().getName())
            .stream()
            .map(ClassInfo::load)
            .filter(candidate -> !Modifier.isAbstract(candidate.getModifiers()))
            .filter(candidate -> !Modifier.isInterface(candidate.getModifiers()))
            .filter(ConfigurableProgramAnalysis.class::isAssignableFrom)
            .filter(candidate -> !candidate.isAnnotationPresent(Unmaintained.class))
            .filter(candidate -> !candidate.getPackage().isAnnotationPresent(Unmaintained.class))
            .collect(Collectors.toCollection(HashSet::new));

    // Filter CPAs that need child CPAs.
    cpas.remove(ARGCPA.class);
    cpas.remove(BAMCPA.class);
    cpas.remove(CacheCPA.class);
    cpas.remove(CompositeCPA.class);
    cpas.remove(MonitorCPA.class);
    cpas.remove(PropertyCheckerCPA.class);
    cpas.remove(SingleSuccessorCompactorCPA.class);

    cpas.remove(ARGReplayCPA.class); // needs ARG to be replayed
    cpas.remove(ABECPA.class); // Shouldn't be used by itself.

    return cpas;
  }

  @ClassRule public static final TemporaryFolder tempFolder = new TemporaryFolder();

  private static final LogManager logManager = LogManager.createTestLogManager();
  private static final ShutdownNotifier shutdownNotifier = ShutdownNotifier.createDummy();
  private static final StateSpacePartition partition = StateSpacePartition.getDefaultPartition();
  private static Configuration config;
  private static CFA cfa;
  private static FunctionEntryNode main;

  @Parameter(0)
  public Class<ConfigurableProgramAnalysis> cpaClass;

  private ConfigurableProgramAnalysis cpa;

  @BeforeClass
  public static void setup()
      throws ParserException, InvalidConfigurationException, IOException, InterruptedException {
    FileTypeConverter fileTypeConverter =
        FileTypeConverter.create(
            Configuration.builder()
                .setOption("output.disable", "true")
                .setOption("rootDirectory", tempFolder.getRoot().toString())
                .build());
    config =
        Configuration.builder()
            .addConverter(FileOption.class, fileTypeConverter)
            .setOption("cfa.findLiveVariables", "true")
            .setOption("cpa.conditions.path.condition", "PathLengthCondition")
            .setOption("cpa.automaton.inputFile", "test/config/automata/AssumptionAutomaton.spc")
            .build();

    // Create dummy files necessary for PolicyEnforcementCPA
    tempFolder.newFile("betamap.conf");
    tempFolder.newFile("immediatechecks.conf");

    cfa =
        TestDataTools.toCFA(
            new CFACreator(config, logManager, shutdownNotifier),
            "  int a;",
            "  a = 1;",
            "  return a;");
    main = cfa.getMainFunction();
  }

  @Before
  public void instantiate()
      throws ReflectiveOperationException, InvalidConfigurationException, CPAException {
    Method factoryMethod = cpaClass.getMethod("factory");

    Optional<ConfigurableProgramAnalysis> childCPA = createChildCpaIfNecessary(cpaClass);

    CPAFactory factory = (CPAFactory) factoryMethod.invoke(null);
    childCPA.ifPresent(factory::setChild);
    try {
      cpa =
          factory
              .setLogger(logManager)
              .setConfiguration(config)
              .setShutdownNotifier(shutdownNotifier)
              .set(new ReachedSetFactory(config), ReachedSetFactory.class)
              .set(cfa, CFA.class)
              .set(Specification.alwaysSatisfied(), Specification.class)
              .set(new AggregatedReachedSets(), AggregatedReachedSets.class)
              .createInstance();
    } catch (LinkageError e) {
      assume().fail(e.getMessage());
    }
  }

  private Optional<ConfigurableProgramAnalysis> createChildCpaIfNecessary(Class<?> cpaClass)
      throws InvalidConfigurationException, CPAException {
    if (cpaClass.equals(TerminationCPA.class)) {
      return Optional.of(
          LocationCPA.factory().set(cfa, CFA.class).setConfiguration(config).createInstance());

    } else {
      return Optional.empty();
    }
  }

  @Test
  public void getInitialState() throws InterruptedException {
    assertThat(cpa.getInitialState(main, partition)).named("initial state").isNotNull();
  }

  @Test
  public void join() throws CPAException, InterruptedException {
    AbstractState initial = cpa.getInitialState(main, partition);
    AbstractState joined;
    try {
      joined = cpa.getAbstractDomain().join(initial, initial);
    } catch (UnsupportedOperationException e) {
      assume().fail(e.getMessage());
      return;
    }
    assertThat(joined).named("result of join").isNotNull();
    assert_()
        .withFailureMessage("Join of same elements is unsound")
        .that(isLessOrEqual(initial, joined))
        .isTrue();
  }

  @Test
  public void merge() throws CPAException, InterruptedException {
    AbstractState initial = cpa.getInitialState(main, partition);
    Precision initialPrec = cpa.getInitialPrecision(main, partition);
    AbstractState merged = cpa.getMergeOperator().merge(initial, initial, initialPrec);
    assertThat(merged).named("result of merge").isNotNull();
    assert_()
        .withFailureMessage("Merging same elements was unsound")
        .that(isLessOrEqual(initial, merged))
        .isTrue();
  }

  @Test
  public void stop_EmptyReached() throws CPAException, InterruptedException {
    AbstractState initial = cpa.getInitialState(main, partition);
    Precision initialPrec = cpa.getInitialPrecision(main, partition);
    Set<AbstractState> reached = ImmutableSet.of();

    // Some CPAs have legitimate reasons to always return "true" from stop,
    // so no assertion here, just a check that it does not crash.
    cpa.getStopOperator().stop(initial, reached, initialPrec);
  }

  @Test
  public void stop_SameElement() throws CPAException, InterruptedException {
    AbstractState initial = cpa.getInitialState(main, partition);
    Precision initialPrec = cpa.getInitialPrecision(main, partition);
    Set<AbstractState> reached = ImmutableSet.of(initial);
    assert_()
        .withFailureMessage("Did not stop on same element")
        .that(cpa.getStopOperator().stop(initial, reached, initialPrec))
        .isTrue();
  }

  private boolean isLessOrEqual(AbstractState s1, AbstractState s2)
      throws CPAException, InterruptedException {
    try {
      return cpa.getAbstractDomain().isLessOrEqual(s1, s2);
    } catch (UnsupportedOperationException e) {
      assume().fail(e.getMessage());
      return false;
    }
  }
}

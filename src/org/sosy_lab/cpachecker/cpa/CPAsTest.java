// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa;

import static com.google.common.truth.Truth.assertWithMessage;
import static com.google.common.truth.Truth.assert_;
import static org.junit.Assume.assumeNoException;

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
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.CPAFactory;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.cpa.abe.ABECPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.argReplay.ARGReplayCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPA;
import org.sosy_lab.cpachecker.cpa.bam.BAMCPAWithBreakOnMissingBlock;
import org.sosy_lab.cpachecker.cpa.cache.CacheCPA;
import org.sosy_lab.cpachecker.cpa.composite.CompositeCPA;
import org.sosy_lab.cpachecker.cpa.dca.DCACPA;
import org.sosy_lab.cpachecker.cpa.flowdep.FlowDependenceCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.monitor.MonitorCPA;
import org.sosy_lab.cpachecker.cpa.powerset.PowerSetCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.cpa.singleSuccessorCompactor.SingleSuccessorCompactorCPA;
import org.sosy_lab.cpachecker.cpa.slab.SLABCPA;
import org.sosy_lab.cpachecker.cpa.slab.SLABPredicateWrappingCPA;
import org.sosy_lab.cpachecker.cpa.slicing.SlicingCPA;
import org.sosy_lab.cpachecker.cpa.termination.TerminationCPA;
import org.sosy_lab.cpachecker.cpa.traceabstraction.TraceAbstractionCPA;
import org.sosy_lab.cpachecker.cpa.usage.UsageCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
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
    cpas.remove(BAMCPAWithBreakOnMissingBlock.class);
    cpas.remove(CacheCPA.class);
    cpas.remove(DCACPA.class);
    cpas.remove(UsageCPA.class);
    cpas.remove(CompositeCPA.class);
    cpas.remove(MonitorCPA.class);
    cpas.remove(PropertyCheckerCPA.class);
    cpas.remove(SingleSuccessorCompactorCPA.class);
    cpas.remove(PowerSetCPA.class);
    cpas.remove(FlowDependenceCPA.class);
    cpas.remove(SlicingCPA.class);
    cpas.remove(SLABCPA.class);

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
  public static void setup() throws Exception {
    FileTypeConverter fileTypeConverter =
        FileTypeConverter.create(
            Configuration.builder()
                .setOption("output.disable", "true")
                .setOption("rootDirectory", tempFolder.getRoot().toString())
                .build());
    Configuration.getDefaultConverters().put(FileOption.class, fileTypeConverter);

    String cProgram = TestDataTools.getEmptyProgram(tempFolder, false);

    config =
        Configuration.builder()
            .addConverter(FileOption.class, fileTypeConverter)
            .setOption("cfa.findLiveVariables", "true")
            .setOption("cpa.conditions.path.condition", "PathLengthCondition")
            .setOption("cpa.automaton.inputFile", "test/config/automata/AssumptionAutomaton.spc")
            .setOption("differential.program", cProgram)
            .build();

    // Create dummy files necessary for PolicyEnforcementCPA
    tempFolder.newFile("betamap.conf");
    tempFolder.newFile("immediatechecks.conf");

    cfa =
        TestDataTools.toSingleFunctionCFA(
            new CFACreator(config, logManager, shutdownNotifier),
            "  int a;",
            "  a = 1;",
            "  return a;");
    main = cfa.getMainFunction();
  }

  @Before
  public void instantiate() throws Exception {
    Method factoryMethod = cpaClass.getMethod("factory");
    final CPAFactory factory = (CPAFactory) factoryMethod.invoke(null);

    final AggregatedReachedSets aggregatedReachedSets = AggregatedReachedSets.empty();
    final ReachedSetFactory reachedSetFactory = new ReachedSetFactory(config, logManager);

    Optional<CPAFactory> childCPA = getChildCpaFactoryIfNecessary(cpaClass);
    if (childCPA.isPresent()) {
      factory.setChild(createCpa(childCPA.orElseThrow(), aggregatedReachedSets, reachedSetFactory));
    }
    try {
      cpa = createCpa(factory, aggregatedReachedSets, reachedSetFactory);
    } catch (LinkageError e) {
      assumeNoException(e);
      throw new AssertionError(e);
    }
  }

  private Optional<CPAFactory> getChildCpaFactoryIfNecessary(Class<?> pCpaClass) {
    if (pCpaClass.equals(TerminationCPA.class)) {
      return Optional.of(LocationCPA.factory());

    } else if (pCpaClass.equals(SLABPredicateWrappingCPA.class)
        || pCpaClass.equals(TraceAbstractionCPA.class)) {
      return Optional.of(PredicateCPA.factory());

    } else {
      return Optional.empty();
    }
  }

  private ConfigurableProgramAnalysis createCpa(
      CPAFactory factory,
      AggregatedReachedSets aggregatedReachedSets,
      ReachedSetFactory reachedSetFactory)
      throws InvalidConfigurationException, CPAException {
    return factory
        .setLogger(logManager)
        .setConfiguration(config)
        .setShutdownNotifier(shutdownNotifier)
        .set(reachedSetFactory, ReachedSetFactory.class)
        .set(cfa, CFA.class)
        .set(Specification.alwaysSatisfied(), Specification.class)
        .set(aggregatedReachedSets, AggregatedReachedSets.class)
        .createInstance();
  }

  @Test
  public void getInitialState() throws InterruptedException {
    assertWithMessage("initial state").that(cpa.getInitialState(main, partition)).isNotNull();
  }

  @Test
  public void join() throws CPAException, InterruptedException {
    AbstractState initial = cpa.getInitialState(main, partition);
    AbstractState joined;
    try {
      joined = cpa.getAbstractDomain().join(initial, initial);
    } catch (UnsupportedOperationException e) {
      assumeNoException(e);
      throw new AssertionError(e);
    }
    assertWithMessage("result of join").that(joined).isNotNull();
    assert_()
        .withMessage("Join of same elements is unsound")
        .that(isLessOrEqual(initial, joined))
        .isTrue();
  }

  @Test
  public void merge() throws CPAException, InterruptedException {
    AbstractState initial = cpa.getInitialState(main, partition);
    Precision initialPrec = cpa.getInitialPrecision(main, partition);
    AbstractState merged = cpa.getMergeOperator().merge(initial, initial, initialPrec);
    assertWithMessage("result of merge").that(merged).isNotNull();
    assert_()
        .withMessage("Merging same elements was unsound")
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
        .withMessage("Did not stop on same element")
        .that(cpa.getStopOperator().stop(initial, reached, initialPrec))
        .isTrue();
  }

  private boolean isLessOrEqual(AbstractState s1, AbstractState s2)
      throws CPAException, InterruptedException {
    try {
      return cpa.getAbstractDomain().isLessOrEqual(s1, s2);
    } catch (UnsupportedOperationException e) {
      assumeNoException(e);
      throw new AssertionError(e);
    }
  }
}

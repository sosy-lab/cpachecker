package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.solver.api.BooleanFormula;

import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class InductiveWeakeningManagerTest {
  private CFACreator creator;
  private LogManager logger;
  private FormulaManagerView fmgr;
  private InductiveWeakeningManager inductiveWeakeningManager;
  private Configuration config;
  private ShutdownNotifier notifier;

  @Before public void setUp() throws Exception {
    config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "solver.solver", "Z3",

            // For easier debugging.
            "cpa.predicate.handlePointerAliasing", "false",
            "cpa.predicate.ignoreIrrelevantVariables", "false",

            "cpa.slicing.runDestructiveSlicing", "false",
            "cpa.slicing.runCounterexampleBasedSlicing", "true"
//            "cpa.slicing.runNewStrategy", "true"
        )
    ).build();
    notifier = ShutdownNotifier.createDummy();
    logger = new BasicLogManager(config,
        new StreamHandler(System.out, new SimpleFormatter()));
    creator = new CFACreator(config, logger, notifier);
    Solver solver = Solver.create(config, logger, notifier);
    fmgr = solver.getFormulaManager();

    inductiveWeakeningManager = new InductiveWeakeningManager(config, fmgr, solver, logger,
        new InductiveWeakeningStatistics());
  }

  @After public void tearDown() throws Exception {
    logger.flush();
  }

  @Test public void testSlicingVerySimple() throws Exception {
    CFA cfa = toCFA("int x, y; x = 1; y = 0;");
    PathFormulaManager pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    PathFormula f = toPathFormula(pfmgr, cfa);
    logger.log(Level.INFO, "Sliced formula: ", f);

    cfa = toCFA("int x; x++;");
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    PathFormula loop = toPathFormula(pfmgr, cfa, f.getSsa());
    logger.log(Level.INFO, "Loop transition: ", loop);

    BooleanFormula slice = inductiveWeakeningManager.slice(f, loop,
        fmgr.getBooleanFormulaManager().makeBoolean(true));

    logger.log(Level.INFO, "Obtained slice", slice);

    cfa = toCFA("int y; y = 0;");
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    BooleanFormula expectedFormula = fmgr.uninstantiate(toPathFormula(pfmgr, cfa)
        .getFormula());

    assertThat(slice).isEqualTo(expectedFormula);
  }

  @Test public void slicingSimpleRearranged() throws Exception {
    CFA cfa = toCFA("int x, y; y = 0; x = 1;");
    PathFormulaManager pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    PathFormula f = toPathFormula(pfmgr, cfa);
    logger.log(Level.INFO, "Sliced formula: ", f);

    cfa = toCFA("int x; x++;");
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    PathFormula loop = toPathFormula(pfmgr, cfa, f.getSsa());
    logger.log(Level.INFO, "Loop transition: ", loop);

    BooleanFormula slice = inductiveWeakeningManager.slice(f, loop,
        fmgr.getBooleanFormulaManager().makeBoolean(true));
    logger.log(Level.INFO, "Obtained slice", slice);

    cfa = toCFA("int y; y = 0;");
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    BooleanFormula expectedFormula = fmgr.uninstantiate(toPathFormula(pfmgr, cfa)
        .getFormula());

    assertThat(slice).isEqualTo(expectedFormula);

  }

  @Test public void testSlicingComplex() throws Exception {
    CFA cfa = toCFA(
        "int x, y, p, nondet;",
        "x = 5;",
        "y = 10;",
        "if (nondet) {",
          "y = 100;",
          "p = 1;",
        "} else {",
          "p = 2;",
        "}"
    );
    PathFormulaManager pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    // FIXME Tests should not rely on a user manually checking log message
    // but instead use proper assertions, otherwise they are useless as regression tests.
    PathFormula input = toPathFormula(pfmgr, cfa);

    cfa = toCFA("int x; x += 1;");
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        cfa, AnalysisDirection.FORWARD);
    PathFormula loopTransition = toPathFormula(pfmgr, cfa);

    BooleanFormula slice = inductiveWeakeningManager.slice(
        input, loopTransition,
        fmgr.getBooleanFormulaManager().makeBoolean(true));

    logger.log(Level.INFO, "Obtained slice", slice);
  }

  private PathFormula toPathFormula(PathFormulaManager pfmgr, CFA cfa) throws Exception {
    return toPathFormula(pfmgr, cfa, SSAMap.emptySSAMap());
  }

  private PathFormula toPathFormula(PathFormulaManager pfmgr, CFA cfa, SSAMap initialSSA) throws Exception {
    return TestDataTools.toPathFormula(cfa, initialSSA,
        fmgr, pfmgr, true);
  }

  private CFA toCFA(String... parts) throws Exception {
    return TestDataTools.toCFA(creator, parts);
  }
}

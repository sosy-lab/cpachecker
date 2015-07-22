package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;

import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.collect.ImmutableMap;

public class InductiveWeakeningManagerTest {
  private CFACreator creator;
  private LogManager logger;
  private PathFormulaManager pfmgr;
  private FormulaManagerView fmgr;
  private InductiveWeakeningManager inductiveWeakeningManager;

  @Before public void setUp() throws Exception {
    Configuration config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "cpa.predicate.solver", "Z3",
            "log.consoleLevel", "FINE",

            // todo: just for easier debugging now.
            "cpa.predicate.handlePointerAliasing", "false",

            "analysis.interprocedural", "false"
        )
    ).build();
    ShutdownNotifier notifier = ShutdownNotifier.create();
    logger = new BasicLogManager(config,
        new StreamHandler(System.out, new SimpleFormatter()));
    creator = new CFACreator(config, logger, notifier);
    FormulaManagerFactory factory = new FormulaManagerFactory(config, logger, notifier);
    fmgr = new FormulaManagerView(factory, config, logger);
    // todo: non-deprecated constructor.
    pfmgr = new PathFormulaManagerImpl(fmgr, config, logger, notifier,
        MachineModel.LINUX32, AnalysisDirection.FORWARD);
    Solver solver = new Solver(fmgr, factory, config, logger);
    inductiveWeakeningManager = new InductiveWeakeningManager(fmgr, solver,
        factory.getFormulaManager().getUnsafeFormulaManager(), logger);
  }

  @After public void tearDown() throws Exception {
    logger.flush();
  }

  @Test public void testSlicingVerySimple() throws Exception {
    PathFormula f = toPathFormula(toCFA("int x, y; x = 1; y = 0;"));
    logger.log(Level.INFO, "Sliced formula: ", f);

    PathFormula loop = toPathFormula(toCFA("int x; x++;"), f.getSsa());
    logger.log(Level.INFO, "Loop transition: ", loop);

    BooleanFormula slice = inductiveWeakeningManager.slice(f, loop);
    logger.log(Level.INFO, "Obtained slice", slice);

    BooleanFormula expectedFormula = fmgr.uninstantiate(toPathFormula(toCFA("int y; y = 0;"))
        .getFormula());

    assertThat(slice).isEqualTo(expectedFormula);
  }

  @Test public void slicingSimpleRearranged() throws Exception {
    PathFormula f = toPathFormula(toCFA("int x, y; y = 0; x = 1;"));
    logger.log(Level.INFO, "Sliced formula: ", f);

    PathFormula loop = toPathFormula(toCFA("int x; x++;"), f.getSsa());
    logger.log(Level.INFO, "Loop transition: ", loop);

    BooleanFormula slice = inductiveWeakeningManager.slice(f, loop);
    logger.log(Level.INFO, "Obtained slice", slice);

    BooleanFormula expectedFormula = fmgr.uninstantiate(toPathFormula(toCFA("int y; y = 0;"))
        .getFormula());

    assertThat(slice).isEqualTo(expectedFormula);

  }

  @Test public void testSlicingComplex() throws Exception {
    PathFormula input = toPathFormula(toCFA(
        "int x, y, p, nondet;",
        "x = 5;",
        "y = 10;",
        "if (nondet) {",
          "y = 100;",
          "p = 1;",
        "} else {",
          "p = 2;",
        "}"
    ));

    PathFormula loopTransition = toPathFormula(toCFA("int x; x += 1;"));

    BooleanFormula slice = inductiveWeakeningManager.slice(
        input, loopTransition);

    logger.log(Level.INFO, "Obtained slice", slice);
  }

  private PathFormula toPathFormula(CFA cfa) throws Exception {
    return toPathFormula(cfa, SSAMap.emptySSAMap());
  }

  private PathFormula toPathFormula(CFA cfa, SSAMap initialSSA) throws Exception {
    return TestDataTools.toPathFormula(cfa, initialSSA,
        fmgr, pfmgr, true);
  }

  private CFA toCFA(String... parts) throws Exception {
    return TestDataTools.toCFA(creator, parts);
  }
}

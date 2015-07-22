package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;

import java.util.Set;
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
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.collect.ImmutableMap;

public class LoopTransitionFinderTest {
  private CFACreator creator;
  private LogManager logger;
  private PathFormulaManager pfmgr;
  private FormulaManagerView fmgr;

  // todo: the CFA creation functionality
  // would come extremely handy at this point.
  // Where can we store it though?
  // maybe even outside of this package?
  // It has very specific hacks though -- P. wouldn't
  // be happy about that.
  // Can't make them static either though due to dependencies =n/

  @Before public void setUp() throws Exception {
    Configuration config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "cpa.predicate.handlePointerAliasing", "false"
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
  }

  @After public void tearDown() throws Exception {
    logger.flush();
  }

  // Hmm if we are at the stage where we are testing this class individually,
  // why not apply the large block encoding to the resulting graph then?
  @Test public void testGetEdgesInLoop() throws Exception {
    CFA cfa = TestDataTools.toCFA(creator,
        "int x = 0; int y = 0;",
        "while (1) {",
        // todo: gives strange result w/ x++; y++; which uses unnecessarily
        // uses temporary variables. Maybe we should ask Philipp?
        "x += 1; y += 1;",
        "}"
    );
    CFANode loopHead = cfa.getAllLoopHeads().get().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(cfa, pfmgr, fmgr, logger);

    PathFormula loopTransition = loopTransitionFinder.generateLoopTransition(loopHead);

    PathFormula expected = TestDataTools.toPathFormula(
        TestDataTools.toCFA(creator, "int x, y; x += 1; y += 1;"),
        SSAMap.emptySSAMap(), fmgr, pfmgr, true);
    assertThat(loopTransition.getFormula()).isEqualTo(expected.getFormula());
  }

  @Test public void test2() throws Exception {
    CFA cfa = TestDataTools.toCFA(creator,
        "int x = 0; int y = 0; int p = 1;",
        "while (1) {",
          "if (p) { x += 1; } else { y += 1; }",
        "}"
        // note: there is an "else" branch, under which both "x" and "y"
        // remain constant.
        // this is a problem, but probably unavoidable without applying LBE
        // properly -> this probably should be emailed to Philipp.
    );
    CFANode loopHead = cfa.getAllLoopHeads().get().iterator().next();
    LoopTransitionFinder loopTransitionFinder =
        new LoopTransitionFinder(cfa, pfmgr, fmgr, logger);
    Set<CFAEdge> out = loopTransitionFinder.getEdgesInSCC(loopHead);

    logger.log(Level.INFO, "Set of loop edges", out);

    PathFormula loopTransition = loopTransitionFinder.generateLoopTransition(loopHead);

    logger.log(Level.INFO, "Loop transition", loopTransition);
  }

}

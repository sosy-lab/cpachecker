package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import org.hamcrest.core.Is;
import org.hamcrest.core.IsNot;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.BasicLogManager;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.solver.SolverContextFactory.Solvers;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.IntegerFormulaManager;

import java.util.Set;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

@RunWith(Parameterized.class)
public class InductiveWeakeningManagerTest {
  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  private InductiveWeakeningManager inductiveWeakeningManager;
  private IntegerFormulaManager ifmgr;


  @Test public void testSlicingVerySimple() throws Exception {
    // TODO: SMTINTERPOL currently does not support solving with assumptions.
    Assume.assumeThat(solver, new IsNot<>(Is.is(Solvers.SMTINTERPOL)));
    SSAMap startingSsa = SSAMap.emptySSAMap().withDefault(0);
    PathFormula transition = new PathFormula(
        ifmgr.equal(
            ifmgr.makeVariable("x@1"),
            ifmgr.add(
                ifmgr.makeVariable("x@0"),
                ifmgr.makeNumber(1)
            )
        ),
        startingSsa.builder().setIndex(
            "x", CNumericTypes.INT, 1
        ).build(),
        PointerTargetSet.emptyPointerTargetSet(), 0
    );
    Set<BooleanFormula> lemmas = ImmutableSet.of(
        ifmgr.equal(
            ifmgr.makeVariable("x@0"), ifmgr.makeNumber(1)
        ),
        ifmgr.equal(
            ifmgr.makeVariable("y@0"), ifmgr.makeNumber(0)
        )
    );
    Set<BooleanFormula> weakening = inductiveWeakeningManager
        .findInductiveWeakeningForRCNF(startingSsa, transition, lemmas);
    assertThat(weakening).containsExactly(
        ifmgr.equal(
            ifmgr.makeVariable("y@0"), ifmgr.makeNumber(0)
        )
    );
  }

  @Before
  public void setUp() throws Exception {
    Configuration config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "solver.solver", solver.toString(),

            // Just to please Princess.
            "cpa.predicate.encodeFloatAs", "integer"
        )
    ).build();
    ShutdownNotifier notifier = ShutdownNotifier.createDummy();
    BasicLogManager logger = new BasicLogManager(config,
        new StreamHandler(System.out, new SimpleFormatter()));
    Solver solver = Solver.create(config, logger, notifier);
    FormulaManagerView fmgr = solver.getFormulaManager();
    inductiveWeakeningManager = new InductiveWeakeningManager(
        config, solver, logger, notifier);
    ifmgr = fmgr.getIntegerFormulaManager();
  }
}

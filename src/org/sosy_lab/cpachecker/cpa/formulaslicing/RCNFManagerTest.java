package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.solver.SolverContextFactory.Solvers;
import org.sosy_lab.solver.api.BooleanFormula;
import org.sosy_lab.solver.api.BooleanFormulaManager;
import org.sosy_lab.solver.test.SolverBasedTest0;

/**
 * Test the semi-CNF conversion.
 */
@RunWith(Parameterized.class)
public class RCNFManagerTest extends SolverBasedTest0{
  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  private RCNFManager RCNFManager;
  private BooleanFormulaManager bfmgr;

  @Before
  public void setUp() throws InvalidConfigurationException {
    Configuration d = Configuration.defaultConfiguration();
    FormulaManagerView mgrView = new FormulaManagerView(
        mgr, d, TestLogManager.getInstance());
    RCNFManager = new RCNFManager(mgrView, d);
    bfmgr = mgrView.getBooleanFormulaManager();
  }

  @Test
  public void testFactorization() throws Exception{
    BooleanFormula a = bfmgr.and(
        bfmgr.makeVariable("p"),
        bfmgr.makeVariable("a")
    );
    BooleanFormula b = bfmgr.and(
        bfmgr.makeVariable("p"),
        bfmgr.makeVariable("b")
    );
    BooleanFormula c = bfmgr.or(a, b);

    BooleanFormula converted = bfmgr.and(RCNFManager.toLemmas(c));
    assertThatFormula(converted).isEquivalentTo(c);
    assertThatFormula(converted).isEqualTo(
        bfmgr.and(
            bfmgr.makeVariable("p"),
            bfmgr.or(
                bfmgr.makeVariable("a"),
                bfmgr.makeVariable("b")
            )
        )
    );
  }

  @Test
  public void testNestedConjunctions() throws Exception {
    BooleanFormula input = bfmgr.and(
        bfmgr.makeVariable("a"),
        bfmgr.and(
            bfmgr.makeVariable("b"),
            bfmgr.and(
                bfmgr.makeVariable("c"),
                bfmgr.makeVariable("d")
            )
        )
    );
    BooleanFormula converted = bfmgr.and(RCNFManager.toLemmas(input));
    assertThatFormula(converted).isEquivalentTo(input);
    BooleanFormula expected =
        bfmgr.and(
            bfmgr.makeVariable("a"),
            bfmgr.makeVariable("b"),
            bfmgr.makeVariable("c"),
            bfmgr.makeVariable("d")
        );
    Truth.assertThat(bfmgr.toConjunctionArgs(converted, true)).isEqualTo(bfmgr.toConjunctionArgs(
        expected, true
    ));
  }

  @Test
  public void testExplicitExpansion() throws Exception {
    BooleanFormula input = bfmgr.or(
        bfmgr.and(ImmutableList.of(v("a"), v("b"), v("c"))),
        bfmgr.and(ImmutableList.of(v("d"), v("e"), v("f")))
    );
    BooleanFormula converted = bfmgr.and(RCNFManager.toLemmas(input));
    assertThatFormula(converted).isEquivalentTo(input);
    BooleanFormula expected =
        bfmgr.and(
            bfmgr.or(v("a"), v("d")),
            bfmgr.or(v("a"), v("e")),
            bfmgr.or(v("a"), v("f")),
            bfmgr.or(v("b"), v("d")),
            bfmgr.or(v("b"), v("e")),
            bfmgr.or(v("b"), v("f")),
            bfmgr.or(v("c"), v("d")),
            bfmgr.or(v("c"), v("e")),
            bfmgr.or(v("c"), v("f"))
        );
    Truth.assertThat(bfmgr.toConjunctionArgs(converted, true)).isEqualTo(bfmgr.toConjunctionArgs(
        expected, true
    ));
  }

  private BooleanFormula v(String name) {
    return bfmgr.makeVariable(name);
  }
}

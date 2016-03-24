package org.sosy_lab.cpachecker.cpa.formulaslicing;

import com.google.common.collect.ImmutableList;

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
public class SemiCNFManagerTest extends SolverBasedTest0{
  @Parameters(name = "{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  private SemiCNFManager semiCNFManager;
  private BooleanFormulaManager bfmgr;

  @Before
  public void setUp() throws InvalidConfigurationException {
    FormulaManagerView mgrView = new FormulaManagerView(
        mgr,
        Configuration.defaultConfiguration(),
        TestLogManager.getInstance());
    semiCNFManager = new SemiCNFManager(mgrView);
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

    BooleanFormula converted = semiCNFManager.convert(c);
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
    BooleanFormula converted = semiCNFManager.convert(input);
    assertThatFormula(converted).isEquivalentTo(input);
    assertThatFormula(converted).isEqualTo(
        bfmgr.and(
            ImmutableList.of(
                bfmgr.makeVariable("a"),
                bfmgr.makeVariable("b"),
                bfmgr.makeVariable("c"),
                bfmgr.makeVariable("d")
            )
        )
    );



  }
}

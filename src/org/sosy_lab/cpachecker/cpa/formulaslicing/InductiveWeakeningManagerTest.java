package org.sosy_lab.cpachecker.cpa.formulaslicing;

import static com.google.common.truth.Truth.assertThat;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.Solver;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
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

  @Test public void testSlicingComplex() throws Exception {

    BooleanFormula slice = inductiveWeakeningManager.slice(
        getSlicedTransition(), getLoopTransition());

    // todo: can we test things though? Or are all guarantees off with a
    // heuristical method?
    // One thing we should do: why not order things, so that we'll get something
    // at least as good as syntactic?
    // todo: Namely, make sure that syntactically different atoms are
    // processed last.
    logger.log(Level.INFO, "Obtained slice", slice);
    logger.flush();


    // First result: infinite loop =(.
  }

  private PathFormula getLoopTransition() throws Exception {
    // todo: this is somehow problematic.
    // in theory this should be done in the context of the same CFA
    // as the transition we have seen before.
    // otherwise it might not work to well.
    // Yet what if we re-declare "x" again? Would that still work?
    return toPathFormula(toCFA("int x; x++;"));
  }

  private PathFormula getSlicedTransition() throws Exception {
    return toPathFormula(toCFA(
        "int x = 5;",
        "int y = 10;",
        "int p;",
        "int nondet;",
        "int nondet2;",
        "if (nondet) {", "y = 100;", "p = 1;", "} else {", "p = 2;", "}"
    ));
  }
  private PathFormula toPathFormula(CFA cfa) throws Exception {
    return toPathFormula(cfa, SSAMap.emptySSAMap());
  }

  private PathFormula toPathFormula(CFA cfa, SSAMap initialSSA) throws Exception {
    Map<CFANode, PathFormula> mapping = new HashMap<>(cfa.getAllNodes().size());
    CFANode start = cfa.getMainFunction();

    PathFormula initial = new PathFormula(
        fmgr.getBooleanFormulaManager().makeBoolean(true), initialSSA,
        PointerTargetSet.emptyPointerTargetSet(),
        0
    );

    mapping.put(start, initial);
    Deque<CFANode> queue = new LinkedList<>();
    queue.add(start);

    while (!queue.isEmpty()) {
      CFANode node = queue.removeLast();
      Preconditions.checkState(!node.isLoopStart(),
          "Can only work on loop-free fragments");
      PathFormula path = mapping.get(node);

      for (CFAEdge e : CFAUtils.leavingEdges(node)) {
        CFANode toNode = e.getSuccessor();
        PathFormula old = mapping.get(toNode);

        PathFormula n;
        if (e instanceof CDeclarationEdge &&
            ((CDeclarationEdge) e).getDeclaration() instanceof CVariableDeclaration) {

          // Skip variable declaration edges.
          n = path;
        } else {
          n = pfmgr.makeAnd(path, e);
        }
        PathFormula out;
        if (old == null) {
          out = n;
        } else {
          out = pfmgr.makeOr(old, n);
          out = out.updateFormula(fmgr.simplify(out.getFormula()));
        }
        mapping.put(toNode, out);
        queue.add(toNode);
      }
    }

    PathFormula out = mapping.get(cfa.getMainFunction().getExitNode());
    out = out.updateFormula(fmgr.simplify(out.getFormula()));
    return out;
  }

  private CFA toCFA(String... parts) throws Exception {
    return creator.parseFileAndCreateCFA(getProgram(parts));
  }

  private String getProgram(String... parts) {
    return "int main() {" +  Joiner.on('\n').join(parts) + "}";
  }
}

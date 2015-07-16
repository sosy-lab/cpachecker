package org.sosy_lab.cpachecker.cpa.formulaslicing;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.junit.Before;
import org.junit.Test;
import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager.Tactic;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UnsafeFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.view.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.test.TestDataTools;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * TODO: Class Description
 */
public class FormulaSlicingTest {
  private CFACreator creator;
  private LogManager logger;
  private PathFormulaManager pfmgr;
  private FormulaManagerView fmgr;
  private BooleanFormulaManager bfmgr;
  private UnsafeFormulaManager ufmgr;
  
  @Before public void setUp() throws Exception {
    Configuration config = TestDataTools.configurationForTest().setOptions(
        ImmutableMap.of(
            "cpa.predicate.solver", "Z3",
            "log.consoleLevel", "FINE",

            // todo: just for easier debugging now.
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
    bfmgr = fmgr.getBooleanFormulaManager();
    ufmgr = factory.getFormulaManager().getUnsafeFormulaManager();
  }
  
  @Test public void blah() throws Exception {
    // todo: read in from the file instead.
    CFA cfa = toCFA(
        "int x = 5;",
        "int y = 10;",
        "int p;",
        "int nondet;",
        "int nondet2;",
        "if (nondet) {",
        "y = 100;",
        "p = 1;",
        "} else {",
        "p = 2;",
        "}"
    );
    logger.log(Level.INFO, "CFA = ", cfa);
    PathFormula pf = toPathFormula(cfa);
    logger.log(Level.INFO, "PathFormula = ", pf);
    BooleanFormula bf = pf.getFormula();
    BooleanFormula tseitin = bfmgr.applyTactic(bf, Tactic.CNF);
    logger.log(Level.INFO, "In tseitin: ", tseitin);

    BooleanFormula negated = bfmgr.not(tseitin);

    BooleanFormula negatedNNF = bfmgr.applyTactic(negated, Tactic.NNF);
    logger.log(Level.INFO, "Negated in NNF: ", negatedNNF);
    logger.flush();
  }

  /**
   * Aim: return a formula in CNF (conjunction over keys of the returned map).
   * Maps to true: inductive, maps to false: not really.
   */
  Map<BooleanFormula, Boolean> entryPoint(
      PathFormula input,
      List<CFAEdge> loopingEdges
  ) throws Exception {
    SSAMap ssa = input.getSsa();
    BooleanFormula f, cnf;

    f = input.getFormula();

    // todo: perform cheap existential quantifier elimination first.
    BooleanFormula noNonFinal = OverApproximationVisitor.bind(fmgr, ssa).visit(f);

    // Transform to CNF.
    cnf = bfmgr.applyTactic(f, Tactic.CNF);

    // Now we perform the same filtering step on CNF.
    Verify.verify(bfmgr.isAnd(cnf));
    List<BooleanFormula> clauses = new ArrayList<>();
    for (int i=0; i<ufmgr.getArity(cnf); i++) {
      clauses.add((BooleanFormula) ufmgr.getArg(cnf, i));
    }

    // todo: this preFiltering has to be done on the original formula,
    // which is not in the CNF encoding.
    final Map<BooleanFormula, Boolean> m = preFiltering(clauses, ssa);
    Set<BooleanFormula> invariantCandidates = Maps.filterKeys(m,
        new Predicate<BooleanFormula>() {
          public boolean apply(BooleanFormula input) {
            return m.get(input);
          }
        }).keySet();

    // Negation operates only on [invariantCandidates]


    return null;
  }


  /**
   * BooleanFormula (each clause) -> mapped to true (only has final vars)
   *                              -> false otherwise
   */
  private Map<BooleanFormula, Boolean> preFiltering(
      List<BooleanFormula> clauses,
      final SSAMap ssa)
      throws Exception {
    return Maps.toMap(clauses, new Function<BooleanFormula, Boolean>() {
      public Boolean apply(BooleanFormula input) {
        return hasOnlyFinalVars(input, ssa);
      }
    });
  }

  boolean hasOnlyFinalVars(BooleanFormula f, SSAMap ssa) {
    for (String s : fmgr.extractFunctionNames(f, true)) {
      Pair<String, Integer> p = FormulaManagerView.parseName(s);
      if (p.getSecond() != null &&
          p.getSecond() < ssa.getIndex(p.getFirst())) {
        return false;
      }
    }
    return true;
  }

  private PathFormula toPathFormula(CFA cfa) throws Exception {
    // todo: extract to some utility func.
    Map<CFANode, PathFormula> mapping = new HashMap<>(cfa.getAllNodes().size());
    CFANode start = cfa.getMainFunction();

    mapping.put(start, pfmgr.makeEmptyPathFormula());
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
        PathFormula n = pfmgr.makeAnd(path, e);
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

    return mapping.get(cfa.getMainFunction().getExitNode());
  }

  private CFA toCFA(String... parts) throws Exception {
    return creator.parseFileAndCreateCFA(getProgram(parts));
  }

  private String getProgram(String... parts) {
    return "int main() {" +  Joiner.on('\n').join(parts) + "}";
  }
}

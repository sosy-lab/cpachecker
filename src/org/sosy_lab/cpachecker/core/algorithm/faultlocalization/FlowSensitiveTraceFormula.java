package org.sosy_lab.cpachecker.core.algorithm.faultlocalization;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.FormulaContext;
import org.sosy_lab.cpachecker.core.algorithm.faultlocalization.formula.TraceFormula;
import org.sosy_lab.cpachecker.util.dependencegraph.DGNode;
import org.sosy_lab.cpachecker.util.dependencegraph.DependenceGraph;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.Formula;

/* Necessary run configuration
-setprop
cfa.createDependenceGraph=true
-setprop
controldeps.use=true
-setprop
flowdeps.use=false
 */
public class FlowSensitiveTraceFormula {

  private TraceFormula errorTrace;
  private Solver solver;
  private BooleanFormulaManager bmgr;
  private DependenceGraph graph;

  public static List<BooleanFormula> asList(
      FormulaContext pContext, TraceFormula pTraceFormula, CFA pCfa) throws InterruptedException {
    return new FlowSensitiveTraceFormula(pContext, pTraceFormula, pCfa).flowSensitiveTraceFormula();
  }

  private FlowSensitiveTraceFormula(FormulaContext pContext, TraceFormula pTraceFormula, CFA pCfa) {
    if (pCfa.getDependenceGraph().isPresent()) {
      graph = pCfa.getDependenceGraph().get();
    } else {
      Preconditions.checkState(
          pCfa.getDependenceGraph().isPresent(),
          "to use the improved version of the error invariants algorithm please enable cfa.createDependenceGraph with -setprop");
    }
    solver = pContext.getSolver();
    bmgr = solver.getFormulaManager().getBooleanFormulaManager();
    errorTrace = pTraceFormula;
  }

  private List<BooleanFormula> flowSensitiveTraceFormula() throws InterruptedException {

    Stack<BooleanFormula> conditions = new Stack<>();
    List<BooleanFormula> blockFormulas = new ArrayList<>();

    Collection<CFAEdge> dependencies = new HashSet<>();

    for (int i = 0; i < errorTrace.traceSize(); i++) {
      SSAMap currMap = errorTrace.getSsaMaps().get(i + 1);
      Set<Formula> variables =
          new HashSet<>(
              solver.getFormulaManager().extractVariables(errorTrace.slice(0, i + 1)).values());
      CFAEdge currEdge = errorTrace.getEdges().get(i);

      if (isIf(currEdge)) {
        conditions.push(errorTrace.getAtom(i));
        blockFormulas.add(bmgr.and(errorTrace.getAtom(i), frame(variables, currMap)));
      } else if (isEndIf(dependencies, currEdge)) {
        conditions.pop();
        blockFormulas.add(frame(variables, currMap));
      } else {
        blockFormulas.add(
            bmgr.implication(
                bmgr.and(conditions),
                transitionFormula(currEdge, errorTrace.getAtom(i), variables, currMap)));
      }
      dependencies = currentDependencies(currEdge);
    }

    return blockFormulas;
  }

  private boolean isIf(CFAEdge pEdge) {
    return pEdge.getEdgeType().equals(CFAEdgeType.AssumeEdge);
  }

  private Collection<CFAEdge> currentDependencies(CFAEdge pEdge) throws InterruptedException {

    List<DGNode> nodes = new ArrayList<>(graph.getAllNodes());
    List<CFAEdge> ignore = new ArrayList<>();
    for (int i = nodes.size() - 1; i >= 0; i--) {
      DGNode current = nodes.get(i);
      if (!errorTrace.getEdges().contains(current.getCfaEdge())) {
        ignore.add(nodes.remove(i).getCfaEdge());
      }
    }
    nodes.sort(Comparator.comparingInt(l -> errorTrace.getEdges().indexOf(l.getCfaEdge())));

    Collection<CFAEdge> assumes =
        graph.getReachable(pEdge, DependenceGraph.TraversalDirection.BACKWARD, ignore);
    assumes.removeIf(l -> !l.getEdgeType().equals(CFAEdgeType.AssumeEdge));

    return assumes;
  }

  private boolean isEndIf(Collection<CFAEdge> pDependencies, CFAEdge pEdge)
      throws InterruptedException {
    Collection<CFAEdge> dependenciesEdge = currentDependencies(pEdge);
    return !dependenciesEdge.containsAll(pDependencies);
  }

  private boolean isStatementEdge(CFAEdge pEdge) {
    CFAEdgeType edgeType = pEdge.getEdgeType();
    return edgeType.equals(CFAEdgeType.StatementEdge)
        || edgeType.equals(CFAEdgeType.DeclarationEdge);
  }

  private BooleanFormula frame(Set<Formula> pVariables, SSAMap pCurrent) {
    BooleanFormula frame = bmgr.makeTrue();
    for (Formula l : pVariables) {
      Formula x = solver.getFormulaManager().uninstantiate(l);
      x = solver.getFormulaManager().instantiate(x, pCurrent);

      String name = l.toString();
      if (name.contains("@")) {
        name = name.split("@")[0];
      }
      if (name.contains("!")) {
        name = name.split("!")[0];
      }

      SSAMap pNext =
          pCurrent
              .builder()
              .setIndex(name, pCurrent.getType(name), pCurrent.getIndex(name) + 1)
              .build();

      Formula x_prime = solver.getFormulaManager().uninstantiate(l);
      x_prime = solver.getFormulaManager().instantiate(x_prime, pNext);

      frame = bmgr.and(frame, solver.getFormulaManager().makeEqual(x, x_prime));
    }
    return frame;
  }

  private BooleanFormula transitionFormula(
      CFAEdge pEdge, BooleanFormula pFormula, Set<Formula> pVariables, SSAMap pCurrent) {
    Set<Formula> variables = new HashSet<>(pVariables);
    FormulaManagerView manager = solver.getFormulaManager();
    if (isIf(pEdge)) {
      return bmgr.and(pFormula, frame(variables, pCurrent));
    }
    if (isStatementEdge(pEdge)) {
      Map<String, Formula> extractedVariables =
          new HashMap<>(solver.getFormulaManager().extractVariables(pFormula));
      for (String s : solver.getFormulaManager().extractVariables(pFormula).keySet()) {
        if (s.contains("__VERIFIER_nondet")) {
          extractedVariables.remove(s);
        }
      }

      Formula left = null;
      if (extractedVariables.entrySet().size() == 1) {
        for (var lef : extractedVariables.values()) {
          left = lef;
          break;
        }
      } else {
        left = extractedVariables.get(pFormula.toString().split("` ")[1].split(" ")[0]);
      }

      String name = left.toString();
      if (name.contains("@")) {
        name = name.split("@")[0];
      }
      if (name.contains("!")) {
        name = name.split("!")[0];
      }

      Map<Formula, Formula> substitute = new HashMap<>();
      SSAMap next =
          pCurrent
              .builder()
              .setIndex(name, pCurrent.getType(name), pCurrent.getIndex(name) + 1)
              .build();
      substitute.put(left, manager.instantiate(manager.uninstantiate(left), next));
      return manager.makeAnd(manager.substitute(pFormula, substitute), frame(variables, pCurrent));
    }
    // TODO havoc is ignored
    return frame(pVariables, pCurrent);
  }

  /*  private SSAMap generatePlainMap(SSAMap lastMap, BooleanFormula formula){
    FormulaManagerView fmgr = solver.getFormulaManager();
    SSAMap plainMap = SSAMap.emptySSAMap();
    Map<String, Formula> variables = fmgr.extractVariables(formula);

    for(Map.Entry<String, Formula> e: variables.entrySet()){
      String name = e.getKey();
      if(name.contains("@")){
        name = name.split("@")[0];
      }
      if (name.contains("!")){
        name = name.split("!")[0];
      }
      plainMap = plainMap.builder().setIndex(name, lastMap.getType(name), 1).build();
    }
    return plainMap;
  }

  private BooleanFormula shiftFormula(SSAMap pCurrent, BooleanFormula pFormula, int primes){
    FormulaManagerView fmgr = solver.getFormulaManager();
    Map<String, Formula> variables = fmgr.extractVariables(pFormula);
    Map<Formula, Formula> substitute = new HashMap<>();
    for(Map.Entry<String, Formula> e: variables.entrySet()){
      String name = e.getKey();
      if(name.contains("@")){
        name = name.split("@")[0];
      }
      if (name.contains("!")){
        name = name.split("!")[0];
      }
      int shiftedIndex = pCurrent.getIndex(name) + primes;
      SSAMap adaptedMap = pCurrent.builder().setIndex(name, pCurrent.getType(name), shiftedIndex).build();
      Formula shifted = fmgr.instantiate(fmgr.uninstantiate(e.getValue()), adaptedMap);
      substitute.put(e.getValue(), shifted);
    }
    return fmgr.substitute(pFormula, substitute);
  }*/

}

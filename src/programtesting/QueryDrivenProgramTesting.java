/**
 *
 */
package programtesting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import cfa.objectmodel.CFAEdge;
import cfa.objectmodel.CFAFunctionDefinitionNode;

import compositeCPA.CompositeCPA;

import cpa.common.CPAAlgorithm;
import cpa.common.CompositeElement;
import cpa.common.automaton.Automaton;
import cpa.common.interfaces.AbstractElement;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.location.LocationCPA;
import cpa.scoperestriction.ScopeRestrictionCPA;
import cpa.symbpredabsCPA.SymbPredAbsCPA;
import cpa.testgoal.TestGoalCPA;
import exceptions.CPAException;

/**
 * @author Michael Tautschnig <tautschnig@forsyte.de>
 *
 */
public class QueryDrivenProgramTesting {

  public static void doIt (CFAFunctionDefinitionNode pMainFunction) {
    // create compositeCPA from automaton CPA and pred abstraction
    // TODO this must be a CPAPlus actually
    List<ConfigurableProgramAnalysis> cpas = new ArrayList<ConfigurableProgramAnalysis> ();
    try {
      cpas.add(new LocationCPA("sep", "sep"));
    } catch (CPAException e) {
      // for fixed values "sep", "sep" this is actually unreachable
      e.printStackTrace();
    }

    Automaton<CFAEdge> passing = new Automaton<CFAEdge>();
    cpas.add(new ScopeRestrictionCPA(passing));

    Automaton<CFAEdge> cover = new Automaton<CFAEdge>();
    cpas.add(new TestGoalCPA(cover));

    SymbPredAbsCPA predAbsCPA = new SymbPredAbsCPA("sep", "sep");
    cpas.add(predAbsCPA);
    ConfigurableProgramAnalysis cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
    CPAAlgorithm algo = new CPAAlgorithm();

    Set<Automaton<CFAEdge>.State> testGoals = cover.getFinalStates();

    while (!testGoals.isEmpty()) {
      // testGoals to be passed in as precision
      AbstractElement initialElement =
        cpa.getInitialElement(pMainFunction);
      Collection<AbstractElement> reached = null;
      try {
        reached = algo.CPA(cpa, initialElement);
      } catch (CPAException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
      //removeInfeasibleTestGoals(testGoals, reached);

      Set<ArrayList<CFAEdge>> paths = new HashSet<ArrayList<CFAEdge>>();
      for (AbstractElement e : reached) {
        CompositeElement comp = (CompositeElement)e;
        if (testGoals.isEmpty()) break;
        //if (!satisfiesTestGoal(testGoals, e)) continue;
        //if (predAbsCPA.isReachable(comp.get(2))) {
          testGoals.iterator().next().unsetFinal();
          //paths.add(e);
        //} else {
          // refine predicate precision
        //}
      }

      //runCBMC(paths);
    }
  }
}

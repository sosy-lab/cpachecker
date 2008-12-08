/**
 *
 */
package programtesting;

import java.util.ArrayList;
import java.util.List;

import cfa.objectmodel.CFAFunctionDefinitionNode;

import compositeCPA.CompositeCPA;

import cpa.common.CPAAlgorithm;
import cpa.common.interfaces.ConfigurableProgramAnalysis;
import cpa.location.LocationCPA;
import cpa.scoperestrictionautomaton.ScopeRestrictionAutomatonCPA;
import cpa.symbpredabsCPA.SymbPredAbsCPA;
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
    ScopeRestrictionAutomatonCPA sraCPA = new ScopeRestrictionAutomatonCPA(null, null);
    cpas.add(sraCPA);
    SymbPredAbsCPA predAbsCPA = new SymbPredAbsCPA("sep", "sep");
    cpas.add(predAbsCPA);
    ConfigurableProgramAnalysis cpa = CompositeCPA.createNewCompositeCPA(cpas, pMainFunction);
    CPAAlgorithm algo = new CPAAlgorithm();
    /*
    Set<AbstractElement> testGoals = new HashSet<AbstractElement>(sraCPA.getTestGoals());
    while (!testGoals.isEmpty()) {
      // testGoals to be passed in as precision
      AbstractElement initialElement =
        cpa.getInitialElement(mainFunction);
      Collection<AbstractElement> reached = algo.CPA(cpa, initialElement);
      removeInfeasibleTestGoals(testGoals, reached);

      Set<String> paths = new HashSet<String>();
      for (AbstractElement e : reached) {
        CompositeElement comp = (CompositeElement)e;
        if (testGoals.isEmpty()) break;
        if (!satisfiesTestGoal(testGoals, e)) continue;
        if (predAbsCPA.isReachable(comp.get(2))) {
          testGoals.remove(e);
          paths.add(e);
        } else {
          // refine predicate precision
        }
      }

      runCBMC(paths);
    }
    */
  }
}

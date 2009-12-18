package symbpredabstraction.bdd;

import java.util.HashMap;
import java.util.Map;

import cmdline.CPAMain;

import common.Pair;

import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;

public abstract class BDDAbstractFormulaManager implements AbstractFormulaManager {

  protected final JavaBDD bddManager;
  protected final boolean useCache;

  private final Map<Pair<AbstractFormula, AbstractFormula>, Boolean> entailsCache;
  
  private final AbstractFormula trueFormula;
  private final AbstractFormula falseFormula;
  
  public BDDAbstractFormulaManager() {
    bddManager = new JavaBDD();
    trueFormula = new BDDAbstractFormula(bddManager.getOne());
    falseFormula = new BDDAbstractFormula(bddManager.getZero());
    
    useCache = CPAMain.cpaConfig.getBooleanValue("cpas.symbpredabs.mathsat.useCache");
    if (useCache) {
      entailsCache = new HashMap<Pair<AbstractFormula, AbstractFormula>, Boolean>();
    } else {
      entailsCache = null;
    }
  }

  @Override
  public boolean entails(AbstractFormula f1, AbstractFormula f2) {
      // check entailment using BDDs: create the BDD representing
      // the implication, and check that it is the TRUE formula
      Pair<AbstractFormula, AbstractFormula> key = null;
      if (useCache) {
          key = new Pair<AbstractFormula, AbstractFormula>(f1, f2);
          if (entailsCache.containsKey(key)) {
              return entailsCache.get(key);
          }
      }
      int imp = bddManager.imp(((BDDAbstractFormula)f1).getBDD(),
                               ((BDDAbstractFormula)f2).getBDD());
      boolean yes = (imp == bddManager.getOne());
      if (useCache) {
          assert(key != null);
          entailsCache.put(key, yes);
      }
      return yes;
  }

  @Override
  public boolean isFalse(AbstractFormula f) {
    return ((BDDAbstractFormula)f).getBDD() == bddManager.getZero();
  }

  @Override
  public AbstractFormula makeTrue() {
    return trueFormula;
  }

  @Override
  public AbstractFormula makeFalse() {
    return falseFormula;
  }

}

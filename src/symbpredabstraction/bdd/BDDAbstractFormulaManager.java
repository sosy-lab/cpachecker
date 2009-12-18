package symbpredabstraction.bdd;

import java.util.HashMap;
import java.util.Map;

import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.Predicate;
import cmdline.CPAMain;

import common.Pair;
import common.Triple;

public abstract class BDDAbstractFormulaManager implements AbstractFormulaManager {

  /**
   * It's deprecated to use this field outside of this class.
   */
  @Deprecated
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

  @Override
  public AbstractFormula makeAnd(AbstractFormula pF1, AbstractFormula pF2) {
    BDDAbstractFormula f1 = (BDDAbstractFormula)pF1;
    BDDAbstractFormula f2 = (BDDAbstractFormula)pF2;
    
    return new BDDAbstractFormula(bddManager.and(f1.getBDD(), f2.getBDD()));
  }

  @Override
  public AbstractFormula makeNot(AbstractFormula pF) {
    BDDAbstractFormula f = (BDDAbstractFormula)pF;
    
    return new BDDAbstractFormula(bddManager.not(f.getBDD()));
  }

  @Override
  public AbstractFormula makeOr(AbstractFormula pF1, AbstractFormula pF2) {
    BDDAbstractFormula f1 = (BDDAbstractFormula)pF1;
    BDDAbstractFormula f2 = (BDDAbstractFormula)pF2;
    
    return new BDDAbstractFormula(bddManager.or(f1.getBDD(), f2.getBDD()));
  }
  
  @Override
  public Predicate createPredicate() {
    int bddVar = bddManager.createVar();
    int varIndex = bddManager.getVar(bddVar);
    BDDAbstractFormula bdd = new BDDAbstractFormula(bddVar);
    
    return new BDDPredicate(bdd, varIndex);
  }
  
  @Override
  public Triple<Predicate, AbstractFormula, AbstractFormula> getIfThenElse(AbstractFormula pF) {
    BDDAbstractFormula f = (BDDAbstractFormula)pF;
    
    int varIndex = bddManager.getVar(f.getBDD());
    BDDAbstractFormula bdd = new BDDAbstractFormula(bddManager.bddForVar(varIndex));
    BDDPredicate predicate = new BDDPredicate(bdd, varIndex);
    BDDAbstractFormula fThen = new BDDAbstractFormula(bddManager.getThen(f.getBDD()));
    BDDAbstractFormula fElse = new BDDAbstractFormula(bddManager.getElse(f.getBDD()));
    
    return new Triple<Predicate, AbstractFormula, AbstractFormula>(predicate, fThen, fElse);
  }
}

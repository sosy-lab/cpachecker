package symbpredabstraction.bdd;

import java.util.HashMap;
import java.util.Map;

import net.sf.javabdd.BDD;
import net.sf.javabdd.BDDFactory;
import symbpredabstraction.interfaces.AbstractFormula;
import symbpredabstraction.interfaces.AbstractFormulaManager;
import symbpredabstraction.interfaces.Predicate;

import common.Pair;
import common.Triple;
import cpa.common.CPAchecker;

/**
 * A wrapper for the javabdd (http://javabdd.sf.net) package.
 * 
 * This class is not thread-safe, but it could be easily made so by synchronizing
 * the {@link #createNewVar()} method.
 * 
 * TODO perhaps introduce caching for BDD -> BDDAbstractFormulas
 */
public class BDDAbstractFormulaManager implements AbstractFormulaManager {
  
  private final boolean useCache;
  private final Map<Pair<AbstractFormula, AbstractFormula>, Boolean> entailsCache;
  
  // static because init() may be called only once!
  private static final String BDD_PACKAGE = "cudd";
  private static final BDDFactory factory = BDDFactory.init(BDD_PACKAGE, 10000, 1000);
  
  private static final AbstractFormula trueFormula = new BDDAbstractFormula(factory.one());
  private static final AbstractFormula falseFormula = new BDDAbstractFormula(factory.zero());

  private static int nextvar = 0;
  private static int varcount = 100;
  {
    factory.setVarNum(varcount);
  }
  
  public BDDAbstractFormulaManager() {
    useCache = CPAchecker.config.getBooleanValue("cpas.symbpredabs.mathsat.useCache");
    if (useCache) {
      entailsCache = new HashMap<Pair<AbstractFormula, AbstractFormula>, Boolean>();
    } else {
      entailsCache = null;
    }
  }

  private static BDD createNewVar() {
    if (nextvar >= varcount) {
      varcount *= 1.5;
      factory.setVarNum(varcount);
    }
    BDD ret = factory.ithVar(nextvar++);

    return ret;
  }
  
  @Override
  public boolean entails(AbstractFormula pF1, AbstractFormula pF2) {
      // check entailment using BDDs: create the BDD representing
      // the implication, and check that it is the TRUE formula
      Pair<AbstractFormula, AbstractFormula> key = null;
      if (useCache) {
          key = new Pair<AbstractFormula, AbstractFormula>(pF1, pF2);
          if (entailsCache.containsKey(key)) {
              return entailsCache.get(key);
          }
      }
      BDDAbstractFormula f1 = (BDDAbstractFormula)pF1;
      BDDAbstractFormula f2 = (BDDAbstractFormula)pF2;
      BDD imp = f1.getBDD().imp(f2.getBDD());
      boolean yes = imp.isOne();
      if (useCache) {
          assert(key != null);
          entailsCache.put(key, yes);
      }
      return yes;
  }

  @Override
  public boolean isFalse(AbstractFormula f) {
    return ((BDDAbstractFormula)f).getBDD().isZero();
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
    
    return new BDDAbstractFormula(f1.getBDD().and(f2.getBDD()));
  }

  @Override
  public AbstractFormula makeNot(AbstractFormula pF) {
    BDDAbstractFormula f = (BDDAbstractFormula)pF;
    
    return new BDDAbstractFormula(f.getBDD().not());
  }

  @Override
  public AbstractFormula makeOr(AbstractFormula pF1, AbstractFormula pF2) {
    BDDAbstractFormula f1 = (BDDAbstractFormula)pF1;
    BDDAbstractFormula f2 = (BDDAbstractFormula)pF2;
    
    return new BDDAbstractFormula(f1.getBDD().or(f2.getBDD()));
  }
  
  @Override
  public Predicate createPredicate() {
    BDD bddVar = createNewVar();
    
    return new BDDPredicate(bddVar);
  }
  
  @Override
  public Triple<Predicate, AbstractFormula, AbstractFormula> getIfThenElse(AbstractFormula pF) {
    BDDAbstractFormula f = (BDDAbstractFormula)pF;
    
    int varIndex = f.getBDD().var();
    BDDPredicate predicate = new BDDPredicate(factory.ithVar(varIndex));
    BDDAbstractFormula fThen = new BDDAbstractFormula(f.getBDD().high());
    BDDAbstractFormula fElse = new BDDAbstractFormula(f.getBDD().low());
    
    return new Triple<Predicate, AbstractFormula, AbstractFormula>(predicate, fThen, fElse);
  }
}

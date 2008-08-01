package cpaplugin.cpa.cpas.symbpredabs.mathsat;

/**
 * A simple wrapper class for the BDD package, to be able to switch easily
 * between different implementation
 * @author alb
 */
public class JDD implements BDD {
    private jdd.bdd.BDD bddManager;
    private static final int INITIAL_BDD_NODE_SIZE = 10000;
    private static final int INITIAL_BDD_CACHE_SIZE = 1000;
    
    public JDD() {
        bddManager = 
            new jdd.bdd.BDD(INITIAL_BDD_NODE_SIZE, INITIAL_BDD_CACHE_SIZE);
    }
    
    public int ref(int bdd) {
        return bddManager.ref(bdd);
    }
    
    public void deref(int bdd) {
        bddManager.deref(bdd);
    }
    
    public int createVar() {
        return bddManager.createVar();
    }
    
    public int getVar(int bdd) {
        return bddManager.getVar(bdd);
    }
    
    public int getOne() {
        return bddManager.getOne();
    }
    
    public int getZero() {
        return bddManager.getZero();
    }
    
    public int getThen(int bdd) {
        return bddManager.getHigh(bdd);
    }
    
    public int getElse(int bdd) {
        return bddManager.getLow(bdd);
    }
    
    public int not(int bdd) {
        return bddManager.not(bdd);
    }
    
    public int and(int bdd1, int bdd2) {
        return bddManager.and(bdd1, bdd2);
    }
    
    public int or(int bdd1, int bdd2) {
        return bddManager.or(bdd1, bdd2);
    }
    
    public int imp(int bdd1, int bdd2) {
        return bddManager.imp(bdd1, bdd2);
    }
}

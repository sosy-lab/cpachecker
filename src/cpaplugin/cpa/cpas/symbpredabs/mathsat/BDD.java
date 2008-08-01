package cpaplugin.cpa.cpas.symbpredabs.mathsat;

/**
 * A simple wrapper class for the BDD package, to be able to switch easily
 * between different implementation
 * @author alb
 */
public interface BDD {
    public int ref(int bdd);
    
    public void deref(int bdd);
    
    public int createVar();
    
    public int getVar(int bdd);
    
    public int getOne();
    
    public int getZero();
    
    public int getThen(int bdd);
    
    public int getElse(int bdd);
    
    public int not(int bdd);
    
    public int and(int bdd1, int bdd2);
    
    public int or(int bdd1, int bdd2);
    
    public int imp(int bdd1, int bdd2);
}

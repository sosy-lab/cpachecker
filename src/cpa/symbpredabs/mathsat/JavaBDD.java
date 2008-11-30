package cpa.symbpredabs.mathsat;

import java.util.HashMap;
import java.util.Map;

/**
 * A wrapper for a generic BDD package. At the moment, only javabdd
 * (http://javabdd.sf.net) is supported
 *
 * For our purposes, BDDs are just integers. This class takes care of
 * converting between javabdd representation and ints
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */ 
public class JavaBDD {
    private Map<Integer, net.sf.javabdd.BDD> index2bdd;
    private Map<net.sf.javabdd.BDD, Integer> bdd2index;
    private net.sf.javabdd.BDDFactory factory;
    private int nextvar;
    private int varnum;
    private int nextindex;
    
    private static final String BDD_PACKAGE = "cudd";
    
    public JavaBDD() {
        index2bdd = new HashMap<Integer, net.sf.javabdd.BDD>();
        bdd2index = new HashMap<net.sf.javabdd.BDD, Integer>();
        factory = net.sf.javabdd.BDDFactory.init(BDD_PACKAGE, 10000, 1000);
        varnum = 100;
        factory.setVarNum(varnum);
        nextvar = 0;
        // put the zero and one bdds in the cache
        net.sf.javabdd.BDD zero = factory.zero();
        index2bdd.put(0, zero);
        bdd2index.put(zero, 0);
        net.sf.javabdd.BDD one = factory.one();
        index2bdd.put(1, one);
        bdd2index.put(one, 1);
        
        nextindex = 2;
    }

    private int toInt(net.sf.javabdd.BDD bdd) {
        if (bdd2index.containsKey(bdd)) {
            return bdd2index.get(bdd);
        } else {
            index2bdd.put(nextindex, bdd);
            bdd2index.put(bdd, nextindex);            
            return nextindex++;
        }
    }

    public int and(int bdd1, int bdd2) {
        assert(index2bdd.containsKey(bdd1));
        assert(index2bdd.containsKey(bdd2));
        
        net.sf.javabdd.BDD b1 = index2bdd.get(bdd1);
        net.sf.javabdd.BDD b2 = index2bdd.get(bdd2);
        
        return toInt(b1.and(b2));
    }
        
    public int createVar() {
        if (nextvar >= varnum) {
            varnum *= 1.5;
        }
        net.sf.javabdd.BDD ret = factory.ithVar(nextvar++);
        index2bdd.put(nextindex, ret);
        bdd2index.put(ret, nextindex);
        
        return nextindex++;
    }
    
    public int getOne() {
        return 1;
    }

    public int getZero() {
        return 0;
    }

    public int getVar(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        return b.var();
    }
    
    public int imp(int bdd1, int bdd2) {
        assert(index2bdd.containsKey(bdd1));
        assert(index2bdd.containsKey(bdd2));
        
        net.sf.javabdd.BDD b1 = index2bdd.get(bdd1);
        net.sf.javabdd.BDD b2 = index2bdd.get(bdd2);
        
        return toInt(b1.imp(b2));
    }

    public int not(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        
        return toInt(b.not());
    }
    
    public int or(int bdd1, int bdd2) {
        assert(index2bdd.containsKey(bdd1));
        assert(index2bdd.containsKey(bdd2));
        
        net.sf.javabdd.BDD b1 = index2bdd.get(bdd1);
        net.sf.javabdd.BDD b2 = index2bdd.get(bdd2);
        
        return toInt(b1.or(b2));
    }
   
    public int getElse(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        net.sf.javabdd.BDD e = b.low();
        
        return toInt(e);
    }
    
    public int getThen(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        net.sf.javabdd.BDD t = b.high();
        
        return toInt(t);
    }

    public int bddForVar(int var) {
        net.sf.javabdd.BDD v = factory.ithVar(var);
        return toInt(v);
    }
    
    public int ite(int var, int thenBdd, int elseBdd) {
        assert(index2bdd.containsKey(thenBdd));
        assert(index2bdd.containsKey(elseBdd));
        
        net.sf.javabdd.BDD t = index2bdd.get(thenBdd);
        net.sf.javabdd.BDD e = index2bdd.get(elseBdd);
        
        return toInt(factory.ithVar(var).ite(t, e));
    }
}

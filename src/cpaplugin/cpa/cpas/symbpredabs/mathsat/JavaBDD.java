package cpaplugin.cpa.cpas.symbpredabs.mathsat;

import java.util.HashMap;
import java.util.Map;

public class JavaBDD implements BDD {
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

    @Override
    public int and(int bdd1, int bdd2) {
        assert(index2bdd.containsKey(bdd1));
        assert(index2bdd.containsKey(bdd2));
        
        net.sf.javabdd.BDD b1 = index2bdd.get(bdd1);
        net.sf.javabdd.BDD b2 = index2bdd.get(bdd2);
        
        return toInt(b1.and(b2));
    }
    
    @Override
    public int createVar() {
        if (nextvar >= varnum) {
            varnum *= 1.5;
        }
        net.sf.javabdd.BDD ret = factory.ithVar(nextvar++);
        index2bdd.put(nextindex, ret);
        bdd2index.put(ret, nextindex);
        
        return nextindex++;
    }
    
    @Override
    public int getOne() {
        return 1;
    }

    @Override
    public int getZero() {
        return 0;
    }

    @Override
    public int getVar(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        return b.var();
    }

    @Override
    public int imp(int bdd1, int bdd2) {
        assert(index2bdd.containsKey(bdd1));
        assert(index2bdd.containsKey(bdd2));
        
        net.sf.javabdd.BDD b1 = index2bdd.get(bdd1);
        net.sf.javabdd.BDD b2 = index2bdd.get(bdd2);
        
        return toInt(b1.imp(b2));
    }

    @Override
    public int not(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        
        return toInt(b.not());
    }

    @Override
    public int or(int bdd1, int bdd2) {
        assert(index2bdd.containsKey(bdd1));
        assert(index2bdd.containsKey(bdd2));
        
        net.sf.javabdd.BDD b1 = index2bdd.get(bdd1);
        net.sf.javabdd.BDD b2 = index2bdd.get(bdd2);
        
        return toInt(b1.or(b2));
    }

    @Override
    public int getElse(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        net.sf.javabdd.BDD e = b.low();
        
//        assert(bdd2index.containsKey(e));
//        return bdd2index.get(e);
        return toInt(e);
    }
    
    @Override
    public int getThen(int bdd) {
        assert(index2bdd.containsKey(bdd));
        
        net.sf.javabdd.BDD b = index2bdd.get(bdd);
        net.sf.javabdd.BDD t = b.high();
        
//        assert(bdd2index.containsKey(t));
//        return bdd2index.get(t);
        return toInt(t);
    }

    @Override
    public int ref(int bdd) {
        return bdd;
    }

    @Override
    public void deref(int bdd) {
    }    
}

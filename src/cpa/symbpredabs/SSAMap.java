package cpa.symbpredabs;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import common.Pair;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * Maps a variable name to its latest "SSA index", that should be used when
 * referring to that variable
 */
public class SSAMap {
    private interface Key {}
    private class VarKey implements Key {
        private String name;

        public VarKey(String str) { name = str; }
        public String getName() { return name; }

        @Override
        public int hashCode() { return name.hashCode(); }
        @Override
        public boolean equals(Object o) {
            if (o instanceof VarKey) {
                return name.equals(((VarKey)o).name);
            } else if (o instanceof String) {
                return name.equals(o);
            }
            return false;
        }

        @Override
        public String toString() { return name; }
    }
    private class FuncKey implements Key {
        private String name;
        private SymbolicFormula[] args;

        public FuncKey(String n, SymbolicFormula[] a) {
            name = n;
            args = a;
        }
        public String getName() { return name; }
        public SymbolicFormula[] getArgs() { return args; }

        @Override
        public int hashCode() {
            int ret = name.hashCode();
            for (SymbolicFormula a : args) ret ^= a.hashCode();
            return ret;
        }
        @Override
        public boolean equals(Object o) {
            if (o instanceof FuncKey) {
                FuncKey f = (FuncKey)o;
                if (!name.equals(f.name)) return false;
                for (int i = 0; i < args.length; ++i) {
                    if (!args[i].equals(f.args[i])) return false;
                }
                return true;
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(name);
            buf.append("(");
            for (SymbolicFormula a : args) {
                buf.append(a.toString());
                buf.append(",");
            }
            buf.deleteCharAt(buf.length()-1);
            buf.append(")");
            return buf.toString();
        }
    }
    private Map<Key, Integer> repr = new HashMap<Key, Integer>();
    private static int nextSSAIndex = 1;

    /**
     * returns the index of the variable in the map
     */
    public int getIndex(String variable) {
        VarKey k = new VarKey(variable);
        if (repr.containsKey(k)) {
            return repr.get(k);
        } else {
            // no index found, return -1
            return -1;
        }
    }

    public void setIndex(String variable, int idx) {
        repr.put(new VarKey(variable), idx);
    }

    public int getIndex(String name, SymbolicFormula[] args) {
        FuncKey k = new FuncKey(name, args);
        if (repr.containsKey(k)) {
            return repr.get(k);
        } else {
            return -1;
        }
    }

    public void setIndex(String name, SymbolicFormula[] args, int idx) {
        repr.put(new FuncKey(name, args), idx);
    }

    public int getIndex(String name, SymbolicFormula arg) {
        SymbolicFormula[] args = {arg};
        return getIndex(name, args);
    }

    public void setIndex(String name, SymbolicFormula arg, int idx) {
        SymbolicFormula[] args = {arg};
        setIndex(name, args, idx);
    }

    /**
     * returns the next available global index. This method is not used anymore
     * (except in broken code :-) and should be removed.
     */
    public static int getNextSSAIndex() {
        return nextSSAIndex++;
    }

    public Collection<String> allVariables() {
        Vector<String> ret = new Vector<String>();
        ret.ensureCapacity(repr.size());
        for (Key k : repr.keySet()) {
            if (k instanceof VarKey) {
                ret.add(((VarKey)k).getName());
            }
        }
        return ret;
    }

    public Collection<Pair<String, SymbolicFormula[]>> allFunctions() {
        Vector<Pair<String, SymbolicFormula[]>> ret =
            new Vector<Pair<String, SymbolicFormula[]>>();
        ret.ensureCapacity(repr.size());
        for (Key k : repr.keySet()) {
            if (k instanceof FuncKey) {
                FuncKey kk = (FuncKey)k;
                Pair<String, SymbolicFormula[]> p =
                    new Pair<String, SymbolicFormula[]>(
                            kk.getName(), kk.getArgs());
                ret.add(p);
            }
        }
        return ret;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{ ");
        for (Key k : repr.keySet()) {
            buf.append(k);
            buf.append("@");
            buf.append(repr.get(k));
            buf.append(" ");
        }
        buf.append("}");
        return buf.toString();
    }

    /**
     * Explicit "copy constructor". I am not experienced enough with Java to
     * dare implementing a proper clone() :-)
     */
    public void copyFrom(SSAMap other) {
        for (Key k : other.repr.keySet()) {
            repr.put(k, other.repr.get(k));
        }
    }

    /**
     * updates this map with the contents of other. That is, adds to this map
     * all the variables present in other but not in this
     */
    public void update(SSAMap other) {
        for (Key k : other.repr.keySet()) {
            if (!repr.containsKey(k)) {
                repr.put(k, other.repr.get(k));
            }
        }
    }
}

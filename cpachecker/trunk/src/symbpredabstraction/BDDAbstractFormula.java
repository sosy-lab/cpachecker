package symbpredabstraction;

/**
 * Abstract formulas represented using BDDs
 *
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 */
public class BDDAbstractFormula implements AbstractFormula {
    private int bddRepr;

    public BDDAbstractFormula(int r) {
        bddRepr = r;
    }

    public int getBDD() {
        return bddRepr;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BDDAbstractFormula) {
            return bddRepr == ((BDDAbstractFormula)o).bddRepr;
        }
        return false;
    }
}

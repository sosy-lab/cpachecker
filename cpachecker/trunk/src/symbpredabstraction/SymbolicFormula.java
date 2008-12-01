package symbpredabstraction;

/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * A generic interface for manipulating symbolic representations of formulas.
 */
public interface SymbolicFormula {
    /**
     * checks whether this formula represents "true"
     * @return true if this formula represents logical truth, false otherwise
     */
    public boolean isTrue();

    /**
     * checks whether this formula represents "false"
     * @return true if this formula represents logical falsity, false otherwise
     */
    public boolean isFalse();
}

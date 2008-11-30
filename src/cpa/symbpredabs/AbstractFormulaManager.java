package cpa.symbpredabs;

import java.util.Collection;


/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * An AbstractFormulaManager is an object that knows how to create/manipulate
 * AbstractFormulas
 */
public interface AbstractFormulaManager {
    /**
     * @return a concrete representation of af
     * The formula returned is a "generic" version, not instantiated to any
     * particular "SSA step" (see SymbolicFormulaManager.instantiate()).
     */
    public SymbolicFormula toConcrete(SymbolicFormulaManager mgr,
                                      AbstractFormula af);
    /**
     * Computes the predicate abstraction of the given formula
     * @param mgr the manager for the symbolic formula
     * @param f the formula to abstract
     * @param ssa the SSAMap to use for resolving variable names
     * @param predicates the list of predicates
     * @return the predicate abstraction of f
     */
    public AbstractFormula toAbstract(SymbolicFormulaManager mgr,
            SymbolicFormula f, SSAMap ssa, Collection<Predicate> predicates);
    
    /**
     * checks whether the data region represented by f1
     * is a subset of that represented by f2
     * @param f1 an AbstractFormula
     * @param f2 an AbstractFormula
     * @return true if (f1 => f2), false otherwise
     */
    public boolean entails(AbstractFormula f1, AbstractFormula f2);
    
    /**
     * checks whether f represents "false"
     * @return true if f represents logical falsity, false otherwise
     */
    public boolean isFalse(AbstractFormula f);
    
    /**
     * @return a representation of logical truth
     */
    public AbstractFormula makeTrue();
    
}

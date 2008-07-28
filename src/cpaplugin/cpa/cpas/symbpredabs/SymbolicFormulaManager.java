package cpaplugin.cpa.cpas.symbpredabs;

import cpaplugin.cfa.objectmodel.CFAEdge;

/**
 * @author alb
 *
 * A SymbolicFormulaManager is an object that can create/manipulate 
 * SymbolicFormulas
 */
public interface SymbolicFormulaManager {
    /**
     * Creates a formula representing an AND of the two argument
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return (f1 & f2)
     */
    public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2);

    /**
     * Creates a formula representing an OR of the two argument
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return (f1 | f2)
     */
    public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2);
    
    /**
     * Creates a formula representing an AND of the two argument
     * @param f1 a SymbolicFormula
     * @param e a CFA edge
     * @param ssa the SSA map for resolving variables
     * @param updateSSA if true, update the given ssa in-place instead of 
     *                  building a new one
     * @param absoluteSSAIndices if true, use a unique index for each ssa 
     *                           variable, instead of keeping a separate index
     *                           for each var
     * @return (f1 & e)
     */
    public Pair<SymbolicFormula, SSAMap> makeAnd(SymbolicFormula f1, CFAEdge e, 
                                                 SSAMap ssa, boolean updateSSA, 
                                                 boolean absoluteSSAIndices)
        throws UnrecognizedCFAEdgeException;
        
    /**
     * @return a SymbolicFormula representing logical truth
     */
    public SymbolicFormula makeTrue();
    
    /**
     * @return a SymbolicFormula representing logical falsity
     */
    public SymbolicFormula makeFalse();

    /**
     * builds a formula that represents the necessary variable assignments
     * to "merge" the two ssa maps. That is, for every variable X that has two
     * different ssa indices i and j in the maps, creates a new formula 
     * (X_k = X_i) | (X_k = X_j), where k is a fresh ssa index.
     * Returns the formula described above, plus a new SSAMap that is the merge
     * of the two. 
     * 
     * @param ssa1 an SSAMap
     * @param ssa2 an SSAMap
     * @param absoluteSSAIndices if true, use a unique index for each ssa 
     *                           variable, instead of keeping a separate index
     *                           for each var
     * @return A pair (SymbolicFormula, SSAMap)
     */
    public Pair<Pair<SymbolicFormula, SymbolicFormula>, SSAMap> mergeSSAMaps(
            SSAMap ssa1, SSAMap ssa2, boolean absoluteSSAIndices);
    
    
    /**
     * checks whether this f1 logically entails f2
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return true if (f1 |= f2), false otherwise
     */
    public boolean entails(SymbolicFormula f1, SymbolicFormula f2);
    
    /**
     * Given a formula that uses "generic" variables, returns the corresponding
     * one that "instantiates" such variables according to the given SSA map.
     * This is used by AbstractFormulaManager.toConcrete()
     * 
     * @param f the generic SymbolicFormula to instantiate
     * @param ssa the SSAMap to use
     * @return a copy of f in which every "generic" variable is replaced by the
     * corresponding "SSA instance"
     */
    public SymbolicFormula instantiate(SymbolicFormula f, SSAMap ssa);
    
    /**
     * "shifts" forward all the variables in the formula f, of the amount
     * given by the input ssa. That is, variables x with index 1 in f will be
     * replaced by variables with index ssa.getIndex(x), vars with index 2 by
     * vars with index ssa.getIndex(x)+1, and so on. 
     * Returns the new formula and the ssa map with the final index for each
     * variable
     * @param f the SymbolicFormula to shift
     * @param ssa the SSAMap to use for shifting
     * @return the shifted formula and the new SSA map
     */
    public Pair<SymbolicFormula, SSAMap> shift(SymbolicFormula f, SSAMap ssa);
}

/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker. 
 *
 *  Copyright (C) 2007-2008  Dirk Beyer and Erkan Keremoglu.
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://www.cs.sfu.ca/~dbeyer/CPAchecker/
 */
package symbpredabstraction.interfaces;

import java.util.Collection;

import symbpredabstraction.PathFormula;
import symbpredabstraction.SSAMap;
import cfa.objectmodel.CFAEdge;

import common.Pair;

import exceptions.UnrecognizedCFAEdgeException;


/**
 * @author Alberto Griggio <alberto.griggio@disi.unitn.it>
 *
 * A SymbolicFormulaManager is an object that can create/manipulate
 * SymbolicFormulas
 */
public interface SymbolicFormulaManager {
  
    /**
     * Creates a formula representing a negation of the argument.
     * @param f a SymbolicFormula
     * @return (!f1)
     */
    public SymbolicFormula makeNot(SymbolicFormula f);
  
    /**
     * Creates a formula representing an AND of the two arguments.
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return (f1 & f2)
     */
    public SymbolicFormula makeAnd(SymbolicFormula f1, SymbolicFormula f2);

    /**
     * Creates a formula representing an OR of the two arguments.
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return (f1 | f2)
     */
    public SymbolicFormula makeOr(SymbolicFormula f1, SymbolicFormula f2);

    /**
     * Creates a formula representing an equivalence of the two arguments.
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return (f1 <-> f2)
     */
    public SymbolicFormula makeEquivalence(SymbolicFormula f1, SymbolicFormula f2);
    
    /**
     * Creates a formula representing "IF atom THEN f1 ELSE f2"
     * @param atom a SymbolicFormula
     * @param f1 a SymbolicFormula
     * @param f2 a SymbolicFormula
     * @return (IF atom THEN f1 ELSE f2)
     */
    
    public SymbolicFormula makeIfThenElse(SymbolicFormula atom,
        SymbolicFormula f1, SymbolicFormula f2);
    
    /**
     * Creates a formula representing an AND of the two argument.
     * @param f1 a SymbolicFormula
     * @param e a CFA edge
     * @param ssa the SSA map for resolving variables
     * @param absoluteSSAIndices if true, use a unique index for each ssa
     *                           variable, instead of keeping a separate index
     *                           for each var
     * @return The formula (f1 & e), and the new/updated SSAMap
     */
    public PathFormula makeAnd(SymbolicFormula f1, CFAEdge e,
                                                 SSAMap ssa,
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
    public boolean entails(SymbolicFormula f1, SymbolicFormula f2,
                           TheoremProver thmProver);

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
    public PathFormula shift(SymbolicFormula f, SSAMap ssa);

    /**
     * Extracts the atoms from the given formula. If uninstantiate is true,
     * then the atoms are in the "generic" form
     * @param f the formula to operate on
     * @param uninstantiate controls whether the returned atoms are still
     *                      instantiated or not
     * @param splitArithEqualities if true, return (x <= y) and (y <= x)
     *                             instead of (x = y)
     * @param conjunctionsOnly if true, don't extract atoms, but only top-level
     *                         conjuncts. For example, if called on:
     *                         a & (b | c), the result will be [a, (b | c)]
     *                         instead of [a, b, c]
     * @return a collection of (atomic) formulas
     */
    public Collection<SymbolicFormula> extractAtoms(SymbolicFormula f,
            boolean uninstantiate, boolean splitArithEqualities,
            boolean conjunctionsOnly);

    /**
     * Create string representation of a formula in a format which may be dumped
     * to a file.
     */
    public String dumpFormula(SymbolicFormula pT);

    /**
     * Create the variable representing a predicate for the given atom. There won't
     * be any tracking of the correspondence between the atom and the variable, 
     * if it is not done by the caller of this method.
     */
    public SymbolicFormula createPredicateVariable(SymbolicFormula pAtom);

}

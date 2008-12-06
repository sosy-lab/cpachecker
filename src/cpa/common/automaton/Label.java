/**
 * 
 */
package cpa.common.automaton;

/**
 * @author holzera
 *
 */
public interface Label<E> {

	/*
	 * @brief Checks whether e matches this label. Returns true if this is the case and false otherwise.
	 * 
	 */
	public boolean matches(E pE);
}

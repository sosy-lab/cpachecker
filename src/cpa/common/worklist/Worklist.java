/**
 * 
 */
package cpa.common.worklist;

import java.util.Enumeration;

/**
 * @author holzera
 *
 */
public interface Worklist<E> extends Enumeration<E> {
	/* 
	 * @brief Adds element to the worklist. 
	 */
	public void add(E element);
	
	/*
	 * @brief Removes oldElement from and inserts newElement into worklist.
	 */
	public void replace(E oldElement, E newElement);
}

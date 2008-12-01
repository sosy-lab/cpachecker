/**
 * 
 */
package cpa.common.worklist;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author holzera
 *
 * Worklist implementation for breadth first traversal.
 */
public class FIFOWorklist<E> implements Worklist<E> {
	private ArrayList<E> elements = new ArrayList<E>();
	
	public FIFOWorklist() {
		
	}
	
	public FIFOWorklist(E initialElement) {
		add(initialElement);
	}
	
	public FIFOWorklist(Collection<E> initialElements) {
		for (E initialElement : initialElements) {
			add(initialElement);
		}
	}
	
	/* (non-Javadoc)
	 * @see cpa.common.worklist.Worklist#add(java.lang.Object)
	 */
	@Override
	public void add(E element) {
		if (element == null) {
			throw new IllegalArgumentException();
		}
		
		elements.add(element);
	}

	/* (non-Javadoc)
	 * @see cpa.common.worklist.Worklist#replace(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void replace(E oldElement, E newElement) {
		if (oldElement == null) {
			throw new IllegalArgumentException();
		}
		
		// TODO: do we want a different replace behavior?
		elements.remove(oldElement);
		add(newElement);
	}

	/* (non-Javadoc)
	 * @see java.util.Enumeration#hasMoreElements()
	 */
	@Override
	public boolean hasMoreElements() {
		return !elements.isEmpty();
	}

	/* (non-Javadoc)
	 * @see java.util.Enumeration#nextElement()
	 */
	@Override
	public E nextElement() {
		E element = elements.get(0);
		elements.remove(0);
		
		return element;
	}
}

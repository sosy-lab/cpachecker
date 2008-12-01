/**
 * 
 */
package cpa.common.worklist;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author holzera
 *
 * Worklist implementation for depth first traversal.
 */
public class LIFOWorklist<E> implements Worklist<E> {
	private ArrayList<E> elements = new ArrayList<E>();
	
	public LIFOWorklist() {
		
	}
	
	public LIFOWorklist(E initialElement) {
		add(initialElement);
	}
	
	public LIFOWorklist(Collection<E> initialElements) {
		if (initialElements == null) {
			throw new IllegalArgumentException();
		}
		
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
		E element = elements.get(elements.size() - 1);
		elements.remove(elements.size() - 1);
		
		return element;
	}
}

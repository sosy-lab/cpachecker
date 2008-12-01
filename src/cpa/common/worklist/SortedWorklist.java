/**
 * 
 */
package cpa.common.worklist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * @author holzera
 *  
 * TODO Optimize storage and retrieval of elements (insertion sort?)!
 * 
 */
public class SortedWorklist<E> implements Worklist<E> {
	private Comparator<E> comparator;
	
	private ArrayList<E> elements = new ArrayList<E>();
	
	public SortedWorklist(Comparator<E> comparator) {
		if (comparator == null) {
			throw new IllegalArgumentException();
		}
		
		this.comparator = comparator;
	}
	
	public SortedWorklist(Comparator<E> comparator, E initialElement) {
		this(comparator);
		
		add(initialElement);
	}
	
	public SortedWorklist(Comparator<E> comparator, Collection<E> initialElements) {
		this(comparator);
		
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
		E currentElement = elements.get(0);
		
		int index = 0;
		
		for(int i = 1; i < elements.size(); i++){
			E currentTempElement = elements.get(i);
			
			if (comparator.compare(currentElement, currentTempElement) < 0) {
				currentElement = currentTempElement;
				index = i;
			}
		}

		elements.remove(index);
		
		return currentElement;
	}

}

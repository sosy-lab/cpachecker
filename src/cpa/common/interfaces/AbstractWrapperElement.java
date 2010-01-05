package cpa.common.interfaces;

public interface AbstractWrapperElement{

  public AbstractElement retrieveElementOfType(String pElementClass);
  public Iterable<AbstractElement> getWrappedElements();
  
}

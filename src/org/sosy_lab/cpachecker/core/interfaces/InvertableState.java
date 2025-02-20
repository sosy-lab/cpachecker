package org.sosy_lab.cpachecker.core.interfaces;

public interface InvertableState<T extends AbstractState> {
  public T flip();
}

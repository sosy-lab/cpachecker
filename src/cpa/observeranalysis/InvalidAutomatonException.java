package cpa.observeranalysis;

import exceptions.CPAException;

class InvalidAutomatonException extends CPAException {

  private static final long serialVersionUID = 4881083051895812266L;

  public InvalidAutomatonException(String msg) {
    super(msg);
  }
}

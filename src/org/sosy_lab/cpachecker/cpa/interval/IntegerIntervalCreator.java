
package org.sosy_lab.cpachecker.cpa.interval;

public class IntegerIntervalCreator implements Creator {

    @Override
    public NumberInterface factoryMethod(Object o) {

        return new IntegerInterval((Long) o);
    }

}

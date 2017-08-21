
package org.sosy_lab.cpachecker.cpa.interval;

public class CreatorIntegerInterval implements Creator {

    @Override
    public NumberInterface factoryMethod(Object o) {

        return new IntegerInterval((Long) o);
    }

}

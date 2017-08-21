
package org.sosy_lab.cpachecker.cpa.interval;

public class CreatorDoubleInterval implements Creator {

    @Override
    public NumberInterface factoryMethod(Object o) {
        return new DoubleInterval((Double) o);
    }

}

package org.sosy_lab.cpachecker.cpa.invariants;

import static junit.framework.Assert.assertNotNull;
import static org.sosy_lab.cpachecker.cpa.invariants.SimpleInterval.*;

import java.math.BigInteger;

import org.junit.Test;

public class SimpleIntervalTest {

  @Test
  public void testConstruction() {
    assertNotNull(singleton(BigInteger.ZERO));
    assertNotNull(singleton(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(singleton(BigInteger.valueOf(Long.MIN_VALUE)));
    
    assertNotNull(lessOrEqual(BigInteger.ZERO));
    assertNotNull(lessOrEqual(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(lessOrEqual(BigInteger.valueOf(Long.MIN_VALUE)));
    
    assertNotNull(greaterOrEqual(BigInteger.ZERO));
    assertNotNull(greaterOrEqual(BigInteger.valueOf(Long.MAX_VALUE)));
    assertNotNull(greaterOrEqual(BigInteger.valueOf(Long.MIN_VALUE)));
    
    assertNotNull(of(BigInteger.ZERO, BigInteger.ZERO));
    assertNotNull(of(BigInteger.ZERO, BigInteger.ONE));
    assertNotNull(of(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE)));
  }

  @Test(expected=IllegalArgumentException.class)
  public void testInvalidConstruction1() {
    of(BigInteger.ONE, BigInteger.ZERO);
  }
  
  @Test(expected=IllegalArgumentException.class)
  public void testInvalidConstruction2() {
    of(BigInteger.valueOf(Long.MAX_VALUE), BigInteger.valueOf(Long.MIN_VALUE));
  }
}

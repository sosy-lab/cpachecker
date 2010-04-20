package fllesh.fql.fllesh.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

public class CillyTest {

  @Before
  public void tearDown() {
    /* XXX: Currently this is necessary to pass all assertions. */
    cpa.common.CPAchecker.logger = null;
  }

  @Test
  public void test001() throws IOException {
    Cilly lCilly = new Cilly();
    
    lCilly.cillyfy("test/programs/simple/functionCall.c");
  }
  
  @Test
  public void test002() throws IOException {
    Cilly lCilly = new Cilly();
    
    assertFalse(lCilly.isCillyInvariant("test/programs/simple/functionCall.c"));
  }
  
  @Test
  public void test003() throws IOException {
    Cilly lCilly = new Cilly();
    
    File lCillyfiedFile = lCilly.cillyfy("test/programs/simple/functionCall.c");
    
    System.out.println(lCillyfiedFile);
    
    assertTrue(lCilly.isCillyInvariant(lCillyfiedFile));
  }
  
  @Test
  public void test004() throws IOException {
    assertEquals("test/programs/simple/functionCall.cil.c", Cilly.getNiceCILName("test/programs/simple/functionCall.c"));
  }
  
}

package fql.fllesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

public class CillyTest {

  @Test
  public void test001() throws IOException {
    Cilly lCilly = new Cilly();
    
    lCilly.cillyfy("test/tests/single/functionCall.c");
  }
  
  @Test
  public void test002() throws IOException {
    Cilly lCilly = new Cilly();
    
    assertFalse(lCilly.isCillyInvariant("test/tests/single/functionCall.c"));
  }
  
  @Test
  public void test003() throws IOException {
    Cilly lCilly = new Cilly();
    
    File lCillyfiedFile = lCilly.cillyfy("test/tests/single/functionCall.c");
    
    System.out.println(lCillyfiedFile);
    
    assertTrue(lCilly.isCillyInvariant(lCillyfiedFile));
  }
  
  @Test
  public void test004() throws IOException {
    assertEquals("test/tests/single/functionCall.cil.c", Cilly.getNiceCILName("test/tests/single/functionCall.c"));
  }
  
}

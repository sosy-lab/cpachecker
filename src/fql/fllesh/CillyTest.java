package fql.fllesh;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;

public class CillyTest {

  @Test
  public void test001() throws IOException {
    // TODO remove absolute path
    //Cilly lCilly = new Cilly("/home/holzera/cil/cil/obj/x86_LINUX/cilly.asm.exe");
    Cilly lCilly = new Cilly();
    
    lCilly.cillyfy("test/tests/single/functionCall.c");
  }
  
  @Test
  public void test002() throws IOException {
    // TODO remove absolute path
    //Cilly lCilly = new Cilly("/home/holzera/cil/cil/obj/x86_LINUX/cilly.asm.exe");
    Cilly lCilly = new Cilly();
    
    assertFalse(lCilly.isCillyInvariant("test/tests/single/functionCall.c"));
  }
  
  @Test
  public void test003() throws IOException {
    // TODO remove absolute path
    //Cilly lCilly = new Cilly("/home/holzera/cil/cil/obj/x86_LINUX/cilly.asm.exe");
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

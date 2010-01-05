package fql.frontend.parser;

import java.io.StringReader;

import org.junit.Test;


public class FQLParserTest {
  
  // TODO Use Michael's test suite, too.
  
  @Test
  public void testFQLParserScanner001() throws Exception {
    String lInput = "COVER STATES(@BASICBLOCKENTRY)";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }

  @Test
  public void testFQLParserScanner002() throws Exception {
    String lInput = "COVER STATES(@CONDITIONEDGE)";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
  
  @Test
  public void testFQLParserScanner003() throws Exception {
    String lInput = "COVER STATES(@DECISIONEDGE)";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
  
  @Test
  public void testFQLParserScanner004() throws Exception {
    String lInput = "COVER STATES(@CONDITIONGRAPH)";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
  
  @Test
  public void testFQLParserScanner005() throws Exception {
    String lInput = "COVER STATES(ID)";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
  
  @Test
  public void testFQLParserScanner006() throws Exception {
    String lInput = "COVER STATES(COMPLEMENT(@BASICBLOCKENTRY))";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
  
  @Test
  public void testFQLParserScanner007() throws Exception {
    String lInput = "COVER EDGES(INTERSECT(@BASICBLOCKENTRY, @CONDITIONEDGE))";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
  
  @Test
  public void testFQLParserScanner008() throws Exception {
    String lInput = "COVER EDGES(UNION(@BASICBLOCKENTRY, @CONDITIONEDGE))";
    
    System.out.println(lInput);
    
    FQLParser lParser = new FQLParser(new FQLLexer(new StringReader(lInput)));
    
    System.out.println(lParser.parse().value);
  }
}

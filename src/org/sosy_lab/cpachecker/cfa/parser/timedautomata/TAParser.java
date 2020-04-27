package org.sosy_lab.cpachecker.cfa.parser.timedautomata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.tree.ParseTree;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaGrammarParser;
import org.sosy_lab.cpachecker.cfa.parser.timedautomata.generated.TaLexer;
import org.sosy_lab.cpachecker.exceptions.ParserException;

/**
 * Parser for a markup language for timed automata. 
 */
class TAParser implements Parser {

    private final LogManager logger;
    private final Timer parseTimer = new Timer();
    private final Timer cfaCreationTimer = new Timer();
  
    public TAParser(final LogManager pLogger) {
      logger = pLogger;
    }

    @Override
    public ParseResult parseFile(String pFilename)
            throws ParserException, IOException, InterruptedException {
        
        logger.log(Level.INFO, "Start parsing timed automaton...");
        try(var input = Files.newInputStream(Paths.get(pFilename))) {
            parseTimer.start();
            TaLexer lexer = new TaLexer(CharStreams.fromStream(input));
            lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
            lexer.addErrorListener(ParserErrorListener.INSTANCE);

            CommonTokenStream tokens = new CommonTokenStream(lexer);

            TaGrammarParser parser = new TaGrammarParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(ParserErrorListener.INSTANCE);
            ParseTree tree = parser.specification();

            TCFABuilder builder = new TCFABuilder();

            parseTimer.stop();
            cfaCreationTimer.start();
            builder.visit(tree);
            
            var nodes = builder.getNodesByAutomatonMap();
            var entryNodes = builder.getEntryNodesByAutomatonMap();

            cfaCreationTimer.stop();

            List<Path> input_file = ImmutableList.of(Paths.get(pFilename));
            return new ParseResult(entryNodes, nodes, new ArrayList<>(), input_file);
        }
    }

    @Override
    public ParseResult parseString(String pFilename, String pCode)
            throws ParserException, InterruptedException {
        return null;
    }

    @Override
    public Timer getParseTime() {
      return parseTimer;
    }
  
    @Override
    public Timer getCFAConstructionTime() {
      return cfaCreationTimer;
    }

}
package org.nulist.plugin.parser;

import com.grammatech.cs.project;
import com.grammatech.cs.result;
import io.shiftleft.fuzzyc2cpg.ast.AstNode;
import io.shiftleft.fuzzyc2cpg.parser.TokenSubStream;
import io.shiftleft.fuzzyc2cpg.parser.functions.AntlrCFunctionParserDriver;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.nulist.plugin.model.channel.ChannelBuilder;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.Parser;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @ClassName FuzzyParser
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/5/19 4:48 PM
 * @Version 1.0
 **/
public class FuzzyParser implements Parser {

    private LogManager logger=null;
    private MachineModel machineModel;
    public ChannelBuilder channelBuilder;
    public final static String channel = "Channel";


    public FuzzyParser(final LogManager pLogger, final MachineModel pMachineModel, Map<String, CFABuilder> lteBuilders){
        logger = pLogger ;
        machineModel = pMachineModel;
        channelBuilder = new ChannelBuilder(lteBuilders, channel);
    }

    @Override
    public Timer getParseTime() {
        return null;
    }

    @Override
    public Timer getCFAConstructionTime() {
        return null;
    }

    @Override
    public ParseResult parseFile(String filename) throws ParserException {


        return null;
    }

    @Override
    public ParseResult parseString(String filename, String code) throws ParserException {
        return null;
    }

    public void parseChannelModel(String filename){
        File file = new File(filename);

        try {
            if(file.isDirectory()){
                File[] files = file.listFiles();
                for(File f:files){
                    if(f.getName().endsWith(".c")){
                        CharStream inputStream = CharStreams.fromPath(f.toPath());
                        AntlrCFunctionParserDriver driver = new AntlrCFunctionParserDriver();
                        Lexer lexer = driver.createLexer(inputStream);
                        TokenSubStream functionTokens = new TokenSubStream(lexer);
                        driver.parseAndWalkTokenStream(functionTokens);
                        channelBuilder.putDriver(f.getPath(),driver);
                    }
                }
                channelBuilder.parseFile();
            }else {
                CharStream inputStream = CharStreams.fromPath(file.toPath());
                AntlrCFunctionParserDriver driver = new AntlrCFunctionParserDriver();
                Lexer lexer = driver.createLexer(inputStream);
                TokenSubStream functionTokens = new TokenSubStream(lexer);
                driver.parseAndWalkTokenStream(functionTokens);
                filename = filename.replace(".c","").split("/")[filename.split("/").length-1];
                channelBuilder.parseBuildFile(filename,driver);
            }

        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }



    public CFABuilder getChannelBuilder() {
        return channelBuilder.channelBuilder;
    }
}

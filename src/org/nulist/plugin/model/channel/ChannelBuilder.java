package org.nulist.plugin.model.channel;

import com.grammatech.cs.result;
import io.shiftleft.fuzzyc2cpg.parser.TokenSubStream;
import io.shiftleft.fuzzyc2cpg.parser.functions.AntlrCFunctionParserDriver;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.nulist.plugin.parser.CFABuilder;
import org.sosy_lab.cpachecker.cfa.ParseResult;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * @ClassName ChannelBuilder
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 5/5/19 5:06 PM
 * @Version 1.0
 **/
public class ChannelBuilder {

    public Map<String, CFABuilder> builderMap;
    public  AntlrCFunctionParserDriver driver;

    public ChannelBuilder (Map<String, CFABuilder> builderMap, AntlrCFunctionParserDriver driver){
        this.builderMap = builderMap;
        this.driver = driver;
    }

    public CFABuilder parseBuildFile(AntlrCFunctionParserDriver driver,String buildName) throws result {

        CFABuilder cfaBuilder=new CFABuilder(null, MachineModel.LINUX64,buildName);

        driver.builderStack.peek();




        return cfaBuilder;
    }

}

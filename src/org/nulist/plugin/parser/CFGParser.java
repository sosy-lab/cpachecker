package org.nulist.plugin.parser;

import com.grammatech.cs.compunit;
import com.grammatech.cs.project;
import com.grammatech.cs.project_compunits_iterator;
import com.grammatech.cs.result;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.exceptions.ParserException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName CFGParser
 * @Description CFG parse. Given a project, CFGParser extract its all user-defined compunits to construct cfa
 * @Author Yinbo Yu
 * @Date 2/27/19 3:42 PM
 * @Version 1.0
 **/
public class CFGParser implements Parser{

    private final LogManager logger;
    private final CFABuilder cfaBuilder;

    private final Timer parseTimer = new Timer();
    private final Timer cfaCreationTimer = new Timer();

    public CFGParser(final LogManager pLogger, final MachineModel pMachineModel){
        logger = pLogger ;
        cfaBuilder = new CFABuilder(logger, pMachineModel);
    }

    @Override
    public ParseResult parseFile(String filename) throws ParserException, IOException, InterruptedException {
        return null;
    }

    @Override
    public ParseResult parseString(String filename, String code) throws ParserException {
        return null;
    }

    public ParseResult parseProject(project project) throws ParserException, result {
        List<Path> input_file = new ArrayList<>();
        parseTimer.start();

        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();
            // only focus on user-defined c files
            if(!cu.is_user())
                continue;
            input_file.add(Paths.get(cu.normalized_name()));
            cfaBuilder.build(cu);
        }

        return new ParseResult(cfaBuilder.functions,
                cfaBuilder.cfaNodes,
                cfaBuilder.globalDeclarations,
                input_file);

    }



    @Override
    public Timer getCFAConstructionTime() {
        return cfaCreationTimer;
    }

    @Override
    public Timer getParseTime() {
        return parseTimer;
    }
}

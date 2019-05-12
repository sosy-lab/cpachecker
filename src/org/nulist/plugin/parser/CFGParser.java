package org.nulist.plugin.parser;

import com.grammatech.cs.*;
import org.nulist.plugin.model.ChannelBuildOperation;
import org.nulist.plugin.model.ITTIModelAbstract;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.*;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.nulist.plugin.model.ChannelBuildOperation.*;
import static org.nulist.plugin.model.action.ITTIAbstract.extendSuffix;

/**
 * @ClassName CFGParser
 * @Description CFG parse. Given a project, CFGParser extract its all user-defined compunits to construct cfa
 * @Author Yinbo Yu
 * @Date 2/27/19 3:42 PM
 * @Version 1.0
 **/
public class CFGParser implements Parser{

    private LogManager logger=null;
    private MachineModel machineModel;
    public final static String ENB = "OAI-ENB";
    public final static String MME = "OAI-MME";
    public final static String UE = "OAI-UE";
    public final static String Channel = "Channel";

    private final Timer parseTimer = new Timer();
    private final Timer cfaCreationTimer = new Timer();


    public CFGParser(){

    }

    public CFGParser(final LogManager pLogger, final MachineModel pMachineModel){
        logger = pLogger ;
        machineModel = pMachineModel;
    }

    @Override
    public ParseResult parseFile(String filename) {
        return null;
    }

    @Override
    public ParseResult parseString(String filename, String code) {
        return null;
    }

    public CFABuilder parseBuildProject(project project) throws result {
        parseTimer.start();
        String projectName = project.name();
        CFABuilder cfaBuilder=new CFABuilder(logger,machineModel,projectName);


        //first step: traverse for building all functionEntry node
        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();
            // only focus on user-defined c files
//            input_file.add(Paths.get(cu.normalized_name()));
//            cfaBuilder.basicBuild(cu);
            if(filter(cu.name(), projectName) ||cu.is_library_model())
            {
                cfaBuilder.basicBuild(cu, project.name());
            }
        }
        System.out.println("Fisrt Traverse complete!");

        //second step: traverse for building intra and inter CFA

        for(project_compunits_iterator cu_it = project.compunits();
            !cu_it.at_end(); cu_it.advance() )
        {
            compunit cu = cu_it.current();

            if(filter(cu.name(),projectName))
            {
                //input_file.add(Paths.get(cu.normalized_name()));
                System.out.println(cu.name());
                cfaBuilder.build(cu);
            }
        }

        //third step: abstract functions
        //insert nas_user_container_t *users as the global variable

        if(projectName.equals(UE)){
            procedure createTasksUE = project.find_procedure(CREATE_TASKS_UE);
            if(createTasksUE!=null)
                ChannelBuildOperation.generateCreateTasksUE(cfaBuilder,createTasksUE);
        }

        if(projectName.equals(ENB)){
            procedure createTasksUE = project.find_procedure(CREATE_TASKS);
            if(createTasksUE!=null)
                ChannelBuildOperation.generatCreateTasksENB(cfaBuilder,createTasksUE);
        }

        procedure proc = project.find_procedure(ITTI_ALLOC_NEW_MESSAGE);
        if(proc!=null)
            ChannelBuildOperation.generateITTI_ALLOC_NEW_MESSAGE(cfaBuilder,proc);
//        proc = project.find_procedure(ITTI_SEND_MSG_TO_TASKS);
//        procedure proc1 = project.find_procedure(ITTI_SEND_MSG_TO_TASKS+extendSuffix);
//        if(proc!=null)
//            ChannelBuildOperation.generateITTI_SEND_TO_TASK(cfaBuilder,proc, proc1);

        parseTimer.stop();
        return cfaBuilder;
    }



    public ParseResult parseProject(project project) throws result {
        CFABuilder cfaBuilder = parseBuildProject(project);

        return new ParseResult(cfaBuilder.functions,
                cfaBuilder.cfaNodes,
                cfaBuilder.getGlobalVariableDeclarations(),
                cfaBuilder.parsedFiles);
    }

    private static boolean filter(String path, String projectName){
        return fileFilter(path, projectName);
    }

    public static boolean targetFile(String path, String projectName){
        return path.endsWith("nas_ue_task.c");
    }

    public static boolean isProjectMainFunction(String filePath, String projectName){
        if(projectName.equals(ENB))
            return filePath.endsWith("targets/RT/USER/lte-softmodem.c");
        else if(projectName.equals(UE))
            return filePath.endsWith("targets/RT/USER/lte-uesoftmodem.c");
        else if(projectName.equals(MME))
            return filePath.endsWith("openair-cn/src/oai_mme/oai_mme.c");
        else
            return true;
    }



    public static boolean fileFilter(String name, String projectName){
        return (name.contains("RRC_Rel14/LTE_") && (projectName.equals(UE) || projectName.equals(ENB))) || //AS application protocol interfaces between UE and ENB: radio resource control
                (name.contains("S1AP_R14/S1AP_") && projectName.equals(ENB)) || //application protocol interfaces between MME and ENB: UE context management
                //(name.contains("X2AP_R14") && projectName.equals(ENB)) || //application protocol interfaces between enbs for handover (UE mobility) and/or self organizing network related function:
                (name.contains("openair2/RRC") && (projectName.equals(UE) || projectName.equals(ENB))) || //
                ((name.contains("openair2/LAYER2/MAC/config.c")||name.contains("openair2/LAYER2/MAC/main.c")) &&  projectName.equals(ENB)) || //
                ((name.contains("openair2/LAYER2/MAC/config_ue.c")||name.contains("openair2/LAYER2/MAC/main_ue.c")) &&  projectName.equals(UE)) || //
                name.contains("openair2/COMMON") ||
                (name.contains("openair2/ENB_APP") && !name.contains("flexran") && !name.contains("NB_IoT")  && projectName.equals(ENB)) ||
                name.contains("common/utils/channel") ||
                name.endsWith("common/utils/ocp_itti/intertask_interface.h") ||
                name.contains("openair2/LAYER2/PDCP_v10.1.0/pdcp.c") ||
                (name.contains("targets/RT/USER/lte-ue.c") && projectName.equals(UE)) ||
                (name.contains("targets/RT/USER/lte-uesoftmodem.c") && projectName.equals(UE)) ||
                (name.contains("targets/RT/USER/lte-enb.c") && projectName.equals(ENB)) ||
                (name.contains("targets/RT/USER/lte-softmodem.c") && projectName.equals(ENB)) ||
                name.contains("targets/RT/USER/lte-softmodem-common.c") ||
                (name.contains("openair3/S1AP") && projectName.equals(ENB)) ||
                (name.contains("openair3/NAS/UE") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/TOOLS") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/API") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/EMM") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/ESM") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/IES") && projectName.equals(UE)) ||
                (name.contains("openair3/NAS/COMMON/ESM") && projectName.equals(UE)) ||
                ((name.contains("openair3/NAS/COMMON/UTIL/nas_timer")||
                        name.contains("openair3/NAS/COMMON/UTIL/OctetString") ||
                        name.contains("openair3/NAS/COMMON/UTIL/parser") ||
                        name.contains("openair3/NAS/COMMON/UTIL/TLVDecoder") ||
                        name.contains("openair3/NAS/COMMON/UTIL/TLVEncoder")) && projectName.equals(UE)) ||
                name.contains("openair3/COMMON") ||
                name.contains("openair3/UTILS") ||
                (name.contains("openair-cn/src/nas") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/s1ap/s1ap_") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/mme") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/oai_mme") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/mme_app") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/common") &&
                        !name.endsWith("intertask_interface.c") &&
                        !name.endsWith("intertask_interface_dump.c") && projectName.equals(MME)) ||
                (name.contains("openair-cn/src/utils") && !name.contains("openair-cn/src/utils/log") && projectName.equals(MME)) ||
                (name.contains("CMakeFiles/r10.5") && projectName.equals(MME));//s1ap
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

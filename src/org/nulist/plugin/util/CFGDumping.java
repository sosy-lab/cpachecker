package org.nulist.plugin.util;

import com.grammatech.cs.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.nulist.plugin.util.ClassTool.getUnsignedInt;

/**
 * @ClassName CFGDumping
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 2/25/19 4:25 PM
 * @Version 1.0
 **/
public class CFGDumping {

    /**
     * @Description //TODO
     * @Param [p]
     * @return int
     **/
    public static int getLineNumber(point p){

        try {
            sfileinst_line_pair slp = p.file_line();
            return  (int)slp.get_second();
        }catch (result r){
            System.out.println("Uncaught exception: "+ r);
        }

        return 0;
    }

    /**
     * @Description //TODO
     * @Param [source, ces]
     * @return java.lang.String
     **/
    public static String dump_cfg_edge_dot(point p, cfg_edge_set ces, int mode){
        StringBuilder builder = new StringBuilder();
        try {
            for( cfg_edge_set_iterator ce_it = ces.cbegin();
                 !ce_it.at_end();
                 ce_it.advance() )
            {
                cfg_edge ce = ce_it.current();

                switch (mode){
                    case 0://intra-source
                        builder.append(ce.get_first().hashCode()).append(" ->").append(p.hashCode());
                        builder.append(" [style=filled,");
                        break;
                    case 1://intra-target
                        builder.append(p.hashCode()).append(" ->").append(ce.get_first().hashCode());
                        builder.append(" [style=filled,");
                        break;
                    case 2://inter-source
                        builder.append(dump_vertex_dot(ce.get_first()));
                        builder.append(ce.get_first().hashCode()).append(" ->").append(p.hashCode());
                        builder.append(" [style=bold,");
                        break;
                    case 3://inter-target
                        builder.append(dump_vertex_dot(ce.get_first()));
                        builder.append(p.hashCode()).append(" ->").append(ce.get_first().hashCode());
                        builder.append(" [style=bold,");
                        break;
                }
                builder.append(" color=blue, label=\"").
                        append(ce.get_second().name()).append("\"];\n");
            }
        }catch (result r){
            System.out.println("Uncaught exception: "+ r);
        }

        return builder.toString();
    }
    /**
     * @Description //TODO
     * @Param [p]
     * @return java.lang.String
     **/
    public static String dump_vertex_dot(point p){
        StringBuilder builder = new StringBuilder();
        try {
            builder.append(p.hashCode()).append(" [label=\"");
            builder.append("[LineNo] @");
            try {
                builder.append(p.file_line().get_second()).append("\\l");
            }catch (result r){
                builder.append("\\l");
            }


            builder.append("[Kind] ").append(p.get_kind().name()).append("\\l");
            try {
                String character = p.get_ast(ast_family.getC_NORMALIZED()).pretty_print();
                character =character.replace('"','\'');
                builder.append("[Char] ").append(character).append("\\l");
            }catch (result r){
                if(!r.equals(result.getSUCCESS()) && !r.equals(result.getTRUNCATED())){
                    builder.append("[Char] ").append("<NOCHAR>\\l");
                }
                //throw r;
            }
            builder.append("\"];\n");

        }catch (result r){
            System.out.println("Uncaught exception: "+r);
        }
        return builder.toString();
    }

    /**
     * @Description //TODO
     * @Param [proc]
     * @return void
     **/
    public static void dumpCFG2Dot(procedure proc, String path){

        try {
            point_set points;
            String filePath = path+proc.name()+"-cfg.dot";
            System.out.println("Dump CFG of "+proc.name());
            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("digraph cluster").append(getUnsignedInt(proc.hashCode()))
                    .append(" {\n");
            stringBuilder.append("label=\"Function Name: ").append(proc.name()).append("\";\n");
            stringBuilder.append("style=filled;\n");
            stringBuilder.append("color=lightgrey;\n");
            try{
                points = proc.points();
            } catch( result r) {
                // the points method raises this exception when
                // invoked on undefined functions
                //if( r.equals(result.getPDG_IS_UNDEFINED()) )
                throw r;
            }
            //points = proc.points();

            for( point_set_iterator point_it = points.cbegin();
                 !point_it.at_end();
                 point_it.advance() )
            {
                point p = point_it.current();
                stringBuilder.append(dump_vertex_dot(p));
                cfg_edge_set ces = p.cfg_targets();//intra
                if(!ces.empty()){
                    stringBuilder.append(dump_cfg_edge_dot(p,ces,1));
                }
            }
            stringBuilder.append("}\n");
            for( point_set_iterator point_it = points.cbegin();
                 !point_it.at_end();
                 point_it.advance() )
            {
                point p = point_it.current();
                cfg_edge_set ces = p.cfg_inter_targets();//inter
                if(!ces.empty()){
                    stringBuilder.append(dump_cfg_edge_dot(p,ces,3));
                }
            }

            FileWriter fileWriter = new FileWriter(new File(filePath));
            fileWriter.write(stringBuilder.toString());
            fileWriter.flush();
            fileWriter.close();

        }catch (result r){
            System.out.println("Uncaught exception: "+ r);
        }catch (IOException e){
            System.out.println("Failed to write the file into disk with exception: "+e);
        }

    }
}

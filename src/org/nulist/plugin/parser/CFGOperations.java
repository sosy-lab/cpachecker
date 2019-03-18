/**
 * @ClassName CFGOperations
 * @Description TODO
 * @Author Yinbo Yu
 * @Date 3/10/19 12:41 PM
 * @Version 1.0
 **/
package org.nulist.plugin.parser;

import com.grammatech.cs.cfg_edge_vector;
import com.grammatech.cs.result;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

public class CFGOperations {

    public static cfg_edge_vector sortVectorByLineNo(cfg_edge_vector cfgEdgeVector)throws result {
        cfg_edge_vector edgeVector = new cfg_edge_vector();
        Map<Long, Integer> lineMap = new HashMap<>();
        for(int i=0;i<cfgEdgeVector.size();i++)
            lineMap.put(cfgEdgeVector.get(i).get_first().file_line().get_second(),i);
        TreeSet<Long> treeSet = new TreeSet<>(lineMap.keySet());
        treeSet.comparator();
        for(Long i:treeSet)
            edgeVector.add(cfgEdgeVector.get(lineMap.get(i)));
        return edgeVector;
    }
}

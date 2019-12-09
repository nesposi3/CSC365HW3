package com.nesposi3;

import com.nesposi3.Utils.CacheUtils;
import com.nesposi3.Utils.ClusteringUtils;

public class Loader {



    public static void  main(String[] args){
        try {
            CacheUtils.initialize();
            CacheUtils.initializeGraph();
            ClusteringUtils.kMedioids();
            CacheUtils.numDisjointSets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

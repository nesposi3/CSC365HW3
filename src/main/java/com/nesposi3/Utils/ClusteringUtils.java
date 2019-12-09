package com.nesposi3.Utils;

import com.nesposi3.BTree;
import com.nesposi3.Cluster;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import static com.nesposi3.Utils.BTreeUtils.BTREE_FOLDER_NAME;
import static org.apache.commons.codec.digest.DigestUtils.md5;

public class ClusteringUtils {
    private static final int NUM_CLUSTERS= 40;
    private static final int SWAP_ITERATION = 50;
    private static final String CLUSTER_LOCATION = "storage/clusters/";
    /**
     * Convert a string into a long (64 bit) hash code
     * Takes the lower 64 bits of a md5 hash
     * @param input
     * @return
     */
    public static long stringHash64(String input){
        byte[] totalMd5 = md5(input);
        byte[] longMd5 = new byte[8];
        for (int i = 0; i <8 ; i++) {
            longMd5[i] = totalMd5[i];
        }
        ByteBuffer buffer = ByteBuffer.wrap(longMd5);
        return buffer.getLong();
    }

    /**
     * This function reforms a k-medioids analysis on the cached btrees
     * @throws IOException
     */
    public static void kMedioids()throws IOException {
        HashMap<BTree, String> bTreeMap = new HashMap<>();
        HashMap<String,HashMap<Long,Integer>> bTreeFreqMap = new HashMap<>();
        // Get all btrees from the folder, add them to the map with String name
        File dir = new File(BTREE_FOLDER_NAME);
        File[] files = dir.listFiles();
        Cluster[] clusters = new Cluster[NUM_CLUSTERS];
        for (int i = 0; i < files.length ; i++) {
            BTree tree = new BTree(BTREE_FOLDER_NAME  + files[i].getName());
            bTreeMap.put(tree,files[i].getName());
            bTreeFreqMap.put(tree.getFileName(),tree.getKeyFreqMap());
        }

        // create blank cluster with given id
        for (int i = 0; i <NUM_CLUSTERS ; i++) {
            Cluster cluster = new Cluster(i);
            clusters[i] = cluster;
        }

        // Assign initial medioids to the clusters
        for (int i = 0; i <NUM_CLUSTERS ; i++) {
            clusters[i].setMedioid(files[i*10].getName());
            System.out.println("Initital Mediod: " + files[i*10].getName() );
        }
        // At every medioid, add some members to it based on who is the closest
        for (int i = 0; i <clusters.length ; i++) {
            Cluster cluster = clusters[i];
            BTree medioid = new BTree(BTREE_FOLDER_NAME+cluster.medioid);
            HashMap<Long,Integer> medMap = medioid.getKeyFreqMap();
            int numMembers = 0;
            while(numMembers<10){
                BTree closest = null;
                double closestValue = Double.MIN_VALUE;
                for(BTree x:bTreeMap.keySet()){
                    double similarity = cosineSimilarity(bTreeFreqMap.get(x.getFileName()),medMap);
                    if(similarity > closestValue){
                        closestValue = similarity;
                        closest = x;
                    }
                }
                cluster.addMember(bTreeMap.get(closest));
                System.out.println(bTreeMap.get(closest));
                numMembers++;
                bTreeMap.remove(closest);
            }

        }

        // Try to swap and make total cost for n iterations
        for (int i = 0; i < clusters.length; i++) {
            Cluster currentCluster = clusters[i];
            String[] members = currentCluster.getMembers();
            double totalcost=0;
            BTree medtree =  new BTree(BTREE_FOLDER_NAME + currentCluster.getMedioid());
            HashMap<Long,Integer> medMap = bTreeFreqMap.get(medtree.getFileName());
            for(String member:members){
                BTree tree = new BTree(BTREE_FOLDER_NAME + member);
                HashMap<Long,Integer> treeMap = bTreeFreqMap.get(tree.getFileName());
                totalcost += cosineSimilarity(medMap,treeMap);
            }
            int j = 0;
            while(j<SWAP_ITERATION && j<members.length){
                double trialCost = 0;
                BTree newMed = new BTree(BTREE_FOLDER_NAME + members[j]);
                HashMap<Long,Integer> trialMedioidMap = bTreeFreqMap.get(newMed.getFileName());
                for (String member: members) {
                    BTree tree = new BTree(BTREE_FOLDER_NAME + member);
                    HashMap<Long,Integer> trialNeighborMap = bTreeFreqMap.get(tree.getFileName());
                    trialCost += cosineSimilarity(trialMedioidMap,trialNeighborMap);
                }
                if(trialCost<totalcost){
                    String oldMedioid = currentCluster.getMedioid();
                    currentCluster.setMedioid(members[j]);
                    currentCluster.removeMember(members[j]);
                    currentCluster.addMember(oldMedioid);
                }
                j++;
            }
            currentCluster.writeToDisk();
            System.out.println("Final: "+currentCluster.toString());
        }
    }
    public static String findClosestTree(String url)throws ParseException, IOException{
        HashMap<BTree, String> bTreeMap = new HashMap<>();

        // Get all btrees from the folder, add them to the map with String name
        File dir = new File(BTREE_FOLDER_NAME);
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length ; i++) {
            BTree tree = new BTree(BTREE_FOLDER_NAME  + files[i].getName());
            bTreeMap.put(tree,files[i].getName());
        }

        Document doc = CacheUtils.getWebsiteDocument(url);
        BTree newTree = CacheUtils.writeDocToBtree(CacheUtils.generateFileName(url),doc,true);
        return SimilarityUtils.findClosest(newTree,bTreeMap);
    }
    public static Cluster findClosestCluster(String url){
        File dir = new File(CLUSTER_LOCATION);
        File[] files = dir.listFiles();
        Cluster[] clusters = new Cluster[files.length];
        try{
            Document doc = CacheUtils.getWebsiteDocument(url);
            BTree newTree = CacheUtils.writeDocToBtree(CacheUtils.generateFileName(url),doc,true);
            double closestNumber = Double.MIN_VALUE;
            int closestIndex = -1;
            for (int i = 0; i <files.length ; i++) {
                Cluster c = new Cluster(Files.readAllBytes(files[i].toPath()));
                clusters[i] = c;
                BTree medioid = new BTree(BTREE_FOLDER_NAME + c.medioid);
                double result = newTree.cosineSimilarity(medioid);
                System.out.println(c.medioid + result);
                if(result>closestNumber){
                    closestIndex = i;
                    closestNumber = result;
                }
            }
            return clusters[closestIndex];
        }catch (IOException e){
            e.printStackTrace();
            return null;
        } catch (ParseException e) {
            return null;
        }
    }
    public static String findClosestTfIdf(String url)throws ParseException, IOException{
        HashMap<BTree, String> bTreeMap = new HashMap<>();

        // Get all btrees from the folder, add them to the map with String name
        File dir = new File(BTREE_FOLDER_NAME);
        File[] files = dir.listFiles();
        for (int i = 0; i < files.length ; i++) {
            BTree tree = new BTree(BTREE_FOLDER_NAME  + files[i].getName());
            bTreeMap.put(tree,files[i].getName());
        }

        Document doc = CacheUtils.getWebsiteDocument(url);
        BTree newTree = CacheUtils.writeDocToBtree(CacheUtils.generateFileName(url),doc,true);
        return SimilarityUtils.findMostSimilarTfIdf(newTree,bTreeMap);
    }
    private static double cosineSimilarity(HashMap<Long,Integer> currMap,HashMap<Long,Integer> otherMap){
        double top = 0;
        double bottomA =0;
        double bottomB = 0;
        for(Map.Entry<Long,Integer> entry:currMap.entrySet()){
            int a = entry.getValue().intValue();
            int b =0;
            if(otherMap.containsKey(entry.getKey())){
                b = otherMap.get(entry.getKey());
            }
            top += (a * b);
            bottomA += (a*a);
        }
        for(Map.Entry<Long,Integer> entry:otherMap.entrySet()){
            int b = entry.getValue().intValue();
            bottomB +=(b*b);
        }
        double bottom = ((Math.sqrt(bottomA)) * (Math.sqrt(bottomB)));
        return top/bottom;
    }

}

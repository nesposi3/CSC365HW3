package com.nesposi3.Utils;

import com.nesposi3.BTree;
import com.nesposi3.Cluster;
import com.nesposi3.GraphNode;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CacheUtils {
    public static final String DIRECTORY_NAME = "storage/btrees/";
    public final static String BASE_URI = "https://en.wikipedia.org";
    public static final String URL_BEGINNING = "httpsenwikipediaorgwiki";
    /**
     * Removes special characters from the input string
     *
     * @param url The string to be transformed
     * @return
     */
    public static String generateFileName(String url) {
        String removePunctPattern = "[\\.\\/:]";
        return url.replaceAll(removePunctPattern, "");
    }

    /**
     * Goes through control file and adds files to cache based on links from those files
     *
     * @throws IOException
     * @throws ParseException
     */
    public static void initialize() throws IOException, ParseException {
        File links = new File("links.txt");
        Scanner file = new Scanner(links);
        //This pattern excludes all files, special wikipedia pages, and disambiguation pages
        Pattern urlPattern = Pattern.compile("\\/wiki\\/((?!((Wikipedia:)|(File:)|(Help:))).)*(?<!(_\\(disambiguation\\)))");
        while (file.hasNextLine()) {
            String line = file.nextLine();
            Document doc = (getWebsiteDocument(line));
            Elements linkElements = doc.select("a");
            int numLinks = 0;
            int i = 0;
            writeDocToBtree(generateFileName(line),doc,true);
            while (numLinks < 50 && i < linkElements.size()) {
                Element e = linkElements.get(i);
                i++;
                String link = (e.attr("href"));
                Matcher m = urlPattern.matcher(link);
                if (m.matches()) {
                    numLinks++;
                    String flink = BASE_URI + (e.attr("href"));
                    Document linkedDoc = getWebsiteDocument(flink);
                    writeDocToBtree(generateFileName(flink),linkedDoc,true);
                }
            }
        }
        file.close();
    }
    public static void initializeGraph() throws IOException, ParseException{
        File dir = new File("storage/html");
        File[] files = dir.listFiles();
        System.out.println(files.length);
        Pattern urlPattern = Pattern.compile("\\/wiki\\/((?!((Wikipedia:)|(File:)|(Help:)|(Special:)|(Template:))).)*(?<!(_\\(disambiguation\\)))");
        for (int i = 0; i <files.length ; i++) {
            //Strips the .html from the filename
            String strippedUrl = files[i].getName().substring(0,files[i].getName().length()-5);
            Document doc = getDocFromFile(strippedUrl);
            Elements linkElements = doc.select("a");
            BTree root = writeDocToBtree(strippedUrl,doc,false);
            GraphNode g = new GraphNode(doc.title(),strippedUrl);
            for (Element e: linkElements
                 ) {
                String link = (e.attr("href"));
                Matcher m = urlPattern.matcher(link);
                if(m.matches()){
                    String flink = BASE_URI + (e.attr("href"));
                    Document linkedDoc = getDocFromFile(generateFileName(flink));
                    if(linkedDoc!=null){
                        //We have this page in our graph
                        BTree linked = writeDocToBtree(generateFileName(flink),linkedDoc,false);
                        double similarity = (1.0 - root.cosineSimilarity(linked));
                        g.addLinked(generateFileName(flink),similarity);
                    }
                }
            }
            g.writeToDisk();
            System.out.println(g.toString());
        }
    }
    public static void numDisjointSets() throws IOException{
        HashMap<String,Integer> nameToIndexMap = new HashMap<>();
        GraphNode[] nodes = getAllNodes();
        int[] arr = new int[nodes.length];
        for (int i = 0; i <nodes.length ; i++) {
            nameToIndexMap.put(nodes[i].getUrl(),i);
            arr[i] = i;
        }

        for (int i = 0; i <nodes.length ; i++) {
            GraphNode curNode = nodes[i];
            String[] children = curNode.getChildren();
            for (int j = 0; j <children.length ; j++) {
                int arrIndex = nameToIndexMap.get(children[j]);
                arr[arrIndex] = i;
            }
        }
        eliminateCycles(arr);
        flattenArray(arr);
        HashSet<Integer> numSets = new HashSet<>();
        for (int i = 0; i <arr.length ; i++) {
            numSets.add(arr[i]);
        }
        System.out.println(numSets.size());
    }

    /**
     * Given an array in Union-Find form,
     * @param arr
     */
    private static void flattenArray(int[] arr){
        for (int i = 0; i <arr.length ; i++) {
            find(arr,i);
        }
    }
    private static int find(int[] arr,int i){
        if(arr[i]!=i){
            arr[i] = find(arr,arr[i]);
        }
        return  arr[i];
    }

    /**
     * Takes a union-find formatted array and eliminates cycles
     * @param arr
     */
    private static void eliminateCycles(int[] arr){
        for (int i = 0; i <arr.length ; i++) {
            cycleFlatten(arr,i);
        }
    }

    /**
     * This method flattens from index i
     * @param arr
     * @param i
     */
    private static void cycleFlatten(int[] arr, int i){
        HashSet<Integer> numSeen = new HashSet<>();
        numSeen.add(i);
        int j = arr[i];
        // Because there always exists an nth level parent of a node, we know that this loop will terminate
        while (j!=arr[j]){
            // If we have seen arr[j] before, we know we have a cycle, set arr[j] to j
            if(numSeen.contains(arr[j])){
                arr[j] = j;
                Integer[] nums = numSeen.toArray(new Integer[0]);
                for (int num: nums
                     ) {
                    arr[num] = j;
                }
            }
            numSeen.add(j);
            j = arr[j];
        }
    }

    /**
     * Takes in a url, creates and stores an html file from the url
     * Checks when files stored in cache were last updated, if later than web, redownload
     *
     * @param url The url for the website to be downloaded
     * @return The jsoup Document created by the method
     * @throws IOException
     * @throws ParseException
     */
    public static Document getWebsiteDocument(String url) throws IOException, ParseException {
        String fileName = generateFileName(url);
        File f = new File("storage/html/" + fileName + ".html");
        if (f.exists()) {
            //file exists, check when local file last modified
            long localMod = f.lastModified();
            // Check when website was last modified
            Connection.Response conn = Jsoup.connect(url).execute();
            String dString = conn.header("Last-Modified");
            // Pattern based on format of HTTP last modified header
            SimpleDateFormat format = new SimpleDateFormat("EEE',' dd MMM YYYY HH':'mm':'ss zz");
            System.out.println(dString);
            Date date = format.parse(dString);
            long webMod = date.getTime();
            // If website modified after local, get page again
            if (webMod > localMod) {
                f.delete();
                f.createNewFile();
                Document doc = Jsoup.connect(url).get();
                String text = doc.outerHtml();
                BufferedWriter writer = new BufferedWriter(new FileWriter(f));
                writer.write(text);
                writer.close();
                return doc;
            } else {
                return Jsoup.parse(f, "UTF-8", "");
            }
        } else {
            f.createNewFile();
            //file doesn't exist, download
            Document doc = Jsoup.connect(url).get();
            String text = doc.outerHtml();
            BufferedWriter writer = new BufferedWriter(new FileWriter(f));
            writer.write(text);
            writer.close();
            return doc;
        }
    }
    public static Document getDocFromFile(String fileName) throws IOException, ParseException {
        File f = new File("storage/html/" + fileName + ".html");
        if(f.exists()){
            return Jsoup.parse(f, "UTF-8", "");
        }else {
            return null;
        }
    }

    public static BTree writeDocToBtree(String name,Document document,boolean diskWrite) throws IOException {

        BTree bTree = new BTree(DIRECTORY_NAME+name);
        if(!diskWrite) return bTree;
        //Maps word hash with frequencies
        HashMap<Long,Integer> map = new HashMap<>();
        String content = document.text();
        String delimiters ="[ .!?@\\[\\]/()\\-—,\"\']";
        String[] words = content.split(delimiters);
        for (int i = 0; i <words.length ; i++) {
            // Use the 64 bit hashing function
            Long hashedWord = ClusteringUtils.stringHash64(words[i]);
            //Check if word exists, if yes, increment value
            if(map.containsKey(hashedWord)){
                map.put(hashedWord,map.get(hashedWord)+1);
            }else{
                map.put(hashedWord,1);
            }
        }
        // Add vals to bTree
        map.forEach(bTree::insert);
        return bTree;
    }
    public static String titleFromFileName(String name){
        return  name.split(URL_BEGINNING)[1];
    }
    public static GraphNode[] getAllNodes() throws IOException {
        File dir = new File("storage/graph");
        File clustDir = new File("storage/clusters");
        File[] clusters = clustDir.listFiles();
        File[] files = dir.listFiles();
        GraphNode[] node = new GraphNode[files.length];
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            boolean isMedioid = false;
            for (int j = 0; j <clusters.length ; j++) {
                Cluster cluster = Cluster.getCLusterFromDisk(j);
                if(cluster.medioid.equals(f.getName())){
                    isMedioid = true;
                }
            }
            node[i] = GraphNode.getNodeFromDisk(f.getName(),isMedioid);
        }
        return node;
    }
}


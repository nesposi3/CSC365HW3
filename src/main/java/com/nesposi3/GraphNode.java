package com.nesposi3;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Objects;

public class GraphNode implements Comparable<GraphNode>{
    private final static String GRAPH_DIRECTORY = "storage/graph/";
    private String name;
    private String url;
    private String[] children;
    private double distToSrc = Double.MAX_VALUE;
    private GraphNode prev = null;
    private double[] childSimilarity;
    private boolean isMedioid = false;

    public GraphNode(String name,String url){
        this.name = name;
        this.children = new String[0];
        this.childSimilarity = new double[0];
        this.url = url;
    }
    public void addLinked(String fileName,double similarity){
        if(fileName.equals(url)){
            return;
        }
        for (String s: children
             ) {
            if(s.equals(fileName)){
                return;
            }
        }
        String[] newChildren= new String[this.children.length+1];
        double[] newSimilarities = new double[this.childSimilarity.length+1];
        for (int i = 0; i <this.children.length ; i++) {
            newChildren[i] = children[i];
            newSimilarities[i] = childSimilarity[i];
        }
        this.children = newChildren;
        this.childSimilarity = newSimilarities;
        this.children[children.length-1] = fileName;
        this.childSimilarity[children.length-1] = similarity;
    }

    public GraphNode(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        byte[] name = new byte[buffer.getInt()];
        buffer.get(name);
        this.name = new String(name);
        byte[] url = new byte[buffer.getInt()];
        buffer.get(url);
        this.url = new String(url);
        int numChildren = buffer.getInt();
        this.children = new String[numChildren];
        this.childSimilarity = new double[numChildren];
        for (int i = 0; i <numChildren ; i++) {
            int nameSize = buffer.getInt();
            byte[] nameBytes = new byte[nameSize];
            buffer.get(nameBytes);
            this.children[i] = new String(nameBytes);
            this.childSimilarity[i] = buffer.getDouble();
        }
    }
    public byte[] toBytes(){
        int totalSize = 12;
        byte[] nameBytes = name.getBytes();
        byte[] urlBytes = url.getBytes();
        int numChildren = this.children.length;
        int[] stringSizes = new int[this.children.length];
        byte[][] childrenBytes = new byte[this.children.length][];
        totalSize += (nameBytes.length + urlBytes.length);
        for (int i = 0; i <children.length ; i++) {
            totalSize +=4;
            byte[] childBytes = this.children[i].getBytes();
            stringSizes[i] = childBytes.length;
            totalSize+=childBytes.length;
            childrenBytes[i] = childBytes;
        }
        totalSize += (8*childSimilarity.length);
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.putInt(nameBytes.length);
        buffer.put(nameBytes);
        buffer.putInt(urlBytes.length);
        buffer.put(urlBytes);
        buffer.putInt(numChildren);
        for (int i = 0; i <children.length ; i++) {
            buffer.putInt(stringSizes[i]);
            buffer.put(childrenBytes[i]);
            buffer.putDouble(childSimilarity[i]);
        }
        return buffer.array();
    }
    public String debugString(){
        StringBuilder builder = new StringBuilder("Node: ");
        builder.append(name + ": "+ url + " \nChildren:\n");
        for (int i = 0; i <children.length ; i++) {
            builder.append(children[i] + " : " + childSimilarity[i] +"\n");
        }
        return builder.toString();
    }
    public String toString(){
        return this.name;
    }
    public GraphNode getPrev(){
        return this.prev;
    }
    public void writeToDisk() throws IOException {
        File f = new File(GRAPH_DIRECTORY + url);
        if(f.exists()){
            f.delete();
        }
        f.createNewFile();
        RandomAccessFile x = new RandomAccessFile(f,"rw");
        x.seek(0);
        x.write(this.toBytes());
        x.close();
    }
    public static GraphNode getNodeFromDisk(String fileName, boolean isMedioid) throws IOException {
        File f = new File(GRAPH_DIRECTORY+ fileName);
        if(!f.exists()){
            System.out.println("no");
            return null;
        }else {
            GraphNode x =  new GraphNode(Files.readAllBytes(f.toPath()));
            x.isMedioid = isMedioid;
            return x;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof GraphNode)) return false;
        GraphNode other = (GraphNode) obj;
        return (
                (other.name.equals(this.name))
                &&(other.url.equals(this.url))
                &&(Arrays.equals(this.children,other.children))
                &&(Arrays.equals(this.childSimilarity,other.childSimilarity))
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(name,url,children,childSimilarity);
    }
    public void setDistToSrc(double distToSrc){
        this.distToSrc = distToSrc;
    }
    public void setPrev(GraphNode prev){
        this.prev = prev;
    }
    public Double getDistToSrc(){
        return this.distToSrc;
    }
    public String getUrl(){
        return url;
    }
    @Override
    public int compareTo(GraphNode o) {
        return Double.compare(this.distToSrc,o.distToSrc);
    }

    public String[] getChildren() {
        return this.children;
    }
    public double[] getChildSimilarity(){
        return this.childSimilarity;
    }

    public String getName() {
        return name;
    }

    public void clearDjikstraInfo() {
        this.prev = null;
        this.distToSrc = Double.MAX_VALUE;
    }

    public boolean isMedioid() {
        return isMedioid;
    }
}

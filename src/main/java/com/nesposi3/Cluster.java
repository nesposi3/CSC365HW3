package com.nesposi3;

import com.nesposi3.Utils.BTreeUtils;
import com.nesposi3.Utils.CacheUtils;

import javax.imageio.IIOException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.file.Files;

/**
 * Disk representation of a cluster
 * Contains strings which are the names of the Btree files that represent websites
 *
 * 'Packet' format:
 *
 * |       4      |              4          |            n              |         4         |           (4 | n)*          |
 *   Cluster Id    Length of medioid string   n bytes of medioid string   Number of members    N pairs of lengths and bytes
 *
 */
public class Cluster {
    private static final String CLUSTER_FILE_LOCATION = "storage/clusters/";
    private int clusterId;
    public String[] members;
    public String medioid;
    public Cluster(int id){
        this.members = new String[0];
        this.clusterId = id;
    }
    public byte[] toBytes(){
        //Start at 12 due to fixed integer size
        int totalSize =12;
        int[] stringSizes = new int[members.length];
        byte[] medioidBytes = medioid.getBytes();
        int medioidSize = medioidBytes.length;
        totalSize += medioidSize;
        byte[][] membersBytes = new byte[this.members.length][];
        for (int i = 0; i <members.length ; i++) {
            membersBytes[i] = members[i].getBytes();
            stringSizes[i] = membersBytes[i].length;
            totalSize+=stringSizes[i];
            totalSize+=4;
        }
        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.putInt(clusterId);
        buffer.putInt(medioidSize);
        buffer.put(medioidBytes);
        buffer.putInt(membersBytes.length);
        for (int i = 0; i <membersBytes.length ; i++) {
            buffer.putInt(stringSizes[i]);
            buffer.put(membersBytes[i]);
        }
        return buffer.array();
    }
    public Cluster(byte[] bytes){
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        this.clusterId = buffer.getInt();
        int medLength = buffer.getInt();
        byte[] medioidBytes = new byte[medLength];
        buffer.get(medioidBytes);
        this.medioid = new String(medioidBytes);
        int numMembers = buffer.getInt();
        this.members = new String[numMembers];
        for (int i = 0; i <numMembers ; i++) {
            int currSize = buffer.getInt();
            byte[] currBytes = new byte[currSize];
            buffer.get(currBytes);
            this.members[i] = new String(currBytes);
        }

    }
    public void addMember(String newMember){
        String[] newMembers = new String[members.length+1];
        for (int i = 0; i <members.length ; i++) {
            newMembers[i] = members[i];
        }
        newMembers[members.length] = newMember;
        this.members = newMembers;
    }
    public void setMedioid(String s){
        this.medioid = s;
    }

    public String getMedioid() {
        return medioid;
    }

    public void writeToDisk(){
        File f = new File(CLUSTER_FILE_LOCATION + this.clusterId);
        if(f.exists()){
            f.delete();
        }
        try {
            f.createNewFile();
            RandomAccessFile x = new RandomAccessFile(f,"rw");
            x.seek(0);
            x.write(this.toBytes());
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
    }
    public static Cluster getCLusterFromDisk(int clusterId){
        File f = new File(CLUSTER_FILE_LOCATION + clusterId);
        if(!f.exists()){
            return null;
        }
        try {
            return new Cluster(Files.readAllBytes(f.toPath()));
        }catch (IOException ioe){
            ioe.printStackTrace();
            return null;
        }
    }
    public String[] getMembers(){
        return members;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Cluster: "  + clusterId + "\n" );
        s.append("Medioid: " + CacheUtils.titleFromFileName(medioid) +"\n");
        for (int i = 0; i < members.length; i++) {
            s.append("Member " + i + " " + CacheUtils.titleFromFileName(members[i]) + "\n");
        }
        return s.toString();
    }

    public void removeMember(String member) {
        for (int i = 0; i <this.members.length ; i++) {
            if(this.members[i].equals(member)){
                for (int j = i; j <members.length-1 ; j++) {
                    this.members[j] = this.members[j+1];
                }
            }
        }
        String[] newMembers = new String[this.members.length-1];
        for (int i = 0; i <members.length-1 ; i++) {
            newMembers[i] = this.members[i];
        }
        this.members = newMembers;
    }
}

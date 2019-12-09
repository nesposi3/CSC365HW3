package com.nesposi3;

import com.nesposi3.Utils.BTreeUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static com.nesposi3.Utils.BTreeUtils.*;

/**
 * Node objects are the Nodes of the Persistent B-Tree.
 * In this case, Nodes represent websites and store word information
 *
 * Nodes have a maximum size of 4096 bytes, so addresses must be at least 4096 apart
 *
 * Block Format in bytes:
 *          8       8           8*2T          8 * (2T-1)    4*(2T-1)
 *    | address | parent |   children    |     keys      |  frequencies
 */
public class Node {
    public long address;
    public long parent;
    public long[] children;
    public long[] keys;
    public int[] frequencies;
    /**
     * This constructor is used for debugging reasons
     * @param address
     * @param parent
     * @param children
     * @param keys
     */
    public Node(long address, long parent, long[] children, long[] keys ) {
        this.address = address;
        this.parent = parent;
        this.children = children;
        this.keys=keys;
    }

    /**
     * This constructor is used to initialize root Node
     *
     */
    public Node(){
        this.children = Arrays.copyOf(getNullLongArray(NUM_CHILDREN),NUM_CHILDREN);
        this.keys = Arrays.copyOf(getNullLongArray(K),K);
        this.frequencies = Arrays.copyOf(getNullIntArray(K),K);
        this.address = 0;
        this.parent = NULL;
    }

    /**
     * Create a Node object from a Byte object array
     * @param objectArr Byte object array representing a Node
     */
    public Node(Byte[] objectArr){
        byte[] arr = BTreeUtils.toPrimitiveBytes(objectArr);
        this.children = new long[NUM_CHILDREN];
        this.keys = new long[K];
        this.frequencies = new int[K];
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        this.address = buffer.getLong();
        this.parent = buffer.getLong();
        for (int i = 0; i <NUM_CHILDREN; i++) {
            this.children[i] = buffer.getLong();
        }
        for (int i = 0; i < K; i++) {
            this.keys[i] = buffer.getLong();
        }
        for (int i = 0; i <K ; i++) {
            this.frequencies[i] = buffer.getInt();
        }
    }

    /**
     * Create a Node object from a byte array
     * @param arr byte primitive array representing a node
     */
    public Node(byte[] arr){
        this.children = new long[NUM_CHILDREN];
        this.keys = new long[K];
        this.frequencies = new int[K];
        ByteBuffer buffer = ByteBuffer.wrap(arr);
        this.address = buffer.getLong();
        this.parent = buffer.getLong();
        for (int i = 0; i <NUM_CHILDREN; i++) {
            this.children[i] = buffer.getLong();
        }
        for (int i = 0; i < K; i++) {
            this.keys[i] = buffer.getLong();
        }
        for (int i = 0; i <K ; i++) {
            this.frequencies[i] = buffer.getInt();
        }

    }

    /**
     * Serializes this Node object into a byte representation
     * @return a byte[] representing the node
     */
    public byte[] toBytes(){
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        buffer.putLong(this.address);
        buffer.putLong(this.parent);
        for (int i = 0; i <NUM_CHILDREN ; i++) {
            buffer.putLong(this.children[i]);
        }
        for (int i = 0; i < K ; i++) {
            buffer.putLong(keys[i]);
        }
        for (int i = 0; i <K ; i++) {
            buffer.putInt(frequencies[i]);
        }
        return buffer.array();
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Node)){
            return false;
        }else{
            Node other = (Node) obj;
            boolean address = other.address == this.address;
            boolean parent = other.parent == this.parent;
            boolean children = true;
            for (int i = 0; i < NUM_CHILDREN ; i++) {
                if(this.children[i]!=other.children[i]){
                    children = false;
                }
            }
            boolean keys = true;
            for (int i = 0; i < K ; i++) {
                if(this.keys[i]!=other.keys[i] || frequencies[i] != other.frequencies[i]){
                    keys = false;
                }
            }
            return (address && parent && children && keys);
        }
    }
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("\nAddress: " + this.address+ "\nParent " + this.parent);
        for (int i = 0; i < NUM_CHILDREN; i++) {
            s.append("\nChild " + i +": " + this.children[i] );
        }
        for (int i = 0; i < K; i++) {
            s.append("\nKey " + i +": " + this.keys[i] + ". Freq:" + this.frequencies[i]);
        }
        return s.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(parent,address,children,frequencies);
    }
    public boolean leafStatus(){
        boolean isLeaf = true;
        for (int i = 0; i <NUM_CHILDREN ; i++) {
            if(children[i]!=-1){
                isLeaf = false;
            }
        }
        return isLeaf;
    }
    public int numKeys(){
        int i = 0;
        for (int j = 0; j < K ; j++) {
            if(keys[j] !=-1){
                i++;
            }
        }
        return i;
    }
    public boolean isFull(){
        return numKeys() == K;
    }

    public void setNumKeys(int n){
        int x =0;
        for (int i = 0; i <K ; i++) {
            if(x==n){
                for (int j = i; j <K ; j++) {
                    keys[j] = NULL;
                    frequencies[j]= -1;
                }
                break;
            }
            if(keys[i]!=NULL){
                x++;
            }
        }
    }
    private long[] getNullLongArray(int size){
        long[] out = new long[size];
        for (int i = 0; i <size ; i++) {
            out[i] = NULL;
        }
        return out;
    }
    private int[] getNullIntArray(int size){
        int[] out = new int[size];
        for (int i = 0; i <size ; i++) {
            out[i] = -1;
        }
        return out;
    }
    public void addChild(long addr){
        for (int i = 0; i <NUM_CHILDREN ; i++) {
            // If the key has found something bigger than it, shift up and add
            if(children[i]==NULL){
                children[i] = addr;
                return;
            }
        }
    }
    public void addKey(long key){
        long[] oldKeys = Arrays.copyOf(this.keys,K);
        for (int i = 0; i <K ; i++) {
            if(keys[i]==NULL){
                // No more to compare with, set key and break;
                keys[i] = key;
                return;
            }
            // If the key has found something bigger than it, shift up and add
            if(key <= keys[i]){
                for (int j = i; j <K-1 ; j++) {
                    this.keys[j+1] = oldKeys[j];
                }
                this.keys[i] = key;
                return;
            }
        }
    }
}

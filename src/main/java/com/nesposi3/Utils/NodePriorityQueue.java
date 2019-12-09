package com.nesposi3.Utils;

import com.nesposi3.GraphNode;
import com.nesposi3.Node;

public class NodePriorityQueue {
    private final int INITIAL_SIZE = 16;
    private GraphNode[] array;
    private int count;
    public NodePriorityQueue(){
        this.array = new GraphNode[INITIAL_SIZE];
    }
    public void add(GraphNode e){
        int k = count++;
        // array not big enough, resize
        if(count > array.length){
            resize();
        }
        array[k] = e;
        siftUp(k);
    }

    private void resize() {
        //double array size
        GraphNode[] newArr = new GraphNode[array.length<<1];
        System.arraycopy(array, 0, newArr, 0, array.length);
        this.array = newArr;
    }

    public GraphNode take(){
        if(count==0){
            return null;
        }
        GraphNode out = array[0];
        array[0] = array[--count];
        siftDown(0);
        return out;
    }
    private void siftDown(int k){
        while(k<count){
            int l = left(k);
            int r = right(k);
            if(l>=count){
                break;
            }
            if(r>=count){
                if(array[l].compareTo(array[k])<1){
                    GraphNode tmp = array[l];
                    array[l] = array[k];
                    array[k] = tmp;
                }
                //Break because we are at the leaf level
                break;
            }
            int least = l;
            if(array[l].compareTo(array[r])>0){
                least = r;
            }
            if(array[least].compareTo(array[k])<1){
                GraphNode tmp = array[k];
                array[k] = array[least];
                array[least] = tmp;
                k = least;
            }else{
                break;
            }
        }
    }
    private void siftUp(int k){
        while(k!=0){
            int p = parent(k);
            if(array[k].compareTo(array[p])<0){
                GraphNode e = array[k];
                array[k] = array[p];
                array[p] = e;
                k = p;
            }else break;
        }
    }
    private int parent(int k){
        return (k-1) >>>1;
    }
    private int left(int k){
        return (k << 1) + 1;
    }
    private int right(int k){
        return (k << 1) + 2;
    }
    public void reweight(GraphNode node,double newWeight){
        for (int i = 0; i <array.length ; i++) {
            if(array[i]==null) continue;
            if(array[i].equals(node)){
                double oldWeight = array[i].getDistToSrc();
                array[i].setDistToSrc(newWeight);
                if(oldWeight>newWeight){
                    //smaller,sift up
                    siftUp(i);
                }else {
                    siftDown(i);
                }
            }
        }
    }
    public boolean isEmpty(){
        return count == 0;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder("[");
        for (int i = 0; i <array.length ; i++) {
            if(array[i] == null){
                stringBuilder.append("| null | ");

            }else{
                stringBuilder.append("| " + array[i] +" : " +array[i].getDistToSrc() + " | ");
            }
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}

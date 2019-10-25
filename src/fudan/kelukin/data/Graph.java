package fudan.kelukin.data;

import tc.wata.data.FastSet;

import java.util.Arrays;
import java.util.HashSet;

abstract class Graph {
    Boolean[] isVertexChosen;
    int vertex_num;
    int edge_num;
    int[] first,nxt, endNode,edgeBefore;
    int[] edgeTriangleCnt;
    int[] nodeDegree;
    HashSet<Pair> dominationRel;
    public int[] category;
    Graph(int vertex_num){
        this.vertex_num = vertex_num;
        first = new int[vertex_num];
        Arrays.fill(first, -1);
        nodeDegree = new int[vertex_num];
        category = new int[vertex_num];
        
    }
    void initializeEdges(int edge_num){
        this.edge_num = 0;
        nxt =new int [edge_num];
        endNode = new int [edge_num];
        edgeBefore = new int[edge_num];
        edgeTriangleCnt = new int[edge_num/2];
        
        Arrays.fill(nxt, -1);
        Arrays.fill(endNode, -1);
        Arrays.fill(edgeBefore, -1);
    }
    abstract void deleteNode(int x);
    abstract void deleteEdge(int x, int y);
}

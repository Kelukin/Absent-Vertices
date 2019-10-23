package fudan.kelukin.data;

import javafx.util.Pair;
import tc.wata.data.FastMap;
import tc.wata.data.FastSet;
import tc.wata.debug.Debug;

import java.util.*;

public class MISGraph extends Graph{
    AuxiliaryGraph auxiliaryGraph;
    int mis_size;
    FastSet used;
    FastMap quickMap;
    int mirror_threshold = 3;
    int[] dominationDegree;
    int[] neighborCnt_dg2;
    int[] iter;
    public int[] newMinus;
    //扔入该队列之前，必须预先设定其category 为3 ，并且检查下
    public int head=0, tail=0;
    MISGraph(int vertex_num, double alpha){
            super(vertex_num);
            neighborCnt_dg2 = new int[vertex_num];
            dominationDegree = new int[vertex_num];
            quickMap = new FastMap(vertex_num);
            used = new FastSet(vertex_num);
            newMinus = new int[vertex_num];
            iter = new int[vertex_num];
            generate_graph(alpha);
            //生成一个alpha稀疏度的图
            for(int i=0;i<vertex_num;i++)
                if(nodeDegree[i] == 2)
                    for(int j = first[i];j !=-1;j = nxt[j]){
                        int node_z = endNode[j];
                        neighborCnt_dg2[node_z]++;
                    }
        }
    MISGraph(int[][] edges){
        super(edges.length);
        generate_graph(edges);
        used = new FastSet(vertex_num);
        neighborCnt_dg2 = new int[vertex_num];
        dominationDegree = new int[vertex_num];
        quickMap = new FastMap(vertex_num);
        newMinus = new int[vertex_num];
        for(int i=0;i<vertex_num;i++)
            if(nodeDegree[i] == 2)
                for(int j = first[i];j !=-1;j = nxt[j]){
                    int node_z = endNode[j];
                    neighborCnt_dg2[node_z]++;
                }
    }
    void generate_graph(double alpha){
        ArrayList<Integer>[] edges = new ArrayList[vertex_num];
        int edge_num = 0;
        for(int i=0; i< vertex_num;i++)
            for(int j=i+1; j< vertex_num; j++){
                if(alpha >= Math.random()){
                    edges[i].add(j);
                    edges[j].add(i);
                    edge_num +=2;
                }
            }
        initializeEdges(edge_num);
        for(int i=0;i<vertex_num;i++)
            for(int x:edges[i])
                if(x > i)
                    addEdge(i,x);
    }
    void generate_graph(int[][] edges){
        int edge_num = 0;
        for(int i=0;i< vertex_num;i++){
            nodeDegree[i] = edges[i].length;
            edge_num += nodeDegree[i];
        }
        initializeEdges(edge_num);
        for(int i=0;i<vertex_num;i++)
            for(int j:edges[i])
                if(j>i)
                addEdge(i,j);
    }
    public void initialized_with_lpReduciton(int x[]){
        //输入的x时VCSolver跑了lpReduciton之后得到的部分的x
        for(int i = 0; i < vertex_num; i++){
            if(x[i] == 0 && category[i] == 0)
                category[i] = -1;//Maybe V_asterisk
            if(x[i] == 1 && category[i] == 0)
                category[i] = -3;//Maybe V_minus
        }
    }
    void addDomination(int node_x, int node_y, Boolean deleteState){
        if(!dominationRel.add(new Pair<Integer, Integer>(node_x, node_y)))
            return;
        dominationDegree[node_y]++;
        if(deleteState)
            auxiliaryGraph.addDomination_mis_graph(node_x, node_y);
    }
    public void initializedTriangleCnt(){
        for(int i=0;i < edge_num; i+=2){
            used.clear();;
            int edge_cnt = 0;
            int x = endNode[i], y = endNode[i ^ 1];
            for(int tmp = first[x]; tmp!=-1; tmp = nxt[tmp])
                used.add(endNode[tmp]);
            for(int tmp = first[y]; tmp != -1; tmp = nxt[tmp])
                if(used.get(endNode[tmp])) edge_cnt++;
            edgeTriangleCnt[i>>1] = edge_cnt;
            if(x > y){
                int t = x;
                x = y; y = y;
            }
            Boolean state = true;
            if(edge_cnt == nodeDegree[x] - 1){
                addDomination(x, y, state);
                state = false;
            }
            if(edge_cnt == nodeDegree[y] - 1)   addDomination(y,x,state);
        }
        while(true){
            auxiliaryGraph.clearCheckQueue();
            if(head ==tail) break;
            else while (head <tail){
                int x = newMinus[head++];
                category[x] = 3;//minus vertex
                deleteNode(x);
            }
        }
        for(int i=0;i<vertex_num;i++)
            if(dominationDegree[i]!=0 && neighborCnt_dg2[i]!=0){//可能存在由此作为起始的 domination chain
                used.clear();;
                for(int j=first[i]; j!= -1; j = nxt[j])
                    if(dominationRel.contains(new Pair<>(endNode[j], i)))
                        used.add(endNode[j]);
                used.add(i);
                if(dfs_find_domination_chain(0, i, -1, 1))//TODO 假设不成立之后进行回溯
                {
                    deleteNode(i);
                }
            }
    }
    void deleteEdgeOneDirection(int startNode, int edgeNo){
        if(edgeNo == first[startNode]){
            first[startNode] = nxt[edgeNo];
            if(nxt[edgeNo]!=-1)
                edgeBefore[nxt[edgeNo]] = -1;
        }
        else{
            nxt[edgeBefore[edgeNo]] = nxt[edgeNo];
            if(nxt[edgeNo]!=-1)
                edgeBefore[nxt[edgeNo]] = edgeBefore[edgeNo];
        }
        nxt[edgeNo] = edgeBefore[edgeNo] = endNode[edgeNo] =  -1;
    }
    @Override
    void deleteEdge(int x, int y) {
        // y :edgeNo
        int edgeNo = y;
        int reverseEdge = y ^ 1, edNode = endNode[edgeNo];
        deleteEdgeOneDirection(x, edgeNo);
        deleteEdgeOneDirection(edNode, reverseEdge);
        quickMap.clear();
        for(int i=first[x]; i != -1; i = nxt[i])    quickMap.setValue(endNode[i],i);
        for(int i = first[edNode]; i != -1; i = nxt[i]){
            int value = quickMap.getValue(endNode[i]);
            if(value != -1){
                edgeTriangleCnt[value>>1]--;
                edgeTriangleCnt[i>>1]--;
            }
        }
        edgeTriangleCnt[edgeNo>>1] = 0;
        
    }
//
//    void judegeDomination(int edgeNo){
//        int node_x = endNode[edgeNo], node_y = endNode[edgeNo^1];
//        if(node_x > node_y){
//            int t = node_x;
//            node_x = node_y;node_y = t;
//        }
//        Boolean state = true;
//        //防止添加双向的dominance关系
//        if(edgeTriangleCnt[edgeNo>>1] == nodeDegree[node_x]-1){
//            addDomination(node_x, node_y, state);
//            state = false;
//        }
//        if(edgeTriangleCnt[edgeNo>>1] == nodeDegree[node_y]-1)
//            addDomination(node_y, node_x, state);
//    }
    public void clear_new_minus_queue(){
        //初始化之后，没每新确认一批节点，则调用该接口进行更新
        while(head != tail){
            int v = newMinus[head++];
            deleteNode(v);
        }
    }
    void increaseDegree2Neighbor(int v, int a){
        //v 原本degree 为2 但已经不再是了
        for(int i = first[v]; i != -1 ;i = nxt[i]){
            int u = endNode[i];
            if(category[u]!=3)
                neighborCnt_dg2[u]+=a;
        }
    }
    @Override
    void deleteNode(int x) {
        //同时将辅助图中的也进行清除
        if(first[x] == -1) return;
        //认为x的类别属于minus
        category[x] = 3;
        auxiliaryGraph.deleteNode_mis_graph(x);
        for(int deleteVertex :auxiliaryGraph.nodeDominationRel[x]){
            dominationRel.remove(new Pair<>(deleteVertex, x));
        }
        dominationDegree[x] = 0;
        mirrors_reduction(x);
        if(nodeDegree[x] == 2)
            increaseDegree2Neighbor(x, -1);
        for(int tmp = first[x]; tmp != -1; tmp = nxt[tmp]){
            int z = nxt[tmp];
            int u = endNode[tmp];
            if(dominationRel.remove(new Pair<>(x, u))){
                dominationDegree[u]--;
            }
            nodeDegree[u]--;
            if(nodeDegree[u]==2)
                increaseDegree2Neighbor(u, 1);
            else if(nodeDegree[u]==1)
                increaseDegree2Neighbor(u, -1);
            deleteEdge(x, tmp);
            tmp = z;
        }
        first[x] = -1;
    }
    void addEdge_oneDirection(int x, int y){
        int edgeNo = edge_num++;
        if(first[x]!=-1)
            edgeBefore[first[x]] = edgeNo;
        nxt[edgeNo] = first[x];
        endNode[edgeNo] = y;
        first[x] = edgeNo;
    }
    void addEdge(int x, int y) {
        addEdge_oneDirection(x, y);
        addEdge_oneDirection(y, x);
        nodeDegree[x]++;
        nodeDegree[y]++;
    }
    
    void mirrors_reduction(int v){//node v
        int[] ps = iter;
        for (int i = 0; i < vertex_num; i++) ps[i] = -2;
        used.clear();
        used.add(v);
        for (int i = first[v]; i != -1 ; i = nxt[i]){
            int u = endNode[i];
            used.add(u);
            ps[u] = -1;
        }
        for (int i = first[v]; i != -1 ; i = nxt[i]){
            int u = endNode[i];
            for (int j = first[u]; j != -1; j = nxt[j]){
                int w = endNode[j];
                if (category[w] != 3 && used.add(w)) {
                    int c1 = nodeDegree[v];
                    for (int k = first[w]; k != -1; k = nxt[k]){
                        int z = endNode[k];
                        if (ps[z] != -2) {
                            ps[z] = w;
                            c1--;
                        }
                    }
                    boolean ok = true;
                    for(int k2 = first[v]; k2 != -1; k2 = nxt[k2]){
                        int u2 = endNode[k2];
                        if (ps[u2] != w) {
                            int c2 = 0;
                            for(int k3 = first[u2]; k3 != -1; k3 = nxt[k3]){
                                int w2 = endNode[k3];
                                if (ps[w2] == w) c2++;
                            }
                            if (c2 != c1 - 1) {
                                ok = false;
                                break;
                            }
                        }
                    }
                    if (ok){
                        newMinus[tail++] = w;
                        category[w] = 3;
                    }
                }
            }
        }
    }
    
    Boolean dfs_find_domination_chain(int no, int node_x, int bf_node, int length){
        //使用前需要将used进行clear
        if(length > 7)
            return false;
        used.add(no);
        if(no % 2 == 1){
            for(int i=first[node_x];i !=-1; i = nxt[i]){
                int node_y = endNode[i];
                if(neighborCnt_dg2[node_y]>=2 && !used.get(node_y))//延长该条串
                    if(dfs_find_domination_chain(no+1, node_y, node_x, length+1)){
                        category[node_x] = 1;
//                printf("%d\n",node_x);
                        return true;
                        //如果找到了这么一条串
                    }
                //后续调整逻辑，应当优先进行删除清算
                if(dominationDegree[node_y]!=0 && !used.get(node_y) &&
                        bf_node!=node_y &&
                        (dominationRel.contains(new Pair<>(node_x,node_y)) ||
                                dominationDegree[node_y]>1)){
                    
                    for(int j=first[node_y]; j!= -1; j = nxt[j])
                        if(dominationRel.contains(new Pair<>(endNode[j],node_y)) &&
                                !used.get(endNode[j])){
                            category[node_x] = 1;
                            deleteNode(node_y);
                            return true;//找到了这么一条串
                        }
                }
            }
        }
        else{
            for(int i=first[node_x]; i != -1; i = nxt[i]){
                int node_y = endNode[i];
                if(nodeDegree[node_y] == 2 &&
                        node_y != bf_node && !used.get(node_y)){
                    //进行一些临时删除操作 也可以不
                    if(dfs_find_domination_chain(no+1, node_y, node_x,length+1)){
                        deleteNode(node_x);
                        return true;
                    }
                }
            }
            //偶数点，进行删边以及维护
        }
        used.remove(no);
        return false;//无法找到这样子的一个串
    }
    
    
    
    class AuxiliaryGraph extends Graph{
        HashMap<Pair<Integer, Integer>, Integer> addedEdge;
        ArrayList<Integer>[] nodeDominationRel;
        Stack<Integer> check_edgeNO, check_node;
        Stack<Integer> freeEdge;
        FastMap quickMap;
        int[] waitDegree;
        AuxiliaryGraph(int vertex_num, int edge_num){
            super(vertex_num);
            nodeDominationRel = new ArrayList[vertex_num];
            for(int i=0;i<vertex_num;i++)
                nodeDominationRel[i] = new ArrayList<>();
            waitDegree = new int[vertex_num];
            check_edgeNO = new Stack<>();
            check_node = new Stack<>();
            freeEdge = new Stack<>();
            addedEdge = new HashMap<>();
            quickMap = MISGraph.this.quickMap;
            // 注意不要再外部调用quickMap的时候调用内部类种设计quickMap的方法
            initializeEdges(edge_num);
        }
        
        void addDomination(int x, int y) {
            nodeDominationRel[y].add(x);
        }
        
        @Override
        void deleteNode(int x) {
            for(int tmp = first[x]; tmp != -1; tmp = nxt[tmp]){
                int y = endNode[tmp];
                int z = nxt[tmp];
                if(category[y]!=2)
                    deleteEdge(x, tmp);
                tmp = z;
            }
            first[x] = -1;
        }
        void deleteEdge_Onedirection(int x, int edge_no){
            if(first[x] == edge_no){
                first[x] = nxt[edge_no];
                if(first[x]!=-1)   edgeBefore[first[x]] = -1;
            }
            else{
                nxt[edgeBefore[edge_no]] = nxt[edge_no];
                if(nxt[edge_no] != -1) edgeBefore[nxt[edge_no]] = edgeBefore[edge_no];
            }
            edgeBefore[edge_no] = nxt[edge_no] = endNode[edge_no] = -1;
        }
        @Override
        void deleteEdge(int x, int y) {
            //y:edgeNo
            int edge_no = y;
            int reverse_no = edge_no ^1;
            freeEdge.push(edge_no);
            freeEdge.push(reverse_no);
            edgeTriangleCnt[edge_no>>1] = 0;
            
            y = endNode[edge_no];
            
            addedEdge.remove(new Pair<>(x,y));
            addedEdge.remove(new Pair<>(y,x));
            
            nodeDegree[x]--;nodeDegree[y]--;
            quickMap.clear();
            for(int tmp = first[y]; tmp != -1; tmp = nxt[tmp])
                quickMap.setValue(endNode[tmp],tmp);
            for(int tmp = first[x]; tmp != -1; tmp = nxt[tmp])
                if(quickMap.exist(endNode[tmp])){
                    edgeTriangleCnt[quickMap.getValue(endNode[tmp])>>1]--;
                    edgeTriangleCnt[tmp>>1]--;
                }
            deleteEdge_Onedirection(y, reverse_no);
            deleteEdge_Onedirection(x, edge_no);
            
            edgeTriangleCnt[edge_no>>1] = 0;
        }
        
        
        void clearCheckQueue(){
            while(!check_edgeNO.empty()){
                int edgeNo = check_edgeNO.pop(), nodeNo = check_node.pop();
                if(category[nodeNo] != 2 || (endNode[edgeNo]!=nodeNo && endNode[edgeNo^1]!=nodeNo)) continue;
                if(edgeTriangleCnt[edgeNo>>1] != nodeDegree[nodeNo]-1){
                    deleteNode(nodeNo);
                    category[nodeNo] = -1;//deleted Minus Set
                    int[] que = MISGraph.this.newMinus;
                    if(MISGraph.this.category[nodeNo]!=3)
                    que[MISGraph.this.tail++] = nodeNo;//将其加入父类的删除当中
                    MISGraph.this.category[nodeNo] = 3;
                    for(int x:nodeDominationRel[nodeNo])if(category[x] == 4){
                        waitDegree[x]--;
                        if(waitDegree[x] == 0){
                            deleteNode(x);
                            category[x] = 2;
                            for(int y:nodeDominationRel[x]){
                                int tmp = addEdge(y,x);
                                check_edgeNO.push(tmp); check_node.push(x);
                            }
                        }
                    }
                }
            }
        }
    
        void addEdge_oneDirection(int x, int y, int edgeNo){
            if(first[x]!=-1)
                edgeBefore[first[x]] = edgeNo;
            edgeBefore[edgeNo] = -1;
            nxt[edgeNo] = first[x];
            endNode[edgeNo] = y;
            first[x] = edgeNo;
        }
        
        int addEdge(int x, int y){
            if(addedEdge.containsKey(new Pair<>(x,y))){
                return addedEdge.get(new Pair<>(x,y));
            }
            int edgeNo = -1, reverseNo;
            if(!freeEdge.empty()){
                edgeNo = freeEdge.pop();
                reverseNo = freeEdge.pop();
            }else{
                edgeNo = edge_num++;
                reverseNo = edge_num++;
            }
            addEdge_oneDirection(x,y, edgeNo);
            addEdge_oneDirection(y,x, reverseNo);
            addedEdge.put(new Pair<>(x,y), edgeNo);
            addedEdge.put(new Pair<>(y,x), reverseNo);
            
            quickMap.clear();
            for(int tmp = first[x]; tmp != -1; tmp = nxt[tmp])  quickMap.setValue(endNode[tmp], tmp);
            
            for(int tmp = first[y]; tmp != -1; tmp = nxt[tmp]) if(quickMap.getValue(endNode[tmp])!=-1){
                int e = quickMap.getValue(endNode[tmp]);
                edgeTriangleCnt[e>>1]++;edgeTriangleCnt[tmp>>1]++; edgeTriangleCnt[edgeNo>>1]++;
            }
            
            return edgeNo;
        }
        
        public void addDomination_mis_graph(int x, int y){
            waitDegree[x]++;
            if(category[y] == 1){
                if(waitDegree[y] != 0)  category[y] = 4;
                else{
                    category[y] = 2;
                }
            }else if(category[y] == 0) category[y] = 2;
            
            if(category[x] != 1){
                if(category[x] ==2 || category[x] == 4) category[x] = 4;
                else category[x] = 1;
                for(int tmp = MISGraph.this.first[x]; tmp != -1; tmp = MISGraph.this.nxt[tmp]){
                    int z = MISGraph.this.endNode[tmp];
                    if(category[z] == 1 || category[z] == 4)
                        addEdge(x,z);
                }
            }
            addDomination(x,y);
            int tmp = addEdge(x,y);
            check_edgeNO.push(tmp); check_node.push(y);
        }
        public void deleteNode_mis_graph(int v){
            //TODO
            //外部mis_graph当中发现了点x作为V^- 时将其周围的点进行删除
            //新发现的不满足要求的也要丢入至newMinus当中
            if(category[v] == -1) return;
        }
    }
    
}

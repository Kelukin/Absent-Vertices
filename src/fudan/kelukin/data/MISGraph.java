package fudan.kelukin.data;

import tc.wata.data.FastMap;
import tc.wata.data.FastSet;
import tc.wata.debug.Debug;

import java.util.*;

public class MISGraph extends Graph{
    AuxiliaryGraph auxiliaryGraph;
    public  static int mode = 4;
    static int DOMINANCE = 1;
    static int MIRROR = 2;
    static int CHAIN = 3;
    static int BRUFORCE = 4;
    public  static Boolean timeMeasure = false;
    public  FastSet used;
    public  FastMap quickMap;
    int mirror_threshold = 3;
    int chain_length_threshold = 5;
    int[] dominationDegree;
    int[] neighborCnt_dg2;
    int[] iter;
    int domination_cnt, chain_cnt, mirror_cnt, bruce_cnt;
    public Pair[] newMinus;
    Stack<Integer> chainCheck_stack;
    //扔入该队列之前，必须预先设定其category 为3 ，并且检查下
    public int head=0, tail=0;
    public MISGraph(int[][] edges){
        super(edges.length);
        chainCheck_stack = new Stack<>();
        domination_cnt = mirror_cnt = chain_cnt = 0;
        generate_graph(edges);
        used = new FastSet(vertex_num);
        iter = new int[vertex_num];
        if(mode >=3){
            neighborCnt_dg2 = new int[vertex_num];
            dominationDegree = new int[vertex_num];
        }
        if(mode >=2) {
            dominationRel = new HashSet<>();
        }
        quickMap = new FastMap(vertex_num);
        newMinus = new Pair[vertex_num];
        if(mode ==2 || mode == 4)  auxiliaryGraph=new AuxiliaryGraph(vertex_num,edge_num);
        if(mode >=3)
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
        if(mode>=2)
            edgeTriangleCnt = new int[edge_num/2];
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
    public void learn_from_opt(int[] vc_opt, int mis_size){
        int sum = 0;
        for(int i=0; i<vertex_num; i++)
            if(vc_opt[i] <= 0){
                sum++;
                if(category[i] == 0)    category[i] = -1;
                else if(category[i] == -3){
                    oracle(i,2);
                }
            }else if(vc_opt[i] >0){
                if(category[i] == 0) category[i] = -3;
                else if(category[i] == -1){
                    oracle(i, 2);
                }
            }
            Debug.check(mis_size == sum);
        //maybe do more to auxiliary graph?
    }
    void addDomination(int node_x, int node_y, Boolean deleteState){
        if(!dominationRel.add(new Pair(node_x, node_y)))
            return;
        if(mode>=3)
        dominationDegree[node_y]++;
        if(deleteState && (mode == 2 || mode == 4))
            auxiliaryGraph.addDomination_mis_graph(node_x, node_y);
    }



    public void initializedTriangleCnt(){
        long begin=0, end;

        if(timeMeasure)
        begin = System.currentTimeMillis();

        for(int i=0;i < edge_num; i+=2){
            used.clear();;
            int edge_cnt = 0;
            int x = endNode[i], y = endNode[i ^ 1];
            for(int tmp = first[x]; tmp!=-1; tmp = nxt[tmp])
                used.add(endNode[tmp]);
            for(int tmp = first[y]; tmp != -1; tmp = nxt[tmp])
                if(used.get(endNode[tmp]) == true) edge_cnt++;
            edgeTriangleCnt[i>>1] = edge_cnt;
            if(x > y){
                int t = x;
                x = y; y = t;
            }
            Boolean state = true;
            if(edge_cnt == nodeDegree[x] - 1){
                addDomination(x, y, state);
                state = false;
            }
            if(edge_cnt == nodeDegree[y] - 1)   addDomination(y,x,state);
        }
        if(timeMeasure){
            end = System.currentTimeMillis();
            System.out.printf("The time to complete initializing " +
                            "triangle count is %.3f %n",
                    (1e-3)* (end-begin));
        }

        if(timeMeasure)
            begin = System.currentTimeMillis();

        if(mode == 2 || mode == 4)
        while(true){
            auxiliaryGraph.clearCheckQueue();
            if(head ==tail) break;
            else while (head <tail){
                Pair tmpPair = newMinus[head++];
                int x = tmpPair.key, method = tmpPair.value;
                category[x] = 3;//minus vertex
                deleteNode(x, method);
            }
        }
        if(mode >=3)
        for(int i=0;i<vertex_num;i++)
            if(meetChainCondition(i)){//可能存在由此作为起始的 domination chain
                tryChainReduction(i);
            }
        if(timeMeasure){
            end = System.currentTimeMillis();
            System.out.printf("The time to first loop deleteNode" +
                            "triangle count is %.3f %n",
                    (1e-3)* (end-begin));
            printCnt();
        }
    }

    public void oracle(int u, int a){
        //oracle tells that u's category is `a`.
        category[u] = a;
        if(a == 1){
            for(int i = first[u]; i != -1; i = nxt[i]) {
                newMinus[tail++] = new Pair(endNode[i], BRUFORCE);
                category[endNode[i]] = 3;
            }
        }else if(a == 2){

        }else if(a == 3){
            newMinus[tail++] = new Pair(u ,BRUFORCE);
        }
    }
    Boolean meetChainCondition(int v){
        return category[v]<=0 && dominationDegree[v] !=0 && neighborCnt_dg2[v] != 0;
    }

    void tryChainReduction(int v){
        int i = v;
        used.clear();
        for(int j=first[i]; j!= -1; j = nxt[j])
            if(category[endNode[j]]<=0 && dominationRel.contains(new Pair(endNode[j], i)))
                used.add(endNode[j]);
        used.add(i);
        if(dfs_find_domination_chain(0, i, -1, 1))//TODO 假设不成立之后进行回溯
        {
            if(category[i]!=3){
                category[i]=3;
                newMinus[tail++] = new Pair(i, CHAIN);
            }
//            deleteNode(i,CHAIN);
        }
    }
    public void printCnt(){
        System.out.printf("mirror reducton: %d, chain reduction: %d " +
                "domination reduction: %d bruce-forced reduction: %d%n"
                , mirror_cnt, chain_cnt, domination_cnt, bruce_cnt);
    }
    void deleteEdgeOneDirection(int startNode, int edgeNo){
        nodeDegree[startNode]--;
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
        if(mode <=1) return;
        for(int i=first[x]; i != -1; i = nxt[i])    quickMap.setValue(endNode[i],i);
        for(int i = first[edNode]; i != -1; i = nxt[i]){
            int value = quickMap.getValue(endNode[i]);
            if(value != -1){
                edgeTriangleCnt[value>>1]--;
                edgeTriangleCnt[i>>1]--;
            }
        }
        edgeTriangleCnt[edgeNo>>1] = 0;
        // the following code is new
        if(mode ==2) return;
        for(int j = first[edNode]; j != -1; j = nxt[j])
            if(nodeDegree[edNode] -1 == edgeTriangleCnt[j>>1]
                    && !dominationRel.contains(new Pair(edNode,endNode[j]))){
                int z = endNode[j];
                Boolean ok = (edgeTriangleCnt[j>>1] != nodeDegree[z]-1);
                Boolean meet_chain_before = meetChainCondition(z);
                addDomination(edNode,z , ok);
                if(!meet_chain_before && meetChainCondition(z))
                    chainCheck_stack.push(z);
            }

    }


    public Boolean clear_new_minus_queue(){
        //初始化之后，没每新确认一批节点，则调用该接口进行更新
        if(mode<=1) {
            tail = head = 0;
            return false;
        }
        if(mode == 2 || mode == 4)
        auxiliaryGraph.clearCheckQueue();
        if(head == tail && chainCheck_stack.empty()) return false;
        while(head != tail){
            Pair tmpPair = newMinus[head++];
            int v = tmpPair.key, method = tmpPair.value;
            deleteNode(v, method);
        }
        head = tail = 0;
        while(!chainCheck_stack.empty()){
            int u = chainCheck_stack.pop();
            tryChainReduction(u);
        }
        while(head!=tail){
            Pair tmpPair = newMinus[head++];
            int v = tmpPair.key, method = tmpPair.value;
            deleteNode(v, method);
        }
        return true;
    }
    void increaseDegree2Neighbor(int v, int a){
        if(mode<=2) return;
        //v 原本degree 为2 但已经不再是了
        for(int i = first[v]; i != -1 ;i = nxt[i]){
            int u = endNode[i];
            if(category[u]<=0){
                Boolean tmp = meetChainCondition(u);
                neighborCnt_dg2[u]+=a;
                if(!tmp && meetChainCondition(u))
                    chainCheck_stack.push(u);
            }
        }
    }
    void deleteNode(int x, int method){
        //同时将辅助图中的也进行清除
        if(first[x] == -1) return;
        //认为x的类别属于minus
        category[x] = 3;
        if(mode == 2 || mode == 4){
            auxiliaryGraph.deleteNode_mis_graph(x);
            for(int deleteVertex :auxiliaryGraph.nodeDominationRel[x]){
                dominationRel.remove(new Pair(deleteVertex, x));
            }
        }
        if(mode>=3)
        dominationDegree[x] = 0;
        if(method != DOMINANCE) mirrors_reduction(x);
        if(mode>=3 && nodeDegree[x] == 2)
            increaseDegree2Neighbor(x, -1);
        for(int tmp = first[x]; tmp != -1;){
            int z = nxt[tmp];
            int u = endNode[tmp];
            if(mode>=3)
            if(dominationRel.remove(new Pair(x, u))){
                dominationDegree[u]--;
            }
            deleteEdge(x, tmp);
            if(nodeDegree[u]==2)
                increaseDegree2Neighbor(u, 1);
            else if(nodeDegree[u]==1)
                increaseDegree2Neighbor(u, -1);
            else if(nodeDegree[u] <= 0) category[u] = 1;
            tmp = z;
        }
        first[x] = -1;

        if(method == CHAIN) chain_cnt++;
        else if(method == MIRROR) mirror_cnt++;
        else if(method == DOMINANCE) domination_cnt++;
        else if(method == BRUFORCE) bruce_cnt++;
    }
    @Override
    void deleteNode(int x) {
        deleteNode(x, BRUFORCE);
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
        Debug.check(edge_num % 2 == 0);
    }
    
    void mirrors_reduction(int v){//node v
        int[] ps = iter;
        for (int i = 0; i < vertex_num; i++) ps[i] = -2;
        used.clear();
        used.add(v);
        int dv = 0;
        for (int i = first[v]; i != -1 ; i = nxt[i])if(category[endNode[i]]!=3){
            int u = endNode[i];
            used.add(u);
            ps[u] = -1;
            dv++;
        }
        int old_tail = tail;
        for (int i = first[v]; i != -1 ; i = nxt[i]){
            int u = endNode[i];
            if(category[u] == 3) continue;
            for (int j = first[u]; j != -1; j = nxt[j]){
                int w = endNode[j];
                if (category[w] !=3 && used.add(w)) {
                    int c1 = dv;//the size of assumed clique
                    for (int k = first[w]; k != -1; k = nxt[k]){
                        int z = endNode[k];
                        if (category[z] != 3 && ps[z] != -2) {// the sharing neighbor of u and  w
                            ps[z] = w;
                            c1--;
                        }
                    }
                    boolean ok = true;
                    for(int k2 = first[v]; k2 != -1; k2 = nxt[k2]){
                        int u2 = endNode[k2];
                        if (ps[u2] != w && category[u2]!=3) {
                            int c2 = 0;
                            for(int k3 = first[u2]; k3 != -1; k3 = nxt[k3]){
                                int w2 = endNode[k3];
                                if (ps[w2] != w && ps[w2]!=-2&& category[w2]!=3) c2++;
                            }
                            if (c2 != c1 - 1) {
                                ok = false;
                                break;
                            }
                        }
                    }
                    if (ok){
                        newMinus[tail++] = new Pair(w, MIRROR);
//                        category[w] = 3;
//                        System.err.printf("mirror:%d%n",w);
//                        mirror_cnt++;
                    }
                }
            }
        }
        for(int i=old_tail;i<tail;i++)
            category[newMinus[i].key] = 3;
    }
    
    Boolean dfs_find_domination_chain(int no, int node_x, int bf_node, int length){
        //使用前需要将used进行clear
        if(length > chain_length_threshold)
            return false;
        used.add(node_x);
        if(no % 2 == 1){
            for(int i=first[node_x];i !=-1; i = nxt[i]){
                int node_y = endNode[i];
                if(category[node_y]<=0 && neighborCnt_dg2[node_y]>=2 && !used.get(node_y))//延长该条串
                    if(dfs_find_domination_chain(no+1, node_y, node_x, length+1)){
                        category[node_x] = 1;
//                printf("%d\n",node_x);
                        return true;
                        //如果找到了这么一条串
                    }
                //后续调整逻辑，应当优先进行删除清算
                if(category[node_y]<=0 && dominationDegree[node_y]!=0 && !used.get(node_y) &&
                        bf_node!=node_y &&
                        (dominationRel.contains(new Pair(node_x,node_y)) ||
                                dominationDegree[node_y]>1)){
                    
                    for(int j=first[node_y]; j!= -1; j = nxt[j])
                        if(dominationRel.contains(new Pair(endNode[j],node_y)) &&
                                !used.get(endNode[j])){
                            category[node_x] = 1;
                            if(category[node_y] <= 0)
                            {
                                category[node_y] = 3;
                                newMinus[tail++] = new Pair(node_y,CHAIN);
//                                deleteNode(node_y, CHAIN);
                            }
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
                        category[node_x] = 3;
                        newMinus[tail++] = new Pair(node_x,CHAIN);
//                            deleteNode(node_x, CHAIN);
                        return true;
                    }
                }
            }
            //偶数点，进行删边以及维护
        }
        used.remove(node_x);
        return false;//无法找到这样子的一个串
    }
    
    
    
    class AuxiliaryGraph extends Graph{
        HashMap<Pair, Integer> addedEdge;
        ArrayList<Integer>[] nodeDominationRel;
        Stack<Integer> check_edgeNO, check_node;
        Stack<Integer> freeEdge;
        FastMap quickMap;
        FastSet used;
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
            quickMap = new FastMap(vertex_num);
            used = new FastSet(vertex_num);
            // 注意不要再外部调用quickMap的时候调用内部类种设计quickMap的方法
            initializeEdges(edge_num);
            if(mode>=2)
                edgeTriangleCnt = new int[edge_num/2];
        }
        
        void addDomination(int x, int y) {
            nodeDominationRel[y].add(x);
        }
        
        @Override
        void deleteNode(int x) {
            for(int tmp = first[x]; tmp != -1;){
                int y = endNode[tmp];
                int z = nxt[tmp];
//                if(category[y]!=2)
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

            y = endNode[edge_no];
            
            addedEdge.remove(new Pair(x,y));
            addedEdge.remove(new Pair(y,x));
            
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

        void set_minus(int nodeNo){
            deleteNode(nodeNo);
            category[nodeNo] = -1;//deleted Minus Set
            Pair[] que = MISGraph.this.newMinus;
            if(MISGraph.this.category[nodeNo]!=3) {
                que[MISGraph.this.tail++] = new Pair(nodeNo, DOMINANCE);//将其加入父类的删除当中
//                MISGraph.this.domination_cnt++;
            }
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
        
        void clearCheckQueue(){
            while(!check_edgeNO.empty()){
                int edgeNo = check_edgeNO.pop(), nodeNo = check_node.pop();
                if(category[nodeNo] != 2
                        || (endNode[edgeNo]!=nodeNo && endNode[edgeNo^1]!=nodeNo)
                        || category[nodeNo] == -1) continue;
                if(edgeTriangleCnt[edgeNo>>1] != nodeDegree[nodeNo]-1){
                    set_minus(nodeNo);
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
            nodeDegree[x]++;
        }
        
        int addEdge(int x, int y){
            if(addedEdge.containsKey(new Pair(x,y))){
                return addedEdge.get(new Pair(x,y));
            }
            int edgeNo = -1, reverseNo;
            if(!freeEdge.empty()){
                edgeNo = freeEdge.pop();
                reverseNo = freeEdge.pop();
                Debug.check(edgeNo == (reverseNo^1));
            }else{
                edgeNo = edge_num++;
                reverseNo = edge_num++;
            }
            addEdge_oneDirection(x,y, edgeNo);
            addEdge_oneDirection(y,x, reverseNo);
            addedEdge.put(new Pair(x,y), edgeNo);
            addedEdge.put(new Pair(y,x), reverseNo);
            
            quickMap.clear();
            for(int tmp = first[x]; tmp != -1; tmp = nxt[tmp])  quickMap.setValue(endNode[tmp], tmp);
            
            for(int tmp = first[y]; tmp != -1; tmp = nxt[tmp]) if(quickMap.getValue(endNode[tmp])!=-1){
                int e = quickMap.getValue(endNode[tmp]);
                edgeTriangleCnt[e>>1]++;edgeTriangleCnt[tmp>>1]++; edgeTriangleCnt[edgeNo>>1]++;
            }
            
            return edgeNo;
        }
        
        public void addDomination_mis_graph(int x, int y){
            if(category[x] < 0 || category[y] < 0
                    || MISGraph.this.category[y] == -1) return;
            waitDegree[x]++;
            if(category[y] == 1){
                if(waitDegree[y] != 0)  category[y] = 4;
                else{
                    Debug.check(false);
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
        public void oracle(int u, int kind){}
        public void deleteNode_mis_graph(int v){
            //TODO
            //外部mis_graph当中发现了点x作为V^- 时将其周围的点进行删除
            //新发现的不满足要求的也要丢入至newMinus当中
            if(category[v] == -1) return;
            if(category[v] == 2)
                set_minus(v);
            else
                set_minus(v);
//                Debug.check(false);
        }
    }
    
}

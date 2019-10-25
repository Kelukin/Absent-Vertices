import fudan.kelukin.data.MIS;
import fudan.kelukin.data.MISGraph;
import tc.wata.debug.Debug;

public class MISModifier {
    public static Boolean to_learn = false;
    MISGraph misGraph;
    MIS mis;
    int mis_size;
    int[][] adj;
    int n;
    public MISModifier(int[][] adj){
        n = adj.length;
        this.adj = adj;
        misGraph = new MISGraph(adj);
        VCSolver vcSolver = new VCSolver(adj, n);
        vcSolver.lpReduction();
//        misGraph.initialized_with_lpReduciton(vcSolver.x);
        vcSolver = new VCSolver(adj,n);
        mis_size = n - vcSolver.solve();
        mis = new MIS(vcSolver.y);
    }
    Boolean try_asterisk(int u){
        VCSolver tmpSolver = new VCSolver(adj, adj.length);
        tmpSolver.reInitializeVertex(u, 1);
        int new_mis_size = n - tmpSolver.solve();
        Debug.check(new_mis_size <= mis_size);
        if(to_learn && new_mis_size == mis_size)
            misGraph.learn_from_opt(tmpSolver.y, mis_size);
        return new_mis_size == mis_size - 1;

    }
    Boolean try_minus(int u){
        VCSolver tmpSolver = new VCSolver(adj, adj.length);
        tmpSolver.reInitializeVertex(u, 0);
        int new_mis_size = n - tmpSolver.solve();
        Debug.check(new_mis_size <= mis_size);
        if(to_learn && new_mis_size == mis_size)
            misGraph.learn_from_opt(tmpSolver.y, mis_size);
        return new_mis_size < mis_size;
    }
    void updateMISGraph(int u, int kind){
        misGraph.oracle(u, kind);
        while(misGraph.clear_new_minus_queue());
    }
    public Boolean check(){
        for(int i=0;i<n;i++)
            if(misGraph.category[i] == 3){
                VCSolver vcSolver = new VCSolver(adj, adj.length);
                vcSolver.reInitializeVertex(i, 0);
                if(mis_size == n - vcSolver.solve())
                    return false;
            }else if(misGraph.category[i] == 1){
                VCSolver vcSolver = new VCSolver(adj, adj.length);
                vcSolver.reInitializeVertex(i, 1);
                if(mis_size == n - vcSolver.solve())
                    return false;
            }
        return true;
    }
    public void printResult(){
        misGraph.printCnt();
        int[] category = misGraph.category;
        int minus_cnt=0, asterisk_cnt = 0, plus_cnt = 0;
        for(int i=0;i<n;i++)
            if(category[i] == 1) asterisk_cnt++;
            else if(category[i] == 2) plus_cnt++;
            else if(category[i] == 3) minus_cnt++;
        System.err.printf("asterisk:%d, plus:%d, minus:%d%n"
                , asterisk_cnt, plus_cnt, minus_cnt);
    }
    public void categoryVertices(){
        misGraph.initializedTriangleCnt();
        while(misGraph.clear_new_minus_queue());
        System.err.printf("initialize OK!%n");
        for(int i=0;i<n;i++)
            if(misGraph.category[i] <= 0){
                int kind = 2;
                if(misGraph.category[i] == -1){
                    if(try_asterisk(i)) kind = 1;
                }else if(misGraph.category[i] == -3){
                    if(try_minus(i)) kind = 3;
                }else{//category = 0
                    if(try_asterisk(i)) kind = 1;
                    else if(try_minus(i)) kind = 3;
//                    else misGraph.category[i] = 2;
                }
                updateMISGraph(i, kind);
                System.err.printf("vertex:%d kind:%d%n",i, kind);
            }
        printResult();
        Debug.check(check());
//        if(!check()) System.err.printf("Error!%n");
    }
    public void categoryVertices_with_priority(){

    }
}

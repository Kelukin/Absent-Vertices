import com.sun.org.apache.xpath.internal.operations.Bool;
import fudan.kelukin.data.MIS;
import fudan.kelukin.data.MISColor;
import fudan.kelukin.data.MISGraph;
import tc.wata.debug.Debug;

public class MISModifier {
    public static int learn = 1;
    public static int mode = 4;
    public static Boolean use_color = true;
    public static int upperBound = 1;
    public static Boolean check=false;
    public static Boolean memoryMeasure = false;
    MISGraph misGraph;
    MISColor colors;
    MIS mis;
    public int mis_size;
    int cnt_mis_solver;
    int[][] adj;
    int[] category;
    int[] deleteMethod;
    int n;
    int vcs_size;
    VCSolver vcSolver;
    public MISModifier(int[][] adj){
        cnt_mis_solver = 0;
        n = adj.length;
        this.adj = adj;
        misGraph = new MISGraph(adj);
        category = misGraph.category;
        deleteMethod = misGraph.deleteMethod;
        vcSolver = new VCSolver(adj, n);
        vcs_size = vcSolver.opt;
        if(mode == 0){
            learn = 0;
            mis_size = n - vcSolver.solve();
            use_color=false;
            upperBound=0;
        }else{
            colors = new MISColor(adj);
            vcSolver.lpReduction();
//        misGraph.initialized_with_lpReduciton(vcSolver.x);
//            vcSolver = new VCSolver(adj,n);
            vcSolver.clear();
            mis_size = n - vcSolver.solve();
            mis = new MIS(vcSolver.y);
        }
    }
    private void initialize_using_color_and_opt(VCSolver vc, int u){
        int fu = colors.getColor(u);
        for(int i=0;i<n;i++)
            if(colors.getColor(i)!= fu && mis.getNodeState(i)==0)
                vc.reInitializeVertex(i, mis.getNodeState(i));
    }

    void optimize_vcsSolver(VCSolver vc, int u){
        if(upperBound==1) {
            vc.setMIS(mis);
            vc.setOptSize(vcs_size+1);
        }
        if(use_color)
            initialize_using_color_and_opt(vc, u);
        for(int i=0;i<n;i++)
            if((category[i]==1 || category[i]==3) && vc.x[i]<0){
                if(category[i]==3)  vc.reInitializeVertex(i, 1);
                else    vc.reInitializeVertex(i, 0);
            }
    }
    Boolean try_asterisk(int u){
        cnt_mis_solver++;
        vcSolver.clear();
        vcSolver.reInitializeVertex(u, 1);
        optimize_vcsSolver(vcSolver, u);
        int new_mis_size = n - vcSolver.solve();
        Debug.check(new_mis_size <= mis_size);
        if(learn==1 && new_mis_size == mis_size)
            misGraph.learn_from_opt(vcSolver.y, mis_size);
        if(memoryMeasure) System.gc();
        return new_mis_size == mis_size - 1;

    }
    Boolean try_minus(int u){
        cnt_mis_solver++;
        VCSolver tmpSolver = vcSolver;
        tmpSolver.clear();
        tmpSolver.reInitializeVertex(u, 0);
        optimize_vcsSolver(tmpSolver, u);
        int new_mis_size = n - tmpSolver.solve();
//        System.err.printf("%d%n",new_mis_size);
        Debug.check(new_mis_size <= mis_size);
        if(learn==1 && new_mis_size == mis_size)
            misGraph.learn_from_opt(tmpSolver.y, mis_size);
        if(memoryMeasure) System.gc();
        return new_mis_size < mis_size;
    }
    void updateMISGraph(int u, int kind){
        misGraph.oracle(u, kind);
        while(misGraph.clear_new_minus_queue());
        if(memoryMeasure) System.gc();
    }
    public Boolean check(){
        for(int i=0;i<n;i++)
            if(misGraph.category[i] == 3){
                vcSolver.clear();
                vcSolver.reInitializeVertex(i, 0);
                if(mis_size == n - vcSolver.solve()){
                    System.out.printf("%d： 3%n", i);
                    return false;
                }
            }else if(misGraph.category[i] == 1){
                vcSolver.clear();
                vcSolver.reInitializeVertex(i, 1);
                if(mis_size == n - vcSolver.solve()){
                    System.out.printf("1");
                    return false;
                }
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
        System.out.printf("asterisk:%d, plus:%d, minus:%d%n"
                , asterisk_cnt, plus_cnt, minus_cnt);
        System.out.printf("MIS Size:%d%n",mis_size);
        System.out.printf("We have called the module of solving MIS %d times.%n",cnt_mis_solver);
    }

    public void categoryVertices(){
        if(mode>=2)  misGraph.initializedTriangleCnt();
        while(misGraph.clear_new_minus_queue());
        System.err.printf("initialize OK!%n");
        for(int i=0;i<n;i++){
//            if(i%1000==0)
//                System.err.println(i);
            if(misGraph.category[i] <= 0){
                int kind = 2;
                if(misGraph.category[i] == -1){
                    if(try_asterisk(i)) kind = 1;
                }else if(misGraph.category[i] == -3){
                    if(try_minus(i)) kind = 3;
                }else{//category = 0
                    if(try_asterisk(i)) kind = 1;
                    else if(try_minus(i)) kind = 3;
                }
                updateMISGraph(i, kind);
//                System.err.printf("vertex:%d kind:%d%n",i, kind);
            }
        }
        if(check&&mode>0)
        Debug.check(check());
    }
    public void categoryVertices_with_priority(){
        //考虑在验证部分台添加优先级模块以进行加速
    }
}

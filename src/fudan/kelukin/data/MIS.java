package fudan.kelukin.data;

public class MIS {
    int x[];//0 代表取,1表示不取
    public MIS(int[] maximumIndependtSet){
        x = maximumIndependtSet.clone();
    }
    public int getNodeState(int no){
        return x[no];
    }
}

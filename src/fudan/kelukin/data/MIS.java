package fudan.kelukin.data;

public class MIS {
    int x[];
    public MIS(int[] maximumIndependtSet){
        x = maximumIndependtSet.clone();
    }
    public int getNodeState(int no){
        return x[no];
    }
}

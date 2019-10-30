package fudan.kelukin.data;

public class MISColor {
    int[] color;
    int[] stack;
    private  int findFa(int x){
        if(color[x] != x && stack == null) color[x] = findFa(color[x]);
        else if(stack != null){
            int head = 0, tail = 0;
            int tmp = x;
            while(color[tmp]!=tmp){
                stack[tail++] = tmp;
                tmp = color[tmp];
            }
            while(head<tail)
                color[stack[head++]] = tmp;
        }
        return color[x];
    }
    public int getColor(int x){
        return findFa(x);
    }
    public MISColor(int[][] adj){
        color = new int[adj.length];
        stack = new int[adj.length];
        for(int i=0;i<color.length; i++)
            color[i] = i;
        for(int i=0;i<color.length;i++)
            for(int j:adj[i])if(i<j){
                int fi = findFa(i), fj = findFa(j);
                if(fi != fj){
                    color[fi] = fj;
                    findFa(i);
                }
            }
        stack = null;
    }
}

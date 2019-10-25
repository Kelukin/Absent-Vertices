package tc.wata.data;

public class FastMap {
    int[] used;
    int[] value;
    int uid = 1;
    
    public FastMap(int n){
        used = new int[n];
        value = new int[n];
    }
    public int getValue(int key){
        if(used[key] != uid) return -1;
        return value[key];
    }
    public void setValue(int key, int value){
        if(add(key))
        this.value[key] = value;
    }
    public void clear() {
        uid++;
        if (uid < 0) {
            for (int i = 0; i < used.length; i++) used[i] = 0;
            uid = 1;
        }
    }
    public boolean add(int i) {
        boolean res = used[i] != uid;
        used[i] = uid;
        return res;
    }
    public void remove(int i) {
        used[i] = uid - 1;
    }
    public boolean exist(int i) {
        return used[i] == uid;
    }
}

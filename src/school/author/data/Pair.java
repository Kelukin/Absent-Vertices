package school.author.data;

public class Pair {
    public int key;
    public int value;
    public Pair(int _key, int _value){
        key = _key;value = _value;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Pair))
            return false;
        Pair testPair = (Pair) obj;
        return testPair.key == key && testPair.value == this.value;
    }

    @Override
    public int hashCode() {
        return new Integer(key).hashCode()<< 16 ^  new Integer(value).hashCode();
    }
}

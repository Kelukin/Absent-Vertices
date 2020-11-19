package fudan.kelukin.data;

import java.util.ArrayList;

public class Timer {
    static private long startTime = 0;
    static private long endTime = 0;
    static private ArrayList<Long> foundTime = new ArrayList<>();
    Timer(){}
    public static void setStartTime(long newTime){
        startTime = newTime;
    }
    public static void foundMinus(){
        foundTime.add((System.currentTimeMillis() - startTime) );
    }
    public static void setEndTime(long newTime){
        endTime = newTime;
    }
    public static long getPassedTime(long currentTime){
        return currentTime - startTime;
    }

    public static void printIncrementalProcess(){
        System.out.println("The incremental finding process:");
        int number = foundTime.size();
        for(int i = 0; i < number; ++i) {
            System.out.printf("%d %f%n", i + 1, foundTime.get(i) / 1e03);
        }
    }

    public static long getPassedTime(){
        return endTime - startTime;
    }
    public static long getCurrentPassedTime(){
        return (System.currentTimeMillis() - startTime) / 1000;
    }
}

import static java.util.Arrays.*;

import java.io.*;

import school.author.data.Timer;
import school.author.data.MISGraph;
import tc.wata.debug.*;
import tc.wata.util.*;
import tc.wata.util.SetOpt.*;

public class Main {
	@Option(abbr = 'u', usage = "0: close the upper bound optimization, 1: using the known MIS to optimize.")
	public static int upperBound = 1;

	@Option(abbr = 'w', usage = "0: Brute-force, 1: Synthetical, 2: Syn+EDR, 3: Syn+D-Chain, 4: SED")
	public static int workMode = 4;
	@Option(abbr = 't')
	public static boolean timeMeasure = false;
	@Option(abbr = 'm')
	public static boolean memoryMeasure = false;

	@Option(abbr = 's')
	public static boolean stopTimeSetting = false;
//	@Option(abbr = 'l', usage = "0: close learning from known MIS, 1: open the learning function.")
//	public static int learn = 1;
//	@Option(abbr = 'r', usage = "0: deg1+dominance+fold2, 1:LP, 2:unconfined+twin+funnel+desk, 3:packing")
	public static int reduction = 3;
	
//	@Option(abbr = 'l', usage = "0: nothing, 1:clique, 2:LP, 3:cycle, 4:all")
	public static int lb = 4;
	
//	@Option(abbr = 'b', usage = "0:random, 1:mindeg, 2:maxdeg")
	public static int branching = 2;
	
//	@Option(abbr = 'o')
	public static boolean outputLP = false;
	
//	@Option(abbr = 'p', usage = "Print the minimum vertex cover. The size of VC is in the first line. Each of the following lines contains the vertex ID.")
	public static boolean printVC = false;

	@Option(abbr = 'c')
	public static boolean check = false;
	@Option(abbr = 'd')
	public static int debug = 0;
	
	int[] vertexID;
	int[][] adj;
	public void outputDegreeInformation(String file, int[][] adj){
		try {
			BufferedReader in = new BufferedReader(new FileReader(file + ".categoryResult.txt"));
			String str;
			int cnt = 0;
			while ((str = in.readLine()) != null) {
				System.out.println(str);
				int tmp = 0;
				for(int i = 0; i < str.length(); ++i){
					if(str.charAt(i) == ' '){
						System.out.printf("%d %d\n", tmp, adj[tmp].length);
						tmp = 0;
						++cnt;
					}else{
						tmp = tmp * 10 + (str.charAt(i) - '0');
					}
				}
			}
		} catch (IOException e) {
		}
	}
	void read(String file) {
		if (file.endsWith(".dat")) {
			GraphIO io = new GraphIO();
			io.read(new File(file));
			adj = io.adj;
			vertexID = new int[adj.length];
			for (int i = 0; i < adj.length; i++) vertexID[i] = i;
		} else {
			GraphConverter conv = new GraphConverter();
			conv.file = file;
			conv.type = "snap";
			conv.undirected = true;
			conv.sorting = true;
			try {
				conv.read();
			} catch (Exception e) {
				conv = new GraphConverter();
				conv.file = file;
				conv.type = "dimacs";
				conv.undirected = true;
				conv.sorting = true;
				try {
					conv.read();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
			adj = conv.adj;
			vertexID = conv.vertexID;
		}
	}

	void run2(String file){
		System.err.printf("The work module is %d$n", workMode);
		MISGraph.mode = workMode;
		MISModifier.mode = workMode;
		MISModifier.check = check;
	    MISModifier.upperBound = upperBound;
	    MISModifier.memoryMeasure = memoryMeasure;
	    MISModifier.stopTimeSetting = stopTimeSetting;
		MISGraph.timeMeasure = timeMeasure;
//	    MISModifier.use_color = true;
//		MISModifier.learn = learn;
		VCSolver.nBranchings = 0;
		VCSolver.REDUCTION = reduction;
		VCSolver.LOWER_BOUND = lb;
		VCSolver.BRANCHING = branching;
		VCSolver.outputLP = outputLP;
		VCSolver.debug = debug;
		read(file);
		if (debug > 0) Stat.setShutdownHook();
		int m = 0;
		for (int i = 0; i < adj.length; i++) m += adj[i].length;
		m /= 2;
		System.out.printf("n = %d, m = %d%n", adj.length, m);
		MISModifier misModifier = new MISModifier(adj);
//		outputDegreeInformation(file, adj);
//		return;
		long start, end;
		Timer.setStartTime(System.currentTimeMillis());
		//start = System.currentTimeMillis();
		misModifier.categoryVertices();
		Timer.setEndTime(System.currentTimeMillis());
		//end = System.currentTimeMillis();
		System.out.printf("category time = %.3f%n",  1e-3 * Timer.getPassedTime());
		misModifier.printResult();
//
//		// out put the absent vertices.
//		misModifier.outputAbsentVertices(file + ".categoryResult.txt");
	}
	void run(String file) {
		long start, end;
		System.err.println("reading the input graph...");
		read(file);
		if (debug > 0) Stat.setShutdownHook();
		int m = 0;
		for (int i = 0; i < adj.length; i++) m += adj[i].length;
		m /= 2;
		System.err.printf("n = %d, m = %d%n", adj.length, m);
		VCSolver vc = new VCSolver(adj, adj.length);
		VCSolver.nBranchings = 0;
		VCSolver.REDUCTION = reduction;
		VCSolver.LOWER_BOUND = lb;
		VCSolver.BRANCHING = branching;
		VCSolver.outputLP = outputLP;
		VCSolver.debug = debug;
		int ans1;
		try (Stat stat = new Stat("solve")) {
			start = System.currentTimeMillis();
			ans1 = vc.solve();
			end = System.currentTimeMillis();
		}


		vc.clear();
		if(vc.solve() != ans1){
			System.err.printf("Error!%n");
			return;
		}
		System.out.printf("opt = %d, time = %.3f%n", vc.opt, 1e-3 * (end - start));
		read(file);
		int sum = 0;
		for (int i = 0; i < adj.length; i++) {
			sum += vc.y[i];
			Debug.check(vc.y[i] == 0 || vc.y[i] == 1);
			for (int j : adj[i]) Debug.check(vc.y[i] + vc.y[j] >= 1);
		}
		Debug.check(sum == vc.opt);
		if (debug > 0) {
			System.err.printf("%d\t%d\t%d\t%.3f\t%d%n", adj.length, m, vc.opt, 1e-3 * (end - start), VCSolver.nBranchings);
		}
		if (printVC) {
			System.out.println(sum);
			for (int i = 0; i < adj.length; i++) if (vc.y[i] > 0) {
				System.out.println(vertexID[i]);
			}
		}
	}
	
	void debug(Object...os) {
		System.err.println(deepToString(os));
	}
	
	public static void main(String[] args) {
		Main main = new Main();
		args = SetOpt.setOpt(main, args);
//		main.run(args[0]);
		main.run2(args[0]);
		if(stopTimeSetting)
			Timer.printIncrementalProcess();
		//System.out.printf("Memory Used: %f MB.%n",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed()/(1024*1024.0));
//		main.run(args[0]);
	}
}

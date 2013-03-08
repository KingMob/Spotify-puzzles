import java.io.*;
import java.util.*;


public class VertexCoverChecker {
	public static final String FNAME = "test-VertexCoverChecker.txt";

	public static void main(String[] args) throws Exception {
		if(args.length < 3){
			System.err.println("Insufficient arguments");
			System.exit(0);
		}

		final int NUM_TEAMS = (int) (Math.random() * Double.parseDouble(args[0])) + 1;
		final int NY_ID_RANGE = (int) (Math.random() * Double.parseDouble(args[1])) + 1;
		final int SE_ID_RANGE = (int) (Math.random() * Double.parseDouble(args[2])) + 1;

		// System.out.println("Using ")

		// while(true){
		for(int i = 0; i < 10000; i++){
			Set<GenTeam> teams = genFile(NUM_TEAMS, NY_ID_RANGE, SE_ID_RANGE);
			String result = execProg();
			// checkResult(result);
			// System.out.println(result);
			verifyOutput(teams, result);
		}
	}

	static Set<GenTeam> genFile(int NUM_TEAMS, int NY_ID_RANGE, int SE_ID_RANGE) throws IOException {
		FileWriter fout = new FileWriter(FNAME, false);

		// final int NUM_TEAMS = (int) (Math.random() * 10000.0);
		// final int NY_ID_RANGE = (int) (Math.random() * 1000.0);
		// final int SE_ID_RANGE = (int) (Math.random() * 1000.0);

		fout.append(String.valueOf(NUM_TEAMS) + "\n");

		int teamCount = 0;
		Set<GenTeam> uniqueTeams = new HashSet<GenTeam>();

		long start = System.currentTimeMillis();
		while(teamCount < NUM_TEAMS) {
			int team1 = (int) (Math.random() * NY_ID_RANGE) + 1000;
			int team2 = (int) (Math.random() * SE_ID_RANGE) + 2000;
			GenTeam currTeam = new GenTeam(team1, team2);
			if(!uniqueTeams.contains(currTeam)) {
				uniqueTeams.add(currTeam);
				fout.append(String.valueOf(team1)).append(' ').append(String.valueOf(team2)).append('\n');
				teamCount++;
			}

			if((System.currentTimeMillis() - start) > 4000){
				System.err.println("Took too long");
				fout.close();
				System.exit(0);
			}
		}
		fout.close();

		return uniqueTeams;
	}

	static String execProg() throws IOException, InterruptedException {
		List<String> command = new ArrayList<String>();
	    command.add("/bin/sh");
    	command.add("-c");
	    command.add("cat " + FNAME + " | ./BilateralBFS");

	
		SystemCommandExecutor commandExecutor = new SystemCommandExecutor(command);
		int result = commandExecutor.executeCommand();

		// get the output from the command
		StringBuilder stdout = commandExecutor.getStandardOutputFromCommand();
		StringBuilder stderr = commandExecutor.getStandardErrorFromCommand();

		// // print the output from the command
		// System.out.println("STDOUT");
		// System.out.println(stdout);
		// System.out.println("STDERR");
		// System.out.println(stderr);

		return stdout.toString();
	}

	static void verifyOutput(Set<GenTeam> uniqueTeams, String result) throws IOException{
		final List<GenTeam> teams = new ArrayList<GenTeam>(uniqueTeams);

		BufferedReader in = new BufferedReader(new StringReader(result));
		final int numIds = Integer.parseInt(in.readLine());

		List<Integer> ids = new ArrayList<Integer>(numIds);
		boolean[] edgeCovered = new boolean[teams.size()];

		String currLine = in.readLine();

		for(int currId = 0; currId < numIds; currId++){
			int id = Integer.parseInt(currLine);

			for(int i = 0; i < teams.size(); i++){
				GenTeam t = teams.get(i);

				if(id == t.getId(1) || id == t.getId(2)){
					edgeCovered[i] = true;
				}
			}

			currLine = in.readLine();
		}

		for(int i = 0; i < edgeCovered.length; i++){
			if(!edgeCovered[i]){
				System.err.println("Edge " + i + " not covered! Team(" + teams.get(i).getId(1) + ", " + teams.get(i).getId(2) + ")");
				System.err.println(result);
				System.exit(0);
			}	
		}
		in.close();
	}
}

class GenTeam {
	private int[] ids = new int[2];

	// Constructor
	public GenTeam(int id1, int id2){
		setIds(id1, id2);
	}

	// Accessors
	public int[] getIds() {
		return ids;
	}
	public int getId(int idNum) {
		return ids[idNum - 1];
	}

	public void setIds(int id1, int id2) {
		assert(id1>= 1000 && id1 <= 1999);
		assert(id2>= 2000 && id2 <= 2999);

		ids[0] = id1;
		ids[1] = id2;
	}

	// Object-inherited methods
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(ids[0]).append(", ").append(ids[1]);
		return sb.toString();
	}

	@Override
	public boolean equals(Object o){
		boolean result = false;
		if(o instanceof GenTeam){
			GenTeam oTeam = (GenTeam) o;
			result = Arrays.equals(ids, oTeam.getIds());
		}
		return result;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(ids);
	}
}

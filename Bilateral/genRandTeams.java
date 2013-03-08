import java.io.*;
import java.util.*;

public class genRandTeams {
	public static void main(String[] args) throws Exception {
		FileWriter fout = new FileWriter("test-max-rand-teams.txt");

		final int NUM_TEAMS = 10000;
		final int NY_ID_RANGE = 800;
		final int SE_ID_RANGE = 200;

		fout.append(String.valueOf(NUM_TEAMS) + "\n");

		int teamCount = 0;
		Set<GenTeam> uniqueTeams = new HashSet<GenTeam>();

		while(teamCount < NUM_TEAMS){
			int team1 = (int) (Math.random() * NY_ID_RANGE) + 1000;
			int team2 = (int) (Math.random() * SE_ID_RANGE) + 2000;
			GenTeam currTeam = new GenTeam(team1, team2);
			if(!uniqueTeams.contains(currTeam)) {
				uniqueTeams.add(currTeam);
				fout.append(String.valueOf(team1)).append(' ').append(String.valueOf(team2)).append('\n');
				teamCount++;
			}
		}

		fout.close();
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
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(ids[0]).append(", ").append(ids[1]);
		return sb.toString();
	}

	public boolean equals(Object o){
		boolean result = false;
		if(o instanceof Team){
			Team oTeam = (Team) o;
			result = Arrays.equals(ids, oTeam.getIds());
		}
		return result;
	}

	public int hashCode() {
		return Arrays.hashCode(ids);
	}
}

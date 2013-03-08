import java.io.*;

public class genMaxTeams {
	public static void main(String[] args) throws Exception {
		FileWriter fout = new FileWriter("test-max-teams.txt");

		fout.append("10000\n");

		for(int i = 1000; i <= 1009; i++) {
			for(int j = 2000; j <= 2999; j++) {
				fout.append(String.valueOf(i)).append(' ').append(String.valueOf(j)).append('\n');
			}
		}

		fout.close();
	}
}
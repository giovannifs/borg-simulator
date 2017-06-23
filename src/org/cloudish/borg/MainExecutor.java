package org.cloudish.borg;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public class MainExecutor {
	
	public static void main(String[] args) throws IOException {
		
		String infraFilePath = args[0];		
		String workloadFilePath = args[1];
		
		List<Host> hosts = createHosts(infraFilePath);
		List<Task> tasks = createTasks(workloadFilePath);
		
		List<Task> pendingQueue = new ArrayList<Task>();
		
		// allocating tasks
		for (Task task : tasks) {
			double bestScore = -1;
			Host bestHost = null;
			
			for (Host host : hosts) {
				double score = host.getScore(task);
				if (score > bestScore) {
					bestScore = score;
					bestHost = host;
				}
			}
			
			if (bestScore >= 0 && bestHost != null){
				bestHost.allocate(task);
			} else {
				pendingQueue.add(task);
			}			
		}
		
		// generating host outputs
		for (Host host : hosts) {
			System.out.println();
		}
	}

	private static List<Task> createTasks(String workloadFilePath) throws IOException {
		List<Task> tasks = new ArrayList<Task>();
		
		BufferedReader br = new BufferedReader(new FileReader(workloadFilePath));
		try {
//		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
		    	System.out.println(line);
		    	tasks.add(new Task(line));
//		        sb.append(line);
//		        sb.append(System.lineSeparator());
		        line = br.readLine();
		    }
//		    String everything = sb.toString();
		} finally {
		    br.close();
		}
		
		return tasks;
	}

	private static List<Host> createHosts(String infraFilePath) throws IOException {
		List<Host> hosts = new ArrayList<Host>();
		
		BufferedReader br = new BufferedReader(new FileReader(infraFilePath));
		try {
		    String line = br.readLine();
		    while (line != null) {
		    	System.out.println(line);
		    	hosts.add(new Host(line));
		    	line = br.readLine();
		    }
		} finally {
		    br.close();
		}
		return hosts;
	}

}

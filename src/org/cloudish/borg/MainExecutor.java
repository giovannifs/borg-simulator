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
		
		long startTime = System.currentTimeMillis();
		
		String infraFilePath = args[0];		
		String workloadFilePath = args[1];
		
		List<Host> hosts = createHosts(infraFilePath);
		List<Task> tasks = createTasks(workloadFilePath);
		
		List<Task> pendingQueue = new ArrayList<Task>();
		
		int index = 0;
		// allocating tasks
		for (Task task : tasks) {
			System.out.println("Task index:" + index);
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
				System.out.println("Task " + index+ " allocated.");
				bestHost.allocate(task);
			} else {
				System.out.println("Task " + index + " pending");
				pendingQueue.add(task);
			}
			index++;
		}
		
		System.out.println("pending-queue=" + pendingQueue.size());
		
		// generating host outputs
		for (Host host : hosts) {
			System.out.println(host.getId() + "," + host.getCpuCapacity() + "," + host.getFreeCPU() + ","
					+ host.getMemCapacity() + "," + host.getFreeMem());
		}
		
		long now = System.currentTimeMillis();
		
		System.out.println("execution time: " + (now - startTime) + " milliseconds.");
	}

//	#%% format {timestamp,task_id,job_id,cpu_req,mem_req,priority,constraints}
//	#{1,0,1,2,2,0,[{"rs", "==", "1"}, {"o/", "/=", "1"}]}.
//	#{1,1,1,4,2,0,[{"rs", "==", "1"}, {"o/", "/=", "0"}]}.
//	#{1,2,1,2,4,0,[{"rs", "==", "1"}, {"o/", "/=", "0"}]}.
//	#{0,0,3418309,0.125,0.07446,9, []}.
	private static List<Task> createTasks(String workloadFilePath) throws IOException {
		List<Task> tasks = new ArrayList<Task>();
		
		BufferedReader br = new BufferedReader(new FileReader(workloadFilePath));
		try {
//		    StringBuilder sb = new StringBuilder();
		    String line = br.readLine();

		    while (line != null) {
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

//	#%% Format: {host_id,host_name,cpu_capacity,mem_capacity,rs,0/,Ql,maq}
//	#{0,"Host_1",8,16,[{"rs","1"},{"o/","1"},{"Ql","1"},{"ma","1"}]}.
//	#{1,"Host_2",16,16,[{"rs","1"},{"o/","1"},{"Ql","1"},{"ma","1"}]}.

	
	private static List<Host> createHosts(String infraFilePath) throws IOException {
		List<Host> hosts = new ArrayList<Host>();
		
		BufferedReader br = new BufferedReader(new FileReader(infraFilePath));
		try {
		    String line = br.readLine();
		    while (line != null) {
		    	hosts.add(new Host(line));
		    	line = br.readLine();
		    }
		} finally {
		    br.close();
		}
		return hosts;
	}

}

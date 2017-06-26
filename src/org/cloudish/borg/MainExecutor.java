package org.cloudish.borg;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public class MainExecutor {
	
	public static void main(String[] args) throws IOException {
	
		Properties properties = new Properties();
		
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		long startTime = System.currentTimeMillis();
		
		String infraFilePath = properties.getProperty("infra_file_path");		
		String workloadFilePath = properties.getProperty("workload_file_path");
		double admittedPendingFraction = Double.parseDouble(properties.getProperty("admited_pending_fraction"));
		
		String outputDir = properties.getProperty("output_dir");
		
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists() || !outputDirFile.isDirectory()) {
			outputDirFile.mkdir();
		}
		
		List<Host> hosts = createHosts(infraFilePath);		
		
		int min = 0;
		int max = hosts.size();
		
		while(max != min) {
			int mid = (int)((min + max)/2);
			
			// chosen first mid hosts
			List<Host> chosenHosts = getFirstHosts(hosts, mid);
			
			System.out.println("hosts="+chosenHosts.size());
			
			List<Task> pendingQueue = new ArrayList<Task>();
			
			long numberOfTasks = allocateTasks(workloadFilePath, chosenHosts, pendingQueue);
			double pendingQueueFraction = new Double(pendingQueue.size())/new Double(numberOfTasks);
			
			saveHostInfo(properties, chosenHosts);
			savePendingQueueInfo(properties, chosenHosts, pendingQueue);
			
			System.out.println("pending-queue-tasks=" + pendingQueue.size());
			System.out.println("pending-queue-fraction=" + pendingQueueFraction);
			
			// checking pending queue fraction
			if (pendingQueueFraction > admittedPendingFraction) {
				min = mid;
			} else {
				max = mid;
			}
		}	
		
		long now = System.currentTimeMillis();		
		System.out.println("execution time: " + (now - startTime) + " milliseconds.");
	}

	private static List<Host> getFirstHosts(List<Host> hosts, int mid) {
		List<Host> firstHosts = new ArrayList<>();
		for (int i = 0; i < mid; i++) {
			firstHosts.add(hosts.get(i).clone());
		}
		return firstHosts;
	}

	private static void savePendingQueueInfo(Properties properties, List<Host> chosenHosts, List<Task> pendingQueue)
			throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(
				properties.getProperty("output_dir") + "/pending-queue-" + chosenHosts.size() + "-hosts.csv", "UTF-8");
		writer.println("tid,jid");
		for (Task task : pendingQueue) {
			writer.println(task.getTid() + "," + task.getJid());
		}
		writer.close();
	}

	private static long allocateTasks(String workloadFilePath, List<Host> chosenHosts, List<Task> pendingQueue)
			throws FileNotFoundException, IOException {

		long startAllocation = System.currentTimeMillis();
		
		System.out.println("Allocating tasks into " + chosenHosts.size() + " hosts.");
		
		int taskIndex = 0;
		
		BufferedReader br = new BufferedReader(new FileReader(workloadFilePath));
		try {
			String line = br.readLine();
			
			while (line != null) {
				Task task = new Task(line);
				
				System.out.println("Task index:" + taskIndex);
				double bestScore = -1;
				Host bestHost = null;
				
				for (Host host : chosenHosts) {
					double score = host.getScore(task);
					if (score > bestScore) {
						bestScore = score;
						bestHost = host;
					}
				}
				
				if (bestScore >= 0 && bestHost != null) {
					System.out.println("Task " + taskIndex + " allocated.");
					bestHost.allocate(task);
				} else {
					System.out.println("Task " + taskIndex + " goes to pending queue.");
					pendingQueue.add(task);
				}
				taskIndex++;
				line = br.readLine();
			}
		} finally {
			br.close();
		}
		
		long now = System.currentTimeMillis();
		
		System.out.println("Allocation execution time for " + chosenHosts.size() + " hosts is "
				+ (now - startAllocation) + " miliseconds.");
		
		return taskIndex;
	}

	private static void saveHostInfo(Properties properties, List<Host> chosenHosts)
			throws FileNotFoundException, UnsupportedEncodingException {
		// generating host outputs
		PrintWriter writer = new PrintWriter(
				properties.getProperty("output_dir") + "/allocation-" + chosenHosts.size() + "-hosts.csv", "UTF-8");
		writer.println("hostId,cpuCapacity,freeCpu,memCapacity,freeMem");
		for (Host host : chosenHosts) {
			writer.println(host.getId() + "," + host.getCpuCapacity() + "," + host.getFreeCPU() + ","
					+ host.getMemCapacity() + "," + host.getFreeMem());
		}
		writer.close();
	}

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

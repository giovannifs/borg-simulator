package org.cloudish.dh;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.LogicalServer;
import org.cloudish.dh.model.ResourcePool;

public class DHMainExecutor {

	public static void main(String[] args) throws IOException {
		Properties properties = new Properties();
		
		FileInputStream input = new FileInputStream(args[0]);
		properties.load(input);
		
		long startTime = System.currentTimeMillis();
		
		String infraFilePath = properties.getProperty("infra_file_path");		
		String workloadFilePath = properties.getProperty("workload_file_path");
				
		String outputDir = properties.getProperty("output_dir");
		createOutputDir(outputDir);
		
		List<ResourcePool> resourcePools = createResourcePools(infraFilePath);
		
		DHManeger dhManager = new DHManeger(resourcePools);
		
		LogicalServer firstServer = dhManager.createLogicalServer(null);
		List<LogicalServer> logicalServers = new ArrayList<>();
		logicalServers.add(firstServer);
		
		List<Task> pendingQueue = new ArrayList<>();
		
		// allocating the tasks		
		int numberOfTasks = 0;
		
		BufferedReader br = new BufferedReader(new FileReader(workloadFilePath));
		try {
			String line = br.readLine();
			
			while (line != null) {
				Task task = new Task(line);
				
				System.out.println("Task index:" + numberOfTasks);
				
				for (LogicalServer lServer : logicalServers) {
					
				}
				
				numberOfTasks++;
				line = br.readLine();
			}
		} finally {
			br.close();
		}
				
		double pendingQueueFraction = new Double(pendingQueue.size())/new Double(numberOfTasks);

		// saveHostInfo(properties, chosenHosts);
		// savePendingQueueInfo(properties, chosenHosts, pendingQueue);

		System.out.println("pending-queue-tasks=" + pendingQueue.size());
		System.out.println("pending-queue-fraction=" + pendingQueueFraction);
	
		long now = System.currentTimeMillis();		
		System.out.println("execution time: " + (now - startTime) + " milliseconds.");
	}
	
	private static void createOutputDir(String outputDir) {
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists() || !outputDirFile.isDirectory()) {
			outputDirFile.mkdir();
		}
	}

	private static List<ResourcePool> createResourcePools(String infraFilePath) {
		// TODO Auto-generated method stub
		return null;
	}
}

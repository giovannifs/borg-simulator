package org.cloudish.dh;

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
import java.util.Map;
import java.util.Properties;

import org.cloudish.borg.model.Host;
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
		
		List<Host> hosts = createHosts(infraFilePath);

		Map<String, List<ResourcePool>> resourcePools = Utils.createResourcePoolsFromHosts(hosts);
		
		System.out.println("How many MemPool? " + resourcePools.get(ResourcePool.MEMORY_TYPE).size());
		System.out.println("MemPool size: " + resourcePools.get(ResourcePool.MEMORY_TYPE).get(0).getCapacity());
		
		System.out.println("How many CpuPool? " + resourcePools.get(ResourcePool.CPU_TYPE).size());
		for (ResourcePool pool : resourcePools.get(ResourcePool.CPU_TYPE)) {
			System.out.println(pool.getId() + " - size: " + pool.getCapacity());
		}
		
		System.out.println("----------------------------------------------------------------------");
		
		List<String> GKValues = Utils.getPossibleGKValues(hosts);
		
		DHManager dhManager = new DHManager(properties, resourcePools, GKValues);
		
		// creating initial DH-based infrastructure
		while (dhManager.getLogicalServers().size() < dhManager.getMinLogicalServer()) {
			
			LogicalServer logicalServer = dhManager.createLogicalServer(null);
			dhManager.addLogicalServer(logicalServer);			
		}
				
		// allocating the tasks
		int numberOfTasks = 0;
		
		BufferedReader br = new BufferedReader(new FileReader(workloadFilePath));
		try {
			String line = br.readLine();

			while (line != null) {
				Task task = new Task(line);

				System.out.println("Task index:" + ++numberOfTasks);

				boolean result = dhManager.allocate(task);
				
				if (result) {
					System.out.println("Task " + numberOfTasks + " allocated.");
				} else {
					System.out.println("Task " + numberOfTasks + " went to pending queue.");
				}

				line = br.readLine();
			}
		} finally {
			br.close();
		}

		double pendingQueueFraction = new Double(dhManager.getPendingQueue().size())/new Double(numberOfTasks);

		 saveLogicalServerInfo(outputDir, dhManager.getLogicalServers());
		 savePendingQueueInfo(outputDir, dhManager);

		System.out.println("logical-servers=" + dhManager.getLogicalServers().size());
		System.out.println("pending-queue-tasks=" + dhManager.getPendingQueue().size());
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

	private static List<Host> createHosts(String infraFilePath) throws FileNotFoundException, IOException {
		// creating hosts
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
	
	private static void saveLogicalServerInfo(String outputDir, List<LogicalServer> logicalServers)
			throws FileNotFoundException, UnsupportedEncodingException {
		// generating host outputs
		PrintWriter writer = new PrintWriter(outputDir + "/allocation-" + logicalServers.size() + "-logicalservers.csv", "UTF-8");
		writer.println("cpuPoolId,cpuCapacity,freeCpu,memCapacity,freeMem,gkAttr,QlAttr");
		for (LogicalServer logicalServer : logicalServers) {
			writer.println(logicalServer.getCpuPool().getId() + "," + logicalServer.getCpuCapacity() + ","
					+ logicalServer.getFreeCPU() + "," + logicalServer.getMemCapacity() + ","
					+ logicalServer.getFreeMem() + "," + logicalServer.getGKAttr() + "," + logicalServer.getQlAttr());
		}
		writer.close();
	}
	
	private static void savePendingQueueInfo(String outputDir, DHManager dhManager)
			throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(
				outputDir + "/pending-queue-" + dhManager.getLogicalServers().size() + "-logicalservers.csv", "UTF-8");
		writer.println("tid,jid,cpuReq,memReq,priority");
		for (Task task : dhManager.getPendingQueue()) {
			writer.println(task.getTid() + "," + task.getJid() + "," + task.getCpuReq() + "," + task.getMemReq() + "," + task.getPriority());
		}
		writer.close();
	}
}

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
		
		boolean isConstraintsOn = properties.getProperty("placement_constraints_on") == null
				|| properties.getProperty("placement_constraints_on").equals("yes") ? true : false;

		System.out.println("isConstraintOn? " + (isConstraintsOn));
		
		Map<String, List<ResourcePool>> resourcePools = Utils.createResourcePoolsFromHosts(hosts, isConstraintsOn);
		
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
		String dhModel = properties.getProperty("dh_model");
		
		// creating DH-based infra considering blade DH model
		if (dhModel.equals("blade")){
			System.out.println("Running blade DH model...");
			
			for (Host host : hosts) {
				LogicalServer logicalServer = dhManager.createLogicalServer(host.getAttributes());
				dhManager.addLogicalServer(logicalServer);
			}
		} else if (dhModel.equals("drawer")) {
			System.out.println("Running drawer DH model...");
			
			for (ResourcePool cpuPool : dhManager.getResourcePools().get(ResourcePool.CPU_TYPE)) {
				int numberOfInitialServers = Utils.getInitialNumberOfServers(cpuPool, 16);
				
				// creating initial servers
				for (int i = 0; i < numberOfInitialServers; i++) {
					LogicalServer logicalServer = dhManager.createLogicalServer(cpuPool.getAttributes());
					dhManager.addLogicalServer(logicalServer);
				}
			}
			
		} else {
			throw new RuntimeException("Invalid DH model informed.");
		}
		
		System.out.println("Execution initiated with " + dhManager.getLogicalServers().size() + " logical servers.");
		
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

		saveOutput(properties, outputDir, dhManager);
		
		System.out.println("logical-servers=" + dhManager.getLogicalServers().size());
		System.out.println("pending-queue-tasks=" + dhManager.getPendingQueue().size());
		System.out.println("pending-queue-fraction=" + pendingQueueFraction);
	
		long now = System.currentTimeMillis();		
		System.out.println("execution time: " + (now - startTime) + " milliseconds.");
	}
	
	private static void createOutputDir(String outputDir) {
		File outputDirFile = new File(outputDir);
		if (!outputDirFile.exists() || !outputDirFile.isDirectory()) {
			outputDirFile.mkdirs();
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
	
	private static void saveOutput(Properties properties, String outputDir, DHManager dhManager)
			throws FileNotFoundException, UnsupportedEncodingException {
		
		// generating host outputs
		int nLogicalServers = dhManager.getLogicalServers().size();
		String isConstraintsOn = properties.getProperty("placement_constraints_on") == null
				|| properties.getProperty("placement_constraints_on").equals("yes") ? "on" : "off";

		//String minLogicalServer = properties.getProperty("min_logical_servers");
		String maxCpuServerCapacity = properties.getProperty("max_cpu_logical_server_capacity");
		String maxMemServerCapacity = properties.getProperty("max_memory_logical_server_capacity");
		String dhModel = properties.getProperty("dh_model");
		
		PrintWriter writer = new PrintWriter(outputDir + "/allocation-" + isConstraintsOn + "-" + dhModel + "-"
				+ maxCpuServerCapacity + "-" + maxMemServerCapacity + "-" + nLogicalServers + "-servers.csv", "UTF-8");
		writer.println("cpuPoolId,cpuCapacity,freeCpu,memCapacity,freeMem,gkAttr,QlAttr");
		for (LogicalServer logicalServer : dhManager.getLogicalServers()) {
			writer.println(logicalServer.getCpuPool().getId() + "," + logicalServer.getCpuCapacity() + ","
					+ logicalServer.getFreeCPU() + "," + logicalServer.getMemCapacity() + ","
					+ logicalServer.getFreeMem() + "," + logicalServer.getGKAttr() + "," + logicalServer.getQlAttr());
		}
		writer.close();
		
		//writing pending queue
		writer = new PrintWriter(outputDir + "/pending-queue-" + isConstraintsOn + "-" + dhModel + "-"
				+ maxCpuServerCapacity + "-" + maxMemServerCapacity + "-" + nLogicalServers + "-servers.csv", "UTF-8");
		writer.println("tid,jid,cpuReq,memReq,priority,couldCreateServer");
		for (Task task : dhManager.getPendingQueue()) {
			writer.println(task.getTid() + "," + task.getJid() + "," + task.getCpuReq() + "," + task.getMemReq() + ","
					+ task.getPriority() + "," + task.isCouldCreateNewServer());
		}
		writer.close();
	}
}

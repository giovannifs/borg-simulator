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
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;
import org.cloudish.score.KubernetesRankingScore;

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
		checkAndCreateDir(outputDir);
		
		int numberOfPermutations = Integer.parseInt(properties.getProperty("number_of_host_permutations"));
		
		if (numberOfPermutations < 1) {
			throw new IllegalArgumentException("number_of_host_permutations property must be a positive value.");
		}
		
		boolean isConstraintsOn = properties.getProperty("placement_constraints_on") == null
				|| properties.getProperty("placement_constraints_on").equals("yes") ? true : false;
		
		List<Host> hosts = createHosts(infraFilePath, isConstraintsOn);		
		
		System.out.println("isConstraintOn? " + (isConstraintsOn));
		
		// allocating in all hosts - without cluster compaction
		System.out.println("Allocating considering all " + hosts.size() + " hosts...");
		List<Host> chosenHosts = getFirstHosts(hosts, hosts.size());
		List<Task> pendingQueue = new ArrayList<Task>();
		
		long numberOfTasks = allocateTasks(workloadFilePath, chosenHosts, pendingQueue);
		double pendingQueueFraction = new Double(pendingQueue.size())/new Double(numberOfTasks);
		
		saveOutput(outputDir, chosenHosts, isConstraintsOn, pendingQueue);
//		savePendingQueueInfo(outputDir, chosenHosts, pendingQueue);
		
		System.out.println("hosts=" + chosenHosts.size());
		System.out.println("pending-queue-tasks=" + pendingQueue.size());
		System.out.println("pending-queue-fraction=" + pendingQueueFraction);
		
		long now = System.currentTimeMillis();		
		System.out.println("first allocation execution time: " + (now - startTime) + " milliseconds.");
		
		if (pendingQueueFraction > admittedPendingFraction) {
			System.out.println("It is not possible fulfill " + admittedPendingFraction
					+ " pending queue fraction considering full infrastructure.");
			System.exit(0);
			
		} else {
			int permutation = 1;
			while (permutation <= numberOfPermutations) {
				// creating output directory
				String permutationOutDir = outputDir + "/host_permutation_" + permutation;
				checkAndCreateDir(permutationOutDir);
				
				// saving host permutation
				saveHostPermutation(permutationOutDir, hosts);
				
				int permutationIndex = 1;
				
				int min = 0;
				int max = hosts.size();
				int mid = (int)((min + max)/2);
				
				List<Integer> previousMids = new ArrayList<>();
				
				while(!previousMids.contains(mid)) {
					System.out.println("Executing scenario " + permutation + " round " + permutationIndex++);
					
					// chosen first mid hosts
					chosenHosts = getFirstHosts(hosts, mid);
					
					System.out.println("scenario " + permutation + " round " + permutationIndex+ " - hosts="+chosenHosts.size());
					
					pendingQueue = new ArrayList<Task>();
					
					numberOfTasks = allocateTasks(workloadFilePath, chosenHosts, pendingQueue);
					pendingQueueFraction = new Double(pendingQueue.size())/new Double(numberOfTasks);
					
					saveOutput(permutationOutDir, chosenHosts, isConstraintsOn, pendingQueue);
//					savePendingQueueInfo(permutationOutDir, chosenHosts, pendingQueue);
					
					System.out.println("scenario " + permutation + " round " + permutationIndex+ " - pending-queue-tasks=" + pendingQueue.size());
					System.out.println("scenario " + permutation + " round " + permutationIndex+ " - pending-queue-fraction=" + pendingQueueFraction);
					
					// checking pending queue fraction
					if (pendingQueueFraction > admittedPendingFraction) {
						min = mid;
					} else {
						max = mid;
					}
					
					// updating variables
					previousMids.add(mid);
					mid = (int)((min + max)/2);
				}
				
				// shuffle hosts
				Collections.shuffle(hosts);
				permutation++;			
			}		
			
		}
		
		now = System.currentTimeMillis();		
		System.out.println("execution time: " + (now - startTime) + " milliseconds.");
	}

	private static String checkAndCreateDir(String dir) {
		File permutationOutDirFile = new File(dir);
		if (!permutationOutDirFile.exists() || !permutationOutDirFile.isDirectory()) {
			permutationOutDirFile.mkdir();
		}
		return dir;
	}

	private static void saveHostPermutation(String permutationOutDir, List<Host> hosts)
			throws FileNotFoundException, UnsupportedEncodingException {
		PrintWriter writer = new PrintWriter(permutationOutDir + "/host-permutation.csv", "UTF-8");
		for (Host host : hosts) {
			writer.println(host.getHostLine());
		}
		writer.close();
	}

	private static List<Host> getFirstHosts(List<Host> hosts, int mid) {
		List<Host> firstHosts = new ArrayList<>();
		for (int i = 0; i < mid; i++) {
			firstHosts.add(hosts.get(i).clone());
		}
		return firstHosts;
	}

//	private static void savePendingQueueInfo(String outputDir, List<Host> chosenHosts, List<Task> pendingQueue)
//			throws FileNotFoundException, UnsupportedEncodingException {
//		PrintWriter writer = new PrintWriter(
//				outputDir + "/pending-queue-" + chosenHosts.size() + "-hosts.csv", "UTF-8");
//		writer.println("tid,jid,cpuReq,memReq,priority");
//		for (Task task : pendingQueue) {
//			writer.println(task.getTid() + "," + task.getJid() + "," + task.getCpuReq() + "," + task.getMemReq() + "," + task.getPriority());
//		}
//		writer.close();
//	}

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
				List<Host> bestHosts = new ArrayList<>();
				
				for (Host host : chosenHosts) {
					double score = host.getScore(task);
					if (score > bestScore) {
						bestScore = score;
						bestHosts.clear();
						bestHosts.add(host);
					} else if (score == bestScore) {
						bestHosts.add(host);
					}
				}
								
				if (bestScore >= 0 && !bestHosts.isEmpty()) {
					Random r = new Random();

					// choosing a random host inside of best hosts list
					Host bestHost = bestHosts.get(r.nextInt(bestHosts.size()));
					bestHost.allocate(task);
					
					System.out.println("Task " + taskIndex + " allocated.");
				} else {
					pendingQueue.add(task);
					System.out.println("Task " + taskIndex + " went to pending queue.");
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

	private static void saveOutput(String outputDir, List<Host> chosenHosts, boolean isConstraintOn, List<Task> pendingQueue)
			throws FileNotFoundException, UnsupportedEncodingException {
		// generating host outputs
		String constraint = "";
		if (isConstraintOn) {
			constraint = "on";
		} else {
			constraint = "off";
		}
		
		PrintWriter writer = new PrintWriter(outputDir + "/allocation-" + constraint + "-"+ chosenHosts.size() + "-hosts.csv", "UTF-8");
		writer.println("hostId,cpuCapacity,freeCpu,memCapacity,freeMem");
		for (Host host : chosenHosts) {
			writer.println(host.getId() + "," + host.getCpuCapacity() + "," + host.getFreeCPU() + ","
					+ host.getMemCapacity() + "," + host.getFreeMem());
		}
		writer.close();
		
		// saving pending queue
		writer = new PrintWriter(
				outputDir + "/pending-queue-" + constraint + "-"+ chosenHosts.size() + "-hosts.csv", "UTF-8");
		writer.println("tid,jid,cpuReq,memReq,priority");
		for (Task task : pendingQueue) {
			writer.println(task.getTid() + "," + task.getJid() + "," + task.getCpuReq() + "," + task.getMemReq() + "," + task.getPriority());
		}
		writer.close();

	}

	private static List<Host> createHosts(String infraFilePath, boolean isConstraintOn) throws IOException {
		List<Host> hosts = new ArrayList<Host>();
		
		BufferedReader br = new BufferedReader(new FileReader(infraFilePath));
		try {
		    String line = br.readLine();
		    while (line != null) {
		    	hosts.add(new Host(line, new KubernetesRankingScore(), isConstraintOn));
		    	line = br.readLine();
		    }
		} finally {
		    br.close();
		}
		return hosts;
	}
}

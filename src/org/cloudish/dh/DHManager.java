package org.cloudish.dh;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.LogicalServer;
import org.cloudish.dh.model.ResourcePool;

public class DHManager {

	private List<LogicalServer> logicalServers = new ArrayList<>();
	private List<Task> pendingQueue = new ArrayList<>();
	
	public DHManager(Properties properties, List<ResourcePool> resourcePools) {
		// TODO Auto-generated constructor stub
	}

	public LogicalServer createLogicalServer(Task task) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean allocate(Task task) {
		
		double bestScore = -1;
		List<LogicalServer> bestLogicalServers = new ArrayList<>();
		
		for (LogicalServer lServer : logicalServers) {
			double score = lServer.getScore(task);
			
			if (score > bestScore) {
				bestScore = score;
				bestLogicalServers.clear();
				bestLogicalServers.add(lServer);
			} else if (score == bestScore) {
				bestLogicalServers.add(lServer);
			}
		}

		// There is not feasible logical server 
		if (bestScore < 0 && bestLogicalServers.isEmpty()) {
			
			// create new logicalServer					
			LogicalServer newLServer = createLogicalServer(task);
			
			if (newLServer != null) {				
				logicalServers.add(newLServer);
				newLServer.allocate(task);
				return true;				
			
			} else { // there was not resource to create a new logical server that fulfills the task
				
				pendingQueue.add(task);
				return false;
			}			
		} else {
			
			List<LogicalServer> filteredBestServers = new ArrayList<>();
			
			// there are more than one logical server with the best score
			if (bestLogicalServers.size() > 1) {

				//order by scale_up_cpu and scale_up_mem

			} 
			
//			LogicalServer bestLServer;
//			bestLServer = bestLogicalServers.get(0);			
//				
//			
//			Random r = new Random();
//
//			// choosing a random host inside of best hosts list
//			Host bestHost = bestLogicalServers.get(r.nextInt(bestLogicalServers.size()));
//			bestHost.allocate(task);
		}
		
		return true;
	}

	public void addLogicalServer(LogicalServer firstServer) {
		// TODO Auto-generated method stub
		
	}

	public List<LogicalServer> getLogicalServers() {
		return logicalServers;
	}

	public List<Task> getPendingQueue() {
		return pendingQueue;
	}

	public boolean hasMinimumLogicalServer() {
		// TODO Auto-generated method stub
		return false;
	}

	public void createMinimumLogicalServer() {
		// TODO Auto-generated method stub
		
	}
}

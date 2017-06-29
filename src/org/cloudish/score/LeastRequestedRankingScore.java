package org.cloudish.score;

import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.Server;

/**
 *
 * LeastRequestedPriority is a priority function that favors nodes with fewer
 * requested resources. It calculates the percentage of memory and CPU requested
 * by pods scheduled on the node, and prioritizes based on the minimum of the
 * average of the fraction of requested to capacity.
 * 
 * Details: cpu((capacity - * sum(requested)) * 10 / capacity) +
 * memory((capacity - sum(requested)) * 10 / capacity) / 2
 * 
 * This definition was gotten from Kubernetes repository
 * https://github.com/kubernetes/kubernetes/blob/e0a6cde6f43e5c628395f8f9b5589c5d6298ec8e/plugin/pkg/scheduler/algorithm/priorities/least_requested.go
 * 
 * @author giovanni
 *
 */


public class LeastRequestedRankingScore implements RankingScore {

	@Override
	public double calculateScore(Task task, Server host) {
		if (host == null) {
			throw new IllegalArgumentException("Host must not be null.");
		}

		if (host.getCpuCapacity() == 0 || host.getMemCapacity() == 0) {
			return 0;
		}
		
		// The unused capacity is calculated on a scale of 0-10
		// 0 being the lowest priority and 10 being the highest.
		// The more unused resources the higher the score is.
		double cpuScore = host.getFreeCPU() * 10 / host.getCpuCapacity();
		double memScore = host.getFreeMem() * 10 / host.getMemCapacity();
		
		// Calculates host priority based on the amount of unused resources.
		return (int) ((cpuScore + memScore) / 2);
	}
	
}

package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

/**
 * BalancedResourceAllocation favors nodes with balanced resource usage rate.
 * BalancedResourceAllocation should **NOT** be used alone, and **MUST** be used
 * together with LeastRequestedPriority. It calculates the difference between
 * the cpu and memory fracion of capacity, and prioritizes the host based on how
 * close the two metrics are to each other. Detail: score = 10 -
 * abs(cpuFraction-memoryFraction)*10. The algorithm is partly inspired by: "Wei
 * Huang et al. An Energy Efficient Virtual Machine Placement Algorithm with
 * Balanced Resource Utilization"
 * 
 * This definition was gotten from kubernetes repository
 * https://github.com/kubernetes/kubernetes/blob/e0a6cde6f43e5c628395f8f9b5589c5d6298ec8e/plugin/pkg/scheduler/algorithm/priorities/balanced_resource_allocation.go
 * 
 * @author giovanni
 *
 */

public class BalancedResourceAllocationRankingScore implements RankingScore {

	@Override
	public double calculateScore(Task task, Host host) {
		if (host == null) {
			throw new IllegalArgumentException("Host must not be null.");
		}
		
		double totalRequestedCpu = host.getCpuCapacity() - host.getFreeCPU();
		double cpuFraction = totalRequestedCpu / host.getCpuCapacity();

		double totalRequestedMem = host.getMemCapacity() - host.getFreeMem();
		double memFraction = totalRequestedMem / host.getMemCapacity();
		
		// if requested >= capacity, the corresponding host should never be preferred.
		if (cpuFraction >= 1 || memFraction >= 1) {
			return 0;
		} else {
			/*
			 * Upper and lower boundary of difference between cpuFraction and
			 * memoryFraction are -1 and 1 respectively. Multilying the absolute
			 * value of the difference by 10 scales the value to 0-10 with 0
			 * representing well balanced allocation and 10 poorly balanced.
			 * Subtracting it from 10 leads to the score which also scales from
			 * 0 to 10 while 10 representing well balanced.
			 */			 
			double diff = Math.abs(cpuFraction - memFraction);
			return (int) (10 - diff*10);
		}
	}

}

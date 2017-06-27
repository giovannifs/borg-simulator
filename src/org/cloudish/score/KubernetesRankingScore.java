package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

/**
 * The algorithm of how to select a node for the Pod is explained. There are two
 * steps before a destination node of a Pod is chosen. The first step is
 * filtering all the nodes and the second is ranking the remaining nodes to find
 * a best fit for the Pod.
 * 
 * The filtered nodes are considered suitable to host the Pod, and it is often
 * that there are more than one nodes remaining. Kubernetes prioritizes the
 * remaining nodes to find the "best" one for the Pod. The prioritization is
 * performed by a set of priority functions. For each remaining node, a priority
 * function gives a score which scales from 0-10 with 10 representing for "most
 * preferred" and 0 for "least preferred". Each priority function is weighted by
 * a positive number and the final score of each node is calculated by adding up
 * all the weighted scores. For example, suppose there are two priority
 * functions, priorityFunc1 and priorityFunc2 with weighting factors weight1 and
 * weight2 respectively, the final score of some NodeA is:
 * 
 * finalScoreNodeA = (weight1 * priorityFunc1) + (weight2 * priorityFunc2)
 * 
 * The definition above was gotten from Kubernetes repository:
 * https://github.com/kubernetes/community/blob/master/contributors/devel/scheduler_algorithm.md#ranking-the-nodes
 * 
 * This class consider two priority functions that was definied in Kubernetes
 * repository: LeastRequestedPriority and BalancedResourceAllocation.
 * 
 * @author giovanni
 *
 */
public class KubernetesRankingScore implements RankingScore {

	LeastRequestedRankingScore leastRequestedScore = new LeastRequestedRankingScore();
	BalancedResourceAllocationRankingScore balancedResourceScore = new BalancedResourceAllocationRankingScore();

	@Override
	public double calculateScore(Task task, Host host) {
		return leastRequestedScore.calculateScore(task, host) + balancedResourceScore.calculateScore(task, host);
	}
}

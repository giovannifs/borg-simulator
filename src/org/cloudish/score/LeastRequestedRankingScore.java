package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public class LeastRequestedRankingScore implements RankingScore {

	//cpu((capacity - sum(requested)) * 10 / capacity) + memory((capacity - sum(requested)) * 10 / capacity) / 2
	
	@Override
	public double calculateScore(Task task, Host host) {
		// TODO Auto-generated method stub
		return 0;
	}
}

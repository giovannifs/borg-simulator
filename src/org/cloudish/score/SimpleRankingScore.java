package org.cloudish.score;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;

public class SimpleRankingScore implements RankingScore {

	@Override
	public double calculateScore(Task task, Host host) {
		return host.getFreeCPU() + host.getFreeMem();
	}

}

package org.cloudish.score;

import org.cloudish.borg.model.Task;
import org.cloudish.dh.model.Server;

public interface RankingScore {
	
	public double calculateScore(Task task, Server server);

}

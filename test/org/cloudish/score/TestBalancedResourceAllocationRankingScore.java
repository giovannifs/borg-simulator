package org.cloudish.score;

import java.util.ArrayList;
import java.util.HashMap;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;
import org.junit.Assert;
import org.junit.Test;

public class TestBalancedResourceAllocationRankingScore {

	private static final double ACCEPTABLE_DIFF = 0.0000001;

	@Test(expected=IllegalArgumentException.class)
	public void testCalculateScoreInvalidHost() {
		BalancedResourceAllocationRankingScore score = new BalancedResourceAllocationRankingScore();

		score.calculateScore(null, null);
	}
	
	@Test
	public void testCalculateScore() {
		BalancedResourceAllocationRankingScore score = new BalancedResourceAllocationRankingScore();

		Host h = new Host(0, 0.5, 0.5, new HashMap<>());
		Assert.assertEquals(10, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore2() {
		BalancedResourceAllocationRankingScore score = new BalancedResourceAllocationRankingScore();

		Host h = new Host(0, 0.5, 0.5, new HashMap<>());
		h.allocate(new Task(0, 10, 0.5, 0, 11, true, new ArrayList<>()));
		
		Assert.assertEquals(0, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore3() {
		BalancedResourceAllocationRankingScore score = new BalancedResourceAllocationRankingScore();

		Host h = new Host(0, 0.5, 0.5, new HashMap<>());
		h.allocate(new Task(0, 10, 0.3, 0.1, 11, true, new ArrayList<>()));
		
		Assert.assertEquals(6, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}


}

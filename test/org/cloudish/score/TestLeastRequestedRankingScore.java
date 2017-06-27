package org.cloudish.score;

import java.util.ArrayList;
import java.util.HashMap;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;
import org.junit.Assert;
import org.junit.Test;

public class TestLeastRequestedRankingScore {
	
	private static final double ACCEPTABLE_DIFF = 0.0000001;

	@Test(expected=IllegalArgumentException.class)
	public void testCalculateScoreInvalidHost() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		score.calculateScore(null, null);
	}

	@Test
	public void testCalculateScore() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		Assert.assertEquals(10, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore2() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		// cpuCapacity is zero
		Host h = new Host(0, 0, 0.5, null, new HashMap<>());
		Assert.assertEquals(0, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore3() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		// memCapacity is zero
		Host h = new Host(0, 0.5, 0, null, new HashMap<>());
		Assert.assertEquals(0, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore4() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		// free cpu and mem are zero
		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.5, 0.5, 11, true, new ArrayList<>()));
		
		Assert.assertEquals(0, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore5() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		// free mem is zero and free cpu is 0.4
		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.1, 0.5, 11, true, new ArrayList<>()));
		
		Assert.assertEquals(4, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore6() {
		LeastRequestedRankingScore score = new LeastRequestedRankingScore();

		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.1, 0.1, 11, true, new ArrayList<>()));
		
		Assert.assertEquals(8, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
}

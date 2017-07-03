package org.cloudish.score;

import java.util.ArrayList;
import java.util.HashMap;

import org.cloudish.borg.model.Host;
import org.cloudish.borg.model.Task;
import org.junit.Assert;
import org.junit.Test;

public class TestKubernetesRankingScore {

	private static final double ACCEPTABLE_DIFF = 0.0000001;
	
	@Test(expected=IllegalArgumentException.class)
	public void testCalculateScoreInvalidHost() {
		KubernetesRankingScore score = new KubernetesRankingScore();

		score.calculateScore(null, null);
	}
	
	@Test
	public void testCalculateScore() {
		KubernetesRankingScore score = new KubernetesRankingScore();

		
		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		Assert.assertEquals(20, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore2() {
		KubernetesRankingScore score = new KubernetesRankingScore();
		
		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.3, 0.1, 11, true, new ArrayList<>()));
		// balancedResource score is 6
		// leastRequestes score is 6
	
		Assert.assertEquals(12, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore3() {
		KubernetesRankingScore score = new KubernetesRankingScore();

		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.3, 0.3, 11, true, new ArrayList<>()));
		// balancedResource score is 10
		// leastRequestes score is 4
	
		Assert.assertEquals(14, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore4() {
		KubernetesRankingScore score = new KubernetesRankingScore();

		Host h = new Host(0, 0.5, 0.5, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.5, 0.5, 11, true, new ArrayList<>()));
		// balancedResource score is 0
		// leastRequestes score is 0
	
		Assert.assertEquals(0, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore5() {
		KubernetesRankingScore score = new KubernetesRankingScore();

		Host h = new Host(0, 0.1, 0.1, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.1, 0.1, 11, true, new ArrayList<>()));
		// balancedResource score is 0
		// leastRequestes score is 0
	
		Assert.assertEquals(0, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	@Test
	public void testCalculateScore6() {
		KubernetesRankingScore score = new KubernetesRankingScore();

		Host h = new Host(0, 0.1, 0.1, null, new HashMap<>());
		h.allocate(new Task(0, 10, 0.05, 0.05, 11, true, new ArrayList<>()));
		// balancedResource score is 10
		// leastRequestes score is 10
	
		Assert.assertEquals(15, score.calculateScore(null, h), ACCEPTABLE_DIFF);
	}
	
	
}

package org.cloudish.dh.model;

import java.util.ArrayList;
import java.util.List;

import org.cloudish.borg.model.Task;
import org.cloudish.borg.model.TaskConstraint;
import org.cloudish.dh.DHManager;
import org.cloudish.dh.Utils;
import org.cloudish.score.KubernetesRankingScore;

public class LogicalServer extends Server {

	private double maxCpuCapacity;
	private double maxMemCapacity;
	private double cpuResourceGrain;
	private double memResourceGrain;
	private ResourcePool cpuPool;
	private ResourcePool memPool;
	private int QlAttr;
	private String GKAttr;
	private List<String> notAllowedGKAttr;
	private int minAllowedQlAtt;
	private int maxAllowedQlAtt;
	private boolean isConstraintOn;
	private DHManager dhManager;
	

	public LogicalServer(ResourcePool cpuPool, ResourcePool memPool, double maxCPUCapacity,
			double maxMemCapacity, double cpuResourceGrain, double memResourceGrain, DHManager dhManager, boolean isConstraintOn) {
		if (memPool == null || cpuPool == null) {
			throw new RuntimeException("The resource pools must be null value.");
		}
		
		this.cpuPool = cpuPool;
		this.memPool = memPool;
		this.maxCpuCapacity = maxCPUCapacity;
		this.maxMemCapacity = maxMemCapacity;
		this.cpuResourceGrain = cpuResourceGrain;
		this.memResourceGrain = memResourceGrain;
		this.dhManager = dhManager;
		this.isConstraintOn = isConstraintOn;
		
		// creating minimal logical server 
		getCpuPool().allocate(getCpuResourceGrain());
		getMemPool().allocate(getMemResourceGrain());
		
		cpuCapacity = getCpuResourceGrain();
		freeCPU = getCpuResourceGrain();
		memCapacity = getMemResourceGrain();
		freeMem = getMemResourceGrain();
		
		this.rankingScore = new KubernetesRankingScore();
		
		// initializing special attributes
		QlAttr = 1;
		minAllowedQlAtt = 1;
		maxAllowedQlAtt = Integer.MAX_VALUE;
		
		GKAttr = "";
		notAllowedGKAttr = new ArrayList<>();
	}

	public double getScore(Task task) {
		if (!isFeasible(task)) {
			return -1;
		}
		
		// check if logical server needs to be scaled up
		if (needsCPUScaleUp(task) || needsMemScaleUp(task)) {
			double cpuToBeRequested = calcCpuToBeRequested(task);
			double memToBeRequested = calcMemToBeRequested(task);
			FakeScaledUpServer server = new FakeScaledUpServer(cpuCapacity + cpuToBeRequested,
					freeCPU + cpuToBeRequested, memCapacity + memToBeRequested, freeMem + memToBeRequested);
			return rankingScore.calculateScore(task, server);
		}
		
		return rankingScore.calculateScore(task, this);
	}

	protected boolean isFeasible(Task task) {
		if (isConstraintOn) {
			if (jidAllocated.contains(task.getJid())) {
//			System.out.println("Logical Server is not feasible because of antiaffinity constraint.");
				return false;
			}
			
			// check if the resource pools are feasible for task
			if (!cpuPool.isFeasible(task) || !memPool.isFeasible(task)) {
//			System.out.println("Logical Server is not feasible because of pool attributes.");
				return false;
			}
			
			// checking feasibility of GK attribute
			if (!checkGKAttrFeasibility(task)) {
//			System.out.println("Logical Server is not feasible because of GK constraint.");
				return false;
			}
			
			// checking feasibility of Ql attribute
			if (!checkQlAttrFeasibility(task)) {
//			System.out.println("Logical Server is not feasible because of Ql constraint.");
				return false;
			}
		}
		
		// check if logical server needs to be scaled up
		if (needsCPUScaleUp(task)) {
			double cpuToBeRequested = calcCpuToBeRequested(task);
			
			if (!cpuPool.hasMoreResource(cpuToBeRequested) || exceedCpuMaxCapacity(cpuToBeRequested)) {
//				System.out.println("Logical Server is not feasible because pool does not have more resource or exceed cpu capacity.");
				return false;
			}
		}
		
		if (needsMemScaleUp(task)) {
			double memToBeRequested = calcMemToBeRequested(task);
			
			if (!memPool.hasMoreResource(memToBeRequested) || exceedMemMaxCapacity(memToBeRequested)) {
				return false;
			} 
		}

		return true;
	}

	private boolean checkQlAttrFeasibility(Task task) {
		List<TaskConstraint> QlConstraints = task.getConstraints("Ql");
		if (!QlConstraints.isEmpty()) {
		
			for (TaskConstraint constraint : QlConstraints) {
				
				if (constraint.getOperator().equals(">")) {
					
					if ((Integer.parseInt(constraint.getAttValue()) + 1) > getMaxAllowedQlAtt()) {
						return false;						
					}
				} else { // constraint operator is <
					if ((Integer.parseInt(constraint.getAttValue()) - 1) < getMinAllowedQlAtt()) {
						return false;
					}					
				}
			}
		}
		
		return true;
	}

	private boolean checkGKAttrFeasibility(Task task) {
		List<TaskConstraint> gkConstraints = task.getConstraints("GK");
		if(!gkConstraints.isEmpty()) {
			
			for (TaskConstraint constraint : gkConstraints) {
						
				// constraint with == operator
				if (constraint.getOperator().equals("==")) {
		
					// check if gk is set
					if (isGKSet()) {
					
						// if gk is set and is diff of the value
						if (!getGKAttr().equals(constraint.getAttValue())) {
							return false;							
						}
					} else { //gk is not set
						
						// gk is not available or there is any taks allocated with != operator
						if (!dhManager.isGKValueAvailable(constraint.getAttValue())
								|| getNotAllowedGKAttr().contains(constraint.getAttValue())) {
							return false;
						}
					}
					
				} else if (constraint.getOperator().equals("!=")) {
					
					if (isGKSet() && getGKAttr().equals(constraint.getAttValue())) {
						return false;
					}
				}
			}
		}
		
		return true;
	}

	private boolean isGKSet() {
		return !"".equals(getGKAttr());
	}

	protected double calcCpuToBeRequested(Task task) {
		if (freeCPU < task.getCpuReq()) {
			double cpuToBeScaled = Utils.format(task.getCpuReq() - freeCPU);
			int numberOfGrains = (int) Math.ceil(cpuToBeScaled/getCpuResourceGrain());
			double cpuToBeRequested = Utils.format(numberOfGrains * getCpuResourceGrain());
			return cpuToBeRequested;
		}
		return 0;
	}

	protected double calcMemToBeRequested(Task task) {
		if (freeMem < task.getMemReq()) {
			double memToBeScaled = Utils.format(task.getMemReq() - freeMem);
			int numberOfGrains = (int) Math.ceil(memToBeScaled/getMemResourceGrain()); 
			
			double memToBeRequested = Utils.format(numberOfGrains * getMemResourceGrain());
			return memToBeRequested;
		}
		return 0;
	}

	private boolean exceedCpuMaxCapacity(double cpuToBeRequested) {
		return (cpuCapacity + cpuToBeRequested) > maxCpuCapacity;
	}

	private boolean exceedMemMaxCapacity(double memToBeRequested) {
		return (memCapacity + memToBeRequested) > maxMemCapacity;
	}

	public void allocate(Task task) {
		
		// scaling up if needed
		if (needsCPUScaleUp(task)) {			
			double cpuToBeRequested = calcCpuToBeRequested(task);
			getCpuPool().allocate(cpuToBeRequested);
			
			cpuCapacity = Utils.format(cpuCapacity + cpuToBeRequested);
			freeCPU = Utils.format(freeCPU + cpuToBeRequested);
		}
		
		if (needsMemScaleUp(task)) {
			double memToBeRequested = calcMemToBeRequested(task);
			
			getMemPool().allocate(memToBeRequested);
			
			memCapacity = Utils.format(memCapacity + memToBeRequested);
			freeMem = Utils.format(freeMem + memToBeRequested);
		}
		
		// allocating the task
		freeCPU = Utils.format(freeCPU - task.getCpuReq());
		freeMem = Utils.format(freeMem - task.getMemReq());
		
		if (isConstraintOn()) {
			if (task.isAntiAffinity()) {
				jidAllocated.add(task.getJid());		
			}
			
			// treat GK and Ql attributes
			treatGKAttr(task);
			treatQlAttr(task);
		}

		// asserting assumptions 
		if (cpuCapacity > maxCpuCapacity || memCapacity > maxMemCapacity) {
			throw new RuntimeException(
					"The capacity of memory or cpu never must be a greater than max values for them.");
		}

		if (freeCPU < 0 || freeMem < 0) {
			throw new RuntimeException("The free memory or cpu never must be a negative value.");
		}
	}

	private void treatQlAttr(Task task) {
		List<TaskConstraint> QlConstraints = task.getConstraints("Ql");
		
		if (!QlConstraints.isEmpty()) {
		
			for (TaskConstraint constraint : QlConstraints) {
				if (constraint.getOperator().equals(">")) {
					QlAttr = Integer.parseInt(constraint.getAttValue()) + 1;
					minAllowedQlAtt = QlAttr;
				} else { // constraint operator is <
					QlAttr = Integer.parseInt(constraint.getAttValue()) - 1;
					maxAllowedQlAtt = QlAttr;
				}
			}
		}
	}

	private void treatGKAttr(Task task) {
		List<TaskConstraint> gkConstraints = task.getConstraints("GK");
		if(!gkConstraints.isEmpty()) {
			
			for (TaskConstraint constraint : gkConstraints) {
						
				// constraint with == operator
				if (constraint.getOperator().equals("==")) {
					if (!isGKSet()) {
						dhManager.allocateGKValue(constraint.getAttValue(), this);
						this.GKAttr = constraint.getAttValue();
					}
					
				} else { // constraint with operator !=
					getNotAllowedGKAttr().add(constraint.getAttValue());
				}
			}
		}
	}

	public boolean needsCPUScaleUp(Task task) {
		return freeCPU < task.getCpuReq();
	}

	public boolean needsMemScaleUp(Task task) {
		return freeMem < task.getMemReq();
	}

	public double getMaxCpuCapacity() {
		return maxCpuCapacity;
	}

	public double getMaxMemCapacity() {
		return maxMemCapacity;
	}

	public double getCpuResourceGrain() {
		return cpuResourceGrain;
	}
	
	public double getMemResourceGrain() {
		return memResourceGrain;
	}

	public ResourcePool getCpuPool() {
		return cpuPool;
	}

	public ResourcePool getMemPool() {
		return memPool;
	}

	public int getQlAttr() {
		return QlAttr;
	}

	public String getGKAttr() {
		return GKAttr;
	}

	public List<String> getNotAllowedGKAttr() {
		return notAllowedGKAttr;
	}

	public int getMinAllowedQlAtt() {
		return minAllowedQlAtt;
	}

	public int getMaxAllowedQlAtt() {
		return maxAllowedQlAtt;
	}
	
	protected DHManager getDhManager() {
		return dhManager;
	}

	protected void setCpuResourceGrain(double resourceGrain) {
		this.cpuResourceGrain = resourceGrain;
	}
	
	protected void setMemResourceGrain(double resourceGrain) {
		this.memResourceGrain = resourceGrain;
	}

	protected void setMaxCpuCapacity(double maxCpuCapacity) {
		this.maxCpuCapacity = maxCpuCapacity;
	}

	protected void setMaxMemCapacity(double maxMemCapacity) {
		this.maxMemCapacity = maxMemCapacity;
	}

	public boolean isConstraintOn() {
		return isConstraintOn;
	}
	
	protected void setConstraintOn(boolean isConstraintOn) {
		this.isConstraintOn = isConstraintOn;
	}	
}

class FakeScaledUpServer extends Server {

	public FakeScaledUpServer(double cpuCapacity, double freeCpu, double memCapacity, double freeMem) {
		this.cpuCapacity = cpuCapacity;
		this.freeCPU = freeCpu;
		this.memCapacity = memCapacity;
		this.freeMem = freeMem;
	}
	
	@Override
	public double getScore(Task task) {
		return -1;
	}

	@Override
	public void allocate(Task task) {
		throw new RuntimeException("Not implemented.");		
	}
	
}

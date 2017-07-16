package org.cloudish.borg.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Task {

	private double time;
	private long jid;
	private int tid;
	private double cpuReq;
	private double memReq;
	private int priority;
	private boolean antiAffinity;
	private boolean couldCreateNewServer;
	private List<TaskConstraint> constraints = new ArrayList<TaskConstraint>();
	
//	#%% format {timestamp,task_id,job_id,cpu_req,mem_req,priority,diffMachine,constraints}
//	0,0,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]
	public Task(String taskLine) {		
		StringTokenizer st = new StringTokenizer(taskLine, "[");

		String properties = st.nextToken();
		
		//creating constraints
		String constraintStr = st.nextToken();		
		constraintStr = constraintStr.replace("]", "").trim();
		
		st = new StringTokenizer(properties, ",");
		time = Double.parseDouble(st.nextToken()); //time
		tid = Integer.parseInt(st.nextToken().trim());
		jid = Long.parseLong(st.nextToken().trim());
		cpuReq = Double.parseDouble(st.nextToken().trim());
		memReq = Double.parseDouble(st.nextToken().trim());
		priority = Integer.parseInt(st.nextToken().trim());
		antiAffinity = Integer.parseInt(st.nextToken().trim()) == 1;
		couldCreateNewServer = false;
		
		if (tid < 0 || jid < 0 || cpuReq < 0 || memReq < 0 || priority < 0) {
			throw new IllegalArgumentException("Task attributes must no be negative values.");
		}
		
		if (constraintStr.length() > 0) {
			StringTokenizer stConst = new StringTokenizer(constraintStr, ";");		
			while (stConst.hasMoreTokens()) {
				st = new StringTokenizer(stConst.nextToken(), ",");
				String attName = st.nextToken();
				String op = st.nextToken();
				String attValue = st.nextToken();
				constraints.add(new TaskConstraint(attName, op, attValue));
			}
		}
	}
	
	public Task(int tid, long jid, double cpuReq, double memReq, int priority, boolean antiAffinity,
			List<TaskConstraint> constraints) {
		
		if (tid < 0 || jid < 0 || cpuReq < 0 || memReq < 0 || priority < 0) {
			throw new IllegalArgumentException("Task attributes must no be negative values.");
		} else if (constraints == null) {
			throw new IllegalArgumentException("Task constraint attribute must no be null.");
		}
		
		this.tid = tid;
		this.jid = jid;
		this.cpuReq = cpuReq;
		this.memReq = memReq;
		this.priority = priority;
		this.antiAffinity = antiAffinity;
		this.constraints = constraints;
		this.couldCreateNewServer = false;
	}

	public long getJid() {
		return jid;
	}

	public int getTid() {
		return tid;
	}

	public double getCpuReq() {
		return cpuReq;
	}

	public double getMemReq() {
		return memReq;
	}

	public boolean isAntiAffinity() {
		return antiAffinity;
	}

	public List<TaskConstraint> getConstraints() {
		return constraints;
	}

	public int getPriority() {
		return priority;
	}
	
	public boolean isCouldCreateNewServer() {
		return couldCreateNewServer;
	}

	public void setCouldCreateNewServer(boolean couldCreateNewServer) {
		this.couldCreateNewServer = couldCreateNewServer;
	}

	public List<TaskConstraint> getConstraints(String attName) {
		List<TaskConstraint> attConstraints = new ArrayList<>();
		for (TaskConstraint taskConstraint : getConstraints()) {
			if (attName.equals(taskConstraint.getAttName())) {
				attConstraints.add(taskConstraint);
			}
		}
		return attConstraints;
	}
	
	public void setJid(long jid) {
		this.jid = jid;
	}

	public void setTid(int tid) {
		this.tid = tid;
	}

	@Override
	public String toString() {
		return "jid=" + getJid() + ", tid=" + getTid() + ", cpuReq=" + getCpuReq() + ", memReq=" + getMemReq()
				+ ", priority=" + getPriority() + ", antiaffinity=" + isAntiAffinity() + ", constraints="
				+ getConstraints();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Task) {
			Task other = (Task) obj;
			return other.getJid() == getJid() && other.getTid() == getTid() && other.getCpuReq() == getCpuReq()
					&& other.getMemReq() == getMemReq() && other.getPriority() == getPriority()
					&& (other.isAntiAffinity() == isAntiAffinity())
					&& (other.getConstraints().containsAll(getConstraints())
							&& getConstraints().containsAll(other.getConstraints()));
		}
		return false;
	}

	public String lineFormat() {
		// 0,0,4028922835,0.09375,0.03198,11,1,[Ql,<,14;wN,==,2;Ql,>,4]

		String constraintFormat = "";
		for (TaskConstraint taskConstraint : getConstraints()) {
			constraintFormat += taskConstraint.lineFormat() + ";";
		}

		// excluding last ;
		if (!constraintFormat.isEmpty()) {
			constraintFormat = constraintFormat.substring(0, constraintFormat.length() - 1);
		}

		return time + "," + getTid() + "," + getJid() + "," + getCpuReq() + "," + getMemReq() + "," + getPriority()
				+ "," + (isAntiAffinity() ? "1" : "0") + "," + "[" + constraintFormat + "]";
	}
}

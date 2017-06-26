package org.cloudish.borg.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Task {

	private long jid;
	private int tid;
	private double cpuReq;
	private double memReq;
	private int priority;
	private boolean antiAffinity;
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
		st.nextToken(); //time
		tid = Integer.parseInt(st.nextToken().trim());
		jid = Long.parseLong(st.nextToken().trim());
		cpuReq = Double.parseDouble(st.nextToken().trim());
		memReq = Double.parseDouble(st.nextToken().trim());
		priority = Integer.parseInt(st.nextToken().trim());
		antiAffinity = Integer.parseInt(st.nextToken().trim()) == 1;
		
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
}

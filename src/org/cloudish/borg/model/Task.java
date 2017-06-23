package org.cloudish.borg.model;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Task {

	long jid;
	int tid;
	double cpuReq;
	double memReq;
	int priority;
	boolean antiAffinity;
	List<TaskConstraint> constraints = new ArrayList<TaskConstraint>();
	
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
		
		System.out.println("Constraint: " + constraintStr);
		
		if (constraintStr.length() > 0) {
			StringTokenizer stConst = new StringTokenizer(constraintStr, ";");		
			while (stConst.hasMoreTokens()) {
				st = new StringTokenizer(stConst.nextToken(), ",");
				String attName = st.nextToken();
				System.out.println(attName);
				String op = st.nextToken();
				String attValue = st.nextToken();
				constraints.add(new TaskConstraint(attName, op, attValue));
			}
		}
	}

	public double getJid() {
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
}

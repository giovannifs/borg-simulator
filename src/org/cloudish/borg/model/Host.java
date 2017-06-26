package org.cloudish.borg.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class Host {

	long id;
	double cpuCapacity;
	double memCapacity;
	double freeCPU;
	double freeMem;
	private Map<String, HostAttribute> attributes = new HashMap<>();
	private List<Long> jidAllocated = new ArrayList<>();

	//	#%% Format: {host_id,host_name,cpu_capacity,mem_capacity,rs,0/,Ql,maq}
	//	#{0,"Host_1",8,16,[{"rs","1"},{"o/","1"},{"Ql","1"},{"ma","1"}]}.
	//	5,Host_5,0.5,0.2493,[5d,2;9e,2;By,4;GK,Ap;Ju,1;Ql,3;UX,2;ma,2;nU,2;nZ,2;rs,Fh;w3,4;wN,2;o/,0;P8,0]
	public Host(String line) {
		StringTokenizer st = new StringTokenizer(line, "[");

		String properties = st.nextToken();
		
		//creating constraints
		String attributesStr = st.nextToken();		
		
		st = new StringTokenizer(properties, ",");
		id = Long.parseLong(st.nextToken());
		st.nextToken(); //hostName
		cpuCapacity = Double.parseDouble(st.nextToken());
		memCapacity = Double.parseDouble(st.nextToken());
		
		freeCPU = cpuCapacity;
		freeMem = memCapacity;

		if (attributesStr.length() > 0) {
			StringTokenizer stConst = new StringTokenizer(attributesStr, ";");		
			while (stConst.hasMoreTokens()) {
				st = new StringTokenizer(stConst.nextToken(), ",");
				String attName = st.nextToken();
				String attValue = st.nextToken();
				attributes.put(attName, new HostAttribute(attName, attValue));
			}
		}
	}
	
	private Host(long id, double cpuCapacity, double freeCPU, double memCapacity, double freeMem,
			Map<String, HostAttribute> attributes) {
		this.id = id;
		this.cpuCapacity = cpuCapacity;
		this.freeCPU = freeCPU;
		this.memCapacity = memCapacity;
		this.freeMem = freeMem;
		this.attributes = attributes;
	}

	public double getScore(Task task) {
		if (!match(task)) {
			return -1;
		}

		// checking capacities and calculating score
		if (freeCPU >= task.getCpuReq() && freeMem >= task.getMemReq()) {
			return freeCPU + freeMem;
		} else {
			return -1;
		}
    }

	protected boolean match(Task task) {
		if (jidAllocated.contains(task.getJid())) {
			return false;
		}
		
		for (TaskConstraint constraint : task.getConstraints()) {
			HostAttribute hostAtt = attributes.get(constraint.getAttName());
			
			if (hostAtt == null || !hostAtt.match(constraint)) {
				return false;
			}			
		}

		return true;
	}

	public void allocate(Task task) {
		freeCPU = freeCPU - task.getCpuReq();
		freeMem = freeMem - task.getMemReq();

		if (task.isAntiAffinity()) {
			jidAllocated.add(task.getJid());		
		}
	}

	public long getId() {
		return id;
	}

	public double getCpuCapacity() {
		return cpuCapacity;
	}

	public double getMemCapacity() {
		return memCapacity;
	}

	public double getFreeCPU() {
		return freeCPU;
	}

	public double getFreeMem() {
		return freeMem;
	}

	public Map<String, HostAttribute> getAttributes() {
		return attributes;
	}
	
	public Host clone() {
		return new Host(getId(), getCpuCapacity(),getFreeCPU(), getMemCapacity(), getFreeMem(), getAttributes());
	}
}

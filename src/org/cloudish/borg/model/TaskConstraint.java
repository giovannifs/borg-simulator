package org.cloudish.borg.model;

public class TaskConstraint {
	
	String attName;
	String operator;
	String attValue;
	
	public TaskConstraint(String attName, String op, String attValue) {
		this.attName = attName;
		this.operator = op;
		this.attValue = attValue;
	}

	public String getAttName() {
		return attName;
	}
	
	public String getOperator() {
		return operator;
	}
	
	public String getAttValue() {
		return attValue;
	}
}

package org.cloudish;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.cloudish.borg.model.Task;

public class PreProcessInput {

	public static void main(String[] args) throws IOException {
		List<String> tasksFilePaths = readFilePaths(args[0]);
		int maxJobTask = Integer.parseInt(args[1]);
	
		for (String taskFilePath : tasksFilePaths) {
			System.out.println("Preprocessing tasks at file " + taskFilePath);
			
			long lastProcessingJob = 1;
			long currentProcessingJob;
			int tasksOfCurrentJob = 0;
			
			BufferedReader br = new BufferedReader(new FileReader(taskFilePath));
						
			try {
				String line = br.readLine();
				
				while (line != null) {
					Task task = new Task(line);
					
					currentProcessingJob = task.getJid();
					
					if (currentProcessingJob == lastProcessingJob) {
						tasksOfCurrentJob++;
						
						if (tasksOfCurrentJob == maxJobTask) {
							//create a new job
							
							long jid = 0;
							currentProcessingJob = jid;
							tasksOfCurrentJob = 1;
							task.setJid(jid);
							task.setTid(tasksOfCurrentJob - 1);
						}
					
					} else {
						tasksOfCurrentJob = 1;
					}
					
					//keep the tasks in the same order
					
					lastProcessingJob = currentProcessingJob;
					line = br.readLine();
				}
			} finally {
				br.close();
			}
		}
	}

	private static List<String> readFilePaths(String string) {
		// TODO Auto-generated method stub
		return null;
	}

}

package org.cloudish;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.cloudish.borg.model.Task;

public class PreProcessInput {

	public static void main(String[] args) throws IOException {
		List<String> tasksFilePaths = readFilePaths(args[0]);
		int maxJobTask = Integer.parseInt(args[1]);
	
		for (String taskFilePath : tasksFilePaths) {
			System.out.println("Preprocessing tasks at file " + taskFilePath);
			
			List<Long> existingJobs = new ArrayList<>();
			long lastProcessingJob = -1;
			long currentProcessingJob;
			int tasksOfCurrentJob = 0;
			
			long createdAdditionalJob = -1;
			boolean jobSpllited = false;
			
			File file = new File(taskFilePath);
			FileReader in = new FileReader(file);
			BufferedReader br = new BufferedReader(in);
			PrintWriter writer = new PrintWriter("timestamps/pre-processed-" + file.getName(), "UTF-8");
						
			try {
				String line = br.readLine();
				
				long taskIndex = 0;
				
				while (line != null) {
					Task task = new Task(line);
					
					currentProcessingJob = task.getJid();
					
					if (task.getPriority() == 1) {
						if (currentProcessingJob == lastProcessingJob) {
							tasksOfCurrentJob++;
							
							if (tasksOfCurrentJob > maxJobTask) {
								//create a new job id and update task info				
								
								Random r = new Random();							
								long jid = r.nextLong();
								
								while (existingJobs.contains(jid)) {
									jid = r.nextLong();	
								}
								
								createdAdditionalJob = jid;
								tasksOfCurrentJob = 1;
								task.setJid(createdAdditionalJob);
								task.setTid(tasksOfCurrentJob - 1);
								jobSpllited = true;
								existingJobs.add(task.getJid());
							} else if (jobSpllited) {
								task.setJid(createdAdditionalJob);
								task.setTid(tasksOfCurrentJob - 1);
							}
							
						} else {
							tasksOfCurrentJob = 1;
							jobSpllited = false;
							existingJobs.add(task.getJid());
						}
					}
					
					//keep the tasks in the same order
			
					writer.println(task.lineFormat());
										
					lastProcessingJob = currentProcessingJob;
					System.out.println("Task index " + ++taskIndex);
					line = br.readLine();
				}
			} finally {
				writer.close();
				br.close();
			}
		}
	}

	private static List<String> readFilePaths(String inputPath) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(inputPath));
		
		List<String> filePaths = new ArrayList<>();
		String line = br.readLine();
		
		while (line != null) {			
			filePaths.add(line);
			line = br.readLine();
		}
		
		return filePaths;
	}

}

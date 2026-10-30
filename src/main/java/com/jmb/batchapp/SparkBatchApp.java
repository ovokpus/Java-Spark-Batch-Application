package com.jmb.batchapp;

import com.jmb.batchapp.configuration.SparkSessionProvider;
import com.jmb.batchapp.exception.ExceptionHandler;
import com.jmb.batchapp.job.Job;
import com.jmb.batchapp.parameter.CommonJobParameter;
import com.jmb.batchapp.parameter.JobsParameters;
import com.jmb.batchapp.parameter.Parameters;
import com.jmb.batchapp.run.JobMetrics;
import com.jmb.batchapp.run.RunRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

@SpringBootApplication
public class SparkBatchApp implements CommandLineRunner {

	private static Logger LOGGER = LoggerFactory
			.getLogger(SparkBatchApp.class);

	@Autowired
	private ExceptionHandler exceptionHandler;

	@Autowired
	private Parameters parameters;

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RunRecorder runRecorder;

	@Autowired
	private JobMetrics jobMetrics;

	public static void main(String[] args) {
		LOGGER.info("Starting Batch Application");
		int exitCode = SpringApplication.exit(SpringApplication.run(SparkBatchApp.class, args));
		LOGGER.info("Batch Application Finished");
		// Spark leaves non-daemon threads behind; exit explicitly so a container run terminates.
		System.exit(exitCode);
	}

	@Override
	public void run(String... args) {
		LOGGER.info("Parsing common job arguments");
		loadCommonParams(args);
		String jobName = parameters.getParamValue(CommonJobParameter.JOB_NAME);
		if (jobName == null) {
			// Lets the Spring context start (e.g. in tests) without running a job.
			LOGGER.warn("No jobName=... argument given; nothing to run. Usage: jobName=<ingesterJob|salesSummaryJob> [key=value ...]");
			return;
		}
		String runId = runRecorder.start(jobName, args);
		jobMetrics.reset();
		try {
			LOGGER.info("Instantiating job");
			Job job = (Job) context.getBean(jobName, Job.class);
			LOGGER.info("Loading Job Specific Arguments");
			loadJobParams(args);
			LOGGER.info("Executing job");
			job.execute();
			runRecorder.succeed(runId, jobMetrics.getRowsWritten());
		} catch (RuntimeException be) {
			runRecorder.fail(runId, be);
			exceptionHandler.handleException(be);
			throw be;
		} finally {
			SparkSessionProvider.stop();
		}
	}

	private void loadCommonParams(String[] args) {
		parameters.loadAllParams(args, CommonJobParameter.values());
	}

	private void loadJobParams(String[] args) {
		String jobName = parameters.getParamValue(CommonJobParameter.JOB_NAME);
		Optional<JobsParameters> paramsFound = JobsParameters.getParamsForJob(jobName);
		if(paramsFound.isPresent()) {
			parameters.loadAllParams(args, paramsFound.get().getParameters());
		}
	}
}

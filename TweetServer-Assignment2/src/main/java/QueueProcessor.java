import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;

@WebServlet("/QueueProcessor")
public class QueueProcessor extends HttpServlet {

	static String myQueueUrl = "https://sqs.us-east-1.amazonaws.com/333363708195/TweetQueue";
	static String aws_access_key_id = "AKIAJTB55RI4FSP2TX7Q";
	static String aws_secret_access_key = "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";

	@Override
	public void init() throws ServletException {
		Thread thread = new Thread(new Runnable() {

			@Override
			public void run() {
				BasicAWSCredentials credentials = null;
				try {
					//credentials = new ProfileCredentialsProvider().getCredentials();
					credentials = new BasicAWSCredentials(aws_access_key_id, aws_secret_access_key);
				} catch (Exception e) {
					throw new AmazonClientException("Can't load the credentials from the credential profiles file. "
							+ "Please make sure that your credentials file is at the correct "
							+ "location (~/.aws/credentials), and is a in valid format.", e);
				}

				final AmazonSQS sqs = new AmazonSQSClient(credentials);
				Region usEast = Region.getRegion(Regions.US_EAST_1);
				sqs.setRegion(usEast);

				try {
					ExecutorService executor = Executors.newFixedThreadPool(3);
					ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest(myQueueUrl);

					while (true) {
						ReceiveMessageResult message = sqs.receiveMessage(receiveMessageRequest.withMessageAttributeNames("All"));
						if (message.getMessages().size() > 0) {
							Runnable worker = new WorkerThread(message);
							executor.execute(worker);
							sqs.deleteMessage(myQueueUrl, message.getMessages().get(0).getReceiptHandle());
						}
					}
				} catch (Exception e) {
					System.out.println("Count exception in QueueProcessor");
					e.printStackTrace();
				}
			}

		});
		thread.start();
	}

}

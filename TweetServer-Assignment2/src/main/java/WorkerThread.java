
import java.util.HashMap;
import java.util.Map;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage;
import com.ibm.watson.developer_cloud.alchemy.v1.model.DocumentSentiment;
import org.json.simple.JSONObject;

public class WorkerThread implements Runnable {

	ReceiveMessageResult message;
	static String arn = "arn:aws:sns:us-east-1:333363708195:TweetTrend";
	static String aws_access_key_id = "AKIAJTB55RI4FSP2TX7Q";
	static String aws_secret_access_key = "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";
	//create a new SNS client and set endpoint
	BasicAWSCredentials credentials = new BasicAWSCredentials(aws_access_key_id, aws_secret_access_key);
	AmazonSNSClient snsClient = new AmazonSNSClient(credentials);
	Region usEast = Region.getRegion(Regions.US_EAST_1);

	public WorkerThread(ReceiveMessageResult message) {
		this.message = message;
	}

	@Override
	public void run() {
		snsClient.setRegion(usEast);
		
		Message msg = message.getMessages().get(0);
		String content = msg.getBody();
		
		AlchemyLanguage service = new AlchemyLanguage();
		service.setApiKey("b4e9c20260c444b37c1a34a8265b04f4e17dce4d");
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(AlchemyLanguage.TEXT, content);
		DocumentSentiment sentiment = service.getSentiment(params).execute();
		String tweetSentiment = sentiment.getSentiment().getType().toString();
		
		// lat, long, cont, key, sent
		Map<String, MessageAttributeValue> msgAttr = msg.getMessageAttributes();
		String latLong = msgAttr.get("Location").getStringValue();
		String[] parts = latLong.split(",");
		
		String latitude = parts[0]; 
		String longitude = parts[1];
		String keyword = msgAttr.get("Word").getStringValue();
		
		String notification = latitude + ":::" + longitude + ":::" + tweetSentiment + ":::" + keyword + ":::" + content;
		
		PublishRequest publishRequest = new PublishRequest(arn, notification);
		PublishResult publishResult = snsClient.publish(publishRequest);
	}

}

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import twitter4j.FilterQuery;
import twitter4j.GeoLocation;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

public class CollectTweets {

	static String custKey = "*";
	static String custSecret = "*";
	static String accTok = "*";
	static String accSecret = "*";


	static List<String> words = new ArrayList<String>();
	static JestClientFactory factory;
	static JestClient client;
	static String myQueueUrl = "*";

	public static void main(String[] args) {

		AWSCredentials credentials = null;
		try {
			credentials = new ProfileCredentialsProvider().getCredentials();
		} catch (Exception e) {
			throw new AmazonClientException("Can't load the credentials from the credential profiles file. "
					+ "Please make sure that your credentials file is at the correct "
					+ "location (~/.aws/credentials), and is a in valid format.", e);
		}

		final AmazonSQS sqs = new AmazonSQSClient(credentials);
		Region usEast = Region.getRegion(Regions.US_EAST_1);
		sqs.setRegion(usEast);

		words.add("trump");
		words.add("hillary");
		words.add("india");
		words.add("obama");
		words.add("google");
		words.add("ronaldo");
		words.add("computer");
		words.add("new york");
		//words.add("a");

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthConsumerKey(custKey);
		cb.setOAuthConsumerSecret(custSecret);
		cb.setOAuthAccessToken(accTok);
		cb.setOAuthAccessTokenSecret(accSecret);

		StatusListener listener = new StatusListener() {

			public void onException(Exception arg0) {			}
			public void onDeletionNotice(StatusDeletionNotice arg0) {			}
			public void onScrubGeo(long arg0, long arg1) {			}
			public void onTrackLimitationNotice(int arg0) {			}
			public void onStallWarning(StallWarning arg0) {			}
			public void onStatus(Status status) {
				if (status.getGeoLocation() != null) {
					for (String word : words) {
						String content = status.getText();
						if (content.contains(word)) {
							// String username =
							// status.getUser().getScreenName();
							// long tweetId = status.getId();
							GeoLocation location = status.getGeoLocation();
							double latitude = location.getLatitude();
							double longitude = location.getLongitude();
							String latLong = latitude + "," + longitude;

							System.out.println("found = " + word + " : loc " + latLong);
							System.out.println("Cont = " + content);


							SendMessageRequest sendMessageRequest = new SendMessageRequest();
							sendMessageRequest.withQueueUrl(myQueueUrl);
							sendMessageRequest.withMessageBody(content);
							sendMessageRequest.addMessageAttributesEntry("Word",
									new MessageAttributeValue().withDataType("String").withStringValue(word));
							sendMessageRequest.addMessageAttributesEntry("Location",
									new MessageAttributeValue().withDataType("String").withStringValue(latLong));
							sqs.sendMessage(sendMessageRequest);
						}
					}
				}
			}
		};

		factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig.Builder(
				"*").multiThreaded(true)
						.readTimeout(60000).build());
		client = factory.getObject();

		/*
		 * client.execute(new CreateIndex.Builder("tweetdata").build());
		 * System.out.println("Index twitterdata created");
		 */

		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		FilterQuery fq = new FilterQuery();
		double[][] bb = { { -180, -90 }, { 180, 90 } };

		fq.locations(bb);
		fq.language(new String[] { "en" });

		twitterStream.addListener(listener);
		twitterStream.filter(fq);
	}
}

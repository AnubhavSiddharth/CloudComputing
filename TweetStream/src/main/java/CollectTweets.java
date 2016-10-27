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

public class CollectTweets {
	
	
	static String custKey = "*";
	static String custSecret = "*";
	static String accTok = "*";
	static String accSecret = "*";
	

	static List<String> words = new ArrayList<String>();
	static JestClientFactory factory;
	static JestClient client;
	
	public static void main(String[] args) {					
	        words.add("trump"); 
	        words.add("hillary"); 
	        words.add("india"); 
	        words.add("obama"); 
	        words.add("google"); 
	        words.add("ronaldo"); 
	        words.add("computer"); 
	        words.add("new york"); 
	        	
	        ConfigurationBuilder cb = new ConfigurationBuilder();
	        cb.setDebugEnabled(true);
	        cb.setOAuthConsumerKey(custKey);
	        cb.setOAuthConsumerSecret(custSecret);
	        cb.setOAuthAccessToken(accTok);
	        cb.setOAuthAccessTokenSecret(accSecret);
	        
	        StatusListener listener = new StatusListener() {
	        	
	            public void onException(Exception arg0) {}
	            public void onDeletionNotice(StatusDeletionNotice arg0) {}
	            public void onScrubGeo(long arg0, long arg1) {}
	            public void onTrackLimitationNotice(int arg0) {}
				public void onStallWarning(StallWarning arg0) {}
	
	            public void onStatus(Status status) {
	            	
	            	try {
						if(status.getGeoLocation() != null) {
						//System.out.println("location = " + status.getGeoLocation().getLatitude() + ", " + status.getGeoLocation().getLongitude());
						
         
						for (String word: words)
						{
							String content = status.getText();
							if(content.contains(word))
							{                
								
						        String username = status.getUser().getScreenName();
						        long tweetId = status.getId();
						        GeoLocation location = status.getGeoLocation();
						        double latitude = location.getLatitude();
						        double longitude = location.getLongitude();
						        
						        System.out.println("found = " + word + " : loc " + location.toString());
						        System.out.println("ID = " + tweetId + " : Cont = " + content);
						        
						        String source = jsonBuilder()
									 .startObject()
									 .field("username", username)
									 .field("tweetid", tweetId)
									 .field("content", content)
									 .field("keyword", word)
									 .field("latitude", latitude)
									 .field("longitude", longitude)
									 .endObject().string();
							 
							 Index index = new Index.Builder(source).index("tweetdata").type("tweet").build();
							 client.execute(index);	  
						        
							}
						} 
						}
					} catch (IOException e) {
						e.printStackTrace();
					}   
	            }
	        };    
	        
	        factory = new JestClientFactory();
			factory.setHttpClientConfig(new HttpClientConfig
			                        .Builder("elasticSearch-URL")
			                        .multiThreaded(true)
			                        .readTimeout(60000)
			                        .build());
			 client = factory.getObject();

			 /*
			 client.execute(new CreateIndex.Builder("tweetdata").build());
			 System.out.println("Index twitterdata created");
			 */
	
	        TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
	        FilterQuery fq = new FilterQuery();    
	        double[][] bb= { {-180, -90}, {180, 90} };
	        
	        fq.locations(bb);
	        fq.language(new String[]{"en"});
	
	        twitterStream.addListener(listener);
	        twitterStream.filter(fq);        
	}
}

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.google.gson.Gson;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;

class GeoLocation {
	private String latitude;
	private String longitude;
	public GeoLocation(String latitude, String longitude) {
		this.latitude = latitude;
		this.longitude = longitude;
	}
}

class Tweet {
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getTweetId() {
		return tweetId;
	}
	public void setTweetId(String tweetId) {
		this.tweetId = tweetId;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getKeyword() {
		return keyword;
	}
	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}
	public String getLatitude() {
		return latitude;
	}
	public void setLatitude(String latitude) {
		this.latitude = latitude;
	}
	public String getLongitude() {
		return longitude;
	}
	public void setLongitude(String longitude) {
		this.longitude = longitude;
	}
	private String username; 
	private String tweetId;
	private String content;
	private String keyword;
	private String latitude;
	private String longitude;
}

@WebServlet("/ActionServlet")
public class ActionServlet extends HttpServlet {
	
	static JestClientFactory factory;
	static JestClient client;
	private static final long serialVersionUID = 1L;
       
	public ActionServlet() {}
    @SuppressWarnings("deprecation")
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    	
    	String keyword = request.getParameter("key");
    	factory = new JestClientFactory();
		factory.setHttpClientConfig(new HttpClientConfig
		                        .Builder("https://search-tweetdata2-fhfcwpqr2s5vebvd6vcoq5lldq.us-east-1.es.amazonaws.com/")
		                        .multiThreaded(true)
		                        .readTimeout(60000)
		                        .build());
		 client = factory.getObject();
		 
		 SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		 searchSourceBuilder.query(QueryBuilders.matchQuery("keyword", keyword));
		 searchSourceBuilder.size(10000);

		 Search search = new Search.Builder(searchSourceBuilder.toString())
		                                 .addIndex("tweetdata")
		                                 .build();
		 
		 SearchResult result = null;
		 try {
			 result = client.execute(search);
		 } catch (IOException e) {
		 	// TODO Auto-generated catch block
			e.printStackTrace();
		 }
		 
		 List<GeoLocation> locs = new ArrayList<GeoLocation>();
		 List<Tweet> listOfTweets = result.getSourceAsObjectList(Tweet.class); 
		 for(Tweet tweet: listOfTweets)
			 locs.add(new GeoLocation(tweet.getLatitude(), tweet.getLongitude()));
		 
		String json = new Gson().toJson(locs);    	
		response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8"); 
		response.getWriter().write(json); 
		}
		
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {}
}

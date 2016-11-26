import java.io.IOException;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;
import io.searchbox.core.Index;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@WebServlet("/NotifyServlet")
public class NotifyServlet extends HttpServlet {

	public NotifyServlet() {
	}

	static JestClientFactory factory;
	static JestClient client;

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String messagetype = request.getHeader("x-amz-sns-message-type");
		if (messagetype == null)
			return;

		Scanner scan = new Scanner(request.getInputStream());
		StringBuilder builder = new StringBuilder();
		while (scan.hasNextLine()) {
			builder.append(scan.nextLine());
		}
		String msg = builder.toString();

		// Process the message based on type.
		if (messagetype.equals("Notification")) {

			factory = new JestClientFactory();
			factory.setHttpClientConfig(new HttpClientConfig.Builder(
					"https://search-tweetsentiment-klny5bkplkqihaitghiyl3hsfm.us-east-1.es.amazonaws.com/")
							.multiThreaded(true).readTimeout(60000).build());
			client = factory.getObject();

			String[] parts = msg.split(":::");
			// Insert into AWS ES

			String source = jsonBuilder().startObject().field("latitude", parts[0]).field("longitude", parts[1])
					.field("sentiment", parts[2]).field("keyword", parts[3]).field("content", parts[4]).endObject()
					.string();

			Index index = new Index.Builder(source).index("tweetdata").type("tweet").build();
			client.execute(index);
			System.out.println("New Tweet Added for " + parts[3]);
		} else if (messagetype.equals("SubscriptionConfirmation")) {
			System.out.println(">>Subscription confirmation = " + msg);
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
	}

}

package pl.oragne.imei.hub;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
public class HubController {

	private static final Logger logger = LoggerFactory.getLogger(HubController.class);

	@Value("#{'${imeiAll}'.split(',')}")
	private List<String> addresses;

	@Value("#{PropertySplitter.map('${bgs-ws-urls}')}")
	private Map<String, String> niceNames;

	@RequestMapping(value = "/hub", method = { GET, PUT, POST, DELETE })
	public ResponseEntity<Map<String, JsonRawResponse>> action(@RequestBody(required = false) JsonNode json,
			HttpServletRequest request, @RequestHeader HttpHeaders headers) {

		String method = request.getMethod();
		logger.info("Json to be forwarded {}", json);
		headers.keySet().forEach(h -> logger.info("Header to be forwarded {} - {}", h, headers.get(h)));

		addresses.stream().forEach(a -> logger.info("Adress in application.properties {}", a));

		Map<String, JsonRawResponse> responses = new HashMap<String, JsonRawResponse>();

		HttpEntity<JsonNode> req = new HttpEntity<JsonNode>(json, headers);

		switch (method) {
		case "GET":
			logger.info("GET invoked");
			send(req, HttpMethod.GET, addresses, responses);
			responses.keySet().stream().forEach(r -> logger.info("Received response {}", r));
			// logger.info("Received response {}",o);
			break;

		case "POST":
			logger.info("POST invoked");
			// response =
			// restTemplate.postForEntity("http://localhost:8087/test/post/",
			// req, String.class);
			send(req, HttpMethod.POST, addresses, responses);
			responses.keySet().stream()
					.forEach(r -> logger.info("Received response from {} - {}", r, responses.get(r)));
			break;

		case "PUT":
			logger.info("PUT invoked");
			send(req, HttpMethod.PUT, addresses, responses);
			// HttpEntity<JsonNode> requestUpdate = new HttpEntity<>(json,
			// null);
			// response = restTemplate.exchange("", HttpMethod.PUT,
			// requestUpdate, String.class);
			responses.keySet().stream().forEach(r -> logger.info("Received response {}", r));
			break;

		case "DELETE":
			logger.info("DELETE invoked");
			send(req, HttpMethod.DELETE, addresses, responses);
			// restTemplate.delete(request.getRequestURI());
			responses.keySet().stream().forEach(r -> logger.info("Received response {}", r));
			break;

		default:
			logger.info("UNKNOWN");
			break;
		}

		return new ResponseEntity<Map<String, JsonRawResponse>>(responses, HttpStatus.valueOf(200));
	}

	private void send(HttpEntity<JsonNode> req, HttpMethod method, List<String> addresses,
			Map<String, JsonRawResponse> responses) {
		addresses.stream().forEach(addr -> {
			logger.info("==========================================");
			logger.info("Sending to {}", addr);
			String responseString = null;
			String responseCode = null;
			ResponseEntity<JsonRawResponse> postForEntity = null;
			try {
				postForEntity = new RestTemplate().postForEntity(addr, req, JsonRawResponse.class);
				logger.info("Response headers received {}", postForEntity.getHeaders());
				logger.info("Response code received {}", postForEntity.getStatusCode());
				logger.info("Response body received {}", postForEntity.getBody());
			} catch (Exception e) {
				logger.info("Exception when invoking {} -> {}", addr, e.getClass());
				if (e instanceof HttpServerErrorException) {
					responseCode = String.valueOf(((HttpServerErrorException) e).getStatusCode());
					responseString = ((HttpServerErrorException) e).getResponseBodyAsString();
				} else if (e instanceof HttpClientErrorException) {
					responseCode = String.valueOf(((HttpClientErrorException) e).getStatusCode());
				}
			}
			responseString = responseString == null ? "NO_REPONSE_BODY_RECEIVED_FROM_BGS_WEBSERVICE" : responseString;
			responseCode = responseCode != null ? responseCode : postForEntity.getStatusCode().toString();

			logger.info("Derived response body to {}", responseString);
			logger.info("==========================================");
		
			String keyFromProps = addr.substring(addr.indexOf("28"),addr.length());
			responses.put(niceNames.get(keyFromProps), new JsonRawResponse(String.format("%s:%s", responseCode, responseString)));
		});
	}

	@RequestMapping(value = "/test/get/{param}", method = { GET })
	public String get(@RequestParam(name = "param", defaultValue = "default") String param,
			HttpServletRequest request) {
		logger.info("Received request {}", request);
		return "";

	}

	@RequestMapping(value = "/test/post", method = { POST })
	public JsonRawResponse post(HttpServletRequest request) {
		logger.info("Received request {}", request);
		ObjectMapper objectMapper = new ObjectMapper();
		String obj = null;
		try {
			obj = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString("{\"name\":\"Ala\"}");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		return new JsonRawResponse(obj);
	}

	@RequestMapping(value = "/test/put", method = { PUT })
	public String put(HttpServletRequest request) {
		logger.info("Received request {}", request);
		return "ok";

	}

	@RequestMapping(value = "/test/delete", method = { DELETE })
	public String delete(HttpServletRequest request) {
		logger.info("Received request {}", request);
		return "ok";

	}
}

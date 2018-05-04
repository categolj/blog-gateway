package am.ik.blog.gateway;

import java.util.LinkedHashMap;
import java.util.Map;

import brave.Tracing;
import brave.internal.HexCodec;
import brave.propagation.TraceContext;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaProducerGatewayFilterFactory extends AbstractGatewayFilterFactory {
	private final KafkaTemplate<Object, Object> kafkaTemplate;

	public KafkaProducerGatewayFilterFactory(
			KafkaTemplate<Object, Object> kafkaTemplate) {
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public GatewayFilter apply(Object config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			String uri = request.getURI().toString();
			Map<String, Object> json = new LinkedHashMap<>();
			json.put("uri", uri);
			json.put("trace", this.traceJson());
			json.put("request", this.requestJson(request));
			long begin = System.currentTimeMillis();
			return chain.filter(exchange).doOnTerminate(() -> {
				long end = System.currentTimeMillis();
				ServerHttpResponse response = exchange.getResponse();
				json.put("response", this.responseJson(response));
				json.put("processTime", this.processTimeJson(begin, end));
				this.kafkaTemplate.sendDefault(uri, json);
			});
		};
	}

	Map<String, Object> requestJson(ServerHttpRequest request) {
		Map<String, Object> req = new LinkedHashMap<>();
		req.put("headers", request.getHeaders());
		req.put("params", request.getQueryParams());
		return req;
	}

	Map<String, Object> responseJson(ServerHttpResponse response) {
		Map<String, Object> res = new LinkedHashMap<>();
		res.put("header", response.getHeaders());
		res.put("statusCode", response.getStatusCode().value());
		return res;
	}

	Map<String, Object> traceJson() {
		Map<String, Object> json = new LinkedHashMap<>();
		TraceContext currentSpan = Tracing.currentTracer().currentSpan().context();
		json.put("X-B3-Spanid", HexCodec.toLowerHex(currentSpan.spanId()));
		json.put("X-B3-Traceid", currentSpan.traceIdString());
		Long parentId = currentSpan.parentId();
		if (parentId != null) {
			json.put("X-B3-Parentspanid", HexCodec.toLowerHex(parentId));
		}
		return json;
	}

	Map<String, Object> processTimeJson(long begin, long end) {
		Map<String, Object> processTime = new LinkedHashMap<>();
		processTime.put("begin", begin);
		processTime.put("end", end);
		processTime.put("millis", end - begin);
		return processTime;
	}
}

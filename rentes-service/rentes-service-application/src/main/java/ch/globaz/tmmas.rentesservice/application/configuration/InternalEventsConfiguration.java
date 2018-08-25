package ch.globaz.tmmas.rentesservice.application.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class InternalEventsConfiguration {




	@Bean(name = "applicationEventMulticaster")
    public ApplicationEventMulticaster simpleApplicationEventMulticaster() {
		SimpleApplicationEventMulticaster eventMulticaster
				= new SimpleApplicationEventMulticaster();

		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
		executor.setTaskDecorator(new MdcTaskDecorator());

		eventMulticaster.setTaskExecutor(executor);
		return eventMulticaster;
	}
/**

	@Bean
	ObjectMapper objectMapper() {
		final ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.findAndRegisterModules();
		objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		return objectMapper;
	}
*/
	/**
	 * Client REST pour ElasticSearch haut niveau.
	 * Voir <a href="https://www.elastic.co/blog/the-elasticsearch-java-high-level-rest-client-is-out">
	 *     RestHighLevelClient doc</a><br/>
	 * Gère en interne un pool de thread pour les clients bas niveau.
	 * @param props
	 * @return une instance de
	 */
	@Bean
	RestHighLevelClient restHighLevelClient(ElasticSearchProperties props) {
		return new RestHighLevelClient(
				RestClient
						.builder(props.hosts())
						.setRequestConfigCallback(config -> config
								.setConnectTimeout(props.getConnectTimeout())
								.setConnectionRequestTimeout(props.getConnectionRequestTimeout())
								.setSocketTimeout(props.getSocketTimeout())
						)
						.setMaxRetryTimeoutMillis(props.getMaxRetryTimeoutMillis()));
	}

}

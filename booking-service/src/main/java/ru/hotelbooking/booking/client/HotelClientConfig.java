package ru.hotelbooking.booking.client;

import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import ru.hotelbooking.booking.security.TokenService;

@Configuration
public class HotelClientConfig {

    // Не через @LoadBalanced RestClient.Builder. Такой билдер подхватывает и eureka-клиент,
    // и запросы к самому реестру начинают идти через балансировщик. Поэтому интерсептор только здесь.
    @Bean
    public RestClient hotelServiceRestClient(RestClient.Builder builder,
                                             HotelClientProperties properties,
                                             LoadBalancerClient loadBalancerClient,
                                             LoadBalancerRequestFactory loadBalancerRequestFactory,
                                             TokenService tokenService) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());

        return builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .requestInterceptor(new LoadBalancerInterceptor(loadBalancerClient, loadBalancerRequestFactory))
                .requestInterceptor(serviceAuthentication(tokenService))
                .build();
    }

    private static ClientHttpRequestInterceptor serviceAuthentication(TokenService tokenService) {
        return (request, body, execution) -> {
            request.getHeaders().setBearerAuth(tokenService.issueServiceToken());
            return execution.execute(request, body);
        };
    }
}

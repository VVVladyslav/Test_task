package com.example.demo.simulator;

import com.example.demo.dto.ClientDto;
import com.example.demo.dto.ClientProfitDto;
import com.example.demo.dto.ClientStatusRequest;
import com.example.demo.dto.CreateClientRequest;
import com.example.demo.dto.CreateOrderRequest;
import com.example.demo.dto.OrderDto;
import com.example.demo.dto.ScenarioAttemptResultDto;
import com.example.demo.dto.ScenarioSummaryDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Service
public class ScenarioService {

    private final RestTemplate restTemplate = new RestTemplate();
    private static final String BASE = "http://localhost:8080";

    private <T> ResponseEntity<T> post(String url, Object body, Class<T> type) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> req = new HttpEntity<>(body, h);
        return restTemplate.exchange(url, HttpMethod.POST, req, type);
    }

    private <T> ResponseEntity<T> patch(String url, Object body, Class<T> type) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Object> req = new HttpEntity<>(body, h);
        return restTemplate.exchange(url, HttpMethod.PATCH, req, type);
    }

    private <T> ResponseEntity<T> get(String url, Class<T> type) {
        return restTemplate.exchange(url, HttpMethod.GET, null, type);
    }

    public ClientDto createClient(String name, String email) {
        CreateClientRequest req = CreateClientRequest.builder()
                .name(name)
                .email(email)
                .address("—")
                .build();
        return post(BASE + "/api/clients", req, ClientDto.class).getBody();
    }

    public OrderDto createOrder(Long supplierId, Long consumerId, String title, BigDecimal price) {
        CreateOrderRequest req = CreateOrderRequest.builder()
                .title(title)
                .supplierId(supplierId)
                .consumerId(consumerId)
                .price(price)
                .build();
        return post(BASE + "/api/orders", req, OrderDto.class).getBody();
    }

    public ClientProfitDto getProfit(Long clientId) {
        return get(BASE + "/api/clients/" + clientId + "/profit", ClientProfitDto.class).getBody();
    }

    public ClientDto setActive(Long clientId, boolean active) {
        ClientStatusRequest body = ClientStatusRequest.builder().active(active).build();
        return patch(BASE + "/api/clients/" + clientId + "/status", body, ClientDto.class).getBody();
    }

    public ScenarioSummaryDto runDuplicates(int n) throws InterruptedException {
        long ts = System.currentTimeMillis();
        ClientDto supplier = createClient("Supp-" + ts, "supp" + ts + "@mail.test");
        ClientDto consumer = createClient("Cons-" + ts, "cons" + ts + "@mail.test");

        final String title = "dup-" + ts;    // одинаковый title для всех попыток
        final BigDecimal price = BigDecimal.ONE;

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(16, n));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ScenarioAttemptResultDto>> futures = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    OrderDto res = createOrder(supplier.getId(), consumer.getId(), title, price);
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(true).httpStatus(201)
                            .orderId(Objects.requireNonNull(res).getId())
                            .message("created").build();
                } catch (HttpStatusCodeException ex) {
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(false).httpStatus(ex.getRawStatusCode())
                            .message(ex.getResponseBodyAsString()).build();
                } catch (Exception e) {
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(false).httpStatus(null)
                            .message(e.getMessage()).build();
                }
            }));
        }

        start.countDown();
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);

        List<ScenarioAttemptResultDto> attempts = new ArrayList<>();
        int ok = 0, fail = 0;
        for (Future<ScenarioAttemptResultDto> f : futures) {
            try {
                ScenarioAttemptResultDto r = f.get(60, TimeUnit.SECONDS);
                attempts.add(r);
                if (r.isSuccess()) ok++; else fail++;
            } catch (Exception e) {
                attempts.add(ScenarioAttemptResultDto.builder()
                        .index(-1).success(false).httpStatus(null).message(e.getMessage()).build());
                fail++;
            }
        }
        return ScenarioSummaryDto.builder()
                .scenario("duplicates")
                .requested(n)
                .succeeded(ok)
                .failed(fail)
                .attempts(attempts)
                .build();
    }

    public ScenarioSummaryDto runDescending(int n) throws InterruptedException {
        long ts = System.currentTimeMillis();
        ClientDto supplier = createClient("Supp-" + ts, "supp" + ts + "@mail.test");
        ClientDto consumer = createClient("Cons-" + ts, "cons" + ts + "@mail.test");

        createOrder(supplier.getId(), consumer.getId(), "seed-" + ts, new BigDecimal("970"));

        final String commonTitle = "dec-common-" + ts;
        List<BigDecimal> prices = new ArrayList<>();
        for (int p = 100; p >= 10 && prices.size() < n; p -= 10) {
            prices.add(new BigDecimal(p));
        }

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(16, prices.size()));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ScenarioAttemptResultDto>> futures = new ArrayList<>();

        for (int i = 0; i < prices.size(); i++) {
            final int idx = i;
            final BigDecimal price = prices.get(i);
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    OrderDto res = createOrder(supplier.getId(), consumer.getId(), commonTitle, price);
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(true).httpStatus(201)
                            .orderId(Objects.requireNonNull(res).getId())
                            .message("created").build();
                } catch (HttpStatusCodeException ex) {
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(false).httpStatus(ex.getRawStatusCode())
                            .message(ex.getResponseBodyAsString()).build();
                } catch (Exception e) {
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(false).httpStatus(null)
                            .message(e.getMessage()).build();
                }
            }));
        }

        start.countDown();
        pool.shutdown();
        pool.awaitTermination(60, TimeUnit.SECONDS);

        List<ScenarioAttemptResultDto> attempts = new ArrayList<>();
        int ok = 0, fail = 0;
        for (Future<ScenarioAttemptResultDto> f : futures) {
            try {
                ScenarioAttemptResultDto r = f.get(60, TimeUnit.SECONDS);
                attempts.add(r);
                if (r.isSuccess()) ok++; else fail++;
            } catch (Exception e) {
                attempts.add(ScenarioAttemptResultDto.builder()
                        .index(-1).success(false).httpStatus(null).message(e.getMessage()).build());
                fail++;
            }
        }
        return ScenarioSummaryDto.builder()
                .scenario("descending")
                .requested(prices.size())
                .succeeded(ok)
                .failed(fail)
                .attempts(attempts)
                .build();
    }

    public ScenarioSummaryDto runDeactivationRace(int n, long deactivateAfterMillis) throws InterruptedException {
        long ts = System.currentTimeMillis();
        ClientDto supplier = createClient("Supp-" + ts, "supp" + ts + "@mail.test");
        ClientDto consumer = createClient("Cons-" + ts, "cons" + ts + "@mail.test");

        ExecutorService pool = Executors.newFixedThreadPool(Math.min(16, n + 1));
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ScenarioAttemptResultDto>> futures = new ArrayList<>();

        // Параллельные заказы с различными title
        for (int i = 0; i < n; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                start.await();
                try {
                    OrderDto res = createOrder(supplier.getId(), consumer.getId(), "race-" + ts + "-" + idx, new BigDecimal("50"));
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(true).httpStatus(201)
                            .orderId(Objects.requireNonNull(res).getId())
                            .message("created").build();
                } catch (HttpStatusCodeException ex) {
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(false).httpStatus(ex.getRawStatusCode())
                            .message(ex.getResponseBodyAsString()).build();
                } catch (Exception e) {
                    return ScenarioAttemptResultDto.builder()
                            .index(idx).success(false).httpStatus(null)
                            .message(e.getMessage()).build();
                }
            }));
        }

        futures.add(pool.submit(() -> {
            start.await();
            try {
                Thread.sleep(Math.max(0, deactivateAfterMillis));
                setActive(consumer.getId(), false);
                return ScenarioAttemptResultDto.builder()
                        .index(n).success(true).httpStatus(200).message("consumer deactivated").build();
            } catch (HttpStatusCodeException ex) {
                return ScenarioAttemptResultDto.builder()
                        .index(n).success(false).httpStatus(ex.getRawStatusCode())
                        .message(ex.getResponseBodyAsString()).build();
            } catch (Exception e) {
                return ScenarioAttemptResultDto.builder()
                        .index(n).success(false).httpStatus(null)
                        .message(e.getMessage()).build();
            }
        }));

        start.countDown();
        pool.shutdown();
        pool.awaitTermination(90, TimeUnit.SECONDS);

        List<ScenarioAttemptResultDto> attempts = new ArrayList<>();
        for (Future<ScenarioAttemptResultDto> f : futures) {
            try {
                attempts.add(f.get(90, TimeUnit.SECONDS));
            } catch (Exception e) {
                attempts.add(ScenarioAttemptResultDto.builder()
                        .index(-1).success(false).httpStatus(null).message(e.getMessage()).build());
            }
        }

        long ordersOk = attempts.stream().filter(a -> a.getIndex() >= 0 && a.getIndex() < n && a.isSuccess()).count();
        long ordersFail = attempts.stream().filter(a -> a.getIndex() >= 0 && a.getIndex() < n && !a.isSuccess()).count();

        return ScenarioSummaryDto.builder()
                .scenario("deactivation_race (orders only, deactivation excluded)")
                .requested(n)
                .succeeded((int) ordersOk)
                .failed((int) ordersFail)
                .attempts(attempts)
                .build();
    }
}
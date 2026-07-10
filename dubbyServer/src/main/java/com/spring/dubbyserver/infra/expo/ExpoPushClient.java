package com.spring.dubbyserver.infra.expo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Expo Push API 클라이언트 — 발송(100건 청크) + 영수증 조회(1000건 청크).
 * https://docs.expo.dev/push-notifications/sending-notifications/
 */
@Slf4j
@Component
public class ExpoPushClient {

    private static final int SEND_CHUNK = 100;
    private static final int RECEIPT_CHUNK = 1000;

    private final RestClient restClient;

    public ExpoPushClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(15));
        this.restClient = RestClient.builder()
                .baseUrl("https://exp.host/--/api/v2")
                .requestFactory(factory)
                .build();
    }

    public record PushMessage(String to, String title, String body, Map<String, Object> data) {}

    /** status: "ok"(id=티켓) | "error"(error=사유, DeviceNotRegistered 등) */
    public record Ticket(String status, String id, String error) {}

    public record Receipt(String status, String error) {}

    /** 입력 순서대로 티켓 반환. 청크 단위 실패 시 해당 청크는 error 티켓으로 채움 */
    public List<Ticket> send(List<PushMessage> messages) {
        List<Ticket> tickets = new ArrayList<>(messages.size());
        for (int i = 0; i < messages.size(); i += SEND_CHUNK) {
            List<PushMessage> chunk = messages.subList(i, Math.min(i + SEND_CHUNK, messages.size()));
            tickets.addAll(sendChunk(chunk));
        }
        return tickets;
    }

    @SuppressWarnings("unchecked")
    private List<Ticket> sendChunk(List<PushMessage> chunk) {
        try {
            List<Map<String, Object>> body = chunk.stream()
                    .map(m -> Map.<String, Object>of(
                            "to", m.to(), "title", m.title(), "body", m.body(),
                            "data", m.data(), "sound", "default"))
                    .toList();
            Map<String, Object> res = restClient.post().uri("/push/send")
                    .contentType(MediaType.APPLICATION_JSON).body(body)
                    .retrieve().body(Map.class);
            List<Map<String, Object>> data = res != null
                    ? (List<Map<String, Object>>) res.getOrDefault("data", List.of()) : List.of();
            List<Ticket> tickets = new ArrayList<>();
            for (int i = 0; i < chunk.size(); i++) {
                if (i < data.size()) {
                    Map<String, Object> t = data.get(i);
                    Map<String, Object> details = (Map<String, Object>) t.getOrDefault("details", Map.of());
                    tickets.add(new Ticket(
                            String.valueOf(t.get("status")),
                            t.get("id") != null ? String.valueOf(t.get("id")) : null,
                            details.get("error") != null ? String.valueOf(details.get("error")) : null));
                } else {
                    tickets.add(new Ticket("error", null, "NoTicket"));
                }
            }
            return tickets;
        } catch (Exception e) {
            log.warn("Expo push 청크 발송 실패({}건): {}", chunk.size(), e.getMessage());
            return chunk.stream().map(m -> new Ticket("error", null, "SendFailed")).toList();
        }
    }

    /** ticketId → Receipt. 아직 준비 안 된 티켓은 응답에 없음 (다음 주기 재확인) */
    @SuppressWarnings("unchecked")
    public Map<String, Receipt> getReceipts(List<String> ticketIds) {
        Map<String, Receipt> receipts = new java.util.HashMap<>();
        for (int i = 0; i < ticketIds.size(); i += RECEIPT_CHUNK) {
            List<String> chunk = ticketIds.subList(i, Math.min(i + RECEIPT_CHUNK, ticketIds.size()));
            try {
                Map<String, Object> res = restClient.post().uri("/push/getReceipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("ids", chunk))
                        .retrieve().body(Map.class);
                Map<String, Map<String, Object>> data = res != null
                        ? (Map<String, Map<String, Object>>) res.getOrDefault("data", Map.of()) : Map.of();
                data.forEach((id, r) -> {
                    Map<String, Object> details = (Map<String, Object>) r.getOrDefault("details", Map.of());
                    receipts.put(id, new Receipt(
                            String.valueOf(r.get("status")),
                            details.get("error") != null ? String.valueOf(details.get("error")) : null));
                });
            } catch (Exception e) {
                log.warn("Expo 영수증 조회 실패: {}", e.getMessage());
            }
        }
        return receipts;
    }
}

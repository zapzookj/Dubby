package com.spring.dubbyserver.domain.chat;

import com.spring.dubbyserver.infra.llm.SafetyFilter.Category;

import java.util.List;
import java.util.Map;

/**
 * 고위험 입력 시스템 카드 문구 — 페르소나 바이블 §6.4.
 * 더비가 아닌 앱(시스템)이 말한다. 이 문구에는 더비 톤을 쓰지 않는다 (장난 금지 구역).
 */
public final class SafetyNotices {

    public record Resource(String label, String action) {}

    public record Notice(String category, String title, String body, List<Resource> resources) {}

    private static final Map<Category, Notice> NOTICES = Map.of(
            Category.SELF_HARM, new Notice("SELF_HARM", "안내",
                    "지금 많이 힘든 상황이라면, 더비보다 도움이 되는 곳이 있어요.",
                    List.of(
                            new Resource("자살예방 상담전화 109 (24시간)", "tel:109"),
                            new Resource("정신건강 위기상담 1577-0199", "tel:15770199"))),
            Category.MEDICAL, new Notice("MEDICAL", "안내",
                    "건강에 관한 내용은 더비가 다룰 수 없어요. 정확한 정보는 의료 전문가에게 확인해 주세요.",
                    List.of()),
            Category.LEGAL, new Notice("LEGAL", "안내",
                    "법률에 관한 내용은 더비가 다룰 수 없어요. 정확한 정보는 법률 전문가에게 확인해 주세요.",
                    List.of()),
            Category.FINANCE, new Notice("FINANCE", "안내",
                    "투자·금융에 관한 조언은 더비가 다룰 수 없어요. 전문가와 상담해 주세요.",
                    List.of()),
            Category.CRIME_VIOLENCE, new Notice("CRIME_VIOLENCE", "안내",
                    "이 주제는 더비가 도울 수 없어요.",
                    List.of()));

    private SafetyNotices() {}

    public static Notice of(Category category) {
        return NOTICES.get(category);
    }
}

package com.spring.dubbyserver.global.common;

import java.util.List;

/** 커서 페이지네이션 공통 응답 { items, nextCursor, hasNext } */
public record CursorPageResponse<T>(List<T> items, Long nextCursor, boolean hasNext) {}

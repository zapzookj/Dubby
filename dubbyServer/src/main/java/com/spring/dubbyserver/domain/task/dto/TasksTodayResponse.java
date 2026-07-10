package com.spring.dubbyserver.domain.task.dto;

import java.util.List;

public record TasksTodayResponse(String date, List<TaskItem> tasks) {}

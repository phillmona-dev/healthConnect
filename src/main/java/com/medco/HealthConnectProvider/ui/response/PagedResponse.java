package com.medco.HealthConnectProvider.ui.response;

import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public PagedResponse(List<UserResponse> content, int number, int size, long totalElements, int totalPages, boolean b, boolean b1) {
    }
}
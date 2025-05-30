package com.medco.HealthConnectProvider.utils.paginationUtils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PaginationUtils {

    public static Pageable paginateResource(int page, int limit, String sortBy, String sortDirection){
        if (page > 0)
            page = page - 1;

        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection),sortBy);

        return PageRequest.of(page, limit,sort);
    }
}

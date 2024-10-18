package com.lx.gulimall.search.service;

import com.lx.gulimall.search.vo.SearchParam;
import com.lx.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam searchParam);
}

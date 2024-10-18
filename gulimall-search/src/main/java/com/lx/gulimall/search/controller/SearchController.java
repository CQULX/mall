package com.lx.gulimall.search.controller;

import com.lx.common.constant.AuthServerConstant;
import com.lx.gulimall.search.service.MallSearchService;
import com.lx.gulimall.search.vo.SearchParam;
import com.lx.gulimall.search.vo.SearchResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchController {

    @Autowired
    MallSearchService mallSearchService;

    @GetMapping("/list.html")
    public String listPage(SearchParam searchParam, Model model, HttpServletRequest request, HttpSession session){
        searchParam.set_queryString(request.getQueryString());
        Object attribute = session.getAttribute(AuthServerConstant.LOGIN_USER);
        if(attribute==null){
            //没登录
            System.out.println("无session");
        }else {
            System.out.println(session);
        }
        SearchResult result=mallSearchService.search(searchParam);
        model.addAttribute("result",result);
        return "list";
    }
}

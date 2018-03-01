package com.play.cn.controller;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import javax.servlet.http.HttpServletRequest;

@Controller
@Scope(value="prototype")
public class IndexController extends BaseController {

	/**
	 * 首页
	 * @return
	 */
	@RequestMapping({"/", "/index"})
	public String index(HttpServletRequest request, Model model) {
		resultMap.put("title", "首页");
		resultMap.put("navigation_url", "/index.shtml");
		resultMap.put("navigation_title", "首页");
		resultMap.put("navigation_sub_title", null);
		model.addAttribute("resultMap", resultMap);
		return "index";
	}
}
